package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.entity.EntityWarTechBase;
import com.wartec.wartecmod.port.entity.EntityWarTechGroundVehicle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.inventory.Container;
import java.util.List;

/** Shared vehicle actions use the open container's checked server endpoint. */
final class VehiclePanelControls {
    static void add(List<GuiButton> buttons,int left,int top,int width,int height) {
        int w=(width-24)/2;
        buttons.add(new WarTechGuiButton(90,left+8,top+height-24,w,20,GuiTheme.tr("vehicle.drive")));
        buttons.add(new WarTechGuiButton(91,left+16+w,top+height-24,w,20,""));
    }
    static void update(List<GuiButton> buttons,EntityWarTechBase target) {
        if(!(target instanceof EntityWarTechGroundVehicle)) return;
        EntityWarTechGroundVehicle vehicle=(EntityWarTechGroundVehicle)target;
        for(GuiButton b:buttons) {
            if(b.id==90) b.enabled=vehicle.canDriveFromPanel();
            if(b.id==91) {
                b.visible=vehicle.supportsDeployment();b.enabled=!vehicle.isBeingRidden() && Math.abs(vehicle.getDriveSpeed())<.01;
                b.displayString=GuiTheme.tr(vehicle.isDeployed()?"vehicle.stow":"vehicle.deploy");
            }
        }
    }
    static boolean action(Container container,GuiButton button,EntityWarTechBase target) {
        if(button.id<90 || button.id>93) return false;
        if(button.enabled) Minecraft.getMinecraft().playerController.sendEnchantPacket(container.windowId,button.id);
        return true;
    }
}
