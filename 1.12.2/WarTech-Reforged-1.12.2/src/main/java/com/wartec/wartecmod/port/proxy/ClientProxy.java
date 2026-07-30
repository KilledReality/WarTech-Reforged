package com.wartec.wartecmod.port.proxy;

import com.wartec.wartecmod.port.client.GuiLegacyAircraft;
import com.wartec.wartecmod.port.client.GuiLegacyAirDefense;
import com.wartec.wartecmod.port.client.GuiLegacyCommandNetwork;
import com.wartec.wartecmod.port.client.GuiLegacyMobileArtillery;
import com.wartec.wartecmod.port.client.GuiLegacyRadar;
import com.wartec.wartecmod.port.client.GuiLegacyTu95;
import com.wartec.wartecmod.port.client.RenderLegacyEntity;
import com.wartec.wartecmod.port.client.RenderWarTechTileEntity;
import com.wartec.wartecmod.port.client.RemoteControlClient;
import com.wartec.wartecmod.port.client.ParticleLegacyMushroom;
import com.wartec.wartecmod.port.client.VehicleInputController;
import com.wartec.wartecmod.port.entity.EntityWarTechAircraft;
import com.wartec.wartecmod.port.entity.EntityWarTechArtilleryProjectile;
import com.wartec.wartecmod.port.entity.EntityWarTechGroundVehicle;
import com.wartec.wartecmod.port.entity.EntityWarTechMissile;
import com.wartec.wartecmod.port.entity.EntityWarTechOrdnance;
import com.wartec.wartecmod.port.entity.EntitySatelliteMissileNuclear;
import com.wartec.wartecmod.port.gameplay.TileEntityWarTechMachine;
import com.wartec.wartecmod.port.gameplay.TileEntityWarTechVisual;
import com.wartec.wartecmod.port.client.RenderSatelliteMissileNuclear;
import com.wartec.wartecmod.port.gui.WarTechGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.common.MinecraftForge;

public final class ClientProxy extends CommonProxy {
    @Override
    public void spawnLegacyMushroomEffect(final double x, final double y,
            final double z, final float scale) {
        final net.minecraft.client.Minecraft minecraft =
                net.minecraft.client.Minecraft.getMinecraft();
        minecraft.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                if (minecraft.world != null) {
                    minecraft.effectRenderer.addEffect(
                            new ParticleLegacyMushroom(
                                    minecraft.world, x, y, z, scale));
                }
            }
        });
    }

    @Override
    public void spawnLegacyKeroseneTrail(final double startX,
            final double startY, final double startZ, final double endX,
            final double endY, final double endZ) {
        final net.minecraft.client.Minecraft minecraft =
                net.minecraft.client.Minecraft.getMinecraft();
        minecraft.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                if (minecraft.world == null) {
                    return;
                }
                double dx = endX - startX;
                double dy = endY - startY;
                double dz = endZ - startZ;
                double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (length <= 0.001D) {
                    return;
                }
                double inverse = 1.0D / length;
                for (double offset = 6.0D;
                        offset < length + 6.0D; offset += 1.0D) {
                    com.hbm.main.MainRegistry.proxy.spawnParticle(
                            endX - dx * inverse * offset,
                            endY - dy * inverse * offset,
                            endZ - dz * inverse * offset,
                            "exKerosene", null);
                }
            }
        });
    }

    @Override
    public void openIffSelector() {
        net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(
                new com.wartec.wartecmod.port.client.GuiLegacyIffTeamSelector());
    }

    @Override
    public void preInit() {
        VehicleInputController vehicleInput = new VehicleInputController();
        FMLCommonHandler.instance().bus().register(vehicleInput);
        MinecraftForge.EVENT_BUS.register(vehicleInput);
        RemoteControlClient.register();
        RenderingRegistry.registerEntityRenderingHandler(
            EntityWarTechMissile.class,
            RenderLegacyEntity::new
        );
        RenderingRegistry.registerEntityRenderingHandler(
            EntityWarTechAircraft.class,
            RenderLegacyEntity::new
        );
        RenderingRegistry.registerEntityRenderingHandler(
            EntityWarTechOrdnance.class,
            RenderLegacyEntity::new
        );
        RenderingRegistry.registerEntityRenderingHandler(
            EntityWarTechGroundVehicle.class,
            RenderLegacyEntity::new
        );
        RenderingRegistry.registerEntityRenderingHandler(
            EntityWarTechArtilleryProjectile.class,
            RenderLegacyEntity::new
        );
        RenderingRegistry.registerEntityRenderingHandler(
            EntitySatelliteMissileNuclear.class,
            RenderSatelliteMissileNuclear::new
        );
        RenderWarTechTileEntity tileRenderer = new RenderWarTechTileEntity();
        ClientRegistry.bindTileEntitySpecialRenderer(
            TileEntityWarTechMachine.class,
            tileRenderer
        );
        ClientRegistry.bindTileEntitySpecialRenderer(
            TileEntityWarTechVisual.class,
            tileRenderer
        );
    }

    @Override
    public Object createControlGui(
        int id,
        EntityPlayer player,
        World world,
        int x,
        int y,
        int z
    ) {
        com.wartec.wartecmod.port.entity.EntityWarTechBase entity =
            WarTechGuiHandler.findEntity(world, x);
        if (entity != null && id == WarTechGuiHandler.guiForEntity(entity)) {
            if (id == WarTechGuiHandler.GUI_RADAR) {
                return new GuiLegacyRadar(player.inventory, entity);
            }
            if (id == WarTechGuiHandler.GUI_COMMAND) {
                return new GuiLegacyCommandNetwork(player.inventory, entity);
            }
            if (id == WarTechGuiHandler.GUI_MOBILE_AIR_DEFENSE) {
                return new GuiLegacyAirDefense(player.inventory, entity);
            }
            if (id == WarTechGuiHandler.GUI_MOBILE_ARTILLERY
                    && entity instanceof EntityWarTechGroundVehicle) {
                return new GuiLegacyMobileArtillery(player.inventory,
                        (EntityWarTechGroundVehicle) entity);
            }
            if (id == WarTechGuiHandler.GUI_TU95) {
                return new GuiLegacyTu95(player.inventory, entity);
            }
            if (id == WarTechGuiHandler.GUI_MQ9) {
                return new GuiLegacyAircraft(player.inventory, entity);
            }
        }
        BlockPos pos = new BlockPos(x, y, z);
        net.minecraft.tileentity.TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntityWarTechMachine)) return null;
        return com.wartec.wartecmod.port.client.GuiLegacyTile.create(
                player.inventory, (TileEntityWarTechMachine) tile, id);
    }
}
