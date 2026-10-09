package com.wartec.flightqa;

import com.google.gson.*;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.integration.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.*;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Real ticking villagers and delayed cluster children, in an isolated world only. */
public final class PayloadQa {
    private MinecraftServer server; private WorldServer world;
    private int tick,index=-1,children,blasts,hurt;
    private final List<CruisePartDefinition> parts=new ArrayList<>();
    private final List<EntityVillager> villagers=new ArrayList<>();
    private final List<String> failures=new ArrayList<>();
    private final List<JsonObject> records=new ArrayList<>();
    private final UUID owner=new UUID(7171,1);
    private final List<String> blastPositions=new ArrayList<>();
    private JsonArray launcherCases;
    private final List<EntityWarTechArtilleryProjectile> bomblets=new ArrayList<>();
    public void started() {
        if(!Files.isRegularFile(Paths.get("FLIGHT_QA_ISOLATED"))) throw new IllegalStateException("Not isolated");
        server=FMLCommonHandler.instance().getMinecraftServerInstance();world=server.getWorld(0);
        if(!"flightqa".equals(world.getWorldInfo().getWorldName())) throw new IllegalStateException("Wrong world");
        if(!server.getCanSpawnNPCs()) throw new IllegalStateException("Payload QA requires NPCs enabled: otherwise villagers disappear before delayed blasts");
        world.getGameRules().setOrCreateGameRule("doMobSpawning","false");
        world.getGameRules().setOrCreateGameRule("randomTickSpeed","0");
        for(CruisePartDefinition p:CruisePartDefinition.values()) if(p.getSlot()==CruiseSlot.WARHEAD) parts.add(p);
        parts.add(CruisePartDefinition.WARHEAD_CLUSTER);parts.add(CruisePartDefinition.WARHEAD_HEAVY_CLUSTER);
        for(int x=-4;x<=4;x++) for(int z=-4;z<=4;z++) world.getChunkFromChunkCoords(x,z);
        launcherCases=Boolean.getBoolean("wartech.payloadqa.launchers")?GroundLauncherQa.run(world,failures):new JsonArray();
        MinecraftForge.EVENT_BUS.register(this);
    }
    @SubscribeEvent public void spawn(EntityJoinWorldEvent e) {
        if(e.getEntity() instanceof EntityWarTechArtilleryProjectile) {
            children++;bomblets.add((EntityWarTechArtilleryProjectile)e.getEntity());
        }
    }
    @SubscribeEvent public void blast(ExplosionEvent.Detonate e) {
        blasts++;
        if(index>=0 && index<parts.size() && CruiseWarheads.effect(parts.get(index))==CruiseWarheads.Effect.CLUSTER) {
            Vec3d p=e.getExplosion().getPosition();
            blastPositions.add(String.format(java.util.Locale.ROOT,"%.2f/%.2f/%.2f: %d entities",p.x,p.y,p.z,e.getAffectedEntities().size()));
        }
    }
    @SubscribeEvent public void hurt(LivingHurtEvent e) { if(villagers.contains(e.getEntityLiving())) hurt++; }
    private void setup() {
        for(Entity e:new ArrayList<Entity>(world.loadedEntityList)) if(e.getDistanceSq(0,64,0)<10000) e.setDead();
        villagers.clear();
        for(int x=-38;x<=38;x++) for(int z=-38;z<=38;z++) {
            world.setBlockState(new BlockPos(x,60,z),Blocks.BEDROCK.getDefaultState(),2);
            for(int y=61;y<=63;y++) world.setBlockState(new BlockPos(x,y,z),Blocks.DIRT.getDefaultState(),2);
            for(int y=64;y<=83;y++) if(!world.isAirBlock(new BlockPos(x,y,z))) world.setBlockToAir(new BlockPos(x,y,z));
        }
        // A solid cover control on the rear side; the open front is the shaped-charge cone.
        for(int y=64;y<=69;y++) for(int z=-4;z<=4;z++) for(int x=-12;x<=-10;x++)
            world.setBlockState(new BlockPos(x,y,z),Blocks.OBSIDIAN.getDefaultState(),2);
        for(double radius:new double[]{3,8,14,22}) for(int side=0;side<4;side++) {
            double angle=side*Math.PI/2;
            EntityVillager v=new EntityVillager(world);v.setPosition(.5+Math.cos(angle)*radius,64,.5+Math.sin(angle)*radius);
            v.setNoAI(true);v.enablePersistence();world.spawnEntity(v);villagers.add(v);
        }
        CruiseBuild b=CruiseBuild.starter(CruisePartDefinition.BODY_CLASSIC);
        ItemStack s=new ItemStack(WarTechContent.ASSEMBLED_CRUISE);b.writeToStack(s);
        EntityCustomCruise source=new EntityCustomCruise(world);source.configure(s,null);source.setOwnerIdentity(owner,"blue");
        source.setPosition(.5,index>=13?76:64.1,.5);
        children=blasts=hurt=0;blastPositions.clear();bomblets.clear();world.rand.setSeed(7171+index);
        CruisePayloadEffects.detonate(source,parts.get(index),new Vec3d(1,-.2,0),false);
    }
    private void finishCase() {
        int damaged=0,killed=0;JsonArray health=new JsonArray();
        for(EntityVillager v:villagers) {
            health.add(v.getHealth());if(v.getHealth()<20) damaged++;if(v.getHealth()<=0) killed++;
            if(v.isDead && v.getHealth()==20) failures.add("Villager disappeared without damage; invalid fixture");
        }
        JsonObject row=new JsonObject();row.addProperty("payload",parts.get(index).getId());row.addProperty("airburst",index>=13);
        row.addProperty("children",children);row.addProperty("blasts",blasts);row.addProperty("hurt_events",hurt);
        row.addProperty("damaged",damaged);row.addProperty("killed",killed);row.add("health_near_to_far_front_side_rear_side",health);
        row.add("blast_positions",new Gson().toJsonTree(blastPositions));
        row.addProperty("children_destroyed_before_payload",bomblets.stream().filter(e->e.isDead && e.getHealthValue()<=0).count());
        row.addProperty("children_still_live",bomblets.stream().filter(e->!e.isDead).count());
        records.add(row);System.out.println("FLIGHTQA payload "+row);
        if(CruiseWarheads.effect(parts.get(index))!=CruiseWarheads.Effect.EMP && damaged==0) failures.add("No living damage: "+row);
        if(CruiseWarheads.effect(parts.get(index))==CruiseWarheads.Effect.CLUSTER && (children==0 || blasts<2)) failures.add("Cluster failed to disperse: "+row);
    }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent e) throws Exception {
        if(e.phase!=TickEvent.Phase.END) return;tick++;
        if(tick==20 || tick>20 && (tick-20)%120==0) {
            if(index>=0) finishCase();index++;
            if(index<parts.size()) setup();
            else {
                for(Entity entity:new ArrayList<Entity>(world.loadedEntityList)) if(entity.getDistanceSq(0,64,0)<10000) entity.setDead();
            }
        }
        if(index==parts.size() && (tick-20)%120==40) {
            String resources=MissileChunkLoader.diagnostics(world);
            if(!resources.contains("flights=0/32") || !resources.contains("queue=0 chunks/0 owners")) failures.add("Loader leak: "+resources);
            JsonObject result=new JsonObject();result.addProperty("complete",true);result.addProperty("tick",tick);
            result.add("failures",new Gson().toJsonTree(failures));result.add("payload_cases",new Gson().toJsonTree(records));result.addProperty("resources",resources);
            result.add("launcher_cases",launcherCases);
            Files.write(Paths.get("flightqa-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result).getBytes(StandardCharsets.UTF_8));
            System.out.println("FLIGHTQA payload finished failures="+failures);MinecraftForge.EVENT_BUS.unregister(this);server.initiateShutdown();
        }
    }
}
