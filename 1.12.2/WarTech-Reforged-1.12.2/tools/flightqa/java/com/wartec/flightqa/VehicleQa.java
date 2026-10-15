package com.wartec.flightqa;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.integration.MissileChunkLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.*;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Actual isolated server ticks for each chassis. No user saves or account. */
public final class VehicleQa {
    private WorldServer world;private int tick;
    private final List<EntityWarTechGroundVehicle> vehicles=new ArrayList<>();
    private final List<FakePlayer> drivers=new ArrayList<>();
    private final JsonArray cases=new JsonArray();private final List<String> failures=new ArrayList<>();
    private final Map<Entity,Vec3d> starts=new HashMap<>();
    private final Map<Entity,Double> coastSpeeds=new HashMap<>();
    private final Map<Entity,Double> watchdogSpeeds=new HashMap<>();
    private static final class Pilot extends FakePlayer {
        Pilot(WorldServer world,GameProfile profile){super(world,profile);}
        @Override public void setPositionAndUpdate(double x,double y,double z){setPosition(x,y,z);}
    }
    private final List<net.minecraftforge.common.ForgeChunkManager.Ticket> tickets=new ArrayList<>();
    public void started() {
        if(!Files.isRegularFile(Paths.get("FLIGHT_QA_ISOLATED")))throw new IllegalStateException("Not isolated");
        world=FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0);
        world.getGameRules().setOrCreateGameRule("doMobSpawning","false");world.getGameRules().setOrCreateGameRule("randomTickSpeed","0");
        MinecraftForge.EVENT_BUS.register(this);
    }
    private void check(String kind,boolean success,String detail) {
        JsonObject row=new JsonObject();row.addProperty("kind",kind);row.addProperty("success",success);row.addProperty("detail",detail);cases.add(row);
        if(!success)failures.add(kind+": "+detail);System.out.println("VEHICLEQA "+row);
    }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) throws Exception {
        if(event.phase!=TickEvent.Phase.END)return;tick++;
        if(tick==1) {
            net.minecraftforge.common.ForgeChunkManager.Ticket ticket=null;int forced=0;
            for(int cx=624;cx<=636;cx++)for(int cz=623;cz<=629;cz++) {
                if(ticket==null || forced==25) {
                    ticket=net.minecraftforge.common.ForgeChunkManager.requestTicket(com.wartec.wartecmod.WarTechReforged.instance,world,net.minecraftforge.common.ForgeChunkManager.Type.NORMAL);
                    if(ticket==null)throw new IllegalStateException("No QA chunk ticket");tickets.add(ticket);forced=0;
                }
                net.minecraftforge.common.ForgeChunkManager.forceChunk(ticket,new net.minecraft.util.math.ChunkPos(cx,cz));forced++;
            }
            for(int x=9984;x<=10176;x+=16)for(int z=9968;z<=10064;z+=16)world.getChunkFromChunkCoords(x>>4,z>>4);
            for(int x=9984;x<10176;x++)for(int z=9970;z<10055;z++) {
                world.setBlockState(new BlockPos(x,3,z),Blocks.STONE.getDefaultState(),2);
                for(int y=4;y<12;y++)world.setBlockToAir(new BlockPos(x,y,z));
            }
            for(int i=0;i<7;i++) {
                EntityWarTechGroundVehicle v=i<3?new EntityStrategicTel(world,i==0?StrategicSystemProfile.ISKANDER:i==1?StrategicSystemProfile.TOPOL_M:StrategicSystemProfile.ORESHNIK):
                    new EntityWarTechGroundVehicle(world,i<5?WarTechEntityProfile.MOBILE_AIR_DEFENSE:i==5?WarTechEntityProfile.MOBILE_ARTILLERY:WarTechEntityProfile.COMMAND_TRUCK);
                if(i==4)v.setVisual("ground/mobile_air_defense",1);
                v.setPosition(10000+i*24,4,10016);v.setDeployed(false);world.spawnEntity(v);vehicles.add(v);
                FakePlayer driver=new Pilot(world,new GameProfile(new UUID(85,i+1),"VehicleQA"+i));drivers.add(driver);
                driver.setPosition(v.posX,v.posY,v.posZ);driver.startRiding(v,true);starts.put(v,v.getPositionVector());
                check("mount_"+i,v.isDrivenBy(driver),"real server riding relation");
                check("crew_collision_"+i,v.getCollisionBox(driver)==null && !v.canBePushed(),"passenger ignored and no human push");
                double speed=v.getDriveSpeed();v.applyEntityCollision(driver);check("push_"+i,v.getDriveSpeed()==speed && v.motionX==0 && v.motionZ==0,"no impulse");
            }
        }
        for(int i=0;i<vehicles.size();i++) {
            EntityWarTechGroundVehicle v=vehicles.get(i);FakePlayer driver=drivers.get(i);
            if(tick<=51)v.acceptDriverInput(driver,1,0,0,tick);
            else if(tick<=61)v.acceptDriverInput(driver,0,0,0,tick);
            else if(tick<=78)v.acceptDriverInput(driver,0,0,1,tick);
            else if(tick<=115)v.acceptDriverInput(driver,-1,0,0,tick);
            else if(tick<=125)v.acceptDriverInput(driver,0,0,1,tick);
            else if(tick<=145)v.acceptDriverInput(driver,0,1,0,tick);
            else if(tick<=150)v.acceptDriverInput(driver,1,0,0,tick);
            if(tick==50) {
                Vec3d offset=v.getPositionVector().subtract(starts.get(v));VehicleChassisProfile p=VehicleChassisProfile.of(v);
                check("forward_"+i,offset.lengthVector()>2 && offset.z*(p.frontYaw==180?-1:1)>0,"distance="+offset.lengthVector());
                check("ground_"+i,Math.abs(v.posY-4)<.02 && v.onGround,"y="+v.posY);
                Vec3d seat=p.seat(v.rotationYaw,v.rotationPitch).add(v.getPositionVector());v.updatePassenger(driver);
                check("seat_"+i,Math.abs(driver.posX-seat.x)<1e-6 && Math.abs(driver.posZ-seat.z)<1e-6,"model-specific seat");
                coastSpeeds.put(v,v.getDriveSpeed());
            }
            if(tick==60)check("coast_"+i,v.getDriveSpeed()>0 && v.getDriveSpeed()<coastSpeeds.get(v),"speed="+v.getDriveSpeed());
            if(tick==78)check("brake_"+i,Math.abs(v.getDriveSpeed())<.001,"speed="+v.getDriveSpeed());
            if(tick==115)check("reverse_"+i,v.getDriveSpeed()<-.05,"speed="+v.getDriveSpeed());
            if(tick==145)check("pivot_"+i,(i==3?Math.abs(v.rotationYaw)>4:Math.abs(v.rotationYaw)<.01),"yaw="+v.rotationYaw);
            if(tick==160)watchdogSpeeds.put(v,v.getDriveSpeed());
            if(tick==163)check("watchdog_braking_"+i,Math.abs(v.getDriveSpeed())<Math.abs(watchdogSpeeds.get(v)),"expired throttle speed="+v.getDriveSpeed());
            if(tick==168)check("watchdog_stopped_"+i,Math.abs(v.getDriveSpeed())<.001,"speed="+v.getDriveSpeed());
        }
        if(tick==165) {
            EntityWarTechGroundVehicle v=vehicles.get(0);FakePlayer driver=drivers.get(0);
            double oldSpeed=v.getDriveSpeed();v.acceptDriverInput(driver,Float.NaN,1,0,166);check("invalid_input",v.getDriveSpeed()==oldSpeed,"NaN does not change state");
            FakePlayer outsider=drivers.get(1);v.acceptDriverInput(outsider,1,1,0,167);check("other_driver_denied",!v.isDrivenBy(outsider),"another seat cannot command");
            VehicleChassisProfile p=VehicleChassisProfile.of(v);Vec3d front=p.offset(0,0,p.length/2+.1,v.rotationYaw,0).add(v.getPositionVector());
            BlockPos obstacle=new BlockPos(front.x,4,front.z);world.setBlockState(obstacle,Blocks.STONE.getDefaultState());
            Vec3d next=p.offset(0,0,.2,v.rotationYaw,0).add(v.getPositionVector());
            check("overhang_wall",!VehicleCollisionHull.clear(v,next.x,v.posY,next.z,v.rotationYaw),"front of chassis blocks before centre box reaches wall");
            world.setBlockToAir(obstacle);
            v.acceptDriverInput(driver,0,0,2,168);NBTTagCompound save=new NBTTagCompound();v.writeToNBT(save);
            EntityStrategicTel copy=new EntityStrategicTel(world,StrategicSystemProfile.ISKANDER);copy.readFromNBT(save);
            check("parking_nbt",copy.isParkingBrake(),"parking brake survives restart data");
            check("click_hitbox_preserved",v.canBeCollidedWith() && v.getCollisionBoundingBox()!=null,"repair and hits remain available");
            for(int i=0;i<vehicles.size();i++)check("custom_input_authority_"+i,!vehicles.get(i).canPassengerSteer(),"vanilla vehicle-position packets disabled");
            v.requestDriverExit();
        }
        if(tick==170)check("safe_exit",drivers.get(0).getRidingEntity()==null && world.getCollisionBoxes(null,drivers.get(0).getEntityBoundingBox()).isEmpty(),"server dismount selects empty space");
        if(tick==175) {
            EntityWarTechGroundVehicle v=vehicles.get(0);VehicleChassisProfile p=VehicleChassisProfile.of(v);
            com.wartec.wartecmod.port.network.VehicleStateMessage reset=new com.wartec.wartecmod.port.network.VehicleStateMessage(v.getEntityId(),10000,4,10044,0,0,0,0,0);
            reset.grounded=true;v.restoreDrivingState(reset);
            EntityWarTechGroundVehicle other=vehicles.get(1);
            com.wartec.wartecmod.port.network.VehicleStateMessage otherBefore=new com.wartec.wartecmod.port.network.VehicleStateMessage(other,0);
            other.setPosition(10000,4,10030);
            check("native_vehicle_overhang_collision",!VehicleCollisionHull.clear(v,v.posX,v.posY,v.posZ,0),"full hulls meet while central boxes do not");
            other.restoreDrivingState(otherBefore);
            Vec3d front=p.offset(0,0,p.length/2+1.2,0,0).add(v.getPositionVector());BlockPos wall=new BlockPos(front);
            for(int x=-3;x<=3;x++)for(int y=4;y<8;y++)world.setBlockState(new BlockPos(wall.getX()+x,y,wall.getZ()),Blocks.STONE.getDefaultState(),2);
            for(int i=0;i<50;i++)v.simulateDriving(1,0,false);
            check("native_wall_stops_chassis",v.posZ>wall.getZ()+p.length/2 && Math.abs(v.posY-4)<.02,"z="+v.posZ+" y="+v.posY);
            for(int x=-3;x<=3;x++)for(int y=4;y<8;y++)world.setBlockToAir(new BlockPos(wall.getX()+x,y,wall.getZ()));
            v.restoreDrivingState(reset);
            for(int x=-3;x<=3;x++)world.setBlockState(new BlockPos(wall.getX()+x,4,wall.getZ()),Blocks.STONE_SLAB.getDefaultState(),2);
            double maxY=4;
            for(int i=0;i<60;i++){v.simulateDriving(1,0,false);maxY=Math.max(maxY,v.posY);}
            check("native_slab_step",v.posZ<10041 && maxY>4.4 && maxY<4.56,"z="+v.posZ+" maxY="+maxY);
            for(int x=-3;x<=3;x++)world.setBlockToAir(new BlockPos(wall.getX()+x,4,wall.getZ()));
            v.restoreDrivingState(reset);v.setPosition(10000,9,10044);v.onGround=false;v.simulateDriving(0,1,false);
            check("native_airborne_gravity",v.posY<9 && v.rotationYaw==0,"gravity without air steering");
            double far=2000000;check("unloaded_path_blocked",!VehicleCollisionHull.clear(v,far,4,far,0),"no driving into unknown chunks");
        }
        if(tick>=180) {
            for(EntityWarTechGroundVehicle v:vehicles)v.setDead();
            for(net.minecraftforge.common.ForgeChunkManager.Ticket ticket:tickets)net.minecraftforge.common.ForgeChunkManager.releaseTicket(ticket);tickets.clear();
            JsonObject out=new JsonObject();out.addProperty("complete",true);out.add("cases",cases);out.add("failures",new Gson().toJsonTree(failures));out.addProperty("resources",MissileChunkLoader.diagnostics(world));
            Files.write(Paths.get("flightqa-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(out).getBytes(StandardCharsets.UTF_8));
            System.out.println("VEHICLEQA FINISHED failures="+failures);world.getMinecraftServer().initiateShutdown();
        }
    }
}
