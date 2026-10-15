package com.wartec.wartecmod.port.gameplay;

import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.integration.*;
import com.wartec.wartecmod.port.planning.*;
import com.wartec.wartecmod.port.silo.*;
import api.hbm.energy.IBatteryItem;
import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;

/** Single server authority for a stationary strategic starter and its optional surface site. */
public final class TileEntityMissileSilo extends HbmPoweredTileEntity implements IInventory,ITickable {
    private final NonNullList<ItemStack> items=NonNullList.withSize(3,ItemStack.EMPTY);
    public EnumFacing facing=EnumFacing.SOUTH;
    public BlockPos anchor,hatchPos,linkedStart,target;
    public int buildIndex=-1,phase,phaseTicks,cooldown;
    private int dryIndex;
    private List<SiloStructure.Cell> blueprint;
    private EntityPlayer builder;
    private UUID launchId,operationId,stepId;
    public String status="silo.state.empty";
    private boolean targetSet;
    public long revision;
    private long targetRevision;
    public boolean builtStructure,targetLoading;
    private boolean launchReady;
    private boolean defenseEnabled,defenseRole;private UUID defenseTarget;private int defenseTargetId=-1;
    public boolean isDefenseEnabled(){return defenseEnabled;}
    public boolean isDefenseMissile(){return items.get(0).getItem()==WarTechContent.SILO_INTERCEPTOR;}
    public boolean isDefenseMode(){return defenseRole || isDefenseMissile() || defenseTarget!=null;}
    public long defenseKey(){return instance().getMostSignificantBits() ^ instance().getLeastSignificantBits();}
    public boolean setDefenseEnabled(EntityPlayer p,boolean value){if(!mayUse(p) || locked() || !isDefenseMode())return false;defenseRole=true;defenseEnabled=value;status=value?"silo.abm.wait_radar":"silo.abm.off";changed();return true;}
    public boolean isLaunchReady(){return launchReady;}
    public boolean payloadLocked(){return locked() || launchReady;}
    public boolean constructionLocked;
    public int buildCredits;
    private BlockPos rebuildController;
    private NBTTagCompound storedCore;
    private java.util.function.BooleanSupplier pendingTargetValid;
    private long targetDeadline;
    private int lightMode=-1;
    public long getTargetRevision(){return targetRevision;}
    public boolean isSite(){return world!=null && world.getBlockState(pos).getBlock()==WarTechContent.MISSILE_SILO;}
    public boolean isStarter(){return world!=null && world.getBlockState(pos).getBlock()==WarTechContent.SILO_START;}
    public TileEntityMissileSilo core(){if(!isSite())return this;if(linkedStart==null || !world.isBlockLoaded(linkedStart))return null;TileEntity t=world.getTileEntity(linkedStart);return t instanceof TileEntityMissileSilo && PlanningAccessPolicy.same(PlanningAccessPolicy.scope(this),PlanningAccessPolicy.scope(t))?(TileEntityMissileSilo)t:null;}
    public void claim(EntityPlayer p){PlanningAccessPolicy.claim(this,p);if(getOwnerTeam().isEmpty())setOwnerTeam(NetworkTeamHelper.getPlayerTeam(p));}
    public boolean mayUse(EntityPlayer p){return p!=null && !p.isSpectator() && !p.isDead && world==p.world && PlanningAccessPolicy.same(PlanningAccessPolicy.scope(p),PlanningAccessPolicy.scope(this));}
    public UUID instance(){return PlanningAccessPolicy.instance(this);}
    public UUID getLaunchId(){return launchId;}
    public StrategicSystemProfile system(){if(isDefenseMissile() || defenseTarget!=null)return StrategicSystemProfile.ORESHNIK;return !missile(items.get(0))?StrategicSystemProfile.TOPOL_M:((StrategicMissileItem)items.get(0).getItem()).getSystem(items.get(0));}
    public NBTTagCompound commandDefenseTelemetry(){NBTTagCompound n=new NBTTagCompound();n.setLong("Key",defenseKey());n.setInteger("Cooldown",cooldown);n.setInteger("Pending",defenseTargetId);n.setInteger("Delay",phase==1?Math.max(35,140-phaseTicks):140);n.setBoolean("Enabled",defenseEnabled);n.setBoolean("Deployed",true);n.setString("Stage",phase==2?"exit":phase==1?"opening":"guard");n.setString("NativeReason",status);return n;}
    public static boolean missile(ItemStack s){return !s.isEmpty() && (s.getItem()==WarTechContent.SILO_INTERCEPTOR || s.getItem()==WarTechContent.STRATEGIC_MISSILE && (s.getMetadata()==0 || s.getMetadata()==2));}
    public boolean locked(){return constructionLocked || phase!=0 || cooldown>0;}
    public boolean hasTarget(){return targetSet && target!=null;}
    @Override public long getMaxPower(){return 1_200_000L;}
    public void changed(){revision++;notifyStateChanged();}
    public void tell(EntityPlayer p,String message){if(p!=null)p.sendStatusMessage(new TextComponentTranslation(message),true);}
    public BlockPos siteAnchor(){return anchor!=null?anchor:SiloGeometry.at(pos,facing,0,0,-6);}
    public String checkSite(){return SiloStructure.check(world,siteAnchor(),facing);}
    private boolean canConstruct(EntityPlayer p,BlockPos at){
        if(!p.canPlayerEdit(at,EnumFacing.UP,p.getHeldItemMainhand()))return false;
        if(!world.isAirBlock(at) && MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.BlockEvent.BreakEvent(world,at,world.getBlockState(at),p)))return false;
        return !ForgeEventFactory.onPlayerBlockPlace(p,BlockSnapshot.getBlockSnapshot(world,at),EnumFacing.UP,EnumHand.MAIN_HAND).isCanceled();
    }
    public boolean build(EntityPlayer player){
        if(!isSite() || linkedStart!=null || buildIndex>=0 || !mayUse(player))return false;
        String error=checkSite();if(!error.isEmpty()){status=error;changed();return false;}
        if(!canConstruct(player,pos)){status="silo.error.protected";changed();return false;}
        anchor=siteAnchor();blueprint=SiloStructure.cells(facing);
        // Check permissions before altering any cell. The user authorizes clearing this volume.
        for(int i=0;i<blueprint.size();i++){
            SiloStructure.Cell c=blueprint.get(i);BlockPos p=SiloGeometry.at(anchor,facing,c.x,c.y,c.z);
            if(p.equals(pos))continue;
            if(!canConstruct(player,p)){
                status="silo.error.protected";changed();return false;
            }
        }
        boolean reuse=storedCore!=null && reusableStructure();
        if(!player.capabilities.isCreativeMode && !reuse) {
            if(buildCredits>0)buildCredits--;
            else if(!consumeConstructionKit(player)){status=storedCore==null?"silo.error.materials":"silo.error.renovation_materials";tell(player,status);changed();return false;}
        }
        builder=player;buildIndex=0;dryIndex=0;status="silo.state.building";changed();return true;
    }
    private boolean reusableStructure(){
        if(anchor==null || HbmSiloHatch.get(world,HbmSiloHatch.controller(anchor,HbmSiloHatch.prefabFacing(facing)))==null)return false;
        for(int floor:SiloGeometry.FLOORS)if(world.getBlockState(SiloGeometry.at(anchor,facing,-1,floor+2,7)).getBlock()!=net.minecraft.init.Blocks.WALL_SIGN)return false;
        for(SiloStructure.Cell cell:blueprint){Block wanted=cell.state.getBlock();if(wanted==net.minecraft.init.Blocks.AIR)continue;
            BlockPos at=SiloGeometry.at(anchor,facing,cell.x,cell.y,cell.z);
            if(!world.isBlockLoaded(at) || world.getBlockState(at).getBlock()!=wanted)return false;}
        return true;
    }
    private boolean consumeConstructionKit(EntityPlayer p){
        net.minecraft.item.Item kit=com.wartec.wartecmod.port.production.ProductionCatalog.stack("plan:silo_build_kit",1).getItem();
        for(int i=0;i<p.inventory.getSizeInventory();i++){ItemStack s=p.inventory.getStackInSlot(i);if(!s.isEmpty() && s.getItem()==kit){s.shrink(1);p.inventory.markDirty();return true;}}
        return false;
    }
    public boolean renovate(EntityPlayer player){
        TileEntityMissileSilo old=core();if(!isSite() || old==null || old==this || !mayUse(player) || old.payloadLocked() || old.targetLoading)return false;
        BlockPos previous=linkedStart;storedCore=old.writeToNBT(new NBTTagCompound());linkedStart=null;
        if(!build(player)){linkedStart=previous;storedCore=null;changed();return false;}
        old.constructionLocked=true;old.rebuildController=pos;old.changed();
        if(player instanceof net.minecraft.entity.player.EntityPlayerMP && ((net.minecraft.entity.player.EntityPlayerMP)player).connection!=null){
            ((net.minecraft.entity.player.EntityPlayerMP)player).closeScreen();
            player.openGui(com.wartec.wartecmod.WarTechReforged.instance,com.wartec.wartecmod.port.gui.WarTechGuiHandler.GUI_SILO,world,pos.getX(),pos.getY(),pos.getZ());
        }return true;
    }
    private void buildTick(){
        if(blueprint==null)blueprint=SiloStructure.cells(facing);
        if(buildIndex>blueprint.size()){status="silo.error.structure";return;}
        if(builder==null || builder.isDead)builder=world.getMinecraftServer().getPlayerList().getPlayerByUUID(PlanningAccessPolicy.tileOwner(this));
        if(builder==null || !mayUse(builder)){status="silo.state.wait_builder";return;}
        long start=System.nanoTime();int budget=0;
        while(buildIndex<blueprint.size() && budget++<128 && System.nanoTime()-start<3_000_000L){
            SiloStructure.Cell c=blueprint.get(buildIndex);BlockPos p=SiloGeometry.at(anchor,facing,c.x,c.y,c.z);
            if(!p.equals(pos)){
                if(!world.isBlockLoaded(p)){status="silo.error.unloaded";return;}
                if(!canConstruct(builder,p)){status="silo.error.protected";changed();return;}
                // Wall buttons must be placed after their backing walls, in every orientation.
                SiloStructure.replace(world,p,c.state.getBlock() instanceof net.minecraft.block.BlockLever?net.minecraft.init.Blocks.AIR.getDefaultState():c.state);
            }
            buildIndex++;markDirty();
        }
        if(buildIndex>=blueprint.size()){
            // Remove fluid which flowed into completed lower rooms while digging above them.
            // A top-down pass clears upstream cells first, then restores washed-out fixtures.
            if(dryIndex<blueprint.size()){
                if(!status.equals("silo.state.sealing")){status="silo.state.sealing";changed();}
                long sweepStart=System.nanoTime();int sweepBudget=0;
                while(dryIndex<blueprint.size() && sweepBudget++<128 && System.nanoTime()-sweepStart<3_000_000L){
                    SiloStructure.Cell c=blueprint.get(blueprint.size()-1-dryIndex);BlockPos p=SiloGeometry.at(anchor,facing,c.x,c.y,c.z);
                    net.minecraft.block.state.IBlockState wanted=c.state.getBlock() instanceof net.minecraft.block.BlockLever?net.minecraft.init.Blocks.AIR.getDefaultState():c.state;
                    if(!p.equals(pos) && Block.getStateId(world.getBlockState(p))!=Block.getStateId(wanted)){
                        if(!world.isBlockLoaded(p) || !canConstruct(builder,p)){status="silo.error.protected";changed();return;}
                        SiloStructure.replace(world,p,wanted);
                    }
                    dryIndex++;markDirty();
                }
                if(dryIndex<blueprint.size())return;
            }
            for(SiloStructure.Cell c:blueprint)if(c.state.getBlock() instanceof net.minecraft.block.BlockLever){
                BlockPos p=SiloGeometry.at(anchor,facing,c.x,c.y,c.z);
                if(!world.isBlockLoaded(p) || !canConstruct(builder,p)){status="silo.error.protected";changed();return;}
                SiloStructure.replace(world,p,c.state);
            }
            if(!HbmSiloHatch.place(world,anchor,HbmSiloHatch.prefabFacing(facing),builder)){if(!status.equals("silo.error.hatch")){status="silo.error.hatch";changed();}return;}
            linkedStart=anchor.down(SiloGeometry.START_DEPTH);
            TileEntity t=world.getTileEntity(linkedStart);
            if(!(t instanceof TileEntityMissileSilo)){status="silo.error.structure";changed();return;}
            TileEntityMissileSilo core=(TileEntityMissileSilo)t;
            boolean restoring=storedCore!=null;if(restoring)core.readFromNBT(storedCore);
            core.claim(builder);core.facing=facing;core.anchor=anchor;core.builtStructure=true;core.hatchPos=HbmSiloHatch.controller(anchor,HbmSiloHatch.prefabFacing(facing));core.changed();
            BlockPos destination=SiloGeometry.at(anchor,facing,0,-4,6);
            TileEntity console=world.getTileEntity(destination);
            if(!(console instanceof TileEntityMissileSilo)){status="silo.error.structure";changed();return;}
            SiloStructure.labels(world,anchor,facing);
            buildIndex=-1;blueprint=null;storedCore=null;builtStructure=true;status="silo.state.built";changed();
            // Move the original module's identity into the room, then use the single bottom inventory.
            NBTTagCompound saved=writeToNBT(new NBTTagCompound());
            saved.setInteger("x",destination.getX());saved.setInteger("y",destination.getY());saved.setInteger("z",destination.getZ());
            TileEntityMissileSilo panel=(TileEntityMissileSilo)console;panel.readFromNBT(saved);
            for(int i=0;i<items.size();i++){if(!restoring)core.items.set(i,items.get(i));panel.items.set(i,ItemStack.EMPTY);items.set(i,ItemStack.EMPTY);}
            if(!restoring){core.target=target;core.targetSet=targetSet;core.targetRevision=targetRevision;core.setPower(core.getPower()+getPower());}
            core.status=missile(core.items.get(0))?"silo.state.ready":"silo.state.empty";
            panel.setPower(0);core.changed();panel.changed();
            java.util.List<net.minecraft.entity.player.EntityPlayerMP> viewers=new ArrayList<>();
            for(net.minecraft.entity.player.EntityPlayerMP p:world.getMinecraftServer().getPlayerList().getPlayers())
                if(p.openContainer instanceof com.wartec.wartecmod.port.gui.ContainerMissileSilo && ((com.wartec.wartecmod.port.gui.ContainerMissileSilo)p.openContainer).source==this){p.closeScreen();viewers.add(p);}
            BlockPos previous=pos;if(!previous.equals(destination)){world.removeTileEntity(previous);world.setBlockState(previous,WarTechContent.SILO_DECK.getDefaultState(),3);}
            for(net.minecraft.entity.player.EntityPlayerMP p:viewers)if(panel.mayUse(p) && p.getDistanceSq(destination)<=64)
                p.openGui(com.wartec.wartecmod.WarTechReforged.instance,com.wartec.wartecmod.port.gui.WarTechGuiHandler.GUI_SILO,world,destination.getX(),destination.getY(),destination.getZ());
        }else if(world.getTotalWorldTime()%10==0)changed();
    }
    public boolean findHatch(){
        if(hatchPos!=null && HbmSiloHatch.get(world,hatchPos)!=null)return true;
        for(int dy=18;dy<=63;dy++){
            BlockPos center=pos.up(dy);
            for(EnumFacing f:EnumFacing.HORIZONTALS){BlockPos p=HbmSiloHatch.controller(center,f);if(HbmSiloHatch.get(world,p)!=null){anchor=center;hatchPos=p;changed();return true;}}
        }return false;
    }
    public boolean program(EntityPlayer p,BlockPos value){if(!mayUse(p) || locked() || value.getY()<0 || value.getY()>255 || !world.getWorldBorder().contains(value))return false;target=value;targetSet=true;targetRevision++;targetLoading=false;pendingTargetValid=null;status=launchReady?"silo.state.launch_ready":"silo.state.target_set";changed();return true;}
    public void importTarget(EntityPlayer p,net.minecraft.inventory.Container window){
        if(!mayUse(p) || locked() || !(world instanceof WorldServer))return;ItemStack original=items.get(1).copy();long rev=targetRevision;
        pendingTargetValid=()->!isInvalid() && mayUse(p) && p.openContainer==window && !locked() && targetRevision==rev && ItemStack.areItemStacksEqual(original,items.get(1));
        targetLoading=true;targetDeadline=world.getTotalWorldTime()+200;status="silo.state.target_loading";changed();
        java.util.function.BooleanSupplier guard=pendingTargetValid;
        if(!DesignatorCompat.resolveSavedTarget((WorldServer)world,original.copy(),Double.NaN,()->targetLoading && guard.getAsBoolean(),point->program(p,new BlockPos(point)))){
            targetLoading=false;pendingTargetValid=null;status="silo.error.target";changed();tell(p,"silo.error.target");
        }
    }
    public String launchError(){
        if(isDefenseMissile())return "silo.abm.automatic";
        if(targetLoading)return "silo.state.target_loading";
        if(!isStarter() || !StrategicFeature.isEnabled())return "silo.error.disabled";
        if(locked())return "silo.error.busy";
        if(!missile(items.get(0)))return "silo.error.ammo";
        if(!hasTarget())return "silo.error.target";
        if(!world.getWorldBorder().contains(target))return "silo.error.range";
        double range=Math.hypot(target.getX()+.5-pos.getX()-.5,target.getZ()+.5-pos.getZ()-.5);
        if(range<system().getMinimumRange() || range>system().getMaximumRange())return "silo.error.range";
        if(!hasPower(75_000))return "silo.error.power";
        if(!findHatch())return "silo.error.hatch";
        if(!shaftClear(false))return "silo.error.clearance";
        if(!MissileChunkLoader.hasCapacity(world))return "flight.error.chunks";
        return "";
    }
    /** Readiness never admits a flight, opens the hatch or consumes the launch charge. */
    public boolean setLaunchReady(EntityPlayer p,boolean ready){return mayUse(p) && readiness(ready);}
    private boolean readiness(boolean ready){
        if(locked() || !isStarter() || isDefenseMissile())return false;
        if(!ready){launchReady=false;status=missile(items.get(0))?"silo.state.ready":"silo.state.empty";changed();return true;}
        String error=launchError();
        if(error.isEmpty() && HbmSiloHatch.get(world,hatchPos).getState()!=com.hbm.interfaces.IDoor.DoorState.CLOSED)error="silo.error.close_first";
        if(!error.isEmpty()){status=error;changed();return false;}
        launchReady=true;status="silo.state.launch_ready";changed();return true;
    }
    public boolean beginLaunch(EntityPlayer p){if(!mayUse(p))return false;return begin(null);}
    private boolean begin(PlanningCommand c){
        String error=launchError();if(!error.isEmpty()){status=error;changed();return false;}
        if(!HbmSiloHatch.open(world,hatchPos)){status="silo.error.hatch";changed();return false;}
        launchReady=false;phase=1;phaseTicks=0;launchId=UUID.randomUUID();operationId=c==null?null:c.operation;stepId=c==null?null:c.step;
        status="silo.state.opening";changed();return true;
    }
    public void revokePlanning(java.util.UUID id){if(id.equals(operationId) && phase==1 && !items.get(0).isEmpty())abort("silo.state.cancelled");}
    public boolean planningOrder(PlanningCommand c,AttackOperation.Action action,BlockPos value){
        if(!c.owns(this) || !isStarter())return false;
        if(isDefenseMissile()){if(action!=AttackOperation.Action.PREPARE || locked())return false;defenseEnabled=true;status="silo.abm.wait_radar";changed();return true;}
        if((action!=AttackOperation.Action.PREPARE && action!=AttackOperation.Action.LAUNCH) || locked() || targetLoading || value==null || value.getY()<0 || value.getY()>255 || !world.getWorldBorder().contains(value))return false;
        target=value;targetSet=true;targetRevision++;
        return action==AttackOperation.Action.PREPARE?readiness(true):begin(c);
    }
    /** Exact 3x3 swept launch volume. Native HBM dummies are ignored only in their own lid plane during opening. */
    public boolean shaftClear(boolean requireOpen){
        if(anchor==null || hatchPos==null || !world.isBlockLoaded(anchor.up(20)))return false;
        for(int y=pos.getY()+1;y<=anchor.getY()+20;y++)for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
            BlockPos p=new BlockPos(pos.getX()+x,y,pos.getZ()+z);if(!world.isBlockLoaded(p))return false;
            Block b=world.getBlockState(p).getBlock();
            if(!requireOpen && y==anchor.getY() && b==com.hbm.blocks.ModBlocks.dummy_block_silo_hatch)continue;
            if(!world.isAirBlock(p))return false;
        }
        AxisAlignedBB moving=new AxisAlignedBB(pos.getX()-1,pos.getY()+1,pos.getZ()-1,pos.getX()+2,anchor.getY()+3,pos.getZ()+2);
        return world.getEntitiesWithinAABBExcludingEntity(null,moving).stream().noneMatch(e->!e.isDead && e.canBeCollidedWith() && !(e instanceof EntityStrategicMissile));
    }
    @Override public void update(){
        if(world==null || world.isRemote)return;
        if(isSite()){if(buildIndex>=0)buildTick();return;}
        if(constructionLocked){if(rebuildController!=null && !world.isBlockLoaded(rebuildController))return;
            TileEntity controller=rebuildController==null?null:world.getTileEntity(rebuildController);
            if(controller instanceof TileEntityMissileSilo && ((TileEntityMissileSilo)controller).buildIndex>=0)return;
            constructionLocked=false;rebuildController=null;changed();}
        if(targetLoading && (pendingTargetValid==null || !pendingTargetValid.getAsBoolean() || world.getTotalWorldTime()>targetDeadline)){targetLoading=false;pendingTargetValid=null;status="silo.error.target";changed();}
        if(builtStructure && anchor!=null){int mode=phase==0 && !launchReady?0:world.getTotalWorldTime()%10<5?1:2;
            if(mode!=lightMode){lightMode=mode;for(BlockPos lamp:SiloStructure.lights(anchor,facing))if(world.isBlockLoaded(lamp) && world.getBlockState(lamp).getBlock()==WarTechContent.SILO_LIGHT)world.setBlockState(lamp,WarTechContent.SILO_LIGHT.getDefaultState().withProperty(SiloLightBlock.MODE,mode),3);}}
        updateEnergyConnections();charge();
        if(phase==1 && operationId!=null && !com.wartec.wartecmod.port.planning.PlanningControl.live(world,operationId,stepId)){abort("silo.state.cancelled");return;}
        if(cooldown>0){cooldown--;if(cooldown==0){status="silo.state.empty";changed();}}
        if(launchReady){
            String error=!StrategicFeature.isEnabled()?"silo.error.disabled":!missile(items.get(0))?"silo.error.ammo":!hasPower(75_000)?"silo.error.power":hatchPos==null || HbmSiloHatch.get(world,hatchPos)==null || HbmSiloHatch.get(world,hatchPos).isLocked()?"silo.error.hatch":HbmSiloHatch.get(world,hatchPos).getState()!=com.hbm.interfaces.IDoor.DoorState.CLOSED?"silo.error.close_first":"";
            if(!error.isEmpty()){launchReady=false;status=error;changed();}
            else{
                if(world.getTotalWorldTime()%4==0)((WorldServer)world).spawnParticle(EnumParticleTypes.CLOUD,true,pos.getX()+.5,pos.getY()+1.65,pos.getZ()+.5,20,.85,.18,.85,.035);
                if(world.getTotalWorldTime()%80==1){SoundEvent sound=SoundEvent.REGISTRY.getObject(new ResourceLocation("wartecmod","weapon.cruisemissileengine"));if(sound!=null)world.playSound(null,pos,sound,SoundCategory.BLOCKS,2,.55F);}
                if(world.getTotalWorldTime()%40==1)world.playSound(null,pos,SoundEvents.BLOCK_NOTE_BELL,SoundCategory.BLOCKS,1.4F,.65F);
            }
        }
        if(phase==0 && cooldown==0 && isDefenseMode()){if(isDefenseMissile())defenseTick();else status="silo.abm.empty";}
        if(phase==1){
            phaseTicks++;
            if(defenseTarget!=null){Entity contact=((WorldServer)world).getEntityFromUuid(defenseTarget);
                // Entity UUID tables and external radars settle after a loaded-world restart.
                // Keep the pending store intact while those native services recover.
                if(contact==null && phaseTicks<=60)return;
                if(!SiloDefense.eligible(contact) || NetworkTeamHelper.isFriendly(getOwnerTeam(),contact)){abort("silo.abm.lost");return;}
                defenseTargetId=contact.getEntityId();com.wartec.wartecmod.port.network.MissileTrackingService.holdReservation(world,defenseTargetId,defenseKey());}
            if(phaseTicks%30==1)world.playSound(null,anchor,SoundEvents.BLOCK_NOTE_BELL,SoundCategory.BLOCKS,2,.65F);
            Entity admitted=launchId==null?null:((WorldServer)world).getEntityFromUuid(launchId);
            if(admitted instanceof EntityStrategicMissile || admitted instanceof EntitySiloInterceptor){consumePower(defenseTarget!=null?SiloDefense.ENERGY:75_000);items.set(0,ItemStack.EMPTY);phase=2;phaseTicks=0;status="silo.state.ejection";changed();return;}
            if(phaseTicks>240 || !hasPower(defenseTarget!=null?SiloDefense.ENERGY:75_000) || HbmSiloHatch.get(world,hatchPos)==null){abort("silo.error.hatch");return;}
            if(HbmSiloHatch.isOpen(world,hatchPos)){
                if(!shaftClear(true)){abort("silo.error.clearance");return;}
                if(defenseTarget!=null){launchDefense();return;}
                EntityStrategicMissile rocket=new EntityStrategicMissile(world);rocket.configureBoost(system(),target.getX()+.5,target.getY(),target.getZ()+.5,PlanningAccessPolicy.tileOwner(this),getOwnerTeam());
                rocket.configureSiloLaunch(anchor.getY(),launchId);rocket.setUniqueId(launchId);rocket.setLocationAndAngles(pos.getX()+.5,pos.getY()+1.5,pos.getZ()+.5,0,-90);
                PlanningHooks.stamp(rocket,operationId,stepId);
                if(!MissileChunkLoader.spawnFlight(rocket)){abort("flight.error.chunks");return;}
                consumePower(75_000);items.set(0,ItemStack.EMPTY);phase=2;phaseTicks=0;status="silo.state.ejection";
                ((WorldServer)world).spawnParticle(EnumParticleTypes.CLOUD,true,anchor.getX()+.5,anchor.getY()+1,anchor.getZ()+.5,340,2.8,.9,2.8,.22);
                launchSound("weapon.ballisticmissiletakeoff",20,.55F);world.playSound(null,anchor,SoundEvents.ENTITY_GENERIC_EXPLODE,SoundCategory.BLOCKS,18,.4F);changed();
            }
        }else if(phase==2){
            phaseTicks++;
            if(phaseTicks<55 && phaseTicks%2==0)((WorldServer)world).spawnParticle(EnumParticleTypes.CLOUD,true,anchor.getX()+.5,anchor.getY()+1,anchor.getZ()+.5,44,3.4,1.1,3.4,.16);
            if(phaseTicks==32)launchSound("weapon.cruisemissileengine",12,.45F);
            Entity e=((WorldServer)world).getEntityFromUuid(launchId);
            if(e!=null && !e.isDead && e.posY<anchor.getY()+5)return;
            if(e==null && phaseTicks<100)return;
            // No re-admission after restart. A missing shot becomes a spent launch, never another missile.
            HbmSiloHatch.close(world,hatchPos);phase=0;cooldown=160;status="silo.state.cooling";defenseTarget=null;defenseTargetId=-1;operationId=stepId=null;changed();
        }
        if(world.getTotalWorldTime()%40==0)changed();
    }
    private void launchSound(String key,float volume,float pitch){SoundEvent sound=SoundEvent.REGISTRY.getObject(new ResourceLocation("wartecmod",key));if(sound!=null)world.playSound(null,anchor,sound,SoundCategory.BLOCKS,volume,pitch);}
    private void abort(String reason){if(defenseTargetId>=0)com.wartec.wartecmod.port.network.MissileTrackingService.releaseReservation(world,defenseTargetId,defenseKey());defenseTarget=null;defenseTargetId=-1;phase=0;phaseTicks=0;operationId=stepId=null;status=reason;HbmSiloHatch.close(world,hatchPos);changed();}
    private void defenseTick(){
        if(!defenseEnabled){status="silo.abm.off";return;}
        if(!hasPower(SiloDefense.ENERGY)){status="silo.abm.power";return;}
        if(!findHatch()){status="silo.error.hatch";return;}
        if(!shaftClear(false)){status="silo.error.clearance";return;}
        net.minecraft.util.math.Vec3d origin=new net.minecraft.util.math.Vec3d(pos.getX()+.5,anchor.getY()+3,pos.getZ()+.5);
        int delay=HbmSiloHatch.isOpen(world,hatchPos)?35:140;
        Entity selected=SiloDefense.select(world,origin,defenseKey(),getOwnerTeam(),delay);
        if(selected==null){status=com.wartec.wartecmod.port.network.MissileTrackingService.linkedRadarCount(world,origin.x,origin.y,origin.z,getOwnerTeam())>0?"silo.abm.guard":"silo.abm.wait_radar";return;}
        if(!com.wartec.wartecmod.port.network.MissileTrackingService.tryReserve(world,selected.getEntityId(),defenseKey()))return;
        defenseTarget=selected.getUniqueID();defenseTargetId=selected.getEntityId();
        if(!HbmSiloHatch.open(world,hatchPos)){abort("silo.error.hatch");return;}
        launchReady=false;phase=1;phaseTicks=0;launchId=UUID.randomUUID();status="silo.abm.opening";changed();
    }
    private void launchDefense(){
        Entity selected=((WorldServer)world).getEntityFromUuid(defenseTarget);
        net.minecraft.util.math.Vec3d origin=new net.minecraft.util.math.Vec3d(pos.getX()+.5,anchor.getY()+3,pos.getZ()+.5);
        if(!SiloDefense.eligible(selected) || !SiloDefense.reachable(selected,origin,35)){abort("silo.abm.lost");return;}
        if(!com.wartec.wartecmod.port.network.MissileTrackingService.hasDefenseContact(world,selected,origin,getOwnerTeam(),true)){status="silo.abm.wait_radar";return;}
        defenseTargetId=selected.getEntityId();
        if(!com.wartec.wartecmod.port.network.MissileTrackingService.holdReservation(world,defenseTargetId,defenseKey()) && !com.wartec.wartecmod.port.network.MissileTrackingService.tryReserve(world,defenseTargetId,defenseKey())){abort("silo.abm.assigned");return;}
        EntitySiloInterceptor rocket=new EntitySiloInterceptor(world);rocket.configure(selected,anchor.getY(),PlanningAccessPolicy.tileOwner(this),getOwnerTeam(),defenseKey());rocket.setUniqueId(launchId);rocket.setLocationAndAngles(pos.getX()+.5,pos.getY()+1.5,pos.getZ()+.5,0,-90);
        if(!MissileChunkLoader.spawnFlight(rocket)){abort("flight.error.chunks");return;}
        com.wartec.wartecmod.port.network.MissileTrackingService.confirmReservation(world,defenseTargetId,defenseKey(),rocket.getEntityId());PlanningHooks.defenseShot(world,this,rocket,selected);
        consumePower(SiloDefense.ENERGY);items.set(0,ItemStack.EMPTY);phase=2;phaseTicks=0;status="silo.abm.ejection";
        ((WorldServer)world).spawnParticle(EnumParticleTypes.CLOUD,true,anchor.getX()+.5,anchor.getY()+1,anchor.getZ()+.5,400,3,.9,3,.22);launchSound("weapon.ballisticmissiletakeoff",22,.6F);world.playSound(null,anchor,SoundEvents.ENTITY_GENERIC_EXPLODE,SoundCategory.BLOCKS,18,.45F);changed();
    }
    private void charge(){ItemStack stack=items.get(2);if(!stack.isEmpty() && stack.getItem() instanceof IBatteryItem){IBatteryItem b=(IBatteryItem)stack.getItem();long n=Math.min(Math.min(b.getCharge(stack),b.getDischargeRate()),getMaxPower()-getPower());if(n>0){b.dischargeBattery(stack,n);setPower(getPower()+n);}}}
    public void closeHatch(EntityPlayer p){if(mayUse(p) && phase==0 && findHatch() && shaftClear(true))HbmSiloHatch.close(world,hatchPos);}
    @Override public AxisAlignedBB getRenderBoundingBox(){return new AxisAlignedBB(pos).expand(0,40,0).grow(5);}
    @Override public double getMaxRenderDistanceSquared(){return 65536;}
    @Override public int getSizeInventory(){return 3;}
    @Override public boolean isEmpty(){for(ItemStack s:items)if(!s.isEmpty())return false;return true;}
    @Override public ItemStack getStackInSlot(int i){return i>=0 && i<3?items.get(i):ItemStack.EMPTY;}
    @Override public ItemStack decrStackSize(int i,int n){if(constructionLocked || i==0 && payloadLocked())return ItemStack.EMPTY;ItemStack s=ItemStackHelper.getAndSplit(items,i,n);changed();return s;}
    @Override public ItemStack removeStackFromSlot(int i){return decrStackSize(i,64);}
    @Override public void setInventorySlotContents(int i,ItemStack s){if(constructionLocked || i<0 || i>=3 || i==0 && payloadLocked() || !s.isEmpty() && !isItemValidForSlot(i,s))return;items.set(i,s);if(i==0 && !s.isEmpty()){defenseRole=s.getItem()==WarTechContent.SILO_INTERCEPTOR;if(!defenseRole)defenseEnabled=false;}if(!s.isEmpty())s.setCount(1);status=launchReady?"silo.state.launch_ready":missile(items.get(0))?"silo.state.ready":"silo.state.empty";changed();}
    @Override public int getInventoryStackLimit(){return 1;}
    @Override public boolean isUsableByPlayer(EntityPlayer p){return !isInvalid() && mayUse(p) && world.getTileEntity(pos)==this;}
    @Override public void openInventory(EntityPlayer p){}
    @Override public void closeInventory(EntityPlayer p){}
    @Override public boolean isItemValidForSlot(int i,ItemStack s){return storedCore==null && !constructionLocked && (i==0?!payloadLocked() && missile(s):i==1?DesignatorCompat.isDesignator(s):i==2 && !s.isEmpty() && s.getItem() instanceof IBatteryItem);}
    @Override public int getField(int i){return 0;}
    @Override public void setField(int i,int n){}
    @Override public int getFieldCount(){return 0;}
    @Override public void clear(){if(!payloadLocked()){for(int i=0;i<3;i++)items.set(i,ItemStack.EMPTY);changed();}}
    @Override public String getName(){return "tile.SiloStart.name";}
    @Override public boolean hasCustomName(){return false;}
    @Override public NBTTagCompound writeToNBT(NBTTagCompound n){super.writeToNBT(n);ItemStackHelper.saveAllItems(n,items);n.setInteger("Facing",facing.getHorizontalIndex());
        n.setBoolean("DefenseRole",defenseRole);n.setBoolean("DefenseEnabled",defenseEnabled);if(defenseTarget!=null)n.setUniqueId("DefenseTarget",defenseTarget);n.setBoolean("LaunchReady",launchReady);n.setBoolean("BuiltStructure",builtStructure);n.setLong("TargetRevision",targetRevision);n.setBoolean("TargetLoading",targetLoading);n.setInteger("BuildLayout",105);if(storedCore!=null)n.setTag("StoredCore",storedCore);
        n.setInteger("DryIndex",dryIndex);n.setInteger("WarTechBuildCredits",buildCredits);
        n.setBoolean("ConstructionLocked",constructionLocked);if(rebuildController!=null)n.setLong("RebuildController",rebuildController.toLong());
        if(anchor!=null)n.setLong("Anchor",anchor.toLong());if(hatchPos!=null)n.setLong("Hatch",hatchPos.toLong());if(linkedStart!=null)n.setLong("Start",linkedStart.toLong());
        if(target!=null)n.setLong("Target",target.toLong());n.setBoolean("TargetSet",targetSet);n.setInteger("Build",buildIndex);
        n.setInteger("Phase",phase);n.setInteger("Ticks",phaseTicks);n.setInteger("Cooldown",cooldown);n.setString("Status",status);n.setLong("Revision",revision);
        if(launchId!=null)n.setUniqueId("Launch",launchId);if(operationId!=null)n.setUniqueId("Operation",operationId);if(stepId!=null)n.setUniqueId("Step",stepId);return n;}
    @Override public void readFromNBT(NBTTagCompound n){super.readFromNBT(n);ItemStackHelper.loadAllItems(n,items);facing=EnumFacing.getHorizontal(n.getInteger("Facing"));
        defenseRole=n.getBoolean("DefenseRole") || isDefenseMissile();defenseEnabled=n.getBoolean("DefenseEnabled");defenseTarget=n.hasUniqueId("DefenseTarget")?n.getUniqueId("DefenseTarget"):null;defenseTargetId=-1;launchReady=n.getBoolean("LaunchReady") && n.getInteger("Phase")==0 && n.getInteger("Cooldown")==0 && !n.getBoolean("ConstructionLocked");builtStructure=n.getBoolean("BuiltStructure");targetRevision=n.getLong("TargetRevision");targetLoading=world!=null && world.isRemote && n.getBoolean("TargetLoading");storedCore=n.hasKey("StoredCore")?n.getCompoundTag("StoredCore"):null;
        dryIndex=Math.max(0,n.getInteger("DryIndex"));buildCredits=n.hasKey("WarTechBuildCredits",99)?MathHelper.clamp(n.getInteger("WarTechBuildCredits"),0,1):n.getBoolean("BuiltStructure") || n.getInteger("Build")>=0?0:1;
        constructionLocked=n.getBoolean("ConstructionLocked");rebuildController=n.hasKey("RebuildController")?BlockPos.fromLong(n.getLong("RebuildController")):null;
        anchor=n.hasKey("Anchor")?BlockPos.fromLong(n.getLong("Anchor")):null;hatchPos=n.hasKey("Hatch")?BlockPos.fromLong(n.getLong("Hatch")):null;linkedStart=n.hasKey("Start")?BlockPos.fromLong(n.getLong("Start")):null;
        target=n.hasKey("Target")?BlockPos.fromLong(n.getLong("Target")):null;targetSet=n.getBoolean("TargetSet");buildIndex=n.hasKey("Build")?n.getInteger("Build"):-1;
        phase=MathHelper.clamp(n.getInteger("Phase"),0,2);phaseTicks=Math.max(0,n.getInteger("Ticks"));cooldown=Math.max(0,n.getInteger("Cooldown"));status=n.getString("Status");revision=n.getLong("Revision");
        if(buildIndex>=0 && n.getInteger("BuildLayout")<105)buildIndex=0;
        if(!targetLoading && status.equals("silo.state.target_loading"))status="silo.error.target";
        launchId=n.hasUniqueId("Launch")?n.getUniqueId("Launch"):null;operationId=n.hasUniqueId("Operation")?n.getUniqueId("Operation"):null;stepId=n.hasUniqueId("Step")?n.getUniqueId("Step"):null;
    }
}
