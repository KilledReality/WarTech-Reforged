package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.entity.EntityWarTechGroundVehicle;
import com.wartec.wartecmod.port.entity.WarTechEntityProfile;
import com.wartec.wartecmod.port.gui.ContainerLegacyEntity;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import java.io.IOException;

/** Transport and EW controls share the normal vehicle panel workflow. */
public final class GuiGroundVehicle extends GuiContainer {
    private final EntityWarTechGroundVehicle vehicle;
    public GuiGroundVehicle(InventoryPlayer inventory,EntityWarTechGroundVehicle vehicle) {
        super(new ContainerLegacyEntity(inventory,vehicle,ContainerLegacyEntity.Layout.VEHICLE));
        this.vehicle=vehicle;xSize=256;ySize=248;
    }
    @Override public void initGui() {
        super.initGui();VehiclePanelControls.add(buttonList,guiLeft,guiTop,xSize,ySize);
        if(vehicle.getProfile()==WarTechEntityProfile.ELECTRONIC_WARFARE) {
            buttonList.add(new WarTechGuiButton(92,guiLeft+12,guiTop+70,196,20,""));
            buttonList.add(new WarTechGuiButton(93,guiLeft+12,guiTop+96,196,20,GuiTheme.tr("vehicle.band")));
        }
    }
    @Override protected void actionPerformed(GuiButton button) throws IOException { VehiclePanelControls.action(inventorySlots,button,vehicle); }
    @Override public void drawScreen(int x,int y,float partial) {
        VehiclePanelControls.update(buttonList,vehicle);
        for(GuiButton b:buttonList) if(b.id==92) b.displayString=GuiTheme.tr(vehicle.isLegacyEnabled()?"disable":"enable");
        drawDefaultBackground();super.drawScreen(x,y,partial);renderHoveredToolTip(x,y);
    }
    @Override protected void drawGuiContainerBackgroundLayer(float partial,int x,int y) {
        GuiTheme.frame(guiLeft,guiTop,xSize,ySize);GuiTheme.panel(guiLeft+7,guiTop+27,242,98);
        GuiTheme.panel(guiLeft+5,guiTop+136,246,82);
        for(Slot slot:inventorySlots.inventorySlots) GuiTheme.slot(guiLeft+slot.xPos-1,guiTop+slot.yPos-1);
    }
    @Override protected void drawGuiContainerForegroundLayer(int x,int y) {
        GuiTheme.text(fontRenderer,GuiTheme.tr(vehicle.getProfile()==WarTechEntityProfile.ELECTRONIC_WARFARE?"vehicle.ew."+vehicle.getVisualVariant():"vehicle.ural"),10,8,236,GuiTheme.TEXT);
        GuiTheme.text(fontRenderer,GuiTheme.tr("health_points",Math.round(vehicle.getHealthValue()),Math.round(vehicle.getHealthCapacity())),12,32,230,GuiTheme.TEXT);
        GuiTheme.text(fontRenderer,GuiTheme.tr("power",100L*vehicle.getLegacyPower()/Math.max(1,vehicle.getEnergyCapacity())),12,46,230,GuiTheme.ACCENT);
        if(vehicle.getProfile()==WarTechEntityProfile.ELECTRONIC_WARFARE)
            GuiTheme.text(fontRenderer,GuiTheme.tr("vehicle.band."+vehicle.getLegacySelectedPayload()),12,58,230,GuiTheme.MUTED);
        else GuiTheme.text(fontRenderer,GuiTheme.tr("vehicle.drive_hint"),12,65,230,GuiTheme.MUTED);
        GuiTheme.text(fontRenderer,GuiTheme.tr("battery"),218,93,31,GuiTheme.MUTED);
        GuiTheme.text(fontRenderer,net.minecraft.client.resources.I18n.format("container.inventory"),8,129,160,GuiTheme.MUTED);
    }
}
