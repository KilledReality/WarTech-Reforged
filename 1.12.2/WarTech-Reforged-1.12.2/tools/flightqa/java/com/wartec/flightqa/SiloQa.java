package com.wartec.flightqa;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gameplay.*;
import com.wartec.wartecmod.port.gui.*;
import com.wartec.wartecmod.port.integration.*;
import com.wartec.wartecmod.port.planning.*;
import com.wartec.wartecmod.port.silo.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

/** Isolated native structure, original hatch, inventory, launch and restart-state regression. */
public final class SiloQa {
    private WorldServer world;private FakePlayer blue,red;private int tick,buildReady=-1,launchAt=-1,finish=-1;
    private final List<TileEntityMissileSilo> sites=new ArrayList<>(),cores=new ArrayList<>();
    private final List<ForgeChunkManager.Ticket> tickets=new ArrayList<>();private final Map<UUID,EntityStrategicMissile> shots=new LinkedHashMap<>();
    private final JsonArray cases=new JsonArray(),trace=new JsonArray();private final List<String> failures=new ArrayList<>();private int entries;
    private TileEntityMissileSilo importProbe;private int importStarted;private TileEntityMissileSilo renovating;private UUID restoredInstance;private final Set<Integer> alarmModes=new HashSet<>();private final Map<String,Integer> sounds=new HashMap<>();
    private AttackOperation operation;private BlockPos protectedProbe;
    private final List<BlockPos> originals=new ArrayList<>();private final List<UUID> identities=new ArrayList<>();
    private final List<BlockPos> obstacles=new ArrayList<>();
    public void started(){if(!Files.isRegularFile(Paths.get("FLIGHT_QA_ISOLATED")))throw new IllegalStateException("Not isolated");world=FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0);world.getGameRules().setOrCreateGameRule("doMobSpawning","false");MinecraftForge.EVENT_BUS.register(this);
        world.addEventListener((net.minecraft.world.IWorldEventListener)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{net.minecraft.world.IWorldEventListener.class},(proxy,method,args)->{
            if(method.getName().equals("hashCode"))return System.identityHashCode(proxy);if(method.getName().equals("equals"))return proxy==args[0];if(method.getName().equals("toString"))return "SiloQA listener";
            if(args!=null)for(Object arg:args)if(arg instanceof net.minecraft.util.SoundEvent){String id=String.valueOf(((net.minecraft.util.SoundEvent)arg).getRegistryName());sounds.put(id,sounds.getOrDefault(id,0)+1);}return null;
        }));}
    private void check(String kind,boolean ok,Object detail){JsonObject r=new JsonObject();r.addProperty("kind",kind);r.addProperty("success",ok);r.addProperty("detail",String.valueOf(detail));cases.add(r);if(!ok)failures.add(kind+": "+detail);System.out.println("SILOQA "+r);}
    private void pin(BlockPos p){ForgeChunkManager.Ticket t=ForgeChunkManager.requestTicket(com.wartec.wartecmod.WarTechReforged.instance,world,ForgeChunkManager.Type.NORMAL);tickets.add(t);for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){ChunkPos c=new ChunkPos((p.getX()>>4)+x,(p.getZ()>>4)+z);world.getChunkFromChunkCoords(c.x,c.z);ForgeChunkManager.forceChunk(t,c);}}
    private void setup(){
        blue=new FakePlayer(world,new GameProfile(new UUID(103,1),"SiloQA"));red=new FakePlayer(world,new GameProfile(new UUID(103,2),"EnemyQA"));blue.capabilities.isCreativeMode=true;NetworkTeamHelper.setPlayerTeam(blue,"blue");NetworkTeamHelper.setPlayerTeam(red,"red");
        check("low_world_rejected",SiloStructure.check(world,new BlockPos(32000,4,32000),EnumFacing.SOUTH).equals("silo.error.depth"),"depth bound prevents opening the void");
        for(int i=0;i<4;i++){
            EnumFacing f=EnumFacing.HORIZONTALS[i];BlockPos anchor=new BlockPos(32008+i*64,80,32008),panel=SiloGeometry.at(anchor,f,0,0,6);pin(anchor);world.setBlockState(panel,WarTechContent.MISSILE_SILO.getDefaultState(),3);
            TileEntityMissileSilo site=(TileEntityMissileSilo)world.getTileEntity(panel);site.facing=f;site.claim(blue);blue.setPosition(panel.getX()+.5,81,panel.getZ()+.5);sites.add(site);originals.add(panel);identities.add(site.instance());
            check("own_panel_"+f,site.mayUse(blue) && !site.mayUse(red),panel);
            BlockPos obstacle=anchor.down(9);world.setBlockState(obstacle,Blocks.CHEST.getDefaultState(),3);
            ((net.minecraft.inventory.IInventory)world.getTileEntity(obstacle)).setInventorySlotContents(0,new ItemStack(net.minecraft.init.Items.DIAMOND,64));
            check("containers_admitted_"+f,site.checkSite().isEmpty() && world.getTileEntity(obstacle)!=null,"authorized clearing includes inventory");
            obstacles.add(obstacle);
            net.minecraft.block.Block[] blocks={Blocks.DIAMOND_ORE,Blocks.BEDROCK,Blocks.WATER,Blocks.LAVA,Blocks.LOG,Blocks.LEAVES,Blocks.STONE,Blocks.GOLD_BLOCK};
            for(int k=0;k<blocks.length;k++){BlockPos q=SiloGeometry.at(anchor,f,1,-9-k,0);world.setBlockState(q,blocks[k].getDefaultState(),2);obstacles.add(q);}
            BlockPos overhead=anchor.up(17);world.setBlockState(overhead,Blocks.LOG.getDefaultState(),2);obstacles.add(overhead);
            site.setInventorySlotContents(0,new ItemStack(WarTechContent.STRATEGIC_MISSILE,1,i%2==0?0:2));site.setPower(12345);
            if(i==0){protectedProbe=obstacle;check("protected_build_denied_before_changes",!site.build(blue) && site.buildIndex<0 && world.getTileEntity(obstacle)!=null && world.getBlockState(panel).getBlock()==WarTechContent.MISSILE_SILO,site.status);protectedProbe=null;}
            check("site_clear_"+f,site.checkSite().isEmpty(),site.checkSite());check("build_admitted_"+f,site.build(blue),"native blueprint + permissions");
        }
    }
    @SubscribeEvent public void protectedCell(net.minecraftforge.event.world.BlockEvent.BreakEvent event){if(protectedProbe!=null && protectedProbe.equals(event.getPos()))event.setCanceled(true);}
    @SubscribeEvent public void join(EntityJoinWorldEvent e){if(e.getWorld()!=world || !(e.getEntity() instanceof EntityStrategicMissile))return;EntityStrategicMissile m=(EntityStrategicMissile)e.getEntity();if(m.isReentryVehicle()){if(m.getStrategicSystem()==StrategicSystemProfile.ORESHNIK)entries++;}else if(m.isSiloLaunch())shots.put(m.getUniqueID(),m);}
    private boolean walkable(BlockPos p){return world.getCollisionBoxes(null,new AxisAlignedBB(p.getX()+.2,p.getY(),p.getZ()+.2,p.getX()+.8,p.getY()+1.8,p.getZ()+.8)).isEmpty();}
    private boolean path(TileEntityMissileSilo core,int floor){
        BlockPos start=SiloGeometry.at(core.anchor,core.facing,6,floor+1,2),end=SiloGeometry.at(core.anchor,core.facing,0,floor+1,3);Set<BlockPos> seen=new HashSet<>();ArrayDeque<BlockPos> q=new ArrayDeque<>();q.add(start);
        while(!q.isEmpty() && seen.size()<300){BlockPos p=q.remove();if(!seen.add(p) || !walkable(p))continue;if(p.equals(end))return true;for(EnumFacing f:EnumFacing.HORIZONTALS){BlockPos n=p.offset(f);if(Math.abs(n.getX()-core.anchor.getX())<=8 && Math.abs(n.getZ()-core.anchor.getZ())<=8)q.add(n);}}return false;
    }
    private boolean access(TileEntityMissileSilo c,int floor){
        BlockPos start=SiloGeometry.at(c.anchor,c.facing,6,1,8),end=SiloGeometry.at(c.anchor,c.facing,1,floor+1,6);ArrayDeque<BlockPos> queue=new ArrayDeque<>();Set<BlockPos> seen=new HashSet<>();queue.add(start);
        while(!queue.isEmpty() && seen.size()<15000){BlockPos p=queue.remove();if(!seen.add(p) || !walkable(p))continue;
            boolean ladder=world.getBlockState(p).getBlock()==Blocks.LADDER || world.getBlockState(p.down()).getBlock()==Blocks.LADDER;
            boolean ground=!world.getCollisionBoxes(null,new AxisAlignedBB(p.getX()+.2,p.getY()-.08,p.getZ()+.2,p.getX()+.8,p.getY(),p.getZ()+.8)).isEmpty();if(!ground && !ladder)continue;
            if(p.equals(end))return true;for(EnumFacing f:EnumFacing.values()){if(f.getAxis()==EnumFacing.Axis.Y && !ladder && world.getBlockState(p.offset(f)).getBlock()!=Blocks.LADDER)continue;BlockPos n=p.offset(f);if(Math.abs(n.getX()-c.anchor.getX())<=8 && Math.abs(n.getZ()-c.anchor.getZ())<=8 && n.getY()>=c.anchor.getY()-29 && n.getY()<=c.anchor.getY()+1)queue.add(n);}
        }return false;
    }
    private void renovate(){
        TileEntityMissileSilo old=sites.get(0);renovating=(TileEntityMissileSilo)world.getTileEntity(SiloGeometry.at(old.anchor,old.facing,0,-4,6));TileEntityMissileSilo c=renovating.core();restoredInstance=c.instance();c.program(blue,c.anchor.add(1200,7,50));c.builtStructure=false;
        SiloStructure.replace(world,c.hatchPos,Blocks.AIR.getDefaultState());for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)if(HbmSiloHatch.footprint(x,z))SiloStructure.replace(world,c.anchor.add(x,0,z),Blocks.AIR.getDefaultState());
        check("legacy_hatch_fixture",HbmSiloHatch.place(world,c.anchor,c.facing,blue),"old lid direction");c.hatchPos=HbmSiloHatch.controller(c.anchor,c.facing);
        world.setBlockState(SiloGeometry.at(c.anchor,c.facing,6,1,7),com.hbm.blocks.ModBlocks.concrete_rebar.getDefaultState(),3);
        check("enemy_renovation_denied",!renovating.renovate(red),"strict owner scope");check("renovation_admitted",renovating.renovate(blue),renovating.status);check("rebuild_inventory_locked",c.constructionLocked && c.decrStackSize(0,1).isEmpty(),"backup cannot be extracted twice");check("rebuild_console_refuses_extra_ammo",!renovating.isItemValidForSlot(0,new ItemStack(WarTechContent.STRATEGIC_MISSILE,1,2)),"stored inventory is authoritative");net.minecraft.util.NonNullList<ItemStack> loot=net.minecraft.util.NonNullList.create();WarTechContent.SILO_START.getDrops(loot,world,c.getPos(),world.getBlockState(c.getPos()),0);check("rebuild_starter_no_duplicate_loot",loot.isEmpty(),loot);
        NBTTagCompound n=renovating.writeToNBT(new NBTTagCompound());TileEntityMissileSilo copy=new TileEntityMissileSilo();copy.readFromNBT(n);check("renovation_checkpoint_preserves_ammo",copy.writeToNBT(new NBTTagCompound()).getCompoundTag("StoredCore").hasKey("Items"),"persistent rebuild backup");
    }
    private void importProbe(){TileEntityMissileSilo old=sites.get(2);TileEntityMissileSilo panel=(TileEntityMissileSilo)world.getTileEntity(SiloGeometry.at(old.anchor,old.facing,0,-4,6));importProbe=panel.core();blue.setPosition(panel.getPos().getX()+.5,panel.getPos().getY()+1,panel.getPos().getZ()+.5);
        ItemStack designator=new ItemStack(WarTechContent.ITEM_TARGET_FINDER);NBTTagCompound n=new NBTTagCompound();n.setInteger("x",old.anchor.getX()+1500);n.setInteger("z",old.anchor.getZ()+300);designator.setTagCompound(n);importProbe.setInventorySlotContents(1,designator);ContainerMissileSilo menu=new ContainerMissileSilo(blue.inventory,panel);blue.openContainer=menu;importStarted=tick;importProbe.importTarget(blue,menu);check("xz_import_pending_blocks_old_target",importProbe.targetLoading && importProbe.launchError().equals("silo.state.target_loading"),importProbe.status);
    }
    private void ready(){
        for(int i=0;i<4;i++){
            TileEntityMissileSilo old=sites.get(i);BlockPos destination=SiloGeometry.at(old.anchor,old.facing,0,-4,6);
            TileEntityMissileSilo site=(TileEntityMissileSilo)world.getTileEntity(destination);sites.set(i,site);
            check("module_relocated_"+i,old.isInvalid() && world.getTileEntity(originals.get(i))==null && world.getBlockState(originals.get(i)).getBlock()==WarTechContent.SILO_DECK && site.instance().equals(identities.get(i)),"same identity, old block gone");
            TileEntityMissileSilo c=site.core();check("structure_completed_"+i,c!=null && c.isStarter(),"build="+site.buildIndex);if(c==null)continue;cores.add(c);
            check("loaded_items_moved_once_"+i,!c.getStackInSlot(0).isEmpty() && c.getPower()==12345 && site.getStackInSlot(0).isEmpty(),"ammo="+c.getStackInSlot(0)+" power="+c.getPower()+" console="+site.getStackInSlot(0));
            check("hatch_faces_clear_side_"+i,c.hatchPos.equals(HbmSiloHatch.controller(c.anchor,HbmSiloHatch.prefabFacing(c.facing))),c.hatchPos);
            check("recessed_lights_"+i,SiloStructure.lights(c.anchor,c.facing).size()==32 && SiloStructure.lights(c.anchor,c.facing).stream().allMatch(p->world.getBlockState(p).getBlock()==WarTechContent.SILO_LIGHT),"32 recessed lights");
            check("native_hbm_hatch_"+i,HbmSiloHatch.get(world,c.hatchPos)!=null,"original block + original tile");
            Object hatchBefore=HbmSiloHatch.get(world,c.hatchPos);check("hatch_replacement_guard_"+i,!HbmSiloHatch.place(world,c.anchor,HbmSiloHatch.prefabFacing(c.facing),blue) && HbmSiloHatch.get(world,c.hatchPos)==hatchBefore,"cannot overwrite a hatch or duplicate its item");
            net.minecraft.tileentity.TileEntity console=world.getTileEntity(SiloGeometry.at(c.anchor,c.facing,0,-4,6));check("interior_console_"+i,console instanceof TileEntityMissileSilo && ((TileEntityMissileSilo)console).core()==c,"same core, no copied inventory");
            for(int f:SiloGeometry.FLOORS){BlockPos button=SiloGeometry.at(c.anchor,c.facing,1,f+2,5);world.getBlockState(button).getBlock().onBlockActivated(world,button,world.getBlockState(button),blue,EnumHand.MAIN_HAND,EnumFacing.UP,0,0,0);check("walkable_floor_"+i+"_"+f,path(c,f),"actual button -> iron door -> gallery");}
            boolean fixtures=true;for(int f:SiloGeometry.FLOORS)for(int z:new int[]{3,5})fixtures &= world.getBlockState(SiloGeometry.at(c.anchor,c.facing,1,f+2,z)).getBlock()==Blocks.LEVER;
            check("all_door_levers_present_"+i,fixtures,"missing="+java.util.Arrays.stream(SiloGeometry.FLOORS).boxed().flatMap(f->java.util.stream.Stream.of(SiloGeometry.at(c.anchor,c.facing,1,f+2,3),SiloGeometry.at(c.anchor,c.facing,1,f+2,5))).filter(p->world.getBlockState(p).getBlock()!=Blocks.LEVER).map(p->p+"="+world.getBlockState(p)).collect(java.util.stream.Collectors.toList()));
            BlockPos entranceLever=SiloGeometry.at(c.anchor,c.facing,7,2,8);world.getBlockState(entranceLever).getBlock().onBlockActivated(world,entranceLever,world.getBlockState(entranceLever),blue,EnumHand.MAIN_HAND,EnumFacing.UP,0,0,0);
            for(int f:SiloGeometry.FLOORS)check("surface_entrance_to_floor_"+i+"_"+f,access(c,f),"outside -> entrance door -> ladder -> room");
            if(i==0)check("renovation_preserves_core_identity_target",c.instance().equals(restoredInstance) && c.hasTarget() && c.target.equals(c.anchor.add(1200,7,50)),"legacy hatch orientation fixed; stored core restored");
            check("power_path_buried_"+i,world.getBlockState(SiloGeometry.at(c.anchor,c.facing,-2,-30,0)).getBlock()==com.hbm.blocks.ModBlocks.red_cable && world.getBlockState(SiloGeometry.at(c.anchor,c.facing,-2,-29,0)).isOpaqueCube() && world.getBlockState(c.getPos().down()).getBlock()==com.hbm.blocks.ModBlocks.red_cable,"under floor and inside solid plinth");
            check("compatible_only_"+i,!c.isItemValidForSlot(0,new ItemStack(WarTechContent.ITEM_MISSILE_ANTI_BALLISTIC_NUCLEAR)) && c.isItemValidForSlot(0,new ItemStack(WarTechContent.STRATEGIC_MISSILE,1,i%2==0?0:2)),"ABM excluded");
            c.setInventorySlotContents(0,new ItemStack(WarTechContent.STRATEGIC_MISSILE,1,i%2==0?0:2));c.setPower(1_000_000);
            check("target_program_"+i,c.program(blue,c.anchor.add(1000,0,0)),"native coordinates");
            net.minecraft.entity.item.EntityItem loose=new net.minecraft.entity.item.EntityItem(world,c.getPos().getX()+.5,c.getPos().getY()+3,c.getPos().getZ()+.5,new ItemStack(net.minecraft.init.Items.WHEAT_SEEDS));world.spawnEntity(loose);
            check("loose_item_does_not_block_"+i,c.launchError().isEmpty(),c.launchError());loose.setDead();
            net.minecraft.entity.item.EntityArmorStand person=new net.minecraft.entity.item.EntityArmorStand(world,c.getPos().getX()+.5,c.getPos().getY()+3,c.getPos().getZ()+.5);world.spawnEntity(person);
            check("solid_entity_blocks_"+i,c.launchError().equals("silo.error.clearance"),c.launchError());person.setDead();
            check("closed_lid_preflight_"+i,c.launchError().isEmpty(),c.launchError());
            NBTTagCompound n=c.writeToNBT(new NBTTagCompound());TileEntityMissileSilo copy=new TileEntityMissileSilo();copy.readFromNBT(n);check("state_roundtrip_"+i,copy.hasTarget() && copy.getStackInSlot(0).getMetadata()==c.getStackInSlot(0).getMetadata() && copy.getPower()==c.getPower(),"ammo,target,power,links preserved");
            PlanningAsset a=PlanningAssetRegistry.describe(c);check("terminal_asset_"+i,a!=null && a.ammo==1 && a.y==80 && a.allows(AttackOperation.Action.LAUNCH),a==null?"missing":a.key);
            blue.setPosition(site.getPos().getX()+.5,site.getPos().getY()+1,site.getPos().getZ()+.5);ContainerMissileSilo menu=new ContainerMissileSilo(blue.inventory,site);check("interior_controls_bottom_"+i,menu.tile==c && menu.canInteractWith(blue) && !menu.canInteractWith(red),"one inventory, relocated console distance");
            ItemStack designator=new ItemStack(WarTechContent.ITEM_TARGET_FINDER);NBTTagCompound coordinates=new NBTTagCompound();coordinates.setInteger("x",c.target.getX());coordinates.setInteger("y",3);coordinates.setInteger("z",c.anchor.getZ()+100);designator.setTagCompound(coordinates);c.setInventorySlotContents(1,designator);blue.openContainer=menu;c.importTarget(blue,menu);check("designator_import_"+i,c.hasTarget() && c.target.getX()==c.anchor.getX()+1000 && c.target.getY()==3 && c.target.getZ()==c.anchor.getZ()+100 && c.getStackInSlot(1)==designator,"XYZ, actual slot, item retained");
            check("foreign_launch_denied_"+i,!c.beginLaunch(red),"strict scope");
            check("foreign_readiness_denied_"+i,!c.setLaunchReady(red,true),"strict scope");
            check("readiness_admitted_"+i,c.setLaunchReady(blue,true) && c.isLaunchReady() && c.phase==0,c.status);
            check("ready_ammo_locked_"+i,c.decrStackSize(0,1).isEmpty() && !menu.inventorySlots.get(0).canTakeStack(blue),"real stack stays in pedestal");
            check("ready_cancel_"+i,c.setLaunchReady(blue,false) && !c.isLaunchReady(),c.status);
            check("ready_rearm_"+i,c.setLaunchReady(blue,true),c.status);
            PlanningAsset armed=PlanningAssetRegistry.describe(c);check("ready_attack_asset_"+i,armed.attack && armed.state.equals("launch_ready") && armed.ammo==1,"surface marker, owned attack asset");
            NBTTagCompound armedNbt=c.writeToNBT(new NBTTagCompound());TileEntityMissileSilo restored=new TileEntityMissileSilo();restored.readFromNBT(armedNbt);check("ready_roundtrip_"+i,restored.isLaunchReady() && restored.phase==0 && restored.getPower()==1_000_000,"no implicit launch");

        }
        check("all_obstructions_removed",obstacles.stream().allMatch(p->world.isAirBlock(p) && world.getTileEntity(p)==null),"chests, ore, bedrock, water, lava, wood, leaves, metal");
        check("no_demolition_drops",world.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class,new AxisAlignedBB(31990,40,31990,32230,90,32030)).stream().noneMatch(e->!e.isDead && (e.getItem().getItem()==net.minecraft.init.Items.DIAMOND || e.getItem().getItem()==net.minecraft.item.Item.getItemFromBlock(WarTechContent.MISSILE_SILO))),"loose="+world.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class,new AxisAlignedBB(31990,40,31990,32230,90,32030)).stream().filter(e->!e.isDead).map(e->e.getItem().toString()).collect(java.util.stream.Collectors.toList()));
        launchAt=tick;
    }
    private void launchReadyCores(){
        check("ready_hold_without_launch",shots.isEmpty() && cores.stream().allMatch(c->c.isLaunchReady() && c.phase==0 && !c.getStackInSlot(0).isEmpty() && c.getPower()==1_000_000 && !HbmSiloHatch.isOpen(world,c.hatchPos)),"100 ticks smoke/alarm, no missile or charge consumed");
        check("ready_alarm_modes",alarmModes.contains(1) && alarmModes.contains(2),alarmModes);
        check("ready_audio",sounds.getOrDefault("wartecmod:weapon.cruisemissileengine",0)>0,sounds);
        for(int i=0;i<4;i++){TileEntityMissileSilo c=cores.get(i);
            if(i==1){ItemStack detonator=new ItemStack(com.hbm.items.ModItems.detonator);blue.setHeldItem(EnumHand.MAIN_HAND,detonator);SiloDetonator.bind(blue,detonator,c);check("native_detonator_bound",SiloDetonator.count(detonator)==1,"native HBM item");UavChainDetonator.activate(blue,detonator);check("native_detonator_launch",c.phase==1 && SiloDetonator.count(detonator)==0,c.status);}
            else if(i==3){operation=new AttackOperation();operation.scope=PlanningAccessPolicy.scope(blue);operation.author=blue.getUniqueID();operation.title="Silo native operation";operation.started=world.getTotalWorldTime();operation.state=AttackOperation.State.RUNNING;
                AttackOperation.Step prepare=new AttackOperation.Step();prepare.asset=PlanningAssetRegistry.key(c);prepare.action=AttackOperation.Action.PREPARE;prepare.x=c.target.getX();prepare.y=c.target.getY();prepare.z=c.target.getZ();operation.steps.add(prepare);
                AttackOperation.Step launch=new AttackOperation.Step();launch.asset=prepare.asset;launch.action=AttackOperation.Action.LAUNCH;launch.after=prepare.id;launch.event=AttackOperation.Event.COMPLETE;launch.offset=60;launch.x=prepare.x;launch.y=prepare.y;launch.z=prepare.z;operation.steps.add(launch);PlanningWorldData.get(world).operations.put(operation.id,operation);PlanningAssetRegistry.refresh(world);check("native_operation_valid",OperationScheduler.validate(world,operation).isEmpty(),OperationScheduler.validate(world,operation));
            }else check("launch_admitted_"+i,c.beginLaunch(blue),c.status);
            if(i!=3){check("duplicate_order_denied_"+i,!c.beginLaunch(blue),"pending hatch opening");check("cannot_extract_during_launch_"+i,c.decrStackSize(0,1).isEmpty(),"locked real stack");}
        }
    }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event)throws Exception{
        if(event.phase!=TickEvent.Phase.END || world==null)return;tick++;
        try{
            if(tick==1){setup();for(net.minecraft.block.Block b:new net.minecraft.block.Block[]{WarTechContent.MISSILE_SILO,WarTechContent.SILO_START,WarTechContent.SILO_DECK,WarTechContent.SILO_LIGHT})check("management_tab_"+b.getRegistryName(),net.minecraft.item.Item.getItemFromBlock(b).getCreativeTab()==WarTechCreativeTabs.GEAR,"all silo items in management");}
            if(buildReady<0 && tick>10 && sites.size()==4 && sites.stream().allMatch(s->s.buildIndex<0)){if(renovating==null)renovate();else if(renovating.buildIndex<0){if(importProbe==null)importProbe();else if(!importProbe.targetLoading){check("xz_target_resolved",importProbe.hasTarget() && importProbe.target.getX()==sites.get(2).anchor.getX()+1500 && importProbe.target.getZ()==sites.get(2).anchor.getZ()+300 && tick-importStarted<200,"ticks="+(tick-importStarted)+" target="+importProbe.target);buildReady=tick;ready();}}}
            if(tick==300 && buildReady<0){for(TileEntityMissileSilo s:sites)System.out.println("SILOQA building "+s.buildIndex+" "+s.status);}
            if(tick==1000 && buildReady<0){check("build_timeout",false,"not finished");finish=tick;}
            if(launchAt>0){
                if(tick==launchAt+100)launchReadyCores();
                if(tick==launchAt+130)check("planned_prepare_waits_for_launch",cores.get(3).isLaunchReady() && cores.get(3).phase==0 && operation.steps.get(1).emitted==0,"PREPARE completed, scheduled LAUNCH still waiting");
                for(TileEntityMissileSilo c:cores)if(c.phase>0 || c.isLaunchReady())alarmModes.add(world.getBlockState(SiloStructure.lights(c.anchor,c.facing).get(0)).getValue(SiloLightBlock.MODE));
                for(EntityStrategicMissile m:shots.values())if(!m.isDead){JsonObject r=new JsonObject();r.addProperty("id",m.getUniqueID().toString());r.addProperty("tick",tick-launchAt);r.addProperty("y",m.posY);r.addProperty("dy",m.motionY);r.addProperty("cold",m.isSiloCold());r.addProperty("falling",m.isFallingDebris());trace.add(r);}
                if(tick==launchAt+250)for(EntityStrategicMissile m:shots.values())if(m.getStrategicSystem()==StrategicSystemProfile.TOPOL_M){check("topol_exit_then_ignition_"+m.getUniqueID(),!m.isFallingDebris() && !m.isSiloCold() && m.posY>83,"y="+m.posY);m.beginCombatCrash();}
                if(tick==launchAt+320){
                    check("native_operation_actual_done",operation!=null && operation.state==AttackOperation.State.COMPLETE && operation.steps.get(1).emitted==1,"real scheduler + actual spawn hook");
                    check("four_real_missiles",shots.size()==4,shots.size());int i=0;for(EntityStrategicMissile m:shots.values()){
                        if(m.getStrategicSystem()==StrategicSystemProfile.ORESHNIK){check("full_exit_then_ignition_"+i,!m.isFallingDebris() && !m.isSiloCold() && m.posY>83,"y="+m.posY);i++;}
                    }
                    for(TileEntityMissileSilo c:cores)check("ammo_power_once_"+c.facing,c.getStackInSlot(0).isEmpty() && c.getPower()==925_000,"power="+c.getPower());
                }
                if(tick==launchAt+1800){check("alarm_both_phases",alarmModes.contains(1) && alarmModes.contains(2),alarmModes);check("launch_audio_dispatched",sounds.getOrDefault("wartecmod:weapon.ballisticmissiletakeoff",0)>=4 && sounds.getOrDefault("wartecmod:weapon.cruisemissileengine",0)>=4,sounds);check("alarm_resets",cores.stream().allMatch(c->SiloStructure.lights(c.anchor,c.facing).stream().allMatch(p->world.getBlockState(p).getValue(SiloLightBlock.MODE)==0)),"white after launch");check("two_oreshniks_seven_blocks_each",entries==14,entries);for(TileEntityMissileSilo c:cores)check("hatch_cycle_complete_"+c.facing,c.phase==0 && HbmSiloHatch.stationary(world,c.hatchPos),c.status);finish=tick;}
            }
            if(finish==tick){for(Entity e:new ArrayList<>(world.loadedEntityList))if(e instanceof EntityStrategicMissile || e.getClass().getName().startsWith("com.hbm.entity."))e.setDead();for(ForgeChunkManager.Ticket t:tickets)ForgeChunkManager.releaseTicket(t);}
            if(finish>0 && tick==finish+60){String resources=MissileChunkLoader.diagnostics(world);check("released_flight_resources",resources.contains("flights=0/") && resources.contains("queue=0 chunks/0 owners"),resources);JsonObject out=new JsonObject();out.addProperty("complete",true);out.add("cases",cases);out.add("failures",new Gson().toJsonTree(failures));out.addProperty("resources",resources);Files.write(Paths.get("flightqa-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(out).getBytes(StandardCharsets.UTF_8));Files.write(Paths.get("silo-trace.json"),new GsonBuilder().setPrettyPrinting().create().toJson(trace).getBytes(StandardCharsets.UTF_8));world.getMinecraftServer().initiateShutdown();}
        }catch(Throwable ex){check("harness_exception",false,ex);ex.printStackTrace();finish=tick;}
    }
}
