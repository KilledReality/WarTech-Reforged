package com.wartec.flightqa;

import com.google.gson.*;
import com.wartec.wartecmod.port.content.WarTechContent;
import com.wartec.wartecmod.port.content.MissileProfile;
import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.integration.*;
import com.wartec.wartecmod.port.network.MissileTrackingService;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.*;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Native isolated WorldServer collisions, damage and reservation lifecycle. Never shipped. */
public final class InterceptorQa {
    private WorldServer world;private int tick,explosions;private Vec3d lastExplosion;
    private EntityCustomCruise crossingTarget;private EntityWarTechMissile crossingMissile;
    private final JsonArray cases=new JsonArray();private final List<String> failures=new ArrayList<>();
    public void started() {
        if(!Files.isRegularFile(Paths.get("FLIGHT_QA_ISOLATED"))) throw new IllegalStateException("Not isolated");
        world=FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0);
        if(!"flightqa".equals(world.getWorldInfo().getWorldName())) throw new IllegalStateException("Wrong world");
        world.getGameRules().setOrCreateGameRule("doMobSpawning","false");
        world.getGameRules().setOrCreateGameRule("randomTickSpeed","0");
        for(int x=-3;x<=3;x++) for(int z=-3;z<=3;z++) world.getChunkFromChunkCoords(x,z);
        MinecraftForge.EVENT_BUS.register(this);
    }
    @SubscribeEvent public void explosion(ExplosionEvent.Detonate event) {
        if(event.getWorld()==world) { explosions++;lastExplosion=event.getExplosion().getPosition(); }
    }
    private void check(String name,boolean ok,String detail) {
        JsonObject row=new JsonObject();row.addProperty("kind",name);row.addProperty("success",ok);row.addProperty("detail",detail);cases.add(row);
        if(!ok) failures.add(name+": "+detail);
    }
    private EntityCustomCruise target(double x,double y,double z) {
        CruiseBuild build=CruiseBuild.starter(CruisePartDefinition.BODY_LIGHT);build.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);
        ItemStack stack=new ItemStack(WarTechContent.ASSEMBLED_CRUISE);build.writeToStack(stack);
        CruiseMission mission=new CruiseMission();mission.setTarget(new Vec3d(x+300,3,z),0);mission.writeToStack(stack);
        EntityCustomCruise target=new EntityCustomCruise(world);target.setPosition(x,y,z);target.configure(stack,null);
        target.setOwnerIdentity(new UUID(75,2),"red");target.updateBlocked=true;world.spawnEntity(target);
        MissileTrackingService.registerLaunch(target,x,y,z,(int)x+300,(int)z,"red");return target;
    }
    private EntityWarTechMissile missile(double x,double y,double z,Entity target) {
        EntityWarTechMissile m=LegacyEntityFactory.missile(world,MissileProfile.ANTI_AIR_TIER_1);
        m.setPosition(x,y,z);m.setOwnerIdentity(new UUID(75,1),"blue");m.setTrackedEntity(target);
        m.ticksExisted=40;m.updateBlocked=true;MissileChunkLoader.spawnFlight(m);return m;
    }
    private void tests() throws Exception {
        EntityCustomCruise t=target(4,20,0);EntityWarTechMissile m=missile(0,20,0,t);
        int before=explosions;VlsInterceptorGuidance.tick(m,1);
        net.minecraft.nbt.NBTTagCompound saved=t.writeToNBT(new net.minecraft.nbt.NBTTagCompound());
        check("direct_body_hit",m.isDead && (t.isDead || saved.getBoolean("Crashing")),"missileDead="+m.isDead+" crashing="+saved.getBoolean("Crashing"));
        check("direct_hit_explosion",explosions>before,"explosions="+explosions);t.setDead();m.setDead();

        t=target(20,20,16);m=missile(0,20,16,t);
        for(int y=17;y<=23;y++) for(int z=14;z<=18;z++) world.setBlockState(new BlockPos(6,y,z),Blocks.PLANKS.getDefaultState());
        EntityVillager villager=new EntityVillager(world);villager.setPosition(4.8,20,18.5);villager.setNoAI(true);world.spawnEntity(villager);
        before=explosions;
        // Tier-dependent acceleration can leave the first step short of the wall.
        for(int step=0;step<4 && !m.isDead;step++) VlsInterceptorGuidance.tick(m,1);
        check("off_target_obstacle_blast",m.isDead && explosions==before+1 && lastExplosion.x>=5.99 && lastExplosion.x<=6.1,"impact="+lastExplosion);
        check("off_target_blast_damage",villager.getHealth()<villager.getMaxHealth(),"villager hp="+villager.getHealth());
        check("obstacle_not_successful_intercept",!t.isDead && !t.writeToNBT(new net.minecraft.nbt.NBTTagCompound()).getBoolean("Crashing"),"target behind wall survives");
        t.setDead();m.setDead();villager.setDead();

        t=target(16,30,32);m=missile(0,30,32,t);VlsInterceptorGuidance.tick(m,1);
        check("crossing_fixture_not_yet_hit",!m.isDead,"first swept step ends short of target");
        crossingTarget=t;crossingMissile=m;

        t=target(30,20,-16);m=missile(0,20,-16,t);m.motionX=9;
        java.lang.reflect.Method failed=VlsInterceptorGuidance.class.getDeclaredMethod("failedIntercept",net.minecraft.world.World.class,EntityWarTechMissile.class,Entity.class,int.class);
        failed.setAccessible(true);failed.invoke(null,world,m,t,1);
        check("miss_continues_visible_flight",!m.isDead && m.getTrackedEntityId()<0 && m.motionX>8,"velocity="+m.motionX);
        // A failed shot can subsequently hit another object; collision is swept even in abort mode.
        for(int y=19;y<=21;y++) for(int z=-18;z<=-14;z++) world.setBlockState(new BlockPos(5,y,z),Blocks.PLANKS.getDefaultState());
        before=explosions;VlsInterceptorGuidance.tick(m,1);
        check("miss_subsequent_obstacle_blast",m.isDead && explosions==before+1,"explosions="+explosions);t.setDead();m.setDead();

        t=target(30,30,0);m=missile(0,30,0,t);
        boolean reserved=MissileTrackingService.tryReserve(world,t.getEntityId(),75);
        MissileTrackingService.confirmReservation(world,t.getEntityId(),75,m.getEntityId());
        check("no_second_shot_same_live_target",reserved && !MissileTrackingService.tryReserve(world,t.getEntityId(),76),"one live interceptor owns engagement");
        m.setDead();check("dead_shot_not_immediately_replaced",!MissileTrackingService.tryReserve(world,t.getEntityId(),76),"failure reassessment cooldown");
        t.setDead();

        // Autopilot resume can occur between the eight-tick terrain scans.
        EntityWarTechMissile geran=LegacyEntityFactory.missile(world,MissileProfile.GERAN_2);
        geran.setPosition(0,20,48);geran.configureLegacyGroundLaunch(0,3,800);geran.ticksExisted=43;
        java.lang.reflect.Field cruiseY=EntityWarTechMissile.class.getDeclaredField("plannedCruiseY");
        cruiseY.setAccessible(true);cruiseY.setDouble(geran,Double.NaN);
        java.lang.reflect.Method autopilot=EntityWarTechMissile.class.getDeclaredMethod("tickGeran");
        autopilot.setAccessible(true);autopilot.invoke(geran);
        check("geran_autopilot_resume_finite",Double.isFinite(geran.posX) && Double.isFinite(geran.posY)
            && Double.isFinite(geran.posZ) && Double.isFinite(cruiseY.getDouble(geran)),"position="+geran.getPositionVector());
        geran.setDead();
    }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) throws Exception {
        if(event.phase!=TickEvent.Phase.END) return;tick++;
        if(tick==10) tests();
        if(tick==11) {
            crossingTarget.setPosition(crossingMissile.posX*.5,30,32+(crossingMissile.posZ-32)*.5);
            VlsInterceptorGuidance.tick(crossingMissile,1);
            check("actual_crossing_hit_registered",crossingMissile.isDead && crossingTarget.writeToNBT(new net.minecraft.nbt.NBTTagCompound()).getBoolean("Crashing"),"tick-order independent moving body hit");
            crossingTarget.setDead();crossingMissile.setDead();
        }
        if(tick==80) {
            JsonObject out=new JsonObject();out.addProperty("complete",true);out.add("cases",cases);out.add("failures",new Gson().toJsonTree(failures));
            out.addProperty("resources",MissileChunkLoader.diagnostics(world));
            Files.write(Paths.get("flightqa-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(out).getBytes(StandardCharsets.UTF_8));
            System.out.println("FLIGHTQA interceptor finished "+out);world.getMinecraftServer().initiateShutdown();
        }
    }
}
