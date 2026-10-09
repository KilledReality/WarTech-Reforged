package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.gameplay.TileEntityCruiseLauncher;
import com.wartec.wartecmod.port.gui.ContainerCruiseLauncher;
import com.wartec.wartecmod.port.network.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;

public final class GuiCruiseLauncher extends GuiContainer {
    private final TileEntityCruiseLauncher tile;
    public GuiCruiseLauncher(InventoryPlayer inventory,TileEntityCruiseLauncher tile) { super(new ContainerCruiseLauncher(inventory,tile));this.tile=tile;xSize=198;ySize=222; }
    @Override public void initGui() { super.initGui();buttonList.add(new WarTechGuiButton(0,guiLeft+18,guiTop+96,162,20,CruiseText.text("launcher.launch"))); }
    @Override protected void actionPerformed(GuiButton button) { WarTechNetwork.CHANNEL.sendToServer(new CruiseLauncherActionMessage(tile.getPos().toLong())); }
    @Override protected void drawGuiContainerBackgroundLayer(float p,int x,int y) {
        GuiTheme.frame(guiLeft,guiTop,xSize,ySize);
        for(Slot slot:inventorySlots.inventorySlots) GuiTheme.slot(guiLeft+slot.xPos-1,guiTop+slot.yPos-1);
        if(!tile.isEmpty()) CruiseRenderer.renderPreview(CruiseBuild.fromStack(tile.getStackInSlot(0)),guiLeft+125,guiTop+55,15);
    }
    @Override protected void drawGuiContainerForegroundLayer(int x,int y) {
        GuiTheme.text(fontRenderer,CruiseText.text("launcher.title"),10,8,178,GuiTheme.TEXT);
        GuiTheme.text(fontRenderer,CruiseText.text(tile.isEmpty()?"launcher.empty":"launcher.ready"),18,70,162,0x78D5BB);
        GuiTheme.text(fontRenderer,CruiseText.text(tile.isRail()?"launcher.rail_hint":"launcher.booster_hint"),18,82,162,GuiTheme.MUTED);
        fontRenderer.drawString(CruiseText.text("gui.inventory"),18,128,0xE1EAE4);
        buttonList.get(0).enabled=!tile.isEmpty();
    }
    @Override public void drawScreen(int x,int y,float p) {
        drawDefaultBackground();super.drawScreen(x,y,p);renderHoveredToolTip(x,y);
        if(tile.isEmpty() && isPointInRegion(17,33,18,18,x,y))
            drawHoveringText(java.util.Arrays.asList(CruiseText.text("launcher.insert_hint"),
                CruiseText.text(tile.isRail()?"launcher.rail_hint":"launcher.booster_hint")),x,y);
    }
}
