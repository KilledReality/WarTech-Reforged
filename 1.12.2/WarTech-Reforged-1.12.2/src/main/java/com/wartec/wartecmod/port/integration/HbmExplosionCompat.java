package com.wartec.wartecmod.port.integration;

import com.hbm.blocks.ModBlocks;
import com.hbm.entity.effect.EntityCloudTom;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.ExplosionNT;
import com.hbm.packet.AuxParticlePacketNT;
import com.hbm.packet.PacketDispatcher;
import com.hbm.util.ContaminationUtil;
import com.wartec.wartecmod.port.network.LegacyMushroomEffectMessage;
import com.wartec.wartecmod.port.network.WarTechNetwork;
import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.NetworkRegistry;

/**
 * NTM Extended 3.0.3 bridge for the exact dev66 warhead effects.
 */
public final class HbmExplosionCompat {
    private HbmExplosionCompat() {
    }

    public static void advancedExplosion(World world, double x, double y, double z,
            float size, float rangeModifier, boolean breaksBlocks) {
        if (world == null || world.isRemote) {
            return;
        }
        net.minecraft.util.SoundEvent detonation =
                net.minecraft.util.SoundEvent.REGISTRY.getObject(
                        new net.minecraft.util.ResourceLocation(
                                "wartecmod", "entity.bombdet3"));
        if (detonation == null) {
            detonation = net.minecraft.init.SoundEvents.ENTITY_GENERIC_EXPLODE;
        }
        world.playSound(null, x, y, z, detonation,
                net.minecraft.util.SoundCategory.PLAYERS, 50.0F,
                0.9F + world.rand.nextFloat() * 0.2F);
        ExplosionLarge.spawnParticles(world, x, y, z, cloudFunction((int) size));
        ExplosionLarge.spawnRubble(world, x, y, z, cloudFunction((int) size));
        ExplosionLarge.spawnShrapnels(world, x, y, z, cloudFunction((int) size));
        LegacyVntExplosion.explode(
                world, x, y, z, size, rangeModifier, breaksBlocks, true);
    }

    public static void thermobaricExplosion(World world, double x, double y, double z,
            float size, float rangeModifier, boolean breaksBlocks) {
        if (world == null || world.isRemote) {
            return;
        }
        net.minecraft.util.SoundEvent detonation =
                net.minecraft.util.SoundEvent.REGISTRY.getObject(
                        new net.minecraft.util.ResourceLocation(
                                "wartecmod", "weapon.explosion_medium"));
        if (detonation == null) {
            detonation = net.minecraft.init.SoundEvents.ENTITY_GENERIC_EXPLODE;
        }
        world.playSound(null, x, y, z, detonation,
                net.minecraft.util.SoundCategory.PLAYERS, 50.0F,
                0.9F + world.rand.nextFloat() * 0.2F);
        LegacyVntExplosion.explode(
                world, x, y, z, size, rangeModifier, breaksBlocks, false);
    }

    public static void artilleryExplosion(World world,
            double x, double y, double z, float size,
            float rangeModifier, boolean breaksBlocks, int slagMetadata) {
        if (world == null || world.isRemote) {
            return;
        }
        IBlockState debris = breaksBlocks && ModBlocks.block_slag != null
                ? ModBlocks.block_slag.getStateFromMeta(slagMetadata) : null;
        LegacyVntExplosion.explode(world, x, y, z, size,
                rangeModifier, breaksBlocks, false, debris);
    }

    public static void artilleryCompositeEffect(World world,
            double x, double y, double z, int cloudCount,
            float cloudScale, int debrisCount, float soundRange) {
        if (world == null || world.isRemote) {
            return;
        }
        ExplosionLarge.spawnParticles(world, x, y, z, cloudCount);
        ExplosionLarge.spawnRubble(world, x, y, z, debrisCount);
        spawnLegacyLargeExplosion(world, x, y, z,
                Math.max(1, cloudCount / 3));
        standardMush(world, x, y, z, cloudScale);
        net.minecraft.util.SoundEvent sound =
                net.minecraft.util.SoundEvent.REGISTRY.getObject(
                        new net.minecraft.util.ResourceLocation(
                                "hbm", "entity.oldexplosion"));
        if (sound != null) {
            world.playSound(null, x, y, z, sound,
                    net.minecraft.util.SoundCategory.PLAYERS,
                    Math.min(1000.0F, Math.max(4.0F, soundRange)), 1.0F);
        }
    }

    public static void artilleryClusterSplit(World world,
            double x, double y, double z) {
        if (world == null || world.isRemote) {
            return;
        }
        NBTTagCompound data = new NBTTagCompound();
        data.setString("type", "plasmablast");
        data.setFloat("r", 1.0F);
        data.setFloat("g", 1.0F);
        data.setFloat("b", 1.0F);
        data.setFloat("scale", 50.0F);
        PacketDispatcher.wrapper.sendToAllAround(
                new AuxParticlePacketNT(data, x, y, z),
                new NetworkRegistry.TargetPoint(
                        world.provider.getDimension(), x, y, z, 500.0D));
    }

    public static void artilleryMiniNuke(World world,
            double x, double y, double z) {
        if (world == null || world.isRemote) {
            return;
        }
        sendAuxEffect(world, x, y + 0.5D, z, "muke", 250.0D);
        net.minecraft.util.SoundEvent sound =
                net.minecraft.util.SoundEvent.REGISTRY.getObject(
                        new net.minecraft.util.ResourceLocation(
                                "hbm", "weapon.mukeexplosion"));
        if (sound != null) {
            world.playSound(null, x, y, z, sound,
                    net.minecraft.util.SoundCategory.PLAYERS,
                    15.0F, 1.0F);
        }
        ExplosionLarge.spawnShrapnels(world, x, y, z, 25);
        new ExplosionNT(world, null, x, y, z, 20.0F)
                .addAttrib(ExplosionNT.ExAttrib.FIRE)
                .addAttrib(ExplosionNT.ExAttrib.NOPARTICLE)
                .addAttrib(ExplosionNT.ExAttrib.NOSOUND)
                .addAttrib(ExplosionNT.ExAttrib.NODROP)
                .addAttrib(ExplosionNT.ExAttrib.NOHURT)
                .overrideResolution(64)
                .explode();
        nuclearAreaDamage(world, x, y, z, 55.0D, 250.0F);
    }

    public static void artilleryFullNuke(World world,
            double x, double y, double z) {
        if (world == null || world.isRemote) {
            return;
        }
        int radius = com.hbm.config.BombConfig.missileRadius;
        world.spawnEntity(EntityNukeExplosionMK5.statFac(
                world, radius, x, y, z));
        EntityNukeTorex.statFac(world, x, y, z, radius);
    }

    private static void sendAuxEffect(World world,
            double x, double y, double z, String type, double range) {
        NBTTagCompound data = new NBTTagCompound();
        data.setString("type", type);
        PacketDispatcher.wrapper.sendToAllAround(
                new AuxParticlePacketNT(data, x, y, z),
                new NetworkRegistry.TargetPoint(
                        world.provider.getDimension(), x, y, z, range));
    }

    private static void nuclearAreaDamage(World world,
            double x, double y, double z, double radius, float maximumDamage) {
        AxisAlignedBB area = new AxisAlignedBB(
                x - radius, y - radius, z - radius,
                x + radius, y + radius, z + radius);
        for (Entity entity
                : world.getEntitiesWithinAABBExcludingEntity(null, area)) {
            double distance = entity.getDistance(x, y, z);
            if (distance > radius) {
                continue;
            }
            Vec3d eye = new Vec3d(entity.posX,
                    entity.posY + entity.getEyeHeight(), entity.posZ);
            if (world.rayTraceBlocks(new Vec3d(x, y, z), eye,
                    false, true, false) != null) {
                continue;
            }
            float damage = (float) (maximumDamage
                    * (radius - distance) / radius);
            entity.attackEntityFrom(
                    new DamageSource("nuclearBlast").setExplosion(), damage);
            entity.setFire(5);
            Vec3d knockback = eye.subtract(x, y, z).normalize().scale(0.2D);
            entity.motionX += knockback.x;
            entity.motionY += knockback.y;
            entity.motionZ += knockback.z;
            entity.velocityChanged = true;
        }
    }

    public static void standardMush(World world, double x, double y, double z, float scale) {
        if (world == null || world.isRemote) {
            return;
        }
        WarTechNetwork.CHANNEL.sendToAllAround(
                new LegacyMushroomEffectMessage(x, y, z, scale),
                new NetworkRegistry.TargetPoint(
                        world.provider.getDimension(), x, y, z, 250.0D));
    }

    public static void spawnLegacyMissileExhaust(World world,
            double x, double y, double z, int count, double width) {
        if (world == null || world.isRemote) {
            return;
        }
        NBTTagCompound data = new NBTTagCompound();
        data.setString("type", "exhaust");
        data.setString("mode", "soyuz");
        data.setInteger("count", count);
        data.setDouble("width", width);
        data.setDouble("posX", x);
        data.setDouble("posY", y);
        data.setDouble("posZ", z);
        PacketDispatcher.wrapper.sendToAllAround(
                new AuxParticlePacketNT(data, x, y, z),
                new NetworkRegistry.TargetPoint(
                        world.provider.getDimension(), x, y, z, 300.0D));
    }

    public static void spawnLegacyLargeExplosion(World world,
            double x, double y, double z, int count) {
        if (world == null || world.isRemote) {
            return;
        }
        NBTTagCompound data = new NBTTagCompound();
        data.setString("type", "vanillaExt");
        data.setString("mode", "largeexplode");
        data.setFloat("size", 0.0F);
        data.setByte("count", (byte) count);
        PacketDispatcher.wrapper.sendToAllAround(
                new AuxParticlePacketNT(data, x, y, z),
                new NetworkRegistry.TargetPoint(
                        world.provider.getDimension(), x, y, z, 150.0D));
    }

    public static void spawnLegacyHaze(World world,
            double x, double y, double z, int count, double spread) {
        if (world == null || world.isRemote) {
            return;
        }
        for (int index = 0; index < count; ++index) {
            double effectX = x + world.rand.nextGaussian() * spread;
            double effectZ = z + world.rand.nextGaussian() * spread;
            NBTTagCompound data = new NBTTagCompound();
            data.setString("type", "haze");
            PacketDispatcher.wrapper.sendToAllAround(
                    new AuxParticlePacketNT(data, effectX, y, effectZ),
                    new NetworkRegistry.TargetPoint(
                            world.provider.getDimension(),
                            effectX, y, effectZ, 150.0D));
        }
    }

    public static int cloudFunction(int size) {
        return (int) (850.0D * (1.0D - Math.pow(Math.E, -size / 15.0D)) + 15.0D);
    }

    public static void spawnChlorine(World world, double x, double y, double z,
            int amount, double spread, int metadata) {
        if (world != null && !world.isRemote) {
            ExplosionChaos.spawnChlorine(world, x, y, z, amount, spread, metadata);
        }
    }

    public static void neutronMicroImpact(World world, double x, double y, double z) {
        if (world == null || world.isRemote) {
            return;
        }
        world.createExplosion(null, x, y, z, 8.5F, true);
        if (world instanceof WorldServer) {
            WorldServer server = (WorldServer) world;
            server.spawnParticle(net.minecraft.util.EnumParticleTypes.EXPLOSION_HUGE,
                    x, y, z, 3, 1.2D, 1.2D, 1.2D, 0.0D);
            server.spawnParticle(net.minecraft.util.EnumParticleTypes.SMOKE_LARGE,
                    x, y, z, 56, 5.0D, 2.5D, 5.0D, 0.045D);
        }
        double radius = 34.0D;
        AxisAlignedBB box = new AxisAlignedBB(
                x - radius, y - radius, z - radius,
                x + radius, y + radius, z + radius);
        List<Entity> entities = world.getEntitiesWithinAABBExcludingEntity(null, box);
        DamageSource source = new DamageSource("wartec.neutronMicro");
        for (Entity entity : entities) {
            double distance = entity.getDistance(x, y, z);
            if (distance > radius) {
                continue;
            }
            float strength = (float) Math.max(0.0D, 1.0D - distance / radius);
            entity.attackEntityFrom(source, 5.0F + strength * 23.0F);
            contaminateNeutron(entity, 18.0F + strength * 82.0F);
        }
    }

    public static void flameDeath(World world, int x, int y, int z, int radius) {
        if (world != null && !world.isRemote) {
            ExplosionChaos.flameDeath(world, new BlockPos(x, y, z), radius);
        }
    }

    public static void burn(World world, int x, int y, int z, int radius) {
        if (world != null && !world.isRemote) {
            ExplosionChaos.burn(world, new BlockPos(x, y, z), radius);
        }
    }

    public static void cluster(World world, int x, int y, int z, int amount, int strength) {
        if (world != null && !world.isRemote) {
            ExplosionChaos.cluster(world, x, y, z, amount, (double) strength);
        }
    }

    public static void nuclear(World world, int strength, double x, double y, double z,
            float cloudScale) {
        if (world == null || world.isRemote) {
            return;
        }
        world.spawnEntity(EntityNukeExplosionMK5.statFac(world, strength, x, y, z));
        EntityCloudTom cloud = new EntityCloudTom(world, 1000);
        cloud.setPosition(x, y, z);
        world.spawnEntity(cloud);
    }

    private static void contaminateNeutron(Entity entity, float amount) {
        if (!(entity instanceof EntityLivingBase)) {
            return;
        }
        ContaminationUtil.contaminate((EntityLivingBase) entity,
                ContaminationUtil.HazardType.NEUTRON,
                ContaminationUtil.ContaminationType.HAZMAT2, amount);
    }
}
