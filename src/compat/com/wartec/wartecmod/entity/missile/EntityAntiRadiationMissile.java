package com.wartec.wartecmod.entity.missile;

import com.wartec.wartecmod.compat.ElectronicWarfareService;
import com.wartec.wartecmod.compat.ElectronicWarfareService.EmitterTarget;
import com.wartec.wartecmod.compat.IAntiRadiationTarget;
import com.wartec.wartecmod.compat.RadarNetworkContent;
import com.wartec.wartecmod.tileentity.vls.TileEntityVlsExhaust;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public final class EntityAntiRadiationMissile extends EntityKalibrMissile {
    private int emitterId = -1;
    private double lastEmitterX;
    private double lastEmitterY;
    private double lastEmitterZ;
    private long lastSignalTick = -1L;
    private int searchX;
    private int searchZ;

    public EntityAntiRadiationMissile(World world) {
        super(world);
    }

    public EntityAntiRadiationMissile(World world, float x, float y, float z,
            int targetX, int targetZ, TileEntityVlsExhaust exhaust) {
        super(world, x, y, z, targetX, targetZ, exhaust);
        this.searchX = targetX;
        this.searchZ = targetZ;
    }

    @Override
    public void func_70071_h_() {
        if (!field_70170_p.field_72995_K && (field_70173_aa % 3) == 0) {
            updateEmitterGuidance();
        }
        if (!field_70170_p.field_72995_K && shouldProximityDetonate()) {
            onImpact();
            func_70106_y();
            return;
        }
        super.func_70071_h_();
        if (!field_70170_p.field_72995_K && !field_70128_L) {
            applyNaturalGuidance();
        }
    }

    private void updateEmitterGuidance() {
        long now = field_70170_p.func_82737_E();
        EmitterTarget emitter = emitterId <= 0 ? null
                : ElectronicWarfareService.getEmitter(field_70170_p, emitterId);
        if (emitter == null && emitterId <= 0) {
            emitter = ElectronicWarfareService.findBestEmitter(field_70170_p,
                    searchX, searchZ, ElectronicWarfareService.ARM_RANGE, "");
        }
        EmitterTarget homeOnJam = ElectronicWarfareService.findBestEmitter(field_70170_p,
                field_70165_t, field_70161_v, 450.0D, "");
        if (homeOnJam != null && homeOnJam.type == ElectronicWarfareService.EMITTER_JAMMER
                && (emitter == null || emitter.type != ElectronicWarfareService.EMITTER_JAMMER)) {
            emitter = homeOnJam;
        }
        if (emitter != null) {
            emitterId = emitter.entityId;
            lastEmitterX = emitter.x;
            lastEmitterY = emitter.y;
            lastEmitterZ = emitter.z;
            lastSignalTick = now;
            targetX = (int) Math.floor(lastEmitterX);
            targetY = (int) Math.floor(lastEmitterY);
            targetZ = (int) Math.floor(lastEmitterZ);
            return;
        }
        if (lastSignalTick < 0L) {
            return;
        }
        long silentTicks = now - lastSignalTick;
        if (silentTicks > 30L && (silentTicks % 20L) < 5L) {
            double error = Math.min(96.0D, 4.0D + silentTicks * 0.11D);
            double angle = field_70170_p.field_73012_v.nextDouble() * Math.PI * 2.0D;
            double radius = field_70170_p.field_73012_v.nextDouble() * error;
            targetX = (int) Math.floor(lastEmitterX + Math.cos(angle) * radius);
            targetZ = (int) Math.floor(lastEmitterZ + Math.sin(angle) * radius);
        }
    }

    private void applyNaturalGuidance() {
        double aimX = targetX + 0.5D;
        double aimZ = targetZ + 0.5D;
        double groundY = lastSignalTick >= 0L ? lastEmitterY
                : field_70170_p.func_72976_f(targetX, targetZ) + 1.0D;
        double dx = aimX - field_70165_t;
        double dz = aimZ - field_70161_v;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        if (horizontalDistance < 0.001D) {
            return;
        }

        double desiredY;
        if (field_70173_aa < 24) {
            desiredY = Math.max(startY + 34.0D, groundY + 26.0D);
        } else if (horizontalDistance > 110.0D) {
            desiredY = groundY + Math.min(64.0D,
                    Math.max(28.0D, horizontalDistance * 0.12D));
        } else {
            double terminal = smoothStep(horizontalDistance / 110.0D);
            desiredY = groundY + 1.5D + terminal * 30.0D;
        }

        double speed = Math.sqrt(field_70159_w * field_70159_w
                + field_70181_x * field_70181_x + field_70179_y * field_70179_y);
        speed = clamp(speed, 0.34D, 0.78D);
        double desiredVertical = clamp((desiredY - field_70163_u) * 0.035D,
                -0.52D, field_70173_aa < 24 ? 0.34D : 0.24D);
        double desiredHorizontal = Math.sqrt(Math.max(0.04D,
                speed * speed - desiredVertical * desiredVertical));
        double desiredX = dx / horizontalDistance * desiredHorizontal;
        double desiredZ = dz / horizontalDistance * desiredHorizontal;
        double turn = horizontalDistance < 110.0D ? 0.24D : 0.11D;
        field_70159_w = blend(field_70159_w, desiredX, turn);
        field_70181_x = blend(field_70181_x, desiredVertical, turn);
        field_70179_y = blend(field_70179_y, desiredZ, turn);

        double correctedSpeed = Math.sqrt(field_70159_w * field_70159_w
                + field_70181_x * field_70181_x + field_70179_y * field_70179_y);
        if (correctedSpeed > 0.001D) {
            double scale = speed / correctedSpeed;
            field_70159_w *= scale;
            field_70181_x *= scale;
            field_70179_y *= scale;
        }
        rotation();
    }

    private boolean shouldProximityDetonate() {
        Entity locked = emitterId <= 0 ? null : field_70170_p.func_73045_a(emitterId);
        if (!(locked instanceof IAntiRadiationTarget) || locked.field_70128_L) {
            return false;
        }
        double dx = locked.field_70165_t - field_70165_t;
        double dy = locked.field_70163_u + 1.5D - field_70163_u;
        double dz = locked.field_70161_v - field_70161_v;
        return dx * dx + dy * dy + dz * dz <= 49.0D;
    }

    @Override
    public void onImpact() {
        if (!field_70170_p.field_72995_K) {
            destroyElectronicTargets();
        }
        super.onImpact();
    }

    private void destroyElectronicTargets() {
        Entity locked = emitterId <= 0 ? null : field_70170_p.func_73045_a(emitterId);
        destroyIfClose(locked, 28.0D);
        List entities = field_70170_p.field_72996_f == null
                ? new ArrayList() : new ArrayList(field_70170_p.field_72996_f);
        for (Object value : entities) {
            if (value instanceof Entity && value != locked) {
                destroyIfClose((Entity) value, 12.0D);
            }
        }
    }

    private void destroyIfClose(Entity entity, double range) {
        if (!(entity instanceof IAntiRadiationTarget) || entity.field_70128_L) {
            return;
        }
        double dx = entity.field_70165_t - field_70165_t;
        double dy = entity.field_70163_u - field_70163_u;
        double dz = entity.field_70161_v - field_70161_v;
        if (dx * dx + dy * dy + dz * dz <= range * range) {
            ((IAntiRadiationTarget) entity).wartecDestroyByAntiRadiationMissile();
        }
    }

    private static double blend(double current, double desired, double amount) {
        return current + (desired - current) * amount;
    }

    private static double smoothStep(double value) {
        value = clamp(value, 0.0D, 1.0D);
        return value * value * (3.0D - 2.0D * value);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public ItemStack getDebrisRareDrop() {
        return new ItemStack(RadarNetworkContent.antiRadiationMissile);
    }

    @Override
    protected void func_70014_b(NBTTagCompound tag) {
        super.func_70014_b(tag);
        tag.func_74768_a("ArmEmitter", emitterId);
        tag.func_74780_a("ArmLastX", lastEmitterX);
        tag.func_74780_a("ArmLastY", lastEmitterY);
        tag.func_74780_a("ArmLastZ", lastEmitterZ);
        tag.func_74772_a("ArmSignal", lastSignalTick);
        tag.func_74768_a("ArmSearchX", searchX);
        tag.func_74768_a("ArmSearchZ", searchZ);
    }

    @Override
    protected void func_70037_a(NBTTagCompound tag) {
        super.func_70037_a(tag);
        emitterId = tag.func_74762_e("ArmEmitter");
        lastEmitterX = tag.func_74769_h("ArmLastX");
        lastEmitterY = tag.func_74769_h("ArmLastY");
        lastEmitterZ = tag.func_74769_h("ArmLastZ");
        lastSignalTick = tag.func_74763_f("ArmSignal");
        searchX = tag.func_74762_e("ArmSearchX");
        searchZ = tag.func_74762_e("ArmSearchZ");
    }
}
