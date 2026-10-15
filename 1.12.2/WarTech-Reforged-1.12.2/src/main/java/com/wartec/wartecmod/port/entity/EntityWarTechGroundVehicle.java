package com.wartec.wartecmod.port.entity;

import api.hbm.entity.IRadarDetectable;
import com.wartec.wartecmod.WarTechReforged;
import com.wartec.wartecmod.port.content.ArtilleryTargetDesignatorItem;
import com.wartec.wartecmod.port.content.ArtilleryAmmoItem;
import com.wartec.wartecmod.port.content.HimarsAmmoItem;
import com.wartec.wartecmod.port.content.MissileProfile;
import com.wartec.wartecmod.port.content.PantsirAmmoBeltItem;
import com.wartec.wartecmod.port.content.WarTechContent;
import com.wartec.wartecmod.port.integration.AircraftCountermeasureCompat;
import com.wartec.wartecmod.port.integration.DesignatorCompat;
import com.wartec.wartecmod.port.integration.HbmExplosionCompat;
import com.wartec.wartecmod.port.integration.MissileChunkLoader;
import com.wartec.wartecmod.port.integration.NetworkTeamHelper;
import com.wartec.wartecmod.port.integration.ElectronicWarfareService;
import com.wartec.wartecmod.port.network.FactionTerritoryData;
import com.wartec.wartecmod.port.gui.WarTechGuiHandler;
import com.wartec.wartecmod.port.network.MissileTrackingService;
import com.wartec.wartecmod.port.network.VehicleStateMessage;
import com.wartec.wartecmod.port.network.WarTechNetwork;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.EnumHand;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class EntityWarTechGroundVehicle extends EntityWarTechBase {
    private static final DataParameter<NBTTagCompound> SHORAD_POSE = EntityDataManager.createKey(EntityWarTechGroundVehicle.class,DataSerializers.COMPOUND_TAG);
    private final ShoradAnimation shorad = new ShoradAnimation();
    private final ShoradAnimation previousShorad = new ShoradAnimation();
    private int shoradTarget=-1;
    private static final DataParameter<Integer> GUN_STATE =
            EntityDataManager.createKey(EntityWarTechGroundVehicle.class,
                    DataSerializers.VARINT);
    private static final DataParameter<Float> ARTILLERY_YAW =
            EntityDataManager.createKey(EntityWarTechGroundVehicle.class,
                    DataSerializers.FLOAT);
    private static final DataParameter<Float> ARTILLERY_PITCH =
            EntityDataManager.createKey(EntityWarTechGroundVehicle.class,
                    DataSerializers.FLOAT);
    private static final DataParameter<Integer> ARTILLERY_AMMO_TYPE =
            EntityDataManager.createKey(EntityWarTechGroundVehicle.class,
                    DataSerializers.VARINT);
    private static final DataParameter<Integer> ARTILLERY_AMMO_COUNT =
            EntityDataManager.createKey(EntityWarTechGroundVehicle.class,
                    DataSerializers.VARINT);
    private static final DataParameter<Float> ARTILLERY_CRANE =
            EntityDataManager.createKey(EntityWarTechGroundVehicle.class,
                    DataSerializers.FLOAT);
    private static final DataParameter<Float> ARTILLERY_RECOIL =
            EntityDataManager.createKey(EntityWarTechGroundVehicle.class,
                    DataSerializers.FLOAT);
    private static final DataParameter<String> ARTILLERY_WHITELIST =
            EntityDataManager.createKey(EntityWarTechGroundVehicle.class,
                    DataSerializers.STRING);
    private static final int TARGET_PLAYERS = 1 << 6;
    private static final int TARGET_ANIMALS = 1 << 7;
    private static final int TARGET_MOBS = 1 << 8;
    private static final int TARGET_MACHINES = 1 << 9;
    private static final int GUN_BURST_ENERGY = 1800;
    private static final int GUN_BURST_ROUNDS = 10;
    private static final double GUN_RANGE = 100.0D;

    private float driverForward;
    private float driverStrafe;
    private int lastDriverInputTick = -1;
    private double driveSpeed;
    private double steeringState;
    private int driverSequence = -1;
    private boolean driverBrake;
    private boolean parkingBrake;
    private boolean exitRequested;
    private boolean allowingExit;
    private java.util.UUID inputDriver;
    private double visualCorrectionX,visualCorrectionY,visualCorrectionZ;
    private float visualCorrectionYaw;
    private double previousCorrectionX,previousCorrectionY,previousCorrectionZ;
    private float previousCorrectionYaw,visualCorrectionPitch,previousCorrectionPitch;
    private boolean localPrediction;
    private int predictionEpoch;
    private double wheelTravel;
    private static final DataParameter<Float> DRIVE_SPEED = EntityDataManager.createKey(EntityWarTechGroundVehicle.class,DataSerializers.FLOAT);
    private static final DataParameter<Float> DRIVE_STEERING = EntityDataManager.createKey(EntityWarTechGroundVehicle.class,DataSerializers.FLOAT);
    private static final DataParameter<Boolean> PARKING_BRAKE = EntityDataManager.createKey(EntityWarTechGroundVehicle.class,DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> DRIVE_AMMO = EntityDataManager.createKey(EntityWarTechGroundVehicle.class,DataSerializers.VARINT);
    private double clientTargetX;
    private double clientTargetY;
    private double clientTargetZ;
    private float clientTargetYaw;
    private float clientTargetPitch;
    private int clientInterpolationTicks;
    private int gunCooldown;
    private int gunFiringTicks;
    private int gunTargetId = -1;
    private int gunLockTicks;
    private int gunHitScore;
    private boolean gunTierOneSolution;
    private float gunAimYaw;
    private float gunAimPitch;
    private float clientPreviousGunYaw;
    private float clientGunYaw;
    private float clientPreviousGunPitch;
    private float clientGunPitch;
    private int clientGunBurstSerial = -1;
    private int artilleryTargetId = -1;
    private int artillerySearchTimer;
    private int artilleryFireTimer;
    private final List<Vec3d> artilleryTargetQueue = new ArrayList<>();
    private float clientPreviousArtilleryYaw;
    private float clientArtilleryYaw;
    private float clientPreviousArtilleryPitch;
    private float clientArtilleryPitch;
    private float clientPreviousArtilleryCrane;
    private float clientArtilleryCrane;
    private float clientPreviousArtilleryRecoil;
    private float clientArtilleryRecoil;

    public EntityWarTechGroundVehicle(World world) {
        super(world, WarTechEntityProfile.COMMAND_TRUCK);
        this.stepHeight = 1.1F;
    }

    public EntityWarTechGroundVehicle(World world, WarTechEntityProfile profile) {
        this(world);
        setProfile(profile);
        if (profile == WarTechEntityProfile.MOBILE_AIR_DEFENSE) {
            setLegacyFlags(getLegacyFlags() | 4);
        } else if (profile == WarTechEntityProfile.MOBILE_ARTILLERY) {
            setLegacyFlags(getLegacyFlags() & ~1
                    | TARGET_MOBS | TARGET_MACHINES);
            setLegacyFireMode(0);
        } else if (profile == WarTechEntityProfile.ELECTRONIC_WARFARE) {
            setLegacySelectedPayload(3);
        }
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(GUN_STATE, 1);
        dataManager.register(ARTILLERY_YAW, 0.0F);
        dataManager.register(ARTILLERY_PITCH, 0.0F);
        dataManager.register(ARTILLERY_AMMO_TYPE, -1);
        dataManager.register(ARTILLERY_AMMO_COUNT, 0);
        dataManager.register(ARTILLERY_CRANE, 0.0F);
        dataManager.register(ARTILLERY_RECOIL, 0.0F);
        dataManager.register(ARTILLERY_WHITELIST, "");
        dataManager.register(DRIVE_SPEED,0F);
        dataManager.register(DRIVE_STEERING,0F);
        dataManager.register(PARKING_BRAKE,false);
        dataManager.register(DRIVE_AMMO,0);
        dataManager.register(SHORAD_POSE,new NBTTagCompound());
    }

    @Override
    public void onUpdate() {
        if(world.isRemote && getProfile()==WarTechEntityProfile.MOBILE_AIR_DEFENSE) {
            previousShorad.read(shorad.write(),isDeployed());
            shorad.read(dataManager.get(SHORAD_POSE),isDeployed());
            if(ticksExisted<2) previousShorad.read(shorad.write(),isDeployed());
        }
        super.onUpdate();
        if (world.isRemote) {
            if(!localPrediction) updateClientInterpolation();
            previousCorrectionX=visualCorrectionX;previousCorrectionY=visualCorrectionY;previousCorrectionZ=visualCorrectionZ;
            previousCorrectionYaw=visualCorrectionYaw;previousCorrectionPitch=visualCorrectionPitch;
            visualCorrectionX*=.80;visualCorrectionY*=.80;visualCorrectionZ*=.80;visualCorrectionYaw*=.80F;visualCorrectionPitch*=.80F;
            wheelTravel+=getDriveSpeed();
        }
        if (world.isRemote
                && getProfile() == WarTechEntityProfile.MOBILE_AIR_DEFENSE) {
            updateClientGunState();
        } else if (world.isRemote
                && getProfile() == WarTechEntityProfile.MOBILE_ARTILLERY) {
            updateClientArtilleryState();
        }
    }

    @Override
    public WarTechEntityType getEntityType() {
        return WarTechEntityType.GROUND_VEHICLE;
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        if (getProfile() == WarTechEntityProfile.ELECTRONIC_WARFARE) {
            return distance < 65536.0D;
        }
        return super.isInRangeToRenderDist(distance);
    }

    @Override
    protected void serverTick(WarTechEntityProfile profile) {
        if (profile == WarTechEntityProfile.MOBILE_AIR_DEFENSE) {
            if (!isTor() && isDeployed() && isLegacyOperational() && shorad.deployment==120
                    && shoradTarget<0 && shorad.cap==0 && shorad.hold==0) {
                gunAimYaw=shorad.yaw;gunAimPitch=shorad.pitch;
                tickPantsirGuns();
            } else {
                resetGunTracking();
            }
            tickShorad();
        } else if (profile == WarTechEntityProfile.MOBILE_ARTILLERY) {
            tickMobileArtillery();
        }
        if(!VehicleChassisProfile.isDrivable(profile)) return;
        Entity passenger=getControllingPassenger();
        boolean driving=passenger instanceof EntityPlayer && !passenger.isDead;
        if(driving && !canManage((EntityPlayer)passenger))requestDriverExit();
        java.util.UUID uuid=driving?passenger.getUniqueID():null;
        if(!java.util.Objects.equals(uuid,inputDriver)) {
            inputDriver=uuid;driverSequence=-1;lastDriverInputTick=-1;driverForward=driverStrafe=0;driverBrake=true;exitRequested=false;
        }
        boolean brake=!driving || driverInputAge(ticksExisted,lastDriverInputTick)>10 || driverBrake || exitRequested;
        float throttle=brake?0:driverForward,turn=brake?0:driverStrafe;
        simulateDriving(throttle,turn,brake);
        dataManager.set(DRIVE_SPEED,(float)driveSpeed);dataManager.set(DRIVE_STEERING,(float)steeringState);
        dataManager.set(PARKING_BRAKE,parkingBrake);
        if(getProfile()==WarTechEntityProfile.MOBILE_AIR_DEFENSE)dataManager.set(DRIVE_AMMO,countLoadedInterceptors());
        if(exitRequested && driving && Math.abs(driveSpeed)<.008) trySafeExit((EntityPlayer)passenger);
        if(passenger instanceof EntityPlayerMP && ((EntityPlayerMP)passenger).connection!=null)
            WarTechNetwork.CHANNEL.sendTo(new VehicleStateMessage(this,driverSequence),(EntityPlayerMP)passenger);
    }

    public void simulateDriving(float throttle,float turn,boolean brake) {
        VehicleChassisProfile p=VehicleChassisProfile.of(this);
        if(isDeployed() || isShoradMotionBlocked()) { throttle=turn=0;brake=true;driveSpeed=0; }
        float oldYaw=rotationYaw;
        if(onGround && !brake && !parkingBrake && !isDeployed() && !isShoradMotionBlocked())driveSpeed+=Math.sin(Math.toRadians(rotationPitch))*.004;
        GroundVehiclePhysics.Motion next=GroundVehiclePhysics.step(p,driveSpeed,steeringState,
                rotationYaw,throttle,turn,brake,parkingBrake,onGround);
        next=GroundVehiclePhysics.withGrip(next,motionX,motionZ,p,isInWater()?.15:.55);
        driveSpeed=next.speed;steeringState=next.steering;
        motionX=next.x;motionZ=next.z;
        motionY=Math.max(-1.2,motionY-.08);
        double oldHeight=posY;
        // Vanilla resolves vertical ground/slab contact. Horizontal and rotational
        // sweeps below use the entire chassis rather than a square at its centre.
        move(net.minecraft.entity.MoverType.SELF,0,motionY,0);
        if(!onGround && motionY<=0)retainHullSupport(p,oldHeight);
        if(onGround)motionY=0;else motionY*=.98;
        int steps=Math.max(1,Math.min(8,(int)Math.ceil((Math.abs(next.speed)
                +Math.abs(next.yaw-oldYaw)*Math.PI/180*p.length/2)/.12)));
        boolean blocked=false;
        for(int i=0;i<steps;i++) {
            double x=posX+motionX/steps,z=posZ+motionZ/steps;
            float yaw=rotationYaw+(next.yaw-oldYaw)/steps;
            if(VehicleCollisionHull.clear(this,x,posY,z,yaw)) { setPosition(x,posY,z);rotationYaw=yaw;continue; }
            boolean climbed=false;
            if(onGround && Math.abs(next.speed)>.004) for(double lift:new double[]{.5,1.0}) {
                if(lift>stepHeight)break;
                if(VehicleCollisionHull.clear(this,posX,posY+lift,posZ,rotationYaw)
                        && VehicleCollisionHull.clear(this,x,posY+lift,z,yaw)) {
                    setPosition(x,posY+lift,z);rotationYaw=yaw;motionY=0;
                    if(world.isRemote)visualCorrectionY-=lift;
                    climbed=true;break;
                }
            }
            if(!climbed) {blocked=true;break;}
        }
        collidedHorizontally=blocked;
        if(blocked) {driveSpeed=0;motionX=motionZ=0;}
        updateTerrainPitch(p);
    }

    private void retainHullSupport(VehicleChassisProfile p,double previousHeight) {
        AxisAlignedBB footprint=VehicleCollisionHull.bounds(p,posX,previousHeight-.25,posZ,rotationYaw,.6);
        double top=Double.NEGATIVE_INFINITY;
        for(AxisAlignedBB box:world.getCollisionBoxes(null,footprint)) {
            if(box.maxY>previousHeight+.02 || box.maxY<posY-.12)continue;
            if(VehicleCollisionHull.overlaps(p,posX,box.minY-.06,posZ,rotationYaw,box.maxY-box.minY+.1,box))top=Math.max(top,box.maxY);
        }
        if(top>=posY && VehicleCollisionHull.clear(this,posX,top,posZ,rotationYaw)) {
            setPosition(posX,top,posZ);onGround=true;motionY=0;
        }
    }

    private void updateTerrainPitch(VehicleChassisProfile p) {
        if(isDeployed() || isShoradMotionBlocked()) {rotationPitch=0;return;}
        double front=0,back=0;int nf=0,nb=0;
        for(int end:new int[]{-1,1}) for(int side:new int[]{-1,1}) {
            Vec3d offset=p.offset(side*p.width*.34,0,end*p.wheelbase*.45,rotationYaw,0);
            Vec3d top=new Vec3d(posX+offset.x,posY+1.15,posZ+offset.z);
            net.minecraft.util.math.RayTraceResult hit=world.rayTraceBlocks(top,top.addVector(0,-2.4,0),false,true,false);
            if(hit!=null && hit.typeOfHit==net.minecraft.util.math.RayTraceResult.Type.BLOCK) {
                if(end==1){front+=hit.hitVec.y;nf++;}else{back+=hit.hitVec.y;nb++;}
            }
        }
        float target=nf>0 && nb>0?(float)MathHelper.clamp(Math.toDegrees(Math.atan2(back/nb-front/nf,p.wheelbase*.9)),-12,12):0;
        rotationPitch+=(target-rotationPitch)*.18F;
    }

    public double getDriveSpeed() { return world.isRemote&&!localPrediction?dataManager.get(DRIVE_SPEED):driveSpeed; }
    public double getSteeringState() { return world.isRemote&&!localPrediction?dataManager.get(DRIVE_STEERING):steeringState; }
    public boolean isParkingBrake() {return world.isRemote&&!localPrediction?dataManager.get(PARKING_BRAKE):parkingBrake;}
    public int getDriveAmmo() {return dataManager.get(DRIVE_AMMO);}
    public double getWheelTravel(float partial) {return wheelTravel+getDriveSpeed()*partial;}
    public void setLocalPrediction(boolean value) {localPrediction=value;}
    public boolean isLocalPrediction() {return localPrediction;}
    public int getPredictionEpoch() {return predictionEpoch;}
    public void predictDriverInput(float throttle,float turn,int flags) {
        if(!world.isRemote)return;
        if((flags&2)!=0)parkingBrake=!parkingBrake;
        simulateDriving(throttle,turn,(flags&1)!=0);
    }
    public void restoreDrivingState(VehicleStateMessage state) {
        setPosition(state.x,state.y,state.z);rotationYaw=state.yaw;rotationPitch=state.pitch;
        motionX=state.motionX;motionY=state.motionY;motionZ=state.motionZ;
        driveSpeed=state.speed;steeringState=state.steering;parkingBrake=state.parking;onGround=state.grounded;
    }
    public void correctVisualPose(double oldX,double oldY,double oldZ,float oldYaw) {
        correctVisualPose(visualPosition(0),new Vec3d(oldX,oldY,oldZ),visualYaw(0),oldYaw,visualPitch(0),visualPitch(1));
    }
    public void correctVisualPose(Vec3d start,Vec3d end,float startYaw,float endYaw,float startPitch,float endPitch) {
        double distance=distanceSq(end.x,end.z,posX,posZ)+(end.y-posY)*(end.y-posY);
        if(distance<4 && !collidedHorizontally) {
            visualCorrectionX=end.x-posX;visualCorrectionY=end.y-posY;visualCorrectionZ=end.z-posZ;
            visualCorrectionYaw=MathHelper.wrapDegrees(endYaw-rotationYaw);visualCorrectionPitch=endPitch-rotationPitch;
            // Preserve BOTH interpolation endpoints. Resetting prevPos to pos
            // made every acknowledgement jump to the end of the current tick.
            prevPosX=lastTickPosX=posX-(end.x-start.x);
            prevPosY=lastTickPosY=posY-(end.y-start.y);
            prevPosZ=lastTickPosZ=posZ-(end.z-start.z);
            prevRotationYaw=rotationYaw-MathHelper.wrapDegrees(endYaw-startYaw);
            prevRotationPitch=rotationPitch-(endPitch-startPitch);
        } else {
            resetVisualCorrections();prevPosX=lastTickPosX=posX;prevPosY=lastTickPosY=posY;prevPosZ=lastTickPosZ=posZ;
            prevRotationYaw=rotationYaw;prevRotationPitch=rotationPitch;
        }
        previousCorrectionX=visualCorrectionX;previousCorrectionY=visualCorrectionY;previousCorrectionZ=visualCorrectionZ;
        previousCorrectionYaw=visualCorrectionYaw;previousCorrectionPitch=visualCorrectionPitch;
    }
    private void resetVisualCorrections() {
        visualCorrectionX=visualCorrectionY=visualCorrectionZ=previousCorrectionX=previousCorrectionY=previousCorrectionZ=0;
        visualCorrectionYaw=previousCorrectionYaw=visualCorrectionPitch=previousCorrectionPitch=0;
    }
    public Vec3d visualPosition(float partial) {
        return new Vec3d(prevPosX+(posX-prevPosX)*partial+previousCorrectionX+(visualCorrectionX-previousCorrectionX)*partial,
                prevPosY+(posY-prevPosY)*partial+previousCorrectionY+(visualCorrectionY-previousCorrectionY)*partial,
                prevPosZ+(posZ-prevPosZ)*partial+previousCorrectionZ+(visualCorrectionZ-previousCorrectionZ)*partial);
    }
    public float visualYaw(float partial) {return prevRotationYaw+MathHelper.wrapDegrees(rotationYaw-prevRotationYaw)*partial+previousCorrectionYaw+(visualCorrectionYaw-previousCorrectionYaw)*partial;}
    public float visualPitch(float partial) {return prevRotationPitch+(rotationPitch-prevRotationPitch)*partial+previousCorrectionPitch+(visualCorrectionPitch-previousCorrectionPitch)*partial;}

    private static double distanceSq(double startX, double startZ,
            double endX, double endZ) {
        double deltaX = endX - startX;
        double deltaZ = endZ - startZ;
        return deltaX * deltaX + deltaZ * deltaZ;
    }

    private void updateClientInterpolation() {
        if (clientInterpolationTicks <= 0) {
            return;
        }
        double blend = 1.0D / clientInterpolationTicks;
        double x = posX + (clientTargetX - posX) * blend;
        double y = posY + (clientTargetY - posY) * blend;
        double z = posZ + (clientTargetZ - posZ) * blend;
        float yawDelta = MathHelper.wrapDegrees(
                clientTargetYaw - rotationYaw);
        rotationYaw += yawDelta * blend;
        rotationPitch += (clientTargetPitch - rotationPitch) * blend;
        setPosition(x, y, z);
        --clientInterpolationTicks;
    }

    public void acceptServerVehicleState(double x, double y, double z,
            double serverMotionX, double serverMotionY,
            double serverMotionZ, float yaw, float pitch) {
        if (!world.isRemote) {
            return;
        }
        setPosition(x, y, z);
        rotationYaw = yaw;
        rotationPitch = pitch;
        clientTargetX = x;
        clientTargetY = y;
        clientTargetZ = z;
        clientTargetYaw = yaw;
        clientTargetPitch = pitch;
        clientInterpolationTicks = 0;
        motionX = serverMotionX;
        motionY = serverMotionY;
        motionZ = serverMotionZ;
    }

    @Override
    public void setPositionAndRotationDirect(double x, double y, double z,
            float yaw, float pitch, int positionIncrements,
            boolean teleport) {
        if (!world.isRemote) {
            super.setPositionAndRotationDirect(x, y, z, yaw, pitch,
                    positionIncrements, teleport);
            return;
        }
        if(localPrediction && !teleport)return;
        if(localPrediction && teleport) {
            ++predictionEpoch;setPosition(x,y,z);rotationYaw=prevRotationYaw=yaw;rotationPitch=prevRotationPitch=pitch;
            prevPosX=lastTickPosX=x;prevPosY=lastTickPosY=y;prevPosZ=lastTickPosZ=z;
            resetVisualCorrections();
            motionX=motionY=motionZ=driveSpeed=0;clientInterpolationTicks=0;return;
        }
        clientTargetX = x;
        clientTargetY = y;
        clientTargetZ = z;
        clientTargetYaw = yaw;
        clientTargetPitch = pitch;
        clientInterpolationTicks = Math.max(3, positionIncrements);
    }

    private void playLegacyDriveSound(WarTechEntityProfile profile) {
        float volume;
        float pitch;
        if (profile == WarTechEntityProfile.COMMAND_TRUCK) {
            volume = 0.28F;
            pitch = (float) (0.82D
                    + Math.min(0.35D, Math.abs(driveSpeed)));
        } else if (profile == WarTechEntityProfile.MOBILE_ARTILLERY) {
            volume = 0.3F;
            pitch = (float) (0.8D
                    + Math.min(0.38D, Math.abs(driveSpeed)));
        } else {
            volume = 0.3F;
            pitch = (float) (0.78D
                    + Math.min(0.35D, Math.abs(driveSpeed)));
        }
        world.playSound(null, posX, posY, posZ,
                SoundEvents.ENTITY_MINECART_RIDING,
                SoundCategory.NEUTRAL, volume, pitch);
    }

    public int getPantsirGunRounds() {
        return isTor() ? 0
                : PantsirAmmoBeltItem.getRounds(getStackInSlot(13));
    }

    public float getRenderArtilleryYaw(float partialTicks) {
        float delta = wrapDegrees(
                clientArtilleryYaw - clientPreviousArtilleryYaw);
        return wrapDegrees(clientPreviousArtilleryYaw
                + delta * partialTicks);
    }

    public float getRenderArtilleryPitch(float partialTicks) {
        return clientPreviousArtilleryPitch
                + (clientArtilleryPitch - clientPreviousArtilleryPitch)
                        * partialTicks;
    }

    public int getArtilleryAmmoType() {
        return dataManager.get(ARTILLERY_AMMO_TYPE);
    }
    public NBTTagCompound productionChassis(){
        NBTTagCompound n=new NBTTagCompound();
        if(getProfile()==WarTechEntityProfile.MOBILE_ARTILLERY && getVisualVariant()==2){n.setInteger("ArtilleryAmmoType",dataManager.get(ARTILLERY_AMMO_TYPE));n.setInteger("ArtilleryAmmoCount",dataManager.get(ARTILLERY_AMMO_COUNT));}
        return n;
    }
    public void restoreProductionChassis(NBTTagCompound n){
        if(getProfile()!=WarTechEntityProfile.MOBILE_ARTILLERY || getVisualVariant()!=2)return;
        int type=n.getInteger("ArtilleryAmmoType");int count=type>=0 && type<8?MathHelper.clamp(n.getInteger("ArtilleryAmmoCount"),0,HimarsAmmoItem.rackCapacity(type)):0;
        dataManager.set(ARTILLERY_AMMO_TYPE,count>0?type:-1);dataManager.set(ARTILLERY_AMMO_COUNT,count);
    }

    public int getArtilleryAmmoCount() {
        if (getVisualVariant() == 2) {
            return dataManager.get(ARTILLERY_AMMO_COUNT);
        }
        int count = 0;
        for (int slot = 1; slot < 10; ++slot) {
            ItemStack stack = getStackInSlot(slot);
            if (!stack.isEmpty()
                    && stack.getItem() == WarTechContent.ARTILLERY_AMMO) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public float getArtilleryCrane() {
        return dataManager.get(ARTILLERY_CRANE);
    }

    public float getRenderArtilleryCrane(float partialTicks) {
        return clientPreviousArtilleryCrane
                + (clientArtilleryCrane - clientPreviousArtilleryCrane)
                        * partialTicks;
    }

    public float getRenderArtilleryRecoil(float partialTicks) {
        return clientPreviousArtilleryRecoil
                + (clientArtilleryRecoil - clientPreviousArtilleryRecoil)
                        * partialTicks;
    }

    public boolean targetsArtilleryPlayers() {
        return (getLegacyFlags() & TARGET_PLAYERS) != 0;
    }

    public boolean targetsArtilleryAnimals() {
        return (getLegacyFlags() & TARGET_ANIMALS) != 0;
    }

    public boolean targetsArtilleryMobs() {
        return (getLegacyFlags() & TARGET_MOBS) != 0;
    }

    public boolean targetsArtilleryMachines() {
        return (getLegacyFlags() & TARGET_MACHINES) != 0;
    }

    public List<String> getArtilleryWhitelist() {
        if (!world.isRemote && getProfile()
                == WarTechEntityProfile.MOBILE_ARTILLERY) {
            ItemStack chip = getStackInSlot(0);
            if (isTurretChip(chip)) {
                NBTTagCompound tag = chip.getTagCompound();
                String packed = tag == null ? ""
                        : tag.getString("WarTechTurretNames");
                if (!packed.equals(dataManager.get(ARTILLERY_WHITELIST))) {
                    dataManager.set(ARTILLERY_WHITELIST, packed);
                }
            } else if (!dataManager.get(ARTILLERY_WHITELIST).isEmpty()) {
                dataManager.set(ARTILLERY_WHITELIST, "");
            }
        }
        String packed = dataManager.get(ARTILLERY_WHITELIST);
        if (packed.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String name : packed.split("\\n")) {
            if (!name.isEmpty()) {
                result.add(name);
            }
        }
        return result;
    }

    public void addArtilleryWhitelist(String value) {
        ItemStack chip = getStackInSlot(0);
        if (!isTurretChip(chip)) {
            return;
        }
        String sanitized = sanitizeWhitelistName(value);
        if (sanitized.isEmpty()) {
            return;
        }
        List<String> names = new ArrayList<>(getArtilleryWhitelist());
        if (!names.contains(sanitized) && names.size() < 32) {
            names.add(sanitized);
            setArtilleryWhitelist(names);
        }
    }

    public void removeArtilleryWhitelist(int index) {
        ItemStack chip = getStackInSlot(0);
        if (!isTurretChip(chip)) {
            return;
        }
        List<String> names = new ArrayList<>(getArtilleryWhitelist());
        if (index >= 0 && index < names.size()) {
            names.remove(index);
            setArtilleryWhitelist(names);
        }
    }

    private void setArtilleryWhitelist(List<String> names) {
        String packed = String.join("\n", names);
        ItemStack chip = getStackInSlot(0);
        if (isTurretChip(chip)) {
            NBTTagCompound tag = chip.getTagCompound();
            if (tag == null) {
                tag = new NBTTagCompound();
                chip.setTagCompound(tag);
            }
            tag.setString("WarTechTurretNames", packed);
        }
        dataManager.set(ARTILLERY_WHITELIST, packed);
    }

    private static boolean isTurretChip(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem().getRegistryName() != null
                && "hbm:turret_chip".equals(
                        stack.getItem().getRegistryName().toString());
    }

    private static String sanitizeWhitelistName(String value) {
        if (value == null) {
            return "";
        }
        String sanitized = value.replace('\n', ' ').replace('\r', ' ').trim();
        return sanitized.length() > 25
                ? sanitized.substring(0, 25) : sanitized;
    }

    private void tickMobileArtillery() {
        float recoil = dataManager.get(ARTILLERY_RECOIL);
        if (recoil > 0.0F) {
            dataManager.set(ARTILLERY_RECOIL,
                    Math.max(0.0F, recoil - 0.05F));
        }
        if (!isDeployed() || getVisualVariant() == 0) {
            setLegacyOperational(false);
            artilleryTargetId = -1;
            dataManager.set(ARTILLERY_YAW, rotationYaw);
            dataManager.set(ARTILLERY_PITCH, 0.0F);
            return;
        }
        boolean active = isLegacyEnabled() && getLegacyPower() >= 100;
        setLegacyOperational(active);
        if (!active) {
            artilleryTargetId = -1;
            artilleryTargetQueue.clear();
            clearGuidanceTarget();
            return;
        }
        setLegacyPower(getLegacyPower() - 100);

        if (getVisualVariant() == 2 && tickHenryReload()) {
            return;
        }

        Entity target = resolveArtilleryTarget();
        boolean manual = isArtilleryManualMode();
        if (manual && !hasGuidanceTarget()
                && !artilleryTargetQueue.isEmpty()) {
            Vec3d queued = artilleryTargetQueue.get(0);
            setGuidanceTarget(queued.x, queued.y, queued.z);
        }
        if (!manual && target != null) {
            setGuidanceTarget(target.posX,
                    target.posY + target.height * 0.5D - target.getYOffset(),
                    target.posZ);
        }
        if (!hasGuidanceTarget()) {
            return;
        }

        double turretY = posY + (getVisualVariant() == 1 ? 2.25D : 2.2D)*VehicleDimensions.scale(getProfile());
        double dx = getTargetX() - posX;
        double dy = getTargetY() - turretY;
        double dz = getTargetZ() - posZ;
        double horizontal = Math.max(0.001D,
                Math.sqrt(dx * dx + dz * dz));
        double allowedRange=com.wartec.wartecmod.port.integration.WeaponBalance.artilleryRange(getVisualVariant(),getLegacyFireMode()==1);
        if(horizontal>allowedRange || manual && horizontal<250) return;
        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float targetPitch;
        if (getVisualVariant() == 1) {
            double speed = getLegacyFireMode() == 1 ? 20.0D : 50.0D;
            targetPitch = (float) Math.toDegrees(ballisticAngle(
                    horizontal, dy, speed, 0.4905D,
                    getLegacyFireMode() != 1));
        } else {
            targetPitch = 45.0F;
        }
        float yaw = approachAngle(dataManager.get(ARTILLERY_YAW),
                targetYaw, 1.0F);
        float pitch = approach(dataManager.get(ARTILLERY_PITCH),
                targetPitch, 0.5F);
        dataManager.set(ARTILLERY_YAW, yaw);
        dataManager.set(ARTILLERY_PITCH, pitch);
        float tolerance = getVisualVariant() == 2 ? 5.0F : 0.55F;
        boolean aligned = Math.abs(wrapDegrees(targetYaw - yaw)) <= tolerance
                && Math.abs(targetPitch - pitch) <= tolerance;
        if (!aligned) {
            return;
        }
        ++artilleryFireTimer;
        int delay = getVisualVariant() == 1
                && getLegacyFireMode() == 0 ? 300 : 40;
        if (artilleryFireTimer % delay == 0) {
            boolean fired;
            if (getVisualVariant() == 1) {
                fired = fireGregArtillery();
            } else {
                fired = fireHenryArtillery();
            }
            if (fired && manual && !artilleryTargetQueue.isEmpty()) {
                artilleryTargetQueue.remove(0);
                clearGuidanceTarget();
            }
        }
    }

    private boolean tickHenryReload() {
        int rounds = dataManager.get(ARTILLERY_AMMO_COUNT);
        float crane = dataManager.get(ARTILLERY_CRANE);
        if (rounds > 0 && crane <= 0.0F) {
            return false;
        }
        float desiredPitch = 0.0F;
        dataManager.set(ARTILLERY_PITCH, approach(
                dataManager.get(ARTILLERY_PITCH), desiredPitch, 0.5F));
        if (Math.abs(dataManager.get(ARTILLERY_PITCH)) > 0.55F) {
            return true;
        }
        if (rounds > 0) {
            crane = Math.max(0.0F, crane - 0.0125F);
        } else {
            crane = Math.min(1.0F, crane + 0.0125F);
            if (crane >= 1.0F) {
                int slot = firstAmmoSlot(WarTechContent.HIMARS_AMMO);
                if (slot >= 0) {
                    ItemStack stack = getStackInSlot(slot);
                    int type = MathHelper.clamp(stack.getMetadata(), 0, 7);
                    dataManager.set(ARTILLERY_AMMO_TYPE, type);
                    dataManager.set(ARTILLERY_AMMO_COUNT,
                            HimarsAmmoItem.rackCapacity(type));
                    decrStackSize(slot, 1);
                }
            }
        }
        dataManager.set(ARTILLERY_CRANE, crane);
        return true;
    }

    private Entity resolveArtilleryTarget() {
        if (isArtilleryManualMode()) {
            artilleryTargetId = -1;
            return null;
        }
        Entity current = artilleryTargetId < 0
                ? null : world.getEntityByID(artilleryTargetId);
        if (isValidArtilleryTarget(current)) {
            return current;
        }
        artilleryTargetId = -1;
        clearGuidanceTarget();
        if (--artillerySearchTimer > 0) {
            return null;
        }
        artillerySearchTimer = getVisualVariant() == 1
                && getLegacyFireMode() == 1 ? 20 : 200;
        double range = com.wartec.wartecmod.port.integration.WeaponBalance.artilleryRange(getVisualVariant(),getLegacyFireMode()==1);
        double bestDistance = range * range;
        Entity best = null;
        for (Entity entity : world.loadedEntityList) {
            if (!isValidArtilleryTarget(entity)) {
                continue;
            }
            double distance = getDistanceSq(entity);
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = entity;
            }
        }
        artilleryTargetId = best == null ? -1 : best.getEntityId();
        return best;
    }

    private boolean isValidArtilleryTarget(Entity entity) {
        if (entity == null || entity == this || entity.isDead
                || isFriendlyOrOwner(entity)
                || entity instanceof EntityWarTechArtilleryProjectile) {
            return false;
        }
        double range = com.wartec.wartecmod.port.integration.WeaponBalance.artilleryRange(getVisualVariant(),getLegacyFireMode()==1);
        double grace = getVisualVariant() == 1
                && getLegacyFireMode() == 1 ? 32.0D : 250.0D;
        double distanceSq = getDistanceSq(entity);
        if (distanceSq < grace * grace || distanceSq > range * range * 1.21D) {
            return false;
        }
        String name = entity instanceof EntityPlayer
                ? entity.getName()
                : entity instanceof EntityLiving
                        ? ((EntityLiving) entity).getCustomNameTag() : "";
        if (!name.isEmpty() && getArtilleryWhitelist().contains(name)) {
            return false;
        }
        boolean category = targetsArtilleryPlayers()
                && entity instanceof EntityPlayer
                || targetsArtilleryAnimals() && entity instanceof IAnimals
                || targetsArtilleryMobs() && entity instanceof IMob
                || targetsArtilleryMachines()
                        && (entity instanceof IRadarDetectable
                                || entity.getClass().getName()
                                        .startsWith("com.hbm.entity.")
                                && !(entity instanceof EntityLiving));
        if (!category) {
            return false;
        }
        if (getVisualVariant() == 1 && getLegacyFireMode() == 1) {
            Vec3d start = new Vec3d(posX, posY + 3.0D, posZ);
            Vec3d end = new Vec3d(entity.posX,
                    entity.posY + entity.height * 0.5D, entity.posZ);
            return world.rayTraceBlocks(start, end,
                    false, true, false) == null;
        }
        return true;
    }

    private boolean isArtilleryManualMode() {
        return getVisualVariant() == 1
                ? getLegacyFireMode() == 2 : getLegacyFireMode() == 1;
    }

    private boolean fireGregArtillery() {
        int slot = firstAmmoSlot(WarTechContent.ARTILLERY_AMMO);
        if (slot < 0) {
            return false;
        }
        ItemStack source = getStackInSlot(slot);
        ItemStack ammunition = source.copy();
        ammunition.setCount(1);
        double speed = getLegacyFireMode() == 1 ? 20.0D : 50.0D;
        float yaw = dataManager.get(ARTILLERY_YAW);
        float pitch = dataManager.get(ARTILLERY_PITCH);
        EntityWarTechArtilleryProjectile projectile =
                createArtilleryProjectile(
                        EntityWarTechArtilleryProjectile.KIND_GREG,
                        ammunition, 2.15D, yaw, pitch, speed);
        projectile.setWhistle(getLegacyFireMode() != 1);
        if (!MissileChunkLoader.prepare(projectile)) return false;
        if (com.wartec.wartecmod.port.integration.MissileChunkLoader.spawnFlight(projectile)) {
            decrStackSize(slot, 1);
            MissileTrackingService.assignProjectileTeam(
                    projectile, getOwnerTeam());
            playArtillerySound("hbm:turret.jeremy_fire", 25.0F, 1.0F);
            spawnArtilleryMuzzleSmoke(2.15D, yaw);
            dataManager.set(ARTILLERY_RECOIL, 1.0F);
            return true;
        }
        MissileChunkLoader.untrack(projectile);
        return false;
    }

    private boolean fireHenryArtillery() {
        int rounds = dataManager.get(ARTILLERY_AMMO_COUNT);
        int type = dataManager.get(ARTILLERY_AMMO_TYPE);
        if (rounds <= 0 || type < 0) {
            return false;
        }
        ItemStack ammunition = new ItemStack(
                WarTechContent.HIMARS_AMMO, 1, type);
        float yaw = dataManager.get(ARTILLERY_YAW);
        float pitch = dataManager.get(ARTILLERY_PITCH);
        EntityWarTechArtilleryProjectile projectile =
                createArtilleryProjectile(
                        EntityWarTechArtilleryProjectile.KIND_HENRY,
                        ammunition, 1.45D, yaw, pitch, 25.0D);
        if (!MissileChunkLoader.prepare(projectile)) return false;
        if (com.wartec.wartecmod.port.integration.MissileChunkLoader.spawnFlight(projectile)) {
            dataManager.set(ARTILLERY_AMMO_COUNT, rounds - 1);
            if (rounds - 1 <= 0) {
                dataManager.set(ARTILLERY_AMMO_TYPE, -1);
            }
            MissileTrackingService.assignProjectileTeam(
                    projectile, getOwnerTeam());
            playArtillerySound("hbm:weapon.rocketFlame", 25.0F, 1.0F);
            spawnArtilleryMuzzleSmoke(1.45D, yaw);
            return true;
        }
        MissileChunkLoader.untrack(projectile);
        return false;
    }

    private EntityWarTechArtilleryProjectile createArtilleryProjectile(
            int kind, ItemStack ammunition, double muzzleDistance,
            float yaw, float pitch, double speed) {
        double yawRadians = Math.toRadians(yaw);
        double pitchRadians = Math.toRadians(pitch);
        double horizontal = Math.cos(pitchRadians);
        double forwardX = -Math.sin(yawRadians);
        double forwardZ = Math.cos(yawRadians);
        double modelScale=VehicleDimensions.scale(getProfile());
        EntityWarTechArtilleryProjectile projectile =
                new EntityWarTechArtilleryProjectile(world);
        projectile.configure(kind, ammunition,
                getTargetX(), getTargetY(), getTargetZ());
        projectile.setPosition(
                posX + forwardX * muzzleDistance*modelScale,
                posY + (kind == EntityWarTechArtilleryProjectile.KIND_GREG
                        ? 2.25D : 2.2D)*modelScale,
                posZ + forwardZ * muzzleDistance*modelScale);
        projectile.setOwnerIdentity(getOwnerUuid(), getOwnerTeam());
        projectile.motionX = forwardX * horizontal * speed;
        projectile.motionY = Math.sin(pitchRadians) * speed;
        projectile.motionZ = forwardZ * horizontal * speed;
        return projectile;
    }

    private int firstAmmoSlot(net.minecraft.item.Item item) {
        for (int slot = 1; slot < 10; ++slot) {
            ItemStack stack = getStackInSlot(slot);
            if (!stack.isEmpty() && stack.getItem() == item) {
                return slot;
            }
        }
        return -1;
    }

    private void spawnArtilleryMuzzleSmoke(double distance, float yaw) {
        if (!(world instanceof WorldServer)) {
            return;
        }
        double radians = Math.toRadians(yaw);
        distance*=VehicleDimensions.scale(getProfile());
        double x = posX - Math.sin(radians) * distance;
        double z = posZ + Math.cos(radians) * distance;
        double y = posY + (getVisualVariant() == 1 ? 2.25D : 2.2D)*VehicleDimensions.scale(getProfile());
        HbmExplosionCompat.spawnLegacyLargeExplosion(
                world, x, y, z, 5);
    }

    private void playArtillerySound(String id, float volume, float pitch) {
        SoundEvent sound = SoundEvent.REGISTRY.getObject(
                new ResourceLocation(id));
        if (sound != null) {
            world.playSound(null, posX, posY, posZ, sound,
                    SoundCategory.HOSTILE, volume, pitch);
        }
    }

    private static double ballisticAngle(double horizontal,
            double vertical, double speed, double gravity,
            boolean highArc) {
        if (horizontal < 0.001D) {
            return Math.toRadians(81.0D);
        }
        double squareSpeed = speed * speed;
        double discriminant = squareSpeed * squareSpeed
                - gravity * (gravity * horizontal * horizontal
                        + 2.0D * vertical * squareSpeed);
        if (discriminant < 0.0D) {
            return Math.toRadians(45.0D);
        }
        double root = Math.sqrt(discriminant);
        return Math.atan((squareSpeed + (highArc ? root : -root))
                / (gravity * horizontal));
    }

    private void updateClientArtilleryState() {
        clientPreviousArtilleryYaw = clientArtilleryYaw;
        clientPreviousArtilleryPitch = clientArtilleryPitch;
        clientPreviousArtilleryCrane = clientArtilleryCrane;
        clientPreviousArtilleryRecoil = clientArtilleryRecoil;
        clientArtilleryYaw = dataManager.get(ARTILLERY_YAW);
        clientArtilleryPitch = dataManager.get(ARTILLERY_PITCH);
        clientArtilleryCrane = dataManager.get(ARTILLERY_CRANE);
        clientArtilleryRecoil = dataManager.get(ARTILLERY_RECOIL);
        if (ticksExisted <= 2) {
            clientPreviousArtilleryYaw = clientArtilleryYaw;
            clientPreviousArtilleryPitch = clientArtilleryPitch;
            clientPreviousArtilleryCrane = clientArtilleryCrane;
            clientPreviousArtilleryRecoil = clientArtilleryRecoil;
        }
    }

    public boolean isPantsirGunsFiring() {
        return !isTor() && (dataManager.get(GUN_STATE) & 2) != 0;
    }

    public float getPantsirGunAimYaw() {
        int packed = dataManager.get(GUN_STATE) >>> 13 & 0x1FFF;
        return (float) (packed >= 4096 ? packed - 8192 : packed) / 10.0F;
    }

    public float getPantsirGunAimPitch() {
        int packed = dataManager.get(GUN_STATE) >>> 2 & 0x7FF;
        return (float) (packed >= 1024 ? packed - 2048 : packed) / 10.0F;
    }

    public float getRenderPantsirGunAimYaw(float partialTicks) {
        float delta = wrapDegrees(clientGunYaw - clientPreviousGunYaw);
        return wrapDegrees(clientPreviousGunYaw + delta * partialTicks);
    }

    public float getRenderPantsirGunAimPitch(float partialTicks) {
        return clientPreviousGunPitch
                + (clientGunPitch - clientPreviousGunPitch) * partialTicks;
    }

    public float getShoradDeployment(float partial) {return (previousShorad.deployment+(shorad.deployment-previousShorad.deployment)*partial)/120F;}
    public float getShoradYaw(float partial) {return interpolateAngle(previousShorad.yaw,shorad.yaw,partial);}
    public float getShoradPitch(float partial) {return previousShorad.pitch+(shorad.pitch-previousShorad.pitch)*partial;}
    public float getShoradRadar(float partial) {return interpolateAngle(previousShorad.radar,shorad.radar,partial);}
    public float getShoradCap(float partial) {return (previousShorad.cap+(shorad.cap-previousShorad.cap)*partial)/12F;}
    public int getShoradSlot() {return shorad.slot;}
    @Override public net.minecraft.nbt.NBTTagCompound commandDefenseTelemetry(){net.minecraft.nbt.NBTTagCompound n=super.commandDefenseTelemetry();if(getProfile()==WarTechEntityProfile.MOBILE_AIR_DEFENSE){n.setInteger("Pending",shoradTarget);n.setInteger("Delay",24+12-shorad.cap);n.setString("Stage",shorad.hold>0?"exit":shorad.cap>0?"opening":shoradTarget>=0?"tracking":"guard");n.setBoolean("Deployed",getShoradDeployment(1)>=1);}return n;}
    public boolean isShoradMotionBlocked() {return getProfile()==WarTechEntityProfile.MOBILE_AIR_DEFENSE && shorad.blocksDriving();}
    private static float interpolateAngle(float a,float b,float t) {return LegacySamGeometry.wrap(a+LegacySamGeometry.wrap(b-a)*t);}
    @Override protected void launchLegacyInterceptor(Entity target) {
        if(getProfile()!=WarTechEntityProfile.MOBILE_AIR_DEFENSE) {super.launchLegacyInterceptor(target);return;}
        if(!isDeployed() || shorad.cap>0 || shorad.hold>0 || shoradTarget>=0)return;
        int slot=-1;
        for(int i=0;i<(isTor()?8:12);i++) if(isRequiredInterceptor(getStackInSlot(i))) {slot=i;break;}
        if(slot>=0) {shorad.slot=slot;shoradTarget=target.getEntityId();}
    }
    private void tickShorad() {
        previousShorad.read(shorad.write(),isDeployed());
        Vec3d sensor=getPositionVector().addVector(0,3,0);
        int tier=isTor()?2:1;
        if(isDeployed() && isLegacyOperational() && getLegacyFireMode()!=0 && shorad.cap==0 && shorad.hold==0) {
            Entity tracked=legacyLaunchCooldown==0?MissileTrackingService.findLaunchThreat(world,sensor,tier,getDefenseEngagementRange(),getLauncherKey(),getOwnerTeam(),false,36,null):null;
            if(tracked==null)tracked=MissileTrackingService.findTrackingThreat(world,sensor,tier,getDefenseEngagementRange(),getLauncherKey(),getOwnerTeam(),shoradTarget,false);
            if(tracked!=null) {
                int slot=-1;for(int i=0;i<(isTor()?8:12);i++) if(isRequiredInterceptor(getStackInSlot(i))) {slot=i;break;}
                shoradTarget=slot<0?-1:tracked.getEntityId();if(slot>=0)shorad.slot=slot;
            } else shoradTarget=-1;
        }
        Entity target=shoradTarget<0?null:world.getEntityByID(shoradTarget);
        boolean tracking=target!=null && !target.isDead && isDeployed() && isLegacyOperational()
                && getLegacyFireMode()!=0 && MissileTrackingService.getThreatTier(target)<=tier
                && MissileTrackingService.hasDefenseContact(world,target,sensor,getOwnerTeam(),false);
        if(!tracking) {shoradTarget=-1;target=null;}
        boolean request=tracking && legacyLaunchCooldown==0 && getLegacyPower()>=50000 && shorad.slot>=0
                && isRequiredInterceptor(getStackInSlot(shorad.slot))
                && com.wartec.wartecmod.port.integration.AirDefenseFireControl.canLaunch(target,sensor,getDefenseEngagementRange(),tier,24+(12-shorad.cap));
        Vec3d aim=tracking?com.wartec.wartecmod.port.integration.AirDefenseFireControl.lead(target,sensor,12-shorad.cap,tier):sensor;
        float yaw=tracking?ShoradGeometry.desiredYaw(aim,getPositionVector(),rotationYaw):gunAimYaw;
        double horizontal=tracking?Math.hypot(aim.x-posX,aim.z-posZ):1;
        float pitch=tracking?(float)Math.toDegrees(Math.atan2(aim.y-(posY+2.625*VehicleDimensions.scale(getProfile())),Math.max(.001,horizontal)))-3:gunAimPitch;
        pitch=MathHelper.clamp(pitch,0,65);
        boolean release=shorad.tick(isDeployed(),yaw,pitch,request,isTor());
        if(release && request) releaseShorad(target);
        dataManager.set(SHORAD_POSE,shorad.write());
    }
    private void releaseShorad(Entity target) {
        long key=getLauncherKey();int slot=shorad.slot;
        if(!MissileTrackingService.tryReserve(world,target.getEntityId(),key)) {shoradTarget=-1;return;}
        LegacySamGeometry.Shot shot=ShoradGeometry.shot(isTor(),1,shorad.yaw,shorad.pitch,slot,getPositionVector(),rotationYaw,rotationPitch);
        EntityWarTechMissile missile=LegacyEntityFactory.missile(world,isTor()?MissileProfile.ANTI_AIR_TIER_2:MissileProfile.ANTI_AIR_TIER_1);
        missile.setPosition(shot.origin.x,shot.origin.y,shot.origin.z);missile.setOwnerIdentity(getOwnerUuid(),getOwnerTeam());
        missile.setVisual("missile/anti_air_tier_"+(isTor()?"2":"1"),0);
        boolean malfunction=com.wartec.wartecmod.port.integration.VlsInterceptorGuidance.configureMobileLaunch(missile,target,isTor()?2:1,isTor());
        missile.configureSamVisual(shot.scale,shot.direction,shot.scale[1]+.18F);
        Vec3d motion=shot.direction.scale(.45);missile.motionX=motion.x;missile.motionY=motion.y;missile.motionZ=motion.z;
        missile.rotationYaw=(float)Math.toDegrees(Math.atan2(motion.x,motion.z));missile.rotationPitch=(float)-Math.toDegrees(Math.atan2(motion.y,Math.hypot(motion.x,motion.z)));
        if(MissileChunkLoader.spawnFlight(missile)) {
            com.wartec.wartecmod.port.planning.PlanningHooks.defenseShot(world,this,missile,target);
            if(malfunction) {MissileTrackingService.releaseReservation(world,target.getEntityId(),key);MissileTrackingService.deferTarget(world,target.getEntityId());}
            else MissileTrackingService.confirmReservation(world,target.getEntityId(),key,missile.getEntityId());
            com.wartec.wartecmod.port.integration.VlsInterceptorGuidance.spawnLaunchSmoke(world,shot.mouth.x,shot.mouth.y,shot.mouth.z,isTor()?2:1);
            setInventorySlotContents(slot,ItemStack.EMPTY);setLegacyPower(getLegacyPower()-50000);
            legacyLaunchCooldown=isTor()?(getLegacyFireMode()==2?3:8):4;
            shorad.released(missile.getModernVisual().getInteger("ExitTicks"));markDirty();
        } else MissileTrackingService.releaseReservation(world,target.getEntityId(),key);
        shoradTarget=-1;
    }

    private void tickPantsirGuns() {
        if (gunCooldown > 0) {
            --gunCooldown;
        }
        if (gunFiringTicks > 0 && --gunFiringTicks == 0) {
            setGunFiring(false);
        }
        if (!isGunsEnabled() || getPantsirGunRounds() <= 0
                || getLegacyPower() < GUN_BURST_ENERGY) {
            resetGunTracking();
            return;
        }

        Entity target = gunTargetId < 0
                ? null : world.getEntityByID(gunTargetId);
        if (!isGunTargetValid(target)) {
            target = MissileTrackingService.findCloseThreat(world,
                    posX, posY + 3.1D*VehicleDimensions.scale(getProfile()), posZ, 1, GUN_RANGE,
                    getLauncherKey(), getOwnerTeam());
            int targetId = target == null ? -1 : target.getEntityId();
            if (targetId != gunTargetId) {
                gunTargetId = targetId;
                gunLockTicks = 0;
                gunHitScore = 0;
                gunTierOneSolution = target != null
                        && MissileTrackingService.getThreatTier(target) == 1
                        && world.rand.nextDouble() < 0.75D;
            }
        }
        if (target == null) {
            resetGunTracking();
            return;
        }

        double dx = target.posX - posX;
        double dy = target.posY + 0.45D - (posY + 2.625D*VehicleDimensions.scale(getProfile()));
        double dz = target.posZ - posZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double leadTime = Math.max(0.35D, Math.min(4.0D, distance / 14.0D));
        double leadX = target.posX + target.motionX * leadTime;
        double leadY = target.posY + 0.45D + target.motionY * leadTime;
        double leadZ = target.posZ + target.motionZ * leadTime;
        dx = leadX - posX;
        dy = leadY - (posY + 2.625D*VehicleDimensions.scale(getProfile()));
        dz = leadZ - posZ;
        double horizontal = Math.max(0.001D, Math.sqrt(dx * dx + dz * dz));
        float targetYaw = wrapDegrees((float) Math.toDegrees(
                Math.atan2(-dx, dz)) - rotationYaw);
        float targetPitch = MathHelper.clamp((float) Math.toDegrees(
                Math.atan2(dy, horizontal)), -8.0F, 78.0F);
        gunAimYaw = approachAngle(gunAimYaw, targetYaw, AirDefenseTraverse.PANTSIR_YAW);
        gunAimPitch = approach(gunAimPitch, targetPitch, AirDefenseTraverse.PANTSIR_PITCH);
        syncGunAim();

        float yawError = Math.abs(wrapDegrees(targetYaw - gunAimYaw));
        float pitchError = Math.abs(targetPitch - gunAimPitch);
        gunLockTicks = yawError <= 3.0F && pitchError <= 3.0F
                ? gunLockTicks + 1 : Math.max(0, gunLockTicks - 2);
        if (gunLockTicks == 4) {
            AircraftCountermeasureCompat.deploy(target);
        }
        if (gunLockTicks >= 8 && gunCooldown <= 0) {
            fireGunBurst(target, leadX, leadY, leadZ, distance);
        }
    }

    private boolean isGunTargetValid(Entity target) {
        if (target == null || target.isDead
                || MissileTrackingService.getThreatTier(target) == 0) {
            return false;
        }
        return !com.wartec.wartecmod.port.integration.NetworkTeamHelper.isFriendly(getOwnerTeam(),target)
            && target.getDistanceSq(this) <= GUN_RANGE * GUN_RANGE
            && com.wartec.wartecmod.port.integration.AirDefenseVisibility.visible(world,
                getPositionVector().addVector(0,3.22*VehicleDimensions.scale(getProfile()),0),target);
    }

    private void fireGunBurst(Entity target, double leadX, double leadY,
            double leadZ, double distance) {
        ItemStack belt = getStackInSlot(13);
        int consumed = PantsirAmmoBeltItem.consume(belt, GUN_BURST_ROUNDS);
        if (consumed <= 0) {
            resetGunTracking();
            return;
        }
        if (PantsirAmmoBeltItem.getRounds(belt) <= 0) {
            setInventorySlotContents(13, ItemStack.EMPTY);
        }
        setLegacyPower(getLegacyPower() - GUN_BURST_ENERGY);
        gunCooldown = 8;
        gunFiringTicks = 5;
        setGunFiring(true);
        markGunBurst();
        playPantsirGunSound();
        markDirty();

        int tier = MissileTrackingService.getThreatTier(target);
        double chance = tier == 1 ? 0.78D : tier == 2 ? 0.22D : 0.045D;
        if (MissileTrackingService.isBallisticTarget(target)) {
            chance *= 0.45D;
        }
        double rangeFactor = 1.0D
                - Math.min(0.22D, distance / GUN_RANGE * 0.22D);
        double lockBonus = Math.min(0.14D,
                Math.max(0, gunLockTicks - 1) * 0.014D);
        double hitChance = Math.min(tier == 1 ? 0.86D : 0.78D,
                (chance * rangeFactor + lockBonus)
                        * (double) consumed / GUN_BURST_ROUNDS);
        boolean decoyed = AircraftCountermeasureCompat.tryDecoy(target, 1);
        boolean hit = !decoyed && (tier != 1 || gunTierOneSolution)
                && world.rand.nextDouble() < hitChance;
        java.util.UUID reportShot=com.wartec.wartecmod.port.planning.PlanningHooks.gunBurst(this,target,consumed);
        emitGunBurst(leadX, leadY, leadZ, hit);
        if (decoyed) {
            gunHitScore = Math.max(0, gunHitScore - 1);
        }
        if (hit) {
            ++gunHitScore;
            emitGunHit(target);
        }
        int requiredHits = tier == 1 ? 2 : tier == 2 ? 4 : 10;
        if (MissileTrackingService.isBallisticTarget(target)) {
            requiredHits += 4;
        }
        if (gunHitScore >= requiredHits && !target.isDead) {
            destroyGunTarget(target);
            com.wartec.wartecmod.port.planning.PlanningHooks.gunOutcome(this,reportShot,target);
        }
    }

    private void emitGunBurst(double leadX, double leadY, double leadZ,
            boolean hit) {
        if (!(world instanceof WorldServer)) {
            return;
        }
        WorldServer server = (WorldServer) world;
        double yaw = Math.toRadians(rotationYaw + gunAimYaw);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double sideX = Math.cos(yaw);
        double sideZ = Math.sin(yaw);
        double spread = hit ? 0.35D : 1.9D;
        double targetX = leadX + (world.rand.nextDouble() - 0.5D) * spread;
        double targetY = leadY + (world.rand.nextDouble() - 0.5D) * spread;
        double targetZ = leadZ + (world.rand.nextDouble() - 0.5D) * spread;
        for (int side = -1; side <= 1; side += 2) {
            double scale=VehicleDimensions.scale(getProfile());
            double muzzleX = posX + (forwardX * 1.45D + sideX * side * 1.05D)*scale;
            double muzzleY = posY + 3.22D*scale;
            double muzzleZ = posZ + (forwardZ * 1.45D + sideZ * side * 1.05D)*scale;
            server.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    muzzleX, muzzleY, muzzleZ, 2,
                    0.08D, 0.08D, 0.08D, 0.01D);
            for (int point = 1; point <= 7; ++point) {
                double step = point / 8.0D;
                server.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK,
                        muzzleX + (targetX - muzzleX) * step,
                        muzzleY + (targetY - muzzleY) * step,
                        muzzleZ + (targetZ - muzzleZ) * step,
                        1, 0.015D, 0.015D, 0.015D, 0.0D);
            }
        }
    }

    private void emitGunHit(Entity target) {
        if (world instanceof WorldServer) {
            ((WorldServer) world).spawnParticle(EnumParticleTypes.CRIT,
                    target.posX, target.posY + 0.35D, target.posZ,
                    10, 0.28D, 0.28D, 0.28D, 0.12D);
        }
    }

    private void destroyGunTarget(Entity target) {
        MissileTrackingService.releaseReservation(world,
                target.getEntityId(), getLauncherKey());
        boolean fire = world.rand.nextDouble() < 0.3D;
        boolean crashing = AircraftCountermeasureCompat.beginCrash(target);
        world.newExplosion(this, target.posX, target.posY + 0.25D,
                target.posZ, crashing ? 0.9F : 1.35F,
                fire && !crashing, false);
        if (!crashing) {
            target.setDead();
        }
        resetGunTracking();
    }

    private void playPantsirGunSound() {
        SoundEvent sound = SoundEvent.REGISTRY.getObject(
                new ResourceLocation("hbm", "turret.richard_fire"));
        if (sound == null) {
            sound = net.minecraft.init.SoundEvents.ENTITY_BLAZE_SHOOT;
        }
        world.playSound(null, posX, posY, posZ, sound,
                SoundCategory.HOSTILE, 4.5F,
                0.84F + world.rand.nextFloat() * 0.08F);
    }

    private void resetGunTracking() {
        gunTargetId = -1;
        gunLockTicks = 0;
        gunHitScore = 0;
        gunTierOneSolution = false;
        gunAimYaw = 0.0F;
        gunAimPitch = 0.0F;
        syncGunAim();
        setGunFiring(false);
    }

    private void setGunFiring(boolean firing) {
        int packed = dataManager.get(GUN_STATE);
        int flags = (isGunsEnabled() ? 1 : 0) | (firing ? 2 : 0);
        setGunState(packed & 0xFFFFFFFC | flags);
    }

    private void syncGunAim() {
        int packed = dataManager.get(GUN_STATE);
        int yaw = Math.round(gunAimYaw * 10.0F) & 0x1FFF;
        int pitch = Math.round(gunAimPitch * 10.0F) & 0x7FF;
        setGunState(packed & 0xFC000003 | yaw << 13 | pitch << 2);
    }

    private void markGunBurst() {
        int packed = dataManager.get(GUN_STATE);
        int serial = (packed >>> 26) + 1 & 0x3F;
        setGunState(packed & 0x03FFFFFF | serial << 26);
    }

    private void setGunState(int value) {
        if (dataManager.get(GUN_STATE) != value) {
            dataManager.set(GUN_STATE, value);
        }
    }

    private void updateClientGunState() {
        clientPreviousGunYaw = clientGunYaw;
        clientPreviousGunPitch = clientGunPitch;
        float yaw = getPantsirGunAimYaw();
        float pitch = getPantsirGunAimPitch();
        int serial = dataManager.get(GUN_STATE) >>> 26 & 0x3F;
        if (clientGunBurstSerial < 0) {
            clientGunYaw = yaw;
            clientGunPitch = pitch;
            clientPreviousGunYaw = yaw;
            clientPreviousGunPitch = pitch;
            clientGunBurstSerial = serial;
            return;
        }
        clientGunYaw = approachAngle(clientGunYaw, yaw, AirDefenseTraverse.PANTSIR_YAW);
        clientGunPitch = approach(clientGunPitch, pitch, AirDefenseTraverse.PANTSIR_PITCH);
        if (serial != clientGunBurstSerial) {
            clientGunBurstSerial = serial;
            spawnClientGunBurst();
        }
    }

    private void spawnClientGunBurst() {
        double yaw = Math.toRadians(rotationYaw + clientGunYaw);
        double pitch = Math.toRadians(clientGunPitch);
        double horizontal = Math.cos(pitch);
        double forwardX = -Math.sin(yaw) * horizontal;
        double forwardY = Math.sin(pitch);
        double forwardZ = Math.cos(yaw) * horizontal;
        double sideX = Math.cos(yaw);
        double sideZ = Math.sin(yaw);
        for (int side = -1; side <= 1; side += 2) {
            double scale=VehicleDimensions.scale(getProfile());
            double muzzleX = posX + (forwardX * 1.45D + sideX * side * 1.05D)*scale;
            double muzzleY = posY + 3.22D*scale;
            double muzzleZ = posZ + (forwardZ * 1.45D + sideZ * side * 1.05D)*scale;
            world.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    muzzleX, muzzleY, muzzleZ,
                    forwardX * 0.03D, 0.025D, forwardZ * 0.03D);
            for (int point = 1; point <= 14; ++point) {
                double distance = point * 6.5D;
                double x = muzzleX + forwardX * distance;
                double y = muzzleY + forwardY * distance;
                double z = muzzleZ + forwardZ * distance;
                world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK,
                        x, y, z,
                        forwardX * 0.16D,
                        forwardY * 0.16D,
                        forwardZ * 0.16D);
                if (point % 4 == 0) {
                    world.spawnParticle(EnumParticleTypes.FLAME,
                            x, y, z,
                            forwardX * 0.04D,
                            forwardY * 0.04D,
                            forwardZ * 0.04D);
                }
            }
        }
    }

    private static float approach(float current, float target, float rate) {
        if (current < target) {
            return Math.min(target, current + rate);
        }
        if (current > target) {
            return Math.max(target, current - rate);
        }
        return current;
    }

    private static float approachAngle(float current, float target, float rate) {
        return wrapDegrees(current + MathHelper.clamp(
                wrapDegrees(target - current), -rate, rate));
    }

    private static float wrapDegrees(float angle) {
        return MathHelper.wrapDegrees(angle);
    }

    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        if (!world.isRemote && !canManage(player)) return true;
        if (getProfile() != WarTechEntityProfile.MOBILE_ARTILLERY) {
            return super.processInitialInteract(player, hand);
        }
        if (hand != EnumHand.MAIN_HAND) {
            return true;
        }
        if (world.isRemote) {
            return true;
        }
        ItemStack held = player.getHeldItem(hand);
        if (trySalvage(player, held, true)) {
            return true;
        }
        if (held.getItem() == WarTechContent.ARTILLERY_TARGET_DESIGNATOR) {
            return ((ArtilleryTargetDesignatorItem) held.getItem())
                    .linkTo(this, player, held);
        }
        if (getOwnerTeam().isEmpty()) {
            setOwnerTeam(NetworkTeamHelper.getPlayerTeam(player));
        }
        if (installArtilleryModule(player, held)) {
            return true;
        }
        if (DesignatorCompat.isDesignator(held)) {
            BlockPos target = DesignatorCompat.getTarget(world, player, held);
            if (target == null) {
                boolean queued=DesignatorCompat.resolveForEntity(this,player,hand,point->acceptArtilleryDesignatorTarget(player,point));
                player.sendMessage(new net.minecraft.util.text.TextComponentTranslation(queued?"uav.message.resolving_y":"uav.message.configure_target"));
            } else {
                acceptArtilleryDesignatorTarget(player, target);
            }
            return true;
        }
        player.openGui(WarTechReforged.instance,WarTechGuiHandler.guiForEntity(this),world,getEntityId(),0,0);
        return true;
    }

    public boolean acceptArtilleryDesignatorTarget(EntityPlayer player,
            BlockPos target) {
        if (world.isRemote
                || getProfile() != WarTechEntityProfile.MOBILE_ARTILLERY) {
            return false;
        }
        if (getVisualVariant() == 0) {
            player.sendMessage(new TextComponentString(
                    "This chassis has no artillery module."));
            return false;
        }
        if (!isDeployed()) {
            player.sendMessage(new TextComponentString(
                    "Deploy the artillery platform first."));
            return false;
        }
        setLegacyFireMode(getVisualVariant() == 1 ? 2 : 1);
        double maximum=com.wartec.wartecmod.port.integration.WeaponBalance.artilleryRange(getVisualVariant(),false);
        double distance=Math.hypot(target.getX()+.5-posX,target.getZ()+.5-posZ);
        if(distance<250 || distance>maximum) {
            player.sendMessage(new net.minecraft.util.text.TextComponentTranslation("flight.error.artillery_range",250,(int)maximum));return false;
        }
        artilleryTargetId = -1;
        artillerySearchTimer = 0;
        boolean wasEmpty = artilleryTargetQueue.isEmpty()
                && !hasGuidanceTarget();
        artilleryTargetQueue.add(new Vec3d(
                target.getX() + 0.5D,
                target.getY() + 0.5D,
                target.getZ() + 0.5D));
        if (wasEmpty) {
            artilleryFireTimer = 0;
            Vec3d queued = artilleryTargetQueue.get(0);
            setGuidanceTarget(queued.x, queued.y, queued.z);
        }
        playArtillerySound("hbm:item.techBoop", 1.0F, 1.0F);
        player.sendMessage(new TextComponentString(
                "Target accepted: " + target.getX() + ", "
                        + target.getY() + ", " + target.getZ()
                        + " (" + artilleryTargetQueue.size()
                        + " queued)"));
        return true;
    }

    private boolean installArtilleryModule(EntityPlayer player,
            ItemStack stack) {
        if (getVisualVariant() != 0 || stack.isEmpty()) {
            return false;
        }
        int mount = 0;
        ResourceLocation registryName = stack.getItem().getRegistryName();
        String id = registryName == null ? "" : registryName.toString();
        if (stack.getItem() == WarTechContent.MOBILE_ARTILLERY) {
            mount = MathHelper.clamp(stack.getMetadata(), 0, 2);
        } else if (id.endsWith(":turret_jeremy")
                || id.endsWith(":turret_arty")) {
            mount = 1;
        } else if (id.endsWith(":turret_rocket")
                || id.endsWith(":turret_himars")) {
            mount = 2;
        }
        if (mount == 0) {
            return false;
        }
        setVisual(getVisualId(), mount);
        setLegacyFireMode(0);
        setLegacyPower(Math.min(getLegacyPower(), getEnergyCapacity()));
        if (!player.capabilities.isCreativeMode) {
            stack.shrink(1);
        }
        playArtillerySound("minecraft:block.anvil.use", 0.9F, 0.82F);
        player.sendMessage(new TextComponentString(mount == 1
                ? "Greg module installed." : "Henry module installed."));
        return true;
    }

    @Override
    public boolean handleLegacyGuiAction(int action, EntityPlayer player) {
        if(action>=90 && action<=93) return handleVehiclePanelAction(action,player);
        if (getProfile() != WarTechEntityProfile.MOBILE_ARTILLERY) {
            return super.handleLegacyGuiAction(action, player);
        }
        if (action == 0) {
            setLegacyEnabled(!isLegacyEnabled());
        } else if (action >= 1 && action <= 4) {
            setLegacyFlags(getLegacyFlags()
                    ^ 1 << (action + 5));
        } else if (action == 5) {
            int modes = getVisualVariant() == 1 ? 3 : 2;
            setLegacyFireMode((getLegacyFireMode() + 1) % modes);
            clearGuidanceTarget();
            artilleryTargetQueue.clear();
            artilleryTargetId = -1;
        } else {
            return false;
        }
        return true;
    }

    public boolean canDriveFromPanel() { return !isDeployed() && !isShoradMotionBlocked() && !isBeingRidden(); }
    public boolean supportsDeployment() { return getProfile()!=WarTechEntityProfile.ELECTRONIC_WARFARE && !(getProfile()==WarTechEntityProfile.MOBILE_ARTILLERY && getVisualVariant()==0); }
    private boolean handleVehiclePanelAction(int action,EntityPlayer player) {
        if(world.isRemote || !isUsableByPlayer(player) || (!getOwnerTeam().isEmpty() && !NetworkTeamHelper.areFriendly(getOwnerTeam(),NetworkTeamHelper.getPlayerTeam(player)))) return false;
        if(action==90) {
            if(!canDriveFromPanel()) return false;
            player.closeScreen();return player.startRiding(this,true);
        }
        if(action==91 && supportsDeployment()) {
            if(isBeingRidden() || Math.abs(getDriveSpeed())>.01) return false;
            if(!isDeployed()) {
                AxisAlignedBB box=getEntityBoundingBox();
                if(world.getCollisionBoxes(null,new AxisAlignedBB(box.minX+.05,box.minY-.125,box.minZ+.05,box.maxX-.05,box.minY+.001,box.maxZ-.05)).isEmpty()) return false;
                if(getProfile()==WarTechEntityProfile.COMMAND_TRUCK) {
                    int claim=FactionTerritoryData.claimAt(world,getOwnerTeam(),posX,posZ);
                    if(claim==FactionTerritoryData.CONFLICT || claim==FactionTerritoryData.INVALID_TEAM || claim==FactionTerritoryData.LIMIT_REACHED) return false;
                }
            }
            setDeployed(!isDeployed());motionX=motionY=motionZ=driveSpeed=0;
            if(getProfile()==WarTechEntityProfile.S400_RADAR) setLegacyEnabled(isDeployed());
            if(!isDeployed()) {setLegacyOperational(false);artilleryTargetId=-1;}
            return true;
        }
        if(getProfile()==WarTechEntityProfile.ELECTRONIC_WARFARE) {
            if(action==92) { setLegacyEnabled(!isLegacyEnabled());if(!isLegacyEnabled()) ElectronicWarfareService.removeNode(world,getEntityId());return true; }
            if(action==93) {setLegacySelectedPayload((getLegacySelectedPayload()+1)%4);return true;}
        }
        return false;
    }
    public void acceptDriverInput(EntityPlayer player,float forward,float strafe) {
        acceptDriverInput(player,forward,strafe,0,driverSequence+1);
    }
    public void acceptDriverInput(EntityPlayer player,float forward,float strafe,int flags,int sequence) {
        if(world.isRemote || !isDrivenBy(player) || !canManage(player) || isDeployed()
                || !Float.isFinite(forward) || !Float.isFinite(strafe))return;
        if(!player.getUniqueID().equals(inputDriver)) {
            inputDriver=player.getUniqueID();driverSequence=-1;driverForward=driverStrafe=0;exitRequested=false;
        }
        if(sequence<0 || sequence<=driverSequence)return;
        driverSequence=sequence;driverForward=GroundVehiclePhysics.finiteClamp(forward);
        driverStrafe=GroundVehiclePhysics.finiteClamp(strafe);driverBrake=(flags&1)!=0;
        lastDriverInputTick=ticksExisted;
        if((flags&2)!=0)parkingBrake=!parkingBrake;
        if((flags&4)!=0) {
            driverBrake=true;driverForward=driverStrafe=0;
            player.openGui(WarTechReforged.instance,WarTechGuiHandler.guiForEntity(this),world,getEntityId(),0,0);
        }
        if((flags&8)!=0)requestDriverExit();
    }
    public boolean isDrivenBy(EntityPlayer player) {
        return player!=null && player.getRidingEntity()==this && getControllingPassenger()==player;
    }
    public void requestDriverExit() {exitRequested=true;driverBrake=true;driverForward=driverStrafe=0;}
    public boolean allowsImmediateExit() {return allowingExit || isDead || isDeployed() || !VehicleChassisProfile.isDrivable(getProfile());}
    public void acceptForcedClientExit(EntityPlayer player) {
        if(!world.isRemote)return;
        allowingExit=true;
        try {if(player.getRidingEntity()==this)player.dismountRidingEntity();} finally {allowingExit=false;}
        localPrediction=false;
    }
    @Override protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if(!world.isRemote && passenger instanceof EntityPlayerMP && ((EntityPlayerMP)passenger).connection!=null)
            WarTechNetwork.CHANNEL.sendTo(new VehicleStateMessage(this,driverSequence),(EntityPlayerMP)passenger);
        driverForward=driverStrafe=0;driverBrake=true;
    }
    private void trySafeExit(EntityPlayer player) {
        VehicleChassisProfile p=VehicleChassisProfile.of(this);
        for(double forward:new double[]{p.seatForward,0,-p.length*.55}) for(int side:new int[]{-1,1}) {
            Vec3d off=p.offset(side*(p.width/2+.8),0,forward,rotationYaw,0);
            Vec3d top=new Vec3d(posX+off.x,posY+3,posZ+off.z);
            net.minecraft.util.math.RayTraceResult hit=world.rayTraceBlocks(top,top.addVector(0,-6,0),false,true,false);
            if(hit==null || hit.typeOfHit!=net.minecraft.util.math.RayTraceResult.Type.BLOCK)continue;
            double y=hit.hitVec.y+.02;
            AxisAlignedBB box=new AxisAlignedBB(top.x-player.width/2,y,top.z-player.width/2,
                    top.x+player.width/2,y+player.height,top.z+player.width/2);
            if(!world.getCollisionBoxes(null,box).isEmpty())continue;
            boolean busy=false;
            for(Entity other:world.getEntitiesWithinAABBExcludingEntity(player,box)) {
                if(other==this || other.isDead)continue;
                if(other instanceof EntityWarTechGroundVehicle || other.canBeCollidedWith()){busy=true;break;}
            }
            if(busy || VehicleCollisionHull.overlaps(p,posX,posY,posZ,rotationYaw,height,box))continue;
            allowingExit=true;
            try {player.dismountRidingEntity();player.setPositionAndUpdate(top.x,y,top.z);} finally {allowingExit=false;}
            exitRequested=false;parkingBrake=true;return;
        }
        if(ticksExisted%40==0)player.sendMessage(new net.minecraft.util.text.TextComponentTranslation("wartecmod.vehicle.exit_blocked"));
    }

    static int driverInputAge(int currentTick, int lastInputTick) {
        if (lastInputTick < 0 || currentTick < lastInputTick) {
            return Integer.MAX_VALUE;
        }
        return currentTick - lastInputTick;
    }

    @Override
    public net.minecraft.entity.Entity getControllingPassenger() {
        return getPassengers().isEmpty() ? null : getPassengers().get(0);
    }
    @Override public boolean canPassengerSteer() {
        // Vanilla CPacketVehicleMove competes with the authoritative input/ACK
        // protocol and lets stale client positions move the server a second time.
        return false;
    }
    @Override public void setVelocity(double x,double y,double z) {
        if(world.isRemote && localPrediction)return;
        super.setVelocity(x,y,z);
    }

    @Override
    public void updatePassenger(net.minecraft.entity.Entity passenger) {
        if(!isPassenger(passenger))return;
        VehicleChassisProfile p=VehicleChassisProfile.of(this);
        Vec3d seat=p.seat(rotationYaw,rotationPitch);
        passenger.setPosition(posX+seat.x,posY+seat.y+passenger.getYOffset(),posZ+seat.z);
        if(passenger instanceof net.minecraft.entity.EntityLivingBase)
            ((net.minecraft.entity.EntityLivingBase)passenger).renderYawOffset=rotationYaw+p.frontYaw;
    }
    @Override public double getMountedYOffset() {return VehicleChassisProfile.of(this).seatHeight;}
    @Override public boolean canBePushed() {return false;}
    @Override public void applyEntityCollision(Entity other) { }
    @Override public AxisAlignedBB getCollisionBox(Entity other) {
        return other==null || isRidingSameEntity(other)?null:other.getEntityBoundingBox();
    }
    @Override public AxisAlignedBB getCollisionBoundingBox() {return getEntityBoundingBox();}
    @Override public AxisAlignedBB getRenderBoundingBox() {
        if(!VehicleChassisProfile.isDrivable(getProfile()))return super.getRenderBoundingBox();
        return VehicleCollisionHull.bounds(VehicleChassisProfile.of(this),posX,posY,posZ,rotationYaw,height).grow(1);
    }

    @Override
    public float getCollisionBorderSize() {
        if (getProfile() == WarTechEntityProfile.MOBILE_ARTILLERY) {
            return isDeployed() ? 1.45F : 0.35F;
        }
        if (getProfile() == WarTechEntityProfile.RADAR_TRUCK) {
            return 0.6F;
        }
        if (getProfile() == WarTechEntityProfile.MOBILE_AIR_DEFENSE) {
            return 0.65F;
        }
        return 0.35F;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (world.isRemote || isDead || amount <= 0.0F) {
            return true;
        }
        Entity attacker = source == null ? null : source.getTrueSource();
        boolean creative = attacker instanceof EntityPlayer
                && ((EntityPlayer) attacker).capabilities.isCreativeMode;
        WarTechEntityProfile profile = getProfile();
        if (creative && source.getImmediateSource()==attacker && !source.isExplosion() && !source.isProjectile()) {
            destroyGroundVehicle(profile != WarTechEntityProfile.MOBILE_AIR_DEFENSE);
            return true;
        }
        if (profile == WarTechEntityProfile.MOBILE_ARTILLERY
                && ticksExisted <= 40) {
            return true;
        }

        float applied = amount;
        if ((profile == WarTechEntityProfile.COMMAND_TRUCK
                || profile == WarTechEntityProfile.S400_RADAR)
                && source != null && source.isExplosion()) {
            applied *= 0.65F;
        } else if (profile == WarTechEntityProfile.ELECTRONIC_WARFARE) {
            applied = Math.min(70.0F, applied);
        } else if (profile == WarTechEntityProfile.MOBILE_ARTILLERY) {
            applied = Math.min(75.0F, applied);
        }
        setHealthValue(getHealthValue() - applied);
        if (getHealthValue() <= 0.0F) {
            destroyGroundVehicle(true);
        } else if (profile == WarTechEntityProfile.RADAR_TRUCK) {
            playArtillerySound("minecraft:block.anvil.land", 0.35F, 1.5F);
        } else if (profile == WarTechEntityProfile.MOBILE_AIR_DEFENSE
                && getHealthValue() < getHealthCapacity()*.25F) {
            playArtillerySound("minecraft:block.anvil.land", 0.5F, 0.72F);
        } else if (profile == WarTechEntityProfile.MOBILE_ARTILLERY) {
            playArtillerySound("minecraft:block.anvil.land", 0.35F, 1.45F);
        }
        return true;
    }

    public void destroyByAntiRadiationMissile() {
        if (world.isRemote || isDead) {
            return;
        }
        WarTechEntityProfile profile = getProfile();
        if (profile != WarTechEntityProfile.COMMAND_TRUCK
                && profile != WarTechEntityProfile.RADAR_TRUCK
                && profile != WarTechEntityProfile.S400_RADAR
                && profile != WarTechEntityProfile.MOBILE_AIR_DEFENSE
                && profile != WarTechEntityProfile.ELECTRONIC_WARFARE) {
            return;
        }
        if (profile == WarTechEntityProfile.MOBILE_AIR_DEFENSE) {
            destroyGroundVehicle(true);
            return;
        }
        if (profile == WarTechEntityProfile.COMMAND_TRUCK
                || profile == WarTechEntityProfile.RADAR_TRUCK
                || profile == WarTechEntityProfile.S400_RADAR) {
            dropInventorySlot(getBatterySlot());
        }
        setDead();
    }

    private void destroyGroundVehicle(boolean explode) {
        if (isDead) {
            return;
        }
        WarTechEntityProfile profile = getProfile();
        int loadedInterceptors = profile == WarTechEntityProfile.MOBILE_AIR_DEFENSE
                ? countLoadedInterceptors() : 0;
        if (profile == WarTechEntityProfile.MOBILE_AIR_DEFENSE || ModernVehicleProfile.of(profile)!=null || this instanceof EntityTubeArtillery) {
            dropAllStoredItems();
        } else if (profile == WarTechEntityProfile.COMMAND_TRUCK
                || profile == WarTechEntityProfile.RADAR_TRUCK
                || profile == WarTechEntityProfile.S400_RADAR) {
            dropInventorySlot(getBatterySlot());
        } else if (profile == WarTechEntityProfile.MOBILE_ARTILLERY) {
            clear();
        }
        setDead();
        if (!explode) {
            return;
        }
        float strength = profile == WarTechEntityProfile.MOBILE_AIR_DEFENSE
                ? loadedInterceptors > 0 ? 5.5F : 4.0F
                : profile == WarTechEntityProfile.ELECTRONIC_WARFARE
                        && getVisualVariant() == 2 ? 2.0F
                        : profile.getExplosionStrength();
        double y = profile == WarTechEntityProfile.S400_RADAR ? posY + 2.0D
                : profile == WarTechEntityProfile.RADAR_TRUCK ? posY + 1.5D
                : profile == WarTechEntityProfile.MOBILE_AIR_DEFENSE
                        ? posY + 1.4D : posY + 1.0D;
        world.newExplosion(this, posX, y, posZ, strength, true, true);
    }

    private int countLoadedInterceptors() {
        int count = 0;
        int capacity = isTor() ? 8 : 12;
        for (int slot = 0; slot < capacity; ++slot) {
            if (isRequiredInterceptor(getStackInSlot(slot))) {
                ++count;
            }
        }
        return count;
    }

    private void dropAllStoredItems() {
        for (int slot = 0; slot < getSizeInventory(); ++slot) {
            dropInventorySlot(slot);
        }
    }

    private void dropInventorySlot(int slot) {
        ItemStack stored = removeStackFromSlot(slot);
        if (!stored.isEmpty()) {
            entityDropItem(stored, 0.5F);
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setTag("ShoradPose",shorad.write());
        compound.setDouble("DriveSpeed", driveSpeed);
        compound.setDouble("SteeringState", steeringState);
        compound.setBoolean("ParkingBrake",parkingBrake);
        compound.setFloat("ArtilleryYaw",
                dataManager.get(ARTILLERY_YAW));
        compound.setFloat("ArtilleryPitch",
                dataManager.get(ARTILLERY_PITCH));
        compound.setInteger("ArtilleryAmmoType",
                dataManager.get(ARTILLERY_AMMO_TYPE));
        compound.setInteger("ArtilleryAmmoCount",
                dataManager.get(ARTILLERY_AMMO_COUNT));
        compound.setFloat("ArtilleryCrane",
                dataManager.get(ARTILLERY_CRANE));
        compound.setFloat("ArtilleryRecoil",
                dataManager.get(ARTILLERY_RECOIL));
        compound.setString("ArtilleryWhitelist",
                dataManager.get(ARTILLERY_WHITELIST));
        NBTTagList targets = new NBTTagList();
        for (Vec3d target : artilleryTargetQueue) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setDouble("X", target.x);
            entry.setDouble("Y", target.y);
            entry.setDouble("Z", target.z);
            targets.appendTag(entry);
        }
        compound.setTag("ArtilleryTargetQueue", targets);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        shorad.read(compound.getCompoundTag("ShoradPose"),isDeployed());
        previousShorad.read(shorad.write(),isDeployed());dataManager.set(SHORAD_POSE,shorad.write());shoradTarget=-1;
        driveSpeed = compound.getDouble("DriveSpeed");
        steeringState = compound.getDouble("SteeringState");
        if(!Double.isFinite(driveSpeed))driveSpeed=0;
        if(!Double.isFinite(steeringState))steeringState=0;
        parkingBrake=compound.getBoolean("ParkingBrake");
        lastDriverInputTick=-1;driverForward=driverStrafe=0;driverBrake=true;
        dataManager.set(ARTILLERY_YAW,
                compound.getFloat("ArtilleryYaw"));
        dataManager.set(ARTILLERY_PITCH,
                compound.getFloat("ArtilleryPitch"));
        dataManager.set(ARTILLERY_AMMO_TYPE,
                compound.hasKey("ArtilleryAmmoType")
                        ? compound.getInteger("ArtilleryAmmoType") : -1);
        dataManager.set(ARTILLERY_AMMO_COUNT,
                compound.getInteger("ArtilleryAmmoCount"));
        dataManager.set(ARTILLERY_CRANE,
                compound.getFloat("ArtilleryCrane"));
        dataManager.set(ARTILLERY_RECOIL,
                compound.getFloat("ArtilleryRecoil"));
        dataManager.set(ARTILLERY_WHITELIST,
                compound.getString("ArtilleryWhitelist"));
        artilleryTargetQueue.clear();
        NBTTagList targets = compound.getTagList(
                "ArtilleryTargetQueue", 10);
        for (int index = 0; index < targets.tagCount(); ++index) {
            NBTTagCompound entry = targets.getCompoundTagAt(index);
            artilleryTargetQueue.add(new Vec3d(
                    entry.getDouble("X"),
                    entry.getDouble("Y"),
                    entry.getDouble("Z")));
        }
    }
}
