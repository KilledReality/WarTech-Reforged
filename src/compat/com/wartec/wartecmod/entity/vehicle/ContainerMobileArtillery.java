package com.wartec.wartecmod.entity.vehicle;

import com.hbm.inventory.container.ContainerTurretBase;
import com.hbm.tileentity.turret.TileEntityTurretBaseNT;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;

public final class ContainerMobileArtillery extends ContainerTurretBase {
    private final EntityMobileArtillery vehicle;

    public ContainerMobileArtillery(InventoryPlayer inventory,
            TileEntityTurretBaseNT turret, EntityMobileArtillery vehicle) {
        super(inventory, turret);
        this.vehicle = vehicle;
    }

    @Override
    public boolean func_75145_c(EntityPlayer player) {
        return vehicle != null
                && !vehicle.field_70128_L
                && vehicle.isDeployed()
                && player.func_70092_e(vehicle.field_70165_t,
                        vehicle.field_70163_u + 1.5D, vehicle.field_70161_v) <= 256.0D;
    }
}
