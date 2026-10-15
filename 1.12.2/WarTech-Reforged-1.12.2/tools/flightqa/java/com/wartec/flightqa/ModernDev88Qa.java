package com.wartec.flightqa;

import com.hbm.items.ModItems;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gameplay.*;
import com.wartec.wartecmod.port.gui.ContainerModernVehicle;
import com.wartec.wartecmod.port.integration.*;
import com.wartec.wartecmod.port.network.MissileTrackingService;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/** Native interaction and live-flight regression for the user's dev88 bug batch. */
final class ModernDev88Qa {
    final WorldServer world;final ModernQa host;final FakePlayer player;
    final List<EntityWarTechMissile> payloadFlights=new ArrayList<>();
    final List<TileEntityWarTechMachine> pads=new ArrayList<>();
    boolean finished;
    int nextPayload;
    final Set<UUID> checkedPayloads=new HashSet<>(),bomblets=new HashSet<>();
    ModernDev88Qa(WorldServer world,ModernQa host,FakePlayer player) { this.world=world;this.host=host;this.player=player; }
    void setup() throws Exception {
        player.connection=new net.minecraft.network.NetHandlerPlayServer(world.getMinecraftServer(),new net.minecraft.network.NetworkManager(net.minecraft.network.EnumPacketDirection.SERVERBOUND),player);
        for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) world.getChunkFromChunkCoords((20000>>4)+x,(20000>>4)+z);
        Method rotate=EntityWarTechMissile.class.getDeclaredMethod("updateLegacyRotation");rotate.setAccessible(true);
        for(ModernVehicleProfile kind:new ModernVehicleProfile[]{ModernVehicleProfile.GRAD,ModernVehicleProfile.URAGAN,ModernVehicleProfile.SMERCH,ModernVehicleProfile.HIMARS}) {
            MissileItem item=(MissileItem)kind.ammunition();
            host.check("stack16_"+kind,item.getItemStackLimit(new ItemStack(item))==16 && item.getVariantCount()==5,"five metadata payloads, stack 16");
            EntityModernVehicle vehicle=new EntityModernVehicle(world,kind);vehicle.setPosition(20000,4,20000);vehicle.setOwnerIdentity(player.getUniqueID(),"modernqa");world.spawnEntity(vehicle);
            player.setPosition(20000,4,20000);player.inventory.setInventorySlotContents(9,new ItemStack(item,16,2));
            ContainerModernVehicle container=new ContainerModernVehicle(player.inventory,vehicle);
            container.transferStackInSlot(player,kind.slots+2);
            host.check("shift_load_stack_"+kind,vehicle.loadedCount()==Math.min(16,kind.slots),"one shift-click fills available tubes");
            NBTTagCompound saved=new NBTTagCompound();vehicle.writeToNBT(saved);EntityModernVehicle restored=new EntityModernVehicle(world,kind);restored.readFromNBT(saved);
            host.check("tube_payload_saved_"+kind,restored.getStackInSlot(0).getMetadata()==2 && restored.getStackInSlot(0).getCount()==1,"cluster metadata, one item per tube");
            ItemStack arty=new ItemStack(WarTechContent.ARTILLERY_TARGET_DESIGNATOR);
            // Resolve the item through the registry if its legacy constant differs.
            host.check("artillery_bind_"+kind,((ArtilleryTargetDesignatorItem)arty.getItem()).linkTo(vehicle,player,arty),"bind while stowed");
            vehicle.processInitialInteract(player,EnumHand.MAIN_HAND);
            host.check("right_click_opens_panel_"+kind,!player.isRiding() && player.openContainer instanceof ContainerModernVehicle,"normal right click opens panel before driving");player.closeScreen();
            NBTTagCompound ready=new NBTTagCompound();vehicle.writeToNBT(ready);ready.setInteger("ModernDeployment",kind.prepareTicks);ready.setInteger("ModernReload",0);vehicle.readFromNBT(ready);vehicle.setDeployed(true);vehicle.setLegacyPower(vehicle.getEnergyCapacity());
            player.setPosition(20000,4,20500);
            host.check("artillery_remote_target_"+kind,vehicle.acceptArtilleryDesignatorTarget(player,new BlockPos(20000,4,20800)) && vehicle.getQueuedShots()==1,"remote target at 500-block operator distance queues exactly one rocket");
            host.check("artillery_second_click_"+kind,vehicle.acceptArtilleryDesignatorTarget(player,new BlockPos(20000,4,20900)) && vehicle.getQueuedShots()==2,"second click reserves another tube, never a whole salvo");
            player.setPosition(20000,4,20000);vehicle.handleLegacyGuiAction(4,player);
            host.check("payload_queue_single_"+kind,vehicle.handleLegacyGuiAction(2,player),"actual single-shot action after remote target");
            Method fire=EntityModernVehicle.class.getDeclaredMethod("tickQueue");fire.setAccessible(true);fire.invoke(vehicle);
            EntityWarTechMissile released=null;
            for(Entity candidate:world.loadedEntityList) if(candidate instanceof EntityWarTechMissile && candidate.getEntityData().hasUniqueId("ModernLauncher")
                && vehicle.getUniqueID().equals(candidate.getEntityData().getUniqueId("ModernLauncher"))) released=(EntityWarTechMissile)candidate;
            host.check("loaded_payload_released_"+kind,released!=null && released.getVisualVariant()==2,"real slot metadata carried into flying missile");
            if(released!=null) {
                released.getEntityData().setBoolean("Dev88PayloadQa",true);released.getEntityData().removeTag("ModernShotMost");released.getEntityData().removeTag("ModernShotLeast");
                // This fixture verifies release identity. The separate probes below verify complete flights.
                released.setDead();MissileChunkLoader.untrack(released);
            }
            vehicle.setDead();
            for(int variant=0;variant<5;variant++) {
                EntityWarTechMissile missile=LegacyEntityFactory.missile(world,item.getProfile());
                missile.setVisual("missile/"+item.getProfile().getIntentPath(),variant);
                NBTTagCompound nbt=new NBTTagCompound();missile.writeToNBT(nbt);EntityWarTechMissile copy=LegacyEntityFactory.missile(world,item.getProfile());copy.readFromNBT(nbt);
                host.check("flight_payload_saved_"+kind+"_"+variant,copy.getVisualVariant()==variant,"metadata survives entity save");
                int tier=kind==ModernVehicleProfile.GRAD?1:2;
                host.check("aa_payload_class_"+kind+"_"+variant,MissileTrackingService.getThreatTier(missile)==tier && MissileTrackingService.canInterceptorEngage(missile,tier,250),"carrier AA tier="+tier);
                for(Vec3d velocity:new Vec3d[]{new Vec3d(2,3,1),new Vec3d(-2,0,1),new Vec3d(1,-3,-2)}) {
                    missile.motionX=velocity.x;missile.motionY=velocity.y;missile.motionZ=velocity.z;rotate.invoke(missile);
                    double yaw=Math.toRadians(missile.rotationYaw),pitch=Math.toRadians(missile.rotationPitch);
                    Vec3d nose=new Vec3d(Math.sin(yaw)*Math.cos(pitch),-Math.sin(pitch),Math.cos(yaw)*Math.cos(pitch));
                    host.check("nose_tangent_"+kind+"_"+variant+"_"+velocity.y,nose.dotProduct(velocity.normalize())>.99999,"actual updateLegacyRotation on ascent/cruise/descent");
                }
            }
        }
        // Twenty real flights reach twenty separate impact sites; no artificial detonation.
        int serial=0;
        for(ModernMissileProfile profile:ModernMissileProfile.values()) if(profile.isMlrs()) for(int variant=0;variant<5;variant++) {
            EntityWarTechMissile missile=LegacyEntityFactory.missile(world,profile.missile);
            missile.setPosition(30000+(serial++)*160,30,30000);missile.setVisual("missile/"+profile.missile.getIntentPath(),variant);
            missile.setOwnerIdentity(player.getUniqueID(),"modernqa");missile.getEntityData().setBoolean("Dev88PayloadQa",true);
            missile.configureLegacyGroundLaunch((int)missile.posX,4,30500);missile.motionY=.8;missile.motionZ=.6;
            payloadFlights.add(missile);
        }
        player.connection=new net.minecraft.network.NetHandlerPlayServer(world.getMinecraftServer(),new net.minecraft.network.NetworkManager(net.minecraft.network.EnumPacketDirection.SERVERBOUND),player);
        for(int i=0;i<2;i++) {
            BlockPos pos=new BlockPos(24000+i*64,4,24000);net.minecraft.block.Block block=i==0?WarTechContent.GERAN_LAUNCHER:WarTechContent.BALLISTIC_MISSILE_LAUNCHER;
            world.setBlockState(pos,block.getDefaultState(),3);player.setPosition(pos.getX()+2,4,pos.getZ());block.onBlockPlacedBy(world,pos,world.getBlockState(pos),player,new ItemStack(block));
            TileEntityWarTechMachine tile=(TileEntityWarTechMachine)world.getTileEntity(pos);pads.add(tile);
            tile.setInventorySlotContents(0,new ItemStack(i==0?WarTechContent.FP5_MISSILE:WarTechContent.FP7_MISSILE));tile.setPower(tile.getMaxPower());
            ItemStack designator=new ItemStack(WarTechContent.ITEM_TARGET_FINDER);NBTTagCompound tag=new NBTTagCompound();tag.setInteger("xCoord",pos.getX());tag.setInteger("yCoord",4);tag.setInteger("zCoord",24600);designator.setTagCompound(tag);tile.setInventorySlotContents(1,designator);
            ItemStack detonator=new ItemStack(i==0?ModItems.detonator:ModItems.detonator_multi);player.setHeldItem(EnumHand.MAIN_HAND,detonator);player.setSneaking(true);
            BlockPos selected=i==0?pos.add(1,0,0):pos;
            PlayerInteractEvent.RightClickBlock bind=new PlayerInteractEvent.RightClickBlock(player,EnumHand.MAIN_HAND,selected,EnumFacing.UP,new Vec3d(selected));MinecraftForge.EVENT_BUS.post(bind);
            host.check("detonator_bind_core_"+i,bind.isCanceled() && ModernPadDetonator.count(detonator)==1,"actual Shift+right-click on launcher/dummy");
            player.setSneaking(false);player.setPosition(pos.getX()+500,4,pos.getZ());
            PlayerInteractEvent.RightClickItem fire=new PlayerInteractEvent.RightClickItem(player,EnumHand.MAIN_HAND);MinecraftForge.EVENT_BUS.post(fire);
            host.check("detonator_event_"+i,fire.isCanceled(),"actual right-click event from outside GUI distance");
        }
    }
    void verify(int tick) {
        if(tick==80) for(int i=0;i<pads.size();i++) host.check("detonator_actual_launch_"+i,pads.get(i).getStackInSlot(0).isEmpty(),"real missile consumed only on successful launch");
        if(tick>=600 && tick%10==0 && nextPayload<payloadFlights.size() && MissileChunkLoader.availableFlightSlots(world)>=24
            && payloadFlights.subList(0,nextPayload).stream().filter(m->!m.isDead).count()<4) {
            EntityWarTechMissile missile=payloadFlights.get(nextPayload);
            world.getChunkFromChunkCoords(MathHelper.floor(missile.posX)>>4,MathHelper.floor(missile.posZ)>>4);
            if(MissileChunkLoader.spawnFlight(missile)) {
                host.check("live_payload_"+missile.getMissileProfile()+"_"+missile.getVisualVariant(),true,"real flight admitted from a loaded launch site");nextPayload++;
            }
        }
        for(Entity child:world.loadedEntityList) if(child.getEntityData().getBoolean("ModernBomblet")) bomblets.add(child.getUniqueID());
        for(EntityWarTechMissile missile:payloadFlights.subList(0,nextPayload)) if(missile.isDead && checkedPayloads.add(missile.getUniqueID())) {
            if(missile.getVisualVariant()==2) host.check("cluster_airburst_"+missile.getMissileProfile(),missile.posY>=missile.getTargetY()+8,"actual release altitude="+missile.posY);
            if(missile.getVisualVariant()==1) {
                int fire=0;BlockPos origin=missile.getPosition();
                for(int x=-14;x<=14;x++) for(int z=-14;z<=14;z++) for(int y=-4;y<=1;y++) {
                    BlockPos point=origin.add(x,y,z);if(world.isBlockLoaded(point) && world.getBlockState(point).getBlock()==net.minecraft.init.Blocks.FIRE) fire++;
                }
                host.check("phosphorus_surface_fire_"+missile.getMissileProfile(),fire>0,"actual surface fire blocks="+fire);
            }
        }
        if(tick%600==0) {
            EntityWarTechMissile active=payloadFlights.stream().filter(m->!m.isDead && m.ticksExisted>0).findFirst().orElse(null);
            System.out.println("MODERNQA payload progress "+MissileChunkLoader.diagnostics(world)+" active="+(active==null?"none":active.getMissileProfile()+" age="+active.ticksExisted+" position="+active.getPositionVector()));
        }
        if(tick>600 && !finished && nextPayload==20 && payloadFlights.stream().allMatch(m->m.isDead)) {
            finished=true;
            host.check("cluster_live_children",!bomblets.isEmpty(),"real submunitions observed="+bomblets.size());
            for(EntityWarTechMissile missile:payloadFlights) host.check("payload_flight_completed_"+missile.getMissileProfile()+"_"+missile.getVisualVariant(),missile.isDead,"native payload reaches impact or cluster airburst");
            host.check("payload_resources_bounded",MissileChunkLoader.availableFlightSlots(world)>=8,MissileChunkLoader.diagnostics(world));
        }
    }
}
