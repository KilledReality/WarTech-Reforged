package com.wartec.wartecmod.port.integration;

import com.wartec.wartecmod.WarTechReforged;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.items.ItemStackHandler;

/** Repairs NTM CE tile data that was saved with older inventory layouts. */
public final class HbmCeWorldMigration {
    private static final String CE_MARKER =
            "com.hbm.inventory.recipes.loader.GenericRecipe";
    private static final Set<TileEntity> CHECKED_TILES =
            Collections.newSetFromMap(
                    new WeakHashMap<TileEntity, Boolean>());
    private static final Map<Class<?>, Integer> EXPECTED_SLOT_COUNTS =
            new HashMap<Class<?>, Integer>();
    private static final HbmCeWorldMigration INSTANCE =
            new HbmCeWorldMigration();

    private static Boolean communityEdition;
    private static Field inventoryField;
    private static Method resizeInventoryMethod;
    private static boolean reflectionUnavailable;
    private static boolean registered;

    private HbmCeWorldMigration() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        MinecraftForge.EVENT_BUS.register(INSTANCE);
        WarTechReforged.logger.info(
                "Registered NTM CE machine inventory migration handler");
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (!isCommunityEdition(event.getWorld())) {
            return;
        }
        for (TileEntity tile : event.getChunk().getTileEntityMap().values()) {
            migrateTile(tile);
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.START
                || !isCommunityEdition(event.world)) {
            return;
        }
        for (TileEntity tile : event.world.loadedTileEntityList) {
            if (CHECKED_TILES.add(tile)) {
                migrateTile(tile);
            }
        }
    }

    private static boolean isCommunityEdition(World world) {
        if (world == null || world.isRemote) {
            return false;
        }
        if (communityEdition == null) {
            try {
                Class.forName(CE_MARKER, false,
                        HbmCeWorldMigration.class.getClassLoader());
                communityEdition = Boolean.TRUE;
            } catch (ClassNotFoundException ignored) {
                communityEdition = Boolean.FALSE;
            }
        }
        return communityEdition.booleanValue();
    }

    private static void migrateTile(TileEntity tile) {
        if (tile == null || reflectionUnavailable
                || !isHbmMachineTile(tile.getClass())) {
            return;
        }
        try {
            resolveReflection(tile.getClass());
            ItemStackHandler inventory =
                    (ItemStackHandler) inventoryField.get(tile);
            int expectedSlots = expectedSlotCount(tile.getClass());
            if (inventory == null || expectedSlots <= 0
                    || inventory.getSlots() >= expectedSlots) {
                return;
            }
            int oldSlots = inventory.getSlots();
            resizeInventoryMethod.invoke(tile, expectedSlots);
            tile.markDirty();
            WarTechReforged.logger.info(
                    "Migrated NTM CE {} inventory at {} from {} to {} slots",
                    tile.getClass().getSimpleName(), tile.getPos(), oldSlots,
                    expectedSlots);
        } catch (ReflectiveOperationException | ClassCastException exception) {
            reflectionUnavailable = true;
            WarTechReforged.logger.error(
                    "Unable to migrate legacy NTM CE machine inventories",
                    exception);
        }
    }

    private static boolean isHbmMachineTile(Class<?> tileClass) {
        Class<?> type = tileClass;
        while (type != null) {
            if ("com.hbm.tileentity.TileEntityMachineBase"
                    .equals(type.getName())) {
                return true;
            }
            type = type.getSuperclass();
        }
        return false;
    }

    private static int expectedSlotCount(Class<?> tileClass) {
        Integer cached = EXPECTED_SLOT_COUNTS.get(tileClass);
        if (cached != null) {
            return cached.intValue();
        }
        int expected = -1;
        try {
            java.lang.reflect.Constructor<?> constructor =
                    tileClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object reference = constructor.newInstance();
            Object inventory = inventoryField.get(reference);
            if (inventory instanceof ItemStackHandler) {
                expected = ((ItemStackHandler) inventory).getSlots();
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Not every HBM tile has a safe no-argument construction path.
        }
        EXPECTED_SLOT_COUNTS.put(tileClass, Integer.valueOf(expected));
        return expected;
    }

    private static void resolveReflection(Class<?> tileClass)
            throws ReflectiveOperationException {
        if (inventoryField != null && resizeInventoryMethod != null) {
            return;
        }
        Class<?> type = tileClass;
        while (type != null && (inventoryField == null
                || resizeInventoryMethod == null)) {
            if (inventoryField == null) {
                try {
                    inventoryField = type.getDeclaredField("inventory");
                    inventoryField.setAccessible(true);
                } catch (NoSuchFieldException ignored) {
                    // Continue through the HBM tile hierarchy.
                }
            }
            if (resizeInventoryMethod == null) {
                try {
                    resizeInventoryMethod =
                            type.getDeclaredMethod("resizeInventory", int.class);
                    resizeInventoryMethod.setAccessible(true);
                } catch (NoSuchMethodException ignored) {
                    // Continue through the HBM tile hierarchy.
                }
            }
            type = type.getSuperclass();
        }
        if (inventoryField == null || resizeInventoryMethod == null) {
            throw new NoSuchMethodException(
                    "NTM CE inventory migration members were not found");
        }
    }
}
