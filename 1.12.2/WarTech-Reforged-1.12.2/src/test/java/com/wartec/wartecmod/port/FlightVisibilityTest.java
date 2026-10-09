package com.wartec.wartecmod.port;
import com.wartec.wartecmod.port.integration.FlightVisibility;
import com.wartec.wartecmod.port.cruise.CruiseNavigation;
import com.wartec.wartecmod.port.cruise.CruiseTerminalApproach;
import com.wartec.wartecmod.port.cruise.CruisePartDefinition;
import net.minecraft.util.math.Vec3d;
import org.junit.Test;
import static org.junit.Assert.*;
public class FlightVisibilityTest {
    private CruiseNavigation.Environment corridor(final double wall,final int lastChunk) {
        return new CruiseNavigation.Environment() {
            public boolean clear(Vec3d a,Vec3d b) {
                Vec3d known=FlightVisibility.knownEnd(a,b,(x,z)->x>=-1 && x<=lastChunk && z>=-1 && z<=1);
                return known!=null && a.distanceTo(known)+.02>=Math.min(24,a.distanceTo(b))
                    && !(a.x<=wall && known.x>=wall);
            }
            public double height(double x,double z,double fallback) { return 4; }
        };
    }
    @Test public void longFlightCanBeginDescentBeforeTheWholeTerminalProbeIsLoaded() {
        Vec3d from=new Vec3d(8,95,8),target=new Vec3d(300,4,8);
        assertFalse(CruiseNavigation.loadedRay(from,from.addVector(92,-28,0),(x,z)->x<=2));
        for(CruisePartDefinition nav:new CruisePartDefinition[]{CruisePartDefinition.NAV_ROUTE,CruisePartDefinition.NAV_TERRAIN})
            assertTrue(CruiseTerminalApproach.directAllowed(nav,from,target,corridor(Double.POSITIVE_INFINITY,2)));
    }
    @Test public void knownWallStillBlocksRollingTerminalApproach() {
        Vec3d from=new Vec3d(8,95,8),target=new Vec3d(300,4,8);
        assertFalse(CruiseTerminalApproach.directAllowed(CruisePartDefinition.NAV_TERRAIN,from,target,corridor(28,2)));
    }
    @Test public void TooShortKnownTerminalCorridorCannotAuthorizeDescent() {
        assertFalse(CruiseTerminalApproach.directAllowed(CruisePartDefinition.NAV_TERRAIN,
            new Vec3d(8,95,8),new Vec3d(300,4,8),corridor(Double.POSITIVE_INFINITY,0)));
    }
    @Test public void fullyLoadedRayIsNotChanged() {
        Vec3d end=new Vec3d(120,20,80);assertEquals(end,FlightVisibility.knownEnd(new Vec3d(0,64,0),end,(x,z)->true));
    }
    @Test public void unknownAheadClipsBeforeBoundaryInsteadOfInventingWall() {
        Vec3d from=new Vec3d(8,96,8),to=new Vec3d(100,64,8);
        Vec3d known=FlightVisibility.knownEnd(from,to,(x,z)->x<=1);
        assertNotNull(known);assertTrue(known.x<32 && known.x>31.98);
        assertTrue(CruiseNavigation.loadedRay(from,known,(x,z)->x<=1));
    }
    @Test public void negativeDirectionAndDiagonalCornersAreSafe() {
        for(double dx:new double[]{-1,1}) for(double dz:new double[]{-1,1}) {
            Vec3d from=new Vec3d(.1,64,.1),to=from.addVector(dx*100,0,dz*100);
            java.util.function.BiPredicate<Integer,Integer> loaded=(x,z)->x>=-1 && x<=1 && z>=-1 && z<=1;
            Vec3d known=FlightVisibility.knownEnd(from,to,loaded);assertNotNull(known);
            assertTrue(CruiseNavigation.loadedRay(from,known,loaded));
        }
    }
    @Test public void unknownOriginNeverAllowsAnUnsafeTrace() {
        assertNull(FlightVisibility.knownEnd(new Vec3d(0,64,0),new Vec3d(100,64,0),(x,z)->false));
    }
    @Test public void oversizeOrCorruptProbeDoesNotBypassLoadedGuard() {
        assertNull(FlightVisibility.knownEnd(new Vec3d(0,64,0),new Vec3d(600,64,0),(x,z)->true));
        assertNull(FlightVisibility.knownEnd(new Vec3d(0,64,0),new Vec3d(Double.NaN,64,0),(x,z)->true));
    }
    @Test public void advancedNavigatorChoosesForwardNotEndlessClimbAtUnknownHorizon() {
        Vec3d from=new Vec3d(8,96,8),target=new Vec3d(3600,4,8);
        java.util.function.BiPredicate<Integer,Integer> loaded=(x,z)->x>=-1 && x<=1 && z>=-1 && z<=1;
        CruiseNavigation.Environment environment=new CruiseNavigation.Environment() {
            public boolean clear(Vec3d a,Vec3d b) {
                Vec3d known=FlightVisibility.knownEnd(a,b,loaded);
                return known!=null && a.distanceTo(known)+.02>=Math.min(12,a.distanceTo(b));
            }
            public double height(double x,double z,double fallback) { return loaded.test((int)Math.floor(x/16),(int)Math.floor(z/16))?4:fallback; }
        };
        for(com.wartec.wartecmod.port.cruise.CruisePartDefinition nav:new com.wartec.wartecmod.port.cruise.CruisePartDefinition[]{
                com.wartec.wartecmod.port.cruise.CruisePartDefinition.NAV_ROUTE,com.wartec.wartecmod.port.cruise.CruisePartDefinition.NAV_TERRAIN}) {
            Vec3d aim=CruiseNavigation.aim(nav,from,target,environment);
            assertTrue("Must advance through known clear corridor",aim.x>from.x+16);
            assertTrue("Unknown chunks are not a wall",aim.y<from.y+1);
        }
    }
}
