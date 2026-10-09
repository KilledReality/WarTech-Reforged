package com.wartec.flightqa;

import com.google.gson.*;
import com.hbm.items.ModItems;
import com.mojang.authlib.GameProfile;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gameplay.TileEntityCruiseLauncher;
import com.wartec.wartecmod.port.integration.*;
import com.wartec.wartecmod.port.network.MissileTrackingService;
import com.wartec.wartecmod.port.uav.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Isolated real Forge event, entity tick and damage/repair QA. Not shipped. */
public final class DefenseQa {
    private final List<String> failures=new ArrayList<>();
    private final JsonArray cases=new JsonArray();
    private MinecraftServer server;private WorldServer world;private FakePlayer player,stranger;
    private EntityWarTechGroundVehicle pantsir;private EntityCustomCruise target;
    private EntityWarTechGroundVehicle damagedDefense;
    private final List<EntityWarTechGroundVehicle> blastTargets=new ArrayList<>();
    private int tick;private int baselineAmmo;
    public void started() {
        if(!Files.isRegularFile(Paths.get("FLIGHT_QA_ISOLATED"))) throw new IllegalStateException("Not isolated");
        server=FMLCommonHandler.instance().getMinecraftServerInstance();world=server.getWorld(0);
        if(!"flightqa".equals(world.getWorldInfo().getWorldName())) throw new IllegalStateException("Wrong world");
        world.getGameRules().setOrCreateGameRule("doMobSpawning","false");
        world.getGameRules().setOrCreateGameRule("randomTickSpeed","0");
        player=FakePlayerFactory.get(world,new GameProfile(new UUID(72,1),"DefenseQA"));
        stranger=FakePlayerFactory.get(world,new GameProfile(new UUID(72,2),"EnemyQA"));
        NetworkTeamHelper.setPlayerTeam(player,"blue");NetworkTeamHelper.setPlayerTeam(stranger,"red");
        MinecraftForge.EVENT_BUS.register(this);
    }
    private void check(boolean pass,String reason) { if(!pass) failures.add(reason); }
    private void record(String kind,String detail,boolean success) {
        JsonObject r=new JsonObject();r.addProperty("kind",kind);r.addProperty("detail",detail);r.addProperty("success",success);cases.add(r);
    }
    private ItemStack missile(CruisePartDefinition body,boolean air,Vec3d goal) {
        CruiseBuild b=CruiseBuild.starter(body);if(air) b.set(CruiseSlot.LAUNCH,CruisePartDefinition.LAUNCH_AIR);
        ItemStack s=new ItemStack(WarTechContent.ASSEMBLED_CRUISE);b.writeToStack(s);
        CruiseMission m=new CruiseMission();m.setTarget(goal,0);m.writeToStack(s);return s;
    }
    private void click(BlockPos pos,boolean sneak) {
        player.setSneaking(sneak);
        PlayerInteractEvent.RightClickBlock e=new PlayerInteractEvent.RightClickBlock(player,EnumHand.MAIN_HAND,pos,EnumFacing.UP,new Vec3d(pos));
        MinecraftForge.EVENT_BUS.post(e);check(e.isCanceled(),"Custom fixture did not capture detonator event");
    }
    private void airClick(boolean sneak) {
        player.setSneaking(sneak);PlayerInteractEvent.RightClickItem e=new PlayerInteractEvent.RightClickItem(player,EnumHand.MAIN_HAND);
        MinecraftForge.EVENT_BUS.post(e);check(e.isCanceled(),"Custom-only detonator fell through into HBM item");
    }
    private void clearFlights() {
        for(Entity e:new ArrayList<Entity>(world.loadedEntityList))
            if(e instanceof EntityCustomCruise || e instanceof EntityCustomUav || e instanceof EntityWarTechMissile || e instanceof EntityWarTechArtilleryProjectile) e.setDead();
    }
    private void detonators() {
        BlockPos pos=new BlockPos(32,4,32);player.setPosition(32,4,32);
        for(boolean multi:new boolean[]{false,true}) for(CruisePartDefinition body:CruiseAirframes.bodies()) {
            ItemStack det=new ItemStack(multi?ModItems.detonator_multi:ModItems.detonator);player.setHeldItem(EnumHand.MAIN_HAND,det);
            world.setBlockToAir(pos);world.setBlockState(pos,(body==CruisePartDefinition.BODY_LIGHT?
                WarTechContent.CRUISE_DRONE_RAIL:WarTechContent.CRUISE_LAUNCH_POINT).getDefaultState(),3);
            TileEntityCruiseLauncher tile=(TileEntityCruiseLauncher)world.getTileEntity(pos);
            tile.load(player,missile(body,false,new Vec3d(32,4,500)));click(pos,true);
            check(CruiseChainDetonator.count(det)==1,"Cruise bind failed "+body+"/"+multi);
            if(multi) airClick(false);else click(pos,false);
            check(tile.isEmpty() && CruiseChainDetonator.count(det)==0,"Cruise remote launch failed "+body+"/"+multi);
            record("detonator_cruise",body.getId()+"/"+(multi?"multi_air_click":"single_block_click"),tile.isEmpty());clearFlights();
            airClick(false); // Exhausted custom NBT must not fire HBM's default coordinates 0/0/0.
        }
        ItemStack det=new ItemStack(ModItems.detonator_multi);player.setHeldItem(EnumHand.MAIN_HAND,det);
        world.setBlockToAir(pos);world.setBlockState(pos,WarTechContent.CRUISE_LAUNCH_POINT.getDefaultState(),3);
        TileEntityCruiseLauncher tile=(TileEntityCruiseLauncher)world.getTileEntity(pos);
        tile.load(player,missile(CruisePartDefinition.BODY_CLASSIC,false,new Vec3d(32,4,500)));click(pos,true);
        // Valid binding, subsequently invalid launch; failed command must retain the link and weapon.
        CruiseMission near=new CruiseMission();near.setTarget(new Vec3d(32,4,33),0);near.writeToStack(tile.getStackInSlot(0));
        airClick(false);check(CruiseChainDetonator.count(det)==1 && !tile.isEmpty(),"Failed launch erased link or weapon");
        newMission(tile.getStackInSlot(0),new Vec3d(32,4,500));airClick(false);
        check(tile.isEmpty() && CruiseChainDetonator.count(det)==0,"Retried launch failed");clearFlights();
        tile.load(player,missile(CruisePartDefinition.BODY_CLASSIC,false,new Vec3d(32,4,500)));click(pos,true);
        tile.removeStackFromSlot(0);tile.load(player,missile(CruisePartDefinition.BODY_CLASSIC,false,new Vec3d(32,4,500)));
        airClick(false);check(!tile.isEmpty(),"Stale binding launched a replacement missile");airClick(true);
        check(CruiseChainDetonator.count(det)==0,"Sneak clear failed");
        world.setBlockToAir(pos);world.setBlockState(pos,WarTechContent.UAV_LAUNCH_POINT.getDefaultState(),3);
        for(boolean multi:new boolean[]{false,true}) for(boolean direct:new boolean[]{false,true}) {
            det=new ItemStack(multi?ModItems.detonator_multi:ModItems.detonator);player.setHeldItem(EnumHand.MAIN_HAND,det);
            EntityCustomUav u=new EntityCustomUav(world);u.configure(UavBuild.starter(UavAirframe.ONE_WAY),player);
            u.setPosition(32.5,5.15,32.5);u.setGuidanceTarget(32,4,500);world.spawnEntity(u);
            if(direct) { player.setSneaking(true);u.processInitialInteract(player,EnumHand.MAIN_HAND); } else click(pos,true);
            check(UavChainDetonator.linkCount(det)==1,"UAV binding failed");
            airClick(false);check(u.getLegacyState()!=0 && UavChainDetonator.linkCount(det)==0,"UAV chain launch failed");
            record("detonator_uav",(multi?"multi":"single")+(direct?"/entity":"/block"),u.getLegacyState()!=0);u.setDead();
        }
        world.setBlockToAir(pos);player.setSneaking(false);
        // Ordinary detonators have one binding across UAVs and missiles.
        det=new ItemStack(ModItems.detonator);player.setHeldItem(EnumHand.MAIN_HAND,det);
        det.setTagCompound(new NBTTagCompound());det.getTagCompound().setInteger("x",123);
        world.setBlockState(pos,WarTechContent.CRUISE_LAUNCH_POINT.getDefaultState(),3);
        tile=(TileEntityCruiseLauncher)world.getTileEntity(pos);tile.load(player,missile(CruisePartDefinition.BODY_CLASSIC,false,new Vec3d(32,4,500)));
        click(pos,true);check(!det.getTagCompound().hasKey("x"),"Single detonator retained previous native binding");
        NBTTagCompound link=det.getTagCompound().getTagList("WarTechCruiseLaunchers",10).getCompoundTagAt(0);
        long original=link.getLong("Position");BlockPos unloaded=new BlockPos(1000000,64,1000000);
        link.setLong("Position",unloaded.toLong());airClick(false);
        check(!world.isBlockLoaded(unloaded) && CruiseChainDetonator.count(det)==1,"Detonator loaded distant chunk or discarded unloaded binding");
        det.getTagCompound().getTagList("WarTechCruiseLaunchers",10).getCompoundTagAt(0).setLong("Position",original);
        airClick(false);check(tile.isEmpty(),"Retry after unloaded fixture failed");clearFlights();
        NetworkTeamHelper.setPlayerTeam(player,"");
        tile.load(player,missile(CruisePartDefinition.BODY_CLASSIC,false,new Vec3d(32,4,500)));
        // The existing blue-owned fixture retains its IFF, new personal fixtures must not adopt a nearby radar team.
        world.setBlockToAir(pos);world.setBlockState(pos,WarTechContent.CRUISE_LAUNCH_POINT.getDefaultState(),3);
        tile=(TileEntityCruiseLauncher)world.getTileEntity(pos);tile.load(player,missile(CruisePartDefinition.BODY_CLASSIC,false,new Vec3d(32,4,500)));
        NBTTagCompound personal=tile.writeToNBT(new NBTTagCompound());
        check(personal.getString("Team").equals(NetworkTeamHelper.getPlayerTeam(player)),"Personal launcher has blank IFF");
        check(!tile.mayUse(stranger),"Enemy can operate personal launcher");world.setBlockToAir(pos);
        NetworkTeamHelper.setPlayerTeam(player,"blue");
        record("detonator_retry_stale_and_clear","retry retained, replacement not launched, explicit clear",true);
    }
    private void newMission(ItemStack s,Vec3d goal) { CruiseMission m=new CruiseMission();m.setTarget(goal,0);m.writeToStack(s); }
    private EntityWarTechGroundVehicle vehicle(WarTechEntityProfile p,double x,double z) {
        // Vanilla skips ordinary entity updates without the surrounding loaded area.
        for(int cx=((int)x>>4)-2;cx<=((int)x>>4)+2;cx++)
            for(int cz=((int)z>>4)-2;cz<=((int)z>>4)+2;cz++) world.getChunkFromChunkCoords(cx,cz);
        EntityWarTechGroundVehicle e=new EntityWarTechGroundVehicle(world,p);e.setVisual("ground/pantsir",1);e.setPosition(x,4,z);
        e.setOwner(player);world.spawnEntity(e);return e;
    }
    private void healthAndRepair() {
        player.capabilities.isCreativeMode=false;
        for(WarTechEntityProfile p:new WarTechEntityProfile[]{WarTechEntityProfile.MOBILE_AIR_DEFENSE,WarTechEntityProfile.RADAR_TRUCK,
                WarTechEntityProfile.S400_RADAR,WarTechEntityProfile.COMMAND_TRUCK}) {
            EntityWarTechGroundVehicle e=vehicle(p,80,80);player.setPosition(80,4,80);stranger.setPosition(80,4,80);
            NBTTagCompound saved=new NBTTagCompound();e.writeToNBT(saved);saved.setInteger("WarTechHealthSchema",2);
            float old=p==WarTechEntityProfile.MOBILE_AIR_DEFENSE?500:p==WarTechEntityProfile.RADAR_TRUCK?300:p==WarTechEntityProfile.S400_RADAR?600:720;
            saved.setFloat("WarTechHealth",old*.5F);e.readFromNBT(saved);
            check(Math.abs(e.getHealthValue()-e.getHealthCapacity()*.5)<.01,"Health migration lost damage ratio "+p);
            float before=e.getHealthValue();ItemStack iron=new ItemStack(Items.IRON_INGOT,2);player.setHeldItem(EnumHand.MAIN_HAND,iron);
            e.processInitialInteract(player,EnumHand.MAIN_HAND);
            check(e.getHealthValue()>before && iron.getCount()==1,"Iron repair did not heal/consume exactly one "+p);
            before=e.getHealthValue();ItemStack enemyIron=new ItemStack(Items.IRON_INGOT,2);stranger.setHeldItem(EnumHand.MAIN_HAND,enemyIron);
            e.processInitialInteract(stranger,EnumHand.MAIN_HAND);
            check(e.getHealthValue()==before && enemyIron.getCount()==2,"Enemy repaired defense "+p);
            player.capabilities.isCreativeMode=true;e.processInitialInteract(player,EnumHand.MAIN_HAND);check(iron.getCount()==1,"Creative repair consumed iron");
            player.capabilities.isCreativeMode=false;
            for(int i=0;i<15;i++) e.tryRepairDefense(player,new ItemStack(Items.IRON_INGOT));
            before=e.getHealthValue();iron=new ItemStack(Items.IRON_INGOT,2);e.tryRepairDefense(player,iron);
            check(before==e.getHealthCapacity() && iron.getCount()==2,"Full health consumed iron "+p);
            record("health_repair_migration",p.name()+"/"+e.getHealthCapacity(),true);e.setDead();
        }
    }
    private void setupIff(boolean hostile,boolean tor) {
        if(!hostile) {
            pantsir=vehicle(WarTechEntityProfile.MOBILE_AIR_DEFENSE,512,512);
            pantsir.setVisual(tor?"ground/tor":"ground/pantsir",tor?0:1);
            pantsir.setLegacyPower(pantsir.getEnergyCapacity());pantsir.setLegacyFireMode(2);pantsir.setLegacyEnabled(true);
            pantsir.handleLegacyGuiAction(1,player);
            check(pantsir.isDeployed() && pantsir.isLegacyEnabled(),"GUI deploy button did not enable radar");
            for(int i=0;i<(tor?8:12);i++) pantsir.setInventorySlotContents(i,new ItemStack(tor?WarTechContent.ITEM_MISSILE_ANTI_AIR_TIER_2:WarTechContent.ITEM_MISSILE_ANTI_AIR_TIER_1));
        } else NetworkTeamHelper.setPlayerTeam(player,"red");
        for(int x=29;x<=35;x++) for(int z=29;z<=38;z++) world.getChunkFromChunkCoords(x,z);
        baselineAmmo=ammo();target=new EntityCustomCruise(world);target.setPosition(512,22,552);
        target.configure(missile(CruisePartDefinition.BODY_LIGHT,true,new Vec3d(512,4,1100)),player);
        target.motionZ=.7;target.rotationYaw=0;world.spawnEntity(target);
        MissileTrackingService.registerLaunch(target,512,22,552,512,1100,target.getOwnerTeam());
        check(target.getOwnerTeam().equals(hostile?"red":"blue"),"Launch did not snapshot player team");
        check(pantsir.getOwnerTeam().equals("blue"),"Changing player team changed deployed defense");
    }
    private int ammo() { int n=0;for(int i=0;i<12;i++) if(!pantsir.getStackInSlot(i).isEmpty()) n++;return n; }
    private void cluster(boolean heavy) {
        world.rand.setSeed(7272);blastTargets.clear();double x=1024,z=1024;
        for(int cx=62;cx<=66;cx++) for(int cz=62;cz<=66;cz++) world.getChunkFromChunkCoords(cx,cz);
        for(double dx:new double[]{3,10,18}) blastTargets.add(vehicle(WarTechEntityProfile.MOBILE_AIR_DEFENSE,x+dx,z));
        EntityCustomCruise source=new EntityCustomCruise(world);source.setPosition(x,8,z);
        source.configure(missile(CruisePartDefinition.BODY_CLASSIC,true,new Vec3d(x,4,z+400)),player);
        CruisePayloadEffects.detonate(source,heavy?CruisePartDefinition.WARHEAD_HEAVY_CLUSTER:CruisePartDefinition.WARHEAD_CLUSTER,new Vec3d(0,0,1),false);
    }
    private void finishCluster(boolean heavy) {
        JsonObject row=new JsonObject();row.addProperty("kind",heavy?"heavy_cluster_pantsir":"cluster_pantsir");JsonArray hp=new JsonArray();boolean damaged=false;
        for(EntityWarTechGroundVehicle e:blastTargets) { hp.add(e.getHealthValue());damaged|=e.getHealthValue()<e.getHealthCapacity();e.setDead(); }
        row.add("health_at_3_10_18_blocks",hp);cases.add(row);check(damaged,"Cluster did not damage nearby Pantsir "+heavy);clearFlights();
    }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) throws Exception {
        if(event.phase!=TickEvent.Phase.END) return;tick++;
        if(tick==10) { detonators();healthAndRepair();setupIff(false,false); }
        if(tick==40) { check(ammo()==baselineAmmo,"Pantsir fired at own team");record("iff_friendly","no fire",ammo()==baselineAmmo);target.setDead();setupIff(true,false); }
        if(tick==75) {
            check(ammo()<baselineAmmo,"Pantsir failed to fire at hostile local launch after owner team switch");
            record("iff_switched_owner","interceptors consumed: "+(baselineAmmo-ammo()),ammo()<baselineAmmo);
            pantsir.setDead();clearFlights();NetworkTeamHelper.setPlayerTeam(player,"blue");setupIff(false,true);
        }
        if(tick==105) { check(ammo()==baselineAmmo,"Tor fired at own team");record("iff_tor_friendly","no fire",ammo()==baselineAmmo);target.setDead();setupIff(true,true); }
        if(tick==140) {
            check(ammo()<baselineAmmo,"Tor failed to fire after owner team switch");
            record("iff_tor_switched_owner","interceptors consumed: "+(baselineAmmo-ammo()),ammo()<baselineAmmo);pantsir.setDead();clearFlights();cluster(false);
        }
        if(tick==260) { finishCluster(false);cluster(true); }
        if(tick==380) {
            finishCluster(true);damagedDefense=vehicle(WarTechEntityProfile.MOBILE_AIR_DEFENSE,80,80);
            damagedDefense.setLegacyPower(damagedDefense.getEnergyCapacity());damagedDefense.setDeployed(true);
            damagedDefense.setLegacyEnabled(true);damagedDefense.attackEntityFrom(DamageSource.GENERIC,165);
        }
        if(tick==395) {
            check(!damagedDefense.isLegacyOperational(),"Critically damaged radar remained operational");
            player.setPosition(80,4,80);damagedDefense.tryRepairDefense(player,new ItemStack(Items.IRON_INGOT));
        }
        if(tick==410) {
            check(damagedDefense.isLegacyOperational(),"Repair did not restore damaged radar");
            record("critical_damage_repair_restart","hp="+damagedDefense.getHealthValue()+" ticks="+damagedDefense.ticksExisted
                +" power="+damagedDefense.getLegacyPower()+" deployed="+damagedDefense.isDeployed(),damagedDefense.isLegacyOperational());damagedDefense.setDead();
        }
        if(tick==420) {
            JsonObject out=new JsonObject();out.addProperty("complete",true);out.addProperty("tick",tick);out.add("cases",cases);out.add("failures",new Gson().toJsonTree(failures));
            out.addProperty("resources",MissileChunkLoader.diagnostics(world));
            Files.write(Paths.get("flightqa-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(out).getBytes(StandardCharsets.UTF_8));
            System.out.println("FLIGHTQA defense finished "+out);MinecraftForge.EVENT_BUS.unregister(this);server.initiateShutdown();
        }
    }
}
