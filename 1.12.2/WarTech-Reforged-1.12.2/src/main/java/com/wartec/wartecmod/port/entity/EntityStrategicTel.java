package com.wartec.wartecmod.port.entity;

import com.wartec.wartecmod.WarTechReforged;
import com.wartec.wartecmod.port.content.StrategicMissileItem;
import com.wartec.wartecmod.port.content.StrategicFeature;
import com.wartec.wartecmod.port.content.WarTechContent;
import com.wartec.wartecmod.port.gui.WarTechGuiHandler;
import com.wartec.wartecmod.port.integration.DesignatorCompat;
import com.wartec.wartecmod.port.integration.NetworkTeamHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumHand;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/** Mobile transporter-erector-launcher shared by Topol-M, Yars and Oreshnik. */
public final class EntityStrategicTel extends EntityWarTechGroundVehicle {
    private static final DataParameter<Integer> ERECTION =
            EntityDataManager.createKey(EntityStrategicTel.class,
                    DataSerializers.VARINT);
    private static final DataParameter<Boolean> LOADED =
            EntityDataManager.createKey(EntityStrategicTel.class,
                    DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> LAUNCH_TICKS =
            EntityDataManager.createKey(EntityStrategicTel.class,
                    DataSerializers.VARINT);

    public EntityStrategicTel(World world) {
        this(world, StrategicSystemProfile.TOPOL_M);
    }

    public EntityStrategicTel(World world, StrategicSystemProfile system) {
        super(world, system.getVehicleProfile());
        setProfile(system.getVehicleProfile());
        setVisual("deployment/strategic_" + system.getId(), system.ordinal());
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(ERECTION, 0);
        dataManager.register(LOADED, true);
        dataManager.register(LAUNCH_TICKS, 0);
    }

    public StrategicSystemProfile getStrategicSystem() {
        return StrategicSystemProfile.fromVehicleProfile(getProfile());
    }

    public int getErectionProgress() { return dataManager.get(ERECTION); }
    public boolean isMissileLoaded() { return dataManager.get(LOADED); }
    public void setMissileLoaded(boolean loaded) { dataManager.set(LOADED, loaded); }
    public int getLaunchTicks() { return dataManager.get(LAUNCH_TICKS); }

    @Override
    public void onUpdate() {
        if (!StrategicFeature.isEnabled()) {
            if (!world.isRemote && isBeingRidden()) removePassengers();
            return;
        }
        super.onUpdate();
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        return StrategicFeature.isEnabled() && super.isInRangeToRenderDist(distance);
    }

    @Override
    public boolean isInvisible() {
        return !StrategicFeature.isEnabled() || super.isInvisible();
    }

    @Override
    public boolean canBeCollidedWith() {
        return StrategicFeature.isEnabled() && super.canBeCollidedWith();
    }

    @Override
    public boolean canBePushed() {
        return StrategicFeature.isEnabled() && super.canBePushed();
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        return StrategicFeature.isEnabled() && super.attackEntityFrom(source, amount);
    }

    @Override
    protected void serverTick(WarTechEntityProfile profile) {
        if (!StrategicFeature.isEnabled()) return;
        super.serverTick(profile);
        int erection = getErectionProgress();
        int target = isDeployed() ? 100 : 0;
        if (erection != target) {
            erection += erection < target ? 2 : -2;
            dataManager.set(ERECTION, Math.max(0, Math.min(100, erection)));
        }
        int sequence = getLaunchTicks();
        if (sequence > 0) {
            sequence++;
            dataManager.set(LAUNCH_TICKS, sequence);
            tickLaunchSequence(sequence);
            if (sequence >= 90) dataManager.set(LAUNCH_TICKS, 0);
        }
    }

    private void tickLaunchSequence(int sequence) {
        if (sequence == 14) {
            world.playSound(null, posX, posY, posZ,
                    SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN,
                    SoundCategory.PLAYERS, 5.0F, 0.55F);
        }
        if (sequence >= 18 && sequence <= 52 && world instanceof WorldServer) {
            ((WorldServer) world).spawnParticle(EnumParticleTypes.CLOUD, true,
                    posX, posY + 0.4D, posZ, 22,
                    1.6D, 0.25D, 1.6D, 0.07D);
        }
        if (sequence != 44) return;
        StrategicSystemProfile system = getStrategicSystem();
        EntityStrategicMissile missile = new EntityStrategicMissile(world);
        double yaw = Math.toRadians(rotationYaw);
        missile.setPosition(posX - Math.sin(yaw) * 0.55D,
                posY + 4.2D, posZ + Math.cos(yaw) * 0.55D);
        missile.configureBoost(system, getTargetX(), getTargetY(), getTargetZ(),
                getOwnerUuid(), getOwnerTeam());
        missile.rotationYaw = rotationYaw;
        if (world.spawnEntity(missile)) {
            setMissileLoaded(false);
            world.playSound(null, posX, posY, posZ,
                    SoundEvents.ENTITY_GENERIC_EXPLODE,
                    SoundCategory.PLAYERS, 45.0F, 0.46F);
        }
    }

    public boolean beginLaunch(EntityPlayer player) {
        if (!StrategicFeature.isEnabled()) return false;
        if (world.isRemote || getLaunchTicks() > 0) return false;
        if (!isDeployed() || getErectionProgress() < 100) {
            tell(player, "TEL must be fully erected before launch.");
            return false;
        }
        if (!isMissileLoaded()) {
            tell(player, "Launch canister is empty.");
            return false;
        }
        if (!hasGuidanceTarget()) {
            tell(player, "Load target coordinates with a designator first.");
            return false;
        }
        double dx = getTargetX() - posX;
        double dz = getTargetZ() - posZ;
        double range = Math.sqrt(dx * dx + dz * dz);
        if (range > getStrategicSystem().getMaximumRange()) {
            tell(player, "Target is outside the "
                    + (int) getStrategicSystem().getMaximumRange()
                    + " block engagement envelope.");
            return false;
        }
        dataManager.set(LAUNCH_TICKS, 1);
        if (world.getMinecraftServer() != null) {
            world.getMinecraftServer().getPlayerList().sendMessage(
                    new TextComponentString("[STRATEGIC WARNING] "
                            + getStrategicSystem().getDisplayName()
                            + " launch detected. Impact corridor: "
                            + (int) getTargetX() + " / " + (int) getTargetZ()));
        }
        return true;
    }

    @Override
    public boolean handleLegacyGuiAction(int action, EntityPlayer player) {
        if (!StrategicFeature.isEnabled()) return false;
        if (player == null || player.getDistanceSq(this) > 144.0D
                || !canOperate(player)) return false;
        if (action == 0) return beginLaunch(player);
        if (action == 1 && getLaunchTicks() == 0) {
            setDeployed(!isDeployed());
            motionX = motionY = motionZ = 0.0D;
            return true;
        }
        return false;
    }

    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        if (!StrategicFeature.isEnabled()) return false;
        if (hand != EnumHand.MAIN_HAND) return true;
        if (world.isRemote) return true;
        ItemStack held = player.getHeldItem(hand);
        if (trySalvage(player, held, true)) return true;
        if (getOwnerTeam().isEmpty()) {
            setOwnerTeam(NetworkTeamHelper.getPlayerTeam(player));
        }
        if (!canOperate(player)) {
            tell(player, "IFF authorization denied.");
            return true;
        }
        if (!held.isEmpty()
                && held.getItem() instanceof StrategicMissileItem) {
            StrategicSystemProfile reload = ((StrategicMissileItem) held.getItem())
                    .getSystem(held);
            if (reload != getStrategicSystem()) {
                tell(player, "This canister does not fit "
                        + getStrategicSystem().getDisplayName() + ".");
            } else if (isMissileLoaded()) {
                tell(player, "Launch canister is already loaded.");
            } else if (getLaunchTicks() > 0 || getErectionProgress() > 0) {
                tell(player, "Retract the TEL before reloading.");
            } else {
                setMissileLoaded(true);
                if (!player.capabilities.isCreativeMode) held.shrink(1);
                tell(player, reload.getDisplayName() + " missile loaded.");
                world.playSound(null, posX, posY, posZ,
                        SoundEvents.BLOCK_ANVIL_USE,
                        SoundCategory.BLOCKS, 1.0F, 0.72F);
            }
            return true;
        }
        if (DesignatorCompat.isDesignator(held)) {
            BlockPos target = DesignatorCompat.getTarget(world, player, held);
            if (target == null) {
                tell(player, "Designator has no target coordinates.");
            } else {
                setGuidanceTarget(target.getX() + 0.5D,
                        target.getY() + 0.5D, target.getZ() + 0.5D);
                tell(player, "Strategic target loaded: " + target.getX()
                        + ", " + target.getY() + ", " + target.getZ());
            }
            return true;
        }
        if (player.isSneaking()) {
            if (getLaunchTicks() > 0) {
                tell(player, "Launch sequence cannot be interrupted.");
                return true;
            }
            setDeployed(!isDeployed());
            motionX = motionY = motionZ = 0.0D;
            world.playSound(null, posX, posY, posZ,
                    SoundEvents.BLOCK_ANVIL_USE,
                    SoundCategory.BLOCKS, 1.0F,
                    isDeployed() ? 0.65F : 1.1F);
            tell(player, isDeployed()
                    ? "TEL deployment started." : "TEL retraction started.");
            return true;
        }
        if (isDeployed()) {
            player.openGui(WarTechReforged.instance,
                    WarTechGuiHandler.GUI_STRATEGIC_TEL,
                    world, getEntityId(), 0, 0);
        } else {
            Entity driver = getControllingPassenger();
            if (driver == null || driver == player) {
                player.startRiding(this, true);
            } else {
                tell(player, "The driver's seat is occupied.");
            }
        }
        return true;
    }

    private static void tell(EntityPlayer player, String text) {
        if (player != null) player.sendMessage(new TextComponentString(text));
    }

    private boolean canOperate(EntityPlayer player) {
        return player.capabilities.isCreativeMode
                || getOwnerUuid() == null && getOwnerTeam().isEmpty()
                || isFriendlyOrOwner(player);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setInteger("StrategicErection", getErectionProgress());
        compound.setBoolean("StrategicLoaded", isMissileLoaded());
        compound.setInteger("StrategicLaunchTicks", getLaunchTicks());
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        dataManager.set(ERECTION, compound.getInteger("StrategicErection"));
        dataManager.set(LOADED, !compound.hasKey("StrategicLoaded")
                || compound.getBoolean("StrategicLoaded"));
        dataManager.set(LAUNCH_TICKS,
                compound.getInteger("StrategicLaunchTicks"));
    }
}
