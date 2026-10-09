package com.wartec.flightqa;

import com.google.gson.*;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.integration.*;
import com.wartec.wartecmod.port.uav.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.*;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Isolated production blast mechanics plus two real adjacent-target inbound missiles. */
public final class BlastQa {
    private final UUID owner=new UUID(7070,1);
    private final List<String> failures=new ArrayList<>();
    private final List<EntityCustomCruise> flights=new ArrayList<>();
    private final List<JsonObject> cases=new ArrayList<>();
    private MinecraftServer server;private WorldServer world;private int tick,done=-1,blasts;
    private double closest=Double.MAX_VALUE;
    public void started() {
        if(!Files.isRegularFile(Paths.get("FLIGHT_QA_ISOLATED"))) throw new IllegalStateException("Not isolated");
        server=FMLCommonHandler.instance().getMinecraftServerInstance();world=server.getWorld(0);
        if(!"flightqa".equals(world.getWorldInfo().getWorldName())) throw new IllegalStateException("Wrong world");
        world.getGameRules().setOrCreateGameRule("doMobSpawning","false");
        world.getGameRules().setOrCreateGameRule("randomTickSpeed","0");MinecraftForge.EVENT_BUS.register(this);
    }
    private void check(boolean pass,String what) { if(!pass) failures.add(what); }
    private EntityCustomCruise missile(double x,double z,String team,UUID identity) {
        CruiseBuild b=CruiseBuild.starter(CruisePartDefinition.BODY_CLASSIC);
        b.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);b.set(CruiseSlot.NAVIGATION,CruisePartDefinition.NAV_ROUTE);
        b.set(CruiseSlot.WARHEAD,CruisePartDefinition.WARHEAD_THERMOBARIC);
        check(b.calculateStats().isValid(),"Invalid test missile "+b.calculateStats().getErrors());
        CruiseMission m=new CruiseMission();m.setTarget(new Vec3d(x,4,8704),0);
        ItemStack s=new ItemStack(WarTechContent.ASSEMBLED_CRUISE);b.writeToStack(s);m.writeToStack(s);
        EntityCustomCruise e=new EntityCustomCruise(world);e.setPosition(x,96,z);e.configure(s,null);e.setOwnerIdentity(identity,team);e.motionZ=.8;return e;
    }
    private void fixture(int index,CruisePartDefinition part) {
        double x=4096+index*128,z=4096;
        for(int cx=((int)x>>4)-2;cx<=((int)x>>4)+2;cx++) for(int cz=((int)z>>4)-2;cz<=((int)z>>4)+2;cz++) world.getChunkFromChunkCoords(cx,cz);
        EntityCustomCruise source=missile(x,z,"blue",owner),friend=missile(x+4,z,"blue",owner),enemy=missile(x-4,z,"red",new UUID(7070,2));
        EntityCustomUav uav=new EntityCustomUav(world);uav.setPosition(x,96,z+5);
        uav.configure(UavBuild.starter(UavAirframe.ONE_WAY),null);uav.setOwnerIdentity(owner,"blue");uav.setArmed(true);
        EntityWarTechMissile geran=new EntityWarTechMissile(world,MissileProfile.GERAN_2);
        geran.setPosition(x,96,z-5);geran.setOwnerIdentity(new UUID(7070,3),"blue");geran.setArmed(true);
        EntityCow cow=new EntityCow(world);cow.setPosition(x+3,96,z+3);
        Entity[] targets={friend,enemy,uav,geran,cow};for(Entity e:targets) world.spawnEntity(e);
        float hp=friend.getHealthValue(),uhp=uav.getHealthValue();int energy=uav.getLegacyPower();Vec3d velocity=new Vec3d(friend.motionX,friend.motionY,friend.motionZ);
        int flags=geran.getFlightStage();
        CruisePayloadEffects.detonate(source,part,new Vec3d(0,0,1),false);
        check(!friend.isDead && friend.isDefenseTarget() && friend.getHealthValue()==hp,"Friendly cruise damaged by "+part);
        check(new Vec3d(friend.motionX,friend.motionY,friend.motionZ).equals(velocity),"Friendly cruise blast knockback "+part);
        check(!uav.isDead && uav.getHealthValue()==uhp,"Friendly strike UAV damaged by "+part);
        check(uav.getLegacyPower()==energy,"Friendly strike UAV disabled by EMP "+part);
        check(!geran.isDead && geran.getFlightStage()==flags,"Friendly Geran damaged by "+part);
        if(CruiseWarheads.effect(part)!=CruiseWarheads.Effect.EMP && CruiseWarheads.effect(part)!=CruiseWarheads.Effect.CLUSTER) {
            check(enemy.getHealthValue()<hp || !enemy.isDefenseTarget(),"Hostile cruise not damaged by "+part);
            check(cow.getHealth()<cow.getMaxHealth(),"Ground/living blast damage lost "+part);
        }
        JsonObject result=new JsonObject();result.addProperty("payload",part.getId());result.addProperty("friendly_health",friend.getHealthValue());
        result.addProperty("enemy_health",enemy.getHealthValue());cases.add(result);
        for(Entity e:targets) e.setDead();
        // Fixtures never become real flight jobs; their actual descendants are cleaned up.
        for(Entity e:new ArrayList<Entity>(world.loadedEntityList)) if(e.getDistanceSq(x,96,z)<1600 && !e.isDead) e.setDead();
    }
    private void secondaryAndDefenseChecks() {
        EntityCustomCruise source=missile(6144,6144,"blue",owner),friend=missile(6148,6144,"blue",owner);
        world.getChunkFromChunkCoords(384,384);
        com.hbm.entity.projectile.EntityShrapnel fragment=new com.hbm.entity.projectile.EntityShrapnel(world);
        fragment.setPosition(6144,96,6144);
        try(StrikeBlastSafety.Scope scope=StrikeBlastSafety.enter(source)) { world.spawnEntity(fragment); }
        NBTTagCompound saved=new NBTTagCompound();fragment.writeToNBT(saved);
        com.hbm.entity.projectile.EntityShrapnel restored=new com.hbm.entity.projectile.EntityShrapnel(world);restored.readFromNBT(saved);
        ProjectileImpactEvent event=new ProjectileImpactEvent.Throwable(restored,new RayTraceResult(friend));
        MinecraftForge.EVENT_BUS.post(event);check(event.isCanceled(),"Owned HBM fragment not filtered after NBT restore");
        EntityCustomCruise enemy=missile(6150,6144,"red",new UUID(7070,2));
        ProjectileImpactEvent hostile=new ProjectileImpactEvent.Throwable(restored,new RayTraceResult(enemy));
        MinecraftForge.EVENT_BUS.post(hostile);check(!hostile.isCanceled(),"Hostile fragment contact filtered");
        EntityWarTechArtilleryProjectile child=new EntityWarTechArtilleryProjectile(world);child.setPosition(6144,96,6144);
        try(StrikeBlastSafety.Scope scope=StrikeBlastSafety.enter(source)) { world.spawnEntity(child); }
        check(StrikeBlastSafety.ignores(friend,new EntityDamageSource("fragment",child).setProjectile()),"Cluster origin lost");
        world.spawnEntity(friend);float hp=friend.getHealthValue();
        try(StrikeBlastSafety.Scope scope=StrikeBlastSafety.enter(child)) { HbmExplosionCompat.advancedExplosion(world,6144,96,6144,8,1,false); }
        check(friend.getHealthValue()==hp && friend.isDefenseTarget(),"Delayed cluster blast damaged friendly inbound");
        world.createExplosion(null,6144,96,6144,8,false);
        check(friend.getHealthValue()<hp || !friend.isDefenseTarget(),"Scope leaked: unrelated explosion became immune");friend.setDead();
        friend=missile(6148,6144,"blue",owner);
        EntityWarTechMissile interceptor=new EntityWarTechMissile(world,MissileProfile.ANTI_AIR_TIER_3);
        interceptor.setOwnerIdentity(new UUID(7070,9),"red");
        check(friend.attackEntityFrom(new EntityDamageSource("air_defense",interceptor).setExplosion(),200),"PVO damage immune");
        check(!friend.isDefenseTarget(),"PVO stopped causing combat crash");
        source.setDead();friend.setDead();child.setDead();fragment.setDead();
    }
    private void launchPair() {
        for(int i=0;i<2;i++) {
            EntityCustomCruise e=missile(8192+i*8,8192-i*12,"blue",owner);e.setUniqueId(new UUID(7070,100+i));e.rotationYaw=0;
            world.getChunkFromChunkCoords((int)e.posX>>4,(int)e.posZ>>4);
            check(MissileChunkLoader.spawnFlight(e),"Pair spawn failed");flights.add(e);
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void blast(ExplosionEvent.Detonate event) {
        if(flights.size()!=2) return;
        blasts++;
        for(EntityCustomCruise e:flights) if(!e.isDead && e.isArmed() && e.isDefenseTarget())
            closest=Math.min(closest,e.getPositionVector().distanceTo(event.getExplosion().getPosition()));
    }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) throws Exception {
        if(event.phase!=TickEvent.Phase.END) return;tick++;
        if(tick==20) {
            int i=0;for(CruisePartDefinition part:CruisePartDefinition.values()) if(part.getSlot()==CruiseSlot.WARHEAD) fixture(i++,part);
            secondaryAndDefenseChecks();launchPair();System.out.println("FLIGHTQA blast fixtures="+cases.size()+" failures="+failures);
        }
        if(flights.size()==2 && flights.stream().allMatch(e->e.isDead) && done<0) done=tick;
        if(tick>3000 && done<0) { failures.add("Pair did not complete");done=tick;for(Entity e:flights)e.setDead(); }
        if(done>=0 && tick-done>200) {
            JsonObject result=new JsonObject();result.addProperty("complete",true);result.addProperty("tick",tick);result.add("failures",new Gson().toJsonTree(failures));
            result.add("payload_cases",new Gson().toJsonTree(cases));result.addProperty("blasts",blasts);result.addProperty("nearest_inbound_at_blast",closest);
            JsonArray impacts=new JsonArray();for(EntityCustomCruise e:flights) { NBTTagCompound n=new NBTTagCompound();e.writeToNBT(n);
                check(n.getBoolean("Detonated") && !n.getBoolean("Crashing"),"Pair member was shot down by neighboring strike");
                JsonObject row=new JsonObject();row.addProperty("detonated",n.getBoolean("Detonated"));row.addProperty("crashing",n.getBoolean("Crashing"));
                row.addProperty("x",e.posX);row.addProperty("y",e.posY);row.addProperty("z",e.posZ);impacts.add(row);
            }
            check(closest<30,"Pair did not exercise overlapping blast radius");
            String diagnostics=MissileChunkLoader.diagnostics(world);result.addProperty("resources",diagnostics);
            check(diagnostics.contains("flights=0/32") && diagnostics.contains("server queue=0 chunks/0 owners"),"Loader leak");
            result.add("impacts",impacts);result.add("failures",new Gson().toJsonTree(failures));
            Files.write(Paths.get("flightqa-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result).getBytes(StandardCharsets.UTF_8));
            System.out.println("FLIGHTQA blast finished failures="+failures);server.initiateShutdown();MinecraftForge.EVENT_BUS.unregister(this);
        }
    }
}
