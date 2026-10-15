package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.entity.*;
import net.minecraft.util.math.Vec3d;
import org.junit.Test;
import static org.junit.Assert.*;

public class DefenseExitTest {
    @Test public void everyBukAndOsaCellAcceleratesAndRetainsItsSourceClearance() {
        for(ModernVehicleProfile p:new ModernVehicleProfile[]{ModernVehicleProfile.BUK_M3,ModernVehicleProfile.OSA})for(int slot=0;slot<6;slot++) {
            float[] curve=ModernLaunchGeometry.exitCurve(p,slot),fins=ModernLaunchGeometry.unfoldCurve(p,slot);
            assertEquals(p==ModernVehicleProfile.BUK_M3?9:7,curve.length);assertEquals(curve.length,fins.length);
            assertEquals(0,curve[0],.00001);double previous=0;
            for(int i=1;i<curve.length;i++) {
                double speed=curve[i]-curve[i-1];assertTrue(speed>.35);assertTrue(speed>=previous-.0001);assertTrue(speed<2.5);previous=speed;
                assertTrue(Float.isFinite(fins[i]));assertTrue(fins[i]>=-.00001 && fins[i]<=1.00001);
            }
            AnimatedAssetScene s=ModernLaunchGeometry.scene(p);
            for(AnimatedAssetScene.Track t:s.clips.get(String.format(java.util.Locale.ROOT,"Launch_%02d",slot+1)))
                if("translation".equals(t.path) && s.nodes[t.node].name.startsWith("MissilePivot_")) {
                    float[] a=t.values[0],b=t.values[t.values.length-1];
                    assertEquals(new Vec3d(b[0]-a[0],b[1]-a[1],b[2]-a[2]).lengthVector()*p.scale,curve[curve.length-1],.00001);
                }
        }
    }
    @Test public void capForecastHandlesOldSavedPosesAndOsaHasNoCapDelay() {
        assertEquals(6,ModernLaunchGeometry.capDelay(ModernVehicleProfile.BUK_M3,0));
        assertEquals(1,ModernLaunchGeometry.capDelay(ModernVehicleProfile.BUK_M3,29));
        assertEquals(0,ModernLaunchGeometry.capDelay(ModernVehicleProfile.BUK_M3,30));
        for(int pose=0;pose<=30;pose++)assertEquals(0,ModernLaunchGeometry.capDelay(ModernVehicleProfile.OSA,pose));
    }
    @Test public void guidedHandoffFromRetimedExitIsContinuousEvenForOppositeTarget() {
        for(ModernVehicleProfile p:new ModernVehicleProfile[]{ModernVehicleProfile.BUK_M3,ModernVehicleProfile.OSA}) {
            float[] c=ModernLaunchGeometry.exitCurve(p,0);Vec3d velocity=new Vec3d(0,c[c.length-1]-c[c.length-2],0);
            for(int i=0;i<16;i++) {
                Vec3d next=LegacySamGeometry.departureVelocity(velocity,new Vec3d(0,-16,0));
                assertTrue(Math.abs(next.lengthVector()-velocity.lengthVector())<=1.80001);
                assertTrue(Math.acos(Math.max(-1,Math.min(1,next.normalize().dotProduct(velocity.normalize()))))<=Math.toRadians(12.001));velocity=next;
            }
        }
    }
}
