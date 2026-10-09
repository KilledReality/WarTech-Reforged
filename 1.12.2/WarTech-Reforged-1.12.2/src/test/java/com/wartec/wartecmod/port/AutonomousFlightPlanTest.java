package com.wartec.wartecmod.port;
import com.wartec.wartecmod.port.entity.AutonomousFlightPlan;
import com.wartec.wartecmod.port.cruise.*;
import java.util.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class AutonomousFlightPlanTest {
    private static final Vec3d GOAL=new Vec3d(0,40,2000),ORIGIN=new Vec3d(0,40,0);
    private static Vec3d aim(AutonomousFlightPlan p,int tier,UUID id,Vec3d at,double available) {
        return p.aim(tier,id,at,GOAL,1,2,available,240,160);
    }
    @Test public void basicControllerRemainsStraightAndLocalAirReleasePredictionIsUnchanged() {
        AutonomousFlightPlan p=new AutonomousFlightPlan();UUID id=new UUID(1,2);
        for(int z=0;z<2000;z+=40) assertEquals(GOAL,aim(p,1,id,new Vec3d(0,40,z),5000));
        for(int tier=2;tier<=3;tier++) {
            p.reset();assertEquals(GOAL,aim(p,tier,id,ORIGIN,5000));
            assertEquals(GOAL,aim(p,tier,id,new Vec3d(0,40,128),5000));
        }
    }
    @Test public void salvoHasStableIndividualLanesRatherThanIdenticalPathsOrRandomTickJitter() {
        Set<Integer> lanes=new HashSet<>();boolean left=false,right=false;
        for(int i=0;i<32;i++) {
            UUID id=new UUID(6768,i);AutonomousFlightPlan p=new AutonomousFlightPlan();aim(p,3,id,ORIGIN,5000);
            Vec3d at=new Vec3d(0,40,700),a=aim(p,3,id,at,5000),b=aim(p,3,id,at,5000);
            assertEquals(a,b);lanes.add((int)a.x);left|=a.x<0;right|=a.x>0;
            assertTrue(Math.abs(a.x)<=104);assertTrue(a.z>at.z);assertEquals(40,a.y,0);
        }
        assertTrue(lanes.size()>12);assertTrue(left&&right);
    }
    @Test public void tierAndFuelReserveReallyLimitRouteVariationAndTerminalEndsIt() {
        UUID id=new UUID(1,2);AutonomousFlightPlan a=new AutonomousFlightPlan(),b=new AutonomousFlightPlan();
        aim(a,2,id,ORIGIN,5000);aim(b,3,id,ORIGIN,5000);
        Vec3d at=new Vec3d(0,40,700);
        assertTrue(Math.abs(aim(b,3,id,at,5000).x)>Math.abs(aim(a,2,id,at,5000).x));
        assertEquals(GOAL,aim(b,3,id,at,at.distanceTo(GOAL)+20));
        assertEquals(GOAL,aim(b,3,id,new Vec3d(0,40,1800),5000));
    }
    @Test public void routeSurvivesSaveAndOldOrCorruptNbtIsSafe() {
        UUID id=new UUID(7,8);AutonomousFlightPlan a=new AutonomousFlightPlan();aim(a,3,id,ORIGIN,5000);
        AutonomousFlightPlan b=new AutonomousFlightPlan();b.read(a.write());Vec3d at=new Vec3d(0,40,700);
        assertEquals(aim(a,3,id,at,5000),aim(b,3,id,at,5000));
        NBTTagCompound n=a.write();n.setDouble("OriginX",Double.NaN);b.read(n);assertTrue(b.write().hasNoTags());
        b.read(new NBTTagCompound());assertTrue(b.write().hasNoTags());
    }
    @Test public void routeChangesSmoothlyAndNeverRequiresBackwardsMarch() {
        UUID id=new UUID(2,3);AutonomousFlightPlan p=new AutonomousFlightPlan();aim(p,3,id,ORIGIN,5000);
        Vec3d last=null;
        for(int z=161;z<1750;z++) {
            Vec3d at=new Vec3d(0,40,z),next=aim(p,3,id,at,5000);
            assertTrue(next.z>z);if(last!=null) assertTrue(Math.abs(next.x-last.x)<2);last=next;
        }
    }
    @Test public void surveyCarrotUsesRealPositionAndAchievableTierDependentPattern() {
        UUID id=new UUID(9,10);Vec3d center=new Vec3d(0,50,0),at=new Vec3d(60,50,0);
        Vec3d p=AutonomousFlightPlan.observation(2,id,center,at,54,.8,1.2);
        Vec3d c=AutonomousFlightPlan.observation(3,id,center,at,54,.8,1.2);
        assertEquals(p,AutonomousFlightPlan.observation(2,id,center,at,54,.8,1.2));
        assertEquals(50,c.y,0);assertTrue(Math.abs(c.x)>Math.abs(p.x));assertTrue(Math.abs(c.z)>1);
        Vec3d heavy=AutonomousFlightPlan.observation(3,id,center,at,20,1.5,.5);
        assertTrue(heavy.distanceTo(center)>150);
    }
    @Test public void preferredLaneCannotBypassLocalObstructionOrRayBudget() {
        final AxisAlignedBB wall=new AxisAlignedBB(-30,0,30,30,120,80);final int[] calls={0,0};
        CruiseNavigation.Environment e=new CruiseNavigation.Environment() {
            public boolean clear(Vec3d a,Vec3d b) { calls[0]++;return !wall.contains(a)&&wall.calculateIntercept(a,b)==null; }
            public double height(double x,double z,double f) { calls[1]++;return 4; }
        };
        Vec3d from=new Vec3d(0,40,0),goal=new Vec3d(0,40,500),preferred=new Vec3d(12,40,70);
        Vec3d next=CruiseNavigation.corridorAim(CruisePartDefinition.NAV_TERRAIN,from,goal,preferred,40,e,new CruiseNavigation.State(),0,1,2);
        assertTrue(e.clear(from,next));assertTrue(calls[0]<=CruiseNavigation.MAX_RAYS+1);assertTrue(calls[1]<=CruiseNavigation.MAX_HEIGHTS);
    }
    @Test public void normalTankRouteHasRoomForVisibleBendWithoutInvadingTerminalOrDropWindow() {
        CruiseStats stats=CruiseBuild.starter(CruisePartDefinition.BODY_HEAVY).calculateStats();
        double length=stats.getRange()*.65;Vec3d goal=new Vec3d(0,4,length);
        for(int i=2;i<6;i++) {
            AutonomousFlightPlan p=new AutonomousFlightPlan();UUID id=new UUID(6868,i);
            p.aim(3,id,new Vec3d(0,70,0),goal,stats.getSpeed(),stats.getTurnRate(),stats.getRange(),300,160);
            double peak=0;
            for(int z=160;z<length-300;z+=4) {
                Vec3d at=new Vec3d(0,40,z),next=p.aim(3,id,at,goal,stats.getSpeed(),stats.getTurnRate(),stats.getRange()-z,300,160);
                peak=Math.max(peak,Math.abs(next.x));
            }
            assertTrue("Intermediate carrot hid the short-leg route",peak>8);
            assertEquals(goal,p.aim(3,id,new Vec3d(0,40,length-250),goal,1,2,1000,300,160));
        }
    }
    @Test public void reusableUavRequestedAltitudeDoesNotAccumulateEachPlannerStep() {
        CruiseNavigation.Environment e=new CruiseNavigation.Environment() {
            public boolean clear(Vec3d a,Vec3d b) { return true; }
            public double height(double x,double z,double f) { return 4; }
        };
        CruiseNavigation.State s=new CruiseNavigation.State();Vec3d p=new Vec3d(0,40,0),goal=new Vec3d(0,40,1200);
        for(int i=0;i<15;i++) {
            Vec3d next=CruiseNavigation.corridorAim(CruisePartDefinition.NAV_ROUTE,p,goal,goal,40,e,s,i*30,1,2);
            assertEquals(40,next.y,0);p=next;
        }
    }
}
