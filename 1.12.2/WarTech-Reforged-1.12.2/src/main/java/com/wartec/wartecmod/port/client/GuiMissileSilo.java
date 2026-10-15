package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.gameplay.TileEntityMissileSilo;
import com.wartec.wartecmod.port.gui.ContainerMissileSilo;
import com.wartec.wartecmod.port.network.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import java.io.IOException;

public final class GuiMissileSilo extends GuiContainer {
    private final ContainerMissileSilo silo;private GuiTextField[] coords;private long shownTargetRevision=-1;private boolean defenseUi;
    public GuiMissileSilo(InventoryPlayer inv,TileEntityMissileSilo source){super(new ContainerMissileSilo(inv,source));silo=(ContainerMissileSilo)inventorySlots;xSize=340;ySize=329;}
    private String tr(String key,Object...args){return I18n.format(key,args);}
    @Override public void initGui(){super.initGui();buttonList.clear();
        boolean site=silo.tile.isSite();defenseUi=silo.tile.isDefenseMode();
        if(site){buttonList.add(new PlanningButton(0,guiLeft+14,guiTop+126,148,20,tr("silo.check")));buttonList.add(new PlanningButton(1,guiLeft+176,guiTop+126,150,20,tr("silo.build")));buttonList.add(new PlanningButton(7,guiLeft+14,guiTop+150,312,20,tr("silo.control")));}
        else{
            coords=new GuiTextField[3];for(int i=0;i<3;i++){coords[i]=new GuiTextField(i,fontRenderer,guiLeft+116+i*70,guiTop+67,63,18);coords[i].setMaxStringLength(10);coords[i].setText(silo.tile.target==null?"":Integer.toString(i==0?silo.tile.target.getX():i==1?silo.tile.target.getY():silo.tile.target.getZ()));}
            if(!silo.tile.isDefenseMode()){buttonList.add(new PlanningButton(2,guiLeft+116,guiTop+89,98,20,tr("silo.set_target")));buttonList.add(new PlanningButton(3,guiLeft+220,guiTop+89,106,20,tr("silo.import")));}
            buttonList.add(new PlanningButton(silo.tile.isDefenseMode()?10:9,guiLeft+116,guiTop+115,210,20,tr(silo.tile.isDefenseMode()?"silo.abm.enable":"silo.readiness")));
            buttonList.add(new PlanningButton(5,guiLeft+116,guiTop+140,98,20,tr("silo.open")));buttonList.add(new PlanningButton(6,guiLeft+220,guiTop+140,106,20,tr("silo.close")));
            if(silo.source.isSite())buttonList.add(new PlanningButton(8,guiLeft+14,guiTop+115,90,20,tr("silo.renovate")));
            if(!silo.tile.isDefenseMode())buttonList.add(new PlanningButton(4,guiLeft+116,guiTop+165,210,25,tr("silo.launch")));
        }
    }
    @Override protected void actionPerformed(GuiButton b){int[] v={0,0,0};if(b.id==2){try{for(int i=0;i<3;i++)v[i]=Integer.parseInt(coords[i].getText());}catch(NumberFormatException ex){return;}}
        WarTechNetwork.CHANNEL.sendToServer(new SiloActionMessage(inventorySlots.windowId,silo.source.getPos(),b.id,v[0],v[1],v[2]));}
    @Override public void updateScreen(){super.updateScreen();if(coords!=null && defenseUi!=silo.tile.isDefenseMode()){String[] saved={coords[0].getText(),coords[1].getText(),coords[2].getText()};initGui();for(int i=0;i<3;i++)coords[i].setText(saved[i]);}if(coords!=null){for(GuiTextField f:coords)f.updateCursorCounter();if(shownTargetRevision!=silo.tile.getTargetRevision()){shownTargetRevision=silo.tile.getTargetRevision();if(silo.tile.hasTarget())for(int i=0;i<3;i++)coords[i].setText(Integer.toString(i==0?silo.tile.target.getX():i==1?silo.tile.target.getY():silo.tile.target.getZ()));}}for(GuiButton b:buttonList){if(b.id==1)b.enabled=silo.tile.buildIndex<0 && silo.tile.linkedStart==null;if(b.id==7)b.enabled=silo.source.linkedStart!=null;if(b.id==10){b.enabled=!silo.tile.locked();b.displayString=tr(silo.tile.isDefenseEnabled()?"silo.abm.disable":"silo.abm.enable");}if(b.id==5)b.enabled=!silo.tile.payloadLocked();if(b.id==9){b.enabled=!silo.tile.locked() && (!silo.tile.targetLoading || silo.tile.isLaunchReady());b.displayString=tr(silo.tile.isLaunchReady()?"silo.cancel_readiness":"silo.readiness");}if(b.id==8)b.enabled=!silo.tile.payloadLocked() && !silo.tile.targetLoading;if(b.id==4)b.enabled=!silo.tile.isDefenseMissile() && !silo.tile.targetLoading && !silo.tile.locked() && !silo.tile.getStackInSlot(0).isEmpty();}}
    @Override protected void keyTyped(char c,int key)throws IOException{if(coords!=null)for(GuiTextField f:coords)if(f.textboxKeyTyped(c,key))return;super.keyTyped(c,key);}
    @Override protected void mouseClicked(int x,int y,int b)throws IOException{super.mouseClicked(x,y,b);if(coords!=null)for(GuiTextField f:coords)f.mouseClicked(x,y,b);}
    @Override protected void drawGuiContainerBackgroundLayer(float partial,int mx,int my){
        int x=guiLeft,y=guiTop;drawRect(x,y,x+xSize,y+ySize,0xff111e27);drawRect(x+4,y+4,x+xSize-4,y+27,0xff294653);drawRect(x+9,y+31,x+104,y+165,0xff182d38);
        // Shaft section: cradle, full rocket silhouette, native hatch indicator.
        drawRect(x+23,y+40,x+30,y+157,0xff627782);drawRect(x+82,y+40,x+89,y+157,0xff627782);drawRect(x+30,y+149,x+82,y+157,0xffe1b66e);
        if(!silo.tile.getStackInSlot(0).isEmpty()){drawRect(x+50,y+64,x+63,y+148,0xffc3d0d4);drawRect(x+53,y+56,x+60,y+65,0xffe2eced);}
        drawRect(x+31,y+39,x+81,y+45,0xff83b6bd);
        for(int i=0;i<3;i++)drawRect(x+21+i*106,y+201,x+39+i*106,y+219,0xff80949b);
        for(int r=0;r<4;r++)for(int c=0;c<9;c++){int sy=r==3?303:245+r*18;drawRect(x+88+c*18,y+sy-1,x+106+c*18,y+sy+17,0xff63757d);drawRect(x+89+c*18,y+sy,x+105+c*18,y+sy+16,0xff263b46);}
        if(coords!=null && !silo.tile.isDefenseMode())for(GuiTextField f:coords)f.drawTextBox();
    }
    @Override protected void drawGuiContainerForegroundLayer(int mx,int my){
        TileEntityMissileSilo t=silo.tile;fontRenderer.drawString(tr(t.isSite()?"tile.MissileSilo.name":"tile.SiloStart.name"),12,12,0xe9f5f8);
        String status=t.status;int split=status.indexOf('@');if(split>=0)status=tr(status.substring(0,split))+": "+status.substring(split+1);else status=tr(status);
        if(t.isSite()){
            fontRenderer.drawSplitString(tr("silo.site_help"),116,35,210,0xc7d7de);fontRenderer.drawSplitString(status,116,91,210,0xf1c67b);
            net.minecraft.util.math.BlockPos a=t.siteAnchor();fontRenderer.drawString("X "+a.getX()+"  Y "+a.getY()+"  Z "+a.getZ(),116,113,0x8bd4b4);
        }else{
            fontRenderer.drawSplitString(status,116,34,210,0xf1c67b);if(t.isDefenseMode()){fontRenderer.drawSplitString(tr("silo.abm.targets"),116,62,210,0xaed8de);fontRenderer.drawSplitString(tr("silo.abm.stats"),116,85,210,0x8bd4b4);}else for(int i=0;i<3;i++)fontRenderer.drawString(new String[]{"X","Y","Z"}[i],116+i*70,56,0xaed8de);
        }
        fontRenderer.drawString(tr("silo.ammo"),19,223,0xc7d7de);fontRenderer.drawString(tr("silo.designator"),125,223,0xc7d7de);fontRenderer.drawString(tr("silo.battery"),231,223,0xc7d7de);
        fontRenderer.drawString(tr("silo.power",t.getPower(),t.getMaxPower()),12,234,0x8bd4b4);
        if(t.isSite() && t.buildIndex>=0)fontRenderer.drawString(tr("silo.progress",t.buildIndex),116,103,0x8bd4b4);
        if(!t.isSite())fontRenderer.drawString(tr("silo.full_scale"),14,153,0x93abb6);
    }
    @Override public void drawScreen(int mx,int my,float partial){drawDefaultBackground();super.drawScreen(mx,my,partial);renderHoveredToolTip(mx,my);}
}
