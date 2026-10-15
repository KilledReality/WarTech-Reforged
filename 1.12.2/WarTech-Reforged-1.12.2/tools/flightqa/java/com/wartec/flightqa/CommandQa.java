package com.wartec.flightqa;
import com.google.gson.*;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gameplay.*;
import com.wartec.wartecmod.port.network.*;
import com.wartec.wartecmod.port.integration.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import io.netty.buffer.*;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;

/** Disposable actual server objects, registry filtering, map sampling and serialized client data. */
public final class CommandQa {
    private WorldServer world;private final JsonArray checks=new JsonArray();private final List<String> failures=new ArrayList<>();
    private void check(String name,boolean pass,Object detail) {JsonObject c=new JsonObject();c.addProperty("kind",name);c.addProperty("success",pass);c.addProperty("detail",String.valueOf(detail));checks.add(c);if(!pass)failures.add(name+": "+detail);}
    private EntityWarTechGroundVehicle vehicle(WarTechEntityProfile profile,int x,int z,int variant) {
        world.getChunkFromChunkCoords(x>>4,z>>4);EntityWarTechGroundVehicle v=new EntityWarTechGroundVehicle(world,profile);v.setVisual(v.getVisualId(),variant);
        v.setPosition(x,4,z);v.setOwnerTeam("command-blue");v.setLegacyPower(500000);v.setDeployed(true);v.setLegacyEnabled(true);v.setLegacyOperational(true);v.updateBlocked=true;if(profile==WarTechEntityProfile.COMMAND_TRUCK)v.setEntityId(9700);check("spawn_"+profile,world.spawnEntity(v),v.getEntityId());return v;
    }
    public void started() throws Exception {
        if(!Files.exists(Paths.get("FLIGHT_QA_ISOLATED"))) throw new IllegalStateException("Not isolated");
        world=FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0);
        int x=2048,z=2048;for(int dx=-6;dx<=7;dx++)for(int dz=-5;dz<=5;dz++)world.getChunkFromChunkCoords((x>>4)+dx,(z>>4)+dz);
        for(int dx=-35;dx<=35;dx++)for(int dz=-7;dz<=7;dz++)world.setBlockState(new BlockPos(x+dx,3,z+dz),Blocks.WATER.getDefaultState(),2);
        for(int dx=-2;dx<=2;dx++)for(int dz=-32;dz<=32;dz++)world.setBlockState(new BlockPos(x+dx,3,z+dz),Blocks.STONE.getDefaultState(),2);
        EntityWarTechGroundVehicle command=vehicle(WarTechEntityProfile.COMMAND_TRUCK,x,z,0);
        FactionTerritoryData.claimAt(world,"command-blue",x,z);FactionTerritoryData.claimAt(world,"command-blue",x-1,z-1);
        MissileTrackingService.updateCommandPost(world,command.getEntityId(),x,4,z,"command-blue");
        EntityWarTechGroundVehicle radar=vehicle(WarTechEntityProfile.S400_RADAR,x+35,z+20,0);
        MissileTrackingService.updateStrategicRadarSweep(world,radar.getEntityId(),radar.posX,12,radar.posZ,8000,3000,32,"command-blue",0);
        for(int variant=0;variant<2;variant++) {
            EntityWarTechGroundVehicle v=vehicle(WarTechEntityProfile.MOBILE_AIR_DEFENSE,x+20+variant*20,z+40,variant);
            v.setInventorySlotContents(0,new ItemStack(variant==0?WarTechContent.ITEM_MISSILE_ANTI_AIR_TIER_2:WarTechContent.ITEM_MISSILE_ANTI_AIR_TIER_1,4));
            MissileTrackingService.updateLauncherPresence(world,v.posX,7,v.posZ,variant==0?2:1,0x4D41445300000000L|(v.getEntityId()&0xFFFFFFFFL),"command-blue");
        }
        for(ModernVehicleProfile kind:new ModernVehicleProfile[]{ModernVehicleProfile.BUK_M3,ModernVehicleProfile.OSA,ModernVehicleProfile.RADAR_9S18}) {
            EntityModernVehicle v=new EntityModernVehicle(world,kind);world.getChunkFromChunkCoords((x+65+kind.ordinal()*4)>>4,(z+15)>>4);v.setPosition(x+65+kind.ordinal()*4,4,z+15);v.setOwnerTeam("command-blue");v.setLegacyPower(500000);v.setDeployed(true);v.setLegacyOperational(true);v.setLegacyFireMode(1);v.updateBlocked=true;check("spawn_"+kind,world.spawnEntity(v),v.getEntityId());
            for(int slot=0;slot<kind.slots;slot++)v.setInventorySlotContents(slot,new ItemStack(kind.ammunition()));
            if(kind.isRadar() || kind==ModernVehicleProfile.OSA) MissileTrackingService.updateRadarSweep(world,v.getEntityId(),v.posX,7,v.posZ,kind.isRadar()?850:220,650,24,"command-blue",1);
            if(!kind.isRadar()) {
                MissileTrackingService.updateLauncherPresence(world,v.posX,7,v.posZ,kind.tier,0x4D4F444E00000000L|(v.getEntityId()&0xFFFFFFFFL),"command-blue");
                java.lang.reflect.Method aim=EntityModernVehicle.class.getDeclaredMethod("aimAt",double.class,double.class);aim.setAccessible(true);int ticks=0;float before=0;
                while(!(Boolean)aim.invoke(v,v.posX,v.posZ-200) && ticks++<100) {float after=v.getTurretYaw();check(kind+"_bounded_"+ticks,Math.abs(LegacySamGeometry.wrap(after-before))<=v.traverseSpeed()+.001,after);before=after;}
                check(kind+"_fast_reaction",ticks<=26,ticks);
            }
        }
        for(boolean patriot:new boolean[]{true,false}) {
            BlockPos pos=new BlockPos(x+(patriot?40:60),4,z+70);LegacyLauncherBlock block=patriot?WarTechContent.PATRIOT_LAUNCHER:WarTechContent.S400_LAUNCHER;
            world.setBlockState(pos,block.getDefaultState().withProperty(LegacyLauncherBlock.META,12),3);TileEntityWarTechMachine t=(TileEntityWarTechMachine)world.getTileEntity(pos);t.setOwnerTeam("command-blue");t.setPower(t.getMaxPower());t.setInventorySlotContents(0,new ItemStack(patriot?WarTechContent.ITEM_MISSILE_ANTI_AIR_TIER_2:WarTechContent.ITEM_MISSILE_ANTI_AIR_TIER_3,8));
            NBTTagCompound saved=new NBTTagCompound();t.writeToNBT(saved);saved.setInteger("SamDeployment",120);saved.setBoolean("SamEnabled",true);t.readFromNBT(saved);
            long key=1469598103934665603L;key=(key^pos.getX())*1099511628211L;key=(key^pos.getY())*1099511628211L;key=(key^pos.getZ())*1099511628211L;
            MissileTrackingService.updateLauncherPresence(world,pos.getX()+.5,10,pos.getZ()+.5,patriot?2:3,key,"command-blue");
        }
        for(boolean friendly:new boolean[]{true,false}) {
            EntityWarTechAircraft aircraft=new EntityWarTechAircraft(world,WarTechEntityProfile.F_16C);aircraft.setPosition(x+(friendly?-80:120),35,z-35);aircraft.setLegacyState(1);aircraft.ticksExisted=100;aircraft.setOwnerTeam(friendly?"command-blue":"command-red");aircraft.updateBlocked=true;world.getChunkFromChunkCoords(((int)aircraft.posX)>>4,((int)aircraft.posZ)>>4);check("spawn_aircraft_"+friendly,world.spawnEntity(aircraft),aircraft.getEntityId());
            check("aircraft_visible_"+friendly,AirDefenseVisibility.visible(world,radar.getPositionVector().addVector(0,8,0),aircraft),aircraft.getTargetType());
            MissileTrackingService.registerLaunch(aircraft,aircraft.posX,aircraft.posY,aircraft.posZ,x,z,aircraft.getOwnerTeam());
        }
        MissileTrackingService.updateStrategicRadarSweep(world,radar.getEntityId(),radar.posX,12,radar.posZ,8000,3000,32,"command-blue",0);
        MissileTrackingService.updateCommunicationRelay(world,9090,x+60,4,z+55,"command-blue");
        MissileTrackingService.updateRadarSweep(world,9900,x+4,10,z+4,850,400,24,"command-red",1);
        MissileTrackingService.updateLauncherPresence(world,x+4,4,z+4,3,9999,"command-red");
        int chunks=world.getChunkProvider().getLoadedChunkCount();
        FactionCommandSnapshot snapshot=FactionCommandRequestMessage.build(command,x+35,z+35,128);
        check("terrain_no_chunk_loads",chunks==world.getChunkProvider().getLoadedChunkCount(),chunks);
        check("radar_has_air_contacts",snapshot.contacts.length>=2,snapshot.contacts.length);
        Set<String> names=new HashSet<>();for(FactionCommandSnapshot.Node n:snapshot.nodes) {names.add(n.system);check("friendly_only_"+n.id,n.id!=9900 && n.id!=9999,n.system);if(n.type==3)check("launcher_metadata_"+n.system,n.range>0 && n.ammo>0,n.range+" / "+n.ammo);}
        for(String name:new String[]{"s400_radar","tor","pantsir","modern_buk_m3","modern_osa","modern_radar_9s18","patriotlauncher","s400launcher","command_truck"})check("system_"+name,names.contains(name),names);
        int known=0,water=0;for(int color:snapshot.terrain.colors) {if(color!=0)known++;if((color&0xFFFFFF)==net.minecraft.block.material.MapColor.WATER.colorValue)water++;}
        check("terrain_has_real_land_and_water",known>0 && water>0,known+" cells / water "+water);
        FactionCommandSnapshot.Terrain far=CommandMapData.terrain(world,100000,100000,16384);int farKnown=0;for(int color:far.colors)if(color!=0)farKnown++;
        check("unknown_terrain_is_not_generated",farKnown==0 && chunks==world.getChunkProvider().getLoadedChunkCount(),farKnown);
        ByteBuf buffer=Unpooled.buffer();new FactionCommandSnapshotMessage(snapshot).toBytes(buffer);FactionCommandSnapshotMessage decoded=new FactionCommandSnapshotMessage();decoded.fromBytes(buffer);check("wire_consumed",buffer.readableBytes()==0,buffer.readableBytes());buffer.release();
        check("wire_nodes",decoded.getSnapshot().nodes.length==snapshot.nodes.length,snapshot.nodes.length);
        Files.write(Paths.get("command-snapshot.json"),new GsonBuilder().setPrettyPrinting().create().toJson(decoded.getSnapshot()).getBytes(StandardCharsets.UTF_8));
        for(net.minecraft.entity.Entity e:new ArrayList<>(world.loadedEntityList)) if(e instanceof EntityWarTechAircraft) { e.setDead();MissileChunkLoader.untrack(e); }
        String resources=MissileChunkLoader.diagnostics(world);check("cleanup",resources.contains("flights=0/") && resources.contains("forced=0 waiting=0") && resources.contains("server queue=0 chunks/0 owners"),resources);
        JsonObject report=new JsonObject();report.addProperty("complete",true);report.add("cases",checks);report.add("failures",new Gson().toJsonTree(failures));report.addProperty("resources",MissileChunkLoader.diagnostics(world));
        Files.write(Paths.get("flightqa-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report).getBytes(StandardCharsets.UTF_8));
        System.out.println("COMMANDQA "+checks.size()+" checks "+failures);FMLCommonHandler.instance().getMinecraftServerInstance().initiateShutdown();
    }
}
