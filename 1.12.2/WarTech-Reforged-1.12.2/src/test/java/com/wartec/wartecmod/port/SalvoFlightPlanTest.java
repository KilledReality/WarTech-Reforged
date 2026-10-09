package com.wartec.wartecmod.port;

import com.wartec.wartecmod.port.entity.SalvoFlightPlan;
import com.wartec.wartecmod.port.cruise.*;
import java.util.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SalvoFlightPlanTest {
    private static final Vec3d ORIGIN=new Vec3d(0,40,0),GOAL=new Vec3d(0,4,2000);
    private static final UUID OWNER=new UUID(69,1);
    private static UUID id(int i) { return new UUID(69,i+100); }
    private static Vec3d aim(SalvoFlightPlan p,SalvoFlightPlan.Directory d,int i,long tick,Vec3d position,double fuel) {
        return p.aim(d,tick,id(i),OWNER,"team",position,GOAL,GOAL,1,2,fuel,260,160);
    }
    @Test public void sixteenNearbySameOrAdjacentTargetsReserveUniqueStableLanes() {
        SalvoFlightPlan.Directory d=new SalvoFlightPlan.Directory();Set<Integer> lanes=new HashSet<>();
        for(int i=0;i<16;i++) {
            Vec3d goal=GOAL.addVector(i*4,0,i*2);
            SalvoFlightPlan.Assignment a=d.claim(100+i*10,id(i),OWNER,"team",ORIGIN.addVector(i,0,0),goal,-1);
            assertNotNull(a);assertTrue(lanes.add(a.lane));assertEquals(i>0,a.active);
            assertEquals(a.lane,d.claim(101+i*10,id(i),OWNER,"team",ORIGIN.addVector(i,0,0),goal,-1).lane);
        }
        assertEquals(16,d.size());
    }
    @Test public void neverSharesUnknownHostileDistantOppositeOrSeparateCohorts() {
        SalvoFlightPlan.Directory d=new SalvoFlightPlan.Directory();d.claim(0,id(0),OWNER,"blue",ORIGIN,GOAL,-1);
        assertNull(d.claim(0,id(1),null,"",ORIGIN,GOAL,-1));
        assertFalse(d.claim(0,id(2),new UUID(3,4),"red",ORIGIN,GOAL,-1).active);
        assertFalse(d.claim(0,id(3),OWNER,"blue",ORIGIN,GOAL.addVector(1000,0,0),-1).active);
        assertFalse(d.claim(0,id(4),OWNER,"blue",GOAL,ORIGIN,-1).active);
        assertFalse(d.claim(0,id(5),OWNER,"blue",ORIGIN.addVector(1000,0,0),GOAL,-1).active);
        assertTrue(d.claim(10,id(6),new UUID(3,4),"blue",ORIGIN,GOAL,-1).active);
        for(int t=100;t<=600;t+=100) d.claim(t,id(0),OWNER,"blue",ORIGIN,GOAL,-1);
        assertFalse(d.claim(601,id(7),OWNER,"blue",ORIGIN,GOAL,-1).active);
    }
    @Test public void boundedDirectoryExpiresWithoutWorldOrEntityReferencesAndOverflowIsSafe() {
        SalvoFlightPlan.Directory d=new SalvoFlightPlan.Directory();
        for(int i=0;i<80;i++) d.claim(0,id(i),new UUID(10,i),"",ORIGIN,GOAL,-1);
        assertEquals(SalvoFlightPlan.MAX_MEMBERS,d.size());
        assertNotNull(d.claim(201,id(100),OWNER,"team",ORIGIN,GOAL,-1));assertEquals(1,d.size());
        d=new SalvoFlightPlan.Directory();
        for(int i=0;i<80;i++) d.claim(0,id(i),OWNER,"team",ORIGIN,GOAL,-1);
        assertEquals(SalvoFlightPlan.LANES,d.size());
        assertNotNull(d.claim(10,id(0),OWNER,"team",ORIGIN,GOAL,-1));
    }
    @Test public void soloDoesNotPretendToBeACooperativeSalvoAndReleaseAndStrikeGatesRemainUntouched() {
        SalvoFlightPlan p=new SalvoFlightPlan();SalvoFlightPlan.Directory d=new SalvoFlightPlan.Directory();
        assertEquals(GOAL,aim(p,d,0,0,ORIGIN,5000));
        assertEquals(GOAL,aim(p,d,0,20,new Vec3d(0,40,500),5000));
        d.claim(20,id(1),OWNER,"team",ORIGIN,GOAL,-1);
        for(int t=40;t<=120;t+=20) aim(p,d,0,t,new Vec3d(0,40,128),5000);
        assertEquals(GOAL,aim(p,d,0,140,new Vec3d(0,40,128),5000));
        assertEquals(GOAL,aim(p,d,0,160,new Vec3d(0,40,1800),5000));
    }
    @Test public void lanesStayDistinctThroughIngressRatherThanMergeEarlyAtAdjacentTargets() {
        SalvoFlightPlan.Directory d=new SalvoFlightPlan.Directory();SalvoFlightPlan[] plans=new SalvoFlightPlan[5];
        for(int i=0;i<plans.length;i++) { plans[i]=new SalvoFlightPlan();aim(plans[i],d,i,0,ORIGIN,5000); }
        for(int t=20;t<=100;t+=20) for(int i=0;i<plans.length;i++) aim(plans[i],d,i,t,new Vec3d(0,40,300),5000);
        Set<Integer> xs=new HashSet<>();double min=1e9,max=-1e9;
        for(int i=0;i<plans.length;i++) {
            Vec3d next=aim(plans[i],d,i,120,new Vec3d(0,40,1500),5000);
            assertTrue(xs.add((int)next.x));min=Math.min(min,next.x);max=Math.max(max,next.x);
            assertTrue(next.z>1500);assertEquals(4,next.y,0);
        }
        assertTrue(max-min>100);
    }
    @Test public void fuelReserveSmoothHandoverAndShortLegFallbackAreReal() {
        SalvoFlightPlan.Directory d=new SalvoFlightPlan.Directory();SalvoFlightPlan p=new SalvoFlightPlan();
        d.claim(0,id(0),OWNER,"team",ORIGIN,GOAL,-1);aim(p,d,1,0,ORIGIN,5000);
        Vec3d prev=GOAL;
        for(int t=10;t<=120;t+=10) {
            Vec3d next=aim(p,d,1,t,new Vec3d(0,40,800),5000);
            assertTrue(Math.abs(next.x-prev.x)<60);assertTrue(next.z>800);prev=next;
        }
        Vec3d low=aim(p,d,1,130,new Vec3d(0,40,800),1200);
        assertEquals(0,low.x,0);
        Vec3d goal=new Vec3d(0,4,400);
        assertEquals(goal,p.aim(d,140,id(1),OWNER,"team",ORIGIN,goal,goal,1,2,5000,260,160));
    }
    @Test public void routeSaveRestoresLaneAndFadeAndCorruptOrOldDataFallsBackSafely() {
        SalvoFlightPlan.Directory d=new SalvoFlightPlan.Directory();SalvoFlightPlan p=new SalvoFlightPlan();
        d.claim(0,id(0),OWNER,"team",ORIGIN,GOAL,-1);aim(p,d,1,0,ORIGIN,5000);
        for(int t=20;t<=100;t+=20) aim(p,d,1,t,new Vec3d(0,40,500),5000);
        SalvoFlightPlan restored=new SalvoFlightPlan();restored.read(p.write());
        Vec3d at=new Vec3d(0,40,1000);
        assertEquals(aim(p,d,1,120,at,5000),aim(restored,new SalvoFlightPlan.Directory(),1,120,at,5000));
        NBTTagCompound n=p.write();n.setInteger("Lane",10000);restored.read(n);assertTrue(restored.write().hasNoTags());
        n=p.write();n.setDouble("AxisX",Double.NaN);restored.read(n);assertTrue(restored.write().hasNoTags());
        restored.read(new NBTTagCompound());assertTrue(restored.write().hasNoTags());
    }
    @Test public void reservedLaneStillPassesTheBoundedObstaclePlanner() {
        final AxisAlignedBB wall=new AxisAlignedBB(-100,0,40,100,150,100);final int[] count={0};
        CruiseNavigation.Environment e=new CruiseNavigation.Environment() {
            public boolean clear(Vec3d a,Vec3d b) { count[0]++;return !wall.contains(a)&&wall.calculateIntercept(a,b)==null; }
            public double height(double x,double z,double f) { return 4; }
        };
        Vec3d next=CruiseNavigation.corridorAim(CruisePartDefinition.NAV_TERRAIN,ORIGIN,GOAL,new Vec3d(80,40,130),40,e,new CruiseNavigation.State(),0,1,2);
        assertTrue(e.clear(ORIGIN,next));assertTrue(count[0]<=CruiseNavigation.MAX_RAYS+1);
    }
    @Test public void cohortAnchorDoesNotWalkAcrossMapOrExtendLaunchWindowThroughLateMembers() {
        SalvoFlightPlan.Directory d=new SalvoFlightPlan.Directory();
        d.claim(0,id(0),OWNER,"team",ORIGIN,GOAL,-1);
        d.claim(10,id(1),OWNER,"team",ORIGIN,GOAL.addVector(128,0,0),-1);
        assertFalse(d.claim(20,id(2),OWNER,"team",ORIGIN,GOAL.addVector(256,0,0),-1).active);
        for(int t=100;t<=500;t+=100) d.claim(t,id(1),OWNER,"team",ORIGIN,GOAL.addVector(128,0,0),-1);
        d.claim(590,id(3),OWNER,"team",ORIGIN,GOAL,-1);
        assertFalse(d.claim(610,id(4),OWNER,"team",ORIGIN,GOAL,-1).active);
    }
    @Test public void groupUsesProgrammedGoalsNotRandomCepOffsetsOrChangingCruiseAltitude() {
        SalvoFlightPlan.Directory d=new SalvoFlightPlan.Directory();SalvoFlightPlan a=new SalvoFlightPlan(),b=new SalvoFlightPlan();
        Vec3d g=GOAL.addVector(192,0,0),miss=GOAL.addVector(210,0,0);
        a.aim(d,0,id(0),OWNER,"team",ORIGIN,GOAL,GOAL,GOAL,1,2,5000,260,160);
        b.aim(d,0,id(1),OWNER,"team",ORIGIN,miss,g,g,1,2,5000,260,160);
        assertTrue(b.write().getBoolean("Active"));
        int slot=b.write().getInteger("Lane");
        b.aim(d,20,id(1),OWNER,"team",new Vec3d(0,40,300),miss.addVector(0,30,0),g.addVector(0,30,0),g,1,2,5000,260,160);
        assertEquals(slot,b.write().getInteger("Lane"));assertEquals(2,d.size());
    }
}
