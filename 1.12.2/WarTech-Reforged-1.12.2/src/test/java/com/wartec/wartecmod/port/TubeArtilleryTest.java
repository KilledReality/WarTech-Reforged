package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.entity.*;
import net.minecraft.util.math.Vec3d;
import org.junit.Test;
import static org.junit.Assert.*;

public class TubeArtilleryTest {
    @Test public void trajectoryHitsActualHeightWithoutGuidance(){
        for(TubeArtilleryProfile p:TubeArtilleryProfile.values())for(int distance:new int[]{p.minimumRange,300,p.range})for(int height:new int[]{-40,0,100}){
            Vec3d start=new Vec3d(10,70,-25),target=start.addVector(distance,height,distance*.01),v=TubeArtilleryBallistics.solve(start,target,p.speed);
            assertNotNull(p+" "+distance+" "+height,v);assertTrue(v.lengthVector()<=p.speed+1e-6);
            double ticks=(target.x-start.x)/v.x;
            assertEquals(target.y,start.y+v.y*ticks-TubeArtilleryBallistics.GRAVITY*ticks*(ticks-1)/2,1e-6);
        }
    }
    @Test public void boundsAndUnreachableHeights(){
        for(TubeArtilleryProfile p:TubeArtilleryProfile.values()){
            assertFalse(p.accepts(p.minimumRange-.01));assertTrue(p.accepts(p.minimumRange));assertTrue(p.accepts(p.range));assertFalse(p.accepts(p.range+.01));assertFalse(p.accepts(Double.NaN));
        }
        assertNull(TubeArtilleryBallistics.solve(Vec3d.ZERO,new Vec3d(100,10000,0),10));
        assertNull(TubeArtilleryBallistics.solve(Vec3d.ZERO,new Vec3d(Double.NaN,0,0),10));
    }
    @Test public void muzzleFollowsGunAndRecoilReturnsToBindPose(){
        for(TubeArtilleryProfile p:TubeArtilleryProfile.values()){
            AnimatedAssetScene s=TubeArtilleryGeometry.scene(p);int node=ModernLaunchGeometry.find(s,"MuzzleSocket");
            for(float yaw:new float[]{-180,-90,0,90,179})for(float pitch:new float[]{0,20,45,70}){
                Vec3d[] a=TubeArtilleryGeometry.muzzle(p,0,0,0,0,yaw,pitch),b=TubeArtilleryGeometry.muzzle(p,0,0,0,90,yaw,pitch);
                assertEquals(1,a[1].lengthVector(),1e-6);assertEquals(a[0].x,b[0].z,1e-5);assertEquals(-a[0].z,b[0].x,1e-5);
                float[] bind=TubeArtilleryGeometry.pose(p,yaw,pitch,-1,0).worldMatrix(node);
                float[] end=TubeArtilleryGeometry.pose(p,yaw,pitch,1.6F,0).worldMatrix(node);
                for(int i=0;i<16;i++)assertEquals(bind[i],end[i],1e-5);
            }
        }
    }
    @Test public void profileOrdinalsStayAppended(){assertEquals(29,WarTechEntityProfile.ARTILLERY_D30.ordinal());assertEquals(30,WarTechEntityProfile.ARTILLERY_CAESAR.ordinal());}
}
