package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.uav.*;
import com.wartec.wartecmod.port.integration.DesignatorCompat;
import java.util.*;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.Vec3d;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/** Regression cases taken from user screenshots. Not a live GPU/client test. */
public class CruiseDev58Test {
    @BeforeClass public static void bootstrap() { Bootstrap.register(); }
    @Test public void appendedRackHasARecipeAndFuturePartsCannotBeSilentlyMissing() throws Exception {
        String source=new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(
                "src/main/java/com/wartec/wartecmod/port/integration/WarTechRecipeRegistration.java")),
                java.nio.charset.StandardCharsets.UTF_8);
        String method=source.substring(source.indexOf("private static void registerUavModuleRecipes"));
        String rows=method.substring(method.indexOf("Object[][] ingredients"),method.indexOf("};"));
        java.util.regex.Matcher matcher=java.util.regex.Pattern.compile("(?m)^\\s*\\{[^}]+\\}").matcher(rows);
        int count=0;while(matcher.find()) count++;
        assertEquals(UavPartDefinition.values().length,count);
        assertTrue(method.contains("ingredients.length != definitions.length"));
    }
    @Test public void allFourBodiesAreFirstWithoutChangingMetadata() {
        NonNullList<ItemStack> items=NonNullList.create();
        WarTechContent.CRUISE_MODULE.getSubItems(WarTechContent.CRUISE_MODULE.getCreativeTab(),items);
        assertEquals(44,items.size());
        CruisePartDefinition[] bodies=CruiseAirframes.bodies();
        for(int i=0;i<4;i++) assertSame(bodies[i],CruisePartDefinition.byMetadata(items.get(i).getMetadata()));
        int previous=-1;Set<Integer> unique=new HashSet<>();
        for(ItemStack stack:items) {
            int slot=CruisePartDefinition.byMetadata(stack.getMetadata()).getSlot().ordinal();
            assertTrue(slot>=previous);previous=slot;assertTrue(unique.add(stack.getMetadata()));
        }
        assertEquals(32,items.get(3).getMetadata());
    }
    @Test public void uavModulesAlsoStayGroupedWithSingleRackVisible() {
        NonNullList<ItemStack> items=NonNullList.create();
        WarTechContent.UAV_MODULE.getSubItems(WarTechContent.UAV_MODULE.getCreativeTab(),items);
        assertEquals(28,items.size());int previous=-1;boolean single=false;
        for(ItemStack stack:items) {
            UavPartDefinition part=UavPartDefinition.byMetadata(stack.getMetadata());
            assertTrue(part.getSlot().ordinal()>=previous);previous=part.getSlot().ordinal();
            if(part==UavPartDefinition.RACK_CRUISE) single=true;
        }
        assertTrue(single);assertEquals(27,UavPartDefinition.RACK_CRUISE.ordinal());
    }
    private ItemStack xz(String x,String z) {
        ItemStack stack=new ItemStack(WarTechContent.ITEM_TARGET_FINDER);
        NBTTagCompound tag=new NBTTagCompound();tag.setDouble(x,-769);tag.setDouble(z,738);stack.setTagCompound(tag);return stack;
    }
    @Test public void xzDesignatorsAcceptUserAltitudeAndSaveIt() {
        for(String[] keys:new String[][]{{"xCoord","yCoord","zCoord"},{"x","y","z"},{"targetX","targetY","targetZ"}}) {
            ItemStack stack=xz(keys[0],keys[2]);
            assertEquals(new Vec3d(-769,4,738),DesignatorCompat.getSavedTarget(null,stack,4));
            assertEquals(4,stack.getTagCompound().getDouble(keys[1]),0);
            assertNotNull(DesignatorCompat.getSavedTarget(stack));
        }
    }
    @Test public void unresolvedRemoteHeightNeverLoadsAChunkOrAssumesSeaLevel() {
        assertNull(DesignatorCompat.getSavedTarget(null,xz("xCoord","zCoord"),Double.NaN));
        assertNull(DesignatorCompat.getSavedTarget(null,xz("xCoord","zCoord"),256));
        assertNull(DesignatorCompat.getSavedTarget(null,xz("xCoord","zCoord"),-1));
    }
    @Test public void newXZCannotReuseAltitudeResolvedForOldTarget() {
        ItemStack stack=xz("xCoord","zCoord");DesignatorCompat.getSavedTarget(null,stack,4);
        stack.getTagCompound().setInteger("xCoord",1200);
        assertNull(DesignatorCompat.getSavedTarget(stack));
        assertNull(DesignatorCompat.getSavedTarget(null,stack,Double.NaN));
        assertEquals(new Vec3d(1200,80,738),DesignatorCompat.getSavedTarget(null,stack,80));
    }
    @Test public void explicitlyStoredYWorksWithoutWorld() {
        ItemStack stack=xz("xCoord","zCoord");stack.getTagCompound().setDouble("yCoord",17);
        assertEquals(17,DesignatorCompat.getSavedTarget(null,stack,200).y,0);
    }
    @Test public void lightScreenshotBuildIsNoLongerStrategicRange() {
        CruiseBuild b=CruiseBuild.starter(CruisePartDefinition.BODY_LIGHT);
        b.set(CruiseSlot.FUEL,CruisePartDefinition.FUEL_STANDARD);b.set(CruiseSlot.NAVIGATION,CruisePartDefinition.NAV_ROUTE);
        b.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);b.set(CruiseSlot.WINGS,CruisePartDefinition.WINGS_RANGE);
        assertTrue(b.calculateStats().isValid());assertTrue(b.calculateStats().getRange()>900);
        assertTrue(b.calculateStats().getRange()<1800);
    }
    @Test public void everyClassHasItsOwnCeilingAndFuelStillMatters() {
        for(CruisePartDefinition body:CruiseAirframes.bodies()) {
            CruiseBuild b=CruiseBuild.starter(body);
            assertTrue(b.calculateStats().getRange()>500);
            assertTrue(b.calculateStats().getRange()<CruiseAirframes.maximumRange(body));
            if(body!=CruisePartDefinition.BODY_LONG_RANGE) {
                int before=b.calculateStats().getRange();b.set(CruiseSlot.FUEL,CruisePartDefinition.FUEL_STANDARD);
                b.set(CruiseSlot.WARHEAD,CruisePartDefinition.WARHEAD_THERMOBARIC);
                assertTrue(b.calculateStats().getRange()<before || body==CruisePartDefinition.BODY_LIGHT);
            }
        }
    }
    @Test public void singleRackFitsReusableFramesAndRejectsKamikaze() {
        assertTrue(UavPartDefinition.RACK_CRUISE.isCompatible(UavAirframe.RECON));
        assertTrue(UavPartDefinition.RACK_CRUISE.isCompatible(UavAirframe.STRIKE));
        assertFalse(UavPartDefinition.RACK_CRUISE.isCompatible(UavAirframe.ONE_WAY));
        UavBuild carrier=UavBuild.starter(UavAirframe.RECON).set(UavSlot.PAYLOAD,UavPartDefinition.RACK_CRUISE);
        carrier.set(UavSlot.ENERGY,UavPartDefinition.FUEL_COMPACT);
        CruiseBuild b=CruiseBuild.starter(CruisePartDefinition.BODY_LIGHT);b.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);
        assertEquals(1,carrier.calculateStats().getHardpoints());assertNull(UavCruiseCarriage.error(carrier,b));
        for(UavAirframe frame:new UavAirframe[]{UavAirframe.RECON,UavAirframe.STRIKE})
            assertNull(UavCruiseCarriage.error(UavBuild.cruiseCarrier(frame),b));
    }
    @Test public void massRejectionIncludesFourConcreteNumbers() {
        UavBuild carrier=UavBuild.starter(UavAirframe.RECON).set(UavSlot.PAYLOAD,UavPartDefinition.RACK_CRUISE);
        CruiseBuild b=CruiseBuild.starter(CruisePartDefinition.BODY_LIGHT);b.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);
        b.set(CruiseSlot.FUEL,CruisePartDefinition.FUEL_STANDARD);b.set(CruiseSlot.WARHEAD,CruisePartDefinition.WARHEAD_THERMOBARIC);
        assertEquals("cruise.error.uav_mass",UavCruiseCarriage.error(carrier,b));
        Object[] args=UavCruiseCarriage.errorArguments(carrier,b,"cruise.error.uav_mass");
        assertEquals(4,args.length);assertTrue((int)args[0]>(int)args[1]);assertTrue((int)args[2]>(int)args[3]);
    }
    @Test public void higherAltitudeAndLargerTurnRadiusStartDescentEarlier() {
        Vec3d target=new Vec3d(1000,4,0);
        assertTrue(CruiseTerminalApproach.entryDistance(new Vec3d(0,110,0),target,1,2)
            >CruiseTerminalApproach.entryDistance(new Vec3d(0,32,0),target,1,2));
        assertTrue(CruiseTerminalApproach.entryDistance(new Vec3d(0,64,0),target,1.5,1)
            >CruiseTerminalApproach.entryDistance(new Vec3d(0,64,0),target,.5,3));
    }
    @Test public void terminalControllerReducesSpeedForLargeHeadingError() {
        Vec3d delta=new Vec3d(15,-12,0);
        double limit=CruiseTerminalApproach.speedLimit(1.5,delta,90,-30,1);
        assertTrue(limit>=1.5*.55 && limit<1.5);
    }
    @Test public void flatWorldDescentDoesNotAddAFortyBlockMissToAimPoint() {
        for(CruisePartDefinition body:CruiseAirframes.bodies()) {
            CruiseBuild b=CruiseBuild.starter(body);CruiseStats stats=b.calculateStats();
            for(double altitude:new double[]{32,64,110}) for(float initialPitch:new float[]{0,-65}) {
                Vec3d target=new Vec3d(0,4,0);
                double entry=CruiseTerminalApproach.entryDistance(new Vec3d(0,altitude,0),target,stats.getSpeed(),stats.getTurnRate());
                Vec3d pos=new Vec3d(0,altitude,-entry);float yaw=0,pitch=initialPitch;double speed=stats.getSpeed();
                double best=Double.POSITIVE_INFINITY;
                for(int tick=0;tick<2400;tick++) {
                    Vec3d delta=target.subtract(pos);
                    yaw=CruiseFlightMath.turn(yaw,CruiseFlightMath.yaw(delta),stats.getTurnRate());
                    pitch=CruiseFlightMath.turn(pitch,CruiseFlightMath.pitch(delta),stats.getTurnRate());
                    double top=CruiseTerminalApproach.speedLimit(stats.getSpeed(),delta,yaw,pitch,stats.getTurnRate());
                    speed=speed>top?Math.max(top,speed-.08):Math.min(top,speed+.02);
                    Vec3d velocity=CruiseFlightMath.direction(yaw,pitch).scale(speed);
                    Vec3d next=pos.add(velocity);best=Math.min(best,next.distanceTo(target));
                    if(next.y<=4) break;
                    pos=next;
                }
                assertTrue(body+" alt="+altitude+" pitch="+initialPitch+" error="+best,best<1.5);
            }
        }
    }
}
