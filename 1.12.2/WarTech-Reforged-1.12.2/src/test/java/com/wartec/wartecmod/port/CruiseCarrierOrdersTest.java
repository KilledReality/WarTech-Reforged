package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.content.WarTechContent;
import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.uav.*;
import java.lang.reflect.Method;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/** Two-way task transfer, provenance and save/load; no live flight claim. */
public class CruiseCarrierOrdersTest {
    @BeforeClass public static void boot() {
        Bootstrap.register();
        if(net.minecraft.item.Item.REGISTRY.getNameForObject(WarTechContent.ASSEMBLED_CRUISE)==null)
            net.minecraftforge.fml.common.registry.ForgeRegistries.ITEMS.register(WarTechContent.ASSEMBLED_CRUISE);
    }
    private ItemStack store(Vec3d target,int dimension,boolean search) {
        CruiseBuild b=CruiseBuild.starter(CruisePartDefinition.BODY_LIGHT);b.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);
        b.set(CruiseSlot.WINGS,CruisePartDefinition.WINGS_COMPACT);
        if(search) b.set(CruiseSlot.SEEKER,CruisePartDefinition.SEEKER_OPTICAL);
        ItemStack stack=new ItemStack(WarTechContent.ASSEMBLED_CRUISE);b.writeToStack(stack);
        if(target!=null) {
            CruiseMission program=new CruiseMission();if(search) program.setMode(CruiseMission.Mode.SEARCH,b);
            program.append(target,dimension);if(search) program.append(target.addVector(200,0,0),dimension);program.writeToStack(stack);
        }
        return stack;
    }
    private ItemStack store(int x,int y,int z) { return store(new Vec3d(x,y,z),0,false); }
    private EntityWarTechAircraft plane(WarTechEntityProfile profile) { return new EntityWarTechAircraft(null,profile); }
    private EntityCustomUav uav() {
        EntityCustomUav u=new EntityCustomUav(null);u.configure(UavBuild.cruiseCarrier(UavAirframe.RECON),null);return u;
    }
    private void target(EntityWarTechBase carrier,int x,int y,int z) {
        assertTrue(carrier.hasGuidanceTarget());assertEquals(x,carrier.getTargetX(),0);assertEquals(y,carrier.getTargetY(),0);assertEquals(z,carrier.getTargetZ(),0);
    }
    private NBTTagCompound save(EntityWarTechBase carrier) throws Exception {
        Method m=carrier.getClass().getDeclaredMethod("writeEntityToNBT",NBTTagCompound.class);m.setAccessible(true);
        NBTTagCompound tag=new NBTTagCompound();m.invoke(carrier,tag);return tag;
    }
    private void read(EntityWarTechBase carrier,NBTTagCompound tag) throws Exception {
        Method m=carrier.getClass().getDeclaredMethod("readEntityFromNBT",NBTTagCompound.class);m.setAccessible(true);m.invoke(carrier,tag);
    }
    @Test public void loadingImmediatelyImportsXYZForBothFightersAndTu95() {
        for(WarTechEntityProfile p:new WarTechEntityProfile[]{WarTechEntityProfile.SU_27,WarTechEntityProfile.F_16C,WarTechEntityProfile.TU_95}) {
            EntityWarTechAircraft a=plane(p);a.setInventorySlotContents(0,store(-700,47,900));
            target(a,-700,47,900);assertEquals(1,a.getTargetCount());assertEquals(9,a.getLegacySelectedPayload());
        }
    }
    @Test public void severalProgrammedStoresBecomeOrderedCarrierTasks() throws Exception {
        EntityWarTechAircraft a=plane(WarTechEntityProfile.SU_27);
        for(int i=0;i<3;i++) a.setInventorySlotContents(i,store(700+i*100,40+i,900+i));
        NBTTagCompound tag=save(a);assertEquals(3,a.getTargetCount());
        for(int i=0;i<3;i++) { assertEquals(700+i*100,tag.getInteger("WarTechTargetX"+i));assertEquals(40+i,tag.getInteger("WarTechTargetY"+i));assertEquals(900+i,tag.getInteger("WarTechTargetZ"+i)); }
    }
    @Test public void replacingAndRemovingStoresRefreshesOnlyAutomaticTasks() {
        EntityWarTechAircraft a=plane(WarTechEntityProfile.F_16C);a.setInventorySlotContents(0,store(700,40,900));
        a.setInventorySlotContents(0,store(1000,80,200));target(a,1000,80,200);
        a.removeStackFromSlot(0);assertEquals(0,a.getTargetCount());assertFalse(a.hasGuidanceTarget());
    }
    @Test public void manualAircraftTargetWinsUntilExplicitImport() {
        EntityWarTechAircraft a=plane(WarTechEntityProfile.SU_27);a.queueTarget(500,75,100,true);
        a.setInventorySlotContents(0,store(1000,50,-300));target(a,500,75,100);
        assertFalse(a.importCruiseTargets(false));assertTrue(a.importCruiseTargets(true));target(a,1000,50,-300);
    }
    @Test public void manuallyClearedOrdersAreNotResurrectedByInventoryUpdates() {
        EntityWarTechAircraft a=plane(WarTechEntityProfile.F_16C);a.setInventorySlotContents(0,store(700,47,900));
        a.clearTargetQueue();a.markDirty();assertFalse(a.hasGuidanceTarget());assertEquals(0,a.getTargetCount());
        assertTrue(a.importCruiseTargets(true));target(a,700,47,900);
    }
    @Test public void invalidOrForeignProgramsDoNotEraseManualOrdersOnExplicitImport() {
        EntityWarTechAircraft a=plane(WarTechEntityProfile.SU_27);a.queueTarget(500,75,100,true);
        a.setInventorySlotContents(0,store(new Vec3d(900,50,100),1,false));assertFalse(a.importCruiseTargets(true));target(a,500,75,100);
        ItemStack malformed=store(900,50,100);malformed.getTagCompound().getCompoundTag("CruiseMission").setBoolean("Corrupt",true);
        a.setInventorySlotContents(0,malformed);assertFalse(a.importCruiseTargets(true));target(a,500,75,100);
    }
    @Test public void emptyProgramsAndNonMissileItemsProvideNoCoordinates() {
        assertNull(CruiseAircraftLoadout.programmedTarget(store(null,0,false),0));
        assertNull(CruiseAircraftLoadout.programmedTarget(ItemStack.EMPTY,0));
        assertNull(CruiseAircraftLoadout.programmedTarget(new ItemStack(WarTechContent.MQ9_PAYLOAD),0));
    }
    @Test public void searchRegionsStayInMissileWhileCarrierGetsOnlyFirstApproachGoal() {
        EntityWarTechAircraft a=plane(WarTechEntityProfile.SU_27);ItemStack s=store(new Vec3d(900,50,100),0,true);NBTTagCompound original=s.getTagCompound().copy();
        a.setInventorySlotContents(0,s);target(a,900,50,100);assertEquals(1,a.getTargetCount());
        assertEquals(original,a.getStackInSlot(0).getTagCompound());assertEquals(2,CruiseMission.fromStack(s).getTargets().size());
    }
    @Test public void activeAircraftSortieIsNeverReplannedByConsumption() {
        EntityWarTechAircraft a=plane(WarTechEntityProfile.F_16C);a.setInventorySlotContents(0,store(700,47,900));a.setLegacyState(2);
        a.removeStackFromSlot(0);target(a,700,47,900);assertFalse(a.importCruiseTargets(true));
    }
    @Test public void automaticAircraftOrderRemainsAutomaticAfterSaveAndLoad() throws Exception {
        EntityWarTechAircraft a=plane(WarTechEntityProfile.SU_27);a.setInventorySlotContents(0,store(700,47,900));
        EntityWarTechAircraft copy=plane(WarTechEntityProfile.SU_27);read(copy,save(a));target(copy,700,47,900);
        copy.setInventorySlotContents(0,store(1100,66,1200));target(copy,1100,66,1200);
    }
    @Test public void manualAircraftOrderAndLegacySavesArePreserved() throws Exception {
        EntityWarTechAircraft a=plane(WarTechEntityProfile.SU_27);a.queueTarget(500,75,100,true);a.setInventorySlotContents(0,store(700,47,900));
        for(boolean legacy:new boolean[]{false,true}) {
            NBTTagCompound tag=save(a);if(legacy) tag.removeTag("WarTechCruiseTargetManual");
            EntityWarTechAircraft copy=plane(WarTechEntityProfile.SU_27);read(copy,tag);copy.markDirty();target(copy,500,75,100);
        }
    }
    @Test public void uavImmediatelyGetsAnActualStrikeMissionFromLoadedMissile() {
        EntityCustomUav u=uav();u.setInventorySlotContents(0,store(700,47,900));target(u,700,47,900);
        assertEquals(1,u.getMission().size());assertEquals(UavWaypointMode.STRIKE,u.getMission().get(0).getMode());
        u.setInventorySlotContents(0,store(1100,65,400));target(u,1100,65,400);
        u.removeStackFromSlot(0);assertFalse(u.hasGuidanceTarget());assertTrue(u.getMission().isEmpty());
    }
    @Test public void manualUavDesignatorReplacesAutomaticMissionWithoutStaleWaypoints() {
        EntityCustomUav u=uav();u.setInventorySlotContents(0,store(700,47,900));u.setGuidanceTarget(550,75,120);
        u.markDirty();target(u,550,75,120);assertTrue(u.getMission().isEmpty());
        assertTrue(u.importCruiseTargets(true));target(u,700,47,900);assertEquals(UavWaypointMode.STRIKE,u.getMission().get(0).getMode());
    }
    @Test public void manuallyProgrammedUavMissionIsNotOverwrittenOnLoading() {
        UavMission mission=new UavMission();mission.add(new UavWaypoint(500,75,100,UavWaypointMode.OBSERVE));
        EntityCustomUav u=new EntityCustomUav(null);u.configure(UavBuild.cruiseCarrier(UavAirframe.RECON),mission,null);
        u.setInventorySlotContents(0,store(700,47,900));target(u,500,75,100);assertEquals(UavWaypointMode.OBSERVE,u.getMission().get(0).getMode());
    }
    @Test public void uavKeepsFullSearchProgramAndDoesNotImportForeignDimension() {
        EntityCustomUav u=uav();ItemStack s=store(new Vec3d(700,47,900),0,true);NBTTagCompound original=s.getTagCompound().copy();
        u.setInventorySlotContents(0,s);target(u,700,47,900);assertEquals(1,u.getMission().size());assertEquals(original,u.getCruiseStore().getTagCompound());
        u.setInventorySlotContents(0,store(new Vec3d(800,44,1200),1,false));assertFalse(u.hasGuidanceTarget());
    }
    @Test public void automaticAndManualUavProvenanceSurvivesSaveLoad() throws Exception {
        for(boolean manual:new boolean[]{false,true}) {
            EntityCustomUav u=uav();u.setInventorySlotContents(0,store(700,47,900));if(manual) u.setGuidanceTarget(500,75,100);
            EntityCustomUav copy=new EntityCustomUav(null);read(copy,save(u));copy.setInventorySlotContents(0,store(1100,65,400));
            if(manual) target(copy,500,75,100);else target(copy,1100,65,400);
        }
    }
    @Test public void uavActiveMissionIsNotChangedByMissileRemoval() {
        EntityCustomUav u=uav();u.setInventorySlotContents(0,store(700,47,900));u.setLegacyState(2);u.removeStackFromSlot(0);
        target(u,700,47,900);assertEquals(1,u.getMission().size());assertFalse(u.importCruiseTargets(true));
    }
}
