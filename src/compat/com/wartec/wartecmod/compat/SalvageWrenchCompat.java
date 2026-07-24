package com.wartec.wartecmod.compat;

import com.wartec.wartecmod.blocks.launcher.BallisticMissileLauncher;
import com.wartec.wartecmod.blocks.vls.VlsExhaust;
import com.wartec.wartecmod.blocks.vls.VlsVerticalLauncher;
import com.wartec.wartecmod.entity.missile.EntityMq9Drone;
import com.wartec.wartecmod.entity.missile.EntityTacticalAircraft;
import com.wartec.wartecmod.entity.missile.EntityTu95Bomber;
import com.wartec.wartecmod.entity.vehicle.EntityCommandTruck;
import com.wartec.wartecmod.entity.vehicle.EntityElectronicWarfareUnit;
import com.wartec.wartecmod.entity.vehicle.EntityMobileAirDefense;
import com.wartec.wartecmod.entity.vehicle.EntityMobileArtillery;
import com.wartec.wartecmod.entity.vehicle.EntityRadarTruck;
import com.wartec.wartecmod.entity.vehicle.EntityS400Radar;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * One recovery path for every deployable WarTech installation. Handling the
 * wrench on Forge's interaction event keeps block/entity GUIs from consuming
 * Shift + RMB before the dismantle action can run.
 */
public final class SalvageWrenchCompat {
    @SubscribeEvent
    public void onEntityInteract(EntityInteractEvent event) {
        EntityPlayer player = event.entityPlayer;
        Entity target = event.target;
        if (!isRecoveryAction(player) || target == null) return;

        ItemStack recovery = recoveryStack(target);
        if (recovery == null) return;
        event.setCanceled(true);
        if (player.field_70170_p.field_72995_K) return;

        if (!isGroundedAircraft(target)) {
            player.func_145747_a(new ChatComponentText(
                    "Aircraft must be landed before dismantling."));
            return;
        }

        if (target instanceof EntityMobileArtillery) {
            ((EntityMobileArtillery) target).dismantleForRecovery(
                    !player.field_71075_bZ.field_75098_d);
        } else {
            if (!player.field_71075_bZ.field_75098_d) {
                dropInventory(target, target);
                target.func_70099_a(recovery, 0.6F);
            }
            MissileChunkLoader.untrack(target);
            target.func_70106_y();
        }
        target.field_70170_p.func_72908_a(target.field_70165_t,
                target.field_70163_u, target.field_70161_v,
                "random.anvil_use", 0.8F, 1.35F);
    }

    @SubscribeEvent
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK
                || !isRecoveryAction(event.entityPlayer)) return;
        World world = event.world;
        if (world == null) return;

        int x = event.x;
        int y = event.y;
        int z = event.z;
        Block block = world.func_147439_a(x, y, z);
        if (block == RadarNetworkContent.communicationMastSegment) {
            int baseY = findCommunicationMastBase(world, x, y, z);
            if (baseY == Integer.MIN_VALUE) return;
            y = baseY;
            block = world.func_147439_a(x, y, z);
        }
        if (!isRecoverableBlock(block)) return;

        event.setCanceled(true);
        if (world.field_72995_K) return;
        TileEntity tile = world.func_147438_o(x, y, z);
        if (!event.entityPlayer.field_71075_bZ.field_75098_d) {
            dropInventory(tile, world, x, y, z);
            Item item = Item.func_150898_a(block);
            if (item != null) {
                world.func_72838_d(new EntityItem(world,
                        x + 0.5D, y + 0.5D, z + 0.5D,
                        new ItemStack(item, 1, world.func_72805_g(x, y, z))));
            }
        }
        world.func_147468_f(x, y, z);
        world.func_72908_a(x + 0.5D, y + 0.5D, z + 0.5D,
                "random.anvil_use", 0.8F, 1.35F);
    }

    private static boolean isRecoveryAction(EntityPlayer player) {
        return player != null && player.func_70093_af()
                && DroneStrikeContent.isSalvageWrench(
                        player.func_71045_bC());
    }

    private static boolean isGroundedAircraft(Entity entity) {
        if (entity instanceof EntityTacticalAircraft
                || entity instanceof EntityMq9Drone) {
            EntityMq9Drone aircraft = (EntityMq9Drone) entity;
            return aircraft.isReady() || aircraft.isWrecked();
        }
        if (entity instanceof EntityTu95Bomber) {
            EntityTu95Bomber bomber = (EntityTu95Bomber) entity;
            return bomber.isReady() || bomber.isWrecked();
        }
        return true;
    }

    private static ItemStack recoveryStack(Entity entity) {
        if (entity instanceof EntityTacticalAircraft) {
            return new ItemStack(((EntityTacticalAircraft) entity).getVariant()
                    == EntityTacticalAircraft.SU27
                    ? TacticalAviationContent.su27Aircraft
                    : TacticalAviationContent.f16Aircraft);
        }
        if (entity instanceof EntityMq9Drone) {
            return new ItemStack(DroneStrikeContent.mq9Drone);
        }
        if (entity instanceof EntityTu95Bomber) {
            return new ItemStack(StrategicAviationContent.tu95Bomber);
        }
        if (entity instanceof EntityRadarTruck) {
            return new ItemStack(RadarNetworkContent.radarTruck);
        }
        if (entity instanceof EntityS400Radar) {
            return new ItemStack(RadarNetworkContent.s400Radar);
        }
        if (entity instanceof EntityCommandTruck) {
            return new ItemStack(RadarNetworkContent.commandTruck);
        }
        if (entity instanceof EntityElectronicWarfareUnit) {
            return new ItemStack(RadarNetworkContent.electronicWarfareUnit,
                    1, ((EntityElectronicWarfareUnit) entity).getMode());
        }
        if (entity instanceof EntityMobileAirDefense) {
            return new ItemStack(RadarNetworkContent.mobileAirDefense,
                    1, ((EntityMobileAirDefense) entity).getVariant());
        }
        if (entity instanceof EntityMobileArtillery) {
            return new ItemStack(MobileArtilleryContent.mobileArtillery,
                    1, ((EntityMobileArtillery) entity).getMount());
        }
        return null;
    }

    private static boolean isRecoverableBlock(Block block) {
        if (block == null) return false;
        if (block == PatriotContent.patriotLauncher
                || block == PatriotContent.s400Launcher
                || block == AdvancedMissileContent.geranLauncher
                || block == RadarNetworkContent.airRaidRelay
                || block == RadarNetworkContent.communicationRelay) {
            return true;
        }
        return block instanceof VlsExhaust
                || block instanceof VlsVerticalLauncher
                || block instanceof BallisticMissileLauncher;
    }

    private static int findCommunicationMastBase(World world,
            int x, int y, int z) {
        for (int offset = 1; offset <= 6; ++offset) {
            int candidateY = y - offset;
            if (world.func_147439_a(x, candidateY, z)
                    == RadarNetworkContent.communicationRelay) {
                return candidateY;
            }
        }
        return Integer.MIN_VALUE;
    }

    private static void dropInventory(Object holder, Entity entity) {
        if (!(holder instanceof IInventory)) return;
        IInventory inventory = (IInventory) holder;
        for (int slot = 0; slot < inventory.func_70302_i_(); ++slot) {
            ItemStack stack = inventory.func_70304_b(slot);
            if (stack != null) entity.func_70099_a(stack, 0.6F);
        }
    }

    private static void dropInventory(Object holder, World world,
            int x, int y, int z) {
        if (!(holder instanceof IInventory)) return;
        IInventory inventory = (IInventory) holder;
        for (int slot = 0; slot < inventory.func_70302_i_(); ++slot) {
            ItemStack stack = inventory.func_70304_b(slot);
            if (stack != null) {
                world.func_72838_d(new EntityItem(world,
                        x + 0.5D, y + 0.5D, z + 0.5D, stack));
            }
        }
    }
}
