package com.wartec.flightqa;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gui.*;
import com.wartec.wartecmod.port.integration.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.item.*;
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

/** Local isolated integration checks for independent targets and real TGT inventory. */
public final class TelTargetQa {
    private WorldServer world;private FakePlayer player,enemy;private EntityStrategicTel tel;
    private ContainerLegacyEntity container;private int tick,cleanup=-1;
    private final EntityWarTechMissile[] flights=new EntityWarTechMissile[2];
    private final JsonArray cases=new JsonArray();private final List<String> failures=new ArrayList<>();
    private final BlockPos first=new BlockPos(32920,3,32080),second=new BlockPos(32320,3,31840);
    private final boolean strikes=Boolean.getBoolean("wartech.telstrikeqa");
    private final boolean commands=Boolean.getBoolean("wartech.telcommandqa");
    private ItemStack command;private NBTTagCompound deferredTopol;private boolean delayedStarted;
    private EntityStrategicTel topol,oresh;private EntityStrategicMissile topolBoost;
    private final List<EntityStrategicMissile> entries=new ArrayList<>();
    private int baselineDestroyed;private float measuredStrength;private boolean centralBlast;
    public void started() {
        if(!Files.isRegularFile(Paths.get("FLIGHT_QA_ISOLATED"))) throw new IllegalStateException("Not isolated");
        world=FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0);
        if(!"flightqa".equals(world.getWorldInfo().getWorldName())) throw new IllegalStateException("Wrong world");
        player=pilot(1);enemy=pilot(2);NetworkTeamHelper.setPlayerTeam(player,"blue");NetworkTeamHelper.setPlayerTeam(enemy,"red");
        if(commands) player.connection=new net.minecraft.network.NetHandlerPlayServer(world.getMinecraftServer(),new net.minecraft.network.NetworkManager(net.minecraft.network.EnumPacketDirection.SERVERBOUND),player);
        world.getGameRules().setOrCreateGameRule("doMobSpawning","false");world.getGameRules().setOrCreateGameRule("randomTickSpeed","0");
        MinecraftForge.EVENT_BUS.register(this);
    }
    private FakePlayer pilot(long id) {
        return new FakePlayer(world,new GameProfile(new UUID(82,id),"TargetQA"+id)) {
            @Override public void sendMessage(ITextComponent text) { }
            @Override public Vec3d getPositionVector() { return new Vec3d(posX,posY,posZ); }
        };
    }
    private void check(String name,boolean success) {
        JsonObject row=new JsonObject();row.addProperty("kind",name);row.addProperty("success",success);cases.add(row);
        if(!success) failures.add(name);System.out.println("STRATEGICQA target "+row);
    }
    private ItemStack designator(int x,Integer y,int z) {
        Item item=WarTechContent.ITEM_TARGET_FINDER;
        if(y==null) {
            item=null;for(Item candidate:Item.REGISTRY) if(candidate.getClass().getName().endsWith(".ItemDesignatorRange")) { item=candidate;break; }
            if(item==null) throw new IllegalStateException("Native HBM ranged designator unavailable");
        }
        ItemStack stack=new ItemStack(item);NBTTagCompound tag=new NBTTagCompound();
        tag.setInteger("xCoord",x);tag.setInteger("zCoord",z);if(y!=null) tag.setInteger("yCoord",y);stack.setTagCompound(tag);return stack;
    }
    private ContainerLegacyEntity open(EntityStrategicTel t) {
        player.setPosition(t.posX+2,t.posY,t.posZ+2);ContainerLegacyEntity c=new ContainerLegacyEntity(player.inventory,t,ContainerLegacyEntity.Layout.STRATEGIC);player.openContainer=c;return c;
    }
    private void put(ContainerLegacyEntity c,ItemStack stack) { c.getSlot(((EntityStrategicTel)c.getTarget()).getTargetCount()).putStack(stack); }
    private void program(EntityStrategicTel t,int slot,BlockPos p) { check("program_slot_"+slot,t.programTarget(player,slot,p.getX(),p.getY(),p.getZ())); }
    private EntityStrategicTel clone(EntityStrategicTel original) {
        NBTTagCompound state=new NBTTagCompound();original.writeToNBT(state);EntityStrategicTel copy=new EntityStrategicTel(world);copy.readFromNBT(state);return copy;
    }
    private void pad(int x,int z,int radius) {
        for(int dx=-radius;dx<=radius;dx++) for(int dz=-radius;dz<=radius;dz++)
            world.setBlockState(new BlockPos(x+dx,4,z+dz),net.minecraft.init.Blocks.STONE.getDefaultState());
    }
    private int damaged(int x,int z,int radius) {
        int count=0;for(int dx=-radius;dx<=radius;dx++) for(int dz=-radius;dz<=radius;dz++) if(world.isAirBlock(new BlockPos(x+dx,4,z+dz))) count++;return count;
    }
    private EntityStrategicTel strategic(StrategicSystemProfile system,int x,int target) {
        // Vanilla entity ticks require the full 32-block neighbourhood, even in a fake-player fixture.
        for(int cx=(x>>4)-2;cx<=(x>>4)+2;cx++) for(int cz=1998;cz<=2002;cz++) world.getChunkFromChunkCoords(cx,cz);
        EntityStrategicTel t=new EntityStrategicTel(world,system);t.setPosition(x,4,32000);t.setOwnerIdentity(player.getUniqueID(),"blue");
        t.setInventorySlotContents(0,new ItemStack(WarTechContent.STRATEGIC_MISSILE,1,system.ordinal()));world.spawnEntity(t);
        player.setPosition(x+2,4,32002);t.programTarget(player,target,3,32000);check(system.getId()+"_deploy",t.handleLegacyGuiAction(1,player));return t;
    }
    private void commandBindings() {
        ItemStack single=new ItemStack(com.hbm.items.ModItems.detonator);player.setHeldItem(EnumHand.MAIN_HAND,single);player.setSneaking(true);
        player.setPosition(topol.posX+2,4,topol.posZ+2);TelChainDetonator.bind(player,single,topol);check("ordinary_detonator_binds_topol",TelChainDetonator.count(single)==1);
        single.getTagCompound().setTag("WarTechUavChain",new net.minecraft.nbt.NBTTagList());single.getTagCompound().setTag("WarTechCruiseLaunchers",new net.minecraft.nbt.NBTTagList());single.getTagCompound().setInteger("x",99);
        player.setPosition(oresh.posX+2,4,oresh.posZ+2);TelChainDetonator.bind(player,single,oresh);
        check("ordinary_binding_replaces_previous_and_other_groups",TelChainDetonator.count(single)==1 && single.getTagCompound().getTagList(TelChainDetonator.KEY,10).getCompoundTagAt(0).getUniqueId("Tel").equals(oresh.getUniqueID())
            && !single.getTagCompound().hasKey("WarTechUavChain") && !single.getTagCompound().hasKey("WarTechCruiseLaunchers") && !single.getTagCompound().hasKey("x"));
        TelChainDetonator.bind(player,single,oresh);check("duplicate_binding_does_not_duplicate",TelChainDetonator.count(single)==1);
        ItemStack denied=new ItemStack(com.hbm.items.ModItems.detonator);enemy.setSneaking(true);enemy.setHeldItem(EnumHand.MAIN_HAND,denied);enemy.setPosition(oresh.posX+2,4,oresh.posZ+2);
        TelChainDetonator.bind(enemy,denied,oresh);check("enemy_detonator_binding_denied",TelChainDetonator.count(denied)==0);
        check("enemy_remote_launch_denied",!oresh.beginLaunchFromDetonator(enemy));
        command=new ItemStack(com.hbm.items.ModItems.detonator_multi);player.setHeldItem(EnumHand.MAIN_HAND,command);
        player.setPosition(topol.posX+2,4,topol.posZ+2);topol.processInitialInteract(player,EnumHand.MAIN_HAND);
        player.setPosition(oresh.posX+2,4,oresh.posZ+2);oresh.processInitialInteract(player,EnumHand.MAIN_HAND);
        check("multi_detonator_links_both_via_real_interaction",TelChainDetonator.count(command)==2);
        ItemStack restored=new ItemStack(command.writeToNBT(new NBTTagCompound()).copy());check("bindings_preserve_uuid_dimension_position_in_nbt",TelChainDetonator.count(restored)==2 && restored.getTagCompound().equals(command.getTagCompound()));
        player.setHeldItem(EnumHand.MAIN_HAND,restored);player.setSneaking(true);
        net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem clear=new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem(player,EnumHand.MAIN_HAND);
        UavChainDetonator.onRightClickItem(clear);check("shift_air_clears_tel_bindings",TelChainDetonator.count(restored)==0 && clear.isCanceled() && TelChainDetonator.count(command)==2);
        player.setHeldItem(EnumHand.MAIN_HAND,command);player.setSneaking(false);
        check("iskander_stays_panel_launched",!tel.beginLaunchFromDetonator(player));
        EntityWarTechMissile tactical=LegacyEntityFactory.missile(world,MissileProfile.ISKANDER);tactical.ticksExisted=70;tactical.setPosition(0,90,0);
        check("iskander_target_class_two",com.wartec.wartecmod.port.network.MissileTrackingService.getThreatTier(tactical)==2);
        tactical.attackEntityFrom(DamageSource.GENERIC,1000);check("iskander_wreck_not_a_defense_target",tactical.getFlightStage()==3 && com.wartec.wartecmod.port.network.MissileTrackingService.getThreatTier(tactical)==0);
        for(StrategicSystemProfile system:new StrategicSystemProfile[]{StrategicSystemProfile.TOPOL_M,StrategicSystemProfile.ORESHNIK}) {
            EntityStrategicMissile target=new EntityStrategicMissile(world);target.configureBoost(system,100,4,100,null,"red");target.setPosition(0,100,0);target.ticksExisted=70;
            check(system.getId()+"_boost_target_class_three",com.wartec.wartecmod.port.network.MissileTrackingService.getThreatTier(target)==3);
            target.configureReentry(system,0,100,4,100,null,"red");target.ticksExisted=1;
            check(system.getId()+"_reentry_immediately_class_three",com.wartec.wartecmod.port.network.MissileTrackingService.getThreatTier(target)==3);
        }
    }
    @SubscribeEvent public void explosion(net.minecraftforge.event.world.ExplosionEvent.Start event) throws Exception {
        if(!strikes || event.getWorld()!=world) return;Vec3d p=event.getExplosion().getPosition();
        if(p.squareDistanceTo(new Vec3d(first))<400) {
            java.lang.reflect.Field size=net.minecraftforge.fml.relauncher.ReflectionHelper.findField(net.minecraft.world.Explosion.class,new String[]{"size","explosionSize","field_77280_f"});
            measuredStrength=Math.max(measuredStrength,size.getFloat(event.getExplosion()));
        }
    }
    @SubscribeEvent public void kineticExplosion(net.minecraftforge.event.world.ExplosionEvent.Detonate event) {
        // LegacyVntExplosion emits Detonate directly; Start belongs to the native HBM Iskander path.
        if(strikes && event.getWorld()==world && event.getExplosion().getPosition().squareDistanceTo(new Vec3d(34000.5,4,32000.5))<25) {
            centralBlast=true;System.out.println("STRATEGICQA central kinetic detonation at "+event.getExplosion().getPosition());
        }
    }
    @SubscribeEvent public void spawn(EntityJoinWorldEvent event) {
        if(strikes && event.getWorld()==world && event.getEntity() instanceof EntityStrategicMissile) {
            EntityStrategicMissile m=(EntityStrategicMissile)event.getEntity();
            if(!m.isReentryVehicle() && m.getStrategicSystem()==StrategicSystemProfile.TOPOL_M) topolBoost=m;
            if(m.isReentryVehicle() && m.getStrategicSystem()==StrategicSystemProfile.ORESHNIK) {
                entries.add(m);
                if(m.getLegacySelectedPayload()==6) check("seventh_warhead_targets_exact_center",Math.abs(m.getTargetX()-34000.5)<.01 && Math.abs(m.getTargetZ()-32000.5)<.01);
            }
            if(m.getTelLaunchOrigin().launcherId>=0) {
                EntityStrategicTel launcher=(EntityStrategicTel)world.getEntityByID(m.getTelLaunchOrigin().launcherId);
                check(m.getStrategicSystem().getId()+"_cold_launch_handoff_position",m.getPositionVector().squareDistanceTo(launcher.getLaunchPosition(0))<1e-9);
                check(m.getStrategicSystem().getId()+"_cold_launch_handoff_velocity",Math.abs(m.motionY-launcher.getStrategicSystem().separationSpeed())<1e-9);
            }
        }
        if(event.getWorld()!=world || !(event.getEntity() instanceof EntityWarTechMissile)) return;
        EntityWarTechMissile m=(EntityWarTechMissile)event.getEntity();TelLaunchOrigin origin=m.getTelLaunchOrigin();
        if(tel==null || origin.launcherId!=tel.getEntityId()) return;
        check("no_duplicate_spawn_slot_"+origin.slot,flights[origin.slot]==null);flights[origin.slot]=m;
        NBTTagCompound state=new NBTTagCompound();m.writeToNBT(state);BlockPos target=origin.slot==0?first:second;
        check("flight_uses_own_target_slot_"+origin.slot,state.getInteger("tX")==target.getX() && state.getInteger("tY")==target.getY() && state.getInteger("tZ")==target.getZ());
        check("native_iskander_profile_slot_"+origin.slot,m.getMissileProfile()==MissileProfile.ISKANDER);
    }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) throws Exception {
        if(event.phase!=TickEvent.Phase.END) return;tick++;
        if(tick==10) {
            for(int x=2035;x<=2040;x++) for(int z=1998;z<=2002;z++) world.getChunkFromChunkCoords(x,z);
            tel=new EntityStrategicTel(world,StrategicSystemProfile.ISKANDER);tel.setPosition(32600,4,32000);tel.setOwnerIdentity(player.getUniqueID(),"blue");world.spawnEntity(tel);
            container=open(tel);enemy.setPosition(player.posX,player.posY,player.posZ);
            check("two_targets_initially_unset",!tel.hasTarget(0) && !tel.hasTarget(1));
            program(tel,1,second);check("right_edit_does_not_set_left",!tel.hasTarget(0) && tel.getTarget(1).equals(second));
            EntityStrategicTel copy=clone(tel);check("unset_left_survives_nbt",!copy.hasTarget(0) && copy.hasTarget(1) && copy.getTarget(1).equals(second));
            program(tel,0,first);check("left_edit_preserves_right",tel.getTarget(1).equals(second));
            check("invalid_tube_rejected",!tel.programTarget(player,2,1,3,1) && !tel.programTarget(player,-1,1,3,1));
            check("invalid_elevation_rejected",!tel.programTarget(player,0,1,256,1) && !tel.programTarget(player,0,1,-1,1));
            check("world_limit_rejected",!tel.programTarget(player,0,Integer.MIN_VALUE,3,1));
            check("enemy_edit_rejected",!tel.programTarget(enemy,1,1,3,1));
            copy=clone(tel);check("two_distinct_targets_survive_nbt",copy.getTarget(0).equals(first) && copy.getTarget(1).equals(second));
            NBTTagCompound old=new NBTTagCompound();tel.writeToNBT(old);old.removeTag("TelTargetMask");old.removeTag("TelTargetFirst");old.removeTag("TelTargetSecond");
            copy=new EntityStrategicTel(world);copy.readFromNBT(old);check("old_shared_target_migrates_both_tubes",copy.hasTarget(0) && copy.hasTarget(1) && copy.getTarget(0).equals(first) && copy.getTarget(1).equals(first));
            check("tgt_slot_rejects_ammo",!container.getSlot(2).isItemValid(new ItemStack(WarTechContent.ITEM_ISKANDER_MISSILE)));
            check("tgt_slot_accepts_designator",container.getSlot(2).isItemValid(designator(first.getX(),3,first.getZ())) && container.getSlot(2).getSlotStackLimit()==1);
            ItemStack stack=designator(32960,40,32080);put(container,stack);
            check("manual_xyz_import",tel.importTarget(player,0,container) && tel.getTarget(0).equals(new BlockPos(32960,40,32080)));
            check("import_left_preserves_right",tel.getTarget(1).equals(second));
            check("designator_not_consumed",container.getDesignator()==stack && stack.getCount()==1);
            int revision=tel.getTargetRevision(0);check("same_target_import_acknowledged",tel.importTarget(player,0,container) && tel.getTargetRevision(0)>revision);
            put(container,designator(second.getX(),3,second.getZ()));check("right_import_independent",tel.importTarget(player,1,container) && tel.getTarget(1).equals(second));
            put(container,new ItemStack(WarTechContent.ITEM_TARGET_FINDER));check("empty_designator_rejected",!tel.importTarget(player,0,container));
            program(tel,0,first);
            put(container,designator(60000,null,60000));check("xz_height_resolution_queued",tel.importTarget(player,0,container));
            check("xz_import_defers_until_terrain",tel.getTarget(0).equals(first));
            if(strikes) {
                pad(33600,32080,32);pad(first.getX(),first.getZ(),32);pad(34000,32000,10);
                world.rand.setSeed(83);com.hbm.explosion.ExplosionLarge.explode(world,null,33600.5,5,32080.5,14,true,true,true);
                baselineDestroyed=damaged(33600,32080,32);check("old_strength14_baseline_measured",baselineDestroyed>0);
                topol=strategic(StrategicSystemProfile.TOPOL_M,32680,37000);oresh=strategic(StrategicSystemProfile.ORESHNIK,32760,34000);
                player.setPosition(tel.posX+2,tel.posY,tel.posZ+2);
            }
        }
        if(tick==35) {
            check("native_hbm_xz_height_resolved",tel.getTarget(0).getX()==60000 && tel.getTarget(0).getZ()==60000 && tel.getTarget(0).getY()==3);
            program(tel,0,first);put(container,designator(64000,null,64000));check("cancelled_edit_queued",tel.importTarget(player,0,container));program(tel,0,first);
        }
        if(tick==50) {
            check("late_import_cannot_overwrite_manual_edit",tel.getTarget(0).equals(first));
            put(container,designator(68000,null,68000));check("cancelled_item_queued",tel.importTarget(player,0,container));container.getDesignator().getTagCompound().setInteger("xCoord",68032);
        }
        if(tick==65) {
            check("changed_designator_invalidates_pending_import",tel.getTarget(0).equals(first));
            put(container,designator(72000,null,72000));check("cancelled_close_queued",tel.importTarget(player,0,container));player.openContainer=player.inventoryContainer;container.onContainerClosed(player);
            check("tgt_returned_on_close",container.getDesignator().isEmpty() && player.inventory.hasItemStack(designator(72000,null,72000)));
        }
        if(tick==80) {
            check("closed_panel_invalidates_pending_import",tel.getTarget(0).equals(first));
            check("closed_panel_import_rejected",!tel.importTarget(player,0,container));
            // Verify shift-click transfer into/out of the temporary slot without consuming it.
            container=open(tel);player.inventory.clear();player.inventory.setInventorySlotContents(9,designator(first.getX(),3,first.getZ()));
            check("shift_click_moves_designator_to_tgt",!container.transferStackInSlot(player,3).isEmpty() && !container.getDesignator().isEmpty() && player.inventory.getStackInSlot(9).isEmpty());
            check("shift_click_returns_designator",!container.transferStackInSlot(player,2).isEmpty() && container.getDesignator().isEmpty());
            for(StrategicSystemProfile system:new StrategicSystemProfile[]{StrategicSystemProfile.TOPOL_M,StrategicSystemProfile.ORESHNIK}) {
                EntityStrategicTel strategic=new EntityStrategicTel(world,system);strategic.setPosition(32600,4,32000);strategic.setOwnerIdentity(player.getUniqueID(),"blue");
                ContainerLegacyEntity c=open(strategic);put(c,designator(35000,3,32000));
                check(system.getId()+"_gui_import",strategic.importTarget(player,0,c) && strategic.getTarget(0).equals(new BlockPos(35000,3,32000)));
                check(system.getId()+"_no_second_target",!strategic.programTarget(player,1,35000,3,32000));c.onContainerClosed(player);
            }
            EntityStrategicTel direct=new EntityStrategicTel(world,StrategicSystemProfile.ISKANDER);direct.setPosition(32600,4,32000);direct.setOwnerIdentity(player.getUniqueID(),"blue");
            player.setHeldItem(EnumHand.MAIN_HAND,designator(first.getX(),3,first.getZ()));direct.processInitialInteract(player,EnumHand.MAIN_HAND);
            player.setHeldItem(EnumHand.MAIN_HAND,designator(second.getX(),3,second.getZ()));direct.processInitialInteract(player,EnumHand.MAIN_HAND);
            check("right_click_import_fills_two_unset_targets",direct.getTarget(0).equals(first) && direct.getTarget(1).equals(second));
            container=open(tel);program(tel,0,first);program(tel,1,second);
            tel.setInventorySlotContents(0,new ItemStack(WarTechContent.ITEM_ISKANDER_MISSILE));tel.setInventorySlotContents(1,new ItemStack(WarTechContent.ITEM_ISKANDER_MISSILE));
            check("deploy_two_target_launcher",tel.handleLegacyGuiAction(1,player));
        }
        if(tick==195) {
            check("left_launch_started",tel.beginLaunch(player,0));check("target_edit_locked_during_launch",!tel.programTarget(player,1,33000,3,32000));
            put(container,designator(33000,3,32000));check("import_locked_during_launch",!tel.importTarget(player,1,container));
            EntityStrategicTel saved=clone(tel);check("pending_launch_preserves_two_targets",saved.getLaunchTicks()>0 && saved.getTarget(0).equals(first) && saved.getTarget(1).equals(second));
            if(strikes) {
                if(commands) {
                    deferredTopol=new NBTTagCompound();topol.writeToNBT(deferredTopol);world.removeEntityDangerously(topol);
                    player.setPosition(50000,4,50000);player.setHeldItem(EnumHand.MAIN_HAND,command);player.setSneaking(false);
                    net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem click=new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem(player,EnumHand.MAIN_HAND);
                    UavChainDetonator.onRightClickItem(click);
                    check("detonator_air_click_consumed_without_ntm_empty_error",click.isCanceled());
                    check("detonator_starts_loaded_oreshnik_at_distance",oresh.getLaunchTicks()>0);
                    check("missing_uuid_queued_without_spending_ammo",TelChainDetonator.count(command)==1 && topol.getMissileMask()==1 && OperationalChunks.pending(world)>0);
                } else {
                for(EntityStrategicTel t:new EntityStrategicTel[]{topol,oresh}) {
                    player.setPosition(t.posX+2,t.posY,t.posZ+2);check(t.getStrategicSystem().getId()+"_fully_deployed_before_launch",t.getErectionProgress()==100);
                    check(t.getStrategicSystem().getId()+"_launch",t.beginLaunch(player));
                }
                }
                player.setPosition(tel.posX+2,tel.posY,tel.posZ+2);
            }
        }
        if(commands && tick==140) { commandBindings();player.setPosition(tel.posX+2,tel.posY,tel.posZ+2); }
        if(commands && tick==144) {
            ItemStack cancel=command.copy();net.minecraft.nbt.NBTTagList list=cancel.getTagCompound().getTagList(TelChainDetonator.KEY,10);
            for(int i=0;i<list.tagCount();i++) list.getCompoundTagAt(i).setUniqueId("Tel",new UUID(84,100+i));
            player.setHeldItem(EnumHand.MAIN_HAND,cancel);TelChainDetonator.launch(player,cancel);
            check("missing_uuid_requests_are_bounded_and_queued",OperationalChunks.pending(world)==2);
            player.setHeldItem(EnumHand.MAIN_HAND,command);
        }
        if(commands && tick==150) check("changing_held_detonator_cancels_pending_requests",OperationalChunks.pending(world)==0);
        if(commands && tick==205) {
            check("missing_uuid_did_not_launch_wrong_entity",TelChainDetonator.count(command)==1);
            topol=new EntityStrategicTel(world);topol.readFromNBT(deferredTopol);world.spawnEntity(topol);
        }
        if(commands && tick==215) {
            delayedStarted=topol.getLaunchTicks()>0;
            check("delayed_uuid_restoration_starts_queued_topol",delayedStarted && TelChainDetonator.count(command)==0);
            int sequence=topol.getLaunchTicks();UavChainDetonator.activate(player,command);
            check("repeated_click_does_not_restart_sequence",topol.getLaunchTicks()==sequence);
        }
        if(tick==195+StrategicSystemProfile.ISKANDER.getSequenceTicks()+5) {
            check("right_target_retained_after_left_launch",tel.getTarget(1).equals(second) && (tel.getMissileMask()&2)!=0);
            check("right_launch_started",tel.beginLaunch(player,1));
        }
        if(strikes && tick==330) {
            check("topol_boost_exists_for_interception",topolBoost!=null && !topolBoost.isDead);
            if(topolBoost!=null) {
                for(int i=0;i<6 && !topolBoost.isFallingDebris();i++) {
                    EntityWarTechMissile interceptor=LegacyEntityFactory.missile(world,MissileProfile.ANTI_AIR_TIER_3);
                    interceptor.setPosition(topolBoost.posX-1,topolBoost.posY+1,topolBoost.posZ);interceptor.setOwnerIdentity(enemy.getUniqueID(),"red");
                    interceptor.setTrackedEntity(topolBoost);interceptor.ticksExisted=40;interceptor.updateBlocked=true;MissileChunkLoader.spawnFlight(interceptor);VlsInterceptorGuidance.tick(interceptor,3);interceptor.setDead();
                }
                check("cold_launched_topol_still_intercepted",topolBoost.isFallingDebris() && !topolBoost.isArmed());
            }
        }
        boolean complete=flights[0]!=null && flights[1]!=null && flights[0].isDead && flights[1].isDead
            && (!strikes || tick>900 && entries.size()==7 && entries.stream().allMatch(m->m.isDead));
        if(tick>400 && cleanup<0 && (complete || tick>=1700)) {
            for(int slot=0;slot<2;slot++) { BlockPos p=slot==0?first:second;EntityWarTechMissile m=flights[slot];check("impact_at_distinct_target_slot_"+slot,m!=null && m.isDead && m.getDistanceSq(p)<100); }
            check("both_ammo_items_consumed_once",tel.getMissileMask()==0);tel.setDead();container.onContainerClosed(player);cleanup=tick;
            if(strikes) {
                int destroyed=damaged(first.getX(),first.getZ(),32);
                check("native_iskander_strength_28",Math.abs(measuredStrength-28)<.001);
                check("iskander_destroys_more_than_strength14",destroyed>baselineDestroyed*1.5);
                System.out.println("STRATEGICQA blast comparison old="+baselineDestroyed+" new="+destroyed+" native strength="+measuredStrength);
                check("seven_oreshnik_reentries",entries.size()==7);
                check("center_warhead_actually_detonates",centralBlast);
                check("target_center_structure_destroyed",world.isAirBlock(new BlockPos(34000,4,32000)));
                check("all_seven_reentries_finish",entries.size()==7 && entries.stream().allMatch(m->m.isDead));topol.setDead();oresh.setDead();
                for(Entity entity:new ArrayList<>(world.loadedEntityList)) if(entity instanceof EntityWarTechBase || entity.getClass().getName().startsWith("com.hbm.entity.")) entity.setDead();
            }
        }
        if(cleanup>0 && tick==cleanup+80) {
            String resources=MissileChunkLoader.diagnostics(world);check("flight_resources_released",resources.contains("flights=0/") && resources.contains("queue=0 chunks/0 owners"));
            JsonObject out=new JsonObject();out.addProperty("complete",true);out.add("cases",cases);out.add("failures",new Gson().toJsonTree(failures));out.addProperty("resources",resources);
            Files.write(Paths.get("flightqa-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(out).getBytes(StandardCharsets.UTF_8));
            System.out.println("FLIGHTQA targets finished failures="+failures);world.getMinecraftServer().initiateShutdown();
        }
    }
}
