package com.wartec.flightqa;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gui.*;
import com.wartec.wartecmod.port.integration.*;
import com.wartec.wartecmod.port.network.MissileTrackingService;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Actual isolated Forge ticks: launch, independent ammo, remote flight and live HBM payload. */
public final class StrategicQa {
    private WorldServer world;private Pilot player,enemy;private int tick,boosts,entries,tacticalSpawns,nukes,clouds;
    private EntityStrategicTel topol,oreshnik,iskander,farTel;
    private EntityWarTechMissile farIskander;
    private boolean supportStayedStable=true;
    private int farCleanupTick=-1;
    private final JsonArray cases=new JsonArray();private final List<String> failures=new ArrayList<>();
    private final Set<UUID> seen=new HashSet<>();
    private EntityWarTechMissile firstIskander,secondIskander;
    private int oreshnikDamageBlocks;
    private static final class Pilot extends FakePlayer {
        Pilot(WorldServer w,long id) { super(w,new GameProfile(new UUID(80,id),"StrategicQA"+id)); }
        @Override public void sendMessage(ITextComponent text) { }
        @Override public Vec3d getPositionVector() { return new Vec3d(posX,posY,posZ); }
    }
    public void started() {
        if(!Files.isRegularFile(Paths.get("FLIGHT_QA_ISOLATED"))) throw new IllegalStateException("Not isolated");
        world=FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0);
        if(!"flightqa".equals(world.getWorldInfo().getWorldName())) throw new IllegalStateException("Wrong world");
        world.getGameRules().setOrCreateGameRule("doMobSpawning","false");world.getGameRules().setOrCreateGameRule("randomTickSpeed","0");
        player=new Pilot(world,1);enemy=new Pilot(world,2);NetworkTeamHelper.setPlayerTeam(player,"blue");NetworkTeamHelper.setPlayerTeam(enemy,"red");
        MinecraftForge.EVENT_BUS.register(this);
    }
    private void check(String name,boolean ok,String detail) {
        JsonObject row=new JsonObject();row.addProperty("kind",name);row.addProperty("success",ok);row.addProperty("detail",detail);cases.add(row);
        if(!ok) failures.add(name+": "+detail);
        System.out.println("STRATEGICQA "+row);
    }
    @SubscribeEvent public void spawned(EntityJoinWorldEvent e) {
        if(e.getWorld()!=world) return;Entity entity=e.getEntity();String name=entity.getClass().getSimpleName();
        if(entity instanceof EntityStrategicMissile) {
            if(((EntityStrategicMissile)entity).isReentryVehicle()) entries++;else boosts++;
        }
        if(entity instanceof EntityWarTechMissile && ((EntityWarTechMissile)entity).getMissileProfile()==MissileProfile.ISKANDER) {
            if(farTel!=null && ((EntityWarTechMissile)entity).getTelLaunchOrigin().launcherId==farTel.getEntityId()) farIskander=(EntityWarTechMissile)entity;
            else { tacticalSpawns++;if(firstIskander==null) firstIskander=(EntityWarTechMissile)entity;else if(secondIskander==null) secondIskander=(EntityWarTechMissile)entity; }
        }
        if(name.contains("NukeExplosion")) nukes++;
        if(name.contains("NukeTorex")) clouds++;
        if(entity instanceof TelLaunchOrigin.Rocket && ((TelLaunchOrigin.Rocket)entity).getTelLaunchOrigin().launcherId>=0) {
            TelLaunchOrigin origin=((TelLaunchOrigin.Rocket)entity).getTelLaunchOrigin();
            Entity launcher=world.getEntityByID(origin.launcherId);
            if(launcher instanceof EntityStrategicTel) {
                EntityStrategicTel tel=(EntityStrategicTel)launcher;StrategicSystemProfile system=tel.getStrategicSystem();
                check(system.getId()+"_handoff_speed",Math.abs(entity.motionY-system.separationSpeed())<1e-9,"last animated step = initial entity velocity");
                check(system.getId()+"_handoff_position",entity.getPositionVector().squareDistanceTo(tel.getLaunchPosition(origin.slot))<1e-9,"no position jump at spawn");
                check(system.getId()+"_handoff_rotation",Math.abs(entity.rotationYaw-(system.isTactical()?180-tel.rotationYaw:tel.rotationYaw-180))<1e-5,"same mesh facing before/after");
            }
        }
    }
    private void near(Entity entity) { player.setPosition(entity.posX+2,entity.posY,entity.posZ+2);enemy.setPosition(entity.posX+2,entity.posY,entity.posZ+2); }
    private EntityStrategicTel tel(StrategicSystemProfile system,double x) {
        for(int cx=((int)x>>4)-2;cx<=((int)x>>4)+2;cx++) for(int cz=1998;cz<=2002;cz++) world.getChunkFromChunkCoords(cx,cz);
        EntityStrategicTel t=new EntityStrategicTel(world,system);t.setPosition(x,4,32000);t.setOwnerIdentity(player.getUniqueID(),"blue");world.spawnEntity(t);
        near(t);check(system.getId()+"_starts_empty",t.getMissileMask()==0,"no free missiles on placement");
        ItemStack ammo=new ItemStack(system.isTactical()?WarTechContent.ITEM_ISKANDER_MISSILE:WarTechContent.STRATEGIC_MISSILE,system.isTactical()?2:1,system.isTactical()?0:system.ordinal());
        player.setHeldItem(EnumHand.MAIN_HAND,ammo);t.processInitialInteract(player,EnumHand.MAIN_HAND);
        if(system.isTactical()) t.processInitialInteract(player,EnumHand.MAIN_HAND);
        check(system.getId()+"_real_ammo_loaded",ammo.isEmpty() && t.getMissileMask()==(system.isTactical()?3:1),"consumed held items once");
        check(system.getId()+"_iff_denied",!t.handleLegacyGuiAction(1,enemy),"enemy cannot deploy");
        t.programTarget(player,(int)x+2200,3,32000);
        if(system.isTactical()) t.programTarget(player,1,(int)x+2200,3,32000);
        check(system.getId()+"_not_deployed_no_launch",!t.beginLaunch(player),"setup required");
        return t;
    }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent e) throws Exception {
        if(e.phase!=TickEvent.Phase.END) return;tick++;
        if(tick==10) {
            topol=tel(StrategicSystemProfile.TOPOL_M,32000);oreshnik=tel(StrategicSystemProfile.ORESHNIK,32300);iskander=tel(StrategicSystemProfile.ISKANDER,32600);
            farTel=tel(StrategicSystemProfile.ISKANDER,33000);near(farTel);farTel.programTarget(player,40800,3,32000);
            check("iskander_range_matches_existing_weapon",StrategicSystemProfile.ISKANDER.getMaximumRange()==WeaponBalance.missileRange(MissileProfile.ISKANDER),"8000 blocks, no separate weaker TEL ammunition");
            world.setBlockState(new BlockPos(32650,4,32000),Blocks.STONE_SLAB.getDefaultState());
            EntityStrategicTel probe=new EntityStrategicTel(world,StrategicSystemProfile.ISKANDER);probe.setPosition(32650.5,4.5,32000.5);probe.onGround=false;
            check("slab_support_without_onGround",probe.hasGroundSupport(),"partial collision shape under tyres");
            probe.setPosition(32650.5,5,32000.5);check("airborne_launcher_rejected",!probe.hasGroundSupport(),"air gap is not ground");
            check("tactical_second_tube_real_item",iskander.getStackInSlot(1).getItem()==WarTechContent.ITEM_ISKANDER_MISSILE,"existing registry item");
            // A visible target structure lets native kinetic damage be measured.
            for(int cx=2151;cx<=2160;cx++) for(int cz=1993;cz<=2006;cz++) world.getChunkFromChunkCoords(cx,cz);
            for(int x=34400;x<=34600;x+=4) for(int z=31900;z<=32100;z+=4) world.setBlockState(new BlockPos(x,4,z),Blocks.STONE.getDefaultState());
        }
        if(tick==15) {
            for(EntityStrategicTel t:new EntityStrategicTel[]{topol,oreshnik,iskander,farTel}) {
                t.onGround=false;
                check(t.getStrategicSystem().getId()+"_support_ignores_unstable_flag",t.hasGroundSupport(),"solid terrain, vanilla flag deliberately false");
                near(t);check(t.getStrategicSystem().getId()+"_deploy",t.handleLegacyGuiAction(1,player),"grounded deployment begins after settling");
            }
        }
        if(tick==125) {
            check("stationary_support_stable_100_ticks",supportStayedStable,"real collision checks across alternating vanilla ground state");
            for(EntityStrategicTel t:new EntityStrategicTel[]{topol,oreshnik,iskander,farTel}) {
                near(t);check(t.getStrategicSystem().getId()+"_fully_deployed",t.getErectionProgress()==100,"original animation duration");
                ContainerLegacyEntity c=new ContainerLegacyEntity(player.inventory,t,ContainerLegacyEntity.Layout.STRATEGIC);
                check(t.getStrategicSystem().getId()+"_deployed_inventory_locked",!c.getSlot(0).canTakeStack(player) && c.transferStackInSlot(player,0).isEmpty(),"click and shift-click locked");
                check(t.getStrategicSystem().getId()+"_launch_started",t.beginLaunch(player),"real GUI action path");
                check(t.getStrategicSystem().getId()+"_duplicate_launch_denied",!t.beginLaunch(player),"one pending sequence");
                check(t.getStrategicSystem().getId()+"_target_locked",!t.programTarget(player,1,64,1),"no retargeting during sequence");
                check(t.getStrategicSystem().getId()+"_ammo_still_present",!t.getStackInSlot(0).isEmpty(),"consumption waits for separation");
            }
            player.setPosition(0,4,0);enemy.setPosition(0,4,0);
        }
        if(tick>=20 && tick<125) for(EntityStrategicTel t:new EntityStrategicTel[]{topol,oreshnik,iskander,farTel}) supportStayedStable&=t.hasGroundSupport();
        if(tick==260) {
            check("exactly_two_strategic_boosts",boosts==2,"boosts="+boosts);
            check("one_tactical_spawn",tacticalSpawns==1,"spawns="+tacticalSpawns);
            check("first_tube_only_consumed",iskander.getMissileMask()==2 && iskander.getStackInSlot(0).isEmpty() && !iskander.getStackInSlot(1).isEmpty(),"right remains loaded");
            check("strategic_canisters_consumed",topol.getMissileMask()==0 && oreshnik.getMissileMask()==0,"one item each");
            near(iskander);check("right_tube_launch_started",iskander.beginLaunch(player,1),"independent second launch");player.setPosition(0,4,0);
            for(Entity entity:new ArrayList<>(world.loadedEntityList)) if(entity instanceof EntityStrategicMissile && !entity.isDead
                    && ((EntityStrategicMissile)entity).getStrategicSystem()==StrategicSystemProfile.TOPOL_M) {
                EntityStrategicMissile target=(EntityStrategicMissile)entity;
                check("strategic_radar_tier",MissileTrackingService.getThreatTier(target)==3 && MissileTrackingService.isBallisticTarget(target),"tier3 ballistic");
                Vec3d before=target.getPositionVector();
                for(int shot=0;shot<6 && !target.isFallingDebris();shot++) {
                    EntityWarTechMissile interceptor=LegacyEntityFactory.missile(world,MissileProfile.ANTI_AIR_TIER_3);
                    interceptor.setPosition(target.posX-1,target.posY+1,target.posZ);interceptor.setOwnerIdentity(enemy.getUniqueID(),"red");
                    interceptor.setTrackedEntity(target);interceptor.ticksExisted=40;interceptor.updateBlocked=true;MissileChunkLoader.spawnFlight(interceptor);
                    VlsInterceptorGuidance.tick(interceptor,3);interceptor.setDead();
                }
                check("native_interceptor_creates_falling_debris",target.isFallingDebris() && !target.isDead && !target.isArmed(),"visible falling wreck, no nuclear strike");
                check("wreck_not_reengaged",MissileTrackingService.getThreatTier(target)==0 && !MissileTrackingService.isBallisticTarget(target),"radar releases wreck");
            }
        }
        if(tick==350) {
            check("two_real_iskander_entities",tacticalSpawns==2 && firstIskander!=secondIskander,"spawns="+tacticalSpawns);
            check("iskander_native_payload_preserved",firstIskander!=null && firstIskander.getMissileSpecification()==LegacyMissileSpecification.ISKANDER && farIskander!=null && farIskander.getMissileSpecification()==firstIskander.getMissileSpecification(),"same native ISKANDER payload for both ranges");
            check("both_tubes_consumed",iskander.getMissileMask()==0,"no duplicate store");
            near(iskander);check("stow_after_launch",iskander.handleLegacyGuiAction(1,player),"normal return animation");
            NBTTagCompound nbt=new NBTTagCompound();iskander.writeToNBT(nbt);
            EntityStrategicTel copy=new EntityStrategicTel(world,StrategicSystemProfile.ISKANDER);copy.readFromNBT(nbt);
            check("launcher_save_roundtrip",copy.getProfile()==WarTechEntityProfile.TACTICAL_ISKANDER && copy.getMissileMask()==0
                && copy.hasGuidanceTarget(),"profile, empty inventory and target retained");player.setPosition(0,4,0);
        }
        if(tick==600 || tick==1000 || tick==1300) {
            for(EntityWarTechMissile missile:new EntityWarTechMissile[]{firstIskander,secondIskander}) if(missile!=null) {
                NBTTagCompound debug=new NBTTagCompound();missile.writeToNBT(debug);
                System.out.println("STRATEGICQA flight diagnostic tick="+tick+" dead="+missile.isDead+" position="+missile.getPositionVector()+" saved="+debug);
            }
        }
        if(tick==1050) {
            check("seven_remote_oreshnik_reentries",entries==7,"entries="+entries);
            check("intercepted_topol_scheduled_no_warhead",StrategicFlightData.get(world).writeToNBT(new NBTTagCompound()).getTagList("Flights",10).tagCount()==0,"queue drained after Oreshnik");
            int damaged=0;
            for(int x=34400;x<=34600;x+=4) for(int z=31900;z<=32100;z+=4) if(world.isAirBlock(new BlockPos(x,4,z))) damaged++;
            check("kinetic_impacts_destroy_structure",damaged>60,"destroyed stone samples="+damaged);oreshnikDamageBlocks=damaged;
        }
        if(tick==1400) {
            // Separately exercise the actual Topol nuclear payload in the disposable world.
            world.getChunkFromChunkCoords(2400,2000);
            EntityStrategicMissile nuclear=new EntityStrategicMissile(world);nuclear.setPosition(38400,10,32000);
            nuclear.configureReentry(StrategicSystemProfile.TOPOL_M,0,38400,3,32000,player.getUniqueID(),"blue");
            MissileChunkLoader.spawnFlight(nuclear);
        }
        if(tick==1450) {
            check("native_nuclear_crater_entity",nukes>0,"NTM deferred nuclear explosions="+nukes);
            check("native_nuclear_torex_cloud",clouds>0,"NTM Torex effects="+clouds);
            if(firstIskander!=null) check("iskander_finite_flight",firstIskander.isDead && firstIskander.getDistanceSq(34800,3,32000)<100,"dead="+firstIskander.isDead+" position="+firstIskander.getPositionVector()+" ticks="+firstIskander.ticksExisted);
            if(secondIskander!=null) check("second_iskander_reaches_target",secondIskander.isDead && secondIskander.getDistanceSq(34800,3,32000)<100,"dead="+secondIskander.isDead+" position="+secondIskander.getPositionVector()+" ticks="+secondIskander.ticksExisted);
            for(Entity entity:new ArrayList<>(world.loadedEntityList)) if(entity!=farIskander && (entity instanceof EntityWarTechBase || entity.getClass().getName().startsWith("com.hbm.entity."))) entity.setDead();
        }
        if(tick>=1530 && farCleanupTick<0 && (farIskander!=null && farIskander.isDead || tick>=6500)) {
            check("iskander_real_7800_block_flight",farIskander!=null && farIskander.isDead && farIskander.getDistanceSq(40800,3,32000)<100,
                farIskander==null?"missing":"dead="+farIskander.isDead+" position="+farIskander.getPositionVector());
            for(Entity entity:new ArrayList<>(world.loadedEntityList)) if(entity instanceof EntityWarTechBase || entity.getClass().getName().startsWith("com.hbm.entity.")) entity.setDead();
            farCleanupTick=tick;
        }
        if(tick>=2000 && tick%500==0 && farIskander!=null && !farIskander.isDead) {
            NBTTagCompound state=new NBTTagCompound();farIskander.writeToNBT(state);
            System.out.println("STRATEGICQA far progress tick="+tick+" position="+farIskander.getPositionVector()+" flightTick="+state.getInteger("TelFlightTicks"));
        }
        if(farCleanupTick>0 && tick==farCleanupTick+80) {
            String resources=MissileChunkLoader.diagnostics(world);
            check("flight_resources_released",resources.contains("flights=0/") && resources.contains("queue=0 chunks/0 owners"),resources);
            JsonObject out=new JsonObject();out.addProperty("complete",true);out.add("cases",cases);out.add("failures",new Gson().toJsonTree(failures));out.addProperty("resources",resources);
            Files.write(Paths.get("flightqa-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(out).getBytes(StandardCharsets.UTF_8));
            System.out.println("FLIGHTQA strategic finished failures="+failures);world.getMinecraftServer().initiateShutdown();
        }
    }
}
