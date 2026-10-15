package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.gui.ContainerModernVehicle;
import com.wartec.wartecmod.port.network.*;
import java.io.IOException;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.input.Keyboard;

/** Area fire, six individual HIMARS targets, or a dedicated AA/radar work panel. */
public final class GuiModernVehicle extends GuiContainer {
    private final EntityModernVehicle vehicle;
    private GuiTextField[][] fields;
    private GuiTextField radius;
    private GuiTextField series;
    private BlockPos[] displayed;
    private boolean[] displayedSet;
    private int[] displayedRevision;
    public GuiModernVehicle(InventoryPlayer inventory,EntityModernVehicle vehicle) {
        super(new ContainerModernVehicle(inventory,vehicle));this.vehicle=vehicle;xSize=480;ySize=346;
    }
    private static String tr(String key,Object... args) { return I18n.format("wartec.modern.ui."+key,args); }
    @Override public void initGui() {
        super.initGui();Keyboard.enableRepeatEvents(true);
        buttonList.add(new WarTechGuiButton(1,guiLeft+16,guiTop+27,104,20,tr(vehicle.isDeployed()?"stow":"deploy")));
        buttonList.add(new WarTechGuiButton(3,guiLeft+126,guiTop+27,80,20,tr("drive")));
        if(vehicle.modernProfile().isMlrs()) {
            buttonList.add(new WarTechGuiButton(2,guiLeft+16,guiTop+154,88,20,tr("single")));
            buttonList.add(new WarTechGuiButton(0,guiLeft+110,guiTop+154,88,20,tr("salvo")));
            buttonList.add(new WarTechGuiButton(4,guiLeft+110,guiTop+204,88,20,tr("cancel")));
            int count=vehicle.targetCount();fields=new GuiTextField[count][3];displayed=new BlockPos[count];displayedSet=new boolean[count];displayedRevision=new int[count];
            for(int row=0;row<count;row++) {
                for(int axis=0;axis<3;axis++) {
                    fields[row][axis]=new GuiTextField(row*3+axis,fontRenderer,guiLeft+236+axis*55,guiTop+80+row*22,50,16);
                    fields[row][axis].setMaxStringLength(10);
                }
                buttonList.add(new WarTechGuiButton(10+row,guiLeft+403,guiTop+78+row*22,28,20,tr("save")));
                buttonList.add(new WarTechGuiButton(20+row,guiLeft+436,guiTop+78+row*22,28,20,"TGT"));load(row);
            }
            if(count==1) {
                radius=new GuiTextField(100,fontRenderer,guiLeft+236,guiTop+124,50,16);
                radius.setMaxStringLength(3);radius.setText(Integer.toString(vehicle.getAreaRadius()));
            }
            series=new GuiTextField(101,fontRenderer,guiLeft+126,guiTop+182,32,16);
            series.setMaxStringLength(2);series.setText(Integer.toString(Math.min(6,vehicle.modernProfile().slots)));
            buttonList.add(new WarTechGuiButton(6,guiLeft+16,guiTop+204,88,20,tr("series")));
        } else if(!vehicle.modernProfile().isRadar()) {
            buttonList.add(new WarTechGuiButton(5,guiLeft+236,guiTop+154,200,20,tr("automatic")));
        }
    }
    private void load(int row) {
        BlockPos target=vehicle.getTarget(row);displayed[row]=target;displayedSet[row]=vehicle.hasTarget(row);
        displayedRevision[row]=vehicle.getTargetRevision(row);
        fields[row][0].setText(Integer.toString(vehicle.hasTarget(row)?target.getX():0));
        fields[row][1].setText(Integer.toString(vehicle.hasTarget(row)?target.getY():64));
        fields[row][2].setText(Integer.toString(vehicle.hasTarget(row)?target.getZ():0));
        if(row==0 && radius!=null) radius.setText(Integer.toString(vehicle.getAreaRadius()));
    }
    @Override public void updateScreen() {
        super.updateScreen();boolean busy=vehicle.getQueuedShots()>0;
        if(fields!=null) for(int row=0;row<fields.length;row++) {
            if(!displayed[row].equals(vehicle.getTarget(row)) || displayedSet[row]!=vehicle.hasTarget(row) || displayedRevision[row]!=vehicle.getTargetRevision(row)) load(row);
            for(GuiTextField field:fields[row]) { field.updateCursorCounter();field.setEnabled(!busy); }
        }
        if(radius!=null) { radius.updateCursorCounter();radius.setEnabled(!busy); }
        if(series!=null) series.updateCursorCounter();
        boolean dirty=false;
        if(fields!=null) for(int row=0;row<fields.length;row++) {
            BlockPos target=vehicle.getTarget(row);
            dirty|=vehicle.hasTarget(row) && (!fields[row][0].getText().equals(Integer.toString(target.getX()))
                || !fields[row][1].getText().equals(Integer.toString(target.getY())) || !fields[row][2].getText().equals(Integer.toString(target.getZ())));
        }
        if(radius!=null) dirty|=!radius.getText().equals(Integer.toString(vehicle.getAreaRadius()));
        for(GuiButton button:buttonList) {
            if(button.id==1) { button.displayString=tr(vehicle.isDeployed()?"stow":"deploy");button.enabled=!busy && vehicle.getCapSlot()<0; }
            else if(button.id==3) button.enabled=!vehicle.isDeployed() && vehicle.getDeployment()==0;
            else if(button.id==0 || button.id==2 || button.id==6) button.enabled=!busy && !dirty && vehicle.isDeployed() && vehicle.getDeployment()==1 && vehicle.loadedCount()>0 && vehicle.getReloadTicks()==0 && vehicle.getLegacyPower()>=2500;
            else if(button.id>=10) button.enabled=!busy;
            else if(button.id==5) button.displayString=tr(vehicle.getLegacyFireMode()==0?"disabled":"automatic");
        }
    }
    @Override protected void actionPerformed(GuiButton button) throws IOException {
        if(button.id>=10) {
            int row=button.id>=20?button.id-20:button.id-10;
            if(button.id>=20) { WarTechNetwork.CHANNEL.sendToServer(new ModernTargetMessage(vehicle.getEntityId(),row,0,0,0,vehicle.getAreaRadius(),true));return; }
            try {
                WarTechNetwork.CHANNEL.sendToServer(new ModernTargetMessage(vehicle.getEntityId(),row,
                    Integer.parseInt(fields[row][0].getText()),Integer.parseInt(fields[row][1].getText()),Integer.parseInt(fields[row][2].getText()),
                    radius==null?0:Integer.parseInt(radius.getText()),button.id>=20));
            } catch(NumberFormatException ignored) { }
        } else if(button.id==6) {
            try { int count=Integer.parseInt(series.getText());if(count>=1 && count<=vehicle.modernProfile().slots) mc.playerController.sendEnchantPacket(inventorySlots.windowId,count+29); }
            catch(NumberFormatException ignored) { }
        } else { mc.playerController.sendEnchantPacket(inventorySlots.windowId,button.id);if(button.id==3) mc.displayGuiScreen(null); }
    }
    @Override protected void drawGuiContainerBackgroundLayer(float partial,int mouseX,int mouseY) {
        drawRect(guiLeft,guiTop,guiLeft+xSize,guiTop+ySize,0xFF14201F);
        drawRect(guiLeft+8,guiTop+8,guiLeft+xSize-8,guiTop+22,0xFF29423B);
        if(!vehicle.modernProfile().isMlrs()) {
            drawRect(guiLeft+16,guiTop+59,guiLeft+207,guiTop+226,0xFF091713);
            GuiLegacyRadar.scope(guiLeft+111,guiTop+142,78,vehicle.getLegacyBlipCount(),vehicle::getLegacyBlip,
                vehicle.modernProfile().isRadar()?850:vehicle.modernProfile()==ModernVehicleProfile.OSA?220:250,vehicle.isLegacyOperational());
        }
        for(net.minecraft.inventory.Slot slot:inventorySlots.inventorySlots) GuiTheme.slot(guiLeft+slot.xPos-1,guiTop+slot.yPos-1);
    }
    @Override protected void drawGuiContainerForegroundLayer(int mouseX,int mouseY) {
        fontRenderer.drawString(I18n.format("wartecmod.vehicle."+vehicle.modernProfile().model),16,11,0xE6EEE6);
        fontRenderer.drawString(tr("status",(int)(vehicle.getDeployment()*100),vehicle.loadedCount(),vehicle.modernProfile().slots,vehicle.getQueuedShots()),236,31,0xB3D6C2);
        if(vehicle.getReloadTicks()>0) fontRenderer.drawString(tr("reload",(vehicle.getReloadTicks()+19)/20),16,52,0xE9B75B);
        fontRenderer.drawString(tr("power",vehicle.getLegacyPower(),vehicle.getEnergyCapacity()),16,267,0xADC4B5);
        fontRenderer.drawString(tr("health",Math.round(vehicle.getHealthValue()),Math.round(vehicle.getHealthCapacity())),16,279,0xADC4B5);
        if(fields!=null) {
            fontRenderer.drawString("X",236,68,0xCEDFCF);fontRenderer.drawString("Y",291,68,0xCEDFCF);fontRenderer.drawString("Z",346,68,0xCEDFCF);
            if(radius!=null) fontRenderer.drawString(tr("radius"),236,110,0xCEDFCF);
            fontRenderer.drawString(tr("ammo"),16,65,0xCEDFCF);
            fontRenderer.drawString(tr("series_count"),16,185,0xCEDFCF);
            fontRenderer.drawString(tr("battery"),16,229,0xCEDFCF);
            fontRenderer.drawString(tr("designator"),70,229,0xCEDFCF);
            fontRenderer.drawString(tr("readiness."+vehicle.readiness()),236,47,0xE9B75B);
            ModernMissileProfile profile=ModernMissileProfile.of(((com.wartec.wartecmod.port.content.MissileItem)vehicle.modernProfile().ammunition()).getProfile());
            int info=fields.length==1?154:218;
            fontRenderer.drawString(tr("distance_band",(int)profile.minimumRange,(int)profile.commandRange),236,info,0xCEDFCF);
            fontRenderer.drawString(tr("accuracy",(int)profile.cep),236,info+12,0xADC4B5);
            if(radius!=null) fontRenderer.drawString(tr("radius_help"),236,142,0xADC4B5);
            fontRenderer.drawString(tr("load_help"),16,322,0xADC4B5);
            fontRenderer.drawString(tr("target_help"),16,335,0xADC4B5);
            fontRenderer.drawString(tr("inventory"),290,238,0xADC4B5);
        } else {
            fontRenderer.drawString(tr("radar_north"),108,61,0xADC4B5);
            fontRenderer.drawString(tr("radar_scope"),23,216,0xADC4B5);
            fontRenderer.drawString(tr("contacts",vehicle.getLegacyContacts()),236,65,0xCEDFCF);
            fontRenderer.drawString(vehicle.modernProfile().isRadar()?tr("sensor"):tr("tier",vehicle.modernProfile().tier),236,83,0xCEDFCF);
            fontRenderer.drawString(tr("range",vehicle.modernProfile().isRadar()?850:vehicle.getDefenseEngagementRange()),236,101,0xCEDFCF);
            fontRenderer.drawString("IFF: "+fontRenderer.trimStringToWidth(vehicle.getOwnerTeam(),172),236,119,0xCEDFCF);
            fontRenderer.drawString(tr("battery"),16,229,0xCEDFCF);
            fontRenderer.drawSplitString(tr("radar_help"),236,180,222,0xADC4B5);
            fontRenderer.drawString(tr("inventory"),290,238,0xADC4B5);
        }
    }
    @Override public void drawScreen(int mouseX,int mouseY,float partial) {
        drawDefaultBackground();super.drawScreen(mouseX,mouseY,partial);
        if(fields!=null) for(GuiTextField[] row:fields) for(GuiTextField field:row) field.drawTextBox();
        if(radius!=null) radius.drawTextBox();if(series!=null) series.drawTextBox();renderHoveredToolTip(mouseX,mouseY);
        for(GuiButton button:buttonList) if(button.isMouseOver()) {
            String key=button.id>=20?"import_help":button.id>=10?"save_help":button.id==0?"salvo_help":button.id==6?"series_help":button.id==3?"drive_help":button.id==1?"deploy_help":null;
            if(key!=null) drawHoveringText(java.util.Arrays.asList(tr(key)),mouseX,mouseY);
        }
    }
    @Override protected void mouseClicked(int x,int y,int button) throws IOException {
        super.mouseClicked(x,y,button);if(fields!=null) for(GuiTextField[] row:fields) for(GuiTextField field:row) field.mouseClicked(x,y,button);
        if(radius!=null) radius.mouseClicked(x,y,button);
        if(series!=null) series.mouseClicked(x,y,button);
    }
    @Override protected void keyTyped(char c,int key) throws IOException {
        if(fields!=null) for(GuiTextField[] row:fields) for(GuiTextField field:row) if(field.textboxKeyTyped(c,key)) return;
        if(radius!=null && radius.textboxKeyTyped(c,key)) return;if(series!=null && series.textboxKeyTyped(c,key)) return;super.keyTyped(c,key);
    }
    @Override public void onGuiClosed() { Keyboard.enableRepeatEvents(false);super.onGuiClosed(); }
}
