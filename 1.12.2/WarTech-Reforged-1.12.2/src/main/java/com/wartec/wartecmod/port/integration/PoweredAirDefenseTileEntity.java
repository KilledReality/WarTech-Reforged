package com.wartec.wartecmod.port.integration;

import com.wartec.wartecmod.port.integration.HbmRadarScanner.RadarContact;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Powered radar base with deterministic nearest-target engagement scheduling.
 */
public abstract class PoweredAirDefenseTileEntity extends PoweredRadarTileEntity {
    private static final String COOLDOWN_KEY = "WarTechEngagementCooldown";
    private int engagementCooldown;
    private int currentTargetEntityId = -1;

    @Override
    protected void afterRadarTick() {
        if (engagementCooldown > 0) {
            engagementCooldown--;
        }

        if (!isRadarOperational() || engagementCooldown > 0) {
            return;
        }

        RadarContact selectedContact = selectTarget();
        currentTargetEntityId = selectedContact == null
            ? -1
            : selectedContact.getEntity().getEntityId();
        if (selectedContact == null) {
            return;
        }

        long engagementEnergy = Math.max(0L, getEngagementEnergyCost());
        Entity target = selectedContact.getEntity();
        if (hasPower(engagementEnergy) && engageTarget(target)) {
            consumePower(engagementEnergy);
            engagementCooldown = Math.max(1, getEngagementCooldownTicks());
            notifyStateChanged();
        }
    }

    public final int getEngagementCooldown() {
        return engagementCooldown;
    }

    public final int getCurrentTargetEntityId() {
        return currentTargetEntityId;
    }

    protected long getEngagementEnergyCost() {
        return 0L;
    }

    protected abstract int getEngagementCooldownTicks();

    /**
     * Return true only after an interceptor was actually launched.
     */
    protected abstract boolean engageTarget(Entity target);

    protected boolean canEngageTarget(RadarContact contact) {
        Entity entity = contact.getEntity();
        return entity != null && !entity.isDead;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger(COOLDOWN_KEY, engagementCooldown);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        engagementCooldown = Math.max(0, compound.getInteger(COOLDOWN_KEY));
        currentTargetEntityId = -1;
    }

    private RadarContact selectTarget() {
        for (RadarContact contact : getRadarContacts()) {
            if (canEngageTarget(contact)) {
                return contact;
            }
        }
        return null;
    }
}
