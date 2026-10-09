package com.wartec.wartecmod.port.gameplay;

import api.hbm.entity.IRadarDetectable;
import api.hbm.energy.IBatteryItem;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.entity.missile.EntityMissileAntiBallistic;
import com.hbm.items.ISatChip;
import com.hbm.items.ModItems;
import com.hbm.lib.HBMSoundHandler;
import com.wartec.wartecmod.port.content.MissileItem;
import com.wartec.wartecmod.port.content.MissileProfile;
import com.wartec.wartecmod.port.content.WarTechContent;
import com.wartec.wartecmod.port.entity.EntityWarTechBase;
import com.wartec.wartecmod.port.entity.EntityWarTechMissile;
import com.wartec.wartecmod.port.entity.LegacyEntityFactory;
import com.wartec.wartecmod.port.entity.WarTechEntityProfile;
import com.wartec.wartecmod.port.integration.DesignatorCompat;
import com.wartec.wartecmod.port.integration.NetworkTeamHelper;
import com.wartec.wartecmod.port.integration.HbmTilePowerLink;
import com.wartec.wartecmod.port.integration.HbmRadarScanner.RadarContact;
import com.wartec.wartecmod.port.integration.PoweredAirDefenseTileEntity;
import com.wartec.wartecmod.port.integration.MissileChunkLoader;
import com.wartec.wartecmod.port.integration.VlsInterceptorGuidance;
import com.wartec.wartecmod.port.integration.ElectronicWarfareService;
import com.wartec.wartecmod.port.network.FactionTerritoryData;
import com.wartec.wartecmod.port.network.MissileTrackingService;
import com.wartec.wartecmod.items.IMissileSpawningItem;
import com.wartec.wartecmod.WarTechReforged;
import java.lang.reflect.Constructor;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundCategory;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;

public class TileEntityWarTechMachine extends PoweredAirDefenseTileEntity implements IInventory {
    private final NonNullList<ItemStack> inventory = NonNullList.withSize(9, ItemStack.EMPTY);
    private boolean relayEnabled = true;
    private boolean relayOnline;
    private int linkedRelays;
    private int networkLaunchCooldown;
    private int openingAnimation;
    private boolean open;
    private int shoot;
    private int pendingTargetEntityId = -1;
    private boolean structureFormed;
    private int warmup;
    private boolean alarmActive;
    private int clientRadarContacts;
    private final int[] radarBlips = new int[16];
    private long manualGeranRequest;
    private boolean manualGeranPending;

    public boolean isRemoteDefenseNode() { return isRadarMachine() || isLauncher() || isCommunicationRelay(); }
    @Override public void onLoad() {
        super.onLoad();com.wartec.wartecmod.port.integration.OperationalChunks.register(this);
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) {
            return;
        }
        // Placement and multiblock formation may finish after onLoad. Reconcile
        // the persistent index once the final block state is available.
        if (world.getTotalWorldTime() % 40 == 0) {
            com.wartec.wartecmod.port.integration.OperationalChunks.register(this);
        }
        if (isStrategicRadar()) {
            HbmTilePowerLink.subscribeNearby(this, this, 18, 24);
        } else if (isCommunicationRelay()) {
            HbmTilePowerLink.subscribeNearby(this, this, 6, 8);
        } else if (isLaunchTube() || isGeranLauncher()) {
            HbmTilePowerLink.subscribeNearby(this, this, 10, 6);
            chargeFromBattery();
        }
        if (isRadarMachine()) {
            chargeFromBattery();
            super.update();
        } else {
            updateEnergyConnections();
            afterRadarTick();
        }
    }

    @Override
    public long getMaxPower() {
        if (isStrategicRadar()) {
            return 50_000_000L;
        }
        if (isCommunicationRelay()) {
            return 500_000L;
        }
        if (isAirRaidRelay()) {
            return 0L;
        }
        return 100_000L;
    }

    @Override
    protected double getRadarHorizontalRange() {
        String name = blockName();
        if (name.contains("strategic")) {
            return 6000.0D;
        }
        if (name.contains("s400")) {
            return 400.0D;
        }
        if (name.contains("patriot")) {
            return 250.0D;
        }
        return 1000.0D;
    }

    @Override
    protected double getRadarVerticalRange() {
        return isStrategicRadar()
                ? 4096.0D : Math.min(512.0D, getRadarHorizontalRange());
    }

    @Override
    protected long getRadarEnergyUsePerTick() {
        return blockName().contains("strategic") ? 2500L : 500L;
    }

    @Override
    protected boolean updateRadarPowerAndGate() {
        if (!isStrategicRadar()) {
            return super.updateRadarPowerAndGate();
        }
        boolean powered = structureFormed && hasPower(2500L);
        if (powered) {
            consumePower(2500L);
            if (warmup < 200) {
                ++warmup;
            }
        } else {
            warmup = Math.max(0, warmup - 4);
        }
        return powered && warmup >= 200;
    }

    @Override
    protected int getRadarScanIntervalTicks() {
        return isStrategicRadar() ? 20 : super.getRadarScanIntervalTicks();
    }
    @Override protected double getRadarSensorOffset() { return isStrategicRadar()?11.5:super.getRadarSensorOffset(); }

    @Override
    protected long getEngagementEnergyCost() {
        return 50_000L;
    }

    @Override
    protected int getEngagementCooldownTicks() {
        return blockName().contains("s400") ? 12 : 20;
    }

    @Override
    protected boolean engageTarget(Entity target) {
        if (!isLauncher() || world == null || world.isRemote
                || target == null || target.isDead) {
            return false;
        }
        int tier = loadedInterceptorTier();
        int slot = loadedInterceptorSlot(tier);
        double range=com.wartec.wartecmod.port.integration.WeaponBalance.interceptorRange(tier);
        if (tier == 0 || slot < 0 || !MissileTrackingService.canInterceptorEngage(target,tier,range)
                || NetworkTeamHelper.isFriendly(getOwnerTeam(),target)
                || (MissileTrackingService.isBallisticTarget(target)
                    ?Math.pow(target.posX-pos.getX()-.5,2)+Math.pow(target.posZ-pos.getZ()-.5,2)
                    :target.getDistanceSq(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5))>range*range
                || !consumePower(50_000L)) {
            return false;
        }
        long launcherKey = launcherKey();
        if (!MissileTrackingService.tryReserve(world,
                target.getEntityId(), launcherKey)) {
            setPower(getPower() + 50_000L);
            return false;
        }
        MissileProfile interceptorProfile = tier == 1
                ? MissileProfile.ANTI_AIR_TIER_1
                : tier == 2 ? MissileProfile.ANTI_AIR_TIER_2
                : MissileProfile.ANTI_AIR_TIER_3;
        EntityWarTechMissile interceptor =
            LegacyEntityFactory.missile(world, interceptorProfile);
        interceptor.setPosition(pos.getX() + 0.5D,
                pos.getY() + (blockName().contains("s400") ? 6.8D : 6.5D)*com.wartec.wartecmod.port.entity.VehicleDimensions.blockScale(blockName()),
                pos.getZ() + 0.5D);
        interceptor.setOwnerIdentity(null, getOwnerTeam());
        interceptor.setVisual("missile/anti_air_tier_" + tier, 0);
        boolean malfunction = VlsInterceptorGuidance.configureStationaryLaunch(
                interceptor, target, tier);
        if (!com.wartec.wartecmod.port.integration.MissileChunkLoader.spawnFlight(interceptor)) {
            MissileTrackingService.releaseReservation(world,
                    target.getEntityId(), launcherKey);
            setPower(getPower() + 50_000L);
            return false;
        }
        if (malfunction) {
            MissileTrackingService.releaseReservation(world,
                    target.getEntityId(), launcherKey);
            MissileTrackingService.deferTarget(world, target.getEntityId());
        } else {
            MissileTrackingService.confirmReservation(world,
                    target.getEntityId(), launcherKey, interceptor.getEntityId());
        }
        VlsInterceptorGuidance.spawnLaunchSmoke(
                world, interceptor.posX, interceptor.posY, interceptor.posZ,
                tier);
        ItemStack stack = inventory.get(slot);
        stack.shrink(1);
        if (stack.isEmpty()) {
            inventory.set(slot, ItemStack.EMPTY);
        }
        markDirty();
        return true;
    }

    @Override
    protected void afterRadarTick() {
        if (!isRadarMachine()) {
            chargeFromBattery();
        }
        if (isCommunicationRelay()) {
            relayOnline = relayEnabled && consumePower(20L);
            if (relayOnline) {
                MissileChunkLoader.trackCommunicationNode(this);
                long relayKey = MissileTrackingService.communicationRelayKey(
                        pos.getX(), pos.getY(), pos.getZ());
                if (world.getTotalWorldTime() % 200L
                        == Math.abs(relayKey) % 200L) {
                    FactionTerritoryData.claimAt(world, getOwnerTeam(),
                            pos.getX() + 0.5D, pos.getZ() + 0.5D);
                }
                if (world.getTotalWorldTime() % 10L == 0L) {
                    MissileTrackingService.updateCommunicationRelay(world,
                            relayKey, pos.getX() + 0.5D, pos.getY() + 0.5D,
                            pos.getZ() + 0.5D, getOwnerTeam());
                    linkedRelays =
                            MissileTrackingService.countLinkedCommunicationRelays(
                                    world, pos.getX() + 0.5D,
                                    pos.getY() + 0.5D, pos.getZ() + 0.5D,
                                    getOwnerTeam());
                }
            } else {
                MissileChunkLoader.untrackCommunicationNode(this);
                MissileTrackingService.removeCommunicationRelay(world,
                        MissileTrackingService.communicationRelayKey(
                                pos.getX(), pos.getY(), pos.getZ()));
                linkedRelays = 0;
            }
        }
        if (isAirRaidRelay() && world.getTotalWorldTime() % 10L == 0L) {
            updateAirRaidAlarm();
        }
        if (isStrategicRadar()) {
            int radarId = radarId();
            if (isRadarOperational()
                    && world.getTotalWorldTime() % 20L == 0L) {
                MissileTrackingService.updateStrategicRadarSweep(world, radarId,
                        pos.getX() + 0.5D, pos.getY() + 12.0D,
                        pos.getZ() + 0.5D, 6000.0D, 4096.0D,
                        64, getOwnerTeam(), 1);
            } else if (!isRadarOperational()) {
                MissileTrackingService.removeRadar(world, radarId);
            }
        } else if(isRadarMachine()) {
            // Ordinary block radars must publish contacts, not merely draw local blips.
            if(!isRadarOperational()) MissileTrackingService.removeRadar(world,radarId());
            else if(world.getTotalWorldTime()%getRadarScanIntervalTicks()==0)
                MissileTrackingService.updateRadarSweep(world,radarId(),pos.getX()+.5,
                    pos.getY()+.5+getRadarSensorOffset(),pos.getZ()+.5,getRadarHorizontalRange(),getRadarVerticalRange(),
                    getRadarContactLimit(),getOwnerTeam(),blockName().contains("s400")?ElectronicWarfareService.BAND_L:
                        blockName().contains("patriot")?ElectronicWarfareService.BAND_X:ElectronicWarfareService.BAND_S);
        }
        if (isLauncher()) {
            tickNetworkLauncher();
        }
        if (hasOpeningCycle()) {
            tickOpeningCycle();
        }
    }

    private void tickNetworkLauncher() {
        if (networkLaunchCooldown > 0) {
            --networkLaunchCooldown;
        }
        int loadedTier = loadedInterceptorTier();
        int networkTier = loadedTier > 0 ? loadedTier
                : blockName().contains("s400") ? 3 : 2;
        long key = launcherKey();
        if (world.getTotalWorldTime() % 10L == Math.abs(pos.toLong()) % 10L) {
            MissileTrackingService.updateLauncherPresence(world,
                    pos.getX() + 0.5D,
                    pos.getY() + (blockName().contains("s400") ? 6.8D : 6.5D)*com.wartec.wartecmod.port.entity.VehicleDimensions.blockScale(blockName()),
                    pos.getZ() + 0.5D, networkTier, key, getOwnerTeam());
        }
        if (loadedTier == 0 || networkLaunchCooldown > 0 || getPower() < 50_000L) {
            return;
        }
        int period = loadedTier == 1 ? 2 : loadedTier == 2 ? 5 : 10;
        if (world.getTotalWorldTime() % period
                != Math.abs(pos.getX() * 31L + pos.getZ() * 17L) % period) {
            return;
        }
        double range = com.wartec.wartecmod.port.integration.WeaponBalance.interceptorRange(loadedTier);
        Entity target = MissileTrackingService.findThreat(world,
                pos.getX() + 0.5D,
                pos.getY() + (blockName().contains("s400") ? 6.8D : 6.5D)*com.wartec.wartecmod.port.entity.VehicleDimensions.blockScale(blockName()),
                pos.getZ() + 0.5D, loadedTier, range, key, getOwnerTeam());
        if (target != null) {
            pendingTargetEntityId = target.getEntityId();
            requestLaunch();
            networkLaunchCooldown = period;
        }
    }

    private void tickOpeningCycle() {
        int previousAnimation = openingAnimation;
        int previousShoot = shoot;
        boolean previousOpen = open;
        if (open && openingAnimation < 90) {
            openingAnimation = Math.min(90, openingAnimation + 3);
        }
        if (!open && openingAnimation > 0) {
            openingAnimation = Math.max(0, openingAnimation - 3);
        }
        if (shoot == 50) {
            open = true;
        }
        if (openingAnimation >= 86 && shoot == 50) {
            Entity target = pendingTargetEntityId < 0
                    ? null : world.getEntityByID(pendingTargetEntityId);
            if (target != null && isLauncher()) {
                engageTarget(target);
            } else {
                launchLoadedMissile(null);
            }
            pendingTargetEntityId = -1;
            --shoot;
            notifyStateChanged();
        }
        if (shoot > 0 && shoot < 50) {
            --shoot;
        }
        if (shoot == 0) {
            open = false;
        }
        if (previousAnimation != openingAnimation || previousShoot != shoot
                || previousOpen != open) {
            notifyStateChanged();
        }
    }

    public void requestLaunch() {
        if (world == null || world.isRemote) {
            return;
        }
        if (isBallisticLauncher()) {
            launchLoadedMissile(null);
        } else if (hasOpeningCycle() && shoot == 0) {
            shoot = 50;
            notifyStateChanged();
        }
    }

    private int loadedInterceptorTier() {
        for (ItemStack stack : inventory) {
            if (stack.isEmpty() || !(stack.getItem() instanceof MissileItem)) {
                continue;
            }
            MissileProfile profile = ((MissileItem) stack.getItem()).getProfile();
            if (profile == MissileProfile.ANTI_AIR_TIER_1) return 1;
            if (profile == MissileProfile.ANTI_AIR_TIER_2) return 2;
            if (profile == MissileProfile.ANTI_AIR_TIER_3) return 3;
        }
        return 0;
    }

    private int loadedInterceptorSlot(int tier) {
        for (int slot = 0; slot < inventory.size(); ++slot) {
            ItemStack stack = inventory.get(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof MissileItem)) continue;
            MissileProfile profile = ((MissileItem) stack.getItem()).getProfile();
            if (tier == 1 && profile == MissileProfile.ANTI_AIR_TIER_1
                    || tier == 2 && profile == MissileProfile.ANTI_AIR_TIER_2
                    || tier == 3 && profile == MissileProfile.ANTI_AIR_TIER_3) {
                return slot;
            }
        }
        return -1;
    }

    private int radarId() {
        int value = pos.getX() * 73428767
                ^ pos.getY() * 19349663 ^ pos.getZ() * 912931;
        return 0x40000000 | value & 0x3FFFFFFF;
    }

    private long launcherKey() {
        long value = 1469598103934665603L;
        value = (value ^ pos.getX()) * 1099511628211L;
        value = (value ^ pos.getY()) * 1099511628211L;
        return (value ^ pos.getZ()) * 1099511628211L;
    }

    @Override
    protected boolean isValidRadarTarget(
        Entity entity,
        IRadarDetectable.RadarTargetType targetType
    ) {
        return targetType != IRadarDetectable.RadarTargetType.PLAYER;
    }

    private boolean isLauncher() {
        String name = blockName();
        return name.contains("vlsexhaust") || name.contains("patriot")
                || name.contains("s400launcher");
    }

    private boolean isRadarMachine() {
        return blockName().contains("radar");
    }

    public String getLegacyMachineName() {
        return blockName();
    }

    public boolean isCommunicationRelay() {
        return blockName().contains("communicationmast");
    }

    public boolean isStrategicRadar() {
        return blockName().contains("strategic");
    }

    public boolean isVlsExhaust() {
        String name = blockName();
        return name.contains("vlsexhaust") || name.contains("patriot")
                || name.contains("s400launcher");
    }

    public boolean isBallisticLauncher() {
        return blockName().contains("ballisticmissilelauncher");
    }

    public boolean isGeranLauncher() {
        return blockName().contains("geranlauncher");
    }

    public boolean isLaunchTube() {
        String name = blockName();
        return name.contains("launchtube") || name.contains("vlsexhaust")
                || name.contains("ballisticmissilelauncher")
                || name.contains("patriot") || name.contains("s400launcher");
    }

    public boolean isAirRaidRelay() {
        return blockName().contains("airraidsirenrelay");
    }

    private boolean hasOpeningCycle() {
        return isLaunchTube() && !isBallisticLauncher()
                && !isGeranLauncher();
    }

    public int getLinkedRelays() {
        return linkedRelays;
    }

    public boolean isRelayEnabled() {
        return relayEnabled;
    }

    public boolean isRelayOnline() {
        return relayOnline;
    }

    public boolean isAlarmActive() {
        return alarmActive;
    }

    public int getOpeningAnimation() {
        return openingAnimation;
    }

    public boolean isOpen() {
        return open;
    }

    public int getLaunchCountdown() {
        return shoot;
    }

    public boolean isStructureFormed() {
        return structureFormed;
    }

    public void setStructureFormed(boolean formed) {
        if (structureFormed != formed) {
            structureFormed = formed;
            notifyStateChanged();
        }
    }

    public int getWarmupPercent() {
        return Math.min(100, warmup * 100 / 200);
    }

    public int getDisplayedRadarContacts() {
        return world != null && world.isRemote
                ? clientRadarContacts : getRadarContacts().size();
    }

    @Override
    protected void onRadarContactsUpdated(List<RadarContact> updatedContacts) {
        int count = Math.min(radarBlips.length, updatedContacts.size());
        for (int index = 0; index < count; ++index) {
            Entity contact = updatedContacts.get(index).getEntity();
            int relativeX = MathHelper.clamp(
                    (int) Math.round(contact.posX - (pos.getX() + 0.5D)),
                    Short.MIN_VALUE, Short.MAX_VALUE);
            int relativeZ = MathHelper.clamp(
                    (int) Math.round(contact.posZ - (pos.getZ() + 0.5D)),
                    Short.MIN_VALUE, Short.MAX_VALUE);
            radarBlips[index] = (relativeX & 0xFFFF) << 16
                    | relativeZ & 0xFFFF;
        }
        for (int index = count; index < radarBlips.length; ++index) {
            radarBlips[index] = 0;
        }
    }

    public int getRadarBlipCount() {
        return Math.min(getDisplayedRadarContacts(), radarBlips.length);
    }

    public int getRadarBlip(int index) {
        return index >= 0 && index < getRadarBlipCount()
                ? radarBlips[index] : 0;
    }

    public void setClientRadarBlip(int index, int packed) {
        if (world != null && world.isRemote
                && index >= 0 && index < radarBlips.length) {
            radarBlips[index] = packed;
        }
    }

    public void setClientLegacyState(int radarEnabled, int radarOperational,
            int contacts, int relayEnabled, int relayOnline, int links,
            int formed, int warmupPercent, int opening, int opened,
            int launchCountdown, int alarm) {
        if (world == null || !world.isRemote) {
            return;
        }
        setRadarEnabled(radarEnabled != 0);
        setClientRadarOperational(radarOperational != 0);
        clientRadarContacts = Math.max(0, contacts);
        this.relayEnabled = relayEnabled != 0;
        this.relayOnline = relayOnline != 0;
        linkedRelays = Math.max(0, links);
        structureFormed = formed != 0;
        warmup = Math.max(0, Math.min(200, warmupPercent * 2));
        openingAnimation = Math.max(0, Math.min(90, opening));
        open = opened != 0;
        shoot = Math.max(0, Math.min(50, launchCountdown));
        alarmActive = alarm != 0;
    }

    public boolean handleLegacyGuiAction(int action, EntityPlayer player) {
        if (action != 0) return false;
        if (isCommunicationRelay()) {
            relayEnabled = !relayEnabled;
            if (!relayEnabled) {
                relayOnline = false;
            }
        } else if (isStrategicRadar()) {
            setRadarEnabled(!isRadarEnabled());
        } else {
            return false;
        }
        notifyStateChanged();
        return true;
    }

    public boolean launchLoadedMissile(EntityPlayer player) {
        return launchLoadedMissile(player, null);
    }

    /** NTM detonators also address old Geran/VLS pads: lease the site and resolve XZ-only targets. */
    public boolean launchFromDetonator(EntityPlayer player) {
        if(!(world instanceof WorldServer) || isInvalid()) return false;
        final ItemStack ammo=inventory.get(0).copy(),designator=inventory.get(1).copy();
        if(ammo.isEmpty()) return false;
        java.util.function.BooleanSupplier valid=()->!isInvalid()
            && ItemStack.areItemStacksEqual(ammo,inventory.get(0))
            && ItemStack.areItemStacksEqual(designator,inventory.get(1));
        final net.minecraft.util.math.Vec3d[] target={null};final boolean[] resolving={false};
        return com.wartec.wartecmod.port.integration.OperationalChunks.requestUntil((WorldServer)world,pos,2,
            "legacy-launch:"+pos.toLong(),valid,()->{
                if(!valid.getAsBoolean()) return true;
                if(target[0]==null && DesignatorCompat.getHorizontalTarget(designator)!=null) {
                    if(!resolving[0]) {
                        resolving[0]=true;
                        DesignatorCompat.resolveSavedTarget((WorldServer)world,designator.copy(),Double.NaN,valid,p->target[0]=p);
                    }
                    if(target[0]==null) return false;
                }
                // A detonator requests autonomous launch, not remote camera/pilot mode.
                launchLoadedMissile(null,target[0]==null?null:new BlockPos(target[0]));return true;
            });
    }

    public boolean launchAtCoordinates(int targetX, int targetZ,
            EntityPlayer player) {
        if (world == null) {
            return false;
        }
        BlockPos column=new BlockPos(targetX,0,targetZ);
        if(!world.isBlockLoaded(column)) return false;
        int targetY = world.getHeight(column).getY();
        return launchLoadedMissile(player,
                new BlockPos(targetX, targetY, targetZ));
    }

    private boolean launchLoadedMissile(EntityPlayer player,
            BlockPos explicitTarget) {
        if (world == null || world.isRemote) return false;
        if (isGeranLauncher()) {
            return launchGeranRemote(player,explicitTarget);
        }
        ItemStack stack = inventory.get(0);
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() == ModItems.missile_anti_ballistic) {
            if (!hasPower(75_000L)) {
                return false;
            }
            EntityMissileAntiBallistic missile =
                    new EntityMissileAntiBallistic(world);
            missile.setPosition(pos.getX() + 0.5D,
                    pos.getY() + (isBallisticLauncher() ? 0.5D : 11.0D),
                    pos.getZ() + 0.5D);
            if (!com.wartec.wartecmod.port.integration.MissileChunkLoader.spawnFlight(missile)) {
                return false;
            }
            consumePower(75_000L);
            inventory.set(0, ItemStack.EMPTY);
            finishMissileLaunch(null);
            return true;
        }
        if (!(stack.getItem() instanceof MissileItem)) {
            return stack.getItem() instanceof IMissileSpawningItem
                    && launchContractMissile(
                            (IMissileSpawningItem) stack.getItem(),
                            player, explicitTarget);
        }
        MissileProfile profile = ((MissileItem) stack.getItem()).getProfile();
        if (!isProfileAccepted(profile)) {
            return false;
        }
        int satelliteId = -1;
        if (profile == MissileProfile.ASAT) {
            ItemStack chip = inventory.get(1);
            if (chip.isEmpty()
                    || !(chip.getItem() instanceof ISatChip)) {
                return false;
            }
            satelliteId = ISatChip.getFreqS(chip);
        }
        long requiredPower = requiredLaunchPower(profile);
        long consumedPower = consumedLaunchPower(profile);
        if (!hasPower(requiredPower)) {
            return false;
        }
        BlockPos target = explicitTarget != null ? explicitTarget
                : DesignatorCompat.getTarget(world, player,
                        inventory.get(1));
        if (requiresDesignator(profile) && target == null) {
            return false;
        }
        if (target == null) {
            target = pos.add(0, 256, 0);
        } else if (target.getX() == pos.getX()
                && target.getZ() == pos.getZ()) {
            target = target.add(isBallisticLauncher() ? 1 : 6, 0, 0);
        }
        EntityWarTechMissile missile =
                LegacyEntityFactory.missile(world, profile);
        double maximum=com.wartec.wartecmod.port.integration.WeaponBalance.missileRange(profile);
        if(maximum>0 && Math.hypot(target.getX()-pos.getX(),target.getZ()-pos.getZ())*1.12+80>maximum) {
            if(player!=null) player.sendMessage(new net.minecraft.util.text.TextComponentTranslation("flight.error.range",(int)maximum));
            return false;
        }
        TileEntityWarTechMachine exhaust = findConnectedVlsExhaust();
        double launchY = pos.getY()
                + (isBallisticLauncher() ? 2.0D : 11.0D);
        missile.setPosition(pos.getX() + 0.5D, launchY,
                pos.getZ() + 0.5D);
        missile.setOwnerTeam(getOwnerTeam());
        missile.setVisual("missile/" + profile.getIntentPath(), 0);
        missile.configureLegacyGroundLaunch(
                target.getX(), target.getY(), target.getZ());
        missile.configureVlsExhaust(
                exhaust == null ? null : exhaust.getPos());
        if (profile == MissileProfile.ASAT) {
            missile.setSatelliteId(satelliteId);
        }
        if (profile.getFlightClass()
                == MissileProfile.FlightClass.INTERCEPTOR) {
            missile.motionY = 2.2D;
        } else if (profile.getFlightClass()
                != MissileProfile.FlightClass.BALLISTIC
                && profile.getFlightClass()
                != MissileProfile.FlightClass.GLIDE) {
            missile.motionY = 0.28D;
        }
        if (!MissileChunkLoader.prepare(missile)) {
            if(player!=null) player.sendMessage(new net.minecraft.util.text.TextComponentTranslation("flight.error.chunks"));return false;
        }
        if (!com.wartec.wartecmod.port.integration.MissileChunkLoader.spawnFlight(missile)) {
            MissileChunkLoader.untrack(missile);return false;
        }
        consumePower(consumedPower);
        inventory.set(0, ItemStack.EMPTY);
        MissileChunkLoader.track(missile);
        MissileTrackingService.registerLaunch(missile,
                pos.getX() + 0.5D, launchY, pos.getZ() + 0.5D,
                target.getX(), target.getZ(), getOwnerTeam());
        finishMissileLaunch(profile, exhaust != null);
        return true;
    }

    private boolean launchContractMissile(IMissileSpawningItem item,
            EntityPlayer player, BlockPos explicitTarget) {
        if (!hasPower(75_000L)) {
            return false;
        }
        BlockPos target = explicitTarget != null ? explicitTarget
                : DesignatorCompat.getTarget(world, player, inventory.get(1));
        if (target == null) {
            return false;
        }
        if (target.getX() == pos.getX() && target.getZ() == pos.getZ()) {
            target = target.add(6, 0, 0);
        }
        try {
            TileEntityWarTechMachine exhaust = findConnectedVlsExhaust();
            Entity missile = constructContractMissile(
                    item.getMissile(), target, exhaust);
            if (missile == null) {
                return false;
            }
            if (missile instanceof EntityWarTechBase) {
                ((EntityWarTechBase) missile).setOwnerTeam(getOwnerTeam());
            }
            if (missile instanceof EntityWarTechMissile) {
                ((EntityWarTechMissile) missile).configureLegacyGroundLaunch(
                        target.getX(), target.getY(), target.getZ());
                ((EntityWarTechMissile) missile).configureVlsExhaust(
                        exhaust == null ? null : exhaust.getPos());
            }
            if (!com.wartec.wartecmod.port.integration.MissileChunkLoader.spawnFlight(missile)) {
                return false;
            }
            consumePower(50_000L);
            inventory.set(0, ItemStack.EMPTY);
            if (missile instanceof EntityWarTechMissile) {
                MissileChunkLoader.track((EntityWarTechMissile) missile);
                MissileTrackingService.registerLaunch(
                        (EntityWarTechMissile) missile,
                        pos.getX() + 0.5D, pos.getY() + 11.0D,
                        pos.getZ() + 0.5D, target.getX(), target.getZ(),
                        getOwnerTeam());
            }
            finishMissileLaunch(
                    MissileProfile.CRUISE_HE, exhaust != null);
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            if (WarTechReforged.logger != null) {
                WarTechReforged.logger.error(
                        "Unable to launch IMissileSpawningItem {} from {}",
                        item.getClass().getName(), pos, exception);
            }
            return false;
        }
    }

    private Entity constructContractMissile(Class<? extends Entity> type,
            BlockPos target, TileEntityWarTechMachine exhaust)
            throws ReflectiveOperationException {
        if (type == null) {
            return null;
        }
        Object[] baseArguments = {
            world,
            Float.valueOf(pos.getX() + 0.5F),
            Float.valueOf(pos.getY() + 11.0F),
            Float.valueOf(pos.getZ() + 0.5F),
            Integer.valueOf(target.getX()),
            Integer.valueOf(target.getZ())
        };
        for (Constructor<?> constructor : type.getConstructors()) {
            Class<?>[] parameters = constructor.getParameterTypes();
            if (parameters.length != 7
                    || parameters[0] != net.minecraft.world.World.class
                    || parameters[1] != Float.TYPE
                    || parameters[2] != Float.TYPE
                    || parameters[3] != Float.TYPE
                    || parameters[4] != Integer.TYPE
                    || parameters[5] != Integer.TYPE
                    || exhaust != null
                    && !parameters[6].isInstance(exhaust)) {
                continue;
            }
            return (Entity) constructor.newInstance(
                    world,
                    baseArguments[1], baseArguments[2], baseArguments[3],
                    baseArguments[4], baseArguments[5], exhaust);
        }
        Constructor<? extends Entity> constructor = type.getConstructor(
                net.minecraft.world.World.class,
                Float.TYPE, Float.TYPE, Float.TYPE,
                Integer.TYPE, Integer.TYPE);
        return constructor.newInstance(baseArguments);
    }

    private boolean isProfileAccepted(MissileProfile profile) {
        MissileProfile.FlightClass flight = profile.getFlightClass();
        if (isBallisticLauncher()) {
            return flight == MissileProfile.FlightClass.BALLISTIC
                    || flight == MissileProfile.FlightClass.GLIDE
                    || flight == MissileProfile.FlightClass.MICRO
                    || flight == MissileProfile.FlightClass.ANTI_SATELLITE;
        }
        if (profile == MissileProfile.ANTI_BALLISTIC_NUCLEAR
                || profile == MissileProfile.ANTI_AIR_TIER_1) {
            return true;
        }
        return flight == MissileProfile.FlightClass.SUBSONIC
                || flight == MissileProfile.FlightClass.SUPERSONIC
                || flight == MissileProfile.FlightClass.HYPERSONIC;
    }

    private boolean requiresDesignator(MissileProfile profile) {
        MissileProfile.FlightClass flight = profile.getFlightClass();
        return flight != MissileProfile.FlightClass.INTERCEPTOR
                && flight != MissileProfile.FlightClass.ANTI_SATELLITE;
    }

    private long requiredLaunchPower(MissileProfile profile) {
        if (isBallisticLauncher()) {
            return 75_000L;
        }
        if (profile == MissileProfile.ANTI_BALLISTIC_NUCLEAR) {
            return 5_000L;
        }
        if (profile == MissileProfile.ANTI_AIR_TIER_1) {
            return 50_000L;
        }
        return 75_000L;
    }

    private long consumedLaunchPower(MissileProfile profile) {
        if (isBallisticLauncher()) {
            return 75_000L;
        }
        if (profile == MissileProfile.ANTI_BALLISTIC_NUCLEAR
                || profile == MissileProfile.ANTI_AIR_TIER_1) {
            return 5_000L;
        }
        return 50_000L;
    }

    private void finishMissileLaunch(MissileProfile profile) {
        finishMissileLaunch(profile, false);
    }

    private void finishMissileLaunch(
            MissileProfile profile, boolean connectedExhaust) {
        markDirty();
        notifyStateChanged();
        boolean cruiseLaunch = !isBallisticLauncher()
                && profile != MissileProfile.ANTI_BALLISTIC_NUCLEAR
                && profile != MissileProfile.ANTI_AIR_TIER_1
                && profile != null;
        SoundEvent sound = SoundEvent.REGISTRY.getObject(
                new ResourceLocation(cruiseLaunch
                        ? "wartecmod:weapon.cruisemissiletakeoff"
                        : "wartecmod:weapon.ballisticmissiletakeoff"));
        if (!cruiseLaunch && HBMSoundHandler.missileTakeoff != null) {
            sound = HBMSoundHandler.missileTakeoff;
        }
        if (sound == null) {
            sound = SoundEvents.ENTITY_FIREWORK_LAUNCH;
        }
        world.playSound(null, pos.getX() + 0.5D,
                pos.getY() + (cruiseLaunch ? 10.0D : 0.5D),
                pos.getZ() + 0.5D, sound, SoundCategory.PLAYERS,
                cruiseLaunch ? 10.0F : 2.0F, 1.0F);
        if (cruiseLaunch && !connectedExhaust) {
            ExplosionLarge.spawnParticles(world,
                    pos.getX() + 0.5D, pos.getY() + 11.0D,
                    pos.getZ() + 0.5D, 5);
        }
    }

    private TileEntityWarTechMachine findConnectedVlsExhaust() {
        if (world == null || isVlsExhaust()) {
            return null;
        }
        Map<Long, TileEntityWarTechMachine> launchers =
                new HashMap<Long, TileEntityWarTechMachine>();
        for (TileEntity tile : world.loadedTileEntityList) {
            if (!(tile instanceof TileEntityWarTechMachine)) {
                continue;
            }
            TileEntityWarTechMachine machine =
                    (TileEntityWarTechMachine) tile;
            BlockPos candidate = machine.getPos();
            if (!machine.isLaunchTube()
                    || candidate.getY() != pos.getY()
                    || Math.abs(candidate.getX() - pos.getX()) > 30
                    || Math.abs(candidate.getZ() - pos.getZ()) > 30) {
                continue;
            }
            launchers.put(packXZ(
                    candidate.getX(), candidate.getZ()), machine);
        }

        ArrayDeque<int[]> open = new ArrayDeque<int[]>();
        Set<Long> visited = new HashSet<Long>();
        open.add(new int[] {pos.getX(), pos.getZ(), 0});
        visited.add(packXZ(pos.getX(), pos.getZ()));
        int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        while (!open.isEmpty()) {
            int[] current = open.removeFirst();
            if (current[2] >= 30) {
                continue;
            }
            for (int[] direction : directions) {
                int x = current[0] + direction[0];
                int z = current[1] + direction[1];
                long key = packXZ(x, z);
                if (!visited.add(key)) {
                    continue;
                }
                TileEntityWarTechMachine machine = launchers.get(key);
                if (machine == null) {
                    continue;
                }
                if (machine.isVlsExhaust()) {
                    return machine;
                }
                open.addLast(new int[] {x, z, current[2] + 1});
            }
        }
        return null;
    }

    private static long packXZ(int x, int z) {
        return (long) x << 32 ^ (long) z & 0xFFFFFFFFL;
    }

    public boolean launchGeranRemote(EntityPlayer player) {
        String error = manualGeranError(player);
        if (error != null) return tellGeranFailure(player, error);
        if (manualGeranPending) return true;
        BlockPos target = DesignatorCompat.getTarget(world, player, inventory.get(1));
        if (target != null) return launchGeranRemote(player, target);
        net.minecraft.util.math.Vec3d horizontal = DesignatorCompat.getHorizontalTarget(inventory.get(1));
        if (horizontal == null) return tellGeranFailure(player, "target");
        if (!geranTargetInRange(new BlockPos(horizontal))) return tellGeranFailure(player, "range");
        final ItemStack ammo = inventory.get(0).copy(), designator = inventory.get(1).copy();
        final long request = ++manualGeranRequest, deadline = world.getTotalWorldTime() + 400;
        manualGeranPending = true;
        java.util.function.BooleanSupplier valid = () -> {
            if (!manualGeranPending || manualGeranRequest != request) return false;
            String invalid = manualGeranError(player);
            if (invalid == null && (!ItemStack.areItemStacksEqual(ammo, inventory.get(0))
                    || !ItemStack.areItemStacksEqual(designator, inventory.get(1)))) invalid = "changed";
            if (invalid == null && world.getTotalWorldTime() > deadline) invalid = "busy";
            if (invalid == null) return true;
            manualGeranPending = false;
            tellGeranFailure(player, invalid);
            return false;
        };
        boolean accepted = DesignatorCompat.resolveSavedTarget((WorldServer) world, designator.copy(),
                Double.NaN, valid, point -> {
                    manualGeranPending = false;
                    launchGeranRemote(player, new BlockPos(point));
                });
        if (!accepted) {
            manualGeranPending = false;
            return tellGeranFailure(player, "busy");
        }
        if (manualGeranPending) player.sendMessage(new net.minecraft.util.text.TextComponentTranslation("geran.launch.resolving"));
        return true;
    }

    private String manualGeranError(EntityPlayer player) {
        if (!(world instanceof WorldServer) || isInvalid() || !isGeranLauncher()
                || world.getTileEntity(pos) != this) return "launcher";
        if (player == null || player.isDead || player.world != world
                || player.getDistanceSq(pos) > 144) return "operator";
        if (!getOwnerTeam().isEmpty() && !NetworkTeamHelper.areFriendly(getOwnerTeam(),
                NetworkTeamHelper.getPlayerTeam(player))) return "iff";
        if (!isGeranAmmo(inventory.get(0))) return "ammo";
        if (!DesignatorCompat.isDesignator(inventory.get(1))) return "designator";
        return getPower() < 25_000L ? "power" : null;
    }

    private boolean geranTargetInRange(BlockPos target) {
        double distance = Math.hypot(target.getX() - (pos.getX() + .5D),
                target.getZ() - (pos.getZ() + .5D));
        return distance >= 20 && distance <= 1000;
    }

    private static boolean isGeranAmmo(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem()==WarTechContent.GERAN_DRONE
                || stack.getItem()==WarTechContent.GERAN_5_DRONE);
    }

    private boolean launchGeranRemote(EntityPlayer player,BlockPos explicitTarget) {
        if (player != null) {
            String error = manualGeranError(player);
            if (error != null) return tellGeranFailure(player, error);
        }
        if (world == null || world.isRemote || !isGeranLauncher()
                || inventory.get(0).isEmpty()
                || !isGeranAmmo(inventory.get(0))
                || inventory.get(1).isEmpty()
                || getPower() < 25_000L) {
            return tellGeranFailure(player, "ammo");
        }
        BlockPos target = explicitTarget!=null?explicitTarget:DesignatorCompat.getTarget(
                world, player, inventory.get(1));
        if (target == null) {
            return tellGeranFailure(player, "target");
        }
        if (!geranTargetInRange(target)) return tellGeranFailure(player, "range");
        MissileProfile geranProfile=((com.wartec.wartecmod.port.content.MissileItem)inventory.get(0).getItem()).getProfile();
        EntityWarTechMissile geran = LegacyEntityFactory.missile(world, geranProfile);
        geran.setPosition(pos.getX() + 0.5D,
                pos.getY() + (geranProfile==MissileProfile.GERAN_5?1.368D:
                    1.35D*com.wartec.wartecmod.port.entity.VehicleDimensions.blockScale(blockName())),
                pos.getZ() + (geranProfile==MissileProfile.GERAN_5?.68D:.5D));
        if(geranProfile==MissileProfile.GERAN_5) geran.rotationPitch=-12;
        geran.setOwnerIdentity(
                player == null ? null : player.getUniqueID(), getOwnerTeam());
        geran.setVisual("missile/"+geranProfile.getIntentPath(), 0);
        geran.setGuidanceTarget(
                target.getX(), target.getY(), target.getZ());
        if (!MissileChunkLoader.spawnFlight(geran)) {
            return tellGeranFailure(player, "busy");
        }
        // A rejected pilot handoff is not an autonomous launch. Preserve ammo and energy.
        if (player != null && !geran.beginRemoteControl(player)) {
            geran.setDead();
            MissileChunkLoader.untrack(geran);
            return tellGeranFailure(player, "control");
        }
        MissileChunkLoader.track(geran);
        MissileTrackingService.registerLaunch(geran,
                pos.getX() + 0.5D, pos.getY() + 1.35D,
                pos.getZ() + 0.5D,
                target.getX(), target.getZ(), getOwnerTeam());
        setPower(getPower() - 25_000L);
        inventory.set(0, ItemStack.EMPTY);
        markDirty();
        notifyStateChanged();
        world.playSound(null, pos, SoundEvents.BLOCK_FIRE_AMBIENT,
                SoundCategory.BLOCKS, 1.35F, 0.72F);
        if (world instanceof WorldServer) {
            WorldServer server = (WorldServer) world;
            server.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    pos.getX() + 0.5D, pos.getY() + 0.8D,
                    pos.getZ() + 0.5D,
                    14, 0.7D, 0.25D, 0.7D, 0.04D);
            server.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                    pos.getX() + 0.5D, pos.getY() + 0.8D,
                    pos.getZ() + 0.5D,
                    28, 1.0D, 0.35D, 1.0D, 0.055D);
            server.spawnParticle(EnumParticleTypes.CLOUD,
                    pos.getX() + 0.5D, pos.getY() + 0.65D,
                    pos.getZ() + 0.5D,
                    8, 0.6D, 0.15D, 0.6D, 0.025D);
        }
        return true;
    }

    private static boolean tellGeranFailure(EntityPlayer player, String reason) {
        if (player != null) {
            player.sendMessage(new net.minecraft.util.text.TextComponentTranslation("geran.launch.error." + reason));
        }
        return false;
    }

    private void chargeFromBattery() {
        int slot = isCommunicationRelay() || isStrategicRadar() ? 0 : 2;
        ItemStack stack = inventory.get(slot);
        if (stack.isEmpty() || !(stack.getItem() instanceof IBatteryItem)) return;
        IBatteryItem battery = (IBatteryItem) stack.getItem();
        long amount = Math.min(Math.min(battery.getCharge(stack), battery.getDischargeRate()),
                getMaxPower() - getPower());
        if (amount > 0L) {
            battery.dischargeBattery(stack, amount);
            setPower(getPower() + amount);
        }
    }

    private void updateAirRaidAlarm() {
        boolean active = MissileTrackingService.hasNetworkAlarm(world,
                pos.getX() + 0.5D, pos.getY() + 0.5D,
                pos.getZ() + 0.5D, getOwnerTeam());
        if (!active) {
            for (Entity entity : world.loadedEntityList) {
                if (!(entity instanceof EntityWarTechBase)) {
                    continue;
                }
                EntityWarTechBase vehicle = (EntityWarTechBase) entity;
                if (vehicle.getProfile() != WarTechEntityProfile.COMMAND_TRUCK
                        || !NetworkTeamHelper.areFriendly(getOwnerTeam(),
                                vehicle.getOwnerTeam())
                        || vehicle.getDistanceSqToCenter(pos) > 9216.0D
                        || !vehicle.isLegacyOperational()
                        || vehicle.getLegacyContacts() <= 0) {
                    continue;
                }
                active = true;
                break;
            }
        }
        if (alarmActive != active) {
            alarmActive = active;
            notifyStateChanged();
            world.notifyNeighborsOfStateChange(pos,
                    world.getBlockState(pos).getBlock(), true);
        }
    }

    public void shutdownStrategicRadar() {
        if (world != null && !world.isRemote && isStrategicRadar()) {
            MissileTrackingService.removeRadar(world, radarId());
        }
        warmup = 0;
        setRadarEnabled(false);
    }

    private String blockName() {
        if (world == null || pos == null || !world.isBlockLoaded(pos)) {
            return "";
        }
        Block block = world.getBlockState(pos).getBlock();
        return block.getRegistryName() == null
            ? ""
            : block.getRegistryName().getResourcePath();
    }

    @Override
    public void invalidate() {
        // Forge can invalidate a tile while unloading/replacing its chunk.
        // Keep the persisted node; a later wake validates and prunes destroyed
        // structures without accidentally forgetting an unloaded battery.
        disconnectNetworkServices();
        super.invalidate();
    }

    @Override
    public void onChunkUnload() {
        disconnectNetworkServices();
        super.onChunkUnload();
    }

    private void disconnectNetworkServices() {
        if (world == null || world.isRemote) {
            return;
        }
        MissileChunkLoader.untrackCommunicationNode(this);
        MissileTrackingService.removeCommunicationRelay(world,
                MissileTrackingService.communicationRelayKey(
                        pos.getX(), pos.getY(), pos.getZ()));
        MissileTrackingService.removeRadar(world, radarId());
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setBoolean("LegacyRelayEnabled", relayEnabled);
        compound.setBoolean("LegacyRelayOnline", relayOnline);
        compound.setInteger("WarTechNetworkLaunchCooldown", networkLaunchCooldown);
        compound.setInteger("WarTechOpeningAnimation", openingAnimation);
        compound.setBoolean("WarTechLauncherOpen", open);
        compound.setInteger("WarTechLaunchCountdown", shoot);
        compound.setInteger("openanim", openingAnimation);
        compound.setBoolean("open", open);
        compound.setInteger("shoot", shoot);
        compound.setBoolean("WarTechStrategicRadarFormed", structureFormed);
        compound.setInteger("WarTechStrategicRadarWarmup", warmup);
        compound.setBoolean("WarTechAlarm", alarmActive);
        ItemStackHelper.saveAllItems(compound, inventory);
        writeLegacyInventory(compound);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        relayEnabled = !compound.hasKey("LegacyRelayEnabled")
                || compound.getBoolean("LegacyRelayEnabled");
        relayOnline = compound.getBoolean("LegacyRelayOnline");
        networkLaunchCooldown = Math.max(0,
                compound.getInteger("WarTechNetworkLaunchCooldown"));
        openingAnimation = Math.max(0, Math.min(90,
                compound.hasKey("WarTechOpeningAnimation", 99)
                        ? compound.getInteger("WarTechOpeningAnimation")
                        : compound.getInteger("openanim")));
        open = compound.hasKey("WarTechLauncherOpen")
                ? compound.getBoolean("WarTechLauncherOpen")
                : compound.getBoolean("open");
        shoot = Math.max(0, Math.min(50,
                compound.hasKey("WarTechLaunchCountdown", 99)
                        ? compound.getInteger("WarTechLaunchCountdown")
                        : compound.getInteger("shoot")));
        structureFormed = compound.getBoolean(
                "WarTechStrategicRadarFormed");
        warmup = Math.max(0, Math.min(200,
                compound.getInteger("WarTechStrategicRadarWarmup")));
        alarmActive = compound.getBoolean("WarTechAlarm");
        pendingTargetEntityId = -1;
        for (int index = 0; index < inventory.size(); ++index) {
            inventory.set(index, ItemStack.EMPTY);
        }
        if (compound.hasKey("Items", 9)) {
            ItemStackHelper.loadAllItems(compound, inventory);
        } else {
            readLegacyInventory(compound);
        }
    }

    private void writeLegacyInventory(NBTTagCompound compound) {
        NBTTagList list = new NBTTagList();
        for (int index = 0; index < inventory.size(); ++index) {
            ItemStack stack = inventory.get(index);
            if (stack.isEmpty()) {
                continue;
            }
            NBTTagCompound item = new NBTTagCompound();
            item.setByte("slot", (byte) index);
            stack.writeToNBT(item);
            list.appendTag(item);
        }
        compound.setTag("items", list);
    }

    private void readLegacyInventory(NBTTagCompound compound) {
        NBTTagList list = compound.getTagList("items", 10);
        for (int index = 0; index < list.tagCount(); ++index) {
            NBTTagCompound item = list.getCompoundTagAt(index);
            int slot = item.hasKey("slot", 99)
                    ? item.getByte("slot") & 255
                    : item.getByte("Slot") & 255;
            if (slot >= 0 && slot < inventory.size()) {
                inventory.set(slot, new ItemStack(item));
            }
        }
    }

    @Override public int getSizeInventory() {
        if (isVlsExhaust()) return 9;
        if (isLaunchTube() || isGeranLauncher()) return 3;
        if (isCommunicationRelay() || isStrategicRadar()) return 1;
        return inventory.size();
    }
    @Override public boolean isEmpty() {
        for (int index = 0; index < getSizeInventory(); ++index) {
            if (!inventory.get(index).isEmpty()) return false;
        }
        return true;
    }
    @Override public ItemStack getStackInSlot(int index) {
        return index >= 0 && index < inventory.size() ? inventory.get(index) : ItemStack.EMPTY;
    }
    @Override public ItemStack decrStackSize(int index, int count) {
        ItemStack result = ItemStackHelper.getAndSplit(inventory, index, count);
        if (!result.isEmpty()) {
            markDirty();
            syncInventoryState();
        }
        return result;
    }
    @Override public ItemStack removeStackFromSlot(int index) {
        ItemStack result = ItemStackHelper.getAndRemove(inventory, index);
        if (!result.isEmpty()) {
            markDirty();
            syncInventoryState();
        }
        return result;
    }
    @Override public void setInventorySlotContents(int index, ItemStack stack) {
        if (index < 0 || index >= inventory.size()) return;
        inventory.set(index, stack);
        if (!stack.isEmpty() && stack.getCount() > getInventoryStackLimit()) {
            stack.setCount(getInventoryStackLimit());
        }
        markDirty();
        syncInventoryState();
    }

    private void syncInventoryState() {
        if (world != null && !world.isRemote) {
            notifyStateChanged();
        }
    }
    @Override public String getName() { return "container.wartecmod." + blockName(); }
    @Override public boolean hasCustomName() { return false; }
    @Override public ITextComponent getDisplayName() { return new TextComponentString(getName()); }
    @Override public int getInventoryStackLimit() { return 64; }
    @Override public boolean isUsableByPlayer(EntityPlayer player) {
        return world != null && world.getTileEntity(pos) == this
                && player.getDistanceSqToCenter(pos) <= 256.0D;
    }
    @Override public void openInventory(EntityPlayer player) { }
    @Override public void closeInventory(EntityPlayer player) { }
    @Override public boolean isItemValidForSlot(int index, ItemStack stack) {
        if (isGeranLauncher()) {
            if (index == 0) {
                return isGeranAmmo(stack);
            }
            if (index == 1) {
                return DesignatorCompat.isDesignator(stack);
            }
            return index == 2 && !stack.isEmpty()
                    && stack.getItem() instanceof IBatteryItem;
        }
        if (isLaunchTube()) {
            if (index == 0) {
                return !stack.isEmpty()
                        && (stack.getItem() instanceof IMissileSpawningItem
                                || stack.getItem()
                                        == ModItems.missile_anti_ballistic);
            }
            if (index == 1) {
                return DesignatorCompat.isDesignator(stack)
                        || !stack.isEmpty()
                                && stack.getItem() instanceof ISatChip;
            }
            return index == 2 && !stack.isEmpty()
                    && stack.getItem() instanceof IBatteryItem;
        }
        if (isCommunicationRelay() || isStrategicRadar()) {
            return index == 0 && !stack.isEmpty() && stack.getItem() instanceof IBatteryItem;
        }
        return false;
    }
    @Override public int getField(int id) { return 0; }
    @Override public void setField(int id, int value) { }
    @Override public int getFieldCount() { return 0; }
    @Override public void clear() { inventory.clear(); }
}
