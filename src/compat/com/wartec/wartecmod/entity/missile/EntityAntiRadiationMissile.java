package com.wartec.wartecmod.entity.missile;

import com.wartec.wartecmod.compat.ElectronicWarfareService;
import com.wartec.wartecmod.compat.ElectronicWarfareService.EmitterTarget;
import com.wartec.wartecmod.compat.RadarNetworkContent;
import com.wartec.wartecmod.tileentity.vls.TileEntityVlsExhaust;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public final class EntityAntiRadiationMissile extends EntityKalibrMissile {
    private int emitterId = -1;
    private double lastEmitterX;
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
        if (!field_70170_p.field_72995_K && (field_70173_aa % 5) == 0) {
            updateEmitterGuidance();
        }
        super.func_70071_h_();
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
            lastEmitterZ = emitter.z;
            lastSignalTick = now;
            targetX = (int) Math.floor(lastEmitterX);
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

    @Override
    public ItemStack getDebrisRareDrop() {
        return new ItemStack(RadarNetworkContent.antiRadiationMissile);
    }

    @Override
    protected void func_70014_b(NBTTagCompound tag) {
        super.func_70014_b(tag);
        tag.func_74768_a("ArmEmitter", emitterId);
        tag.func_74780_a("ArmLastX", lastEmitterX);
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
        lastEmitterZ = tag.func_74769_h("ArmLastZ");
        lastSignalTick = tag.func_74763_f("ArmSignal");
        searchX = tag.func_74762_e("ArmSearchX");
        searchZ = tag.func_74762_e("ArmSearchZ");
    }
}
