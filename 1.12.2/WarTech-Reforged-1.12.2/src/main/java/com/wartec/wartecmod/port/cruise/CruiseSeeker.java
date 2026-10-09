package com.wartec.wartecmod.port.cruise;

import java.util.UUID;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Deterministic fictional game tracking, not a physical guidance model. */
public final class CruiseSeeker {
    public static final int SCAN_INTERVAL=5,MAX_CANDIDATES=64,MAX_SCANNED=256,MAX_VISIBILITY=8;
    private CruiseSeeker() { }
    public static double area(CruisePartDefinition brain) { return brain==CruisePartDefinition.NAV_TERRAIN?80:brain==CruisePartDefinition.NAV_ROUTE?56:32; }
    public static int searchDuration(CruisePartDefinition brain) { return brain==CruisePartDefinition.NAV_TERRAIN?1200:brain==CruisePartDefinition.NAV_ROUTE?800:480; }
    public static int memory(CruisePartDefinition brain) { return brain==CruisePartDefinition.NAV_TERRAIN?50:brain==CruisePartDefinition.NAV_ROUTE?35:20; }
    public static double range(CruisePartDefinition seeker,boolean daylight,boolean raining,double noise) {
        if(seeker==null || seeker==CruisePartDefinition.SEEKER_NONE) return 0;
        double range=seeker.getPrimary();
        if(seeker==CruisePartDefinition.SEEKER_OPTICAL) return range*(daylight?1:.65)*(raining?.75:1);
        if(seeker==CruisePartDefinition.SEEKER_RADAR) {
            if(!Double.isFinite(noise) || noise>.55) return 0;
            return range*(1-Math.max(0,noise)*.5);
        }
        return range;
    }
    public static Vec3d searchDirection(Vec3d forward,Vec3d towardArea) {
        // Limited game sensor pan lets an orbit inspect its centre, never behind the missile.
        return forward.normalize().add(towardArea.normalize().scale(.75)).normalize();
    }
    public static boolean inView(Vec3d forward,Vec3d offset,CruisePartDefinition seeker) {
        if(offset.lengthSquared()<16) return true;
        double half=seeker==CruisePartDefinition.SEEKER_OPTICAL?55:seeker==CruisePartDefinition.SEEKER_THERMAL?70:85;
        return forward.normalize().dotProduct(offset.normalize())>=Math.cos(Math.toRadians(half));
    }
    public static Vec3d lead(Vec3d position,Vec3d velocity,Vec3d missile,double speed,CruisePartDefinition brain) {
        if(!Double.isFinite(velocity.lengthSquared())) velocity=Vec3d.ZERO;
        double max=brain==CruisePartDefinition.NAV_TERRAIN?20:brain==CruisePartDefinition.NAV_ROUTE?12:5;
        double ticks=Math.min(max,missile.distanceTo(position)/Math.max(.2,speed));
        Vec3d change=velocity.scale(ticks);if(change.lengthSquared()>24*24) change=change.normalize().scale(24);
        return position.add(change);
    }
    public static double orbitRadius(double area,double speed,double turnRate) {
        return MathHelper.clamp(Math.max(area*.8,speed/Math.toRadians(Math.max(.2,turnRate))*1.35),24,120);
    }
    public static double searchSpeed(double maximum,double area,double turnRate) {
        return Math.min(maximum,Math.max(.2,area*.8*Math.toRadians(Math.max(.2,turnRate))/1.35));
    }
    public static Vec3d orbit(Vec3d center,Vec3d position,double radius,int direction) {
        double angle=Math.atan2(position.z-center.z,position.x-center.x)+(direction<0?-.65:.65);
        return center.addVector(Math.cos(angle)*radius,0,Math.sin(angle)*radius);
    }
    public static final class Track {
        private UUID target,pending;
        private Vec3d position,velocity=Vec3d.ZERO;
        private int lastSeen,pendingTick,pendingScans;
        private boolean visible;
        public UUID target() { return target; }
        public boolean confirmed(int tick) { return visible && target!=null && tick-lastSeen<=SCAN_INTERVAL; }
        public void reset() { target=pending=null;position=null;velocity=Vec3d.ZERO;lastSeen=pendingTick=pendingScans=0;visible=false; }
        public void observe(UUID id,Vec3d p,Vec3d v,int tick,CruisePartDefinition brain) {
            if(!id.equals(target)) {
                if(id.equals(pending) && tick-pendingTick<=SCAN_INTERVAL*2) pendingScans++;
                else { pending=id;pendingScans=1; }
                pendingTick=tick;
                if(pendingScans<(brain==CruisePartDefinition.NAV_COORDINATE?3:2)) return;
                target=id;pending=null;pendingScans=0;
            }
            position=p;velocity=!Double.isFinite(v.lengthSquared())?Vec3d.ZERO:v.lengthSquared()>16?v.normalize().scale(4):v;lastSeen=tick;visible=true;
        }
        public void missed(int tick,CruisePartDefinition brain) {
            visible=false;
            pending=null;pendingScans=0;
            if(target!=null && tick-lastSeen>=memory(brain)) reset();
        }
        public Vec3d aim(Vec3d missile,double speed,int tick,CruisePartDefinition brain) {
            if(target==null || position==null) return null;
            return confirmed(tick)?lead(position,velocity,missile,speed,brain):position;
        }
        public NBTTagCompound write() {
            NBTTagCompound n=new NBTTagCompound();
            if(target!=null && position!=null) {
                n.setUniqueId("Target",target);n.setInteger("LastSeen",lastSeen);
                n.setDouble("X",position.x);n.setDouble("Y",position.y);n.setDouble("Z",position.z);
                n.setDouble("VX",velocity.x);n.setDouble("VY",velocity.y);n.setDouble("VZ",velocity.z);
            }
            return n;
        }
        public void read(NBTTagCompound n,int tick) {
            reset();if(!n.hasUniqueId("Target")) return;
            Vec3d p=new Vec3d(n.getDouble("X"),n.getDouble("Y"),n.getDouble("Z"));
            Vec3d v=new Vec3d(n.getDouble("VX"),n.getDouble("VY"),n.getDouble("VZ"));
            if(!Double.isFinite(p.x)||!Double.isFinite(p.y)||!Double.isFinite(p.z)||!Double.isFinite(v.lengthSquared())
                    || Math.abs(p.x)>29999984 || Math.abs(p.z)>29999984 || p.y<0 || p.y>256 || v.lengthSquared()>16) return;
            target=n.getUniqueId("Target");position=p;velocity=v;
            lastSeen=MathHelper.clamp(n.getInteger("LastSeen"),tick-61,tick-SCAN_INTERVAL-1);
        }
    }
}
