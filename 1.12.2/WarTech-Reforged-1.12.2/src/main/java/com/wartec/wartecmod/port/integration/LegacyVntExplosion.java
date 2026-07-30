package com.wartec.wartecmod.port.integration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentProtection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketExplosion;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;

/**
 * Mapping-stable 1.12 port of the dev66 Vanillant explosion configuration:
 * BlockAllocatorStandard(48), no-drop BlockProcessorStandard,
 * EntityProcessorCross(7.5), PlayerProcessorStandard and optional standard SFX.
 */
final class LegacyVntExplosion {
    private static final int BLOCK_RESOLUTION = 48;
    private static final float STEP_SIZE = 0.3F;
    private static final double NODE_DISTANCE = 7.5D;

    private LegacyVntExplosion() {
    }

    static void explode(World world, double x, double y, double z,
            float size, float rangeModifier, boolean breaksBlocks,
            boolean standardEffects) {
        explode(world, x, y, z, size, rangeModifier, breaksBlocks,
                standardEffects, null);
    }

    static void explode(World world, double x, double y, double z,
            float size, float rangeModifier, boolean breaksBlocks,
            boolean standardEffects, IBlockState debris) {
        Explosion compatibility =
                new Explosion(world, null, x, y, z, size, false, breaksBlocks);
        Set<BlockPos> affected = breaksBlocks
                ? allocateBlocks(world, compatibility, x, y, z, size)
                : new HashSet<BlockPos>();
        compatibility.getAffectedBlockPositions().addAll(affected);

        Map<EntityPlayer, Vec3d> playerKnockback =
                processEntities(world, compatibility, x, y, z,
                        size, rangeModifier);
        processBlocks(world, compatibility, affected, debris);
        sendEffects(world, x, y, z, size, affected,
                playerKnockback, standardEffects);
    }

    private static Set<BlockPos> allocateBlocks(World world,
            Explosion explosion, double x, double y, double z, float size) {
        Set<BlockPos> affected = new HashSet<BlockPos>();
        for (int ix = 0; ix < BLOCK_RESOLUTION; ++ix) {
            for (int iy = 0; iy < BLOCK_RESOLUTION; ++iy) {
                for (int iz = 0; iz < BLOCK_RESOLUTION; ++iz) {
                    if (ix != 0 && ix != BLOCK_RESOLUTION - 1
                            && iy != 0 && iy != BLOCK_RESOLUTION - 1
                            && iz != 0 && iz != BLOCK_RESOLUTION - 1) {
                        continue;
                    }
                    double dirX = (double) ((float) ix
                            / ((float) BLOCK_RESOLUTION - 1.0F)
                            * 2.0F - 1.0F);
                    double dirY = (double) ((float) iy
                            / ((float) BLOCK_RESOLUTION - 1.0F)
                            * 2.0F - 1.0F);
                    double dirZ = (double) ((float) iz
                            / ((float) BLOCK_RESOLUTION - 1.0F)
                            * 2.0F - 1.0F);
                    double length = Math.sqrt(
                            dirX * dirX + dirY * dirY + dirZ * dirZ);
                    dirX /= length;
                    dirY /= length;
                    dirZ /= length;

                    float remaining =
                            size * (0.7F + world.rand.nextFloat() * 0.6F);
                    double rayX = x;
                    double rayY = y;
                    double rayZ = z;
                    while (remaining > 0.0F) {
                        BlockPos pos = new BlockPos(rayX, rayY, rayZ);
                        IBlockState state = world.getBlockState(pos);
                        if (state.getMaterial() != Material.AIR) {
                            float resistance = state.getBlock()
                                    .getExplosionResistance(
                                            world, pos, null, explosion);
                            remaining -=
                                    (resistance + 0.3F) * STEP_SIZE;
                        }
                        if (remaining > 0.0F) {
                            affected.add(pos);
                        }
                        rayX += dirX * STEP_SIZE;
                        rayY += dirY * STEP_SIZE;
                        rayZ += dirZ * STEP_SIZE;
                        remaining -= STEP_SIZE * 0.75F;
                    }
                }
            }
        }
        return affected;
    }

    private static Map<EntityPlayer, Vec3d> processEntities(World world,
            Explosion explosion, double x, double y, double z,
            float baseSize, float rangeModifier) {
        float size = baseSize * 2.0F * rangeModifier;
        Map<EntityPlayer, Vec3d> affectedPlayers =
                new HashMap<EntityPlayer, Vec3d>();
        if (size <= 0.0F) {
            return affectedPlayers;
        }
        AxisAlignedBB search = new AxisAlignedBB(
                x - size - 1.0D, y - size - 1.0D, z - size - 1.0D,
                x + size + 1.0D, y + size + 1.0D, z + size + 1.0D);
        List<Entity> entities = world.getEntitiesWithinAABBExcludingEntity(
                null, search);
        ForgeEventFactory.onExplosionDetonate(world, explosion, entities, size);

        Vec3d[] nodes = {
            new Vec3d(x, y - NODE_DISTANCE, z),
            new Vec3d(x, y + NODE_DISTANCE, z),
            new Vec3d(x, y, z - NODE_DISTANCE),
            new Vec3d(x, y, z + NODE_DISTANCE),
            new Vec3d(x - NODE_DISTANCE, y, z),
            new Vec3d(x + NODE_DISTANCE, y, z),
            new Vec3d(x, y, z)
        };
        Map<Entity, Float> damage = new HashMap<Entity, Float>();
        for (Entity entity : entities) {
            AxisAlignedBB bounds = entity.getEntityBoundingBox();
            double distX = axisDistance(bounds.minX, bounds.maxX, x);
            double distY = axisDistance(bounds.minY, bounds.maxY, y);
            double distZ = axisDistance(bounds.minZ, bounds.maxZ, z);
            double distanceScaled = Math.sqrt(
                    distX * distX + distY * distY + distZ * distZ) / size;
            if (distanceScaled > 1.0D) {
                continue;
            }

            double deltaX = entity.posX - x;
            double deltaY = entity.posY + entity.getEyeHeight() - y;
            double deltaZ = entity.posZ - z;
            double distance = Math.sqrt(deltaX * deltaX
                    + deltaY * deltaY + deltaZ * deltaZ);
            if (distance == 0.0D) {
                continue;
            }
            deltaX /= distance;
            deltaY /= distance;
            deltaZ /= distance;

            double density = 0.0D;
            for (Vec3d node : nodes) {
                density = Math.max(
                        density, world.getBlockDensity(node, bounds));
            }
            double knockback = (1.0D - distanceScaled) * density;
            float amount = (float) ((int) ((knockback * knockback + knockback)
                    * 0.5D * 8.0D * size + 1.0D));
            Float previous = damage.get(entity);
            if (previous == null || previous.floatValue() < amount) {
                damage.put(entity, amount);
            }
            double reduced = entity instanceof EntityLivingBase
                    ? EnchantmentProtection.getBlastDamageReduction(
                            (EntityLivingBase) entity, knockback)
                    : knockback;
            entity.motionX += deltaX * reduced;
            entity.motionY += deltaY * reduced;
            entity.motionZ += deltaZ * reduced;
            entity.velocityChanged = true;
            if (entity instanceof EntityPlayer) {
                affectedPlayers.put((EntityPlayer) entity,
                        new Vec3d(deltaX * knockback,
                                deltaY * knockback,
                                deltaZ * knockback));
            }
        }
        for (Map.Entry<Entity, Float> entry : damage.entrySet()) {
            entry.getKey().attackEntityFrom(
                    explosionDamageSource(explosion), entry.getValue());
        }
        return affectedPlayers;
    }

    private static double axisDistance(double min, double max, double point) {
        return min <= point && max >= point
                ? 0.0D : Math.min(Math.abs(min - point), Math.abs(max - point));
    }

    private static DamageSource explosionDamageSource(Explosion explosion) {
        EntityLivingBase placedBy = explosion.getExplosivePlacedBy();
        return placedBy == null
                ? new DamageSource("explosion").setExplosion()
                : new EntityDamageSource("explosion.player", placedBy)
                        .setExplosion();
    }

    private static void processBlocks(World world, Explosion explosion,
            Set<BlockPos> affected, IBlockState debris) {
        for (BlockPos pos : affected) {
            IBlockState state = world.getBlockState(pos);
            if (state.getMaterial() != Material.AIR) {
                state.getBlock().onBlockExploded(world, pos, explosion);
            }
        }
        if (debris == null) {
            return;
        }
        for (BlockPos pos : affected) {
            if (!world.isAirBlock(pos)) {
                continue;
            }
            for (net.minecraft.util.EnumFacing facing
                    : net.minecraft.util.EnumFacing.VALUES) {
                IBlockState neighbor = world.getBlockState(pos.offset(facing));
                if (neighbor.isFullCube()
                        && neighbor.getBlock() != debris.getBlock()) {
                    world.setBlockState(pos, debris, 3);
                    break;
                }
            }
        }
    }

    private static void sendEffects(World world, double x, double y, double z,
            float size, Set<BlockPos> affected,
            Map<EntityPlayer, Vec3d> playerKnockback,
            boolean standardEffects) {
        if (standardEffects) {
            world.playSound(null, x, y, z,
                    net.minecraft.init.SoundEvents.ENTITY_GENERIC_EXPLODE,
                    SoundCategory.BLOCKS, 4.0F,
                    (1.0F + (world.rand.nextFloat()
                            - world.rand.nextFloat()) * 0.2F) * 0.7F);
        }
        List<BlockPos> packetBlocks = standardEffects
                ? new ArrayList<BlockPos>(affected)
                : new ArrayList<BlockPos>();
        for (EntityPlayer player : world.playerEntities) {
            if (!(player instanceof EntityPlayerMP)
                    || player.getDistanceSq(x, y, z) >= 62500.0D) {
                continue;
            }
            Vec3d knockback = playerKnockback.get(player);
            EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
            if (standardEffects) {
                serverPlayer.connection.sendPacket(
                        new SPacketExplosion(x, y, z, size, packetBlocks,
                                knockback == null ? Vec3d.ZERO : knockback));
            } else if (knockback != null) {
                serverPlayer.connection.sendPacket(
                        new SPacketEntityVelocity(serverPlayer));
            }
        }
    }
}
