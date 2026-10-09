package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.content.WarTechContent;
import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.integration.DesignatorCompat;
import com.wartec.wartecmod.port.network.CruiseMissionEditMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Method;
import java.util.UUID;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/** Headless persistence/adapter checks, not a substitute for an in-game flight. */
public class CruisePersistenceTest {
    @BeforeClass public static void bootstrap() { Bootstrap.register(); }
    private static ItemStack missile(CruisePartDefinition body) {
        ItemStack stack=new ItemStack(WarTechContent.ASSEMBLED_CRUISE);
        CruiseBuild.starter(body).writeToStack(stack);
        CruiseMission mission=new CruiseMission();mission.append(new Vec3d(1500,64,900),0);mission.writeToStack(stack);
        return stack;
    }
    private static Object call(EntityCustomCruise entity,String name,Class<?>[] types,Object... arguments) throws Exception {
        Method method=EntityCustomCruise.class.getDeclaredMethod(name,types);method.setAccessible(true);
        return method.invoke(entity,arguments);
    }
    @Test public void buildOwnerMissionAndBodyHealthSurviveEntitySave() throws Exception {
        for(CruisePartDefinition body:CruiseAirframes.bodies()) {
            EntityCustomCruise source=new EntityCustomCruise(null);source.configure(missile(body),null);
            UUID owner=UUID.randomUUID();source.setOwnerIdentity(owner,"team-test");
            NBTTagCompound tag=new NBTTagCompound();call(source,"writeEntityToNBT",new Class<?>[]{NBTTagCompound.class},tag);
            EntityCustomCruise restored=new EntityCustomCruise(null);call(restored,"readEntityFromNBT",new Class<?>[]{NBTTagCompound.class},tag);
            assertSame(body,restored.getBuild().getAirframe());assertEquals(source.getBuild().checksum(),restored.getBuild().checksum());
            assertEquals(owner,restored.getOwnerUuid());assertEquals("team-test",restored.getOwnerTeam());
            assertEquals(CruiseAirframes.maximumHealth(body),restored.getHealthValue(),0.001);
            NBTTagCompound again=new NBTTagCompound();call(restored,"writeEntityToNBT",new Class<?>[]{NBTTagCompound.class},again);
            assertEquals(tag.getCompoundTag("CruiseMission"),again.getCompoundTag("CruiseMission"));
            assertFalse(again.getBoolean("Detonated"));assertEquals(-1,again.getInteger("Delay"));
        }
    }
    @Test public void shootDownEntersPersistedCrashInsteadOfInstantFullDetonation() throws Exception {
        EntityCustomCruise entity=new EntityCustomCruise(null);entity.configure(missile(CruisePartDefinition.BODY_HEAVY),null);
        call(entity,"explodeAndRemove",new Class<?>[0]);assertFalse(entity.isDead);assertEquals(3,entity.getFlightStage());
        NBTTagCompound tag=new NBTTagCompound();call(entity,"writeEntityToNBT",new Class<?>[]{NBTTagCompound.class},tag);
        assertTrue(tag.getBoolean("Crashing"));assertFalse(tag.getBoolean("Detonated"));
        EntityCustomCruise restored=new EntityCustomCruise(null);call(restored,"readEntityFromNBT",new Class<?>[]{NBTTagCompound.class},tag);
        assertEquals(3,restored.getFlightStage());
    }
    @Test public void legacyLifetimeDoesNotDeleteLongRangeCruise() throws Exception {
        EntityCustomCruise entity=new EntityCustomCruise(null);entity.configure(missile(CruisePartDefinition.BODY_CLASSIC),null);
        call(entity,"onLifetimeExpired",new Class<?>[0]);assertFalse(entity.isDead);
    }
    @Test public void corruptedFlightDistanceIsMarkedSpentOnRestore() throws Exception {
        EntityCustomCruise entity=new EntityCustomCruise(null);entity.configure(missile(CruisePartDefinition.BODY_CLASSIC),null);
        NBTTagCompound tag=new NBTTagCompound();call(entity,"writeEntityToNBT",new Class<?>[]{NBTTagCompound.class},tag);
        tag.setDouble("Travelled",Double.NaN);call(entity,"readEntityFromNBT",new Class<?>[]{NBTTagCompound.class},tag);
        NBTTagCompound again=new NBTTagCompound();call(entity,"writeEntityToNBT",new Class<?>[]{NBTTagCompound.class},again);
        assertTrue(again.getBoolean("Detonated"));
    }
    @Test public void completeDesignatorCoordinatesDoNotNeedAWorldOrChunkLookup() {
        ItemStack item=new ItemStack(WarTechContent.ITEM_TARGET_FINDER);NBTTagCompound tag=new NBTTagCompound();
        tag.setInteger("xCoord",1500000);tag.setInteger("zCoord",-1500000);item.setTagCompound(tag);
        assertNull(DesignatorCompat.getSavedTarget(item));tag.setInteger("yCoord",64);
        assertEquals(new Vec3d(1500000,64,-1500000),DesignatorCompat.getSavedTarget(item));
        assertNull(DesignatorCompat.getSavedTarget(missile(CruisePartDefinition.BODY_LIGHT)));
    }
    @Test public void missionPacketHasFixedBoundedEncodingAndStableRoundTrip() {
        CruiseMissionEditMessage message=new CruiseMissionEditMessage(40,-1140612620,0,1500,64,-900);
        ByteBuf first=Unpooled.buffer();ByteBuf second=Unpooled.buffer();
        try {
            message.toBytes(first);assertEquals(34,first.readableBytes());
            CruiseMissionEditMessage copy=new CruiseMissionEditMessage();copy.fromBytes(first.duplicate());copy.toBytes(second);
            assertEquals(first,second);
        } finally { first.release();second.release(); }
    }
    @Test public void onlyAirAdaptersFitFightersAndTu95NotReaper() {
        for(WarTechEntityProfile profile:new WarTechEntityProfile[]{WarTechEntityProfile.F_16C,WarTechEntityProfile.SU_27,WarTechEntityProfile.TU_95,WarTechEntityProfile.MQ_9_REAPER}) {
            EntityWarTechAircraft carrier=new EntityWarTechAircraft(null,profile);
            for(CruisePartDefinition body:new CruisePartDefinition[]{CruisePartDefinition.BODY_LIGHT,CruisePartDefinition.BODY_CLASSIC,CruisePartDefinition.BODY_HEAVY}) {
                ItemStack stack=missile(body);assertFalse(carrier.isPayloadCompatible(stack));
                CruiseBuild build=CruiseBuild.fromStack(stack);build.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);build.writeToStack(stack);
                assertEquals(profile!=WarTechEntityProfile.MQ_9_REAPER,carrier.isPayloadCompatible(stack));
            }
        }
    }
    @Test public void airborneBurstMustRemainBelowWorldCeiling() {
        CruiseBuild build=CruiseBuild.starter(CruisePartDefinition.BODY_CLASSIC);build.set(CruiseSlot.FUSE,CruisePartDefinition.FUSE_AIRBURST);
        CruiseMission mission=new CruiseMission();mission.append(new Vec3d(1500,250,900),0);
        assertFalse(mission.isValidFor(build,0));mission.clear();mission.append(new Vec3d(1500,245,900),0);
        assertTrue(mission.isValidFor(build,0));
    }
    @Test public void newPayloadCodesDoNotChangePersistedLegacyCodes() {
        EntityWarTechAircraft carrier=new EntityWarTechAircraft(null,WarTechEntityProfile.SU_27);
        CruisePartDefinition[] bodies={CruisePartDefinition.BODY_LIGHT,CruisePartDefinition.BODY_CLASSIC,CruisePartDefinition.BODY_HEAVY};
        for(int i=0;i<3;i++) {
            ItemStack stack=missile(bodies[i]);carrier.setInventorySlotContents(i,stack);
            assertEquals(13+i,carrier.getLegacyPayloadCodeAt(i));assertSame(bodies[i],carrier.getCruiseStoreBuild(i).getAirframe());
        }
        for(int i=0;i<9;i++) { carrier.setInventorySlotContents(0,new ItemStack(WarTechContent.MQ9_PAYLOAD,1,i));assertEquals(i+1,carrier.getLegacyPayloadCodeAt(0)); }
        carrier.setInventorySlotContents(0,new ItemStack(WarTechContent.KH555_MISSILE));assertEquals(10,carrier.getLegacyPayloadCodeAt(0));
        carrier.setInventorySlotContents(0,new ItemStack(WarTechContent.STRATEGIC_BOMB,1,0));assertEquals(11,carrier.getLegacyPayloadCodeAt(0));
        carrier.setInventorySlotContents(0,new ItemStack(WarTechContent.STRATEGIC_BOMB,1,1));assertEquals(12,carrier.getLegacyPayloadCodeAt(0));
    }
    @Test public void copiedMissileProgramsAreIndependent() {
        ItemStack source=missile(CruisePartDefinition.BODY_CLASSIC);ItemStack copy=source.copy();
        CruiseMission changed=CruiseMission.fromStack(copy);changed.clear();changed.writeToStack(copy);
        assertEquals(1,CruiseMission.fromStack(source).getRoute().size());assertEquals(0,CruiseMission.fromStack(copy).getRoute().size());
    }
    @Test public void reconClassifiesTheNewEntityAsMissile() throws Exception {
        EntityCustomCruise missile=new EntityCustomCruise(null);missile.configure(missile(CruisePartDefinition.BODY_CLASSIC),null);
        Method classify=EntityCustomUav.class.getDeclaredMethod("reconContactType",net.minecraft.entity.Entity.class);classify.setAccessible(true);
        assertEquals(com.wartec.wartecmod.port.uav.UavReconReport.ReconContact.MISSILE,((Integer)classify.invoke(null,missile)).intValue());
    }
    @Test public void searchCategoryAndTrackPersistButReloadCannotInventVisibility() throws Exception {
        CruiseBuild build=CruiseBuild.starter(CruisePartDefinition.BODY_CLASSIC);build.set(CruiseSlot.SEEKER,CruisePartDefinition.SEEKER_RADAR);
        ItemStack stack=new ItemStack(WarTechContent.ASSEMBLED_CRUISE);build.writeToStack(stack);
        CruiseMission mission=new CruiseMission();mission.setMode(CruiseMission.Mode.SEARCH,build);mission.setCategory(CruiseTargetCategory.AIRCRAFT,build);mission.append(new Vec3d(1000,64,0),0);mission.writeToStack(stack);
        EntityCustomCruise source=new EntityCustomCruise(null);source.configure(stack,null);
        java.lang.reflect.Field tick=EntityCustomCruise.class.getDeclaredField("flightTicks");tick.setAccessible(true);tick.setInt(source,10);
        java.lang.reflect.Field field=EntityCustomCruise.class.getDeclaredField("seekerTrack");field.setAccessible(true);
        CruiseSeeker.Track track=(CruiseSeeker.Track)field.get(source);UUID target=UUID.randomUUID();
        track.observe(target,new Vec3d(1000,100,0),Vec3d.ZERO,5,CruisePartDefinition.NAV_ROUTE);track.observe(target,new Vec3d(1000,100,0),Vec3d.ZERO,10,CruisePartDefinition.NAV_ROUTE);
        NBTTagCompound tag=new NBTTagCompound();call(source,"writeEntityToNBT",new Class<?>[]{NBTTagCompound.class},tag);
        EntityCustomCruise restored=new EntityCustomCruise(null);call(restored,"readEntityFromNBT",new Class<?>[]{NBTTagCompound.class},tag);
        assertEquals(CruiseTargetCategory.AIRCRAFT,CruiseMission.read(tag.getCompoundTag("CruiseMission")).getCategory());
        CruiseSeeker.Track recovered=(CruiseSeeker.Track)field.get(restored);assertEquals(target,recovered.target());assertFalse(recovered.confirmed(10));
        NBTTagCompound again=new NBTTagCompound();call(restored,"writeEntityToNBT",new Class<?>[]{NBTTagCompound.class},again);assertEquals(tag.getCompoundTag("CruiseMission"),again.getCompoundTag("CruiseMission"));
    }
    @Test public void acquiringOrDroppingTargetInvalidatesTheOldSearchWaypointImmediately() throws Exception {
        EntityCustomCruise entity=new EntityCustomCruise(null);entity.configure(missile(CruisePartDefinition.BODY_CLASSIC),null);
        java.lang.reflect.Field field=EntityCustomCruise.class.getDeclaredField("seekerTrack");field.setAccessible(true);CruiseSeeker.Track track=(CruiseSeeker.Track)field.get(entity);
        java.lang.reflect.Field aim=EntityCustomCruise.class.getDeclaredField("navigationAim");aim.setAccessible(true);aim.set(entity,new Vec3d(10,100,10));
        UUID target=UUID.randomUUID();track.observe(target,new Vec3d(100,100,0),Vec3d.ZERO,5,CruisePartDefinition.NAV_ROUTE);track.observe(target,new Vec3d(100,100,0),Vec3d.ZERO,10,CruisePartDefinition.NAV_ROUTE);
        call(entity,"synchroniseSeekerAim",new Class<?>[]{Vec3d.class,CruisePartDefinition.class},new Vec3d(900,64,0),CruisePartDefinition.NAV_ROUTE);assertNull(aim.get(entity));
        aim.set(entity,new Vec3d(20,100,20));track.reset();call(entity,"synchroniseSeekerAim",new Class<?>[]{Vec3d.class,CruisePartDefinition.class},new Vec3d(900,64,0),CruisePartDefinition.NAV_ROUTE);assertNull(aim.get(entity));
    }
}
