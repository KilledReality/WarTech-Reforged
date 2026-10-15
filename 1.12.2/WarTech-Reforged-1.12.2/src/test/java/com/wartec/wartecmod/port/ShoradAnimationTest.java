package com.wartec.wartecmod.port;
import com.wartec.wartecmod.port.entity.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import org.junit.Test;
import static org.junit.Assert.*;

public class ShoradAnimationTest {
 @Test public void departureLocksPoseAndStowClosesBeforeMoving() {
  ShoradAnimation a=new ShoradAnimation();a.deployment=120;a.slot=3;
  for(int i=0;i<80;i++) if(a.tick(true,90,45,true,false))break;
  assertEquals(12,a.cap);float yaw=a.yaw,pitch=a.pitch;
  a.released(4);
  for(int i=0;i<6;i++) {assertFalse(a.tick(false,-90,0,false,false));assertEquals(yaw,a.yaw,0);assertEquals(pitch,a.pitch,0);assertEquals(12,a.cap);}
  for(int i=0;i<12;i++) {a.tick(false,0,0,false,false);assertEquals(yaw,a.yaw,0);assertEquals(120,a.deployment);}
  int guard=0;while(a.blocksDriving()&&guard++<400)a.tick(false,0,0,false,false);
  assertFalse(a.blocksDriving());assertEquals(0,a.yaw,0);assertEquals(0,a.pitch,0);assertEquals(0,a.deployment);
 }
 @Test public void noLaunchWhileDeployingOrBeforeAlignmentAndHatch() {
  ShoradAnimation a=new ShoradAnimation();a.slot=0;
  for(int i=0;i<120;i++) assertFalse(a.tick(true,110,50,true,false));
  assertEquals(120,a.deployment);assertEquals(0,a.cap);
  int elapsed=0;while(!a.tick(true,110,50,true,false)&&elapsed++<200)assertTrue(a.cap<12||Math.abs(a.yaw-110)<1);
  assertTrue(elapsed>=20 && elapsed<=30);assertEquals(12,a.cap);assertTrue(Math.abs(a.yaw-110)<=1);
 }
 @Test public void canceledRequestClosesHatchWithoutMovingItOrConsumingARelease() {
  ShoradAnimation a=new ShoradAnimation();a.deployment=120;a.slot=6;a.yaw=50;a.cap=7;
  for(int i=0;i<7;i++) {assertFalse(a.tick(true,-50,60,false,true));assertEquals(50,a.yaw,0);}
  assertEquals(0,a.cap);assertEquals(0,a.hold);
 }
 @Test public void stateRestoresMidMotionAndSanitizesInvalidNumbers() {
  ShoradAnimation a=new ShoradAnimation();a.deployment=73;a.cap=4;a.slot=5;a.yaw=-130;a.pitch=40;a.hold=3;
  ShoradAnimation b=new ShoradAnimation();b.read(a.write(),true);assertEquals(a.write(),b.write());
  NBTTagCompound t=a.write();t.setFloat("Yaw",Float.NaN);t.setInteger("Deployment",900);t.setInteger("Cap",-20);b.read(t,false);
  assertEquals(0,b.yaw,0);assertEquals(120,b.deployment);assertEquals(0,b.cap);
  b.read(new NBTTagCompound(),true);assertEquals(120,b.deployment);b.read(new NBTTagCompound(),false);assertEquals(0,b.deployment);
 }
 @Test public void everySlotHasAUniqueMouthAndNoseFitsItsOpeningOnEveryCourse() {
  for(boolean tor:new boolean[]{true,false}) for(float yaw:new float[]{0,90,179,-179}) for(float course:new float[]{0,90,180,270}) {
   java.util.Set<Vec3d> points=new java.util.HashSet<>();
   for(int slot=0;slot<(tor?8:12);slot++) {
    LegacySamGeometry.Shot s=ShoradGeometry.shot(tor,1,yaw,tor?0:45,slot,new Vec3d(2000,64,-3000),course,0);
    assertTrue(points.add(s.mouth));assertEquals(1,s.direction.lengthVector(),1e-6);
    assertTrue(s.origin.add(s.direction.scale(s.scale[1]*.5)).distanceTo(s.mouth)<1e-5);
    assertTrue(s.scale[0]*.087857<.14*VehicleDimensions.scale(WarTechEntityProfile.MOBILE_AIR_DEFENSE));
    if(tor)assertEquals(1,s.direction.y,1e-6);else assertTrue(s.direction.y>.70);
   }
  }
 }
 @Test public void roofsRotateOnFixedHingesAndOnlyRequestedCoverMoves() {
  for(boolean tor:new boolean[]{true,false}) {
   AnimatedAssetScene scene=ShoradGeometry.scene(tor);
   AnimatedAssetScene.Pose rest=ShoradGeometry.pose(scene,tor,1,90,45,0,-1,0,0,0);
   for(int slot=0;slot<(tor?8:12);slot++) for(int step=0;step<=12;step++) {
    AnimatedAssetScene.Pose p=ShoradGeometry.pose(scene,tor,1,90,45,0,slot,step/12F,0,0);
    for(int i=0;i<scene.nodes.length;i++) if(scene.nodes[i].name.startsWith(tor?"Hatch_":"Cap_")) {
     assertArrayEquals(rest.translation[i],p.translation[i],0);
     String selected=(tor?"Hatch_":"Cap_")+(tor?slot/4:slot);
     if(!scene.nodes[i].name.equals(selected))assertArrayEquals(rest.rotation[i],p.rotation[i],0);
    }
   }
  }
 }
}
