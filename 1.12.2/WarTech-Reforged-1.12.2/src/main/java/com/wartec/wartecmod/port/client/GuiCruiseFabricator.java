package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.gameplay.TileEntityCruiseFabricator;
import com.wartec.wartecmod.port.gui.ContainerCruiseFabricator;
import com.wartec.wartecmod.port.network.CruiseFabricatorActionMessage;
import com.wartec.wartecmod.port.network.WarTechNetwork;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import java.io.IOException;
import java.util.Locale;
import org.lwjgl.input.Keyboard;

public final class GuiCruiseFabricator extends GuiContainer {
    private final TileEntityCruiseFabricator tile;
    private GuiTextField nameField;
    private boolean edited;
    private CruiseStats previous;
    private int previousChecksum,deltaUntil;
    private double rangeDelta,speedDelta,massDelta;
    public GuiCruiseFabricator(InventoryPlayer inventory,TileEntityCruiseFabricator tile) {
        super(new ContainerCruiseFabricator(inventory,tile)); this.tile=tile; xSize=304; ySize=266;
    }
    @Override public void initGui() {
        super.initGui(); Keyboard.enableRepeatEvents(true);
        nameField=new GuiTextField(20,fontRenderer,guiLeft+86,guiTop+100,108,16);
        nameField.setMaxStringLength(32);nameField.setText(tile.getCurrentBuild().getName());edited=false;
        buttonList.add(new WarTechGuiButton(0,guiLeft+200,guiTop+98,46,20,CruiseText.text("gui.save")));
        buttonList.add(new WarTechGuiButton(1,guiLeft+250,guiTop+98,46,20,CruiseText.text("gui.assemble")));
        previous=tile.getCurrentBuild().calculateStats();previousChecksum=tile.getCurrentBuild().checksum();
    }
    @Override protected void actionPerformed(GuiButton button) {
        if(button.id==0 || button.id==1) WarTechNetwork.CHANNEL.sendToServer(new CruiseFabricatorActionMessage(tile.getPos(),button.id,edited?nameField.getText():""));
    }
    @Override public void onGuiClosed() { Keyboard.enableRepeatEvents(false); super.onGuiClosed(); }
    @Override public void updateScreen() {
        super.updateScreen(); if(!edited && !nameField.isFocused()) nameField.setText(tile.getCurrentBuild().getName()); nameField.updateCursorCounter();
        CruiseBuild build=tile.getCurrentBuild();
        if(build.checksum()!=previousChecksum) {
            CruiseStats now=build.calculateStats();rangeDelta=now.getRange()-previous.getRange();speedDelta=(now.getSpeed()-previous.getSpeed())*20;massDelta=now.getMass()-previous.getMass();
            deltaUntil=mc.player.ticksExisted+80;previous=now;previousChecksum=build.checksum();
        }
    }
    @Override protected void keyTyped(char c,int key) throws IOException {
        if(nameField.textboxKeyTyped(c,key)) {edited=true;return;} super.keyTyped(c,key);
    }
    @Override protected void mouseClicked(int x,int y,int button) throws IOException { super.mouseClicked(x,y,button);nameField.mouseClicked(x,y,button); }
    @Override public void drawScreen(int x,int y,float partialTicks) { drawDefaultBackground();super.drawScreen(x,y,partialTicks);nameField.drawTextBox();renderHoveredToolTip(x,y); }
    @Override protected void drawGuiContainerBackgroundLayer(float partialTicks,int mouseX,int mouseY) {
        GuiTheme.frame(guiLeft,guiTop,xSize,ySize);
        Gui.drawRect(guiLeft+7,guiTop+24,guiLeft+297,guiTop+88,0xFF253036);
        Gui.drawRect(guiLeft+7,guiTop+120,guiLeft+297,guiTop+178,0xFF172126);
        for(Slot slot:inventorySlots.inventorySlots) {
            GuiTheme.slot(guiLeft+slot.xPos-1,guiTop+slot.yPos-1);
        }
        GuiTheme.panel(guiLeft+228,guiTop+122,67,48);
        CruiseRenderer.renderPreview(tile.getCurrentBuild(),guiLeft+261,guiTop+144,7);
    }
    @Override protected void drawGuiContainerForegroundLayer(int mouseX,int mouseY) {
        label(CruiseText.text("gui.title"),9,7,0xE8F1EC);
        for(CruiseSlot slot:CruiseSlot.values()) {
            String label=CruiseText.text("slot."+slot.name().toLowerCase(Locale.ROOT));
            label(fontRenderer.trimStringToWidth(label,52),12+slot.ordinal()%5*56,slot.ordinal()<5?46:78,0xCBD9D2);
        }
        label(CruiseText.text("gui.plan"),12,90,0xCBD9D2);label(CruiseText.text("gui.out"),48,90,0xCBD9D2);
        label(CruiseText.text("gui.name"),86,90,0xCBD9D2);
        CruiseBuild build=tile.getCurrentBuild(); CruiseStats stats=build.calculateStats();
        boolean delta=mc.player.ticksExisted<deltaUntil;
        label(CruiseText.text("stats.range",(stats.isValid()?"":"~")+stats.getRange())+(delta?String.format(Locale.US," (%+.0f)",rangeDelta):""),13,122,0xE1EAE4);
        label(CruiseText.text("stats.speed",String.format(Locale.US,"%.1f",stats.getSpeed()*20))+(delta?String.format(Locale.US," (%+.1f)",speedDelta):""),13,132,0xE1EAE4);
        label(CruiseText.text("stats.mass",String.format(Locale.US,"%.0f",stats.getMass()),String.format(Locale.US,"%.0f",stats.getMaximumMass()))+(delta?String.format(Locale.US," (%+.0f)",massDelta):""),13,142,0xE1EAE4);
        String status=stats.isValid()?CruiseText.text("gui.ready"):CruiseText.text("build_error."+stats.getErrors().get(0));
        label(CruiseText.text("stats.cep",String.format(Locale.US,"%.1f",CruiseCombatProfile.cep(build,0,false))),13,152,0xE1EAE4);
        label(CruiseText.text("stats.tiers",CruiseCombatProfile.navigationTier(build),CruiseCombatProfile.threatTier(build)),13,162,0xCBD9D2);
        label(fontRenderer.trimStringToWidth(status,280),13,172,stats.isValid()?0x55EE99:0xFF7777);
    }
    @Override protected void renderHoveredToolTip(int x,int y) {
        if(!mc.player.inventory.getItemStack().isEmpty()) return;
        if(isPointInRegion(10,150,284,20,x,y)) {
            CruiseBuild build=tile.getCurrentBuild();java.util.List<String> lines=new java.util.ArrayList<>();
            lines.add(CruiseText.text("stats.cep",String.format(Locale.US,"%.1f",CruiseCombatProfile.cep(build,0,false))));
            if(build.get(CruiseSlot.SEEKER)!=null && build.get(CruiseSlot.SEEKER)!=CruisePartDefinition.SEEKER_NONE)
                lines.add(CruiseText.text("stats.cep_locked",String.format(Locale.US,"%.1f",CruiseCombatProfile.cep(build,0,true))));
            lines.add(CruiseText.text("stats.cep_note"));lines.add(CruiseText.text("stats.tiers",CruiseCombatProfile.navigationTier(build),CruiseCombatProfile.threatTier(build)));
            lines.add(CruiseText.text("stats.tiers_note"));drawHoveringText(lines,x,y);return;
        }
        for(Slot slot:inventorySlots.inventorySlots) {
            if(!slot.getHasStack() || !isPointInRegion(slot.xPos,slot.yPos,16,16,x,y)) continue;
            if(!(slot.getStack().getItem() instanceof com.wartec.wartecmod.port.content.CruisePartItem)) break;
            CruisePartDefinition part=((com.wartec.wartecmod.port.content.CruisePartItem)slot.getStack().getItem()).getDefinition(slot.getStack());
            if(part==null) break;
            java.util.List<String> lines=new java.util.ArrayList<>(slot.getStack().getTooltip(mc.player,net.minecraft.client.util.ITooltipFlag.TooltipFlags.NORMAL));
            CruiseBuild before=tile.getCurrentBuild(),after=CruiseBuild.read(before.write());after.set(part.getSlot(),part);
            CruiseStats a=before.calculateStats(),b=after.calculateStats();
            lines.add(CruiseText.text("stats.preview"));
            lines.add(CruiseText.text("stats.delta_range",b.getRange(),String.format(Locale.US,"%+d",b.getRange()-a.getRange())));
            lines.add(CruiseText.text("stats.delta_speed",String.format(Locale.US,"%.1f",b.getSpeed()*20),String.format(Locale.US,"%+.1f",(b.getSpeed()-a.getSpeed())*20)));
            lines.add(CruiseText.text("stats.delta_mass",String.format(Locale.US,"%.0f",b.getMass()),String.format(Locale.US,"%+.0f",b.getMass()-a.getMass())));
            lines.add(CruiseText.text("stats.cep",String.format(Locale.US,"%.1f",CruiseCombatProfile.cep(after,0,false))));
            lines.add(CruiseText.text("stats.tiers",CruiseCombatProfile.navigationTier(after),CruiseCombatProfile.threatTier(after)));
            drawHoveringText(lines,x,y);return;
        }
        super.renderHoveredToolTip(x,y);
    }
    private void label(String text,int x,int y,int color) {
        int width=y>=122 && y<=162?210:xSize-x-10;
        GuiTheme.text(fontRenderer,text,x,y,width,color);
    }
}
