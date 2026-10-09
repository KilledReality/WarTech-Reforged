package com.wartec.wartecmod.port.entity;

import com.wartec.wartecmod.WarTechReforged;
import com.wartec.wartecmod.port.content.WarTechContent;
import com.wartec.wartecmod.port.content.MissileProfile;
import com.wartec.wartecmod.port.content.PantsirAmmoBeltItem;
import com.wartec.wartecmod.port.gui.WarTechGuiHandler;
import com.wartec.wartecmod.port.integration.ElectronicWarfareService;
import com.wartec.wartecmod.port.integration.ITeamOwned;
import com.wartec.wartecmod.port.integration.MissileChunkLoader;
import com.wartec.wartecmod.port.integration.NetworkTeamHelper;
import com.wartec.wartecmod.port.integration.OwnerTeamNbt;
import com.wartec.wartecmod.port.integration.VlsInterceptorGuidance;
import com.wartec.wartecmod.port.network.FactionTerritoryData;
import com.wartec.wartecmod.port.network.MissileTrackingService;
import java.util.List;
import java.util.UUID;

import api.hbm.entity.IRadarDetectable;
import api.hbm.energy.IBatteryItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public abstract class EntityWarTechBase extends Entity
        implements IRadarDetectable, IInventory, ITeamOwned {
    private static final int HEALTH_SCHEMA = 3;
    private static final DataParameter<Integer> PROFILE =
            EntityDataManager.createKey(EntityWarTechBase.class, DataSerializers.VARINT);
    private static final DataParameter<Float> HEALTH =
            EntityDataManager.createKey(EntityWarTechBase.class, DataSerializers.FLOAT);
    private static final DataParameter<String> OWNER_UUID =
            EntityDataManager.createKey(EntityWarTechBase.class, DataSerializers.STRING);
    private static final DataParameter<String> OWNER_TEAM =
            EntityDataManager.createKey(EntityWarTechBase.class, DataSerializers.STRING);
    private static final DataParameter<Boolean> HAS_TARGET =
            EntityDataManager.createKey(EntityWarTechBase.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Float> TARGET_X =
            EntityDataManager.createKey(EntityWarTechBase.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> TARGET_Y =
            EntityDataManager.createKey(EntityWarTechBase.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> TARGET_Z =
            EntityDataManager.createKey(EntityWarTechBase.class, DataSerializers.FLOAT);
    private static final DataParameter<Boolean> ARMED =
            EntityDataManager.createKey(EntityWarTechBase.class, DataSerializers.BOOLEAN);
    private static final DataParameter<String> VISUAL_ID =
            EntityDataManager.createKey(EntityWarTechBase.class, DataSerializers.STRING);
    private static final DataParameter<Integer> VISUAL_VARIANT =
            EntityDataManager.createKey(EntityWarTechBase.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> LEGACY_POWER =
            EntityDataManager.createKey(EntityWarTechBase.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> LEGACY_STATE =
            EntityDataManager.createKey(EntityWarTechBase.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> LEGACY_CONTACTS =
            EntityDataManager.createKey(EntityWarTechBase.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> LEGACY_FIRE_MODE =
            EntityDataManager.createKey(EntityWarTechBase.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> LEGACY_SELECTED_PAYLOAD =
            EntityDataManager.createKey(EntityWarTechBase.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> LEGACY_SELECTED_HARDPOINT =
            EntityDataManager.createKey(EntityWarTechBase.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> LEGACY_FLAGS =
            EntityDataManager.createKey(EntityWarTechBase.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> LEGACY_PAYLOAD_MASK =
            EntityDataManager.createKey(EntityWarTechBase.class, DataSerializers.VARINT);

    private final WarTechEntityProfile defaultProfile;
    private final NonNullList<ItemStack> legacyInventory =
            NonNullList.withSize(14, ItemStack.EMPTY);
    private int operationalAge;
    private int legacyLaunchCooldown;
    private final int[] legacyBlips = new int[16];

    protected EntityWarTechBase(World world, WarTechEntityProfile defaultProfile) {
        super(world);
        this.defaultProfile = defaultProfile;
        this.dataManager.set(PROFILE, defaultProfile.ordinal());
        this.dataManager.set(HEALTH, defaultProfile.getMaxHealth());
        this.setSize(defaultProfile.getWidth(), defaultProfile.getHeight());
        this.stepHeight = defaultProfile.getType() == WarTechEntityType.GROUND_VEHICLE ? 1.0F : 0.0F;
    }

    @Override
    protected void entityInit() {
        // Entity invokes this before the subclass constructor assigns its family default.
        this.dataManager.register(PROFILE, WarTechEntityProfile.STORM_SHADOW.ordinal());
        this.dataManager.register(HEALTH, WarTechEntityProfile.STORM_SHADOW.getMaxHealth());
        this.dataManager.register(OWNER_UUID, "");
        this.dataManager.register(OWNER_TEAM, "");
        this.dataManager.register(HAS_TARGET, false);
        this.dataManager.register(TARGET_X, 0.0F);
        this.dataManager.register(TARGET_Y, 0.0F);
        this.dataManager.register(TARGET_Z, 0.0F);
        this.dataManager.register(ARMED, false);
        this.dataManager.register(VISUAL_ID, "");
        this.dataManager.register(VISUAL_VARIANT, 0);
        this.dataManager.register(LEGACY_POWER, 0);
        this.dataManager.register(LEGACY_STATE, 0);
        this.dataManager.register(LEGACY_CONTACTS, 0);
        this.dataManager.register(LEGACY_FIRE_MODE, 1);
        this.dataManager.register(LEGACY_SELECTED_PAYLOAD, 0);
        this.dataManager.register(LEGACY_SELECTED_HARDPOINT, -1);
        this.dataManager.register(LEGACY_FLAGS, 1);
        this.dataManager.register(LEGACY_PAYLOAD_MASK, 0);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.world.isRemote || this.isDead) {
            return;
        }

        if (com.wartec.wartecmod.port.integration.MissileChunkLoader.needsFlight(this)
                && !com.wartec.wartecmod.port.integration.MissileChunkLoader.flightReady(this)) return;
        this.operationalAge++;
        if(ticksExisted%40==0) com.wartec.wartecmod.port.integration.OperationalChunks.register(this);
        tickLegacySystems();
        this.serverTick(getProfile());
        this.velocityChanged = true;

        int lifetime = flightLifetime();
        if (lifetime > 0 && this.operationalAge >= lifetime) {
            onLifetimeExpired();
        }
    }

    protected abstract void serverTick(WarTechEntityProfile profile);

    protected int flightLifetime() { return getProfile().getMaxLifetime(); }

    protected void onLifetimeExpired() {
        if (isArmed() && getProfile().getExplosionStrength() > 0.0F) {
            explodeAndRemove();
        } else {
            setDead();
        }
    }

    public WarTechEntityProfile getProfile() {
        return WarTechEntityProfile.byOrdinal(this.dataManager.get(PROFILE), this.defaultProfile);
    }
    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if(PROFILE.equals(key)) setSize(getProfile().getWidth(),getProfile().getHeight());
        if(VISUAL_VARIANT.equals(key) && getProfile()==WarTechEntityProfile.ELECTRONIC_WARFARE) {
            int mode=getVisualVariant();float scale=VehicleDimensions.scale(getProfile());
            setSize((mode==2?1.4F:2.4F)*scale,(mode==1?4:mode==2?2:3.2F)*scale);
        }
    }

    public final void setProfile(WarTechEntityProfile profile) {
        if (profile == null || profile.getType() != getEntityType()) {
            throw new IllegalArgumentException("Profile does not belong to " + getEntityType());
        }
        this.dataManager.set(PROFILE, profile.ordinal());
        this.dataManager.set(HEALTH, profile.getMaxHealth());
        this.setSize(profile.getWidth(), profile.getHeight());
    }

    public abstract WarTechEntityType getEntityType();

    public float getHealthValue() {
        return this.dataManager.get(HEALTH);
    }

    protected void setHealthValue(float value) {
        this.dataManager.set(HEALTH, MathHelper.clamp(value, 0.0F,
                getMaximumHealthValue()));
    }

    protected float getMaximumHealthValue() {
        return getProfile().getMaxHealth();
    }

    public float getHealthCapacity() { return getMaximumHealthValue(); }
    public int getDefenseEngagementRange() {
        int range=isTor()?220:100;
        return getHealthValue()<getHealthCapacity()*.5F?(int)Math.round(range*.72):range;
    }

    public boolean isDefenseVehicle() {
        WarTechEntityProfile p=getProfile();
        return p==WarTechEntityProfile.MOBILE_AIR_DEFENSE || p==WarTechEntityProfile.RADAR_TRUCK
            || p==WarTechEntityProfile.S400_RADAR || p==WarTechEntityProfile.COMMAND_TRUCK;
    }

    /** Temporary field maintenance: one ingot repairs 10%, never repairs enemies or consumes at full HP. */
    public boolean tryRepairDefense(EntityPlayer player, ItemStack held) {
        if(!isDefenseVehicle() || held.isEmpty() || held.getItem()!=net.minecraft.init.Items.IRON_INGOT) return false;
        if(world.isRemote) return true;
        String key="wartec.repair.done";
        if(isDead || getHealthValue()<=0 || !isUsableByPlayer(player)
                || !(player.getUniqueID().equals(getOwnerUuid())
                    || NetworkTeamHelper.areFriendly(getOwnerTeam(),NetworkTeamHelper.getPlayerTeam(player))
                    || getOwnerUuid()==null && getOwnerTeam().isEmpty())) key="wartec.repair.denied";
        else if(getHealthValue()>=getHealthCapacity()-.01F) key="wartec.repair.full";
        else {
            setHealthValue(Math.min(getHealthCapacity(),getHealthValue()+Math.max(20,getHealthCapacity()*.1F)));
            if(!player.capabilities.isCreativeMode) held.shrink(1);
            playLegacySound("minecraft:block.anvil.use",SoundEvents.BLOCK_ANVIL_USE,.5F,1.3F);
        }
        player.sendMessage(new net.minecraft.util.text.TextComponentTranslation(key,
            Math.round(getHealthValue()),Math.round(getHealthCapacity())));
        return true;
    }

    public void setOwner(EntityPlayer player) {
        if (player == null) {
            setOwnerIdentity(null, "");
            return;
        }
        setOwnerIdentity(player.getUniqueID(), NetworkTeamHelper.getPlayerTeam(player));
    }

    public void setOwnerIdentity(UUID ownerUuid, String team) {
        this.dataManager.set(OWNER_UUID, ownerUuid == null ? "" : ownerUuid.toString());
        this.dataManager.set(OWNER_TEAM, sanitizeTeam(team));
    }

    public UUID getOwnerUuid() {
        String value = this.dataManager.get(OWNER_UUID);
        if (value.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public EntityPlayer getOwner() {
        UUID ownerUuid = getOwnerUuid();
        return ownerUuid == null ? null : this.world.getPlayerEntityByUUID(ownerUuid);
    }

    public String getOwnerTeam() {
        return this.dataManager.get(OWNER_TEAM);
    }

    public void setOwnerTeam(String team) {
        this.dataManager.set(OWNER_TEAM, sanitizeTeam(team));
    }

    public void setGuidanceTarget(double x, double y, double z) {
        this.dataManager.set(TARGET_X, (float) x);
        this.dataManager.set(TARGET_Y, (float) y);
        this.dataManager.set(TARGET_Z, (float) z);
        this.dataManager.set(HAS_TARGET, true);
    }

    public void clearGuidanceTarget() {
        this.dataManager.set(HAS_TARGET, false);
    }

    public boolean hasGuidanceTarget() {
        return this.dataManager.get(HAS_TARGET);
    }

    public double getTargetX() {
        return this.dataManager.get(TARGET_X);
    }

    public double getTargetY() {
        return this.dataManager.get(TARGET_Y);
    }

    public double getTargetZ() {
        return this.dataManager.get(TARGET_Z);
    }

    public boolean isArmed() {
        return this.dataManager.get(ARMED);
    }

    public void setArmed(boolean armed) {
        this.dataManager.set(ARMED, armed);
    }

    public void setVisual(String visualId, int variant) {
        this.dataManager.set(VISUAL_ID, visualId == null ? "" : visualId);
        this.dataManager.set(VISUAL_VARIANT, Math.max(0, variant));
        if (getProfile() == WarTechEntityProfile.ELECTRONIC_WARFARE) {
            int mode = Math.max(0, variant);
            float scale=VehicleDimensions.scale(getProfile());
            setSize((mode == 2 ? 1.4F : 2.4F)*scale,
                    (mode == 1 ? 4.0F : mode == 2 ? 2.0F : 3.2F)*scale);
        }
    }

    public String getVisualId() {
        return this.dataManager.get(VISUAL_ID);
    }

    public int getVisualVariant() {
        return this.dataManager.get(VISUAL_VARIANT);
    }

    protected int getOperationalAge() {
        return operationalAge;
    }

    protected double distanceSqToTarget() {
        if (!hasGuidanceTarget()) {
            return Double.MAX_VALUE;
        }
        double dx = getTargetX() - this.posX;
        double dy = getTargetY() - this.posY;
        double dz = getTargetZ() - this.posZ;
        return dx * dx + dy * dy + dz * dz;
    }

    protected void steerToward(double targetX, double targetY, double targetZ, WarTechEntityProfile profile,
            boolean includeVertical) {
        double dx = targetX - this.posX;
        double dy = includeVertical ? targetY - this.posY : 0.0D;
        double dz = targetZ - this.posZ;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-5D) {
            return;
        }

        double desiredX = dx / length * profile.getSpeed();
        double desiredY = dy / length * profile.getSpeed();
        double desiredZ = dz / length * profile.getSpeed();
        double blend = MathHelper.clamp(profile.getTurnRate(), 0.0D, 1.0D);
        double maxChange = Math.max(profile.getAcceleration(), 1.0E-4D);

        this.motionX += MathHelper.clamp((desiredX - this.motionX) * blend, -maxChange, maxChange);
        if (includeVertical) {
            this.motionY += MathHelper.clamp((desiredY - this.motionY) * blend, -maxChange, maxChange);
        }
        this.motionZ += MathHelper.clamp((desiredZ - this.motionZ) * blend, -maxChange, maxChange);
        updateRotationFromMotion();
    }

    protected void moveWithCurrentMotion() {
        this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
    }

    protected void updateRotationFromMotion() {
        double horizontal = Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
        if (horizontal > 1.0E-5D) {
            this.rotationYaw = (float) (MathHelper.atan2(this.motionZ, this.motionX) * 180.0D / Math.PI) - 90.0F;
            this.rotationPitch = (float) -(MathHelper.atan2(this.motionY, horizontal) * 180.0D / Math.PI);
        }
    }

    protected Entity findImpactEntity(double grow) {
        AxisAlignedBB searchBox = this.getEntityBoundingBox()
                .expand(this.motionX, this.motionY, this.motionZ)
                .grow(grow);
        List<Entity> entities = this.world.getEntitiesWithinAABBExcludingEntity(this, searchBox);
        for (Entity entity : entities) {
            if (entity.canBeCollidedWith() && !isFriendlyOrOwner(entity)) {
                return entity;
            }
        }
        return null;
    }

    protected boolean isFriendlyOrOwner(Entity entity) {
        if(entity==null) return false;
        String otherTeam=NetworkTeamHelper.getEntityTeam(entity);
        // Team snapshots on deployed vehicles/ordnance take precedence over the
        // placing player's UUID after that player joins the opposing faction.
        if(!getOwnerTeam().isEmpty() && !otherTeam.isEmpty()) return getOwnerTeam().equals(otherTeam);
        UUID ownerUuid = getOwnerUuid();
        if (ownerUuid != null && ownerUuid.equals(entity.getUniqueID())) {
            return true;
        }
        String team = getOwnerTeam();
        if (team.isEmpty()) {
            return false;
        }
        if (entity instanceof EntityWarTechBase) {
            return team.equals(((EntityWarTechBase) entity).getOwnerTeam());
        }
        Team entityTeam = entity.getTeam();
        return entityTeam != null && team.equals(entityTeam.getName());
    }

    protected void explodeAndRemove() {
        if (this.isDead) {
            return;
        }
        WarTechEntityProfile profile = getProfile();
        this.world.createExplosion(this, this.posX, this.posY, this.posZ, profile.getExplosionStrength(), true);
        this.setDead();
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (com.wartec.wartecmod.port.integration.StrikeBlastSafety.ignores(this, source)) return false;
        if (this.world.isRemote || this.isDead || this.isEntityInvulnerable(source)) {
            return false;
        }
        Entity attacker = source.getTrueSource();
        if (attacker != null && isFriendlyOrOwner(attacker)) {
            return false;
        }
        float health = getHealthValue() - Math.max(0.0F, amount);
        this.dataManager.set(HEALTH, health);
        if (health <= 0.0F) {
            if (isArmed() && getProfile().getExplosionStrength() > 0.0F) {
                explodeAndRemove();
            } else {
                setDead();
            }
        }
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return !this.isDead;
    }

    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        if (getEntityType() != WarTechEntityType.AIRCRAFT
            && getEntityType() != WarTechEntityType.GROUND_VEHICLE) {
            return false;
        }
        if (hand != EnumHand.MAIN_HAND) return true;
        if (!world.isRemote) {
            ItemStack held = player.getHeldItem(hand);
            if (tryRepairDefense(player,held)) return true;
            if (trySalvage(player, held, true)) {
                return true;
            }
            if (!held.isEmpty() && held.getItem() instanceof IBatteryItem) {
                chargeFromHeldBattery(held);
                return true;
            }
            if (getProfile() == WarTechEntityProfile.ELECTRONIC_WARFARE) {
                if (player.isSneaking()) {
                    setLegacyEnabled(!isLegacyEnabled());
                    if (!isLegacyEnabled()) {
                        ElectronicWarfareService.removeNode(
                                world, getEntityId());
                    }
                    playLegacySound("hbm:item.techBleep",
                            SoundEvents.BLOCK_NOTE_PLING, 0.8F,
                            isLegacyEnabled() ? 1.25F : 0.75F);
                    player.sendMessage(new TextComponentString(
                            isLegacyEnabled()
                                    ? "EW unit online."
                                    : "EW unit offline."));
                    return true;
                }
                if (getVisualVariant() == 0
                        || getVisualVariant() == 2) {
                    setLegacySelectedPayload((getLegacySelectedPayload() + 1) % 4);
                }
                String status = isLegacyEnabled() ? "ONLINE" : "OFFLINE";
                if (getVisualVariant() == 1) {
                    player.sendMessage(new TextComponentString(
                            "Passive ESM | emitters: " + getLegacyContacts()
                                    + " | range: 900 | " + status));
                } else {
                    String mode = getVisualVariant() == 0
                            ? "Synytsia jammer" : "Radar decoy";
                    player.sendMessage(new TextComponentString(
                            mode + " | band: "
                                    + ElectronicWarfareService.bandName(
                                            getLegacySelectedPayload())
                                    + " | " + status));
                }
                return true;
            }
            if (getProfile() == WarTechEntityProfile.COMMAND_TRUCK
                    && player.isSneaking()) {
                boolean deploy = !isDeployed();
                if (deploy) {
                    if (getOwnerTeam().isEmpty()) {
                        setOwnerTeam(NetworkTeamHelper.getPlayerTeam(player));
                    }
                    int claim = FactionTerritoryData.claimAt(world,
                            getOwnerTeam(), posX, posZ);
                    if (claim == FactionTerritoryData.CONFLICT) {
                        player.sendMessage(new TextComponentString(
                                "Sector belongs to another IFF faction."));
                        return true;
                    }
                    if (claim == FactionTerritoryData.INVALID_TEAM) {
                        player.sendMessage(new TextComponentString(
                                "Assign an IFF faction before deployment."));
                        return true;
                    }
                    if (claim == FactionTerritoryData.LIMIT_REACHED) {
                        player.sendMessage(new TextComponentString(
                                "Faction sector limit reached."));
                        return true;
                    }
                    if (claim == FactionTerritoryData.CLAIMED) {
                        player.sendMessage(new TextComponentString(
                                "Faction defense sector "
                                        + FactionTerritoryData.getSectorX(posX)
                                        + ":" + FactionTerritoryData.getSectorZ(posZ)
                                        + " claimed."));
                    }
                }
                setDeployed(deploy);
                motionX = motionY = motionZ = 0.0D;
                playLegacySound(SoundEvents.BLOCK_ANVIL_USE,
                        0.75F, deploy ? 0.78F : 1.12F);
                player.sendMessage(new TextComponentString(deploy
                        ? "Command post deployed."
                        : "Command post retracted."));
                return true;
            }
            if (getProfile() == WarTechEntityProfile.MOBILE_AIR_DEFENSE
                    && player.isSneaking()) {
                boolean deploy = !isDeployed();
                setDeployed(deploy);
                motionX = motionY = motionZ = 0.0D;
                playLegacySound(SoundEvents.BLOCK_ANVIL_USE,
                        0.75F, deploy ? 0.78F : 1.12F);
                player.sendMessage(new TextComponentString(
                        (isTor() ? "9K331 Tor-M1" : "96K6 Pantsir-S2")
                                + (deploy ? " deployed." : " retracted.")));
                return true;
            }
            if (getProfile() == WarTechEntityProfile.RADAR_TRUCK
                    && player.isSneaking()) {
                setLegacyEnabled(!isLegacyEnabled());
                playLegacySound("hbm:item.techBleep",
                        SoundEvents.BLOCK_NOTE_PLING, 0.8F,
                        isLegacyEnabled() ? 1.25F : 0.75F);
                return true;
            }
            if (getProfile() == WarTechEntityProfile.S400_RADAR
                    && player.isSneaking()) {
                setDeployed(!isDeployed());
                setLegacyEnabled(isDeployed());
                motionX = motionY = motionZ = 0.0D;
                playLegacySound(SoundEvents.BLOCK_ANVIL_USE,
                        0.75F, isDeployed() ? 0.75F : 1.15F);
                return true;
            }
            if ((getProfile() == WarTechEntityProfile.COMMAND_TRUCK
                    || getProfile() == WarTechEntityProfile.MOBILE_AIR_DEFENSE)
                    && !isDeployed()) {
                mountDriver(player);
                return true;
            }
            int guiId = WarTechGuiHandler.guiForEntity(this);
            if (guiId < 0) return false;
            player.openGui(
                WarTechReforged.instance,
                guiId,
                world,
                getEntityId(),
                0,
                0
            );
        }
        return true;
    }

    protected boolean trySalvage(EntityPlayer player, ItemStack held,
            boolean grounded) {
        if (player == null || held.isEmpty()
                || held.getItem() != WarTechContent.WARTEC_SALVAGE_WRENCH
                || !player.isSneaking()) {
            return false;
        }
        if (!grounded) {
            player.sendMessage(new TextComponentString(
                    "Aircraft must be landed before dismantling."));
            return true;
        }
        ItemStack recovery = createRecoveryStack();
        if (recovery.isEmpty()) {
            return false;
        }
        if (!player.capabilities.isCreativeMode) {
            for (int index = 0; index < getSizeInventory(); ++index) {
                ItemStack stored = removeStackFromSlot(index);
                if (!stored.isEmpty()) {
                    entityDropItem(stored, 0.6F);
                }
            }
            OwnerTeamNbt.write(recovery, getOwnerTeam());
            entityDropItem(recovery, 0.6F);
        }
        MissileChunkLoader.untrack(this);
        world.playSound(null, posX, posY, posZ,
                SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS,
                0.8F, 1.35F);
        setDead();
        return true;
    }

    private ItemStack createRecoveryStack() {
        switch (getProfile()) {
            case MQ_9_REAPER:
                return new ItemStack(WarTechContent.MQ9_REAPER_DRONE);
            case F_16C:
                return new ItemStack(WarTechContent.TACTICAL_AIRCRAFT);
            case SU_27:
                return new ItemStack(
                        WarTechContent.SU27_TACTICAL_AIRCRAFT);
            case TU_95:
                return new ItemStack(
                        WarTechContent.TU95_STRATEGIC_BOMBER);
            case RADAR_TRUCK:
                return new ItemStack(
                        WarTechContent.MOBILE_RADAR_TRUCK);
            case S400_RADAR:
                return new ItemStack(
                        WarTechContent.S400_LONG_RANGE_RADAR);
            case COMMAND_TRUCK:
                return new ItemStack(
                        WarTechContent.AIR_DEFENSE_COMMAND_TRUCK);
            case ELECTRONIC_WARFARE:
                return new ItemStack(
                        WarTechContent.ELECTRONIC_WARFARE_UNIT,
                        1, getVisualVariant());
            case MOBILE_AIR_DEFENSE:
                return new ItemStack(
                        WarTechContent.MOBILE_AIR_DEFENSE_SYSTEM,
                        1, getVisualVariant());
            case MOBILE_ARTILLERY:
                return new ItemStack(
                        WarTechContent.MOBILE_ARTILLERY,
                        1, getVisualVariant());
            case STRATEGIC_TOPOL_M:
            case STRATEGIC_YARS:
            case STRATEGIC_ORESHNIK:
                ItemStack strategic = new ItemStack(
                        getProfile() == WarTechEntityProfile.STRATEGIC_TOPOL_M
                                ? WarTechContent.TOPOL_M_TEL
                                : getProfile() == WarTechEntityProfile.STRATEGIC_YARS
                                        ? WarTechContent.YARS_TEL
                                        : WarTechContent.ORESHNIK_TEL);
                if (this instanceof EntityStrategicTel) {
                    strategic.setTagInfo("StrategicLoaded",
                            new net.minecraft.nbt.NBTTagByte(
                                    (byte) (((EntityStrategicTel) this)
                                            .isMissileLoaded() ? 1 : 0)));
                }
                return strategic;
            default:
                return ItemStack.EMPTY;
        }
    }

    private void mountDriver(EntityPlayer player) {
        Entity current = getControllingPassenger();
        if (current == null || current == player) {
            player.startRiding(this, true);
        } else {
            player.sendMessage(new TextComponentString("The driver's seat is occupied."));
        }
    }

    private void chargeFromHeldBattery(ItemStack stack) {
        IBatteryItem battery = (IBatteryItem) stack.getItem();
        long amount = Math.min(Math.min(battery.getCharge(stack), battery.getDischargeRate()),
                getEnergyCapacity() - getLegacyPower());
        if (amount > 0L) {
            battery.dischargeBattery(stack, amount);
            setLegacyPower(getLegacyPower() + (int) amount);
        }
    }

    protected void playLegacySound(SoundEvent sound, float volume, float pitch) {
        world.playSound(null, posX, posY, posZ, sound,
                SoundCategory.BLOCKS, volume, pitch);
    }

    protected void playLegacySound(String id, SoundEvent fallback,
            float volume, float pitch) {
        SoundEvent sound = SoundEvent.REGISTRY.getObject(new ResourceLocation(id));
        playLegacySound(sound == null ? fallback : sound, volume, pitch);
    }

    @Override
    public RadarTargetType getTargetType() {
        return getProfile().getRadarTargetType();
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        compound.setInteger("WarTechProfile", getProfile().ordinal());
        compound.setInteger("WarTechHealthSchema", HEALTH_SCHEMA);
        compound.setFloat("WarTechHealth", getHealthValue());
        compound.setString("WarTechOwner", this.dataManager.get(OWNER_UUID));
        compound.setString("WarTechOwnerTeam", getOwnerTeam());
        compound.setBoolean("WarTechHasTarget", hasGuidanceTarget());
        compound.setDouble("WarTechTargetX", getTargetX());
        compound.setDouble("WarTechTargetY", getTargetY());
        compound.setDouble("WarTechTargetZ", getTargetZ());
        compound.setBoolean("WarTechArmed", isArmed());
        compound.setString("WarTechVisual", getVisualId());
        compound.setInteger("WarTechVisualVariant", getVisualVariant());
        compound.setInteger("WarTechOperationalAge", this.operationalAge);
        compound.setInteger("LegacyPower", getLegacyPower());
        compound.setInteger("LegacyState", getLegacyState());
        compound.setInteger("LegacyContacts", getLegacyContacts());
        compound.setInteger("LegacyFireMode", getLegacyFireMode());
        compound.setInteger("LegacySelectedPayload", getLegacySelectedPayload());
        compound.setInteger("LegacySelectedHardpoint", getLegacySelectedHardpoint());
        compound.setInteger("LegacyFlags", getLegacyFlags());
        ItemStackHelper.saveAllItems(compound, legacyInventory);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        WarTechEntityProfile loaded = WarTechEntityProfile.byOrdinal(
                compound.getInteger("WarTechProfile"), this.defaultProfile);
        if (loaded.getType() != getEntityType()) {
            loaded = this.defaultProfile;
        }
        setProfile(loaded);
        if (compound.hasKey("WarTechHealth", 5)) {
            float health = compound.getFloat("WarTechHealth");
            if (compound.getInteger("WarTechHealthSchema") < HEALTH_SCHEMA) {
                float oldMaximum = compound.getInteger("WarTechHealthSchema")<2
                    ? legacyPortMaximumHealth(loaded) : previousDefenseMaximumHealth(loaded);
                if (oldMaximum > 0.0F) {
                    health *= loaded.getMaxHealth() / oldMaximum;
                }
            }
            this.dataManager.set(HEALTH,
                    MathHelper.clamp(health, 0.0F, loaded.getMaxHealth()));
        }
        this.dataManager.set(OWNER_UUID, compound.getString("WarTechOwner"));
        this.dataManager.set(OWNER_TEAM, sanitizeTeam(compound.getString("WarTechOwnerTeam")));
        if (compound.getBoolean("WarTechHasTarget")) {
            setGuidanceTarget(compound.getDouble("WarTechTargetX"), compound.getDouble("WarTechTargetY"),
                    compound.getDouble("WarTechTargetZ"));
        } else {
            clearGuidanceTarget();
        }
        setArmed(compound.getBoolean("WarTechArmed"));
        setVisual(compound.getString("WarTechVisual"),
                compound.getInteger("WarTechVisualVariant"));
        this.operationalAge = Math.max(0, compound.getInteger("WarTechOperationalAge"));
        setLegacyPower(compound.getInteger("LegacyPower"));
        setLegacyState(compound.getInteger("LegacyState"));
        setLegacyContacts(compound.getInteger("LegacyContacts"));
        setLegacyFireMode(compound.getInteger("LegacyFireMode"));
        setLegacySelectedPayload(compound.getInteger("LegacySelectedPayload"));
        setLegacySelectedHardpoint(compound.getInteger("LegacySelectedHardpoint"));
        setLegacyFlags(compound.hasKey("LegacyFlags") ? compound.getInteger("LegacyFlags") : 1);
        ItemStackHelper.loadAllItems(compound, legacyInventory);
        updatePayloadMask();
    }

    private static float legacyPortMaximumHealth(WarTechEntityProfile profile) {
        switch (profile) {
            case MQ_9_REAPER:
                return 40.0F;
            case F_16C:
                return 70.0F;
            case SU_27:
                return 82.0F;
            case TU_95:
                return 150.0F;
            case COMMAND_TRUCK:
                return 90.0F;
            case RADAR_TRUCK:
                return 100.0F;
            case MOBILE_AIR_DEFENSE:
                return 125.0F;
            case MOBILE_ARTILLERY:
                return 135.0F;
            case ELECTRONIC_WARFARE:
                return 105.0F;
            case S400_RADAR:
                return 120.0F;
            default:
                return profile.getMaxHealth();
        }
    }

    private static float previousDefenseMaximumHealth(WarTechEntityProfile profile) {
        switch(profile) {
            case MOBILE_AIR_DEFENSE:return 500;
            case RADAR_TRUCK:return 300;
            case S400_RADAR:return 600;
            case COMMAND_TRUCK:return 720;
            default:return profile.getMaxHealth();
        }
    }

    private void tickLegacySystems() {
        if (legacyLaunchCooldown > 0) {
            --legacyLaunchCooldown;
        }
        chargeFromBattery(getBatterySlot());
        WarTechEntityProfile profile = getProfile();
        if (profile == WarTechEntityProfile.RADAR_TRUCK) {
            tickLegacyRadar(600, 500);
            updateRadarEmitter(ElectronicWarfareService.BAND_S);
        } else if (profile == WarTechEntityProfile.S400_RADAR) {
            if (isDeployed()) {
                tickLegacyRadar(1200, 900);
                updateRadarEmitter(ElectronicWarfareService.BAND_L);
            } else {
                setLegacyContacts(0);
                setLegacyOperational(false);
                ElectronicWarfareService.removeNode(world, getEntityId());
            }
        } else if (profile == WarTechEntityProfile.MOBILE_AIR_DEFENSE) {
            if (isDeployed()) {
                tickLegacyRadar(isTor() ? 340 : 260, isTor() ? 260 : 190);
                updateRadarEmitter(ElectronicWarfareService.BAND_X);
            } else {
                setLegacyContacts(0);
                setLegacyOperational(false);
                ElectronicWarfareService.removeNode(world, getEntityId());
            }
        } else if (profile == WarTechEntityProfile.COMMAND_TRUCK) {
            boolean active = isDeployed() && getLegacyPower() >= 120;
            setLegacyOperational(active);
            if (active) {
                setLegacyPower(getLegacyPower() - 120);
                if (ticksExisted % 200 == Math.abs(getEntityId()) % 200) {
                    FactionTerritoryData.claimAt(world, getOwnerTeam(), posX, posZ);
                }
                ElectronicWarfareService.updateEmitter(world, getEntityId(),
                        posX, posY + 2.0D, posZ,
                        ElectronicWarfareService.EMITTER_COMMAND,
                        ElectronicWarfareService.BAND_X, getOwnerTeam());
                if (ticksExisted % 10 == Math.abs(getEntityId()) % 10) {
                    MissileTrackingService.CommandSnapshot snapshot =
                            MissileTrackingService.updateCommandPost(world,
                                    getEntityId(), posX, posY + 2.0D, posZ,
                                    getOwnerTeam());
                    setLegacyContacts(snapshot.contacts);
                }
            } else {
                ElectronicWarfareService.removeNode(world, getEntityId());
                MissileTrackingService.removeCommandPost(world, getEntityId());
                setLegacyContacts(0);
            }
        } else if (profile == WarTechEntityProfile.ELECTRONIC_WARFARE) {
            int use = getVisualVariant() == 0 ? 70 : getVisualVariant() == 1 ? 18 : 8;
            boolean active = isLegacyEnabled() && getLegacyPower() >= use;
            setLegacyOperational(active);
            if (active) {
                setLegacyPower(getLegacyPower() - use);
                int band = MathHelper.clamp(getLegacySelectedPayload(), 0, 3);
                if (getVisualVariant() == 0) {
                    ElectronicWarfareService.updateJammer(world, getEntityId(),
                            posX, posY + 1.5D, posZ, band, getOwnerTeam());
                    setLegacyContacts(ElectronicWarfareService.updatePassiveSweep(
                            world, posX, posY, posZ, 450.0D, getOwnerTeam()));
                } else if (getVisualVariant() == 1) {
                    ElectronicWarfareService.removeNode(world, getEntityId());
                    setLegacyContacts(ElectronicWarfareService.updatePassiveSweep(
                            world, posX, posY + 2.0D, posZ, 900.0D, getOwnerTeam()));
                } else {
                    ElectronicWarfareService.updateEmitter(world, getEntityId(),
                            posX, posY + 1.0D, posZ,
                            ElectronicWarfareService.EMITTER_DECOY,
                            band, getOwnerTeam());
                    setLegacyContacts(0);
                }
            } else {
                setLegacyContacts(0);
                ElectronicWarfareService.removeNode(world, getEntityId());
            }
        }
    }

    private void updateRadarEmitter(int band) {
        if (isLegacyOperational()) {
            ElectronicWarfareService.updateEmitter(world, getEntityId(),
                    posX, posY + 3.0D, posZ,
                    ElectronicWarfareService.EMITTER_RADAR,
                    band, getOwnerTeam());
        } else {
            ElectronicWarfareService.removeNode(world, getEntityId());
        }
    }

    @Override
    public void setDead() {
        if (world != null && !world.isRemote) {
            com.wartec.wartecmod.port.integration.OperationalChunks.forget(this);
            ElectronicWarfareService.removeNode(world, getEntityId());
            MissileTrackingService.removeRadar(world, getEntityId());
            MissileTrackingService.removeCommandPost(world, getEntityId());
            MissileChunkLoader.untrack(this);
        }
        super.setDead();
    }

    private int countHostileEmitters(double range) {
        AxisAlignedBB box = getEntityBoundingBox().grow(range, 256.0D, range);
        int count = 0;
        for (Entity entity : world.getEntitiesWithinAABBExcludingEntity(this, box)) {
            if (!(entity instanceof EntityWarTechBase) || isFriendlyOrOwner(entity)) continue;
            WarTechEntityProfile profile = ((EntityWarTechBase) entity).getProfile();
            if (profile == WarTechEntityProfile.RADAR_TRUCK
                    || profile == WarTechEntityProfile.S400_RADAR
                    || profile == WarTechEntityProfile.MOBILE_AIR_DEFENSE
                    || profile == WarTechEntityProfile.COMMAND_TRUCK) ++count;
        }
        return count;
    }

    private void tickLegacyRadar(int range, int ceiling) {
        int use = getProfile() == WarTechEntityProfile.S400_RADAR ? 300
                : getProfile() == WarTechEntityProfile.MOBILE_AIR_DEFENSE
                        ? (isTor() ? 180 : 220) : 100;
        if (!isLegacyEnabled() || getLegacyPower() < use
                || getProfile() == WarTechEntityProfile.MOBILE_AIR_DEFENSE
                        && getHealthValue() <= getHealthCapacity()*.20F) {
            setLegacyContacts(0);
            setLegacyOperational(false);
            MissileTrackingService.removeRadar(world, getEntityId());
            return;
        }
        setLegacyPower(getLegacyPower() - use);
        setLegacyOperational(true);
        if (getProfile() == WarTechEntityProfile.MOBILE_AIR_DEFENSE
                && ticksExisted % 10 == Math.abs(getEntityId()) % 10) {
            MissileTrackingService.updateLauncherPresence(world,
                    posX, posY + 2.4D, posZ,
                    isTor() ? 2 : 1, getLauncherKey(), getOwnerTeam());
        }
        int sweepPeriod = getProfile() == WarTechEntityProfile.MOBILE_AIR_DEFENSE
                ? (isTor() ? 5 : 3) : 10;
        if (ticksExisted % sweepPeriod == Math.abs(getEntityId()) % sweepPeriod) {
            int contactLimit = getProfile() == WarTechEntityProfile.MOBILE_AIR_DEFENSE
                    ? (isTor() ? 12 : 10)
                    : getProfile() == WarTechEntityProfile.S400_RADAR
                            ? 32 : Integer.MAX_VALUE;
            double scanY = getProfile() == WarTechEntityProfile.RADAR_TRUCK
                    ? 2.5D : getProfile() == WarTechEntityProfile.S400_RADAR
                            ? 4.0D : 2.8D;
            int band = getProfile() == WarTechEntityProfile.RADAR_TRUCK
                    ? ElectronicWarfareService.BAND_S
                    : getProfile() == WarTechEntityProfile.S400_RADAR
                            ? ElectronicWarfareService.BAND_L
                            : ElectronicWarfareService.BAND_X;
            int count = MissileTrackingService.updateRadarSweep(world, getEntityId(),
                    posX, posY + scanY, posZ, range, ceiling,
                    contactLimit, getOwnerTeam(), band);
            int blipLimit = getProfile() == WarTechEntityProfile.MOBILE_AIR_DEFENSE
                    ? 5 : 8;
            int[] blips = MissileTrackingService.getRadarBlips(world, getEntityId(),
                    posX, posZ, blipLimit);
            for (int index = 0; index < legacyBlips.length; ++index) {
                legacyBlips[index] = index < blips.length ? blips[index] : 0;
            }
            setLegacyContacts(count);
        }
        if (getProfile() == WarTechEntityProfile.MOBILE_AIR_DEFENSE) {
            int engagementRange = getDefenseEngagementRange();
            if (getLegacyFireMode() != 0
                    && legacyLaunchCooldown == 0 && getLegacyPower() >= 50000) {
                Entity target = MissileTrackingService.findThreat(world,
                        posX, posY + 3.0D, posZ, isTor() ? 2 : 1,
                        engagementRange, getLauncherKey(), getOwnerTeam());
                if (target != null) {
                    launchLegacyInterceptor(target);
                }
            }
        }
    }

    private long getLauncherKey() {
        return 0x4D41445300000000L | (long) getEntityId() & 0xFFFFFFFFL;
    }

    private void launchLegacyInterceptor(Entity target) {
        int capacity = isTor() ? 8 : 12;
        int slot = -1;
        for (int index = 0; index < capacity; ++index) {
            if (isRequiredInterceptor(legacyInventory.get(index))) {
                slot = index;
                break;
            }
        }
        if (slot < 0) return;
        long launcherKey = getLauncherKey();
        if (!MissileTrackingService.tryReserve(world, target.getEntityId(), launcherKey)) {
            return;
        }
        EntityWarTechMissile missile = LegacyEntityFactory.missile(world,
                isTor() ? MissileProfile.ANTI_AIR_TIER_2
                        : MissileProfile.ANTI_AIR_TIER_1);
        double yaw = Math.toRadians(rotationYaw);
        missile.setPosition(posX - Math.sin(yaw) * (isTor() ? 0.0D : 0.35D),
                posY + (isTor() ? 3.5D : 3.25D),
                posZ + Math.cos(yaw) * (isTor() ? 0.0D : 0.35D));
        missile.setOwnerIdentity(getOwnerUuid(), getOwnerTeam());
        missile.setVisual("missile/anti_air_tier_" + (isTor() ? "2" : "1"), 0);
        boolean malfunction = VlsInterceptorGuidance.configureMobileLaunch(
                missile, target, isTor() ? 2 : 1, isTor());
        if (com.wartec.wartecmod.port.integration.MissileChunkLoader.spawnFlight(missile)) {
            if (malfunction) {
                MissileTrackingService.releaseReservation(world,
                        target.getEntityId(), launcherKey);
                MissileTrackingService.deferTarget(world, target.getEntityId());
            } else {
                MissileTrackingService.confirmReservation(world,
                        target.getEntityId(), launcherKey, missile.getEntityId());
            }
            VlsInterceptorGuidance.spawnLaunchSmoke(
                    world, missile.posX, missile.posY, missile.posZ,
                    isTor() ? 2 : 1);
            legacyInventory.set(slot, ItemStack.EMPTY);
            setLegacyPower(getLegacyPower() - 50000);
            legacyLaunchCooldown = isTor() ? (getLegacyFireMode() == 2 ? 3 : 8) : 4;
            markDirty();
        } else {
            MissileTrackingService.releaseReservation(world,
                    target.getEntityId(), launcherKey);
        }
    }

    private void chargeFromBattery(int slot) {
        if (slot < 0 || slot >= legacyInventory.size()
                || getLegacyPower() >= getEnergyCapacity()) {
            return;
        }
        ItemStack stack = legacyInventory.get(slot);
        if (stack.isEmpty() || !(stack.getItem() instanceof IBatteryItem)) {
            return;
        }
        IBatteryItem battery = (IBatteryItem) stack.getItem();
        long available = battery.getCharge(stack);
        long amount = Math.min(Math.min(available, battery.getDischargeRate()),
                getEnergyCapacity() - getLegacyPower());
        if (amount > 0L) {
            battery.dischargeBattery(stack, amount);
            setLegacyPower(getLegacyPower() + (int) amount);
            markDirty();
        }
    }

    protected boolean releaseLegacyAircraftWeapon() {
        int slot = getLegacySelectedHardpoint() >= 0
                ? getLegacySelectedHardpoint() : firstLoadedHardpoint();
        if (slot < 0 || slot >= 6 || legacyInventory.get(slot).isEmpty()) {
            return false;
        }
        ItemStack payload = legacyInventory.get(slot);
        int code = payloadCode(payload);
        if (code == 10) {
            EntityWarTechMissile missile =
                    LegacyEntityFactory.missile(world, MissileProfile.KH555);
            missile.setPosition(posX, posY - 0.75D, posZ);
            missile.setOwnerIdentity(getOwnerUuid(), getOwnerTeam());
            missile.setVisual("missile/kh555", 0);
            missile.setGuidanceTarget(getTargetX(), getTargetY(), getTargetZ());
            missile.motionX = motionX;
            missile.motionY = motionY;
            missile.motionZ = motionZ;
            missile.configureAirLaunch(rotationYaw, motionX, motionY, motionZ);
            if (!com.wartec.wartecmod.port.integration.MissileChunkLoader.spawnFlight(missile)) return false;
        } else {
        EntityWarTechOrdnance ordnance = code == 11 || code == 12
                ? LegacyEntityFactory.strategicBomb(world,
                        code == 11 ? WarTechEntityProfile.FAB_5000
                                : WarTechEntityProfile.KAB_3000)
                : LegacyEntityFactory.aviationOrdnance(world);
        ordnance.setPosition(posX, posY - 0.75D, posZ);
        ordnance.setOwnerIdentity(getOwnerUuid(), getOwnerTeam());
        if (code == 11 || code == 12) {
            ordnance.setVisual("ordnance/strategic_bomb", code == 12 ? 1 : 0);
        } else {
            ordnance.setVisual("ordnance/mq9_payload", Math.max(0, code - 1));
        }
        ordnance.setGuidanceTarget(getTargetX(), getTargetY(), getTargetZ());
        ordnance.motionX = motionX;
        ordnance.motionY = Math.min(-0.2D, motionY);
        ordnance.motionZ = motionZ;
        if (!com.wartec.wartecmod.port.integration.MissileChunkLoader.spawnFlight(ordnance)) return false;
        }
        payload.shrink(1);
        if (payload.isEmpty()) {
            legacyInventory.set(slot, ItemStack.EMPTY);
        }
        setLegacySelectedHardpoint(firstLoadedHardpoint());
        markDirty();
        return true;
    }

    private int firstLoadedHardpoint() {
        for (int slot = 0; slot < 6; ++slot) {
            if (!legacyInventory.get(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    public boolean handleLegacyGuiAction(int action, EntityPlayer player) {
        WarTechEntityProfile profile = getProfile();
        if (profile == WarTechEntityProfile.RADAR_TRUCK
                || profile == WarTechEntityProfile.S400_RADAR) {
            if (action != 0) return false;
            if (profile == WarTechEntityProfile.S400_RADAR) {
                setDeployed(!isDeployed());
                setLegacyEnabled(isDeployed());
            } else {
                setLegacyEnabled(!isLegacyEnabled());
            }
            return true;
        }
        if (profile == WarTechEntityProfile.MOBILE_AIR_DEFENSE) {
            if (action == 0) setLegacyFireMode((getLegacyFireMode() + 1) % 3);
            else if (action == 1) {
                if(!isDeployed()) { setDeployed(true);setLegacyEnabled(true);motionX=motionY=motionZ=0; }
                else setLegacyEnabled(!isLegacyEnabled());
            }
            else if (action == 2 && !isTor()) setLegacyFlags(getLegacyFlags() ^ 4);
            else return false;
            return true;
        }
        if (profile.getType() == WarTechEntityType.AIRCRAFT) {
            if (action == 0) {
                if (getLegacyState() == 0) {
                    if (!hasGuidanceTarget() || firstLoadedHardpoint() < 0
                            || getLegacyPower() < getAircraftLaunchEnergy()) return false;
                    setLegacyPower(getLegacyPower() - getAircraftLaunchEnergy());
                    setLegacyState(1);
                } else {
                    clearGuidanceTarget();
                    setLegacyState(4);
                }
            } else if (action == 1) {
                setLegacySelectedPayload((getLegacySelectedPayload() + 1) % 9);
            } else if (action == 2 || action == 3) {
                clearGuidanceTarget();
            } else if (action == 4 && (profile == WarTechEntityProfile.F_16C
                    || profile == WarTechEntityProfile.SU_27)) {
                setLegacyFlags(getLegacyFlags() ^ 8);
            } else if (action == 4 || action == 5) {
                setLegacyFlags(getLegacyFlags() ^ 16);
            } else {
                return false;
            }
            return true;
        }
        return false;
    }

    public int getLegacyPower() { return this.dataManager.get(LEGACY_POWER); }
    public void setLegacyPower(int value) {
        this.dataManager.set(LEGACY_POWER,
                MathHelper.clamp(value, 0, getEnergyCapacity()));
    }
    public int getLegacyState() { return this.dataManager.get(LEGACY_STATE); }
    public void setLegacyState(int value) {
        this.dataManager.set(LEGACY_STATE, Math.max(0, value));
    }
    public int getLegacyContacts() { return this.dataManager.get(LEGACY_CONTACTS); }
    public void setLegacyContacts(int value) {
        this.dataManager.set(LEGACY_CONTACTS, Math.max(0, value));
    }
    public int getLegacyFireMode() { return this.dataManager.get(LEGACY_FIRE_MODE); }
    public void setLegacyFireMode(int value) {
        this.dataManager.set(LEGACY_FIRE_MODE, MathHelper.clamp(value, 0, 2));
    }
    public int getLegacySelectedPayload() {
        return this.dataManager.get(LEGACY_SELECTED_PAYLOAD);
    }
    public void setLegacySelectedPayload(int value) {
        this.dataManager.set(LEGACY_SELECTED_PAYLOAD, Math.max(0, value));
    }
    public int getLegacySelectedHardpoint() {
        return this.dataManager.get(LEGACY_SELECTED_HARDPOINT);
    }
    public void setLegacySelectedHardpoint(int value) {
        this.dataManager.set(LEGACY_SELECTED_HARDPOINT, value);
    }
    public int getLegacyFlags() { return this.dataManager.get(LEGACY_FLAGS); }
    public void setLegacyFlags(int value) { this.dataManager.set(LEGACY_FLAGS, value); }
    public boolean isLegacyEnabled() { return (getLegacyFlags() & 1) != 0; }
    public void setLegacyEnabled(boolean enabled) {
        setLegacyFlags(enabled ? getLegacyFlags() | 1 : getLegacyFlags() & ~1);
    }
    public boolean isLegacyOperational() { return (getLegacyFlags() & 2) != 0; }
    public void setLegacyOperational(boolean operational) {
        setLegacyFlags(operational ? getLegacyFlags() | 2 : getLegacyFlags() & ~2);
    }
    public boolean isGunsEnabled() { return (getLegacyFlags() & 4) != 0; }
    public boolean isInterceptorMode() { return (getLegacyFlags() & 8) != 0; }
    public boolean isRemoteControlled() { return (getLegacyFlags() & 16) != 0; }
    public boolean isDeployed() { return (getLegacyFlags() & 32) != 0; }
    public void setDeployed(boolean deployed) {
        setLegacyFlags(deployed ? getLegacyFlags() | 32 : getLegacyFlags() & ~32);
    }
    public boolean isTor() { return getVisualVariant() == 0; }
    public int getLegacyBlipCount() {
        return Math.min(getLegacyContacts(), legacyBlips.length);
    }
    public int getLegacyBlip(int index) {
        return index >= 0 && index < getLegacyBlipCount() ? legacyBlips[index] : 0;
    }
    public void setLegacyBlip(int index, int packed) {
        if (index >= 0 && index < legacyBlips.length) legacyBlips[index] = packed;
    }
    public int getEnergyCapacity() {
        switch (getProfile()) {
            case MQ_9_REAPER: return 800000;
            case F_16C: return 1400000;
            case SU_27: return 1800000;
            case TU_95: return 4000000;
            case S400_RADAR: return 5000000;
            case RADAR_TRUCK: return 1000000;
            case MOBILE_AIR_DEFENSE: return 1200000;
            case MOBILE_ARTILLERY:
                return getVisualVariant() == 2 ? 1000000 : 100000;
            case COMMAND_TRUCK: return 2000000;
            case ELECTRONIC_WARFARE: return 1000000;
            default: return 250000;
        }
    }
    public int getBatterySlot() {
        return getProfile() == WarTechEntityProfile.RADAR_TRUCK
                || getProfile() == WarTechEntityProfile.S400_RADAR
                || getProfile() == WarTechEntityProfile.COMMAND_TRUCK ? 0
                : getProfile() == WarTechEntityProfile.MOBILE_AIR_DEFENSE ? 12
                : getProfile() == WarTechEntityProfile.MOBILE_ARTILLERY ? 10 : 6;
    }
    public int getRequiredInterceptorTier() {
        return isTor() ? 2 : 1;
    }
    public String getRequiredInterceptorName() {
        return isTor() ? "WTI-2 LANCE" : "WTI-1 FALCON";
    }
    public boolean isRequiredInterceptor(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return getRequiredInterceptorTier() == 1
                ? stack.getItem() == WarTechContent.ITEM_MISSILE_ANTI_AIR_TIER_1
                : stack.getItem() == WarTechContent.ITEM_MISSILE_ANTI_AIR_TIER_2;
    }
    public boolean isPayloadSlotAvailable(int slot) {
        return slot >= 0 && slot < getHardpointCount();
    }
    public boolean isPayloadCompatible(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() == WarTechContent.ASSEMBLED_CRUISE) {
            com.wartec.wartecmod.port.cruise.CruiseBuild build=com.wartec.wartecmod.port.cruise.CruiseBuild.fromStack(stack);
            return (getProfile()==WarTechEntityProfile.F_16C || getProfile()==WarTechEntityProfile.SU_27 || getProfile()==WarTechEntityProfile.TU_95)
                && (build.getAirframe()!=com.wartec.wartecmod.port.cruise.CruisePartDefinition.BODY_LONG_RANGE || getProfile()==WarTechEntityProfile.TU_95)
                && build.calculateStats().isValid()
                && build.get(com.wartec.wartecmod.port.cruise.CruiseSlot.LAUNCH)==com.wartec.wartecmod.port.cruise.CruisePartDefinition.LAUNCH_AIR;
        }
        if (getProfile() == WarTechEntityProfile.TU_95) {
            return stack.getItem() == WarTechContent.KH555_MISSILE
                    || stack.getItem() == WarTechContent.STRATEGIC_BOMB;
        }
        if (stack.getItem() != WarTechContent.MQ9_PAYLOAD) return false;
        int carrier = getProfile() == WarTechEntityProfile.MQ_9_REAPER ? 1
                : getProfile() == WarTechEntityProfile.F_16C ? 2
                : getProfile() == WarTechEntityProfile.SU_27 ? 4 : 0;
        int[] carriers = {7, 7, 7, 7, 6, 6, 6, 7, 6};
        int type = stack.getMetadata();
        return type >= 0 && type < carriers.length && (carriers[type] & carrier) != 0;
    }
    public int getLegacyPayloadCodeAt(int slot) {
        if (slot < 0 || slot >= 6) return 0;
        return getLegacyPayloadMask() >>> (slot * 4) & 15;
    }
    public int getLegacyPayloadMask() {
        return this.dataManager.get(LEGACY_PAYLOAD_MASK);
    }
    public int getHardpointCount() {
        return getProfile() == WarTechEntityProfile.F_16C ? 4 : 6;
    }
    public int getMaximumTargets() {
        return getProfile() == WarTechEntityProfile.TU_95 ? 6 : 4;
    }
    public int getMissionRange() {
        return getProfile() == WarTechEntityProfile.TU_95 ? 8000 : 4000;
    }
    public String getLegacyStateName() {
        if (isRemoteControlled()) return "REMOTE CONTROL";
        switch (getLegacyState()) {
            case 1: return "TAKEOFF";
            case 2: return "EN ROUTE";
            case 3: return "ATTACK";
            case 4: return "RETURNING";
            case 5: return "LANDING";
            case 6: return "LOST";
            default: return "READY";
        }
    }

    public int getAircraftLaunchEnergy() {
        switch (getProfile()) {
            case F_16C: return 60000;
            case SU_27: return 75000;
            case TU_95: return 120000;
            default: return 35000;
        }
    }

    @Override public int getSizeInventory() { return legacyInventory.size(); }
    @Override public boolean isEmpty() {
        for (ItemStack stack : legacyInventory) if (!stack.isEmpty()) return false;
        return true;
    }
    @Override public ItemStack getStackInSlot(int index) {
        return index >= 0 && index < legacyInventory.size()
                ? legacyInventory.get(index) : ItemStack.EMPTY;
    }
    @Override public ItemStack decrStackSize(int index, int count) {
        ItemStack result = ItemStackHelper.getAndSplit(legacyInventory, index, count);
        if (!result.isEmpty()) markDirty();
        return result;
    }
    @Override public ItemStack removeStackFromSlot(int index) {
        ItemStack result = ItemStackHelper.getAndRemove(legacyInventory, index);
        if (!result.isEmpty()) markDirty();
        return result;
    }
    @Override public void setInventorySlotContents(int index, ItemStack stack) {
        if (index < 0 || index >= legacyInventory.size()) return;
        legacyInventory.set(index, stack);
        if (!stack.isEmpty() && stack.getCount() > getInventoryStackLimit()) {
            stack.setCount(getInventoryStackLimit());
        }
        markDirty();
    }
    @Override public String getName() { return "container.wartecmod." + getProfile().name().toLowerCase(); }
    @Override public boolean hasCustomName() { return false; }
    @Override public ITextComponent getDisplayName() { return new TextComponentString(getName()); }
    @Override public int getInventoryStackLimit() { return 64; }
    @Override public void markDirty() { updatePayloadMask(); }
    @Override public boolean isUsableByPlayer(EntityPlayer player) {
        return !isDead && player.getDistanceSq(this) <= 256.0D;
    }
    @Override public void openInventory(EntityPlayer player) { }
    @Override public void closeInventory(EntityPlayer player) { }
    @Override public boolean isItemValidForSlot(int index, ItemStack stack) {
        if (index == getBatterySlot()) return stack.getItem() instanceof IBatteryItem;
        if (getProfile() == WarTechEntityProfile.MOBILE_AIR_DEFENSE
                && index == 13) {
            return !isTor() && PantsirAmmoBeltItem.isAmmo(stack);
        }
        if (getProfile() == WarTechEntityProfile.MOBILE_AIR_DEFENSE
                && index >= 0 && index < (isTor() ? 8 : 12)) {
            return isRequiredInterceptor(stack);
        }
        if (getProfile() == WarTechEntityProfile.MOBILE_ARTILLERY) {
            if (index == 0) {
                return stack.getItem().getRegistryName() != null
                        && "hbm:turret_chip".equals(
                                stack.getItem().getRegistryName().toString());
            }
            if (index >= 1 && index < 10) {
                return getVisualVariant() == 1
                        ? stack.getItem() == WarTechContent.ARTILLERY_AMMO
                        : getVisualVariant() == 2
                                && stack.getItem() == WarTechContent.HIMARS_AMMO;
            }
        }
        if (getEntityType() == WarTechEntityType.AIRCRAFT && index < 6) {
            return isPayloadSlotAvailable(index) && isPayloadCompatible(stack);
        }
        return true;
    }
    @Override public int getField(int id) { return 0; }
    @Override public void setField(int id, int value) { }
    @Override public int getFieldCount() { return 0; }
    @Override public void clear() {
        legacyInventory.clear();
        updatePayloadMask();
    }

    private void updatePayloadMask() {
        int packed = 0;
        for (int slot = 0; slot < 6; ++slot) {
            int code = payloadCode(legacyInventory.get(slot));
            packed |= (code & 15) << (slot * 4);
        }
        this.dataManager.set(LEGACY_PAYLOAD_MASK, packed);
    }

    private static int payloadCode(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.getItem() == WarTechContent.ASSEMBLED_CRUISE) {
            com.wartec.wartecmod.port.cruise.CruisePartDefinition body=com.wartec.wartecmod.port.cruise.CruiseBuild.fromStack(stack).getAirframe();
            return body==com.wartec.wartecmod.port.cruise.CruisePartDefinition.BODY_LIGHT?13
                :body==com.wartec.wartecmod.port.cruise.CruisePartDefinition.BODY_CLASSIC?14:15;
        }
        if (stack.getItem() == WarTechContent.MQ9_PAYLOAD) {
            return MathHelper.clamp(stack.getMetadata(), 0, 8) + 1;
        }
        if (stack.getItem() == WarTechContent.KH555_MISSILE) return 10;
        if (stack.getItem() == WarTechContent.STRATEGIC_BOMB) {
            return stack.getMetadata() == 1 ? 12 : 11;
        }
        return 0;
    }

    @Override
    protected boolean canFitPassenger(Entity passenger) {
        return getPassengers().isEmpty()
                && (getProfile() == WarTechEntityProfile.COMMAND_TRUCK
                || getProfile() == WarTechEntityProfile.MOBILE_AIR_DEFENSE
                || getProfile() == WarTechEntityProfile.MOBILE_ARTILLERY);
    }

    @Override
    public void updatePassenger(Entity passenger) {
        if (!isPassenger(passenger)) return;
        double yaw = Math.toRadians(rotationYaw);
        passenger.setPosition(posX - Math.sin(yaw) * 0.25D,
                posY + getMountedYOffset() + passenger.getYOffset(),
                posZ + Math.cos(yaw) * 0.25D);
    }

    @Override
    public double getMountedYOffset() {
        return 1.65D;
    }

    private static String sanitizeTeam(String team) {
        if (team == null) {
            return "";
        }
        String trimmed = team.trim();
        return trimmed.length() > 64 ? trimmed.substring(0, 64) : trimmed;
    }
}
