package com.wartec.wartecmod.entity.vehicle;

import api.hbm.item.IDesignatorItem;
import com.hbm.entity.projectile.EntityArtilleryRocket;
import com.hbm.entity.projectile.EntityArtilleryShell;
import com.hbm.blocks.ModBlocks;
import com.hbm.items.ModItems;
import com.wartec.wartecmod.compat.MobileArtilleryContent;
import com.wartec.wartecmod.compat.HeavyVehicleDynamics;
import com.wartec.wartecmod.compat.HeavyVehicleDynamics.Motion;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.turret.TileEntityTurretBaseArtillery;
import com.hbm.tileentity.turret.TileEntityTurretArty;
import com.hbm.tileentity.turret.TileEntityTurretHIMARS;
import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public final class EntityMobileArtillery extends Entity implements IGUIProvider {
    public static final int MOUNT_NONE = 0;
    public static final int MOUNT_GREG = 1;
    public static final int MOUNT_HENRY = 2;
    private static final double MAX_HEALTH = 500.0D;
    private static final double MAX_DAMAGE_PER_HIT = 75.0D;

    private static final int DW_MOUNT = 18;
    private static final int DW_DEPLOYED = 19;
    private static final int DW_AMMO_TYPE = 20;
    private static final int DW_AMMO_COUNT = 21;
    private static final int DW_TARGET_X = 22;
    private static final int DW_TARGET_Y = 23;
    private static final int DW_TARGET_Z = 24;
    private static final int DW_TURRET_YAW = 25;
    private static final int DW_TURRET_PITCH = 26;
    private static final int DW_PROXY_X = 27;
    private static final int DW_PROXY_Y = 28;
    private static final int DW_PROXY_Z = 29;
    private static final int DW_CONTROL_FLAGS = 30;
    private static final int DW_POWER = 31;

    private double driveSpeed;
    private double steeringState;
    private double vehicleHealth = MAX_HEALTH;
    private int fireCooldown;
    private NBTTagCompound ammoTag;
    private TileEntityTurretBaseArtillery mobileTurret;
    private NBTTagCompound mobileTurretData;
    private int proxyX;
    private int proxyY;
    private int proxyZ;
    private double clientTargetX;
    private double clientTargetY;
    private double clientTargetZ;
    private float clientTargetYaw;
    private float clientTargetPitch;
    private int clientInterpolationTicks;

    public EntityMobileArtillery(World world) {
        super(world);
        field_70156_m = true;
        func_70105_a(3.0F, 2.35F);
        field_70129_M = 0.0F;
        field_70138_W = 1.1F;
        updateVehicleBounds();
    }

    public EntityMobileArtillery(World world, int mount) {
        this(world);
        setMount(mount);
    }

    @Override
    protected void func_70088_a() {
        field_70180_af.func_75682_a(DW_MOUNT, Byte.valueOf((byte) MOUNT_NONE));
        field_70180_af.func_75682_a(DW_DEPLOYED, Byte.valueOf((byte) 0));
        field_70180_af.func_75682_a(DW_AMMO_TYPE, Integer.valueOf(-1));
        field_70180_af.func_75682_a(DW_AMMO_COUNT, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_TARGET_X, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_TARGET_Y, Integer.valueOf(-4096));
        field_70180_af.func_75682_a(DW_TARGET_Z, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_TURRET_YAW, Float.valueOf(0.0F));
        field_70180_af.func_75682_a(DW_TURRET_PITCH, Float.valueOf(0.0F));
        field_70180_af.func_75682_a(DW_PROXY_X, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_PROXY_Y, Integer.valueOf(-4096));
        field_70180_af.func_75682_a(DW_PROXY_Z, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_CONTROL_FLAGS, Integer.valueOf(0));
        field_70180_af.func_75682_a(DW_POWER, Integer.valueOf(0));
    }

    public int getMount() {
        return field_70180_af.func_75683_a(DW_MOUNT) & 255;
    }

    public void setMount(int mount) {
        field_70180_af.func_75692_b(DW_MOUNT,
                Byte.valueOf((byte) Math.max(MOUNT_NONE, Math.min(MOUNT_HENRY, mount))));
    }

    public boolean isDeployed() {
        return field_70180_af.func_75683_a(DW_DEPLOYED) != 0;
    }

    public int getAmmoType() {
        return field_70180_af.func_75679_c(DW_AMMO_TYPE);
    }

    public int getAmmoCount() {
        return field_70180_af.func_75679_c(DW_AMMO_COUNT);
    }

    public float getTurretYaw() {
        return field_70180_af.func_111145_d(DW_TURRET_YAW);
    }

    public float getTurretPitch() {
        return field_70180_af.func_111145_d(DW_TURRET_PITCH);
    }

    public boolean hasTarget() {
        return field_70180_af.func_75679_c(DW_TARGET_Y) > -2048;
    }

    @Override
    public void func_70071_h_() {
        super.func_70071_h_();
        updateVehicleBounds();
        if (fireCooldown > 0) {
            fireCooldown--;
        }
        if (field_70170_p.field_72995_K) {
            updateClientInterpolation();
            if (isDeployed()) {
                ensureMobileTurret();
                applyMobileTurretState();
            } else if (mobileTurret != null) {
                detachMobileTurret(false);
            }
            return;
        }

        if (isDeployed()) {
            ensureMobileTurret();
            syncMobileTurret();
        } else if (mobileTurret != null) {
            detachMobileTurret(true);
        }

        if (mobileTurret == null) {
            updateAim();
        }
        if (isDeployed()) {
            driveSpeed *= 0.55D;
            field_70159_w = 0.0D;
            field_70179_y = 0.0D;
            enforceDeployedCollision();
        } else {
            updateDriving();
        }

        if (!field_70122_E) {
            field_70181_x -= 0.08D;
        } else if (field_70181_x < 0.0D) {
            field_70181_x = 0.0D;
        }
        if (field_70123_F && field_70122_E && Math.abs(driveSpeed) > 0.04D) {
            field_70181_x = Math.max(field_70181_x, 0.20D);
        }
        double oldY = field_70163_u;
        func_70091_d(field_70159_w, field_70181_x, field_70179_y);
        field_70125_A = HeavyVehicleDynamics.suspensionPitch(
                field_70125_A, field_70163_u - oldY);
        updateVehicleBounds();
        field_70159_w *= 0.72D;
        field_70179_y *= 0.72D;
        field_70181_x *= 0.98D;
    }

    private void updateClientInterpolation() {
        if (clientInterpolationTicks <= 0) {
            return;
        }
        double fraction = 1.0D / clientInterpolationTicks;
        double x = field_70165_t + (clientTargetX - field_70165_t) * fraction;
        double y = field_70163_u + (clientTargetY - field_70163_u) * fraction;
        double z = field_70161_v + (clientTargetZ - field_70161_v) * fraction;
        double yawDelta = clientTargetYaw - field_70177_z;
        while (yawDelta < -180.0D) yawDelta += 360.0D;
        while (yawDelta >= 180.0D) yawDelta -= 360.0D;
        field_70177_z = (float) (field_70177_z + yawDelta * fraction);
        field_70125_A = (float) (field_70125_A
                + (clientTargetPitch - field_70125_A) * fraction);
        func_70107_b(x, y, z);
        clientInterpolationTicks--;
    }

    @Override
    public void func_70056_a(double x, double y, double z,
            float yaw, float pitch, int increments) {
        if (!field_70170_p.field_72995_K) {
            func_70012_b(x, y, z, yaw, pitch);
            return;
        }
        clientTargetX = x;
        clientTargetY = y;
        clientTargetZ = z;
        clientTargetYaw = yaw;
        clientTargetPitch = pitch;
        clientInterpolationTicks = Math.max(3, increments);
    }

    @Override
    public void func_70107_b(double x, double y, double z) {
        super.func_70107_b(x, y, z);
        updateVehicleBounds();
    }

    private void updateVehicleBounds() {
        double halfWidth = 1.18D;
        double height = 2.30D;
        field_70121_D.func_72324_b(
                field_70165_t - halfWidth, field_70163_u, field_70161_v - halfWidth,
                field_70165_t + halfWidth, field_70163_u + height,
                field_70161_v + halfWidth);
    }

    @Override
    public float func_70111_Y() {
        return field_70180_af != null && isDeployed() ? 1.45F : 0.35F;
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

    private void updateDriving() {
        float forward = 0.0F;
        float steering = 0.0F;
        if (field_70153_n instanceof EntityPlayer) {
            EntityPlayer driver = (EntityPlayer) field_70153_n;
            forward = driver.field_70701_bs;
            steering = driver.field_70702_br;
        }
        Motion motion = HeavyVehicleDynamics.step(driveSpeed, steeringState,
                field_70177_z, forward, steering, 0.38D, 0.18D,
                field_70122_E, field_70123_F);
        driveSpeed = motion.speed;
        steeringState = motion.steering;
        field_70177_z = motion.yaw;
        field_70159_w = motion.motionX;
        field_70179_y = motion.motionZ;
        if (field_70153_n != null && Math.abs(driveSpeed) > 0.04D
                && field_70173_aa % 24 == 0) {
            field_70170_p.func_72956_a(this, "minecart.base", 0.30F,
                    (float) (0.80D + Math.min(0.38D, Math.abs(driveSpeed))));
        }
    }

    private void enforceDeployedCollision() {
        AxisAlignedBB bodyArea = AxisAlignedBB.func_72330_a(
                field_70165_t - 4.0D, field_70163_u, field_70161_v - 4.0D,
                field_70165_t + 4.0D, field_70163_u + 2.35D,
                field_70161_v + 4.0D);
        java.util.List entities = field_70170_p.func_72839_b(this, bodyArea);
        if (entities == null) {
            return;
        }
        double yaw = Math.toRadians(field_70177_z);
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        for (Object raw : entities) {
            if (!(raw instanceof EntityPlayer)) {
                continue;
            }
            EntityPlayer player = (EntityPlayer) raw;
            if (player.field_70154_o == this
                    || player.field_70163_u >= field_70163_u + 2.30D
                    || player.field_70163_u + 1.8D <= field_70163_u) {
                continue;
            }
            double dx = player.field_70165_t - field_70165_t;
            double dz = player.field_70161_v - field_70161_v;
            double localX = dx * cos + dz * sin;
            double localZ = -dx * sin + dz * cos;
            double limitX = 1.42D;
            double limitZ = 3.30D;
            if (Math.abs(localX) >= limitX || Math.abs(localZ) >= limitZ) {
                continue;
            }
            double penetrationX = limitX - Math.abs(localX);
            double penetrationZ = limitZ - Math.abs(localZ);
            if (penetrationX < penetrationZ) {
                localX = localX < 0.0D ? -limitX : limitX;
            } else {
                localZ = localZ < 0.0D ? -limitZ : limitZ;
            }
            double worldX = field_70165_t + localX * cos - localZ * sin;
            double worldZ = field_70161_v + localX * sin + localZ * cos;
            player.field_70159_w = 0.0D;
            player.field_70179_y = 0.0D;
            player.func_70107_b(worldX, player.field_70163_u, worldZ);
        }
    }

    private void updateAim() {
        if (!hasTarget()) {
            field_70180_af.func_75692_b(DW_TURRET_YAW, Float.valueOf(field_70177_z));
            field_70180_af.func_75692_b(DW_TURRET_PITCH, Float.valueOf(0.0F));
            return;
        }
        double dx = field_70180_af.func_75679_c(DW_TARGET_X) + 0.5D - field_70165_t;
        double dy = field_70180_af.func_75679_c(DW_TARGET_Y) + 0.5D - (field_70163_u + 2.0D);
        double dz = field_70180_af.func_75679_c(DW_TARGET_Z) + 0.5D - field_70161_v;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        if (getMount() == MOUNT_GREG) {
            pitch = (float) -Math.toDegrees(ballisticAngle(horizontal, dy, 50.0D, 0.4905D));
        }
        field_70180_af.func_75692_b(DW_TURRET_YAW, Float.valueOf(yaw));
        field_70180_af.func_75692_b(DW_TURRET_PITCH, Float.valueOf(pitch));
    }

    @Override
    public boolean func_130002_c(EntityPlayer player) {
        if (field_70170_p.field_72995_K) {
            return true;
        }
        ItemStack held = player.func_71045_bC();
        if (held != null && installModule(player, held)) {
            return true;
        }
        if (held != null && held.func_77973_b() == ModItems.designator_arty_range) {
            linkArtilleryDesignator(player, held);
            return true;
        }
        if (held != null && held.func_77973_b() instanceof IDesignatorItem) {
            setTarget(player, held, (IDesignatorItem) held.func_77973_b());
            return true;
        }
        if (player.func_70093_af()) {
            toggleDeploy(player);
            return true;
        }
        if (isDeployed()) {
            ensureMobileTurret();
            if (mobileTurret == null) {
                tell(player, "Turret interface is unavailable.");
                return true;
            }
            FMLNetworkHandler.openGui(player, MainRegistry.instance, 0,
                    field_70170_p, func_145782_y(), 0, 0);
            return true;
        }
        if (field_70153_n == null || field_70153_n == player) {
            player.func_70078_a(this);
            return true;
        }
        tell(player, "The driver's seat is occupied.");
        return true;
    }

    private void linkArtilleryDesignator(EntityPlayer player, ItemStack stack) {
        if (!isDeployed()) {
            tell(player, "Deploy the artillery platform first.");
            return;
        }
        ensureMobileTurret();
        if (mobileTurret == null) {
            tell(player, "Turret interface is unavailable.");
            return;
        }
        if (!stack.func_77942_o()) {
            stack.field_77990_d = new NBTTagCompound();
        }
        stack.field_77990_d.func_74768_a("x", proxyX);
        stack.field_77990_d.func_74768_a("y", proxyY);
        stack.field_77990_d.func_74768_a("z", proxyZ);
        field_70170_p.func_72956_a(player, "hbm:item.techBleep", 1.0F, 1.0F);
        tell(player, "Artillery designator linked to mobile turret.");
    }

    private boolean installModule(EntityPlayer player, ItemStack stack) {
        if (getMount() != MOUNT_NONE) {
            return false;
        }
        int mount = MOUNT_NONE;
        if (stack.func_77973_b() == net.minecraft.item.Item.func_150898_a(ModBlocks.turret_arty)) {
            mount = MOUNT_GREG;
        } else if (stack.func_77973_b()
                == net.minecraft.item.Item.func_150898_a(ModBlocks.turret_himars)) {
            mount = MOUNT_HENRY;
        }
        if (mount == MOUNT_NONE) {
            return false;
        }
        setMount(mount);
        if (!player.field_71075_bZ.field_75098_d) {
            stack.field_77994_a--;
            if (stack.field_77994_a <= 0) {
                player.field_71071_by.func_70299_a(player.field_71071_by.field_70461_c, null);
            }
        }
        field_70170_p.func_72956_a(this, "random.anvil_use", 0.9F, 0.82F);
        tell(player, mount == MOUNT_GREG ? "Greg module installed." : "Henry module installed.");
        return true;
    }

    private void setTarget(EntityPlayer player, ItemStack stack, IDesignatorItem designator) {
        int x = floor(field_70165_t);
        int y = floor(field_70163_u);
        int z = floor(field_70161_v);
        if (!designator.isReady(field_70170_p, stack, x, y, z)) {
            tell(player, "Designator has no target coordinates.");
            return;
        }
        Vec3 target = designator.getCoords(field_70170_p, stack, x, y, z);
        if (target == null) {
            tell(player, "Designator target is unavailable.");
            return;
        }
        if (isDeployed()) {
            ensureMobileTurret();
            if (mobileTurret != null) {
                mobileTurret.enqueueTarget(target.field_72450_a,
                        target.field_72448_b, target.field_72449_c);
            }
        }
        field_70180_af.func_75692_b(DW_TARGET_X, Integer.valueOf(floor(target.field_72450_a)));
        field_70180_af.func_75692_b(DW_TARGET_Y, Integer.valueOf(floor(target.field_72448_b)));
        field_70180_af.func_75692_b(DW_TARGET_Z, Integer.valueOf(floor(target.field_72449_c)));
        field_70170_p.func_72956_a(this, "hbm:item.techBoop", 1.0F, 1.0F);
        tell(player, "Target accepted: " + floor(target.field_72450_a) + ", "
                + floor(target.field_72448_b) + ", " + floor(target.field_72449_c));
    }

    private boolean loadAmmo(EntityPlayer player, ItemStack stack) {
        int mount = getMount();
        boolean accepted = mount == MOUNT_GREG && stack.func_77973_b() == ModItems.ammo_arty;
        accepted |= mount == MOUNT_HENRY && stack.func_77973_b() == ModItems.ammo_himars;
        if (!accepted) {
            return false;
        }
        int type = stack.func_77960_j();
        int count = getAmmoCount();
        int capacity = mount == MOUNT_HENRY ? 6 : 4;
        if (count > 0 && getAmmoType() != type) {
            tell(player, "Unload the current ammunition type first.");
            return true;
        }
        if (count >= capacity) {
            tell(player, "Ammunition rack is full (" + capacity + ").");
            return true;
        }
        field_70180_af.func_75692_b(DW_AMMO_TYPE, Integer.valueOf(type));
        field_70180_af.func_75692_b(DW_AMMO_COUNT, Integer.valueOf(count + 1));
        ammoTag = stack.func_77942_o() ? (NBTTagCompound) stack.field_77990_d.func_74737_b() : null;
        if (!player.field_71075_bZ.field_75098_d) {
            stack.field_77994_a--;
            if (stack.field_77994_a <= 0) {
                player.field_71071_by.func_70299_a(player.field_71071_by.field_70461_c, null);
            }
        }
        field_70170_p.func_72956_a(this, "random.click", 0.8F, 0.85F);
        tell(player, "Ammunition loaded: " + (count + 1) + "/" + capacity);
        return true;
    }

    private void toggleDeploy(EntityPlayer player) {
        if (getMount() == MOUNT_NONE) {
            tell(player, "This chassis has no artillery module.");
            return;
        }
        boolean deployed = !isDeployed();
        field_70180_af.func_75692_b(DW_DEPLOYED, Byte.valueOf((byte) (deployed ? 1 : 0)));
        driveSpeed = 0.0D;
        if (deployed) {
            ensureMobileTurret();
        } else {
            detachMobileTurret(true);
            setProxyWatchers(0, -4096, 0);
        }
        field_70170_p.func_72956_a(this, "random.anvil_use", 0.7F, deployed ? 0.72F : 1.15F);
        tell(player, deployed ? "Artillery platform deployed." : "Artillery platform retracted.");
    }

    private void ensureMobileTurret() {
        if (!isDeployed() || getMount() == MOUNT_NONE) {
            return;
        }
        if (field_70170_p.field_72995_K) {
            proxyX = field_70180_af.func_75679_c(DW_PROXY_X);
            proxyY = field_70180_af.func_75679_c(DW_PROXY_Y);
            proxyZ = field_70180_af.func_75679_c(DW_PROXY_Z);
            if (proxyY <= -2048) {
                return;
            }
        } else {
            int[] position = calculateProxyPosition();
            proxyX = position[0];
            proxyY = position[1];
            proxyZ = position[2];
            setProxyWatchers(proxyX, proxyY, proxyZ);
            if (!ensureProxyBlock()) {
                setProxyWatchers(0, -4096, 0);
                return;
            }
        }
        if (mobileTurret != null) {
            if (mobileTurret.field_145851_c == proxyX
                    && mobileTurret.field_145848_d == proxyY
                    && mobileTurret.field_145849_e == proxyZ) {
                if (field_70170_p.func_147438_o(proxyX, proxyY, proxyZ) != mobileTurret) {
                    mobileTurret.func_145834_a(field_70170_p);
                    field_70170_p.func_147455_a(proxyX, proxyY, proxyZ, mobileTurret);
                    mobileTurret.func_145829_t();
                }
                return;
            }
            detachMobileTurret(true);
        }
        TileEntity existing = field_70170_p.func_147438_o(proxyX, proxyY, proxyZ);
        if ((getMount() == MOUNT_GREG && existing instanceof MobileTileArty)
                || (getMount() == MOUNT_HENRY && existing instanceof MobileTileHimars)) {
            mobileTurret = (TileEntityTurretBaseArtillery) existing;
            return;
        }
        mobileTurret = getMount() == MOUNT_GREG
                ? new MobileTileArty() : new MobileTileHimars();
        mobileTurret.func_145834_a(field_70170_p);
        if (mobileTurretData != null) {
            mobileTurret.func_145839_a(mobileTurretData);
        } else {
            mobileTurret.rotationYaw = Math.toRadians(field_70177_z);
            mobileTurret.lastRotationYaw = mobileTurret.rotationYaw;
        }
        mobileTurret.func_145834_a(field_70170_p);
        mobileTurret.field_145851_c = proxyX;
        mobileTurret.field_145848_d = proxyY;
        mobileTurret.field_145849_e = proxyZ;
        field_70170_p.func_147455_a(proxyX, proxyY, proxyZ, mobileTurret);
        mobileTurret.func_145829_t();
    }

    private void setProxyWatchers(int x, int y, int z) {
        field_70180_af.func_75692_b(DW_PROXY_X, Integer.valueOf(x));
        field_70180_af.func_75692_b(DW_PROXY_Y, Integer.valueOf(y));
        field_70180_af.func_75692_b(DW_PROXY_Z, Integer.valueOf(z));
    }

    private void syncMobileTurret() {
        if (mobileTurret == null) {
            return;
        }
        field_70180_af.func_75692_b(DW_TURRET_YAW,
                Float.valueOf((float) Math.toDegrees(mobileTurret.rotationYaw)));
        field_70180_af.func_75692_b(DW_TURRET_PITCH,
                Float.valueOf((float) Math.toDegrees(mobileTurret.rotationPitch)));
        if (mobileTurret instanceof TileEntityTurretHIMARS) {
            TileEntityTurretHIMARS henry = (TileEntityTurretHIMARS) mobileTurret;
            field_70180_af.func_75692_b(DW_AMMO_TYPE, Integer.valueOf(henry.typeLoaded));
            field_70180_af.func_75692_b(DW_AMMO_COUNT, Integer.valueOf(henry.ammo));
        } else if (mobileTurret instanceof TileEntityTurretArty) {
            ItemStack loaded = ((TileEntityTurretArty) mobileTurret).getShellLoaded();
            int count = 0;
            int type = loaded == null ? -1 : loaded.func_77960_j();
            for (int i = 0; i < mobileTurret.func_70302_i_(); i++) {
                ItemStack stack = mobileTurret.func_70301_a(i);
                if (stack != null && stack.func_77973_b() == ModItems.ammo_arty) {
                    count += stack.field_77994_a;
                }
            }
            field_70180_af.func_75692_b(DW_AMMO_TYPE, Integer.valueOf(type));
            field_70180_af.func_75692_b(DW_AMMO_COUNT, Integer.valueOf(count));
        }
        int mode = mobileTurret instanceof TileEntityTurretArty
                ? ((TileEntityTurretArty) mobileTurret).mode
                : ((TileEntityTurretHIMARS) mobileTurret).mode;
        int flags = (mobileTurret.isOn ? 1 : 0)
                | (mobileTurret.targetPlayers ? 2 : 0)
                | (mobileTurret.targetAnimals ? 4 : 0)
                | (mobileTurret.targetMobs ? 8 : 0)
                | (mobileTurret.targetMachines ? 16 : 0)
                | ((mode & 255) << 8);
        long power = mobileTurret.getPower();
        field_70180_af.func_75692_b(DW_CONTROL_FLAGS, Integer.valueOf(flags));
        field_70180_af.func_75692_b(DW_POWER, Integer.valueOf(
                (int) Math.max(0L, Math.min(Integer.MAX_VALUE, power))));
    }

    private void applyMobileTurretState() {
        if (mobileTurret == null) {
            return;
        }
        int flags = field_70180_af.func_75679_c(DW_CONTROL_FLAGS);
        mobileTurret.isOn = (flags & 1) != 0;
        mobileTurret.targetPlayers = (flags & 2) != 0;
        mobileTurret.targetAnimals = (flags & 4) != 0;
        mobileTurret.targetMobs = (flags & 8) != 0;
        mobileTurret.targetMachines = (flags & 16) != 0;
        short mode = (short) ((flags >>> 8) & 255);
        if (mobileTurret instanceof TileEntityTurretArty) {
            ((TileEntityTurretArty) mobileTurret).mode = mode;
        } else if (mobileTurret instanceof TileEntityTurretHIMARS) {
            ((TileEntityTurretHIMARS) mobileTurret).mode = mode;
        }
        mobileTurret.setPower(field_70180_af.func_75679_c(DW_POWER));
    }

    private void detachMobileTurret(boolean save) {
        if (mobileTurret == null) {
            return;
        }
        if (save && !field_70170_p.field_72995_K) {
            mobileTurretData = new NBTTagCompound();
            mobileTurret.func_145841_b(mobileTurretData);
        }
        int x = mobileTurret.field_145851_c;
        int y = mobileTurret.field_145848_d;
        int z = mobileTurret.field_145849_e;
        if (field_70170_p.func_147438_o(x, y, z) == mobileTurret) {
            field_70170_p.func_147475_p(x, y, z);
        }
        if (!field_70170_p.field_72995_K
                && field_70170_p.func_147439_a(x, y, z)
                        == MobileArtilleryContent.mobileTurretProxy) {
            field_70170_p.func_147468_f(x, y, z);
        }
        mobileTurret.func_145843_s();
        mobileTurret = null;
    }

    private int[] calculateProxyPosition() {
        double yaw = Math.toRadians(field_70177_z);
        int rearX = floor(field_70165_t + Math.sin(yaw) * 1.35D);
        int rearZ = floor(field_70161_v - Math.cos(yaw) * 1.35D);
        int centerX = floor(field_70165_t);
        int centerZ = floor(field_70161_v);
        int baseY = floor(field_70163_u) + 1;
        int[][] candidates = new int[][] {
                { rearX, baseY, rearZ },
                { centerX, baseY, centerZ },
                { rearX, baseY + 1, rearZ },
                { centerX, baseY + 1, centerZ }
        };
        for (int i = 0; i < candidates.length; i++) {
            int[] candidate = candidates[i];
            if (field_70170_p.func_147439_a(candidate[0], candidate[1], candidate[2])
                    == MobileArtilleryContent.mobileTurretProxy) {
                return candidate;
            }
        }
        for (int i = 0; i < candidates.length; i++) {
            int[] candidate = candidates[i];
            if (field_70170_p.func_147437_c(candidate[0], candidate[1], candidate[2])) {
                return candidate;
            }
        }
        return new int[] { centerX, -4096, centerZ };
    }

    private boolean ensureProxyBlock() {
        if (proxyY <= -2048) {
            return false;
        }
        if (isDeployed() && proxyY > -2048
                && field_70170_p.func_147439_a(proxyX, proxyY, proxyZ)
                == MobileArtilleryContent.mobileTurretProxy) {
            return true;
        }
        if (!field_70170_p.func_147437_c(proxyX, proxyY, proxyZ)) {
            return false;
        }
        return field_70170_p.func_147465_d(proxyX, proxyY, proxyZ,
                MobileArtilleryContent.mobileTurretProxy, 0, 2);
    }

    @Override
    public Container provideContainer(int id, EntityPlayer player, World world,
            int x, int y, int z) {
        ensureMobileTurret();
        return mobileTurret == null ? null
                : new ContainerMobileArtillery(player.field_71071_by, mobileTurret, this);
    }

    @Override
    public Object provideGUI(int id, EntityPlayer player, World world,
            int x, int y, int z) {
        ensureMobileTurret();
        return mobileTurret == null ? null
                : mobileTurret.provideGUI(id, player, world, proxyX, proxyY, proxyZ);
    }

    private void fire(EntityPlayer player) {
        if (fireCooldown > 0) {
            tell(player, "Weapon is cycling.");
            return;
        }
        if (!hasTarget()) {
            tell(player, "No target. Use an HBM designator on the vehicle.");
            return;
        }
        if (getAmmoCount() <= 0 || getAmmoType() < 0) {
            tell(player, "No compatible ammunition loaded.");
            return;
        }
        if (getMount() == MOUNT_GREG) {
            fireGreg();
            fireCooldown = 70;
        } else if (getMount() == MOUNT_HENRY) {
            fireHenry();
            fireCooldown = 28;
        } else {
            return;
        }
        field_70180_af.func_75692_b(DW_AMMO_COUNT, Integer.valueOf(getAmmoCount() - 1));
        if (getAmmoCount() <= 0) {
            field_70180_af.func_75692_b(DW_AMMO_TYPE, Integer.valueOf(-1));
            ammoTag = null;
        }
        spawnMuzzleSmoke();
    }

    private void fireGreg() {
        double[] muzzle = muzzlePosition(2.15D);
        double tx = field_70180_af.func_75679_c(DW_TARGET_X) + 0.5D;
        double ty = field_70180_af.func_75679_c(DW_TARGET_Y) + 0.5D;
        double tz = field_70180_af.func_75679_c(DW_TARGET_Z) + 0.5D;
        double dx = tx - muzzle[0];
        double dz = tz - muzzle[2];
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double angle = ballisticAngle(horizontal, ty - muzzle[1], 50.0D, 0.4905D);
        double cos = Math.cos(angle);
        EntityArtilleryShell shell = new EntityArtilleryShell(field_70170_p);
        shell.func_70080_a(muzzle[0], muzzle[1], muzzle[2], 0.0F, 0.0F);
        shell.func_70186_c(dx / horizontal * cos, Math.sin(angle), dz / horizontal * cos, 50.0F, 0.0F);
        shell.setTarget(tx, ty, tz);
        ItemStack ammo = new ItemStack(ModItems.ammo_arty, 1, getAmmoType());
        if (ammoTag != null) {
            ammo.field_77990_d = (NBTTagCompound) ammoTag.func_74737_b();
        }
        shell.setType(getAmmoType());
        if (getAmmoType() == 8 && ammo.func_77942_o()) {
            NBTTagCompound cargo = ammo.field_77990_d.func_74775_l("cargo");
            ItemStack cargoStack = ItemStack.func_77949_a(cargo);
            if (cargoStack != null) {
                shell.setCargo(cargoStack);
            }
        }
        shell.setWhistle(true);
        field_70170_p.func_72838_d(shell);
        field_70170_p.func_72908_a(muzzle[0], muzzle[1], muzzle[2],
                "hbm:turret.jeremy_fire", 16.0F, 0.9F);
    }

    private void fireHenry() {
        double[] muzzle = muzzlePosition(1.45D);
        double tx = field_70180_af.func_75679_c(DW_TARGET_X) + 0.5D;
        double ty = field_70180_af.func_75679_c(DW_TARGET_Y) + 0.5D;
        double tz = field_70180_af.func_75679_c(DW_TARGET_Z) + 0.5D;
        double dx = tx - muzzle[0];
        double dz = tz - muzzle[2];
        double horizontal = Math.max(0.001D, Math.sqrt(dx * dx + dz * dz));
        double elevation = Math.toRadians(18.0D);
        EntityArtilleryRocket rocket = new EntityArtilleryRocket(field_70170_p);
        rocket.func_70080_a(muzzle[0], muzzle[1], muzzle[2], 0.0F, 0.0F);
        rocket.func_70186_c(dx / horizontal * Math.cos(elevation), Math.sin(elevation),
                dz / horizontal * Math.cos(elevation), 25.0F, 0.0F);
        rocket.setTarget(tx, ty, tz).setType(getAmmoType());
        field_70170_p.func_72838_d(rocket);
        field_70170_p.func_72908_a(muzzle[0], muzzle[1], muzzle[2],
                "hbm:weapon.rocketFlame", 12.0F, 1.0F);
    }

    private double[] muzzlePosition(double forward) {
        double yaw = Math.toRadians(getTurretYaw());
        return new double[] {
                field_70165_t - Math.sin(yaw) * forward,
                field_70163_u + (getMount() == MOUNT_GREG ? 2.25D : 2.2D),
                field_70161_v + Math.cos(yaw) * forward
        };
    }

    private void spawnMuzzleSmoke() {
        if (!(field_70170_p instanceof WorldServer)) {
            return;
        }
        double[] muzzle = muzzlePosition(getMount() == MOUNT_GREG ? 2.15D : 1.45D);
        WorldServer server = (WorldServer) field_70170_p;
        server.func_147487_a("largesmoke", muzzle[0], muzzle[1], muzzle[2],
                16, 0.35D, 0.25D, 0.35D, 0.04D);
        server.func_147487_a("smoke", muzzle[0], muzzle[1], muzzle[2],
                24, 0.55D, 0.3D, 0.55D, 0.06D);
    }

    private static double ballisticAngle(double horizontal, double height,
            double velocity, double gravity) {
        if (horizontal < 0.001D) {
            return Math.PI * 0.45D;
        }
        double velocitySq = velocity * velocity;
        double root = velocitySq * velocitySq
                - gravity * (gravity * horizontal * horizontal + 2.0D * height * velocitySq);
        if (root < 0.0D) {
            return Math.toRadians(45.0D);
        }
        return Math.atan((velocitySq + Math.sqrt(root)) / (gravity * horizontal));
    }

    @Override
    public void func_70043_V() {
        if (field_70153_n != null) {
            double yaw = Math.toRadians(field_70177_z);
            double forwardX = -Math.sin(yaw);
            double forwardZ = Math.cos(yaw);
            double rightX = Math.cos(yaw);
            double rightZ = Math.sin(yaw);
            field_70153_n.func_70107_b(
                    field_70165_t + forwardX * 3.35D - rightX * 0.56D,
                    field_70163_u + 1.08D + field_70153_n.func_70033_W(),
                    field_70161_v + forwardZ * 3.35D - rightZ * 0.56D);
        }
    }

    @Override
    public double func_70042_X() {
        return 1.45D;
    }

    @Override
    public boolean func_70067_L() {
        return !field_70128_L;
    }

    @Override
    public boolean func_70097_a(DamageSource source, float amount) {
        if (field_70170_p.field_72995_K || field_70128_L) {
            return true;
        }
        if (source.func_76346_g() instanceof EntityPlayer
                && ((EntityPlayer) source.func_76346_g()).field_71075_bZ.field_75098_d) {
            destroyVehicle();
            return true;
        }
        if (field_70173_aa <= 40 || amount <= 0.0F) {
            return true;
        }
        vehicleHealth -= Math.min(MAX_DAMAGE_PER_HIT, amount);
        if (vehicleHealth <= 0.0D) {
            destroyVehicle();
        } else {
            field_70170_p.func_72956_a(this, "random.anvil_land", 0.35F, 1.45F);
        }
        return true;
    }

    private void destroyVehicle() {
        if (field_70128_L) {
            return;
        }
        if (mobileTurret != null) {
            for (int i = 0; i < mobileTurret.func_70302_i_(); i++) {
                mobileTurret.func_70299_a(i, null);
            }
        }
        detachMobileTurret(false);
        if (isDeployed() && proxyY > -2048
                && field_70170_p.func_147439_a(proxyX, proxyY, proxyZ)
                == MobileArtilleryContent.mobileTurretProxy) {
            field_70170_p.func_147468_f(proxyX, proxyY, proxyZ);
        }
        mobileTurretData = null;
        func_70106_y();
        field_70170_p.func_72885_a(this, field_70165_t, field_70163_u + 1.0D,
                field_70161_v, 5.0F, true, true);
    }

    public void dismantleForRecovery(boolean dropItems) {
        if (field_70128_L) return;
        if (mobileTurret != null && dropItems) {
            for (int i = 0; i < mobileTurret.func_70302_i_(); ++i) {
                ItemStack stack = mobileTurret.func_70304_b(i);
                if (stack != null) func_70099_a(stack, 0.6F);
            }
        }
        detachMobileTurret(false);
        if (isDeployed() && proxyY > -2048
                && field_70170_p.func_147439_a(proxyX, proxyY, proxyZ)
                == MobileArtilleryContent.mobileTurretProxy) {
            field_70170_p.func_147468_f(proxyX, proxyY, proxyZ);
        }
        mobileTurretData = null;
        if (dropItems) {
            func_70099_a(new ItemStack(MobileArtilleryContent.mobileArtillery,
                    1, getMount()), 0.6F);
        }
        func_70106_y();
    }

    @Override
    protected void func_70014_b(NBTTagCompound tag) {
        tag.func_74768_a("Mount", getMount());
        tag.func_74757_a("Deployed", isDeployed());
        tag.func_74768_a("AmmoType", getAmmoType());
        tag.func_74768_a("AmmoCount", getAmmoCount());
        tag.func_74768_a("TargetX", field_70180_af.func_75679_c(DW_TARGET_X));
        tag.func_74768_a("TargetY", field_70180_af.func_75679_c(DW_TARGET_Y));
        tag.func_74768_a("TargetZ", field_70180_af.func_75679_c(DW_TARGET_Z));
        tag.func_74780_a("DriveSpeed", driveSpeed);
        tag.func_74780_a("VehicleHealth", vehicleHealth);
        if (ammoTag != null) {
            tag.func_74782_a("AmmoTag", ammoTag);
        }
        if (mobileTurret != null) {
            mobileTurretData = new NBTTagCompound();
            mobileTurret.func_145841_b(mobileTurretData);
        }
        if (mobileTurretData != null) {
            tag.func_74782_a("MobileTurret", mobileTurretData);
        }
    }

    @Override
    protected void func_70037_a(NBTTagCompound tag) {
        setMount(tag.func_74762_e("Mount"));
        field_70180_af.func_75692_b(DW_DEPLOYED,
                Byte.valueOf((byte) (tag.func_74767_n("Deployed") ? 1 : 0)));
        field_70180_af.func_75692_b(DW_AMMO_TYPE, Integer.valueOf(tag.func_74762_e("AmmoType")));
        field_70180_af.func_75692_b(DW_AMMO_COUNT, Integer.valueOf(tag.func_74762_e("AmmoCount")));
        field_70180_af.func_75692_b(DW_TARGET_X, Integer.valueOf(tag.func_74762_e("TargetX")));
        field_70180_af.func_75692_b(DW_TARGET_Y, Integer.valueOf(tag.func_74762_e("TargetY")));
        field_70180_af.func_75692_b(DW_TARGET_Z, Integer.valueOf(tag.func_74762_e("TargetZ")));
        driveSpeed = tag.func_74769_h("DriveSpeed");
        if (tag.func_74764_b("VehicleHealth")) {
            vehicleHealth = Math.max(1.0D,
                    Math.min(MAX_HEALTH, tag.func_74769_h("VehicleHealth")));
        }
        ammoTag = tag.func_74764_b("AmmoTag") ? tag.func_74775_l("AmmoTag") : null;
        mobileTurretData = tag.func_74764_b("MobileTurret")
                ? tag.func_74775_l("MobileTurret") : null;
    }

    private static int floor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }

    private static void tell(EntityPlayer player, String text) {
        player.func_145747_a(new ChatComponentText(text));
    }
}
