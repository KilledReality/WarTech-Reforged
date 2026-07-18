package com.wartec.wartecmod.compat;

import cpw.mods.fml.common.network.IGuiHandler;
import java.lang.reflect.Constructor;
import com.wartec.wartecmod.entity.vehicle.EntityCommandTruck;
import com.wartec.wartecmod.entity.vehicle.EntityMobileAirDefense;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public final class RadarGuiHandler implements IGuiHandler {
    public static final int GUI_ID = 71;
    public static final int GUI_ID_COMMAND = 72;
    public static final int GUI_ID_MOBILE_AIR_DEFENSE = 73;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world,
            int x, int y, int z) {
        IRadarGuiTarget radar = find(id, world, x);
        if (radar != null) {
            return new ContainerRadarVehicle(player.field_71071_by, radar);
        }
        EntityCommandTruck command = findCommand(id, world, x);
        if (command != null) {
            return new ContainerCommandVehicle(player.field_71071_by, command);
        }
        EntityMobileAirDefense system = findMobileAirDefense(id, world, x);
        return system == null ? null
                : new ContainerMobileAirDefense(player.field_71071_by, system);
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world,
            int x, int y, int z) {
        IRadarGuiTarget radar = find(id, world, x);
        try {
            if (radar != null) {
                Class<?> gui = Class.forName(
                        "com.wartec.wartecmod.compat.client.GuiRadarVehicle");
                Constructor<?> constructor = gui.getConstructor(
                        net.minecraft.entity.player.InventoryPlayer.class,
                        IRadarGuiTarget.class);
                return constructor.newInstance(player.field_71071_by, radar);
            }
            EntityCommandTruck command = findCommand(id, world, x);
            if (command != null) {
                Class<?> gui = Class.forName(
                        "com.wartec.wartecmod.compat.client.GuiCommandNetwork");
                Constructor<?> constructor = gui.getConstructor(
                        net.minecraft.entity.player.InventoryPlayer.class,
                        EntityCommandTruck.class);
                return constructor.newInstance(player.field_71071_by, command);
            }
            EntityMobileAirDefense system = findMobileAirDefense(id, world, x);
            if (system != null) {
                Class<?> gui = Class.forName(
                        "com.wartec.wartecmod.compat.client.GuiMobileAirDefense");
                Constructor<?> constructor = gui.getConstructor(
                        net.minecraft.entity.player.InventoryPlayer.class,
                        EntityMobileAirDefense.class);
                return constructor.newInstance(player.field_71071_by, system);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static IRadarGuiTarget find(int id, World world, int entityId) {
        if (id != GUI_ID || world == null) {
            return null;
        }
        Entity entity = world.func_73045_a(entityId);
        return entity instanceof IRadarGuiTarget ? (IRadarGuiTarget) entity : null;
    }

    private static EntityCommandTruck findCommand(int id, World world, int entityId) {
        if (id != GUI_ID_COMMAND || world == null) return null;
        Entity entity = world.func_73045_a(entityId);
        return entity instanceof EntityCommandTruck ? (EntityCommandTruck) entity : null;
    }

    private static EntityMobileAirDefense findMobileAirDefense(int id,
            World world, int entityId) {
        if (id != GUI_ID_MOBILE_AIR_DEFENSE || world == null) return null;
        Entity entity = world.func_73045_a(entityId);
        return entity instanceof EntityMobileAirDefense
                ? (EntityMobileAirDefense) entity : null;
    }
}
