package com.wartec.wartecmod.port.entity;

import api.hbm.energy.IBatteryItem;
import com.wartec.wartecmod.WarTechReforged;
import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.integration.*;
import com.wartec.wartecmod.port.network.MissileTrackingService;
import com.wartec.wartecmod.port.planning.*;
import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.network.datasync.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.*;

/** Seven independent chassis on the shared dev86 driving/camera path. */
public final class EntityModernVehicle extends EntityWarTechGroundVehicle {
    private static final DataParameter<Integer> DEPLOY=EntityDataManager.createKey(EntityModernVehicle.class,DataSerializers.VARINT);
    private static final DataParameter<Float> TURRET=EntityDataManager.createKey(EntityModernVehicle.class,DataSerializers.FLOAT);
    private static final DataParameter<Integer> CAP_SLOT=EntityDataManager.createKey(EntityModernVehicle.class,DataSerializers.VARINT);
    private static final DataParameter<Integer> CAP_TICKS=EntityDataManager.createKey(EntityModernVehicle.class,DataSerializers.VARINT);
    private static final DataParameter<Integer> QUEUED=EntityDataManager.createKey(EntityModernVehicle.class,DataSerializers.VARINT);
    private static final DataParameter<NBTTagCompound> VISUAL=EntityDataManager.createKey(EntityModernVehicle.class,DataSerializers.COMPOUND_TAG);
    private static final DataParameter<NBTTagCompound> TARGETS=EntityDataManager.createKey(EntityModernVehicle.class,DataSerializers.COMPOUND_TAG);
    private final NonNullList<ItemStack> stores=NonNullList.withSize(42,ItemStack.EMPTY);
    private final Deque<ShotOrder> queue=new ArrayDeque<>();
    private int reload,cooldown,shotSerial,pendingTarget=-1,importRevision,capHold;
    private long queueDeadline;
    private float previousTurretYaw, shownTurretYaw,previousCapTicks,shownCapTicks;
    @Override public void onUpdate() {
        if(world.isRemote) { previousTurretYaw=shownTurretYaw;shownTurretYaw=getTurretYaw();if(ticksExisted<2) previousTurretYaw=shownTurretYaw; }
        if(world.isRemote) { previousCapTicks=shownCapTicks;shownCapTicks=dataManager.get(CAP_TICKS);if(ticksExisted<2)previousCapTicks=shownCapTicks; }
        super.onUpdate();
    }
    public float getTurretYaw(float partial) { return world.isRemote?AirDefenseTraverse.interpolate(previousTurretYaw,shownTurretYaw,partial):getTurretYaw(); }
    public float traverseSpeed() { return modernProfile().isMlrs()?2F:modernProfile()==ModernVehicleProfile.BUK_M3?AirDefenseTraverse.BUK_YAW:AirDefenseTraverse.OSA_YAW; }
    public EntityModernVehicle(World world) { super(world,WarTechEntityProfile.MODERN_GRAD);setLegacyFireMode(0); }
    public EntityModernVehicle(World world,ModernVehicleProfile kind) { this(world);setProfile(kind.entity); }
    @Override protected void entityInit() {
        super.entityInit();dataManager.register(DEPLOY,0);dataManager.register(TURRET,0F);
        dataManager.register(CAP_SLOT,-1);dataManager.register(CAP_TICKS,0);dataManager.register(QUEUED,0);
        dataManager.register(VISUAL,new NBTTagCompound());dataManager.register(TARGETS,new NBTTagCompound());
    }
    public ModernVehicleProfile modernProfile() {
        ModernVehicleProfile p=ModernVehicleProfile.of(getProfile());return p==null?ModernVehicleProfile.GRAD:p;
    }
    public float getDeployment() { return dataManager.get(DEPLOY)/(float)modernProfile().prepareTicks; }
    public float getTurretYaw() { return dataManager.get(TURRET); }
    public int getCapSlot() { return dataManager.get(CAP_SLOT); }
    public float getCapSeconds() { return dataManager.get(CAP_TICKS)/20F; }
    public float getCapSeconds(float partial) { return world.isRemote?(previousCapTicks+(shownCapTicks-previousCapTicks)*partial)/20F:getCapSeconds(); }
    public int getQueuedShots() { return dataManager.get(QUEUED); }
    public int getReloadTicks() { return dataManager.get(VISUAL).getInteger("Reload"); }
    public int targetCount() { return modernProfile()==ModernVehicleProfile.HIMARS?6:1; }
    public boolean hasTarget(int row) { return row>=0 && row<targetCount() && dataManager.get(TARGETS).hasKey("T"+row,10); }
    public BlockPos getTarget(int row) {
        NBTTagCompound t=dataManager.get(TARGETS).getCompoundTag("T"+row);
        return new BlockPos(t.getInteger("X"),t.getInteger("Y"),t.getInteger("Z"));
    }
    public int getAreaRadius() { return dataManager.get(TARGETS).getInteger("Radius"); }
    public int getTargetRevision(int row) { return dataManager.get(TARGETS).getCompoundTag("T"+row).getInteger("Revision"); }
    public boolean isLoaded(int slot) {
        NBTTagCompound v=dataManager.get(VISUAL);return slot<32?(v.getInteger("Low")&(1<<slot))!=0:(v.getInteger("High")&(1<<(slot-32)))!=0;
    }
    public int loadedCount() { int n=0;for(int i=0;i<modernProfile().slots;i++) if(isLoaded(i)) n++;return n; }
    public boolean canOperate(EntityPlayer player) {
        return canRemoteOperate(player) && getDistanceSq(player)<=196;
    }
    public boolean canRemoteOperate(EntityPlayer player) {
        return canManage(player);
    }
    @Override public boolean isUsableByPlayer(EntityPlayer player) { return canOperate(player); }
    @Override public boolean isDefenseVehicle() { return !modernProfile().isMlrs(); }
    @Override public int getRequiredInterceptorTier() { return modernProfile().tier; }
    @Override public int getDefenseEngagementRange() { return modernProfile().tier==2?250:100; }
    @Override public int getBatterySlot() { return modernProfile().slots; }
    @Override public int getEnergyCapacity() { return modernProfile().isMlrs()?250000:1200000; }
    public boolean programTarget(EntityPlayer player,int row,int x,int y,int z,int radius) {
        return canOperate(player) && setTarget(player,row,x,y,z,radius);
    }
    private boolean setTarget(EntityPlayer player,int row,int x,int y,int z,int radius) {
        if(!queue.isEmpty() || !modernProfile().isMlrs() || row<0 || row>=targetCount()
            || y<0 || y>=world.getHeight() || !world.getWorldBorder().contains(new BlockPos(x,y,z))) return false;
        ModernMissileProfile missile=ModernMissileProfile.of(((MissileItem)modernProfile().ammunition()).getProfile());
        if(!missile.accepts(Math.hypot(x-posX,z-posZ))) { tell(player,"range");return false; }
        writeTarget(row,x,y,z,radius);return true;
    }
    private void writeTarget(int row,int x,int y,int z,int radius) {
        NBTTagCompound targets=dataManager.get(TARGETS).copy(),target=new NBTTagCompound();
        target.setInteger("X",x);target.setInteger("Y",y);target.setInteger("Z",z);
        target.setInteger("Revision",targets.getCompoundTag("T"+row).getInteger("Revision")+1);targets.setTag("T"+row,target);
        targets.setInteger("Radius",MathHelper.clamp(radius,0,120));dataManager.set(TARGETS,targets);
    }
    public boolean importTarget(EntityPlayer player,int row) {
        if(!canOperate(player) || !(world instanceof WorldServer) || !queue.isEmpty() || row<0 || row>=targetCount()) return false;
        ItemStack stack=getStackInSlot(modernProfile().slots+1),snapshot=stack.copy();int revision=++importRevision;
        return DesignatorCompat.resolveSavedTarget((WorldServer)world,snapshot.copy(),Double.NaN,
            ()->canOperate(player) && queue.isEmpty() && revision==importRevision && ItemStack.areItemStacksEqual(getStackInSlot(modernProfile().slots+1),snapshot),
            point->programTarget(player,row,MathHelper.floor(point.x),MathHelper.floor(point.y),MathHelper.floor(point.z),getAreaRadius()));
    }
    @Override protected void serverTick(WarTechEntityProfile ignored) {
        super.serverTick(ignored);
        ModernVehicleProfile kind=modernProfile();
        int progress=dataManager.get(DEPLOY),desired=isDeployed()?kind.prepareTicks:0;
        // Return the traversed package above the chassis before lowering it.
        boolean aligned=true;
        if(!isDeployed() && !kind.isRadar()) {
            float yaw=MathHelper.wrapDegrees(getTurretYaw());
            float next=progress==0?0:yaw-MathHelper.clamp(yaw,-traverseSpeed(),traverseSpeed());
            dataManager.set(TURRET,next);aligned=Math.abs(next)<.001F;
        }
        if(progress!=desired && (desired>progress || aligned)) dataManager.set(DEPLOY,progress+(desired>progress?1:-1));
        if(reload>0) reload--;if(cooldown>0) cooldown--;
        if(reload>0 && ticksExisted%10==0 || reload==0 && getReloadTicks()>0) syncStores();
        if(pendingTarget<0 && getCapSlot()>=0) {
            if(capHold>0) capHold--;
            else if(dataManager.get(CAP_TICKS)>0) dataManager.set(CAP_TICKS,Math.max(0,dataManager.get(CAP_TICKS)-ModernLaunchGeometry.BUK_CAP_STEP));
            else dataManager.set(CAP_SLOT,-1);
        }
        boolean revoked=queue.removeIf(order->order.operation!=null && !com.wartec.wartecmod.port.planning.PlanningControl.live(world,order.operation,order.step));if(revoked){if(queue.isEmpty())cancelQueue();syncQueue();}
        if(!queue.isEmpty() && world instanceof WorldServer && ticksExisted%20==1) holdQueueChunks();
        charge();
        if(!isDeployed() || progress<kind.prepareTicks || !hasGroundSupport() || Math.abs(getDriveSpeed())>.01) {
            setLegacyOperational(false);setLegacyContacts(0);releasePending();
            if(!kind.isMlrs()) MissileTrackingService.removeRadar(world,getEntityId());
            return;
        }
        if(kind.isMlrs()) tickQueue();else tickDefense();
    }
    private void charge() {
        ItemStack stack=getStackInSlot(getBatterySlot());
        if(stack.getItem() instanceof IBatteryItem) {
            IBatteryItem b=(IBatteryItem)stack.getItem();
            long amount=Math.min(Math.min(b.getCharge(stack),b.getDischargeRate()),getEnergyCapacity()-getLegacyPower());
            if(amount>0) { b.dischargeBattery(stack,amount);setLegacyPower(getLegacyPower()+(int)amount); }
        }
    }
    private int liveFlights(boolean interceptors) {
        int count=0;for(Entity e:world.loadedEntityList) if(!e.isDead && e.getEntityData().hasUniqueId("ModernLauncher")
            && getUniqueID().equals(e.getEntityData().getUniqueId("ModernLauncher")) && e instanceof EntityWarTechMissile
            && (((EntityWarTechMissile)e).getMissileSpecification().getFlightFamily()==LegacyMissileSpecification.FlightFamily.INTERCEPTOR)==interceptors
            && (!interceptors || ((EntityWarTechMissile)e).getTrackedEntityId()>=0)) count++;
        return count;
    }
    private void tickQueue() {
        if(queue.isEmpty()) return;
        if(world.getTotalWorldTime()>queueDeadline) { cancelQueue();return; }
        if(cooldown>0 || reload>0 || getLegacyPower()<2500 || liveFlights(false)>=12 || MissileChunkLoader.availableFlightSlots(world)<=8) return;
        ShotOrder order=queue.peek();
        if(ModernSalvoLedger.contains(world,order.id)) { stores.set(order.slot,ItemStack.EMPTY);queue.remove();syncStores();syncQueue();return; }
        if(getStackInSlot(order.slot).isEmpty()) { queue.remove();syncQueue();return; }
        if(!aimAt(order.target.getX(),order.target.getZ())) return;
        ModernLaunchGeometry.Shot shot=ModernLaunchGeometry.shot(this,order.slot);
        EntityWarTechMissile missile=LegacyEntityFactory.missile(world,((MissileItem)modernProfile().ammunition()).getProfile());
        missile.setPosition(shot.origin.x,shot.origin.y,shot.origin.z);missile.setOwnerIdentity(getOwnerUuid(),getOwnerTeam());
        missile.setVisual("missile/"+missile.getMissileProfile().getIntentPath(),((MissileItem)stores.get(order.slot).getItem()).getVariant(stores.get(order.slot)));
        missile.configureLegacyGroundLaunch(order.target.getX(),order.target.getY(),order.target.getZ());
        missile.configureModernVisual(shot.scale,false,shot.direction,shot.scale[1]+.25F);
        missile.motionX=shot.direction.x;missile.motionY=shot.direction.y;missile.motionZ=shot.direction.z;
        missile.rotationYaw=(float)Math.toDegrees(Math.atan2(shot.direction.x,shot.direction.z));
        missile.rotationPitch=(float)-Math.toDegrees(Math.asin(shot.direction.y));
        missile.getEntityData().setUniqueId("ModernLauncher",getUniqueID());
        missile.getEntityData().setUniqueId("ModernShot",order.id);
        PlanningHooks.stamp(missile,order.operation,order.step);
        missile.getEntityData().setBoolean("ModernExactAim",order.target.equals(getTarget(0)) && targetCount()==1 && getAreaRadius()>0);
        if(!MissileChunkLoader.spawnFlight(missile)) return;
        PlanningHooks.emitted(missile);
        ModernSalvoLedger.commit(world,order.id);
        stores.set(order.slot,ItemStack.EMPTY);queue.remove();cooldown=Math.max(modernProfile().interval,order.interval);shotSerial++;setLegacyPower(getLegacyPower()-2500);
        syncStores();syncQueue();launchEffects(shot);
    }
    private void tickDefense() {
        ModernVehicleProfile kind=modernProfile();
        boolean online=getHealthValue()>=getHealthCapacity()*.2F && getLegacyPower()>=120;
        setLegacyOperational(online);
        if(!online) { MissileTrackingService.removeRadar(world,getEntityId());setLegacyContacts(0);releasePending();return; }
        setLegacyPower(getLegacyPower()-120);
        if(kind.isRadar() || kind==ModernVehicleProfile.OSA) {
            if(ticksExisted%10==Math.floorMod(getEntityId(),10)) {
                double range=kind.isRadar()?850:220,ceiling=kind.isRadar()?650:180;
                if(getHealthValue()<getHealthCapacity()*.5F) range*=.72;
                setLegacyContacts(MissileTrackingService.updateRadarSweep(world,getEntityId(),posX,posY+3,posZ,range,ceiling,24,getOwnerTeam(),ElectronicWarfareService.BAND_S));
                int[] blips=MissileTrackingService.getRadarBlips(world,getEntityId(),posX,posZ,16);
                for(int i=0;i<16;i++) setLegacyBlip(i,i<blips.length?blips[i]:0);
            }
        }
        if(kind==ModernVehicleProfile.BUK_M3 && ticksExisted%10==0) {
            int contacts=0;for(Entity e:world.loadedEntityList) if(eligible(e)) {
                if(contacts<16) setLegacyBlip(contacts,((int)Math.round(e.posX-posX)&65535)<<16 | ((int)Math.round(e.posZ-posZ)&65535));
                contacts++;
            }
            for(int i=Math.min(contacts,16);i<16;i++) setLegacyBlip(i,0);
            setLegacyContacts(Math.min(24,contacts));
        }
        if(!kind.isRadar() && getLegacyFireMode()!=0 && pendingTarget<0 && getCapSlot()<0) {
            Entity tracked=MissileTrackingService.findTrackingThreat(world,getPositionVector().addVector(0,3,0),kind.tier,
                getDefenseEngagementRange(),launcherKey(),getOwnerTeam(),-1,false);
            if(tracked!=null) {
                Vec3d lead=AirDefenseFireControl.lead(tracked,getPositionVector().addVector(0,3,0),ModernLaunchGeometry.capDelay(kind,0),kind.tier);
                aimAt(lead.x,lead.z);
            }
        }
        if(kind.isRadar() || getLegacyFireMode()==0 || cooldown>0 || reload>0 || getLegacyPower()<50000 || liveFlights(true)>=(kind==ModernVehicleProfile.BUK_M3?2:1)) return;
        if(pendingTarget>=0) {
            Entity target=world.getEntityByID(pendingTarget);
            if(!eligible(target) || !MissileTrackingService.holdReservation(world,pendingTarget,launcherKey())) { releasePending();return; }
            Vec3d lead=AirDefenseFireControl.lead(target,getPositionVector().addVector(0,3,0),ModernLaunchGeometry.capDelay(kind,dataManager.get(CAP_TICKS)),kind.tier);
            if(!aimAt(lead.x,lead.z)) return;
            int ticks=Math.min(30,dataManager.get(CAP_TICKS)+ModernLaunchGeometry.BUK_CAP_STEP);dataManager.set(CAP_TICKS,ticks);
            if(kind==ModernVehicleProfile.BUK_M3 && ticks<30) return;
            launchInterceptor(target,dataManager.get(CAP_SLOT));return;
        }
        if(getCapSlot()>=0) return;
        int slot=-1;for(int i=0;i<kind.slots;i++) if(!stores.get(i).isEmpty()) { slot=i;break; }
        if(slot<0) return;
        Entity target=reserveDefenseTarget();if(target==null) return;
        pendingTarget=target.getEntityId();dataManager.set(CAP_SLOT,slot);dataManager.set(CAP_TICKS,0);
    }
    private Entity reserveDefenseTarget() {
        Entity e=MissileTrackingService.findLaunchThreat(world,getPositionVector().addVector(0,3,0),modernProfile().tier,
            getDefenseEngagementRange(),launcherKey(),getOwnerTeam(),false,24+ModernLaunchGeometry.capDelay(modernProfile(),0),this::eligible);
        if(eligible(e) && MissileTrackingService.tryReserve(world,e.getEntityId(),launcherKey())) return e;
        return null;
    }
    private boolean eligible(Entity e) {
        ModernVehicleProfile kind=modernProfile();
        if(e==null || e==this || e.isDead || e instanceof EntityStrategicMissile || isFriendlyOrOwner(e)
            || !MissileTrackingService.canInterceptorEngage(e,kind.tier,getDefenseEngagementRange())) return false;
        if(MissileTrackingService.getThreatTier(e)>kind.tier) return false;
        Vec3d sensor=getPositionVector().addVector(0,3,0);
        return MissileTrackingService.hasDefenseContact(world,e,sensor,getOwnerTeam(),false)
            && AirDefenseFireControl.canLaunch(e,sensor,getDefenseEngagementRange(),kind.tier,
                24+ModernLaunchGeometry.capDelay(kind,dataManager.get(CAP_TICKS)));
    }
    private long launcherKey() { return 0x4D4F444E00000000L | (getEntityId()&0xFFFFFFFFL); }
    @Override public NBTTagCompound commandDefenseTelemetry(){NBTTagCompound n=super.commandDefenseTelemetry();n.setLong("Key",launcherKey());n.setInteger("Cooldown",Math.max(cooldown,reload));n.setInteger("Pending",pendingTarget);n.setInteger("Delay",24+ModernLaunchGeometry.capDelay(modernProfile(),dataManager.get(CAP_TICKS)));n.setBoolean("Deployed",getDeployment()>=1 && hasGroundSupport());n.setInteger("Channels",liveFlights(true));n.setInteger("MaxChannels",modernProfile()==ModernVehicleProfile.BUK_M3?2:1);n.setString("Stage",capHold>0?"exit":dataManager.get(CAP_TICKS)>0?"opening":pendingTarget>=0?"tracking":"guard");return n;}
    private void launchInterceptor(Entity target,int slot) {
        ModernLaunchGeometry.Shot shot=ModernLaunchGeometry.shot(this,slot);
        EntityWarTechMissile missile=LegacyEntityFactory.missile(world,modernProfile().tier==2?MissileProfile.ANTI_AIR_TIER_2:MissileProfile.ANTI_AIR_TIER_1);
        missile.setPosition(shot.origin.x,shot.origin.y,shot.origin.z);missile.setOwnerIdentity(getOwnerUuid(),getOwnerTeam());
        missile.setVisual("missile/anti_air_tier_"+modernProfile().tier,0);
        boolean malfunction=VlsInterceptorGuidance.configureMobileLaunch(missile,target,modernProfile().tier,false);
        missile.configureModernVisual(shot.scale,true,shot.direction,shot.scale[1]+.25F);
        missile.configureModernExit(modernProfile(),slot);
        missile.motionX=shot.direction.x;missile.motionY=shot.direction.y;missile.motionZ=shot.direction.z;
        missile.rotationYaw=(float)Math.toDegrees(Math.atan2(shot.direction.x,shot.direction.z));
        missile.rotationPitch=(float)-Math.toDegrees(Math.asin(shot.direction.y));
        missile.getEntityData().setUniqueId("ModernLauncher",getUniqueID());
        if(!MissileChunkLoader.spawnFlight(missile)) return;
        PlanningHooks.defenseShot(world,this,missile,target);
        if(malfunction) { MissileTrackingService.releaseReservation(world,target.getEntityId(),launcherKey());MissileTrackingService.deferTarget(world,target.getEntityId()); }
        else MissileTrackingService.confirmReservation(world,target.getEntityId(),launcherKey(),missile.getEntityId());
        stores.set(slot,ItemStack.EMPTY);pendingTarget=-1;cooldown=modernProfile().interval;capHold=missile.getModernVisual().getInteger("ExitTicks")+2;setLegacyPower(getLegacyPower()-50000);
        syncStores();launchEffects(shot);
    }
    private void launchEffects(ModernLaunchGeometry.Shot shot) {
        playLegacySound("wartecmod:weapon.ballisticmissiletakeoff",net.minecraft.init.SoundEvents.ENTITY_GENERIC_EXPLODE,modernProfile().isMlrs()?8:10,.8F);
        VlsInterceptorGuidance.spawnLaunchSmoke(world,shot.origin.x,shot.origin.y,shot.origin.z,Math.max(1,modernProfile().tier));
    }
    private void releasePending() {
        if(pendingTarget>=0) MissileTrackingService.releaseReservation(world,pendingTarget,launcherKey());
        pendingTarget=-1;
        // A launched missile must finish leaving the tube before its cap closes.
        // Cancelling a reservation reverses the existing cap animation smoothly.
        if(capHold==0 && dataManager.get(CAP_TICKS)==0) dataManager.set(CAP_SLOT,-1);
    }
    private void syncStores() {
        if(world==null || world.isRemote) return;
        int low=0,high=0;for(int i=0;i<modernProfile().slots;i++) if(!stores.get(i).isEmpty()) { if(i<32) low|=1<<i;else high|=1<<(i-32); }
        NBTTagCompound tag=new NBTTagCompound();tag.setInteger("Low",low);tag.setInteger("High",high);tag.setInteger("Reload",reload);dataManager.set(VISUAL,tag);
    }
    private void syncQueue() { dataManager.set(QUEUED,queue.size()); }
    private boolean holdQueueChunks() {
        return world instanceof WorldServer && OperationalChunks.requestUntil((WorldServer)world,getPosition(),1,"modern-queue:"+getUniqueID(),
            ()->!isDead && !queue.isEmpty() && world.getTotalWorldTime()<=queueDeadline,()->queue.isEmpty());
    }
    private boolean aimAt(double x,double z) {
        float wanted=MathHelper.wrapDegrees((float)Math.toDegrees(Math.atan2(posX-x,z-posZ))-rotationYaw);
        float current=getTurretYaw(),difference=MathHelper.wrapDegrees(wanted-current);
        dataManager.set(TURRET,MathHelper.wrapDegrees(current+MathHelper.clamp(difference,-traverseSpeed(),traverseSpeed())));
        return Math.abs(MathHelper.wrapDegrees(wanted-getTurretYaw()))<2;
    }
    private void cancelQueue() { queue.clear();syncQueue();releasePending(); }
    public boolean hasGroundSupport() {
        AxisAlignedBB b=getEntityBoundingBox();return !isInWater() && !isInLava() && !world.getCollisionBoxes(null,
            new AxisAlignedBB(b.minX+.05,b.minY-.15,b.minZ+.05,b.maxX-.05,b.minY+.002,b.maxZ-.05)).isEmpty();
    }
    @Override public boolean handleLegacyGuiAction(int action,EntityPlayer player) {
        if(!canOperate(player)) return false;
        if(action==1 && queue.isEmpty() && pendingTarget<0 && getCapSlot()<0) {
            if(!isDeployed() && (!hasGroundSupport() || Math.abs(getDriveSpeed())>.01)) { tell(player,"stop");return false; }
            setDeployed(!isDeployed());removePassengers();return true;
        }
        if(action==3 && !isDeployed() && getDeployment()==0 && !isBeingRidden()) { player.closeScreen();return player.startRiding(this,true); }
        if(action==4) { cancelQueue();return true; }
        if(action==5 && !modernProfile().isMlrs()) { setLegacyFireMode(getLegacyFireMode()==0?1:0);if(getLegacyFireMode()==0) releasePending();return true; }
        if(action==0 || action==2 || action>=30 && action<=69) return beginSalvo(player,action==2?1:action>=30?action-29:modernProfile().slots);
        return false;
    }
    private boolean beginSalvo(EntityPlayer player,int limit) {
        if(modernProfile().isMlrs() && queue.isEmpty() && isDeployed() && getDeployment()==1 && reload==0 && hasGroundSupport()
            && Math.abs(getDriveSpeed())<.01 && getLegacyPower()>=2500 && loadedCount()>0) {
            ModernMissileProfile profile=ModernMissileProfile.of(((MissileItem)modernProfile().ammunition()).getProfile());
            for(int slot=0;slot<modernProfile().slots;slot++) if(!stores.get(slot).isEmpty()) {
                int row=targetCount()==6?slot:0;if(!hasTarget(row)) { tell(player,"target");cancelQueue();return false; }
                BlockPos point=getTarget(row);
                if(targetCount()==1 && !queue.isEmpty() && getAreaRadius()>0) {
                    double a=slot*2.399963229728653;double r=getAreaRadius()*Math.sqrt(slot/(double)Math.max(1,modernProfile().slots-1));
                    point=point.add((int)Math.round(Math.cos(a)*r),0,(int)Math.round(Math.sin(a)*r));
                }
                if(!world.getWorldBorder().contains(point) || !profile.accepts(Math.hypot(point.getX()-posX,point.getZ()-posZ))) { tell(player,"range");cancelQueue();return false; }
                queue.add(new ShotOrder(slot,point,UUID.randomUUID()));if(queue.size()>=limit) break;
            }
            queueDeadline=world.getTotalWorldTime()+3600;
            if(!queue.isEmpty() && !holdQueueChunks()) { cancelQueue();tell(player,"busy");return false; }
            syncQueue();return !queue.isEmpty();
        }
        return false;
    }
    @Override public boolean acceptArtilleryDesignatorTarget(EntityPlayer player,BlockPos point) {
        if(!modernProfile().isMlrs() || !canRemoteOperate(player)) { tell(player,"denied");return false; }
        if(!isDeployed()) { tell(player,"deploy");return false; }
        if(!hasGroundSupport() || Math.abs(getDriveSpeed())>.01) { tell(player,"stop");return false; }
        if(loadedCount()==0) { tell(player,"empty");return false; }
        // Preparation/reloading and the physical firing interval are transient.
        // Clicks may reserve different tubes while earlier orders are still aiming.
        if(getDeployment()!=1 || reload>0) return false;
        BitSet reserved=new BitSet(modernProfile().slots);
        for(ShotOrder order:queue) reserved.set(order.slot);
        int slot=0;while(slot<modernProfile().slots && (stores.get(slot).isEmpty() || reserved.get(slot))) slot++;
        if(slot>=modernProfile().slots) return false; // Loaded tubes already have orders.
        if(getLegacyPower()<2500*(queue.size()+1)) { tell(player,"power");return false; }
        ModernMissileProfile missile=ModernMissileProfile.of(((MissileItem)modernProfile().ammunition()).getProfile());
        if(point.getY()<0 || point.getY()>=world.getHeight() || !world.getWorldBorder().contains(point)
            || !missile.accepts(Math.hypot(point.getX()-posX,point.getZ()-posZ))) { tell(player,"range");return false; }
        boolean first=queue.isEmpty();ShotOrder order=new ShotOrder(slot,point,UUID.randomUUID());queue.add(order);
        queueDeadline=world.getTotalWorldTime()+3600;
        if(first && !holdQueueChunks()) { queue.removeLast();syncQueue();tell(player,"busy");return false; }
        writeTarget(targetCount()==6?slot:0,point.getX(),point.getY(),point.getZ(),0);
        syncQueue();return true;
    }
    @Override public boolean processInitialInteract(EntityPlayer player,EnumHand hand) {
        if(hand!=EnumHand.MAIN_HAND || world.isRemote) return true;
        if(!canOperate(player)) { tell(player,"denied");return true; }
        ItemStack held=player.getHeldItem(hand);
        if(tryRepairDefense(player,held)) return true;
        if(queue.isEmpty() && pendingTarget<0 && Math.abs(getDriveSpeed())<.01 && trySalvage(player,held,hasGroundSupport())) return true;
        if(!modernProfile().isMlrs() && !held.isEmpty() && held.getItem()==modernProfile().ammunition()) {
            if(isDeployed() || getDeployment()>0) { tell(player,"stow");return true; }
            for(int i=0;i<modernProfile().slots;i++) if(stores.get(i).isEmpty()) {
                ItemStack loaded=held.copy();loaded.setCount(1);setInventorySlotContents(i,loaded);if(!player.capabilities.isCreativeMode) held.shrink(1);return true;
            }
            tell(player,"full");return true;
        }
        if(held.getItem() instanceof ArtilleryTargetDesignatorItem && modernProfile().isMlrs() && player.isSneaking()) {
            ((ArtilleryTargetDesignatorItem)held.getItem()).linkTo(this,player,held);return true;
        }
        if(DesignatorCompat.isDesignator(held) && modernProfile().isMlrs() && player.isSneaking()) {
            int row=0;if(targetCount()==6) while(row<5 && hasTarget(row)) row++;
            final int selected=row;
            DesignatorCompat.resolveForEntity(this,player,hand,p->programTarget(player,selected,p.getX(),p.getY(),p.getZ(),getAreaRadius()));return true;
        }
        if(held.getItem() instanceof IBatteryItem) return super.processInitialInteract(player,hand);
        player.openGui(WarTechReforged.instance,com.wartec.wartecmod.port.gui.WarTechGuiHandler.GUI_MODERN_VEHICLE,world,getEntityId(),0,0);return true;
    }
    private static void tell(EntityPlayer player,String key) { if(player!=null) player.sendMessage(new TextComponentTranslation("wartec.modern.error."+key)); }
    public String readiness() {
        if(getQueuedShots()>0) return "firing";
        if(!hasGroundSupport() || Math.abs(getDriveSpeed())>.01) return "stop";
        if(!isDeployed()) return "stowed";
        if(getDeployment()<1) return "preparing";
        if(getReloadTicks()>0) return "reload";
        if(getLegacyPower()<2500) return "power";
        if(loadedCount()==0) return "empty";
        for(int slot=0;slot<modernProfile().slots;slot++) if(isLoaded(slot) && !hasTarget(targetCount()==6?slot:0)) return "target";
        return "ready";
    }
    @Override public int getSizeInventory() { return modernProfile().slots+2; }
    @Override public boolean isEmpty() { for(int i=0;i<getSizeInventory();i++) if(!stores.get(i).isEmpty()) return false;return true; }
    @Override public ItemStack getStackInSlot(int slot) { return slot>=0 && slot<getSizeInventory()?stores.get(slot):ItemStack.EMPTY; }
    @Override public ItemStack decrStackSize(int slot,int count) {
        if(slot<0 || slot>=getSizeInventory()) return ItemStack.EMPTY;
        ItemStack stack=ItemStackHelper.getAndSplit(stores,slot,count);syncStores();return stack;
    }
    @Override public ItemStack removeStackFromSlot(int slot) { return decrStackSize(slot,64); }
    @Override public void setInventorySlotContents(int slot,ItemStack stack) {
        if(slot<0 || slot>=getSizeInventory()) return;
        stores.set(slot,stack);if(!stack.isEmpty()) stack.setCount(1);
        if(slot<modernProfile().slots) reload=modernProfile().reloadTicks;syncStores();
    }
    @Override public boolean isItemValidForSlot(int slot,ItemStack stack) {
        if(stack.isEmpty()) return false;
        if(slot==getBatterySlot()) return stack.getItem() instanceof IBatteryItem;
        if(slot==getBatterySlot()+1) return modernProfile().isMlrs() && DesignatorCompat.isDesignator(stack);
        return slot>=0 && slot<modernProfile().slots && !isDeployed() && getDeployment()==0 && queue.isEmpty() && pendingTarget<0 && stack.getItem()==modernProfile().ammunition();
    }
    @Override public int getInventoryStackLimit() { return 1; }
    @Override public void clear() { stores.clear();cancelQueue();syncStores(); }
    @Override public void destroyByAntiRadiationMissile() { if(isDefenseVehicle()) attackEntityFrom(net.minecraft.util.DamageSource.GENERIC,getHealthCapacity()+1); }
    @Override public void setDead() { if(world!=null && !world.isRemote) { cancelQueue();MissileTrackingService.removeRadar(world,getEntityId()); }super.setDead(); }
    @Override public AxisAlignedBB getRenderBoundingBox() { return getEntityBoundingBox().grow(14,18,14); }
    @Override protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);NBTTagCompound inventory=new NBTTagCompound();ItemStackHelper.saveAllItems(inventory,stores);tag.setTag("ModernStores",inventory);
        tag.setInteger("ModernDeployment",dataManager.get(DEPLOY));tag.setFloat("ModernTurretYaw",getTurretYaw());tag.setTag("ModernTargets",dataManager.get(TARGETS).copy());
        tag.setInteger("ModernReload",reload);tag.setInteger("ModernCooldown",cooldown);tag.setLong("ModernDeadline",queueDeadline);
        tag.setInteger("ModernCapSlot",getCapSlot());tag.setInteger("ModernCapTicks",dataManager.get(CAP_TICKS));tag.setInteger("ModernCapHold",capHold);
        tag.setInteger("ModernShotSerial",shotSerial);NBTTagList orders=new NBTTagList();for(ShotOrder order:queue) orders.appendTag(order.write());tag.setTag("ModernQueue",orders);
    }
    @Override protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);ItemStackHelper.loadAllItems(tag.getCompoundTag("ModernStores"),stores);
        dataManager.set(DEPLOY,MathHelper.clamp(tag.getInteger("ModernDeployment"),0,modernProfile().prepareTicks));dataManager.set(TURRET,tag.getFloat("ModernTurretYaw"));
        dataManager.set(TARGETS,tag.getCompoundTag("ModernTargets").copy());reload=Math.max(0,tag.getInteger("ModernReload"));cooldown=Math.max(0,tag.getInteger("ModernCooldown"));
        queueDeadline=tag.getLong("ModernDeadline");shotSerial=tag.getInteger("ModernShotSerial");queue.clear();NBTTagList orders=tag.getTagList("ModernQueue",10);
        Set<Integer> slots=new HashSet<>();for(int i=0;i<Math.min(40,orders.tagCount());i++) {
            ShotOrder order=ShotOrder.read(orders.getCompoundTagAt(i));if(order.slot>=0 && order.slot<modernProfile().slots && slots.add(order.slot)) queue.add(order);
        }
        pendingTarget=-1;
        int cap=tag.hasKey("ModernCapSlot")?tag.getInteger("ModernCapSlot"):-1;
        dataManager.set(CAP_SLOT,cap>=0 && cap<modernProfile().slots?cap:-1);
        dataManager.set(CAP_TICKS,MathHelper.clamp(tag.getInteger("ModernCapTicks"),0,30));capHold=MathHelper.clamp(tag.getInteger("ModernCapHold"),0,128);
        syncStores();syncQueue();
    }
    private static final class ShotOrder {
        final int slot;final BlockPos target;final UUID id;
        UUID operation,step;int interval;
        ShotOrder(int slot,BlockPos target,UUID id) { this.slot=slot;this.target=target;this.id=id; }
        NBTTagCompound write() { NBTTagCompound tag=new NBTTagCompound();tag.setInteger("Slot",slot);tag.setLong("Target",target.toLong());tag.setUniqueId("Id",id);if(step!=null){tag.setUniqueId("PlanStep",step);tag.setUniqueId("PlanOperation",operation);tag.setInteger("PlanInterval",interval);}return tag; }
        static ShotOrder read(NBTTagCompound tag) { ShotOrder s=new ShotOrder(tag.getInteger("Slot"),BlockPos.fromLong(tag.getLong("Target")),tag.hasUniqueId("Id")?tag.getUniqueId("Id"):UUID.randomUUID());if(tag.hasUniqueId("PlanStep") && tag.hasUniqueId("PlanOperation")){s.step=tag.getUniqueId("PlanStep");s.operation=tag.getUniqueId("PlanOperation");s.interval=MathHelper.clamp(tag.getInteger("PlanInterval"),1,72000);}return s; }
    }
    public void revokePlanning(java.util.UUID id){if(queue.removeIf(order->id.equals(order.operation))){if(queue.isEmpty())cancelQueue();else syncQueue();}}
    public boolean planningOrder(PlanningCommand command,AttackOperation.Action action,BlockPos point,int preferredSlot,int count,int interval) {
        if(world.isRemote || !command.owns(this))return false;
        if(action==AttackOperation.Action.PREPARE){if(!hasGroundSupport() || Math.abs(getDriveSpeed())>.01 || isBeingRidden())return false;setDeployed(true);return true;}
        if(action!=AttackOperation.Action.LAUNCH || !modernProfile().isMlrs() || !isDeployed() || getDeployment()!=1 || reload>0 || !hasGroundSupport() || Math.abs(getDriveSpeed())>.01 || !queue.isEmpty())return false;
        ModernMissileProfile p=ModernMissileProfile.of(((MissileItem)modernProfile().ammunition()).getProfile());
        if(!world.getWorldBorder().contains(point) || !p.accepts(Math.hypot(point.getX()-posX,point.getZ()-posZ)) || getLegacyPower()<2500*count || loadedCount()<count)return false;
        if(preferredSlot>=0 && (count!=1 || !isLoaded(preferredSlot)))return false;
        for(int slot=0;slot<modernProfile().slots && queue.size()<count;slot++)if(isLoaded(slot) && (preferredSlot<0 || preferredSlot==slot)){
            ShotOrder s=new ShotOrder(slot,point,UUID.randomUUID());s.operation=command.operation;s.step=command.step;s.interval=interval;queue.add(s);
            writeTarget(targetCount()==6?slot:0,point.getX(),point.getY(),point.getZ(),0);
        }
        queueDeadline=world.getTotalWorldTime()+Math.min(72000,Math.max(3600,(long)count*interval+1200));
        if(queue.size()!=count || !holdQueueChunks()){cancelQueue();return false;}syncQueue();return true;
    }
}
