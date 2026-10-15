package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.entity.EntityTubeArtillery;
import com.wartec.wartecmod.port.gui.ContainerTubeArtillery;
import com.wartec.wartecmod.port.network.*;
import java.io.IOException;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.input.Keyboard;

public final class GuiTubeArtillery extends GuiContainer {
    private final EntityTubeArtillery vehicle;
    private final GuiTextField[] coordinates=new GuiTextField[3];
    private BlockPos shownTarget;
    public GuiTubeArtillery(InventoryPlayer i,EntityTubeArtillery v){super(new ContainerTubeArtillery(i,v));vehicle=v;xSize=284;ySize=268;}
    private String tr(String k,Object... a){return I18n.format("wartec.tube."+k,a);}
    @Override public void initGui(){
        super.initGui();Keyboard.enableRepeatEvents(true);
        BlockPos p=vehicle.target();shownTarget=p;
        for(int i=0;i<3;i++){coordinates[i]=new GuiTextField(i,fontRenderer,guiLeft+14+i*88,guiTop+55,78,16);coordinates[i].setMaxStringLength(10);coordinates[i].setText(p==null?"":Integer.toString(i==0?p.getX():i==1?p.getY():p.getZ()));}
        buttonList.add(new GuiButton(20,guiLeft+14,guiTop+76,122,18,tr("set")));
        buttonList.add(new GuiButton(21,guiLeft+146,guiTop+76,124,18,tr("import")));
        for(int i=0;i<4;i++)buttonList.add(new GuiButton(10+i,guiLeft+12+i*26,guiTop+132,22,18,Integer.toString(i+1)));
        buttonList.add(new GuiButton(3,guiLeft+174,guiTop+132,96,18,tr("drive")));
        buttonList.add(new GuiButton(0,guiLeft+14,guiTop+154,80,20,tr("fire")));
        buttonList.add(new GuiButton(1,guiLeft+98,guiTop+154,94,20,tr("deploy")));
        buttonList.add(new GuiButton(2,guiLeft+196,guiTop+154,74,20,tr("cancel")));
    }
    @Override protected void drawGuiContainerBackgroundLayer(float partial,int mx,int my){
        drawRect(guiLeft,guiTop,guiLeft+xSize,guiTop+ySize,0xFF18242D);
        drawRect(guiLeft+5,guiTop+5,guiLeft+xSize-5,guiTop+29,0xFF263946);
        for(net.minecraft.inventory.Slot s:inventorySlots.inventorySlots){drawRect(guiLeft+s.xPos-1,guiTop+s.yPos-1,guiLeft+s.xPos+17,guiTop+s.yPos+17,0xFF52616A);drawRect(guiLeft+s.xPos,guiTop+s.yPos,guiLeft+s.xPos+16,guiTop+s.yPos+16,0xFF101B23);}
    }
    @Override protected void drawGuiContainerForegroundLayer(int mx,int my){
        GuiTheme.text(fontRenderer,tr(vehicle.tubeProfile().model),14,9,100,0xF0D69B);
        String status=tr(vehicle.readiness());GuiTheme.text(fontRenderer,status,120,9,150,0xF0F0F0);
        GuiTheme.text(fontRenderer,tr("stats",vehicle.tubeProfile().minimumRange,vehicle.tubeProfile().range,vehicle.reloadTicks()/20F,vehicle.getLegacyPower()),14,33,256,0xCBD8E0);
        for(int i=0;i<3;i++)fontRenderer.drawString(new String[]{"X","Y","Z"}[i],14+i*88,45,0xAABCC8);
        fontRenderer.drawString(tr("ammo"),14,100,0xAABCC8);fontRenderer.drawString(tr("battery"),176,100,0xAABCC8);fontRenderer.drawString(tr("designator"),227,100,0xAABCC8);
        int slot=vehicle.selectedSlot();drawRect(12+slot*26,129,34+slot*26,131,0xFFFFC65C);
        fontRenderer.drawString(I18n.format("container.inventory"),61,175,0xAABCC8);
    }
    @Override public void drawScreen(int mx,int my,float partial){
        for(GuiButton b:buttonList){if(b.id==3)b.enabled=vehicle.canDriveFromPanel();if(b.id==1)b.displayString=tr(vehicle.isDeployed()?"stow":"deploy");if(b.id==0)b.enabled="ready".equals(vehicle.readiness());if(b.id==1 || b.id>=10)b.enabled=!vehicle.isFiring();}
        drawDefaultBackground();super.drawScreen(mx,my,partial);for(GuiTextField f:coordinates)f.drawTextBox();renderHoveredToolTip(mx,my);
        if(mx>=guiLeft+5 && mx<=guiLeft+xSize-5 && my>=guiTop+5 && my<=guiTop+43)drawHoveringText(java.util.Arrays.asList(tr(vehicle.tubeProfile().model),tr(vehicle.readiness()),tr("stats",vehicle.tubeProfile().minimumRange,vehicle.tubeProfile().range,vehicle.reloadTicks()/20F,vehicle.getLegacyPower()),tr("help")),mx,my);
    }
    @Override protected void actionPerformed(GuiButton b){
        if(b.id==20 || b.id==21){
            try{int x=b.id==21?0:Integer.parseInt(coordinates[0].getText()),y=b.id==21?0:Integer.parseInt(coordinates[1].getText()),z=b.id==21?0:Integer.parseInt(coordinates[2].getText());
                WarTechNetwork.CHANNEL.sendToServer(new ModernTargetMessage(vehicle.getEntityId(),0,x,y,z,0,b.id==21));
            }catch(NumberFormatException e){for(GuiTextField f:coordinates)f.setTextColor(0xFF7777);}return;
        }
        mc.playerController.sendEnchantPacket(inventorySlots.windowId,b.id);
    }
    @Override protected void mouseClicked(int x,int y,int button)throws IOException{super.mouseClicked(x,y,button);for(GuiTextField f:coordinates)f.mouseClicked(x,y,button);}
    @Override protected void keyTyped(char c,int key)throws IOException{for(GuiTextField f:coordinates)if(f.textboxKeyTyped(c,key)){f.setTextColor(0xFFFFFF);return;}super.keyTyped(c,key);}
    @Override public void updateScreen(){super.updateScreen();for(GuiTextField f:coordinates)f.updateCursorCounter();BlockPos p=vehicle.target();if(p!=null && !p.equals(shownTarget)){shownTarget=p;for(int i=0;i<3;i++)coordinates[i].setText(Integer.toString(i==0?p.getX():i==1?p.getY():p.getZ()));}}
    @Override public void onGuiClosed(){Keyboard.enableRepeatEvents(false);super.onGuiClosed();}
}
