package com.wartec.wartecmod.port.planning;

import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.uav.*;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.*;

/** Observations originate at the physical UAV. Delivery can wait locally for a link or recovery. */
public final class PlanningReconSensor {
    private PlanningReconSensor() { }
    public static void cell(EntityCustomUav u,int x,int z,int height,int color,UavPartDefinition sensor){u.planningIntel().cell(x,z,height,color,u.world.getTotalWorldTime(),sensor.name(),u.getUniqueID());}
    public static void contact(EntityCustomUav u,Entity e,float quality,UavPartDefinition sensor){
        if(!(e instanceof EntityWarTechBase) || PlanningAccessPolicy.same(PlanningAccessPolicy.scope(u),PlanningAccessPolicy.scope((EntityWarTechBase)e)))return;
        String kind=e instanceof EntityWarTechAircraft || e instanceof EntityCustomUav?"aircraft":e instanceof EntityWarTechGroundVehicle?"vehicle":"missile";
        PlanningAsset asset=PlanningAssetRegistry.describe((EntityWarTechBase)e);String name=asset==null?e.getName():asset.name;
        u.planningIntel().contact(PlanningAssetRegistry.key(e),name,kind,e.posX,e.posY,e.posZ,quality,u.world.getTotalWorldTime(),sensor.name(),u.getUniqueID());
    }
    public static void tiles(EntityCustomUav u,UavPartDefinition sensor){
        double range=sensor==UavPartDefinition.SENSOR_SAR?240:sensor==UavPartDefinition.SENSOR_EO_IR?160:96;
        for(TileEntity tile:new ArrayList<>(u.world.loadedTileEntityList)){
            if(tile.isInvalid() || PlanningAccessPolicy.same(PlanningAccessPolicy.scope(u),PlanningAccessPolicy.scope(tile)))continue;
            BlockPos pos=tile.getPos();Vec3d at=new Vec3d(pos).addVector(.5,1,.5);if(u.getPositionVector().distanceTo(at)>range || !visible(u,at,sensor))continue;
            PlanningAsset a=PlanningAssetRegistry.describe(tile);if(a==null)continue;
            float quality=(float)MathHelper.clamp(u.getUavStats().getSensorQuality()*(1-u.getPositionVector().distanceTo(at)/range*.72)/2.1,.12,1);
            u.planningIntel().contact(a.key,a.name,a.kind,a.x,a.y,a.z,quality,u.world.getTotalWorldTime(),sensor.name(),u.getUniqueID());
        }
        recheck(u,sensor,range);
    }
    private static void recheck(EntityCustomUav u,UavPartDefinition sensor,double range){
        TeamIntelData archive=PlanningWorldData.get(u.world).intel.get(PlanningAccessPolicy.scope(u));if(archive==null)return;TeamIntelData buffer=u.planningIntel();long now=u.world.getTotalWorldTime();
        for(NBTTagCompound c:archive.contacts.values())if(!"aircraft".equals(c.getString("Kind")) && !"missile".equals(c.getString("Kind"))){
            NBTTagCompound current=buffer.contacts.get(c.getString("Key"));if(current!=null && current.getBoolean("Confirmed") && current.getLong("Seen")>=now-20)continue;
            int cx=Math.floorDiv((int)Math.floor(c.getDouble("X")),16),cz=Math.floorDiv((int)Math.floor(c.getDouble("Z")),16);NBTTagCompound observed=buffer.cells.get(((long)cx<<32)|(cz&0xffffffffL));Vec3d point=new Vec3d(c.getDouble("X"),c.getDouble("Y")+2,c.getDouble("Z"));
            if(observed!=null && observed.getLong("Seen")>=now-20 && u.getPositionVector().distanceTo(point)<=range && visible(u,point,sensor)){NBTTagCompound checked=c.copy();checked.setBoolean("Confirmed",false);checked.setLong("Checked",now);buffer.contacts.put(c.getString("Key"),checked);}
        }
    }
    public static boolean visible(EntityCustomUav u,Vec3d point,UavPartDefinition sensor){
        if(!u.world.isBlockLoaded(new BlockPos(point)))return false;
        if(sensor==UavPartDefinition.SENSOR_SAR)return point.y>=u.world.getHeight(new BlockPos(point)).getY()-4;
        RayTraceResult hit=u.world.rayTraceBlocks(u.getPositionVector().addVector(0,.2,0),point,false,true,false);
        return (hit==null || hit.hitVec.distanceTo(point)<1.6) && (sensor!=UavPartDefinition.SENSOR_DAY || u.world.getLight(new BlockPos(point))>=8);
    }
    public static void publish(EntityCustomUav u){
        if(u.world.isRemote || !u.planningCanTransmit())return;TeamIntelData buffer=u.planningIntel();if(buffer.cells.isEmpty() && buffer.contacts.isEmpty())return;
        PlanningWorldData d=PlanningWorldData.get(u.world);TeamIntelData archive=d.intel(PlanningAccessPolicy.scope(u));if(archive==null)return;
        for(NBTTagCompound c:buffer.cells.values()){
            long key=((long)c.getInteger("X")<<32)|(c.getInteger("Z")&0xffffffffL);NBTTagCompound old=archive.cells.get(key);
            if(old==null || c.getLong("Seen")>=old.getLong("Seen"))archive.cell(c.getInteger("X"),c.getInteger("Z"),c.getInteger("Height"),c.getInteger("Color"),c.getLong("Seen"),c.getString("Sensor"),c.getUniqueId("Source"));
        }
        Set<String> present=new HashSet<>();for(NBTTagCompound c:buffer.contacts.values())if(c.getLong("Seen")>=u.world.getTotalWorldTime()-20)present.add(c.getString("Key"));
        for(NBTTagCompound c:buffer.contacts.values()){
            NBTTagCompound old=archive.contacts.get(c.getString("Key"));
            if(!c.getBoolean("Confirmed")){if(old!=null && c.getLong("Checked")>=Math.max(old.getLong("Seen"),old.getLong("Checked")))archive.markUnconfirmed(c.getString("Key"),c.getLong("Checked"));}
            else if(old==null || c.getLong("Seen")>=Math.max(old.getLong("Seen"),old.getLong("Checked")))archive.contact(c.getString("Key"),c.getString("Name"),c.getString("Kind"),c.getDouble("X"),c.getDouble("Y"),c.getDouble("Z"),c.getFloat("Quality"),c.getLong("Seen"),c.getString("Sensor"),c.getUniqueId("Source"));
        }
        // Absence requires a new physical observation of the old location, never a map refresh.
        UavPartDefinition sensor=u.getBuild().get(UavSlot.SENSOR);
        if(sensor!=null){double range=sensor==UavPartDefinition.SENSOR_SAR?240:sensor==UavPartDefinition.SENSOR_EO_IR?160:96;
            for(NBTTagCompound c:archive.contacts.values())if(!present.contains(c.getString("Key")) && !"aircraft".equals(c.getString("Kind")) && !"missile".equals(c.getString("Kind"))){
                int cx=Math.floorDiv((int)Math.floor(c.getDouble("X")),16),cz=Math.floorDiv((int)Math.floor(c.getDouble("Z")),16);NBTTagCompound observed=buffer.cells.get(((long)cx<<32)|(cz&0xffffffffL));
                Vec3d point=new Vec3d(c.getDouble("X"),c.getDouble("Y")+2,c.getDouble("Z"));
                if(observed!=null && observed.getLong("Seen")>=u.world.getTotalWorldTime()-20 && u.getPositionVector().distanceTo(point)<=range && visible(u,point,sensor))archive.markUnconfirmed(c.getString("Key"),observed.getLong("Seen"));
            }
        }
        buffer.cells.clear();buffer.contacts.clear();d.markDirty();
    }
}
