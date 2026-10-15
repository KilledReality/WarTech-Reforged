package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.entity.*;
import net.minecraft.util.math.Vec3d;
import org.junit.Test;
import static org.junit.Assert.*;

public class LegacySamGeometryTest {
    private static Vec3d point(float[] m,float y) { return new Vec3d(m[12]+m[4]*y,m[13]+m[5]*y,m[14]+m[6]*y); }
    @Test public void s400ContainerBottomsReachGroundWithoutCrossingItDuringFolding() throws Exception {
        AnimatedAssetScene scene=LegacySamGeometry.scene(false);
        int packet=ModernLaunchGeometry.find(scene,"Package");
        java.util.Set<String> groups=new java.util.HashSet<>();
        for(AnimatedAssetScene.Part part:scene.meshes[scene.nodes[packet].mesh]) groups.add(part.group);
        java.util.List<float[]> vertices=new java.util.ArrayList<>();
        try(java.io.BufferedReader reader=new java.io.BufferedReader(new java.io.InputStreamReader(
                getClass().getResourceAsStream("/assets/wartecmod/models/modern_animated/s400_rig.obj"),java.nio.charset.StandardCharsets.UTF_8))) {
            boolean selected=false;String line;
            while((line=reader.readLine())!=null) {
                if(line.startsWith("g ")) selected=groups.contains(line.substring(2));
                if(selected&&line.startsWith("v ")) {
                    String[] p=line.split("\\s+");vertices.add(new float[]{Float.parseFloat(p[1]),Float.parseFloat(p[2]),Float.parseFloat(p[3])});
                }
            }
        }
        assertFalse(vertices.isEmpty());
        double[] bottoms={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY};
        for(int tick=0;tick<=120;tick++) {
            float[] m=LegacySamGeometry.pose(scene,false,tick/120F,0,-1,0).worldMatrix(packet);
            for(float[] p:vertices) {
                double x=m[0]*p[0]+m[4]*p[1]+m[8]*p[2]+m[12];
                double y=m[1]*p[0]+m[5]*p[1]+m[9]*p[2]+m[13];
                double z=m[2]*p[0]+m[6]*p[1]+m[10]*p[2]+m[14];
                assertTrue("packet below ground at tick "+tick,y>=-.01);
                if(tick==0) assertTrue("transport packet intersects bed height",y>1.48);
                if(tick==120) {int slot=(z>6.4?2:0)+(x>0?1:0);bottoms[slot]=Math.min(bottoms[slot],y);}
            }
        }
        for(double bottom:bottoms) assertEquals("each container rests at ground level",0,bottom,.01);
    }
    @Test public void s400TransportContainersRestCloseToTheBedInsteadOfHovering() {
        for(int slot=0;slot<2;slot++) {
            LegacySamGeometry.Shot socket=LegacySamGeometry.shot(false,0,0,slot,Vec3d.ZERO);
            double underside=socket.mouth.y-.4264;
            assertTrue("lower row must sit on the low bed cradle",underside>=1.60&&underside<=1.72);
            assertEquals("transport tubes must be horizontal",0,socket.direction.y,1e-5);
            assertTrue(socket.direction.z<-.999);
        }
    }
    @Test public void fourDifferentChannelsReleaseWithNoseAtMouth() {
        for(boolean patriot:new boolean[]{true,false}) {
            java.util.Set<Vec3d> mouths=new java.util.HashSet<>();
            for(int slot=0;slot<4;slot++) {
                LegacySamGeometry.Shot s=LegacySamGeometry.shot(patriot,1,patriot?95:0,slot,new Vec3d(-100,64,300));
                assertTrue(mouths.add(s.mouth));assertEquals(1,s.direction.lengthVector(),1e-6);
                assertTrue(s.direction.y>(patriot?.90:.999));
                assertTrue(s.origin.add(s.direction.scale(s.scale[1]*.5)).distanceTo(s.mouth)<1e-5);
                assertTrue(s.scale[0]*.272F<(patriot?.55:.78));
            }
        }
    }
    @Test public void telescopesStayAttachedThroughoutTheLiftAndTraverse() {
        for(boolean patriot:new boolean[]{true,false}) {
            AnimatedAssetScene scene=LegacySamGeometry.scene(patriot);
            for(int tick=0;tick<=120;tick+=2) for(float yaw:new float[]{-179,0,179}) {
                AnimatedAssetScene.Pose pose=LegacySamGeometry.pose(scene,patriot,tick/120F,yaw,-1,0);
                for(String side:new String[]{"L","R"}) {
                    Vec3d base=point(pose.worldMatrix(ModernLaunchGeometry.find(scene,"LiftBase_"+side)),0);
                    Vec3d anchor=point(pose.worldMatrix(ModernLaunchGeometry.find(scene,"LiftAnchor_"+side)),0);
                    float[] barrel=pose.worldMatrix(ModernLaunchGeometry.find(scene,"LiftBarrel_"+side));
                    float[] rod=pose.worldMatrix(ModernLaunchGeometry.find(scene,"LiftRod_"+side));
                    assertTrue("barrel pivot",point(barrel,0).distanceTo(base)<1e-4);
                    assertTrue("rod pin",point(rod,1).distanceTo(anchor)<1e-4);
                    assertTrue("rod overlap",point(barrel,1).distanceTo(base)>point(rod,0).distanceTo(base));
                }
                assertArrayEquals(scene.pose().worldMatrix(1),pose.worldMatrix(1),0);
            }
        }
    }
    @Test public void lidHingeDoesNotTranslateAndOnlySelectedLidMoves() {
        for(boolean patriot:new boolean[]{true,false}) {
            AnimatedAssetScene scene=LegacySamGeometry.scene(patriot);
            AnimatedAssetScene.Pose closed=LegacySamGeometry.pose(scene,patriot,1,53,-1,0);
            for(int slot=0;slot<4;slot++) for(int step=0;step<=12;step++) {
                AnimatedAssetScene.Pose opened=LegacySamGeometry.pose(scene,patriot,1,53,slot,step/12F);
                for(int lid=0;lid<4;lid++) {
                    int index=ModernLaunchGeometry.find(scene,"CapPivot_"+lid);
                    assertEquals(point(closed.worldMatrix(index),0),point(opened.worldMatrix(index),0));
                    if(lid!=slot) assertArrayEquals(closed.rotation[index],opened.rotation[index],0);
                }
            }
        }
    }
    @Test public void azimuthCrossesTheAngleSeamByTheShortPath() {
        assertEquals(-179,LegacySamGeometry.approach(179,-179,2),0);
        assertEquals(179,LegacySamGeometry.approach(-179,179,2),0);
        assertEquals(-90,LegacySamGeometry.desiredYaw(new Vec3d(100,0,0),Vec3d.ZERO),0);
        assertEquals(90,LegacySamGeometry.desiredYaw(new Vec3d(-100,0,0),Vec3d.ZERO),0);
    }
    @Test public void departureBoundsTurnAndAccelerationEvenForOppositeTargets() {
        for(Vec3d launch:new Vec3d[]{new Vec3d(0,1.2,0),new Vec3d(.6,1.0392304845,0)})
        for(Vec3d aim:new Vec3d[]{new Vec3d(15.5,0,0),new Vec3d(0,-15.5,0),new Vec3d(-15.5,0,0)}) {
            Vec3d current=launch;
            for(int tick=0;tick<16;tick++) {
                Vec3d next=LegacySamGeometry.departureVelocity(current,aim);
                double dot=current.normalize().dotProduct(next.normalize());
                // 1.12.2 Vec3d length/normalize passes through float sqrt.
                assertTrue("bounded 12 degree turn",Math.acos(Math.max(-1,Math.min(1,dot)))<=Math.toRadians(12.001));
                assertTrue("bounded acceleration",Math.abs(Math.sqrt(next.dotProduct(next))-Math.sqrt(current.dotProduct(current)))<=1.800001);
                current=next;
            }
            assertTrue(current.distanceTo(aim)<1e-5);
        }
    }
}
