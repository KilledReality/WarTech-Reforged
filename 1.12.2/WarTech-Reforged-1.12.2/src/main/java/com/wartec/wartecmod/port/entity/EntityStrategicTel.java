package com.wartec.wartecmod.port.entity;

import com.wartec.wartecmod.WarTechReforged;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.gui.WarTechGuiHandler;
import com.wartec.wartecmod.port.integration.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.*;

/** Animated TEL; ammunition and coordinates stay locked throughout a launch. */
public final class EntityStrategicTel extends EntityWarTechGroundVehicle {
    private static final DataParameter<Integer> ERECTION=EntityDataManager.createKey(EntityStrategicTel.class,DataSerializers.VARINT);
    private static final DataParameter<Integer> LOAD_MASK=EntityDataManager.createKey(EntityStrategicTel.class,DataSerializers.VARINT);
    private static final DataParameter<Integer> LAUNCH_TICKS=EntityDataManager.createKey(EntityStrategicTel.class,DataSerializers.VARINT);
    private static final DataParameter<Integer> LAUNCH_SLOT=EntityDataManager.createKey(EntityStrategicTel.class,DataSerializers.VARINT);
    private static final DataParameter<Integer> TARGET_MASK=EntityDataManager.createKey(EntityStrategicTel.class,DataSerializers.VARINT);
    private static final DataParameter<BlockPos> TARGET_FIRST=EntityDataManager.createKey(EntityStrategicTel.class,DataSerializers.BLOCK_POS);
    private static final DataParameter<BlockPos> TARGET_SECOND=EntityDataManager.createKey(EntityStrategicTel.class,DataSerializers.BLOCK_POS);
    private static final DataParameter<Integer> TARGET_REV_FIRST=EntityDataManager.createKey(EntityStrategicTel.class,DataSerializers.VARINT);
    private static final DataParameter<Integer> TARGET_REV_SECOND=EntityDataManager.createKey(EntityStrategicTel.class,DataSerializers.VARINT);
    private final int[] targetRevision=new int[2];
    private int previousDeployment,previousLaunch;
    public EntityStrategicTel(World world) { this(world,StrategicSystemProfile.TOPOL_M); }
    public EntityStrategicTel(World world,StrategicSystemProfile system) {
        super(world,system.getVehicleProfile());setProfile(system.getVehicleProfile());
        setVisual("deployment/strategic_"+system.getId(),system.ordinal());
    }
    @Override protected void entityInit() {
        super.entityInit();dataManager.register(ERECTION,0);dataManager.register(LOAD_MASK,0);
        dataManager.register(LAUNCH_TICKS,0);dataManager.register(LAUNCH_SLOT,0);
        dataManager.register(TARGET_MASK,0);dataManager.register(TARGET_FIRST,BlockPos.ORIGIN);dataManager.register(TARGET_SECOND,BlockPos.ORIGIN);
        dataManager.register(TARGET_REV_FIRST,0);dataManager.register(TARGET_REV_SECOND,0);
    }
    public StrategicSystemProfile getStrategicSystem() { return StrategicSystemProfile.fromVehicleProfile(getProfile()); }
    public int getErectionProgress() { return dataManager.get(ERECTION)*100/getStrategicSystem().getDeploymentTicks(); }
    public float getAnimatedDeployment(float partial) {
        return (previousDeployment+(dataManager.get(ERECTION)-previousDeployment)*partial)/getStrategicSystem().getDeploymentTicks();
    }
    public float getAnimatedLaunch(float partial) {
        int current=getLaunchTicks();return current==0?0:previousLaunch>0?previousLaunch+(current-previousLaunch)*partial:current;
    }
    public int getMissileMask() { return dataManager.get(LOAD_MASK); }
    public int getVisualMissileMask() {
        int mask=getMissileMask(),sequence=getLaunchTicks(),slot=getLaunchSlot();
        if(!world.isRemote || sequence==0) return mask;
        for(EntityWarTechBase entity:world.getEntitiesWithinAABB(EntityWarTechBase.class,getRenderBoundingBox()))
            if(entity instanceof TelLaunchOrigin.Rocket && !entity.isDead) {
                TelLaunchOrigin origin=((TelLaunchOrigin.Rocket)entity).getTelLaunchOrigin();
                if(origin.launcherId==getEntityId() && origin.slot==slot) return mask&~(1<<slot);
            }
        // Metadata and entity spawn use separate packets. Hold the last attached
        // frame briefly rather than showing a hole while the spawn is in transit.
        return sequence>=getStrategicSystem().getSeparationTick() && sequence<=getStrategicSystem().getSeparationTick()+6?mask|1<<slot:mask;
    }
    public boolean isMissileLoaded() { return getMissileMask()!=0; }
    public int getLaunchTicks() { return dataManager.get(LAUNCH_TICKS); }
    public int getLaunchSlot() { return dataManager.get(LAUNCH_SLOT); }
    public int getTargetCount() { return getStrategicSystem().isTactical()?2:1; }
    public boolean hasTarget(int slot) { return slot>=0 && slot<getTargetCount() && (dataManager.get(TARGET_MASK)&1<<slot)!=0; }
    public BlockPos getTarget(int slot) { return dataManager.get(slot==1?TARGET_SECOND:TARGET_FIRST); }
    public int getTargetRevision(int slot) { return dataManager.get(slot==1?TARGET_REV_SECOND:TARGET_REV_FIRST); }
    private void setTarget(int slot,BlockPos point) {
        dataManager.set(slot==1?TARGET_SECOND:TARGET_FIRST,point);
        dataManager.set(TARGET_MASK,dataManager.get(TARGET_MASK)|1<<slot);targetRevision[slot]++;
        dataManager.set(slot==1?TARGET_REV_SECOND:TARGET_REV_FIRST,getTargetRevision(slot)+1);
        if(slot==0) super.setGuidanceTarget(point.getX()+.5,point.getY()+.5,point.getZ()+.5);
    }
    /** Legacy callers assigned one shared target; retain that meaning for old integrations/saves. */
    @Override public void setGuidanceTarget(double x,double y,double z) {
        super.setGuidanceTarget(x,y,z);
        for(int slot=0;slot<getTargetCount();slot++) setTarget(slot,new BlockPos(x,y,z));
    }
    @Override public void clearGuidanceTarget() {
        super.clearGuidanceTarget();dataManager.set(TARGET_MASK,0);
        for(int slot=0;slot<2;slot++) targetRevision[slot]++;
    }
    public void setMissileLoaded(boolean loaded) {
        if(getStrategicSystem().isTactical()) return;
        setInventorySlotContents(0,loaded?new ItemStack(WarTechContent.STRATEGIC_MISSILE,1,getStrategicSystem().ordinal()):ItemStack.EMPTY);
    }
    @Override public void markDirty() {
        super.markDirty();int mask=0;
        for(int i=0;i<(getStrategicSystem().isTactical()?2:1);i++) if(isCompatibleMissile(getStackInSlot(i))) mask|=1<<i;
        dataManager.set(LOAD_MASK,mask);
    }
    public boolean isCompatibleMissile(ItemStack stack) {
        if(stack==null || stack.isEmpty()) return false;
        return getStrategicSystem().isTactical()?stack.getItem()==WarTechContent.ITEM_ISKANDER_MISSILE:
            stack.getItem()==WarTechContent.STRATEGIC_MISSILE && ((StrategicMissileItem)stack.getItem()).getSystem(stack)==getStrategicSystem();
    }
    @Override public boolean isItemValidForSlot(int slot,ItemStack stack) {
        return slot>=0 && slot<(getStrategicSystem().isTactical()?2:1) && !isDeployed() && getLaunchTicks()==0 && dataManager.get(ERECTION)==0 && isCompatibleMissile(stack);
    }
    public boolean canOperate(EntityPlayer player) {
        return player!=null && !player.isSpectator() && !player.isDead && !isDead && getStrategicSystem().isAvailable() && player.world==world
            && player.getDistanceSq(this)<=144 && (player.capabilities.isCreativeMode
                || getOwnerUuid()==null && getOwnerTeam().isEmpty() || isFriendlyOrOwner(player));
    }
    @Override public boolean isUsableByPlayer(EntityPlayer player) { return canOperate(player); }
    @Override public void onUpdate() {
        if(!getStrategicSystem().isAvailable()) { if(!world.isRemote && isBeingRidden()) removePassengers();return; }
        previousDeployment=dataManager.get(ERECTION);previousLaunch=getLaunchTicks();super.onUpdate();
    }
    @Override public boolean isInvisible() { return !getStrategicSystem().isAvailable() || super.isInvisible(); }
    @Override public boolean canBeCollidedWith() { return getStrategicSystem().isAvailable() && super.canBeCollidedWith(); }
    @Override public boolean attackEntityFrom(DamageSource source,float amount) { return getStrategicSystem().isAvailable() && super.attackEntityFrom(source,amount); }
    @Override protected void serverTick(WarTechEntityProfile profile) {
        super.serverTick(profile);
        int progress=dataManager.get(ERECTION),target=isDeployed()?getStrategicSystem().getDeploymentTicks():0;
        if(progress!=target) dataManager.set(ERECTION,progress+(progress<target?1:-1));
        if(getLaunchTicks()>0 && !com.wartec.wartecmod.port.planning.PlanningControl.stampedLive(this) && isCompatibleMissile(getStackInSlot(getLaunchSlot())))revokePlanning(getEntityData().getUniqueId("PlanningOperation"));
        int sequence=getLaunchTicks();
        if(sequence>0) {
            if(world instanceof WorldServer) holdLaunchChunks();
            tickLaunchSequence(sequence);
            if(getLaunchTicks()>0) dataManager.set(LAUNCH_TICKS,sequence>=getStrategicSystem().getSequenceTicks()?0:sequence+1);
        }
    }
    private boolean holdLaunchChunks() {
        // ENTITY tickets survive restarts and reload this pending launcher by UUID.
        return MissileChunkLoader.prepare(this);
    }
    public Vec3d getLaunchPosition(int slot) {
        return getLaunchPosition(slot,getStrategicSystem().getSeparationTick());
    }
    public Vec3d getLaunchPosition(int slot,float sequence) {
        Vec3d local=getStrategicSystem().launchOffset(slot,sequence);
        double angle=Math.toRadians(180-rotationYaw),c=Math.cos(angle),s=Math.sin(angle);
        return new Vec3d(posX+local.x*c+local.z*s,posY+local.y,posZ-local.x*s+local.z*c);
    }
    public Vec3d getEjectionCloudPosition() {
        return getLaunchPosition(getLaunchSlot(),getStrategicSystem().getEjectionTick()).addVector(0,getStrategicSystem().getModelScale()*1.80,0);
    }
    private boolean shaftClear(int slot) {
        Vec3d point=getLaunchPosition(slot);
        if(point.y+getStrategicSystem().getModelScale()*1.8>252) return false;
        for(int y=(int)posY+4;y<Math.min(255,(int)point.y+18);y++)
            for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++)
                if(!world.isAirBlock(new BlockPos(point.x+x,y,point.z+z))) return false;
        return true;
    }
    private void tickLaunchSequence(int sequence) {
        StrategicSystemProfile system=getStrategicSystem();int slot=getLaunchSlot();
        if(sequence==1) world.playSound(null,posX,posY,posZ,SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN,SoundCategory.BLOCKS,4,.65F);
        if(!system.isTactical() && sequence==system.getEjectionTick()) {
            Vec3d nozzle=getEjectionCloudPosition();
            world.playSound(null,nozzle.x,nozzle.y,nozzle.z,SoundEvents.BLOCK_PISTON_EXTEND,SoundCategory.BLOCKS,8,.55F);
            if(world instanceof WorldServer) ((WorldServer)world).spawnParticle(EnumParticleTypes.CLOUD,true,nozzle.x,nozzle.y,nozzle.z,160,1.5,.8,1.5,.30);
        }
        if(sequence==system.getIgnitionTick()) playLegacySound("wartecmod:weapon.ballisticmissiletakeoff",SoundEvents.ENTITY_GENERIC_EXPLODE,system.isTactical()?14:24,.65F);
        if(sequence!=system.getSeparationTick()) return;
        if(!isCompatibleMissile(getStackInSlot(slot)) || !shaftClear(slot)) { dataManager.set(LAUNCH_TICKS,0);return; }
        EntityWarTechBase missile;Vec3d start=getLaunchPosition(slot);BlockPos target=getTarget(slot);
        if(system.isTactical()) {
            EntityWarTechMissile tactical=LegacyEntityFactory.missile(world,MissileProfile.ISKANDER);missile=tactical;
            missile.setPosition(start.x,start.y,start.z);
            tactical.configureTelLaunch(target.getX(),target.getY(),target.getZ());
        } else {
            EntityStrategicMissile strategic=new EntityStrategicMissile(world);missile=strategic;
            missile.setPosition(start.x,start.y,start.z);
            strategic.configureBoost(system,target.getX()+.5,target.getY()+.5,target.getZ()+.5,getOwnerUuid(),getOwnerTeam());
        }
        missile.setOwnerIdentity(getOwnerUuid(),getOwnerTeam());missile.setArmed(true);missile.rotationYaw=rotationYaw;
        ((TelLaunchOrigin.Rocket)missile).getTelLaunchOrigin().configure(this,slot);
        missile.rotationYaw=system.isTactical()?180-rotationYaw:rotationYaw-180;
        missile.rotationPitch=system.isTactical()?0:-90;missile.prevRotationYaw=missile.rotationYaw;missile.prevRotationPitch=missile.rotationPitch;
        missile.prevPosX=missile.lastTickPosX=start.x;missile.prevPosY=missile.lastTickPosY=start.y;missile.prevPosZ=missile.lastTickPosZ=start.z;
        missile.motionY=system.separationSpeed();
        com.wartec.wartecmod.port.planning.PlanningHooks.stamp(this,missile);
        if(MissileChunkLoader.spawnFlight(missile)) {
            com.wartec.wartecmod.port.planning.PlanningHooks.emitted(missile);
            decrStackSize(slot,1);markDirty();
        } else dataManager.set(LAUNCH_TICKS,0);
    }
    public void revokePlanning(java.util.UUID id){if(com.wartec.wartecmod.port.planning.PlanningControl.matches(this,id) && isCompatibleMissile(getStackInSlot(getLaunchSlot()))){dataManager.set(LAUNCH_TICKS,0);com.wartec.wartecmod.port.planning.PlanningFlightControl.clear(this);}}
    public boolean planningOrder(com.wartec.wartecmod.port.planning.PlanningCommand c,com.wartec.wartecmod.port.planning.AttackOperation.Action action,BlockPos point,int slot) {
        if(world.isRemote || !c.owns(this) || getLaunchTicks()>0)return false;
        if(action==com.wartec.wartecmod.port.planning.AttackOperation.Action.PREPARE){if(!hasGroundSupport() || Math.hypot(motionX,motionZ)>.01 || isBeingRidden())return false;setDeployed(true);return true;}
        if(action!=com.wartec.wartecmod.port.planning.AttackOperation.Action.LAUNCH || !isDeployed() || getErectionProgress()<100)return false;
        if(slot<0){slot=0;while(slot<getTargetCount() && !isCompatibleMissile(getStackInSlot(slot)))slot++;}
        double distance=Math.hypot(point.getX()+.5-posX,point.getZ()+.5-posZ);
        if(slot>=getTargetCount() || !isCompatibleMissile(getStackInSlot(slot)) || distance<getStrategicSystem().getMinimumRange() || distance>getStrategicSystem().getMaximumRange() || !shaftClear(slot) || !world.getWorldBorder().contains(point))return false;
        setTarget(slot,point);dataManager.set(LAUNCH_SLOT,slot);dataManager.set(LAUNCH_TICKS,1);
        if(!holdLaunchChunks()){dataManager.set(LAUNCH_TICKS,0);return false;}
        com.wartec.wartecmod.port.planning.PlanningHooks.command(this,c);return true;
    }
    public boolean beginLaunch(EntityPlayer player) { return beginLaunch(player,0); }
    public boolean beginLaunch(EntityPlayer player,int slot) {
        return beginLaunch(player,slot,false);
    }
    public boolean canCommand(EntityPlayer player) {
        return player!=null && !player.isSpectator() && !player.isDead && !isDead && player.world==world && getStrategicSystem().isAvailable()
            && (player.capabilities.isCreativeMode || getOwnerUuid()==null && getOwnerTeam().isEmpty() || isFriendlyOrOwner(player));
    }
    public boolean beginLaunchFromDetonator(EntityPlayer player) {
        return !getStrategicSystem().isTactical() && beginLaunch(player,0,true);
    }
    private boolean beginLaunch(EntityPlayer player,int slot,boolean remote) {
        if(world.isRemote || !(remote?canCommand(player):canOperate(player)) || getLaunchTicks()>0) return false;
        if(!isDeployed() || getErectionProgress()<100) { tell(player,"deploy_first");return false; }
        if(slot<0 || slot>(getStrategicSystem().isTactical()?1:0) || !isCompatibleMissile(getStackInSlot(slot))) { tell(player,"empty");return false; }
        if(!hasTarget(slot)) { tell(player,"no_target");return false; }
        BlockPos target=getTarget(slot);
        double distance=Math.sqrt((target.getX()+.5-posX)*(target.getX()+.5-posX)+(target.getZ()+.5-posZ)*(target.getZ()+.5-posZ));
        if(distance<getStrategicSystem().getMinimumRange() || distance>getStrategicSystem().getMaximumRange()) { tell(player,"range");return false; }
        if(!shaftClear(slot)) { tell(player,"obstructed");return false; }
        com.wartec.wartecmod.port.planning.PlanningFlightControl.clear(this);dataManager.set(LAUNCH_SLOT,slot);dataManager.set(LAUNCH_TICKS,1);
        if(!(world instanceof WorldServer) || !holdLaunchChunks()) { dataManager.set(LAUNCH_TICKS,0);tell(player,"busy");return false; }
        return true;
    }
    public boolean programTarget(EntityPlayer player,int x,int y,int z) {
        return programTarget(player,0,x,y,z);
    }
    public boolean programTarget(EntityPlayer player,int slot,int x,int y,int z) {
        if(world.isRemote || slot<0 || slot>=getTargetCount() || !canOperate(player) || getLaunchTicks()!=0 || Math.abs((long)x)>29999984 || Math.abs((long)z)>29999984 || y<0 || y>255
            || !world.getWorldBorder().contains(new BlockPos(x,y,z))) return false;
        setTarget(slot,new BlockPos(x,y,z));player.sendMessage(new TextComponentTranslation("wartec.tel.target_slot_set",slot+1));return true;
    }
    public boolean importTarget(EntityPlayer player,int slot,com.wartec.wartecmod.port.gui.ContainerLegacyEntity container) {
        if(!(world instanceof WorldServer) || slot<0 || slot>=getTargetCount() || !canOperate(player) || getLaunchTicks()!=0
            || player.openContainer!=container || container.getTarget()!=this) return false;
        ItemStack stack=container.getDesignator(),snapshot=stack.copy();int revision=++targetRevision[slot];
        boolean queued=DesignatorCompat.resolveSavedTarget((WorldServer)world,snapshot.copy(),Double.NaN,
            ()->canOperate(player) && getLaunchTicks()==0 && player.openContainer==container && targetRevision[slot]==revision
                && container.getDesignator()==stack && ItemStack.areItemStacksEqual(stack,snapshot),
            point->programTarget(player,slot,MathHelper.floor(point.x),MathHelper.floor(point.y),MathHelper.floor(point.z)));
        if(!queued) tell(player,"designator_missing");return queued;
    }
    @Override public AxisAlignedBB getRenderBoundingBox() {
        return getEntityBoundingBox().grow(14,30,14);
    }
    @Override public boolean handleLegacyGuiAction(int action,EntityPlayer player) {
        if(!canOperate(player)) return false;
        if(action==0 || action==2) return beginLaunch(player,action==2?1:0);
        if(action==1 && getLaunchTicks()==0) {
            if(!isDeployed() && (!hasGroundSupport() || Math.abs(motionX)+Math.abs(motionZ)>.05)) { tell(player,"stop_first");return false; }
            setDeployed(!isDeployed());motionX=motionZ=0;removePassengers();return true;
        }
        if(action==3 && !isDeployed() && dataManager.get(ERECTION)==0 && !isBeingRidden()) { player.closeScreen();return player.startRiding(this,true); }
        return false;
    }
    public boolean hasGroundSupport() {
        if(world==null || isInWater() || isInLava() || motionY>.08 || motionY<-.3) return false;
        AxisAlignedBB box=getEntityBoundingBox();
        AxisAlignedBB feet=new AxisAlignedBB(box.minX+.05,box.minY-.125,box.minZ+.05,box.maxX-.05,box.minY+.001,box.maxZ-.05);
        // onGround becomes false on zero-motion ticks. Test solid collision
        // beneath the tyres instead; slabs and other partial blocks count too.
        return !world.getCollisionBoxes(null,feet).isEmpty();
    }
    @Override public boolean processInitialInteract(EntityPlayer player,EnumHand hand) {
        if(hand!=EnumHand.MAIN_HAND) return true;if(world.isRemote) return true;
        if(!canOperate(player)) { tell(player,"denied");return true; }
        ItemStack held=player.getHeldItem(hand);
        if(UavChainDetonator.isDetonator(held)) { TelChainDetonator.bind(player,held,this);return true; }
        if(getLaunchTicks()==0 && trySalvage(player,held,true)) return true;
        if(isCompatibleMissile(held)) {
            if(isDeployed() || dataManager.get(ERECTION)>0 || getLaunchTicks()>0) { tell(player,"stow_first");return true; }
            for(int slot=0;slot<(getStrategicSystem().isTactical()?2:1);slot++) if(getStackInSlot(slot).isEmpty()) {
                ItemStack loaded=held.copy();loaded.setCount(1);setInventorySlotContents(slot,loaded);
                if(!player.capabilities.isCreativeMode) held.shrink(1);tell(player,"loaded");return true;
            }
            tell(player,"full");return true;
        }
        if(DesignatorCompat.isDesignator(held)) {
            int slot=getTargetCount()==2 && hasTarget(0) && !hasTarget(1)?1:0;
            int revision=++targetRevision[slot];
            if(getLaunchTicks()==0 && !DesignatorCompat.resolveForEntity(this,player,hand,
                    p->{ if(canOperate(player) && getLaunchTicks()==0 && targetRevision[slot]==revision) programTarget(player,slot,p.getX(),p.getY(),p.getZ()); })) tell(player,"no_target");
            return true;
        }
        if(player.isSneaking()) { handleLegacyGuiAction(1,player);return true; }
        player.openGui(WarTechReforged.instance,WarTechGuiHandler.GUI_STRATEGIC_TEL,world,getEntityId(),0,0);return true;
    }
    private static void tell(EntityPlayer player,String key) { if(player!=null) player.sendMessage(new TextComponentTranslation("wartec.tel."+key)); }
    @Override protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);tag.setInteger("TelDeploymentTicks",dataManager.get(ERECTION));
        tag.setInteger("StrategicLaunchTicks",getLaunchTicks());tag.setInteger("TelLaunchSlot",getLaunchSlot());tag.setBoolean("TelInventory",true);
        tag.setInteger("TelTargetMask",dataManager.get(TARGET_MASK));
        tag.setLong("TelTargetFirst",getTarget(0).toLong());tag.setLong("TelTargetSecond",getTarget(1).toLong());
    }
    @Override protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        dataManager.set(ERECTION,MathHelper.clamp(tag.hasKey("TelDeploymentTicks")?tag.getInteger("TelDeploymentTicks"):
            tag.getInteger("StrategicErection")*getStrategicSystem().getDeploymentTicks()/100,0,getStrategicSystem().getDeploymentTicks()));
        dataManager.set(LAUNCH_TICKS,MathHelper.clamp(tag.getInteger("StrategicLaunchTicks"),0,getStrategicSystem().getSequenceTicks()));
        dataManager.set(LAUNCH_SLOT,MathHelper.clamp(tag.getInteger("TelLaunchSlot"),0,1));
        if(tag.hasKey("TelTargetMask",99)) {
            dataManager.set(TARGET_FIRST,BlockPos.fromLong(tag.getLong("TelTargetFirst")));
            dataManager.set(TARGET_SECOND,BlockPos.fromLong(tag.getLong("TelTargetSecond")));
            dataManager.set(TARGET_MASK,tag.getInteger("TelTargetMask")&((1<<getTargetCount())-1));
        }
        if(!tag.getBoolean("TelInventory") && tag.getBoolean("StrategicLoaded") && !getStrategicSystem().isTactical()) setMissileLoaded(true);
        markDirty();
    }
}
