package com.wartec.wartecmod.port.integration;

import api.hbm.entity.IRadarDetectable;
import com.wartec.wartecmod.port.integration.HbmRadarScanner.RadarContact;
import java.util.Collections;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;

/**
 * Functional server-side radar base using NTM Extended HE and radar contracts.
 */
public abstract class PoweredRadarTileEntity extends HbmPoweredTileEntity implements ITickable {
    private static final String RADAR_ENABLED_KEY = "WarTechRadarEnabled";
    private boolean radarEnabled = true;
    private boolean radarOperational;
    private int scanTicker;
    private List<RadarContact> contacts = Collections.emptyList();

    @Override
    public void update() {
        if (world == null || world.isRemote) {
            return;
        }

        updateEnergyConnections();
        boolean wasOperational = radarOperational;
        radarOperational = radarEnabled && updateRadarPowerAndGate();
        if (!radarOperational) {
            clearContacts();
        } else if (++scanTicker >= Math.max(1, getRadarScanIntervalTicks())) {
            scanTicker = 0;
            contacts = HbmRadarScanner.scan(
                world,
                pos,
                getRadarHorizontalRange(),
                getRadarVerticalRange(),
                Math.max(1, getRadarContactLimit()),
                getOwnerTeam(),
                excludeFriendlyContacts(),
                new HbmRadarScanner.TargetFilter() {
                    @Override
                    public boolean accept(Entity entity, IRadarDetectable.RadarTargetType targetType) {
                        return isValidRadarTarget(entity, targetType);
                    }
                },getRadarSensorOffset()
            );
            onRadarContactsUpdated(contacts);
        }

        if (wasOperational != radarOperational) {
            notifyStateChanged();
        }
        afterRadarTick();
    }

    public final boolean isRadarEnabled() {
        return radarEnabled;
    }

    public final void setRadarEnabled(boolean enabled) {
        if (radarEnabled != enabled) {
            radarEnabled = enabled;
            if (!enabled) {
                radarOperational = false;
                clearContacts();
            }
            notifyStateChanged();
        }
    }

    public final boolean isRadarOperational() {
        return radarOperational;
    }

    public final void setClientRadarOperational(boolean operational) {
        if (world != null && world.isRemote) {
            radarOperational = operational;
        }
    }

    public final List<RadarContact> getRadarContacts() {
        return contacts;
    }

    protected abstract double getRadarHorizontalRange();

    protected abstract double getRadarVerticalRange();

    protected abstract long getRadarEnergyUsePerTick();

    protected boolean updateRadarPowerAndGate() {
        return consumePower(Math.max(0L, getRadarEnergyUsePerTick()));
    }

    protected int getRadarScanIntervalTicks() {
        return 10;
    }

    protected int getRadarContactLimit() {
        return 64;
    }
    protected double getRadarSensorOffset() { return 2.5; }

    protected boolean excludeFriendlyContacts() {
        return true;
    }

    protected boolean isValidRadarTarget(
        Entity entity,
        IRadarDetectable.RadarTargetType targetType
    ) {
        return true;
    }

    protected void onRadarContactsUpdated(List<RadarContact> updatedContacts) {
    }

    protected void afterRadarTick() {
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setBoolean(RADAR_ENABLED_KEY, radarEnabled);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        radarEnabled = !compound.hasKey(RADAR_ENABLED_KEY)
            || compound.getBoolean(RADAR_ENABLED_KEY);
        radarOperational = false;
        scanTicker = 0;
        contacts = Collections.emptyList();
    }

    private void clearContacts() {
        if (!contacts.isEmpty()) {
            contacts = Collections.emptyList();
            onRadarContactsUpdated(contacts);
        }
        scanTicker = 0;
    }
}
