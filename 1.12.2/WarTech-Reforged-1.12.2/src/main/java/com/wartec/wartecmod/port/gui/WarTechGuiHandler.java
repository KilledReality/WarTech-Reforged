package com.wartec.wartecmod.port.gui;

import com.wartec.wartecmod.WarTechReforged;
import com.wartec.wartecmod.port.entity.EntityWarTechBase;
import com.wartec.wartecmod.port.entity.WarTechEntityProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public final class WarTechGuiHandler implements IGuiHandler {
    public static final int GUI_LAUNCH_TUBE = 1;
    public static final int GUI_BALLISTIC_LAUNCHER = 2;
    public static final int GUI_RADAR = 71;
    public static final int GUI_COMMAND = 72;
    public static final int GUI_MOBILE_AIR_DEFENSE = 73;
    public static final int GUI_MQ9 = 74;
    public static final int GUI_TU95 = 75;
    public static final int GUI_COMMUNICATION_MAST = 76;
    public static final int GUI_STRATEGIC_RADAR = 77;
    public static final int GUI_MOBILE_ARTILLERY = 78;

    @Override
    public Object getServerGuiElement(
        int id,
        EntityPlayer player,
        World world,
        int x,
        int y,
        int z
    ) {
        if (id == GUI_LAUNCH_TUBE || id == GUI_BALLISTIC_LAUNCHER
                || id == GUI_COMMUNICATION_MAST || id == GUI_STRATEGIC_RADAR) {
            return createTileContainer(id, player, world, new BlockPos(x, y, z));
        }
        EntityWarTechBase entity = findEntity(world, x);
        if (entity == null || id != guiForEntity(entity)) return null;
        return new ContainerLegacyEntity(player.inventory, entity, layoutFor(id));
    }

    @Override
    public Object getClientGuiElement(
        int id,
        EntityPlayer player,
        World world,
        int x,
        int y,
        int z
    ) {
        return WarTechReforged.proxy.createControlGui(id, player, world, x, y, z);
    }

    public static EntityWarTechBase findEntity(World world, int entityId) {
        Entity entity = world == null ? null : world.getEntityByID(entityId);
        return entity instanceof EntityWarTechBase ? (EntityWarTechBase) entity : null;
    }

    public static int guiForEntity(EntityWarTechBase entity) {
        WarTechEntityProfile profile = entity.getProfile();
        if (profile == WarTechEntityProfile.RADAR_TRUCK
                || profile == WarTechEntityProfile.S400_RADAR) return GUI_RADAR;
        if (profile == WarTechEntityProfile.COMMAND_TRUCK) return GUI_COMMAND;
        if (profile == WarTechEntityProfile.MOBILE_AIR_DEFENSE) return GUI_MOBILE_AIR_DEFENSE;
        if (profile == WarTechEntityProfile.MOBILE_ARTILLERY) return GUI_MOBILE_ARTILLERY;
        if (profile == WarTechEntityProfile.TU_95) return GUI_TU95;
        if (profile == WarTechEntityProfile.MQ_9_REAPER
                || profile == WarTechEntityProfile.F_16C
                || profile == WarTechEntityProfile.SU_27) return GUI_MQ9;
        return -1;
    }

    private static ContainerLegacyEntity.Layout layoutFor(int id) {
        if (id == GUI_RADAR) return ContainerLegacyEntity.Layout.RADAR;
        if (id == GUI_COMMAND) return ContainerLegacyEntity.Layout.COMMAND;
        if (id == GUI_MOBILE_AIR_DEFENSE) return ContainerLegacyEntity.Layout.AIR_DEFENSE;
        if (id == GUI_MOBILE_ARTILLERY) return ContainerLegacyEntity.Layout.ARTILLERY;
        return ContainerLegacyEntity.Layout.AIRCRAFT;
    }

    private static Object createTileContainer(int id, EntityPlayer player,
            World world, BlockPos pos) {
        net.minecraft.tileentity.TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof com.wartec.wartecmod.port.gameplay.TileEntityWarTechMachine)) {
            return null;
        }
        return new ContainerLegacyTile(player.inventory,
                (com.wartec.wartecmod.port.gameplay.TileEntityWarTechMachine) tile, id);
    }
}
