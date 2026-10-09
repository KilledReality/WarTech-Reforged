package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.content.WarTechContent;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gui.ContainerLegacyEntity;
import java.lang.reflect.Method;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.player.InventoryPlayer;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class CruiseAircraftOperationsTest {
    @BeforeClass public static void boot() { Bootstrap.register(); }
    private CruiseBuild build(CruisePartDefinition body) {
        CruiseBuild b=CruiseBuild.starter(body);b.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);return b;
    }
    private ItemStack store(CruisePartDefinition body) {
        ItemStack stack=new ItemStack(WarTechContent.ASSEMBLED_CRUISE);build(body).writeToStack(stack);return stack;
    }
    @Test public void fightersHaveExactlyThreeTwoOneAndNoStrizh() {
        for(WarTechEntityProfile p:new WarTechEntityProfile[]{WarTechEntityProfile.SU_27,WarTechEntityProfile.F_16C}) {
            int i=0;for(CruisePartDefinition body:CruiseAirframes.bodies()) assertEquals(new int[]{3,2,1,0}[i++],CruiseAircraftLoadout.capacity(p,body));
        }
    }
    @Test public void capacityIsEnforcedOnEverySlotNotOnlyInTheTooltip() {
        for(WarTechEntityProfile p:new WarTechEntityProfile[]{WarTechEntityProfile.SU_27,WarTechEntityProfile.F_16C})
            for(CruisePartDefinition body:CruiseAirframes.bodies()) {
                EntityWarTechAircraft aircraft=new EntityWarTechAircraft(null,p);
                for(int i=0;i<6;i++) assertEquals(i<CruiseAircraftLoadout.capacity(p,body),aircraft.isItemValidForSlot(i,store(body)));
            }
    }
    @Test public void fullLightAndNeptuneLoadsFitAndTheirNextSlotDoesNot() {
        for(CruisePartDefinition body:new CruisePartDefinition[]{CruisePartDefinition.BODY_LIGHT,CruisePartDefinition.BODY_CLASSIC,CruisePartDefinition.BODY_HEAVY}) {
            EntityWarTechAircraft a=new EntityWarTechAircraft(null,WarTechEntityProfile.SU_27);int count=CruiseAircraftLoadout.capacity(a.getProfile(),body);
            for(int i=0;i<count;i++) { assertTrue(a.isItemValidForSlot(i,store(body)));a.setInventorySlotContents(i,store(body)); }
            assertEquals(count,a.getCustomCruiseCount());assertFalse(a.isItemValidForSlot(count,store(body)));
        }
    }
    @Test public void fightersDoNotMixClassesOrConventionalStores() {
        EntityWarTechAircraft a=new EntityWarTechAircraft(null,WarTechEntityProfile.F_16C);
        a.setInventorySlotContents(0,store(CruisePartDefinition.BODY_LIGHT));
        assertEquals("cruise.error.aircraft_mixed",a.getCruiseLoadError(1,store(CruisePartDefinition.BODY_CLASSIC)));
        assertFalse(a.isItemValidForSlot(1,new ItemStack(WarTechContent.MQ9_PAYLOAD)));
        a.removeStackFromSlot(0);a.setInventorySlotContents(2,new ItemStack(WarTechContent.MQ9_PAYLOAD));
        assertEquals("cruise.error.aircraft_mixed",a.getCruiseLoadError(0,store(CruisePartDefinition.BODY_LIGHT)));
    }
    @Test public void tu95AcceptsSixHeaviestMissilesAndMixedBodies() {
        EntityWarTechAircraft a=new EntityWarTechAircraft(null,WarTechEntityProfile.TU_95);
        for(int i=0;i<6;i++) { ItemStack s=store(CruisePartDefinition.BODY_LONG_RANGE);assertTrue(a.isItemValidForSlot(i,s));a.setInventorySlotContents(i,s); }
        assertEquals(6,a.getCustomCruiseCount());
        for(int i=0;i<6;i++) assertTrue(a.isItemValidForSlot(i,store(CruiseAirframes.bodies()[i%4])));
    }
    @Test public void loadingCustomMissilesAutomaticallyDisarmsInterceptorModeAndSelectsStrike() {
        EntityWarTechAircraft a=new EntityWarTechAircraft(null,WarTechEntityProfile.SU_27);a.setLegacyFlags(a.getLegacyFlags()|8);
        a.setInventorySlotContents(0,store(CruisePartDefinition.BODY_CLASSIC));
        assertFalse(a.isInterceptorMode());assertEquals(9,a.getLegacySelectedPayload());assertEquals(0,a.getLegacySelectedHardpoint());
    }
    @Test public void emptyMissileProgramInheritsAllThreeTargetCoordinatesWithoutMutatingInventory() {
        ItemStack original=store(CruisePartDefinition.BODY_LIGHT);Vec3d goal=new Vec3d(700,42,-300);
        ItemStack prepared=CruiseAircraftLoadout.prepare(original,goal,2);
        assertEquals(goal,CruiseMission.fromStack(prepared).getTargets().get(0));assertEquals(2,CruiseMission.fromStack(prepared).getDimension());
        assertTrue(CruiseMission.fromStack(original).getTargets().isEmpty());
    }
    @Test public void ownSearchProgramAndCategorySurviveCarrierTargeting() {
        ItemStack s=store(CruisePartDefinition.BODY_CLASSIC);CruiseBuild b=CruiseBuild.fromStack(s);b.set(CruiseSlot.SEEKER,CruisePartDefinition.SEEKER_OPTICAL);b.writeToStack(s);
        CruiseMission m=new CruiseMission();m.setMode(CruiseMission.Mode.SEARCH,b);m.append(new Vec3d(900,50,800),0);m.append(new Vec3d(1100,60,800),0);m.writeToStack(s);
        assertEquals(m.write(),CruiseMission.fromStack(CruiseAircraftLoadout.prepare(s,new Vec3d(40,4,20),0)).write());
    }
    @Test public void corruptProgramsAreNotSilentlyReplaced() {
        ItemStack s=store(CruisePartDefinition.BODY_LIGHT);NBTTagCompound bad=new NBTTagCompound();bad.setInteger("Schema",999);s.getTagCompound().setTag("CruiseMission",bad);
        assertFalse(CruiseMission.fromStack(CruiseAircraftLoadout.prepare(s,new Vec3d(700,50,0),0)).isValidFor(CruiseBuild.fromStack(s),0));
    }
    @Test public void aircraftPreparationUsesItsSelectedTargetAndDoesNotRequireItemProgramming() throws Exception {
        EntityWarTechAircraft a=new EntityWarTechAircraft(null,WarTechEntityProfile.F_16C);a.setInventorySlotContents(0,store(CruisePartDefinition.BODY_LIGHT));
        a.queueTarget(500,64,120,false);Method method=EntityWarTechAircraft.class.getDeclaredMethod("preparedAircraftCruise",int.class);method.setAccessible(true);
        ItemStack prepared=(ItemStack)method.invoke(a,0);assertEquals(new Vec3d(500.5,64.5,120.5),CruiseMission.fromStack(prepared).getTargets().get(0));
    }
    @Test public void carrierFrontAxisCorrectionCancelsNativeRotation() {
        assertEquals(0,-90+CruiseAircraftLoadout.nativeCorrection(WarTechEntityProfile.SU_27),0);
        assertEquals(0,90+CruiseAircraftLoadout.nativeCorrection(WarTechEntityProfile.F_16C),0);
    }
    @Test public void mountsAreSymmetricAndStrizhWingSpansDoNotOverlap() {
        for(WarTechEntityProfile p:new WarTechEntityProfile[]{WarTechEntityProfile.SU_27,WarTechEntityProfile.F_16C}) {
            Vec3d left=CruiseAircraftLoadout.mount(p,build(CruisePartDefinition.BODY_LIGHT),0),right=CruiseAircraftLoadout.mount(p,build(CruisePartDefinition.BODY_LIGHT),1);
            assertEquals(-left.x,right.x,0);assertEquals(left.y,right.y,0);
            Vec3d centre=CruiseAircraftLoadout.mount(p,build(CruisePartDefinition.BODY_LIGHT),2);assertEquals(0,centre.x,0);assertTrue(centre.y<left.y-.35);
            assertEquals(0,CruiseAircraftLoadout.mount(p,build(CruisePartDefinition.BODY_HEAVY),0).x,0);
        }
        for(int i=1;i<6;i++) assertTrue(CruiseAircraftLoadout.mount(WarTechEntityProfile.TU_95,build(CruisePartDefinition.BODY_LONG_RANGE),i).x-
            CruiseAircraftLoadout.mount(WarTechEntityProfile.TU_95,build(CruisePartDefinition.BODY_LONG_RANGE),i-1).x>2*.6561*1.45);
    }
    @Test public void forwardLaunchWindowAllowsAReasonableCloseStrikeButNotPointBlank() {
        CruiseBuild b=build(CruisePartDefinition.BODY_LIGHT);CruiseMission m=new CruiseMission();m.setTarget(new Vec3d(140,64,0),0);
        assertNull(CruiseCarrierRelease.error(b,m,new Vec3d(0,95,0),1000));
        m.setTarget(new Vec3d(40,64,0),0);assertEquals("cruise.error.carrier_too_close",CruiseCarrierRelease.error(b,m,new Vec3d(0,200,0),1000));
    }
    @Test public void launcherHasLongTwinRailsAndTwoAttachedSaddlesForEveryGroundBody() {
        for(CruisePartDefinition body:CruiseAirframes.bodies()) for(CruisePartDefinition launch:new CruisePartDefinition[]{CruisePartDefinition.LAUNCH_BOOSTER,CruisePartDefinition.LAUNCH_RAIL}) {
            CruiseBuild b=build(body);b.set(CruiseSlot.LAUNCH,launch);java.util.List<CruiseLaunchCradle.Beam> beams=CruiseLaunchCradle.beams(b);
            assertEquals(19,beams.size());assertTrue(beams.get(0).a.distanceTo(beams.get(0).b)>2.5);
            for(CruiseLaunchCradle.Beam beam:beams) { assertTrue(Double.isFinite(beam.a.lengthSquared()));assertTrue(beam.a.y>0 && beam.b.y>0);assertTrue(beam.width>0); }
        }
    }
    @Test public void inventoryCoordinatesMatchTheNewSeparatedGuiRows() {
        EntityWarTechAircraft a=new EntityWarTechAircraft(null,WarTechEntityProfile.F_16C);
        ContainerLegacyEntity c=new ContainerLegacyEntity(new InventoryPlayer(null),a,ContainerLegacyEntity.Layout.AIRCRAFT);
        assertEquals(83,c.getSlot(0).yPos);assertEquals(83,c.getSlot(6).yPos);assertEquals(176,c.getSlot(8).yPos);assertEquals(234,c.getSlot(35).yPos);
    }
    @Test public void lowObservableTerrainBuildIsHarderThanASimpleHeavyEnduranceBody() {
        CruiseBuild storm=build(CruisePartDefinition.BODY_HEAVY),strizh=build(CruisePartDefinition.BODY_LONG_RANGE);
        assertEquals(3,CruiseCombatProfile.threatTier(storm));assertEquals(1,CruiseCombatProfile.threatTier(strizh));
        assertEquals("MISSILE_TIER3",CruiseCombatProfile.radarType(storm).name());
        for(int tier=1;tier<=3;tier++) assertTrue(CruiseCombatProfile.interceptChance(tier,3)<CruiseCombatProfile.interceptChance(tier,1));
    }
    /** Generated QA data comes from the SAME geometry functions used by server and renderer. */
    @Test public void exportGeometryForOfflinePreview() throws Exception {
        com.google.gson.JsonObject root=new com.google.gson.JsonObject();com.google.gson.JsonArray launches=new com.google.gson.JsonArray(),carriers=new com.google.gson.JsonArray();
        for(CruisePartDefinition body:CruiseAirframes.bodies()) {
            CruiseBuild b=build(body);b.set(CruiseSlot.LAUNCH,body==CruisePartDefinition.BODY_LIGHT?CruisePartDefinition.LAUNCH_RAIL:CruisePartDefinition.LAUNCH_BOOSTER);
            com.google.gson.JsonObject launch=new com.google.gson.JsonObject();launch.addProperty("body",CruiseVisuals.familyName(body));
            launch.addProperty("scale",CruiseAirframes.modelScale(body));launch.addProperty("pitch",body==CruisePartDefinition.BODY_LIGHT?-12:-65);
            launch.add("origin",vector(CruiseVisuals.launchOrigin(b)));com.google.gson.JsonArray beams=new com.google.gson.JsonArray();
            for(CruiseLaunchCradle.Beam beam:CruiseLaunchCradle.beams(b)) {
                com.google.gson.JsonObject out=new com.google.gson.JsonObject();out.add("a",vector(beam.a));out.add("b",vector(beam.b));out.addProperty("width",beam.width);beams.add(out);
            }
            launch.add("beams",beams);launches.add(launch);
        }
        for(WarTechEntityProfile profile:new WarTechEntityProfile[]{WarTechEntityProfile.SU_27,WarTechEntityProfile.F_16C,WarTechEntityProfile.TU_95})
            for(CruisePartDefinition body:CruiseAirframes.bodies()) {
                int count=CruiseAircraftLoadout.capacity(profile,body);if(count==0) continue;
                com.google.gson.JsonObject carrier=new com.google.gson.JsonObject();carrier.addProperty("profile",profile.name());carrier.addProperty("body",CruiseVisuals.familyName(body));
                carrier.addProperty("scale",CruiseAirframes.modelScale(body));carrier.addProperty("correction",CruiseAircraftLoadout.nativeCorrection(profile));
                com.google.gson.JsonArray slots=new com.google.gson.JsonArray();for(int i=0;i<count;i++) slots.add(vector(CruiseAircraftLoadout.mount(profile,build(body),i)));
                carrier.add("slots",slots);carriers.add(carrier);
            }
        root.add("launches",launches);root.add("carriers",carriers);
        java.nio.file.Path path=java.nio.file.Paths.get("build/qa/cruise-dev60-geometry.json");java.nio.file.Files.createDirectories(path.getParent());
        java.nio.file.Files.write(path,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(4,launches.size());assertEquals(10,carriers.size());
    }
    private static com.google.gson.JsonArray vector(Vec3d v) {
        com.google.gson.JsonArray a=new com.google.gson.JsonArray();a.add(v.x);a.add(v.y);a.add(v.z);return a;
    }
}
