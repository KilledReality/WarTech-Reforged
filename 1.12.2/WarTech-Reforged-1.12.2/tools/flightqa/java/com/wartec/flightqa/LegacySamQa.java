package com.wartec.flightqa;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gameplay.*;
import com.wartec.wartecmod.port.integration.*;
import com.wartec.wartecmod.port.network.MissileTrackingService;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.*;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Actual legacy block placement, network acquisition, socket launch and stow. */
public final class LegacySamQa {
    private WorldServer world;private int tick,doneAt=-1;
    private final JsonArray checks=new JsonArray();private final List<String> failures=new ArrayList<>();
    private final List<Fixture> fixtures=new ArrayList<>();private final List<Entity> targets=new ArrayList<>();
    private final List<Flight> flights=new ArrayList<>();
    private final JsonArray exitTrace=new JsonArray();
    private boolean stow(TileEntityWarTechMachine tile) {
        FakePlayer player=new FakePlayer(world,new com.mojang.authlib.GameProfile(new UUID(96,91),"SamOperator"));
        player.setPosition(tile.getPos().getX(),tile.getPos().getY(),tile.getPos().getZ());
        com.wartec.wartecmod.port.integration.NetworkTeamHelper.setPlayerTeam(player,"samqa-blue");
        net.minecraft.tileentity.TileEntity installed=world.getTileEntity(tile.getPos());
        world.setTileEntity(tile.getPos(),tile);
        try { return tile.handleLegacyGuiAction(0,player); }
        finally { installed.validate();world.setTileEntity(tile.getPos(),installed);installed.validate(); }
    }
    private final class Fixture {
        final boolean patriot;final BlockPos pos;TileEntityWarTechMachine tile;int shots;boolean folded;final Set<Integer> channels=new HashSet<>();
        Fixture(boolean patriot,int x) {
            this.patriot=patriot;pos=new BlockPos(x,4,2048);LegacyLauncherBlock block=patriot?WarTechContent.PATRIOT_LAUNCHER:WarTechContent.S400_LAUNCHER;
            world.getChunkFromBlockCoords(pos);world.setBlockState(pos,block.getDefaultState(),3);
            block.onBlockPlacedBy(world,pos,block.getDefaultState(),null,new ItemStack(block));tile=(TileEntityWarTechMachine)world.getTileEntity(pos);
            tile.setOwnerTeam("samqa-blue");tile.setPower(tile.getMaxPower());
            tile.setInventorySlotContents(0,new ItemStack(patriot?WarTechContent.ITEM_MISSILE_ANTI_AIR_TIER_2:WarTechContent.ITEM_MISSILE_ANTI_AIR_TIER_3,8));
            EntityModernVehicle radar=new EntityModernVehicle(world,ModernVehicleProfile.RADAR_9S18);
            radar.setEntityId(patriot?95001:95002);
            radar.setPosition(x+65,4,2048+40);radar.setOwnerIdentity(null,"samqa-blue");radar.setLegacyPower(radar.getEnergyCapacity());radar.setDeployed(true);
            world.getChunkFromChunkCoords((x+65)>>4,(2048+40)>>4);
            if(!world.spawnEntity(radar))throw new IllegalStateException("External SAM radar spawn rejected");
            for(int i=0;i<8;i++) target(x+(i%2==0?65:-65),34+i*3,2048+40-i*10);
        }
        String id() { return patriot?"patriot":"s400"; }
    }
    private final class Flight {
        final EntityWarTechMissile missile;final Vec3d origin,axis;final int exit;final Fixture owner;final float launchYaw;boolean checked;
        Vec3d previousMotion;int previousAge=-1,handoffSteps;double maxTurn,maxAcceleration;boolean handoffChecked;
        Flight(EntityWarTechMissile missile,Fixture owner) {
            this.missile=missile;this.owner=owner;launchYaw=owner.tile.getSamYaw(1);origin=missile.getPositionVector();NBTTagCompound tag=missile.getModernVisual();
            axis=new Vec3d(tag.getDouble("DirectionX"),tag.getDouble("DirectionY"),tag.getDouble("DirectionZ"));exit=tag.getInteger("ExitTicks");
        }
    }
    private void check(String name,boolean success,Object detail) {
        JsonObject row=new JsonObject();row.addProperty("kind",name);row.addProperty("success",success);row.addProperty("detail",String.valueOf(detail));checks.add(row);
        if(!success) failures.add(name+": "+detail);
    }
    public void started() throws Exception {
        if(!Files.isRegularFile(Paths.get("FLIGHT_QA_ISOLATED"))) throw new IllegalStateException("Not isolated");
        world=FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0);
        if(!"flightqa".equals(world.getWorldInfo().getWorldName())) throw new IllegalStateException("Wrong world");
        world.getGameRules().setOrCreateGameRule("doMobSpawning","false");world.getGameRules().setOrCreateGameRule("randomTickSpeed","0");
        MinecraftForge.EVENT_BUS.register(this);fixtures.add(new Fixture(true,2048));fixtures.add(new Fixture(false,4096));
        roundtripTests();
        if(Boolean.getBoolean("wartech.samexitcheck")) {
            Fixture owner=fixtures.get(1);LegacySamGeometry.Shot shot=LegacySamGeometry.shot(false,1,0,0,new Vec3d(owner.pos.getX()+60.5,4,owner.pos.getZ()+.5));
            int cx=((int)Math.floor(shot.origin.x))>>4,cz=((int)Math.floor(shot.origin.z))>>4;
            for(int dx=-2;dx<=2;dx++) for(int dz=-2;dz<=2;dz++) world.getChunkFromChunkCoords(cx+dx,cz+dz);
            EntityWarTechMissile abort=LegacyEntityFactory.missile(world,MissileProfile.ANTI_AIR_TIER_3);
            abort.setPosition(shot.origin.x,shot.origin.y,shot.origin.z);abort.configureSamVisual(shot.scale,shot.direction,shot.scale[1]+.25F);
            abort.motionY=1.2;abort.rotationPitch=-90;abort.setOwnerIdentity(null,"samqa-blue");abort.getEntityData().setBoolean("ForcedSamAbort",true);
            check("forced_sam_abort_spawn",MissileChunkLoader.spawnFlight(abort),"native targetless launch exercises deterministic abort transition");flights.add(new Flight(abort,owner));
        }
    }
    private void target(double x,double y,double z) {
        world.getChunkFromChunkCoords((int)x>>4,(int)z>>4);
        CruiseBuild build=CruiseBuild.starter(CruisePartDefinition.BODY_LONG_RANGE);build.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);
        ItemStack stack=new ItemStack(WarTechContent.ASSEMBLED_CRUISE);build.writeToStack(stack);CruiseMission mission=new CruiseMission();mission.setTarget(new Vec3d(x+300,4,z),0);mission.writeToStack(stack);
        EntityCustomCruise target=new EntityCustomCruise(world);target.setEntityId(91000+targets.size());target.setPosition(x,y,z);target.configure(stack,null);target.setOwnerIdentity(new UUID(91,targets.size()),"samqa-red");target.updateBlocked=true;
        target.ticksExisted=100;if(!MissileChunkLoader.spawnFlight(target))throw new IllegalStateException("Target flight rejected");MissileTrackingService.registerLaunch(target,x,y,z,(int)x,(int)z,"samqa-red");targets.add(target);
    }
    private void roundtripTests() throws Exception {
        java.lang.reflect.Method step=TileEntityWarTechMachine.class.getDeclaredMethod("tickSam");step.setAccessible(true);
        for(Fixture f:fixtures) {
            NBTTagCompound initial=new NBTTagCompound();f.tile.writeToNBT(initial);
            initial.setInteger("SamDeployment",64);initial.setFloat("SamYaw",0);f.tile.readFromNBT(initial);
            NBTTagCompound saved=new NBTTagCompound();f.tile.writeToNBT(saved);
            TileEntityWarTechMachine restored=LegacyTileTypes.createMachine(f.patriot?"PatriotLauncher":"S400Launcher");restored.setWorld(world);restored.setPos(f.pos);restored.readFromNBT(saved);
            check(f.id()+"_mid_deploy_restore",restored.getSamDeployment(1)==f.tile.getSamDeployment(1) && restored.getStackInSlot(0).getCount()==8,"pose and inventory");
            stow(restored);for(int i=0;i<130;i++) step.invoke(restored);
            check(f.id()+"_cancel_to_stow",restored.getSamDeployment(1)==0 && !restored.isSamEnabled(),"reversible cancellation at midpoint");
            saved.setInteger("SamDeployment",120);saved.setFloat("SamYaw",179);saved.setBoolean("SamEnabled",false);restored.readFromNBT(saved);step.invoke(restored);
            check(f.id()+"_home_before_lower",!f.patriot || restored.getSamDeployment(1)==1 && Math.abs(restored.getSamYaw(1))==179-AirDefenseTraverse.PATRIOT_YAW,"yaw returns before package lowers");
            NBTTagCompound old=initial.copy();for(String key:new String[]{"SamDeployment","SamYaw","SamEnabled","SamChannel","SamNextChannel","SamCap","SamHold"}) old.removeTag(key);
            restored.readFromNBT(old);check(f.id()+"_old_world_migration",restored.getSamDeployment(1)==1 && restored.isSamEnabled(),"old static machines stay deployed");
            initial.setInteger("SamDeployment",0);initial.setFloat("SamYaw",0);f.tile.readFromNBT(initial);restored.invalidate();
        }
    }
    @SubscribeEvent public void joined(EntityJoinWorldEvent event) {
        if(event.getWorld()!=world || !(event.getEntity() instanceof EntityWarTechMissile)) return;
        EntityWarTechMissile missile=(EntityWarTechMissile)event.getEntity();if(!missile.getEntityData().hasKey("SamLauncher")) return;
        for(Fixture f:fixtures) if(f.pos.toLong()==missile.getEntityData().getLong("SamLauncher")) {
            int channel=missile.getEntityData().getInteger("SamChannel");LegacySamGeometry.Shot expected=LegacySamGeometry.shot(f.patriot,1,f.tile.getSamYaw(1),channel,new Vec3d(f.pos.getX()+.5,f.pos.getY(),f.pos.getZ()+.5));
            check(f.id()+"_socket_"+f.shots,expected.origin.distanceTo(missile.getPositionVector())<1e-5,"channel "+channel+" origin "+missile.getPositionVector());
            check(f.id()+"_ready_"+f.shots,f.tile.getSamDeployment(1)==1 && f.tile.getSamCap(1)==1,"no firing through closed lid or moving frame");
            if(Boolean.getBoolean("wartech.samexitcheck")) check(f.id()+"_sam_visual_"+f.shots,missile.getModernVisual().getBoolean("SamLaunch"),"departure fit synchronized with entity metadata");
            flights.add(new Flight(missile,f));f.channels.add(channel);f.shots++;
        }
    }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) throws Exception {
        if(event.phase!=TickEvent.Phase.END) return;tick++;
        for(Flight f:flights) if(!f.checked) {
            int age=f.missile.getModernAge();
            if(age>0 && age<=f.exit) {
                Vec3d expected=f.origin.add(f.axis.scale(age*1.2));
                if(f.missile.getPositionVector().distanceTo(expected)>.001) { check(f.owner.id()+"_continuous_exit_"+f.owner.shots,false,f.missile.getPositionVector()+" expected "+expected);f.checked=true; }
            }
            if(age>=f.exit || f.missile.isDead) { check(f.owner.id()+"_clears_own_block_"+flights.indexOf(f),age>=f.exit,"age "+age+" / "+f.exit);f.checked=true; }
        }
        for(Flight f:flights) if(!f.missile.isDead && f.missile.getModernAge()<=f.exit+12) {
            JsonObject row=new JsonObject();row.addProperty("launcher",f.owner.id());row.addProperty("missile",f.missile.getEntityId());
            row.addProperty("launcherYaw",f.launchYaw);row.addProperty("channel",f.missile.getEntityData().getInteger("SamChannel"));row.addProperty("age",f.missile.getModernAge());row.addProperty("exit",f.exit);
            row.addProperty("forcedAbort",f.missile.getEntityData().getBoolean("ForcedSamAbort"));
            row.addProperty("x",f.missile.posX);row.addProperty("y",f.missile.posY);row.addProperty("z",f.missile.posZ);
            row.addProperty("vx",f.missile.motionX);row.addProperty("vy",f.missile.motionY);row.addProperty("vz",f.missile.motionZ);
            row.addProperty("yaw",f.missile.rotationYaw);row.addProperty("pitch",f.missile.rotationPitch);exitTrace.add(row);
        }
        if(Boolean.getBoolean("wartech.samexitcheck")) for(Flight f:flights) {
            int age=f.missile.getModernAge();Vec3d motion=new Vec3d(f.missile.motionX,f.missile.motionY,f.missile.motionZ);
            if(age>f.previousAge && f.previousMotion!=null && age>f.exit && age<=f.exit+16) {
                double dot=motion.normalize().dotProduct(f.previousMotion.normalize());
                f.maxTurn=Math.max(f.maxTurn,Math.toDegrees(Math.acos(Math.max(-1,Math.min(1,dot)))));
                f.maxAcceleration=Math.max(f.maxAcceleration,Math.abs(motion.lengthVector()-f.previousMotion.lengthVector()));f.handoffSteps++;
            }
            if(age==f.exit && f.previousAge!=age) {
                Vec3d mouth=f.origin.add(f.axis.scale(f.missile.getModernVisual().getFloat("ScaleY")*.5));
                Vec3d tail=f.missile.getPositionVector().subtract(f.axis.scale(f.missile.getModernVisual().getFloat("ScaleY")*.5));
                check(f.owner.id()+"_tail_clearance_"+flights.indexOf(f),tail.subtract(mouth).dotProduct(f.axis)>.24,"tail clear before guidance / fins unfold");
            }
            if(!f.handoffChecked && (age>=f.exit+16 || f.missile.isDead)) {
                check(f.owner.id()+"_handoff_"+flights.indexOf(f),f.handoffSteps>0 && f.maxTurn<=12.001 && f.maxAcceleration<=1.8001,"steps="+f.handoffSteps+" maxTurn="+f.maxTurn+" maxAcceleration="+f.maxAcceleration);
                f.handoffChecked=true;
            }
            if(age>f.previousAge) {f.previousAge=age;f.previousMotion=motion;}
        }
        for(Fixture f:fixtures) {
            if(f.shots>=4 && !f.folded) { stow(f.tile);f.folded=true; }
            // Model an external power supply instead of exhausting the buffer.
            f.tile.setPower(f.tile.getMaxPower());
            if(tick%100==0) { NBTTagCompound state=new NBTTagCompound();f.tile.writeToNBT(state);System.out.println("SAMQA state "+tick+" "+f.id()+" shots="+f.shots+" pose="+state.getInteger("SamDeployment")+" yaw="+state.getFloat("SamYaw")+" cap="+state.getInteger("SamCap")+" pending="+state.getInteger("shoot")); }
            if(f.folded && f.tile.getSamDeployment(1)<1) checkOnce(f.id()+"_safe_lower",f.tile.getSamCap(1)==0 && Math.abs(f.tile.getSamYaw(1))<.01,"lids shut and yaw home before lowering");
        }
        boolean finished=fixtures.stream().allMatch(f->f.folded && f.tile.getSamDeployment(1)==0);
        if(doneAt<0 && (finished || tick>900)) {
            for(Fixture f:fixtures) {
                check(f.id()+"_four_channels",f.channels.size()==4,f.channels);
                check(f.id()+"_stow_complete",f.tile.getSamDeployment(1)==0 && !f.tile.isSamEnabled(),"real world ticks "+tick);
                check(f.id()+"_one_item_per_launch",f.tile.getStackInSlot(0).getCount()==8-f.shots,"ammo "+f.tile.getStackInSlot(0).getCount()+" shots "+f.shots);
                world.setBlockToAir(f.pos);
            }
            for(Entity e:targets)e.setDead();for(Flight f:flights)f.missile.setDead();doneAt=tick;
        }
        if(doneAt>=0 && tick-doneAt>=100) {
            JsonObject out=new JsonObject();out.addProperty("complete",true);out.add("cases",checks);out.add("failures",new Gson().toJsonTree(failures));out.addProperty("resources",MissileChunkLoader.diagnostics(world));
            Files.write(Paths.get("flightqa-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(out).getBytes(StandardCharsets.UTF_8));
            Files.write(Paths.get("sam-exit-trace.json"),new GsonBuilder().setPrettyPrinting().create().toJson(exitTrace).getBytes(StandardCharsets.UTF_8));
            System.out.println("SAMQA finished checks="+checks.size()+" failures="+failures);world.getMinecraftServer().initiateShutdown();
        }
    }
    private final Set<String> once=new HashSet<>();
    private void checkOnce(String name,boolean success,Object detail) { if(once.add(name))check(name,success,detail); }
}
