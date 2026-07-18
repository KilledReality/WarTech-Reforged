package com.wartec.wartecmod.compat;

import cpw.mods.fml.common.network.IGuiHandler;
import java.lang.reflect.Constructor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public final class RadarGuiHandler implements IGuiHandler {
    public static final int GUI_ID = 71;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world,
            int x, int y, int z) {
        IRadarGuiTarget radar = find(id, world, x);
        return radar == null ? null : new ContainerRadarVehicle(player.field_71071_by, radar);
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world,
            int x, int y, int z) {
        IRadarGuiTarget radar = find(id, world, x);
        if (radar == null) {
            return null;
        }
        try {
            Class<?> gui = Class.forName(
                    "com.wartec.wartecmod.compat.client.GuiRadarVehicle");
            Constructor<?> constructor = gui.getConstructor(
                    net.minecraft.entity.player.InventoryPlayer.class,
                    IRadarGuiTarget.class);
            return constructor.newInstance(player.field_71071_by, radar);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static IRadarGuiTarget find(int id, World world, int entityId) {
        if (id != GUI_ID || world == null) {
            return null;
        }
        Entity entity = world.func_73045_a(entityId);
        return entity instanceof IRadarGuiTarget ? (IRadarGuiTarget) entity : null;
    }
}
