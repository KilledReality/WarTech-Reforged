package com.wartec.wartecmod.port.network;

import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gameplay.TileEntityWarTechMachine;
import com.wartec.wartecmod.port.integration.NetworkTeamHelper;
import com.wartec.wartecmod.port.integration.WeaponBalance;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

/** Adds presentation data to the actual faction network, never discovering hostile units. */
public final class CommandMapData {
    private CommandMapData() { }
    public static FactionCommandSnapshot.Node describe(WorldServer world,String team,MissileTrackingService.FactionNode n) {
        String system="";int range=n.type<=2?n.value:0,ammo=-1,status=0;
        for(Entity e:world.loadedEntityList) if(e instanceof EntityWarTechBase && !e.isDead
                && Math.hypot(e.posX-n.x,e.posZ-n.z)<.1
                && NetworkTeamHelper.areFriendly(team,((EntityWarTechBase)e).getOwnerTeam())) {
            EntityWarTechBase v=(EntityWarTechBase)e;
            system=v.getProfile().name().toLowerCase(java.util.Locale.ROOT);
            if(v.getProfile()==WarTechEntityProfile.MOBILE_AIR_DEFENSE) system=v.isTor()?"tor":"pantsir";
            status=!v.isDeployed()?4:!v.isLegacyOperational()?5:v.getLegacyPower()<=0?3:1;
            if(n.type==3) {
                range=v.getDefenseEngagementRange();ammo=0;
                for(int i=0;i<v.getBatterySlot();i++) if(!v.getStackInSlot(i).isEmpty() && v.isRequiredInterceptor(v.getStackInSlot(i))) ammo+=v.getStackInSlot(i).getCount();
                if(v instanceof EntityModernVehicle) { ammo=((EntityModernVehicle)v).loadedCount();status=!v.isDeployed()?4:v.getLegacyPower()<=0?3:v.getLegacyFireMode()==0?5:1; }
                if(status==1 && ammo==0) status=2;
            }
            break;
        }
        if(system.isEmpty()) for(TileEntity t:world.loadedTileEntityList) if(t instanceof TileEntityWarTechMachine
                && Math.abs(t.getPos().getX()+.5-n.x)<.1 && Math.abs(t.getPos().getZ()+.5-n.z)<.1
                && NetworkTeamHelper.areFriendly(team,((TileEntityWarTechMachine)t).getOwnerTeam())) {
            TileEntityWarTechMachine m=(TileEntityWarTechMachine)t;system=m.getLegacyMachineName();status=m.getPower()<=0?3:1;
            if(n.type==3) { ammo=m.getSamAmmoCount();range=(int)WeaponBalance.interceptorRange(n.value);
                if(m.isAnimatedSam() && (!m.isSamEnabled() || m.getSamDeployment(1)<1)) status=4;
                else if(m.isAnimatedSam() && !m.isSamAutoFire()) status=5;
                else if(ammo==0) status=2;
            }
            break;
        }
        return new FactionCommandSnapshot.Node(n.type,n.id,n.x,n.y,n.z,n.value,n.band,system,range,ammo,status);
    }
    public static FactionCommandSnapshot.Terrain terrain(WorldServer world,double x,double z,int radius) {
        int[] colors=new int[FactionCommandSnapshot.Terrain.WIDTH*FactionCommandSnapshot.Terrain.HEIGHT];
        for(int row=0;row<FactionCommandSnapshot.Terrain.HEIGHT;row++) for(int col=0;col<FactionCommandSnapshot.Terrain.WIDTH;col++) {
            int wx=(int)Math.floor(x+((col+.5)/FactionCommandSnapshot.Terrain.WIDTH-.5)*2*radius);
            int wz=(int)Math.floor(z+((row+.5)/FactionCommandSnapshot.Terrain.HEIGHT-.5)*2*radius*CommandMapViewport.HEIGHT/CommandMapViewport.WIDTH);
            // getLoadedChunk is deliberate: moving/zooming the map must not generate terrain or obtain tickets.
            Chunk chunk=world.getChunkProvider().getLoadedChunk(wx>>4,wz>>4);
            if(chunk==null) continue;
            int y=Math.max(0,chunk.getHeightValue(wx&15,wz&15)-1);
            BlockPos pos=new BlockPos(wx,y,wz);net.minecraft.block.state.IBlockState state=chunk.getBlockState(pos);
            colors[row*FactionCommandSnapshot.Terrain.WIDTH+col]=0xFF000000|state.getMapColor(world,pos).colorValue;
        }
        return new FactionCommandSnapshot.Terrain(x,z,radius,colors);
    }
}
