package com.wartec.wartecmod.entity.vehicle;

import com.hbm.tileentity.turret.TileEntityTurretHIMARS;
import com.wartec.wartecmod.compat.HbmTilePowerLink;
import com.wartec.wartecmod.compat.MissileTrackingService;
import net.minecraft.util.Vec3;

public final class MobileTileHimars extends TileEntityTurretHIMARS {
    private String ownerTeam = "";

    public void setOwnerTeam(String team) {
        ownerTeam = team == null ? "" : team;
    }

    @Override
    public void spawnShell(int type) {
        super.spawnShell(type);
        MissileTrackingService.assignNewestArtilleryProjectile(
                field_145850_b, field_145851_c + 0.5D,
                field_145848_d + 1.25D, field_145849_e + 0.5D,
                ownerTeam, true);
    }

    @Override
    public void handleButtonPacket(int value, int id) {
        super.handleButtonPacket(value, id);
        func_70296_d();
    }

    @Override
    public Vec3 getHorizontalOffset() {
        return Vec3.func_72443_a(0.5D, 0.0D, 0.5D);
    }

    @Override
    public Vec3 getTurretPos() {
        return Vec3.func_72443_a(field_145851_c + 0.5D,
                field_145848_d + 1.25D, field_145849_e + 0.5D);
    }

    @Override
    protected void updateConnections() {
        super.updateConnections();
        HbmTilePowerLink.subscribeNearby(this, this, 7, 4);
    }
}
