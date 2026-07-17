package com.wartec.wartecmod.entity.vehicle;

import com.wartec.wartecmod.compat.MissileTrackingService;
import com.wartec.wartecmod.compat.IAntiRadiationTarget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public final class EntityRadarTruck extends Entity implements IAntiRadiationTarget {
    public static final double RADAR_RANGE = 600.0D;
    public static final double RADAR_CEILING = 500.0D;
    private static final int DW_ACTIVE = 18;
    private static final int DW_CONTACTS = 19;
    private static final double MAX_HEALTH = 300.0D;
    private double radarHealth = MAX_HEALTH;
    private String ownerTeam = "";

    public EntityRadarTruck(World world) {
        super(world);
        field_70156_m = true;
        func_70105_a(4.2F, 3.0F);
        updateBounds();
    }

    @Override
    protected void func_70088_a() {
        field_70180_af.func_75682_a(DW_ACTIVE, Byte.valueOf((byte) 1));
        field_70180_af.func_75682_a(DW_CONTACTS, Integer.valueOf(0));
    }

    public boolean isRadarActive() {
        return field_70180_af.func_75683_a(DW_ACTIVE) != 0;
    }

    public int getRadarContacts() {
        return field_70180_af.func_75679_c(DW_CONTACTS);
    }

    public void setOwnerTeam(String team) {
        ownerTeam = team == null ? "" : team;
    }

    @Override
    public void func_70071_h_() {
        super.func_70071_h_();
        field_70159_w = 0.0D;
        field_70181_x = 0.0D;
        field_70179_y = 0.0D;
        updateBounds();
        if (field_70170_p.field_72995_K) {
            return;
        }
        if (!isRadarActive()) {
            MissileTrackingService.removeRadar(field_70170_p, func_145782_y());
            if (getRadarContacts() != 0) {
                field_70180_af.func_75692_b(DW_CONTACTS, Integer.valueOf(0));
            }
            return;
        }
        if (field_70173_aa % 10 == Math.abs(func_145782_y()) % 10) {
            int contacts = MissileTrackingService.updateRadarSweep(field_70170_p,
                    func_145782_y(), field_70165_t, field_70163_u + 2.5D, field_70161_v,
                    RADAR_RANGE, RADAR_CEILING, Integer.MAX_VALUE, ownerTeam,
                    com.wartec.wartecmod.compat.ElectronicWarfareService.BAND_S);
            field_70180_af.func_75692_b(DW_CONTACTS, Integer.valueOf(contacts));
        }
    }

    @Override
    public void func_70107_b(double x, double y, double z) {
        super.func_70107_b(x, y, z);
        updateBounds();
    }

    private void updateBounds() {
        double halfWidth = 2.1D;
        field_70121_D.func_72324_b(field_70165_t - halfWidth, field_70163_u,
                field_70161_v - halfWidth, field_70165_t + halfWidth,
                field_70163_u + 3.0D, field_70161_v + halfWidth);
    }

    @Override
    public AxisAlignedBB func_70046_E() {
        return field_70121_D;
    }

    @Override
    public AxisAlignedBB func_70114_g(Entity entity) {
        return entity.field_70121_D;
    }

    @Override
    public boolean func_70104_M() {
        return true;
    }

    @Override
    public float func_70111_Y() {
        return 0.6F;
    }

    @Override
    public boolean func_70067_L() {
        return !field_70128_L;
    }

    @Override
    public boolean func_130002_c(EntityPlayer player) {
        if (field_70170_p.field_72995_K) {
            return true;
        }
        if (player.func_70093_af()) {
            boolean active = !isRadarActive();
            field_70180_af.func_75692_b(DW_ACTIVE, Byte.valueOf((byte) (active ? 1 : 0)));
            if (!active) {
                MissileTrackingService.removeRadar(field_70170_p, func_145782_y());
                field_70180_af.func_75692_b(DW_CONTACTS, Integer.valueOf(0));
            }
            field_70170_p.func_72956_a(this, "hbm:item.techBleep", 0.8F,
                    active ? 1.25F : 0.75F);
            tell(player, active ? "Radar network online." : "Radar network offline.");
            return true;
        }
        tell(player, "TRM radar: " + (isRadarActive() ? "ONLINE" : "OFFLINE")
                + " | contacts: " + getRadarContacts()
                + " | range: 600 | ceiling: 500");
        return true;
    }

    @Override
    public boolean func_70097_a(DamageSource source, float amount) {
        if (field_70170_p.field_72995_K || field_70128_L || amount <= 0.0F) {
            return true;
        }
        if (source.func_76346_g() instanceof EntityPlayer
                && ((EntityPlayer) source.func_76346_g()).field_71075_bZ.field_75098_d) {
            destroyRadar();
            return true;
        }
        radarHealth -= amount;
        if (radarHealth <= 0.0D) {
            destroyRadar();
        } else {
            field_70170_p.func_72956_a(this, "random.anvil_land", 0.35F, 1.5F);
        }
        return true;
    }

    private void destroyRadar() {
        MissileTrackingService.removeRadar(field_70170_p, func_145782_y());
        func_70106_y();
        field_70170_p.func_72885_a(this, field_70165_t, field_70163_u + 1.5D,
                field_70161_v, 3.5F, true, true);
    }

    @Override
    public void wartecDestroyByAntiRadiationMissile() {
        if (!field_70128_L && !field_70170_p.field_72995_K) {
            MissileTrackingService.removeRadar(field_70170_p, func_145782_y());
            func_70106_y();
        }
    }

    @Override
    public void func_70106_y() {
        if (field_70170_p != null && !field_70170_p.field_72995_K) {
            MissileTrackingService.removeRadar(field_70170_p, func_145782_y());
        }
        super.func_70106_y();
    }

    @Override
    protected void func_70014_b(NBTTagCompound tag) {
        tag.func_74757_a("RadarActive", isRadarActive());
        tag.func_74780_a("RadarHealth", radarHealth);
        tag.func_74778_a("RadarTeam", ownerTeam);
    }

    @Override
    protected void func_70037_a(NBTTagCompound tag) {
        boolean active = !tag.func_74764_b("RadarActive") || tag.func_74767_n("RadarActive");
        field_70180_af.func_75692_b(DW_ACTIVE, Byte.valueOf((byte) (active ? 1 : 0)));
        if (tag.func_74764_b("RadarHealth")) {
            radarHealth = Math.max(1.0D, Math.min(MAX_HEALTH, tag.func_74769_h("RadarHealth")));
        }
        ownerTeam = tag.func_74779_i("RadarTeam");
    }

    private static void tell(EntityPlayer player, String text) {
        player.func_145747_a(new ChatComponentText(text));
    }
}
