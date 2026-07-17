package com.wartec.wartecmod.entity.vehicle;

import com.wartec.wartecmod.compat.ElectronicWarfareService;
import com.wartec.wartecmod.compat.VehicleEnergyHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public final class EntityElectronicWarfareUnit extends Entity {
    public static final int MODE_JAMMER = 0;
    public static final int MODE_ESM = 1;
    public static final int MODE_DECOY = 2;
    public static final int ENERGY_CAPACITY = 1000000;
    private static final int DW_MODE = 18;
    private static final int DW_ACTIVE = 19;
    private static final int DW_POWER = 20;
    private static final int DW_CONTACTS = 21;
    private static final int DW_BAND = 22;
    private static final double MAX_HEALTH = 240.0D;
    private double unitHealth = MAX_HEALTH;
    private String ownerTeam = "";

    public EntityElectronicWarfareUnit(World world) {
        super(world);
        field_70156_m = true;
        func_70105_a(2.4F, 3.2F);
        updateBounds();
    }

    @Override
    protected void func_70088_a() {
        field_70180_af.func_75682_a(DW_MODE, Byte.valueOf((byte) MODE_JAMMER));
        field_70180_af.func_75682_a(DW_ACTIVE, Byte.valueOf((byte) 1));
        field_70180_af.func_75682_a(DW_POWER, Integer.valueOf(500000));
        field_70180_af.func_75682_a(DW_CONTACTS, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_BAND,
                Byte.valueOf((byte) ElectronicWarfareService.BAND_WIDEBAND));
    }

    public int getMode() {
        return field_70180_af.func_75683_a(DW_MODE);
    }

    public void setMode(int mode) {
        mode = Math.max(MODE_JAMMER, Math.min(MODE_DECOY, mode));
        field_70180_af.func_75692_b(DW_MODE, Byte.valueOf((byte) mode));
        func_70105_a(mode == MODE_DECOY ? 1.4F : 2.4F,
                mode == MODE_ESM ? 4.0F : mode == MODE_DECOY ? 2.0F : 3.2F);
        updateBounds();
    }

    public boolean isActive() {
        return field_70180_af.func_75683_a(DW_ACTIVE) != 0;
    }

    public int getPower() {
        return field_70180_af.func_75679_c(DW_POWER);
    }

    public int getContacts() {
        return field_70180_af.func_75679_c(DW_CONTACTS);
    }

    public int getBand() {
        return field_70180_af.func_75683_a(DW_BAND);
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
        if (!isActive()) {
            ElectronicWarfareService.removeNode(field_70170_p, func_145782_y());
            setContacts(0);
            return;
        }
        int use = getMode() == MODE_JAMMER ? 70 : getMode() == MODE_ESM ? 18 : 8;
        int power = getPower();
        if (power < use) {
            field_70180_af.func_75692_b(DW_ACTIVE, Byte.valueOf((byte) 0));
            ElectronicWarfareService.removeNode(field_70170_p, func_145782_y());
            setContacts(0);
            return;
        }
        field_70180_af.func_75692_b(DW_POWER, Integer.valueOf(power - use));
        if (field_70173_aa % 10 != Math.abs(func_145782_y()) % 10) {
            return;
        }
        if (getMode() == MODE_JAMMER) {
            ElectronicWarfareService.updateJammer(field_70170_p, func_145782_y(),
                    field_70165_t, field_70163_u + 1.5D, field_70161_v,
                    getBand(), ownerTeam);
            setContacts(ElectronicWarfareService.updatePassiveSweep(field_70170_p,
                    field_70165_t, field_70163_u, field_70161_v, 450.0D, ownerTeam));
        } else if (getMode() == MODE_ESM) {
            setContacts(ElectronicWarfareService.updatePassiveSweep(field_70170_p,
                    field_70165_t, field_70163_u + 2.0D, field_70161_v,
                    ElectronicWarfareService.ESM_RANGE, ownerTeam));
        } else {
            ElectronicWarfareService.updateEmitter(field_70170_p, func_145782_y(),
                    field_70165_t, field_70163_u + 1.0D, field_70161_v,
                    ElectronicWarfareService.EMITTER_DECOY, getBand(), ownerTeam);
            setContacts(0);
        }
    }

    private void setContacts(int contacts) {
        if (getContacts() != contacts) {
            field_70180_af.func_75692_b(DW_CONTACTS, Integer.valueOf(contacts));
        }
    }

    @Override
    public boolean func_130002_c(EntityPlayer player) {
        ItemStack held = player.func_71045_bC();
        if (field_70170_p.field_72995_K) {
            return true;
        }
        if (VehicleEnergyHelper.isBattery(held)) {
            int power = VehicleEnergyHelper.chargeFromHeld(player, getPower(), ENERGY_CAPACITY);
            field_70180_af.func_75692_b(DW_POWER, Integer.valueOf(power));
            field_70170_p.func_72956_a(this, "hbm:item.techBleep", 0.7F, 1.05F);
            tell(player, "EW power: " + power + "/" + ENERGY_CAPACITY + " HE");
            return true;
        }
        if (player.func_70093_af()) {
            boolean active = !isActive();
            field_70180_af.func_75692_b(DW_ACTIVE, Byte.valueOf((byte) (active ? 1 : 0)));
            if (!active) {
                ElectronicWarfareService.removeNode(field_70170_p, func_145782_y());
            }
            field_70170_p.func_72956_a(this, "hbm:item.techBleep", 0.8F,
                    active ? 1.25F : 0.75F);
            tell(player, active ? "EW unit online." : "EW unit offline.");
            return true;
        }
        if (getMode() == MODE_JAMMER || getMode() == MODE_DECOY) {
            int band = (getBand() + 1) % 4;
            field_70180_af.func_75692_b(DW_BAND, Byte.valueOf((byte) band));
            tell(player, modeName() + " | band: " + ElectronicWarfareService.bandName(band)
                    + " | " + (isActive() ? "ONLINE" : "OFFLINE"));
        } else {
            tell(player, "Passive ESM | emitters: " + getContacts()
                    + " | range: 900 | " + (isActive() ? "ONLINE" : "OFFLINE"));
        }
        return true;
    }

    private String modeName() {
        return getMode() == MODE_JAMMER ? "Synytsia jammer"
                : getMode() == MODE_ESM ? "Passive ESM array" : "Radar decoy";
    }

    @Override
    public boolean func_70097_a(DamageSource source, float amount) {
        if (field_70170_p.field_72995_K || field_70128_L || amount <= 0.0F) {
            return true;
        }
        if (source.func_76346_g() instanceof EntityPlayer
                && ((EntityPlayer) source.func_76346_g()).field_71075_bZ.field_75098_d) {
            destroyUnit();
            return true;
        }
        unitHealth -= Math.min(70.0D, amount);
        if (unitHealth <= 0.0D) {
            destroyUnit();
        }
        return true;
    }

    private void destroyUnit() {
        ElectronicWarfareService.removeNode(field_70170_p, func_145782_y());
        func_70106_y();
        field_70170_p.func_72885_a(this, field_70165_t, field_70163_u + 1.0D,
                field_70161_v, getMode() == MODE_DECOY ? 2.0F : 3.5F, true, true);
    }

    @Override
    public void func_70106_y() {
        if (field_70170_p != null && !field_70170_p.field_72995_K) {
            ElectronicWarfareService.removeNode(field_70170_p, func_145782_y());
        }
        super.func_70106_y();
    }

    @Override
    public void func_70107_b(double x, double y, double z) {
        super.func_70107_b(x, y, z);
        updateBounds();
    }

    private void updateBounds() {
        double half = getMode() == MODE_DECOY ? 0.7D : 1.2D;
        double height = getMode() == MODE_ESM ? 4.0D : getMode() == MODE_DECOY ? 2.0D : 3.2D;
        field_70121_D.func_72324_b(field_70165_t - half, field_70163_u,
                field_70161_v - half, field_70165_t + half, field_70163_u + height,
                field_70161_v + half);
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
    public boolean func_70067_L() {
        return !field_70128_L;
    }

    @Override
    public boolean func_70112_a(double distance) {
        return distance < 65536.0D;
    }

    @Override
    protected void func_70014_b(NBTTagCompound tag) {
        tag.func_74774_a("EWMode", (byte) getMode());
        tag.func_74757_a("EWActive", isActive());
        tag.func_74768_a("EWPower", getPower());
        tag.func_74774_a("EWBand", (byte) getBand());
        tag.func_74778_a("EWTeam", ownerTeam);
        tag.func_74780_a("EWHealth", unitHealth);
    }

    @Override
    protected void func_70037_a(NBTTagCompound tag) {
        setMode(tag.func_74771_c("EWMode"));
        field_70180_af.func_75692_b(DW_ACTIVE,
                Byte.valueOf((byte) (tag.func_74767_n("EWActive") ? 1 : 0)));
        field_70180_af.func_75692_b(DW_POWER, Integer.valueOf(Math.max(0,
                Math.min(ENERGY_CAPACITY, tag.func_74762_e("EWPower")))));
        field_70180_af.func_75692_b(DW_BAND, Byte.valueOf(tag.func_74771_c("EWBand")));
        ownerTeam = tag.func_74779_i("EWTeam");
        if (tag.func_74764_b("EWHealth")) {
            unitHealth = Math.max(1.0D, Math.min(MAX_HEALTH, tag.func_74769_h("EWHealth")));
        }
    }

    private static void tell(EntityPlayer player, String text) {
        player.func_145747_a(new ChatComponentText(text));
    }
}
