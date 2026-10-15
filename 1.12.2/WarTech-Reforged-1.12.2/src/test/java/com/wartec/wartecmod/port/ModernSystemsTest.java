package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.content.*;
import java.util.UUID;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import org.junit.*;
import static org.junit.Assert.*;

public class ModernSystemsTest {
    @BeforeClass public static void bootstrap() { Bootstrap.register(); }
    @Test public void stowedPackagesIgnoreSavedTraverseForEveryMlrs() {
        for(ModernVehicleProfile profile:ModernVehicleProfile.values()) if(profile.isMlrs()) {
            AnimatedAssetScene scene=ModernLaunchGeometry.scene(profile);
            AnimatedAssetScene.Pose home=ModernLaunchGeometry.pose(scene,profile,0,0,-1,0,0,0,0);
            for(float yaw:new float[]{-179,-90,49,120,180}) {
                AnimatedAssetScene.Pose stowed=ModernLaunchGeometry.pose(scene,profile,0,yaw,-1,0,0,0,0);
                for(int slot=0;slot<profile.slots;slot++) {
                    int socket=ModernLaunchGeometry.find(scene,String.format("LaunchSocket_%02d",slot+1));
                    assertArrayEquals(profile+" stowed socket "+slot,home.worldMatrix(socket),stowed.worldMatrix(socket),.00001F);
                }
            }
        }
    }
    @Test public void fp7AcceleratesContinuouslyFromItsLauncherAndSurvivesReconstruction() {
        ModernFlightPath path=new ModernFlightPath(ModernMissileProfile.FP7,Vec3d.ZERO,new Vec3d(0,0,6000),new Vec3d(0,1,0));
        ModernFlightPath loaded=new ModernFlightPath(ModernMissileProfile.FP7,Vec3d.ZERO,new Vec3d(0,0,6000),new Vec3d(0,1,0));
        assertTrue(path.point(1).lengthVector()<.25);double previous=0;
        for(int tick=1;tick<=90;tick++) {
            double speed=path.point(tick).distanceTo(path.point(tick-1));
            assertTrue(speed<=3.02);assertTrue(Math.abs(speed-previous)<.25);previous=speed;
            assertEquals(path.point(tick),loaded.point(tick));
        }
        assertTrue(previous>2.98);
    }
    @Test public void allModernArcsRemainContinuousAtMinimumAndMaximumRange() {
        for(ModernMissileProfile profile:ModernMissileProfile.values()) for(double range:new double[]{profile.minimumRange,profile.commandRange}) {
            Vec3d start=new Vec3d(-8192,8,-8192),target=start.addVector(range,-4,0);
            ModernFlightPath path=new ModernFlightPath(profile,start,target,new Vec3d(0,.8,.6));
            assertEquals(start,path.point(0));assertTrue(path.point(path.duration).distanceTo(target)<1e-6);
            Vec3d previous=start;
            for(int tick=1;tick<=path.duration;tick++) {
                Vec3d point=path.point(tick);assertTrue(Double.isFinite(point.y));assertTrue(point.y>=target.y-1e-6);
                assertTrue(profile+" tick "+tick,point.distanceTo(previous)<=profile.speed+.02);previous=point;
            }
            ModernFlightPath restored=new ModernFlightPath(profile,start,target,new Vec3d(0,.8,.6));
            assertEquals(path.duration,restored.duration);assertEquals(path.point(path.duration/2),restored.point(path.duration/2));
        }
    }
    @Test public void accuracyIsStableAndCannotExtendCommandRange() {
        Vec3d start=new Vec3d(-1000,4,1000);UUID id=new UUID(87,77);
        for(ModernMissileProfile profile:ModernMissileProfile.values()) {
            Vec3d target=start.addVector(profile.commandRange,0,0);
            Vec3d point=ModernAccuracy.aim(profile,start,target,id,false);
            assertEquals(point,ModernAccuracy.aim(profile,start,target,id,false));
            assertTrue(Math.hypot(point.x-start.x,point.z-start.z)<=profile.commandRange+1e-6);
            assertEquals(target,ModernAccuracy.aim(profile,start,target,id,true));
            assertFalse(profile.accepts(profile.minimumRange-.1));assertFalse(profile.accepts(profile.commandRange+.1));
        }
    }
    @Test public void eachSourceSlotHasForwardNoseAndRealIndependentCoordinates() {
        for(ModernVehicleProfile profile:ModernVehicleProfile.values()) {
            AnimatedAssetScene scene=ModernLaunchGeometry.scene(profile);
            AnimatedAssetScene.Pose pose=ModernLaunchGeometry.pose(scene,profile,1,0,-1,0,0,0,0);
            for(int slot=0;slot<profile.slots;slot++) {
                int node=ModernLaunchGeometry.find(scene,String.format(profile.isMlrs()?"LaunchSocket_%02d":"Missile_%02d",slot+1));
                float[] matrix=pose.worldMatrix(node);
                Vec3d axis=new Vec3d(matrix[4],matrix[5],matrix[6]).normalize();
                assertTrue(profile+" elevates its actual projectile",axis.y>.3 && axis.z>.3);
                if(profile.isMlrs()) {
                    float[] scale=ModernLaunchGeometry.rocketScale(profile,slot);
                    assertTrue(scale[0]>0 && scale[1]>scale[0] && scale[2]>0);
                    // Loaded tail + half length equals flying centre; both noses meet the socket.
                    Vec3d mouth=new Vec3d(matrix[12],matrix[13],matrix[14]).scale(profile.scale);
                    Vec3d centre=mouth.subtract(axis.scale(scale[1]*.5));
                    assertTrue(centre.add(axis.scale(scale[1]*.5)).distanceTo(mouth)<1e-6);
                } else {
                    float[] curve=ModernLaunchGeometry.exitCurve(profile,slot),unfold=ModernLaunchGeometry.unfoldCurve(profile,slot);
                    assertEquals(curve.length,unfold.length);assertTrue(curve[curve.length-1]>6);
                    assertEquals(0,curve[0],1e-5);assertEquals(0,unfold[0],1e-5);assertEquals(1,unfold[unfold.length-1],1e-5);
                    for(int tick=1;tick<curve.length;tick++) assertTrue(curve[tick]>=curve[tick-1]);
                }
            }
        }
    }
    @Test public void steeringDoesNotRotateWheelPositionOrOtherAxles() {
        for(ModernVehicleProfile profile:ModernVehicleProfile.values()) {
            AnimatedAssetScene scene=ModernLaunchGeometry.scene(profile);
            AnimatedAssetScene.Pose rest=ModernLaunchGeometry.pose(scene,profile,0,0,-1,0,0,0,0);
            AnimatedAssetScene.Pose rolling=ModernLaunchGeometry.pose(scene,profile,0,0,-1,0,0,5,.6);
            for(int node=0;node<scene.nodes.length;node++) if(scene.nodes[node].name.startsWith("Wheel") && scene.nodes[node].name.endsWith("_Steer")) {
                float[] a=rest.worldMatrix(node),b=rolling.worldMatrix(node);
                assertEquals(a[12],b[12],1e-5);assertEquals(a[13],b[13],1e-5);assertEquals(a[14],b[14],1e-5);
            }
        }
    }
    @Test public void factoryBanderolKeepsItsMissionWhenMovingBetweenLaunchSystems() {
        ItemStack missile=new ItemStack(WarTechContent.S8000_MISSILE);
        CruiseMission mission=new CruiseMission();mission.setTarget(new Vec3d(4000,4,100),0);mission.writeToStack(missile);
        CruiseBuild booster=CruiseBuild.banderol();booster.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_BOOSTER);booster.writeToStack(missile);
        CruiseBuild store=CruiseBuild.fromStack(missile);
        assertTrue(store.isS8000());assertEquals(CruisePartDefinition.LAUNCH_AIR,store.get(CruiseSlot.LAUNCH));
        assertEquals(CruiseMission.fromStack(missile).getTargets().get(0),new Vec3d(4000,4,100));
        assertEquals(CruisePartDefinition.LAUNCH_BOOSTER,CruiseBuild.read(booster.write()).get(CruiseSlot.LAUNCH));
        NBTTagCompound forged=booster.write();forged.setString("WARHEAD",CruisePartDefinition.WARHEAD_EMP.getId());
        assertEquals(CruiseBuild.banderol().get(CruiseSlot.WARHEAD),CruiseBuild.read(forged).get(CruiseSlot.WARHEAD));
    }
}
