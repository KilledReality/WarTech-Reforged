package com.wartec.wartecmod.port.proxy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import com.wartec.wartecmod.port.uav.UavReconReport;

public class CommonProxy {
    public void spawnCruiseVisualEvent(com.wartec.wartecmod.port.network.CruiseVisualEventMessage message) { }
    public void updateCruiseTrail(com.wartec.wartecmod.port.entity.EntityCustomCruise entity) { }
    public void preInit() {
    }

    public void spawnLegacyMushroomEffect(double x, double y, double z,
            float scale) {
    }

    public void spawnLegacyKeroseneTrail(double startX, double startY,
            double startZ, double endX, double endY, double endZ) {
    }

    public void openIffSelector() {
    }
    public void openCruiseProgrammer(net.minecraft.util.EnumHand hand) { }

    public void openUavGuide(EntityPlayer player, ItemStack stack) {
    }

    public void openUavReconReport(String title, UavReconReport report) {
    }

    public Object createControlGui(
        int id,
        EntityPlayer player,
        World world,
        int x,
        int y,
        int z
    ) {
        return null;
    }
}
