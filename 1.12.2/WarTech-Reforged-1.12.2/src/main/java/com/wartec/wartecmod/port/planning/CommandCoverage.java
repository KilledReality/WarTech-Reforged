package com.wartec.wartecmod.port.planning;

import com.wartec.wartecmod.port.integration.AirDefenseVisibility;
import java.util.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.*;
import net.minecraft.world.WorldServer;

/** Small cached geometric overlay; unknown chunks stay unknown and are never generated. */
public final class CommandCoverage {
    private static final Map<WorldServer,LinkedHashMap<String,NBTTagCompound>> CACHE=new WeakHashMap<>();
    private CommandCoverage(){}
    public static NBTTagCompound view(WorldServer world,String scope,String asset,double x,double z,int radius,double aspect){
        PlanningAsset a=PlanningWorldData.get(world).assets.get(asset);
        if(a==null || !a.loaded || !PlanningAccessPolicy.same(scope,a.scope) || a.range<=0 || !("pvo".equals(a.kind) || "radar".equals(a.kind)))return new NBTTagCompound();
        if(!Double.isFinite(x) || !Double.isFinite(z) || Math.abs(x)>29999984 || Math.abs(z)>29999984)return new NBTTagCompound();
        if(!Double.isFinite(aspect) || aspect<.2 || aspect>2)aspect=.5625;
        radius=Math.max(64,Math.min(16384,radius));long now=world.getTotalWorldTime();String key=scope+":"+asset+":"+(int)x+":"+(int)z+":"+radius+":"+(int)(aspect*100)+":"+(int)a.x+":"+(int)a.z;
        LinkedHashMap<String,NBTTagCompound> cache=CACHE.computeIfAbsent(world,w->new LinkedHashMap<>());NBTTagCompound previous=cache.get(key);
        if(previous!=null && now-previous.getLong("Tick")<80)return previous.copy();
        int[] mask=new int[144];double altitude=Math.min(1200,a.y+100);Vec3d sensor=new Vec3d(a.x,a.y+(a.tier==4?5:3),a.z);
        for(int row=0;row<9;row++)for(int col=0;col<16;col++){double tx=x+((col+.5)/16-.5)*radius*2,tz=z+((row+.5)/9-.5)*radius*2*aspect;BlockPos cell=new BlockPos(tx,64,tz);if(!world.isBlockLoaded(cell))continue;int at=row*16+col;Vec3d point=new Vec3d(tx,altitude,tz);if(sensor.distanceTo(point)>a.range)mask[at]=3;else mask[at]=AirDefenseVisibility.clear(world,sensor,point)?1:2;}
        NBTTagCompound n=new NBTTagCompound();n.setString("Asset",asset);n.setDouble("X",x);n.setDouble("Z",z);n.setInteger("Radius",radius);n.setDouble("Aspect",aspect);n.setDouble("Altitude",altitude);n.setIntArray("Mask",mask);n.setLong("Tick",now);
        if(cache.size()>=16)cache.remove(cache.keySet().iterator().next());cache.put(key,n);return n.copy();
    }
}
