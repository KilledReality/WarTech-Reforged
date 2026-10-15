package com.wartec.wartecmod.port.entity;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Oriented rectangular hull vs Minecraft's block boxes; includes rear/front overhang. */
public final class VehicleCollisionHull {
    private VehicleCollisionHull() { }
    public static AxisAlignedBB bounds(VehicleChassisProfile p,double x,double y,double z,float yaw,double height) {
        double a=Math.toRadians(yaw+p.frontYaw),c=Math.abs(Math.cos(a)),s=Math.abs(Math.sin(a));
        double rx=c*p.width/2+s*p.length/2,rz=s*p.width/2+c*p.length/2;
        return new AxisAlignedBB(x-rx,y+.06,z-rz,x+rx,y+height-.03,z+rz);
    }
    public static boolean overlaps(VehicleChassisProfile p,double x,double y,double z,float yaw,double height,AxisAlignedBB b) {
        if(b.maxY<=y+.06 || b.minY>=y+height-.03)return false;
        double a=Math.toRadians(yaw+p.frontYaw),c=Math.cos(a),s=Math.sin(a);
        double dx=(b.minX+b.maxX)/2-x,dz=(b.minZ+b.maxZ)/2-z;
        double bx=(b.maxX-b.minX)/2,bz=(b.maxZ-b.minZ)/2,w=p.width/2,l=p.length/2;
        return Math.abs(dx)<bx+Math.abs(c)*w+Math.abs(s)*l-.005
            && Math.abs(dz)<bz+Math.abs(s)*w+Math.abs(c)*l-.005
            && Math.abs(dx*c+dz*s)<w+Math.abs(c)*bx+Math.abs(s)*bz-.005
            && Math.abs(-dx*s+dz*c)<l+Math.abs(s)*bx+Math.abs(c)*bz-.005;
    }
    public static boolean clear(EntityWarTechGroundVehicle vehicle,double x,double y,double z,float yaw) {
        World world=vehicle.world;VehicleChassisProfile p=VehicleChassisProfile.of(vehicle);
        AxisAlignedBB broad=bounds(p,x,y,z,yaw,vehicle.height);
        if(broad.minY<0 || broad.maxY>256 || !world.isAreaLoaded(new BlockPos(broad.minX,broad.minY,broad.minZ),
                new BlockPos(broad.maxX,broad.maxY,broad.maxZ))) return false;
        for(AxisAlignedBB box:world.getCollisionBoxes(null,broad))
            if(overlaps(p,x,y,z,yaw,vehicle.height,box))return false;
        for(Entity other:world.getEntitiesWithinAABBExcludingEntity(vehicle,broad.grow(11,0,11))) {
            if(!(other instanceof EntityWarTechGroundVehicle)||other.isDead||vehicle.isRidingSameEntity(other))continue;
            EntityWarTechGroundVehicle ground=(EntityWarTechGroundVehicle)other;
            if(!VehicleChassisProfile.isDrivable(ground.getProfile())) {
                if(overlaps(p,x,y,z,yaw,vehicle.height,ground.getEntityBoundingBox()))return false;
                continue;
            }
            if(vehiclesOverlap(p,x,y,z,yaw,vehicle.height,VehicleChassisProfile.of(ground),
                    ground.posX,ground.posY,ground.posZ,ground.rotationYaw,ground.height))return false;
        }
        return true;
    }
    public static boolean vehiclesOverlap(VehicleChassisProfile a,double ax,double ay,double az,float yawA,double heightA,
            VehicleChassisProfile b,double bx,double by,double bz,float yawB,double heightB) {
        if(ay+heightA<=by+.06 || by+heightB<=ay+.06)return false;
        double ar=Math.toRadians(yawA+a.frontYaw),br=Math.toRadians(yawB+b.frontYaw);
        double ac=Math.cos(ar),as=Math.sin(ar),bc=Math.cos(br),bs=Math.sin(br),dx=bx-ax,dz=bz-az;
        for(double[] axis:new double[][]{{ac,as},{-as,ac},{bc,bs},{-bs,bc}}) {
            double ra=Math.abs(axis[0]*ac+axis[1]*as)*a.width/2+Math.abs(-axis[0]*as+axis[1]*ac)*a.length/2;
            double rb=Math.abs(axis[0]*bc+axis[1]*bs)*b.width/2+Math.abs(-axis[0]*bs+axis[1]*bc)*b.length/2;
            if(Math.abs(dx*axis[0]+dz*axis[1])>=ra+rb-.005)return false;
        }
        return true;
    }
}
