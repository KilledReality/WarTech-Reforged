package com.wartec.wartecmod.port.entity;

import api.hbm.energy.IBatteryItem;
import com.wartec.wartecmod.WarTechReforged;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.integration.*;
import com.wartec.wartecmod.port.planning.*;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.*;

/** Two gun systems. Commands reserve one shot; only a successful physical spawn consumes it. */
public final class EntityTubeArtillery extends EntityWarTechGroundVehicle {
    private static final DataParameter<NBTTagCompound> STATE=EntityDataManager.createKey(EntityTubeArtillery.class,DataSerializers.COMPOUND_TAG);
    private final NonNullList<ItemStack> stores=NonNullList.withSize(6,ItemStack.EMPTY);
    private int deployment,reload,shotAge=1000,selected,pendingSlot=-1;
    private float yaw,pitch,oldYaw,oldPitch;
    private BlockPos target;
    private boolean pending;
    private long deadline;
    private UUID operation,step;
    private String failure="";
    @Override public NBTTagCompound productionChassis(){NBTTagCompound n=super.productionChassis();n.setInteger("TubeReload",reload);n.setInteger("TubeShotAge",shotAge);n.setInteger("TubeSelected",selected);if(target!=null)n.setLong("TubeTarget",target.toLong());return n;}
    @Override public void restoreProductionChassis(NBTTagCompound n){super.restoreProductionChassis(n);reload=MathHelper.clamp(n.getInteger("TubeReload"),0,tubeProfile().reloadTicks);shotAge=MathHelper.clamp(n.getInteger("TubeShotAge"),0,1000);selected=MathHelper.clamp(n.getInteger("TubeSelected"),0,3);target=n.hasKey("TubeTarget")?BlockPos.fromLong(n.getLong("TubeTarget")):null;sync();}
    @Override public boolean trySalvage(EntityPlayer player,ItemStack held,boolean grounded){
        if(pending || shotAge<32 || Math.abs(getDriveSpeed())>.01)return false;
        return super.trySalvage(player,held,grounded && grounded());
    }
    public EntityTubeArtillery(World w){super(w,WarTechEntityProfile.ARTILLERY_D30);setLegacyPower(0);}
    public EntityTubeArtillery(World w,TubeArtilleryProfile p){this(w);setProfile(p.entity);}
    @Override protected void entityInit(){super.entityInit();dataManager.register(STATE,new NBTTagCompound());}
    public TubeArtilleryProfile tubeProfile(){TubeArtilleryProfile p=TubeArtilleryProfile.of(getProfile());return p==null?TubeArtilleryProfile.D30:p;}
    private NBTTagCompound state(){return dataManager.get(STATE);}
    public float gunYaw(float partial){return world.isRemote?AirDefenseTraverse.interpolate(oldYaw,yaw,partial):yaw;}
    public float gunPitch(float partial){return world.isRemote?oldPitch+(pitch-oldPitch)*partial:pitch;}
    public float shotSeconds(float partial){return (world.isRemote?shotAge:state().getInteger("ShotAge"))+partial<33?((world.isRemote?shotAge:state().getInteger("ShotAge"))+partial)/20F:-1;}
    public int deploymentTicks(){return state().getInteger("Deployment");}
    public int reloadTicks(){return state().getInteger("Reload");}
    public int selectedSlot(){return state().getInteger("Selected");}
    public boolean isFiring(){return state().getBoolean("Pending");}
    public BlockPos target(){return state().hasKey("Target")?BlockPos.fromLong(state().getLong("Target")):null;}
    public int ammunitionCount(){return state().getInteger("Ammo");}
    public boolean grounded(){AxisAlignedBB b=getEntityBoundingBox();return !isInWater() && !isInLava() && !world.getCollisionBoxes(null,new AxisAlignedBB(b.minX+.05,b.minY-.15,b.minZ+.05,b.maxX-.05,b.minY+.002,b.maxZ-.05)).isEmpty();}
    @Override public void onUpdate(){
        if(world.isRemote){oldYaw=yaw;oldPitch=pitch;yaw=state().getFloat("Yaw");pitch=state().getFloat("Pitch");shotAge=state().getInteger("ShotAge");if(ticksExisted<2){oldYaw=yaw;oldPitch=pitch;}}
        super.onUpdate();
    }
    @Override public boolean canDriveFromPanel(){return tubeProfile()==TubeArtilleryProfile.CAESAR && deploymentTicks()==0 && !isFiring() && super.canDriveFromPanel();}
    @Override public int getBatterySlot(){return 4;}
    @Override public int getEnergyCapacity(){return 100000;}
    @Override public int getMissionRange(){return tubeProfile().range;}
    @Override public boolean isUsableByPlayer(EntityPlayer p){return canManage(p) && getDistanceSq(p)<=196;}
    @Override public AxisAlignedBB getRenderBoundingBox(){return getEntityBoundingBox().grow(9,9,9);}
    @Override protected void serverTick(WarTechEntityProfile p){
        super.serverTick(p);if(reload>0)reload--;shotAge=Math.min(1000,shotAge+1);
        ItemStack stack=stores.get(4);if(stack.getItem() instanceof IBatteryItem){IBatteryItem b=(IBatteryItem)stack.getItem();long amount=Math.min(Math.min(b.getCharge(stack),b.getDischargeRate()),getEnergyCapacity()-getLegacyPower());if(amount>0){b.dischargeBattery(stack,amount);setLegacyPower(getLegacyPower()+(int)amount);}}
        int desired=isDeployed()?tubeProfile().prepareTicks:0;
        if(!isDeployed()){
            yaw=approach(yaw,0,2,true);pitch=approach(pitch,0,1,false);
            if(Math.abs(yaw)<.01 && pitch<.01)deployment=Math.max(0,deployment-1);
        }else deployment=Math.min(desired,deployment+1);
        if(pending && (world.getTotalWorldTime()>deadline || operation!=null && !PlanningControl.live(world,operation,step)))cancel();
        if(pending && world instanceof WorldServer && ticksExisted%20==1)hold();
        if(pending && readyBase())tickShot();
        sync();
    }
    private static float approach(float a,float b,float speed,boolean wrap){float d=wrap?MathHelper.wrapDegrees(b-a):b-a;float n=a+MathHelper.clamp(d,-speed,speed);return wrap?MathHelper.wrapDegrees(n):n;}
    private boolean readyBase(){return isDeployed() && deployment>=tubeProfile().prepareTicks && grounded() && !isBeingRidden() && Math.abs(getDriveSpeed())<.01;}
    public String readiness(){
        String reason=state().getString("Failure");if(!reason.isEmpty())return reason;
        if(isFiring())return "firing";if(!grounded() || Math.abs(getDriveSpeed())>.01)return "stop";
        if(!isDeployed())return "stowed";if(deploymentTicks()<tubeProfile().prepareTicks)return "preparing";
        if(reloadTicks()>0)return "reload";if(getLegacyPower()<500)return "power";
        if(ammunitionCount()==0 || state().getInteger("SelectedCount")==0)return "empty";if(target()==null)return "target";return "ready";
    }
    private void tickShot(){
        if(target==null || !validTarget(target) || pendingSlot<0 || pendingSlot>=4 || !ammo(stores.get(pendingSlot))){cancel();return;}
        Vec3d aim=new Vec3d(target).addVector(.5,.25,.5);
        // Iteration includes the moving muzzle, native bore tilt, and chassis heading.
        float wantedYaw=yaw,wantedPitch=pitch;Vec3d velocity=null;
        for(int i=0;i<6;i++){
            Vec3d[] socket=TubeArtilleryGeometry.muzzle(this,wantedYaw,wantedPitch);
            velocity=TubeArtilleryBallistics.solve(socket[0],aim,tubeProfile().speed);if(velocity==null){cancel();failure="trajectory";return;}
            float actualYaw=(float)Math.toDegrees(Math.atan2(-socket[1].x,socket[1].z));
            float desiredYaw=(float)Math.toDegrees(Math.atan2(-velocity.x,velocity.z));
            wantedYaw=MathHelper.wrapDegrees(wantedYaw+MathHelper.wrapDegrees(desiredYaw-actualYaw));
            wantedPitch+=Math.toDegrees(Math.atan2(velocity.y,Math.hypot(velocity.x,velocity.z)))-Math.toDegrees(Math.asin(socket[1].y));
        }
        if(wantedPitch<0 || wantedPitch>70){cancel();failure="trajectory";return;}
        yaw=approach(yaw,wantedYaw,tubeProfile()==TubeArtilleryProfile.D30?1.2F:2.0F,true);
        pitch=approach(pitch,wantedPitch,.85F,false);
        if(reload>0 || getLegacyPower()<500 || Math.abs(MathHelper.wrapDegrees(wantedYaw-yaw))>.12 || Math.abs(wantedPitch-pitch)>.12)return;
        Vec3d[] socket=TubeArtilleryGeometry.muzzle(this,yaw,pitch);
        velocity=TubeArtilleryBallistics.solve(socket[0],aim,tubeProfile().speed);if(velocity==null){cancel();return;}
        // A blocked muzzle never fires through terrain or consumes ammunition.
        if(world.rayTraceBlocks(socket[0],socket[0].add(socket[1].scale(1.2)),false,true,false)!=null){failure="muzzle";return;}
        failure="";
        EntityWarTechArtilleryProjectile shell=new EntityWarTechArtilleryProjectile(world);
        shell.configure(EntityWarTechArtilleryProjectile.KIND_GREG,stores.get(pendingSlot),aim.x,aim.y,aim.z);
        shell.configureTubeArtillery(tubeProfile());shell.setWhistle(true);
        shell.setOwnerIdentity(getOwnerUuid(),getOwnerTeam());shell.setPosition(socket[0].x,socket[0].y,socket[0].z);
        shell.motionX=velocity.x;shell.motionY=velocity.y;shell.motionZ=velocity.z;
        shell.rotationYaw=(float)Math.toDegrees(Math.atan2(-velocity.x,velocity.z));shell.rotationPitch=(float)-Math.toDegrees(Math.atan2(velocity.y,Math.hypot(velocity.x,velocity.z)));
        shell.prevRotationYaw=shell.rotationYaw;shell.prevRotationPitch=shell.rotationPitch;
        if(operation!=null)PlanningHooks.stamp(shell,operation,step);
        if(!MissileChunkLoader.spawnFlight(shell)){failure="busy";return;}
        stores.get(pendingSlot).shrink(1);setLegacyPower(getLegacyPower()-500);reload=tubeProfile().reloadTicks;shotAge=0;
        world.playSound(null,posX,posY,posZ,net.minecraft.util.SoundEvent.REGISTRY.getObject(new ResourceLocation("hbm:turret.jeremy_fire")),SoundCategory.BLOCKS,8F,tubeProfile()==TubeArtilleryProfile.D30?1.05F:.82F);
        if(world instanceof WorldServer){WorldServer w=(WorldServer)world;w.spawnParticle(EnumParticleTypes.SMOKE_LARGE,socket[0].x,socket[0].y,socket[0].z,22,.16,.16,.16,.08);w.spawnParticle(EnumParticleTypes.FLAME,socket[0].x,socket[0].y,socket[0].z,6,.08,.08,.08,.04);}
        cancel();sync();
    }
    private boolean validTarget(BlockPos point){return point!=null && point.getY()>=0 && point.getY()<world.getHeight() && world.getWorldBorder().contains(point) && tubeProfile().accepts(Math.hypot(point.getX()+.5-posX,point.getZ()+.5-posZ));}
    public boolean programTarget(EntityPlayer player,BlockPos point){
        if(world.isRemote || !isUsableByPlayer(player) || pending)return false;
        if(!validTarget(point)){tell(player,"range",tubeProfile().minimumRange,tubeProfile().range);return false;}failure="";target=point;sync();return true;
    }
    public boolean importTarget(EntityPlayer player){
        if(!isUsableByPlayer(player) || pending || !(world instanceof WorldServer))return false;
        ItemStack snapshot=stores.get(5).copy();return DesignatorCompat.resolveSavedTarget((WorldServer)world,snapshot,Double.NaN,
            ()->isUsableByPlayer(player) && !pending && ItemStack.areItemStacksEqual(snapshot,stores.get(5)),
            point->programTarget(player,new BlockPos(point)));
    }
    private boolean reserve(int slot){
        if(pending || !readyBase() || reload>0 || getLegacyPower()<500 || !validTarget(target))return false;
        if(slot<0)slot=selected;
        if(slot<0 || slot>=4 || !ammo(stores.get(slot)))return false;
        pendingSlot=slot;pending=true;failure="";deadline=world.getTotalWorldTime()+1200;
        if(!hold()){cancel();return false;}sync();return true;
    }
    private boolean hold(){return world instanceof WorldServer && OperationalChunks.requestUntil((WorldServer)world,getPosition(),1,"tube-arty:"+getUniqueID(),()->!isDead && pending && world.getTotalWorldTime()<=deadline,()->!pending);}
    private void cancel(){pending=false;pendingSlot=-1;operation=step=null;failure="";}
    @Override public boolean acceptArtilleryDesignatorTarget(EntityPlayer player,BlockPos point){
        if(world.isRemote || !canManage(player)){tell(player,"denied");return false;}
        if(!isDeployed()){tell(player,"deploy");return false;}
        if(pending || reload>0)return false;
        if(!validTarget(point)){tell(player,"range",tubeProfile().minimumRange,tubeProfile().range);return false;}
        target=point;boolean result=reserve(-1);if(!result)tell(player,readiness());return result;
    }
    @Override public boolean handleLegacyGuiAction(int action,EntityPlayer player){
        if(!isUsableByPlayer(player) || world.isRemote)return false;
        if(action==0){boolean ok=reserve(-1);if(!ok)tell(player,readiness());return ok;}
        if(action==1 && !pending && shotAge>=32){
            if(isBeingRidden() || Math.abs(getDriveSpeed())>.01 || !grounded())return false;
            setDeployed(!isDeployed());sync();return true;
        }
        if(action==2){cancel();sync();return true;}
        if(action==3 && canDriveFromPanel()){player.closeScreen();return player.startRiding(this,true);}
        if(action>=10 && action<14 && !pending){selected=action-10;sync();return true;}
        return false;
    }
    @Override public boolean processInitialInteract(EntityPlayer player,EnumHand hand){
        if(world.isRemote || hand!=EnumHand.MAIN_HAND)return true;
        if(!isUsableByPlayer(player)){tell(player,"denied");return true;}
        ItemStack held=player.getHeldItem(hand);
        if(!pending && shotAge>=32 && Math.abs(getDriveSpeed())<.01 && trySalvage(player,held,grounded()))return true;
        if(held.getItem() instanceof ArtilleryTargetDesignatorItem && player.isSneaking()){((ArtilleryTargetDesignatorItem)held.getItem()).linkTo(this,player,held);return true;}
        if(DesignatorCompat.isDesignator(held) && player.isSneaking()){DesignatorCompat.resolveForEntity(this,player,hand,p->programTarget(player,p));return true;}
        player.openGui(WarTechReforged.instance,com.wartec.wartecmod.port.gui.WarTechGuiHandler.GUI_TUBE_ARTILLERY,world,getEntityId(),0,0);return true;
    }
    private void sync(){if(world==null || world.isRemote)return;NBTTagCompound n=new NBTTagCompound();n.setString("Failure",failure);n.setInteger("Deployment",deployment);n.setInteger("Reload",reload);n.setInteger("ShotAge",shotAge);n.setInteger("Selected",selected);n.setInteger("SelectedCount",stores.get(selected).getCount());n.setBoolean("Pending",pending);n.setFloat("Yaw",yaw);n.setFloat("Pitch",pitch);if(target!=null)n.setLong("Target",target.toLong());int count=0;for(int i=0;i<4;i++)count+=stores.get(i).getCount();n.setInteger("Ammo",count);dataManager.set(STATE,n);}
    private static boolean ammo(ItemStack s){return !s.isEmpty() && s.getItem()==WarTechContent.ARTILLERY_AMMO && s.getMetadata()>=0 && s.getMetadata()<12;}
    private static void tell(EntityPlayer p,String key,Object... args){if(p!=null)p.sendMessage(new TextComponentTranslation("wartec.tube."+key,args));}
    @Override public int getSizeInventory(){return 6;}
    @Override public ItemStack getStackInSlot(int i){return i>=0 && i<6?stores.get(i):ItemStack.EMPTY;}
    @Override public ItemStack decrStackSize(int i,int count){if(i<0 || i>=6)return ItemStack.EMPTY;ItemStack s=ItemStackHelper.getAndSplit(stores,i,count);sync();return s;}
    @Override public ItemStack removeStackFromSlot(int i){return decrStackSize(i,64);}
    @Override public void setInventorySlotContents(int i,ItemStack s){if(i<0 || i>=6)return;stores.set(i,s);if(!s.isEmpty())s.setCount(Math.min(s.getCount(),i<4?16:1));sync();}
    @Override public boolean isItemValidForSlot(int i,ItemStack s){return !s.isEmpty() && !pending && (i>=0 && i<4?ammo(s):i==4?s.getItem() instanceof IBatteryItem:i==5 && DesignatorCompat.isDesignator(s));}
    @Override public boolean isEmpty(){for(ItemStack s:stores)if(!s.isEmpty())return false;return true;}
    @Override public int getInventoryStackLimit(){return 16;}
    @Override public void clear(){cancel();stores.clear();sync();}
    @Override protected void writeEntityToNBT(NBTTagCompound n){super.writeEntityToNBT(n);NBTTagCompound inv=new NBTTagCompound();ItemStackHelper.saveAllItems(inv,stores);n.setTag("TubeStores",inv);sync();n.setTag("TubeState",state().copy());n.setInteger("TubePendingSlot",pendingSlot);n.setLong("TubeDeadline",deadline);if(operation!=null){n.setUniqueId("TubeOperation",operation);n.setUniqueId("TubeStep",step);}}
    @Override protected void readEntityFromNBT(NBTTagCompound n){super.readEntityFromNBT(n);ItemStackHelper.loadAllItems(n.getCompoundTag("TubeStores"),stores);NBTTagCompound s=n.getCompoundTag("TubeState");dataManager.set(STATE,s.copy());failure=s.getString("Failure");deployment=MathHelper.clamp(s.getInteger("Deployment"),0,tubeProfile().prepareTicks);reload=MathHelper.clamp(s.getInteger("Reload"),0,tubeProfile().reloadTicks);shotAge=MathHelper.clamp(s.getInteger("ShotAge"),0,1000);selected=MathHelper.clamp(s.getInteger("Selected"),0,3);yaw=s.getFloat("Yaw");pitch=MathHelper.clamp(s.getFloat("Pitch"),0,70);target=s.hasKey("Target")?BlockPos.fromLong(s.getLong("Target")):null;pendingSlot=n.getInteger("TubePendingSlot");pending=s.getBoolean("Pending") && pendingSlot>=0 && pendingSlot<4;deadline=n.getLong("TubeDeadline");if(n.hasUniqueId("TubeOperation") && n.hasUniqueId("TubeStep")){operation=n.getUniqueId("TubeOperation");step=n.getUniqueId("TubeStep");}sync();}
    public boolean planningOrder(PlanningCommand c,AttackOperation.Action action,BlockPos point,int slot){
        if(world.isRemote || !c.owns(this))return false;
        if(action==AttackOperation.Action.PREPARE){if(!grounded() || isBeingRidden() || Math.abs(getDriveSpeed())>.01)return false;setDeployed(true);return true;}
        if(action!=AttackOperation.Action.LAUNCH || !validTarget(point))return false;target=point;
        if(!reserve(slot))return false;operation=c.operation;step=c.step;return true;
    }
    public void revokePlanning(UUID id){if(id.equals(operation)){cancel();sync();}}
}
