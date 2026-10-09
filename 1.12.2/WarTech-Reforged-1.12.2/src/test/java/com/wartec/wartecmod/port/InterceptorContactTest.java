package com.wartec.wartecmod.port;
import com.wartec.wartecmod.port.integration.InterceptorContact;
import net.minecraft.util.math.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class InterceptorContactTest {
    private final AxisAlignedBB body=new AxisAlignedBB(-.5,-.5,-.5,.5,.5,.5);
    @Test public void fastMissileCannotSkipThinBody() {
        assertEquals(new Vec3d(-.5,0,0),InterceptorContact.bodyHit(new Vec3d(-20,0,0),new Vec3d(22,0,0),body));
    }
    @Test public void crossingMovingTargetUsesRelativeSweep() {
        Vec3d from=new Vec3d(-10,0,0).subtract(new Vec3d(0,0,-10));
        Vec3d to=new Vec3d(10,0,0).subtract(new Vec3d(0,0,10));
        assertNotNull(InterceptorContact.bodyHit(from,to,body));
    }
    @Test public void visualNearMissIsNotDirectBodyCollision() {
        assertNull(InterceptorContact.bodyHit(new Vec3d(-20,2,0),new Vec3d(20,2,0),body));
    }
    @Test public void insideBodyContactsAtStart() {
        assertEquals(Vec3d.ZERO,InterceptorContact.bodyHit(Vec3d.ZERO,new Vec3d(20,0,0),body));
    }
    @Test public void firstImpactWinsAndNullsAreSafe() {
        Vec3d near=new Vec3d(2,0,0),far=new Vec3d(4,0,0);
        assertEquals(near,InterceptorContact.nearest(Vec3d.ZERO,far,near));
        assertEquals(near,InterceptorContact.nearest(Vec3d.ZERO,null,near));
        assertNull(InterceptorContact.nearest(Vec3d.ZERO,null,null));
    }
}
