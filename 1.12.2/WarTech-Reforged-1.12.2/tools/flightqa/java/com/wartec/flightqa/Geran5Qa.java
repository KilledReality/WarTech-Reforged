package com.wartec.flightqa;

import com.google.gson.*;
import com.hbm.items.ModItems;
import com.mojang.authlib.GameProfile;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gameplay.*;
import com.wartec.wartecmod.port.integration.*;
import com.wartec.wartecmod.port.network.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.*;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.*;
import net.minecraftforge.common.util.*;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Actual isolated platform launches, ticking flight, manual input and save identity. */
public final class Geran5Qa {
    private WorldServer world;private FakePlayer player;private int tick;
    private EntityWarTechMissile old,jet;private Vec3d manualStart;
    private final JsonArray cases=new JsonArray();private final List<String> failures=new ArrayList<>();
    private final BlockPos site=new BlockPos(20000,4,20000);
    public void started() {
        if(!Files.isRegularFile(Paths.get("FLIGHT_QA_ISOLATED"))) throw new IllegalStateException("Not isolated");
        world=FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0);
        if(!"flightqa".equals(world.getWorldInfo().getWorldName())) throw new IllegalStateException("Wrong world");
        world.getGameRules().setOrCreateGameRule("doMobSpawning","false");world.getGameRules().setOrCreateGameRule("randomTickSpeed","0");
        player=FakePlayerFactory.get(world,new GameProfile(new UUID(76,1),"GeranQA"));
        player.connection=new net.minecraft.network.NetHandlerPlayServer(world.getMinecraftServer(),
            new net.minecraft.network.NetworkManager(net.minecraft.network.EnumPacketDirection.SERVERBOUND),player);
        player.setPosition(0,4,0);player.capabilities.isCreativeMode=true;NetworkTeamHelper.setPlayerTeam(player,"blue");
        MinecraftForge.EVENT_BUS.register(this);
    }
    private void check(String name,boolean ok,String detail) {
        JsonObject c=new JsonObject();c.addProperty("kind",name);c.addProperty("success",ok);c.addProperty("detail",detail);cases.add(c);
        if(!ok) failures.add(name+": "+detail);
    }
    private void platform(BlockPos p,ItemStack ammo) {
        for(int x=(p.getX()>>4)-2;x<=(p.getX()>>4)+2;x++) for(int z=(p.getZ()>>4)-2;z<=(p.getZ()>>4)+2;z++) world.getChunkFromChunkCoords(x,z);
        int[] dim=WarTechContent.GERAN_LAUNCHER.getDimensions();
        for(int y=-dim[1];y<=dim[0];y++) for(int x=-dim[3];x<=dim[2];x++) for(int z=-dim[5];z<=dim[4];z++) world.setBlockToAir(p.add(x,y,z));
        world.setBlockState(p,WarTechContent.GERAN_LAUNCHER.getDefaultState());
        WarTechContent.GERAN_LAUNCHER.onBlockPlacedBy(world,p,world.getBlockState(p),player,new ItemStack(WarTechContent.GERAN_LAUNCHER));
        TileEntityWarTechMachine tile=(TileEntityWarTechMachine)world.getTileEntity(p);tile.setPower(100000);
        check("platform_accepts_"+ammo.getItem().getRegistryName().getResourcePath(),tile.isItemValidForSlot(0,ammo),"existing Geran catapult");
        tile.setInventorySlotContents(0,ammo);
        ItemStack d=new ItemStack(ModItems.designator_range);NBTTagCompound n=new NBTTagCompound();
        n.setInteger("xCoord",p.getX());n.setInteger("zCoord",p.getZ()+700);d.setTagCompound(n);tile.setInventorySlotContents(1,d);
        check("autonomous_command_queued",tile.launchFromDetonator(player),"XZ-only designator; no pilot activation");
    }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) throws Exception {
        if(event.phase!=TickEvent.Phase.END) return;tick++;
        if(tick==10) { platform(site,new ItemStack(WarTechContent.GERAN_5_DRONE));platform(site.add(80,0,0),new ItemStack(WarTechContent.GERAN_DRONE)); }
        if(tick==180) {
            for(Entity e:world.loadedEntityList) if(e instanceof EntityWarTechMissile) {
                EntityWarTechMissile m=(EntityWarTechMissile)e;if(m.getMissileProfile()==MissileProfile.GERAN_5) jet=m;
                if(m.getMissileProfile()==MissileProfile.GERAN_2) old=m;
            }
            check("both_launched",jet!=null && old!=null,"real platform entities");
            if(jet!=null && old!=null) {
                double fast=jet.posZ-site.getZ(),slow=old.posZ-site.getZ();
                check("faster_actual_flight",fast>slow*1.2 && fast>70,"jet="+fast+" old="+slow);
                check("finite_unobserved_flight",Double.isFinite(jet.posY) && jet.posY>5 && !jet.isRemoteControlled(),jet.getPositionVector().toString());
                check("higher_defense_class",MissileTrackingService.getThreatTier(jet)==2 && MissileTrackingService.getThreatTier(old)==1,"jet2 old1");
                NBTTagCompound n=new NBTTagCompound();
                check("save_supported",jet.writeToNBTOptional(n),"same entity save path used by chunks");
                Entity restored=net.minecraft.entity.EntityList.createEntityFromNBT(n,world);
                check("registered_save_identity",restored instanceof LegacyEntityTypes.Geran5Missile
                    && ((EntityWarTechMissile)restored).getMissileProfile()==MissileProfile.GERAN_5,"id="+n.getString("id"));
                check("saved_visual",restored instanceof EntityWarTechBase && ((EntityWarTechBase)restored).getVisualId().equals("missile/geran_5"),"distinct model after reload");
            }
        }
        if(tick==200 && jet!=null) {
            world.playerEntities.add(player);manualStart=jet.getPositionVector();
            check("manual_control_begin",jet.beginRemoteControl(player),"same Geran pilot mechanism");
            // FakePlayer is not on the server's player list; reproduce the player
            // chunk-map updates normally supplied by a connected pilot.
            world.getPlayerChunkMap().addPlayer(player);
            check("manual_telemetry_type",new RemoteControlTelemetryMessage(jet).vehicleType==8,"distinct HUD and camera");
        }
        if(tick>=201 && tick<250 && jet!=null) {
            world.getPlayerChunkMap().updateMovingPlayer(player);
            jet.handleRemoteInput(player,-40,-3,1,0);
        }
        if(tick==250 && jet!=null) {
            check("manual_input_changes_course",jet.isRemoteControlled() && jet.posX>manualStart.x+10
                && Math.hypot(jet.motionX,jet.motionZ)>1.3,"position="+jet.getPositionVector()+" speed="+Math.hypot(jet.motionX,jet.motionZ));
            jet.handleRemoteInput(player,jet.rotationYaw,jet.rotationPitch,1,2);
            check("manual_exit_reclaims_flight_ticket",MissileChunkLoader.diagnostics(world).contains("flights\u003d2/32"),
                MissileChunkLoader.diagnostics(world));
            world.getPlayerChunkMap().removePlayer(player);world.playerEntities.remove(player);
            check("manual_exit_autopilot",!jet.isRemoteControlled(),"return to saved target");
        }
        if(tick==280 && jet!=null) check("resume_finite",Double.isFinite(jet.posX) && Double.isFinite(jet.posY) && Double.isFinite(jet.posZ),jet.getPositionVector().toString());
        if(tick==1000) {
            check("autonomous_strike_reached_ground",jet!=null && jet.isDead && jet.posZ>site.getZ()+650 && jet.posY<10,
                jet==null?"missing":jet.getPositionVector().toString());
            if(jet!=null) jet.setDead();if(old!=null) old.setDead();
        }
        if(tick==1050) warheadRegression();
        if(tick==1240) {
            check("resources_released",OperationalChunks.pending(world)==0 && MissileChunkLoader.diagnostics(world).contains("queue=0 chunks/0 owners"),MissileChunkLoader.diagnostics(world));
            JsonObject result=new JsonObject();result.addProperty("complete",true);result.add("cases",cases);
            result.add("failures",new Gson().toJsonTree(failures));result.addProperty("resources",MissileChunkLoader.diagnostics(world));
            Files.write(Paths.get("flightqa-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result).getBytes(StandardCharsets.UTF_8));
            System.out.println("FLIGHTQA geran5 finished "+result);world.getMinecraftServer().initiateShutdown();
        }
    }
    private void warheadRegression() throws Exception {
        // Identical open-air targets, outside the Geran-2 blast but inside
        // the strengthened Geran-5 blast. Do not touch the user's worlds.
        java.lang.reflect.Method impact=EntityWarTechMissile.class.getDeclaredMethod("detonatePayload");impact.setAccessible(true);
        float[] remaining=new float[2];int index=0;
        for(MissileProfile profile:new MissileProfile[]{MissileProfile.GERAN_2,MissileProfile.GERAN_5}) {
            BlockPos center=site.add(200+index*160,36,0);
            for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) world.getChunkFromChunkCoords((center.getX()>>4)+x,(center.getZ()>>4)+z);
            net.minecraft.entity.passive.EntityVillager victim=new net.minecraft.entity.passive.EntityVillager(world);
            victim.setNoAI(true);victim.setPosition(center.getX()+16,center.getY(),center.getZ());world.spawnEntity(victim);
            EntityWarTechMissile missile=LegacyEntityFactory.missile(world,profile);missile.setPosition(center.getX(),center.getY(),center.getZ());
            world.rand.setSeed(7700);impact.invoke(missile);
            remaining[index]=victim.getHealth();victim.setDead();index++;
        }
        check("geran2_warhead_unchanged",remaining[0]==20,"villager 16 blocks away: hp="+remaining[0]);
        check("geran5_heavy_warhead_damage",remaining[1]<remaining[0],"same target hp old="+remaining[0]+" jet="+remaining[1]);
        check("exact_double_warhead_parameter",LegacyEntityFactory.missile(world,MissileProfile.GERAN_5).getGeranWarheadStrength()
            ==2*LegacyEntityFactory.missile(world,MissileProfile.GERAN_2).getGeranWarheadStrength(),"6 -> 12; nonlinear terrain-dependent damage");
    }
}
