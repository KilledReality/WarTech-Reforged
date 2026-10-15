package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.entity.EntityWarTechBase;
import com.wartec.wartecmod.port.gui.ContainerLegacyEntity;
import com.wartec.wartecmod.port.network.*;
import java.io.IOException;
import java.util.*;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.client.resources.I18n;

/** Terrain, named network stations and radar tracks share one north-up coordinate system. */
public final class GuiLegacyCommandNetwork extends GuiContainer {
    private static final int MX=12,MY=44,MW=CommandMapViewport.WIDTH,MH=CommandMapViewport.HEIGHT;
    private final EntityWarTechBase command;
    private int refreshTicks,zoomIndex=2,page,tab,selectedContact=-1,selectedType=-1;
    private long selectedNode;
    private double viewX,viewZ;
    private boolean autoFit=true;
    private int dragX,dragY;private double dragViewX,dragViewZ;private boolean dragging;
    private final List<Mark> marks=new ArrayList<>();
    private final List<int[]> labels=new ArrayList<>();
    private static final class Mark { final FactionCommandSnapshot.Node n;final int x,y;Mark(FactionCommandSnapshot.Node n,int x,int y) {this.n=n;this.x=x;this.y=y;} }
    public GuiLegacyCommandNetwork(InventoryPlayer inventory,EntityWarTechBase command) {
        super(new ContainerLegacyEntity(inventory,command,ContainerLegacyEntity.Layout.COMMAND));
        this.command=command;xSize=540;ySize=374;viewX=command.posX;viewZ=command.posZ;
    }
    private String tr(String key,Object... args) { return GuiTheme.tr("cmd."+key,args); }
    private FactionCommandSnapshot snapshot() { return FactionCommandClient.getSnapshot(); }
    @Override public void initGui() {
        super.initGui();buttonList.clear();
        buttonList.add(new WarTechGuiButton(1,guiLeft+12,guiTop+238,24,17,"+"));
        buttonList.add(new WarTechGuiButton(2,guiLeft+40,guiTop+238,24,17,"-"));
        buttonList.add(new WarTechGuiButton(3,guiLeft+68,guiTop+238,88,17,tr("center")));
        buttonList.add(new WarTechGuiButton(6,guiLeft+160,guiTop+238,84,17,tr("fit")));
        buttonList.add(new WarTechGuiButton(10,guiLeft+340,guiTop+29,92,20,tr("stations")));
        buttonList.add(new WarTechGuiButton(11,guiLeft+436,guiTop+29,92,20,tr("tracks")));
        buttonList.add(new WarTechGuiButton(12,guiLeft+490,guiTop+163,17,13,"<"));
        buttonList.add(new WarTechGuiButton(13,guiLeft+510,guiTop+163,17,13,">"));
        VehiclePanelControls.add(buttonList,guiLeft,guiTop,xSize,ySize);
        buttonList.add(new WarTechGuiButton(99,guiLeft+250,guiTop+238,160,17,net.minecraft.client.resources.I18n.format("planning.title")));
        requestSnapshot();
    }
    @Override public void updateScreen() {
        super.updateScreen();
        if(autoFit && snapshot().nodes.length>0) { fit();autoFit=false;requestSnapshot(); }
        page=Math.min(page,Math.max(0,(rowCount()-1)/6));
        if(++refreshTicks>=20) {refreshTicks=0;requestSnapshot();}
    }
    @Override protected void actionPerformed(GuiButton b) throws IOException {
        if(b.id==99){mc.playerController.sendEnchantPacket(inventorySlots.windowId,99);return;}
        if(VehiclePanelControls.action(inventorySlots,b,command)) return;
        if(b.id==1 || b.id==2) { zoomIndex=Math.max(0,Math.min(CommandMapViewport.RADII.length-1,zoomIndex+(b.id==1?-1:1)));autoFit=false; }
        else if(b.id==3) { viewX=command.posX;viewZ=command.posZ; }
        else if(b.id==6) {fit();autoFit=false;}
        else if(b.id==10 || b.id==11) {tab=b.id-10;page=0;}
        else if(b.id==12) page=Math.max(0,page-1);
        else if(b.id==13) page=Math.min(Math.max(0,(rowCount()-1)/6),page+1);
        if(b.id<=6) requestSnapshot();
    }
    private void fit() { viewX=command.posX;viewZ=command.posZ;zoomIndex=CommandMapViewport.fit(snapshot().nodes,viewX,viewZ); }
    @Override protected void mouseClicked(int x,int y,int button) throws IOException {
        if(button==0 && inside(x,y)) {
            selectedType=-1;selectedContact=-1;double best=100;
            for(Mark m:marks) { double distance=sq(x-m.x,y-m.y);if(distance<best) {best=distance;selectedNode=m.n.id;selectedType=m.n.type;} }
            for(FactionCommandSnapshot.Contact c:snapshot().contacts) { double distance=sq(x-px(c.x),y-py(c.z));if(distance<best) {best=distance;selectedContact=c.entityId;selectedType=-1;} }
            if(best==100) {dragging=true;dragX=x;dragY=y;dragViewX=viewX;dragViewZ=viewZ;}
            else tab=selectedType<0?1:0;
        }
        if(button==0 && x>=guiLeft+340 && x<guiLeft+528 && y>=guiTop+54 && y<guiTop+162) {
            int row=page*6+(y-guiTop-54)/18;
            if(tab==0 && row<snapshot().nodes.length) {FactionCommandSnapshot.Node n=snapshot().nodes[row];selectedNode=n.id;selectedType=n.type;selectedContact=-1;focus(n.x,n.z);}
            if(tab==1 && row<snapshot().contacts.length) {FactionCommandSnapshot.Contact c=snapshot().contacts[row];selectedContact=c.entityId;selectedType=-1;focus(c.x,c.z);}
        }
        super.mouseClicked(x,y,button);
    }
    private void focus(double x,double z) { viewX=x;viewZ=z;autoFit=false;requestSnapshot(); }
    @Override protected void mouseClickMove(int x,int y,int button,long elapsed) {
        if(dragging && button==0) {double scale=2.0*radius()/MW;viewX=dragViewX-(x-dragX)*scale;viewZ=dragViewZ-(y-dragY)*scale;autoFit=false;}
        else super.mouseClickMove(x,y,button,elapsed);
    }
    @Override protected void mouseReleased(int x,int y,int state) {if(dragging) {dragging=false;requestSnapshot();}super.mouseReleased(x,y,state);}
    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput();int wheel=org.lwjgl.input.Mouse.getEventDWheel();
        int x=org.lwjgl.input.Mouse.getEventX()*width/mc.displayWidth,y=height-org.lwjgl.input.Mouse.getEventY()*height/mc.displayHeight-1;
        if(wheel!=0 && inside(x,y)) {zoomIndex=Math.max(0,Math.min(CommandMapViewport.RADII.length-1,zoomIndex+(wheel>0?-1:1)));autoFit=false;requestSnapshot();}
    }
    @Override protected void drawGuiContainerBackgroundLayer(float partial,int mouseX,int mouseY) {
        GuiTheme.frame(guiLeft,guiTop,xSize,ySize);
        GuiTheme.panel(guiLeft+340,guiTop+52,188,124);GuiTheme.panel(guiLeft+340,guiTop+179,188,76);
        Gui.drawRect(guiLeft+MX,guiTop+MY,guiLeft+MX+MW,guiTop+MY+MH,0xFF0D1920);
        drawMap();
        GuiTheme.panel(guiLeft+6,guiTop+258,528,88);
        GuiTheme.slot(guiLeft+510,guiTop+270);
        for(int row=0;row<3;row++) for(int col=0;col<9;col++) GuiTheme.slot(guiLeft+12+col*18,guiTop+270+row*18);
        for(int col=0;col<9;col++) GuiTheme.slot(guiLeft+12+col*18,guiTop+328);
        GuiTheme.healthBar(guiLeft+190,guiTop+285,270,8,command.getHealthValue()/command.getHealthCapacity());

    }
    private void drawMap() {
        FactionCommandSnapshot s=snapshot();FactionCommandSnapshot.Terrain terrain=s.terrain;
        if(terrain.colors.length==FactionCommandSnapshot.Terrain.WIDTH*FactionCommandSnapshot.Terrain.HEIGHT) {
            for(int row=0;row<FactionCommandSnapshot.Terrain.HEIGHT;row++) for(int col=0;col<FactionCommandSnapshot.Terrain.WIDTH;col++) {
                int color=terrain.colors[row*FactionCommandSnapshot.Terrain.WIDTH+col];if(color==0) continue;
                double left=terrain.x+((double)col/FactionCommandSnapshot.Terrain.WIDTH-.5)*2*terrain.radius;
                double top=terrain.z+((double)row/FactionCommandSnapshot.Terrain.HEIGHT-.5)*2*terrain.radius*MH/MW;
                clippedRect(px(left),py(top),px(left+2.0*terrain.radius/FactionCommandSnapshot.Terrain.WIDTH)+1,py(top+2.0*terrain.radius*MH/MW/FactionCommandSnapshot.Terrain.HEIGHT)+1,0xFF000000|((color&0xFEFEFE)>>>1));
            }
        }
        int step=Math.max(32,radius()/4);
        for(double x=Math.floor((viewX-radius())/step)*step;x<viewX+radius();x+=step) clippedRect(px(x),guiTop+MY,px(x)+1,guiTop+MY+MH,0x2238C7B5);
        for(double z=Math.floor((viewZ-radius())/step)*step;z<viewZ+radius();z+=step) clippedRect(guiLeft+MX,py(z),guiLeft+MX+MW,py(z)+1,0x2238C7B5);
        for(FactionCommandSnapshot.Sector sector:s.sectors) {
            int x1=px(sector.x*512.0),x2=px((sector.x+1)*512.0),y1=py(sector.z*512.0),y2=py((sector.z+1)*512.0);
            clippedRect(x1,y1,x2,y1+1,0x8069D7C7);clippedRect(x1,y2-1,x2,y2,0x8069D7C7);clippedRect(x1,y1,x1+1,y2,0x8069D7C7);clippedRect(x2-1,y1,x2,y2,0x8069D7C7);
        }
        FactionCommandSnapshot.Node chosen=selectedStation();
        if(chosen!=null && chosen.range>0) circle(px(chosen.x),py(chosen.z),chosen.range*MW/(2.0*radius()),0x665FE6EF);
        marks.clear();labels.clear();
        for(FactionCommandSnapshot.Node n:s.nodes) {
            int x=px(n.x),y=py(n.z);if(!inside(x,y)) continue;
            int originalX=x,originalY=y;
            for(int attempt=0;attempt<24;attempt++) {
                boolean conflict=false;for(Mark mark:marks) if(sq(mark.x-x,mark.y-y)<196) {conflict=true;break;}
                if(!conflict) break;
                double angle=attempt*2.4;double distance=14+attempt*1.8;x=originalX+(int)(Math.cos(angle)*distance);y=originalY+(int)(Math.sin(angle)*distance);
                x=Math.max(guiLeft+MX+7,Math.min(guiLeft+MX+MW-7,x));y=Math.max(guiTop+MY+7,Math.min(guiTop+MY+MH-7,y));
            }
            if(x!=originalX || y!=originalY) line(originalX,originalY,x,y,0xFF536C75);
            marks.add(new Mark(n,x,y));icon(n.type,x,y,color(n.type));
            if(isSelected(n)) border(x-7,y-7,15,15,0xFFFFFFFF);
        }
        for(Mark m:marks) if(isSelected(m.n) || s.nodes.length<=24) mapLabel(name(m.n),m.x,m.y,color(m.n.type));
        for(FactionCommandSnapshot.Contact c:s.contacts) {
            int x=px(c.x),y=py(c.z);if(!inside(x,y)) continue;int color=c.friendly?0xFF88DB9D:c.assigned?0xFFFFCD65:0xFFFF7777;
            line(x-4,y,x,y-4,color);line(x,y-4,x+4,y,color);line(x+4,y,x,y+4,color);line(x,y+4,x-4,y,color);
            double speed=Math.hypot(c.velocityX,c.velocityZ);if(speed>.001) line(x,y,x+(int)(c.velocityX/speed*12),y+(int)(c.velocityZ/speed*12),color);
            if(c.entityId==selectedContact) {border(x-7,y-7,15,15,0xFFFFFFFF);mapLabel(contactName(c),x,y,color);}
        }
    }
    private void drawRows() {
        int count=rowCount();
        for(int i=0;i<6;i++) {
            int row=page*6+i;if(row>=count) break;int y=54+i*18;
            boolean selected=tab==0?isSelected(snapshot().nodes[row]):snapshot().contacts[row].entityId==selectedContact;
            if(selected) Gui.drawRect(341,y,527,y+18,0xFF304B56);
            if(tab==0) {FactionCommandSnapshot.Node n=snapshot().nodes[row];icon(n.type,349,y+8,color(n.type));text(name(n),359,y+1,158,color(n.type));text(tr("position",(int)n.x,(int)n.z),359,y+10,158,GuiTheme.MUTED);}
            else {FactionCommandSnapshot.Contact c=snapshot().contacts[row];text(contactName(c),347,y+1,176,c.friendly?GuiTheme.GOOD:0xFF7777);text(tr("track_row",(int)c.y,Math.round(c.quality*100)),347,y+10,176,GuiTheme.MUTED);}
        }
    }
    @Override protected void drawGuiContainerForegroundLayer(int x,int y) {
        text(tr("title"),12,8,285,GuiTheme.TEXT);
        drawRows();
        int radars=count(1)+count(2),pvo=count(3);String status=tr(!command.isLegacyOperational()?"offline":radars==0?"no_radar":pvo==0?"no_pvo":"online");
        text(status,310,8,216,command.isLegacyOperational() && radars>0 && pvo>0?GuiTheme.GOOD:GuiTheme.WARN);
        text(tr("map"),12,30,104,GuiTheme.TEXT);text(tr("counts",radars,pvo),118,30,214,GuiTheme.MUTED);
        text(tr("north"),MX+MW/2-12,MY+3,35,GuiTheme.TEXT);
        text(tr("map_position",(int)viewX,(int)viewZ,radius()),MX+4,MY+MH-11,MW-8,GuiTheme.TEXT);
        icon(3,17,230,color(3));text(tr("pvo"),25,226,56,color(3));
        icon(1,84,230,color(1));text(tr("radar"),92,226,56,color(1));
        icon(4,151,230,color(4));text(tr("hq"),159,226,52,color(4));
        icon(5,214,230,color(5));text(tr("relay"),222,226,104,color(5));
        // The planning-terminal button occupies the former short map hint.
        text(rowCount()==0?tr(tab==0?"empty_stations":"empty_tracks"):tr("page",page+1,Math.max(1,(rowCount()+5)/6)),347,165,136,GuiTheme.MUTED);
        FactionCommandSnapshot.Node node=selectedStation();FactionCommandSnapshot.Contact contact=selectedTrack();
        if(node!=null) {
            text(name(node),347,184,175,color(node.type));text(tr("position3",(int)node.x,(int)node.y,(int)node.z),347,197,175,GuiTheme.TEXT);
            text(node.type==3?tr("tier_range",node.value,node.range):node.type<=2?tr("range",node.range):tr("control_node"),347,210,175,GuiTheme.TEXT);
            text(node.ammo>=0?tr("ammo",node.ammo):node.type<=2?tr("sensor_band",node.band==0?"L":node.band==1?"S":node.band==2?"X":"L/S/X"):tr("network_link"),347,223,175,GuiTheme.TEXT);
            text(tr("state."+node.status),347,239,175,node.status==1?GuiTheme.GOOD:GuiTheme.WARN);
        } else if(contact!=null) {
            text(contactName(contact),347,184,175,contact.friendly?GuiTheme.GOOD:0xFF7777);
            text(tr("position3",(int)contact.x,(int)contact.y,(int)contact.z),347,197,175,GuiTheme.TEXT);
            text(tr("speed",Math.round(Math.hypot(contact.velocityX,contact.velocityZ)*20)),347,210,175,GuiTheme.TEXT);
            text(tr("quality",Math.round(contact.quality*100),contact.sourceCount),347,223,175,GuiTheme.TEXT);
            text(tr(contact.assigned?"engaged":"tracking"),347,239,175,GuiTheme.WARN);
        } else { text(tr("select"),347,186,175,GuiTheme.TEXT);int yy=201;for(String line:fontRenderer.listFormattedStringToWidth(tr("select_hint"),173)) {text(line,347,yy,175,GuiTheme.MUTED);yy+=11;} }
        text(tr("inventory"),12,260,170,GuiTheme.MUTED);text(GuiTheme.tr("battery"),477,260,54,GuiTheme.MUTED);
        text(GuiTheme.tr("health_points",Math.round(command.getHealthValue()),Math.round(command.getHealthCapacity())),190,272,270,GuiTheme.TEXT);
        int yy=300;for(String line:fontRenderer.listFormattedStringToWidth(GuiTheme.tr("repair_hint"),300)) {if(yy>335) break;text(line,190,yy,300,GuiTheme.MUTED);yy+=10;}
    }
    private void text(String s,int x,int y,int w,int c) {GuiTheme.text(fontRenderer,s,x,y,w,c);}
    private String name(FactionCommandSnapshot.Node n) {String key="wartec.ui.cmd.system."+n.system;return I18n.hasKey(key)?I18n.format(key)+(n.system.equals("modern_osa") && n.type<=2?" / "+tr("radar"):""):tr(n.type<=2?"radar":n.type==3?"pvo":n.type==4?"hq":"relay");}
    private String contactName(FactionCommandSnapshot.Contact c) {return tr(c.friendly?"friendly":"hostile")+" "+tr("contact."+c.type)+" #"+c.entityId;}
    private int count(int type) {int n=0;for(FactionCommandSnapshot.Node node:snapshot().nodes) if(node.type==type)n++;return n;}
    private int rowCount() {return tab==0?snapshot().nodes.length:snapshot().contacts.length;}
    private boolean isSelected(FactionCommandSnapshot.Node n) {return selectedType==n.type && selectedNode==n.id;}
    private FactionCommandSnapshot.Node selectedStation() {for(FactionCommandSnapshot.Node n:snapshot().nodes) if(isSelected(n))return n;return null;}
    private FactionCommandSnapshot.Contact selectedTrack() {for(FactionCommandSnapshot.Contact c:snapshot().contacts) if(c.entityId==selectedContact)return c;return null;}
    private int radius() {return CommandMapViewport.RADII[zoomIndex];}
    private int px(double x) {return guiLeft+MX+(int)Math.round(CommandMapViewport.pixelX(x,viewX,radius()));}
    private int py(double z) {return guiTop+MY+(int)Math.round(CommandMapViewport.pixelY(z,viewZ,radius()));}
    private boolean inside(int x,int y) {return x>=guiLeft+MX && x<guiLeft+MX+MW && y>=guiTop+MY && y<guiTop+MY+MH;}
    private static double sq(int x,int y) {return (double)x*x+(double)y*y;}
    private static int color(int type) {return type==3?0xFFB3E780:type==4?0xFFFFFFFF:type==5?0xFFE6C078:type==2?0xFF88AEFF:0xFF69E0EC;}
    private static void icon(int type,int x,int y,int color) {
        Gui.drawRect(x-5,y-5,x+6,y+6,0xFF101C23);
        if(type<=2) {for(int dy=-4;dy<=0;dy++) {int dx=4+dy;Gui.drawRect(x-dx,y+dy,x+dx+1,y+dy+1,color);}Gui.drawRect(x,y,x+1,y+5,color);Gui.drawRect(x-3,y+4,x+4,y+5,color);}
        else if(type==3) {for(int dy=-4;dy<=3;dy++) {int dx=(dy+4)/2;Gui.drawRect(x-dx,y+dy,x+dx+1,y+dy+1,color);}Gui.drawRect(x-4,y+4,x+5,y+5,color);}
        else if(type==4) {border(x-4,y-4,9,9,color);Gui.drawRect(x-2,y,x+3,y+1,color);Gui.drawRect(x,y-2,x+1,y+3,color);}
        else {Gui.drawRect(x-4,y-4,x-1,y-1,color);Gui.drawRect(x+2,y-4,x+5,y-1,color);Gui.drawRect(x-4,y+2,x-1,y+5,color);Gui.drawRect(x+2,y+2,x+5,y+5,color);Gui.drawRect(x-1,y-1,x+2,y+2,color);}
    }
    private static void border(int x,int y,int w,int h,int color) {Gui.drawRect(x,y,x+w,y+1,color);Gui.drawRect(x,y+h-1,x+w,y+h,color);Gui.drawRect(x,y,x+1,y+h,color);Gui.drawRect(x+w-1,y,x+w,y+h,color);}
    private void clippedRect(int x1,int y1,int x2,int y2,int color) {x1=Math.max(x1,guiLeft+MX);y1=Math.max(y1,guiTop+MY);x2=Math.min(x2,guiLeft+MX+MW);y2=Math.min(y2,guiTop+MY+MH);if(x1<x2 && y1<y2) Gui.drawRect(x1,y1,x2,y2,color);}
    private void line(int x1,int y1,int x2,int y2,int color) {int steps=Math.max(Math.abs(x2-x1),Math.abs(y2-y1));for(int i=0;i<=steps;i++) {int x=x1+(x2-x1)*i/Math.max(1,steps),y=y1+(y2-y1)*i/Math.max(1,steps);clippedRect(x,y,x+1,y+1,color);}}
    private void circle(int x,int y,double r,int color) {if(r>Math.hypot(MW,MH)+Math.hypot(x-guiLeft-MX-MW*.5,y-guiTop-MY-MH*.5)) return;for(int i=0;i<180;i++) {double a=i*Math.PI/90,b=(i+1)*Math.PI/90;line(x+(int)(Math.cos(a)*r),y+(int)(Math.sin(a)*r),x+(int)(Math.cos(b)*r),y+(int)(Math.sin(b)*r),color);}}
    private void mapLabel(String name,int x,int y,int color) {
        String text=GuiTheme.clip(fontRenderer,name,100);int w=fontRenderer.getStringWidth(text)+4;
        for(int dy:new int[]{-14,8,-25,19}) {int xx=Math.max(guiLeft+MX+2,Math.min(guiLeft+MX+MW-w-2,x+8)),yy=y+dy;
            if(yy<guiTop+MY+14 || yy+10>guiTop+MY+MH-14) continue;
            boolean overlap=false;for(int[] rect:labels) if(xx<rect[2] && xx+w>rect[0] && yy<rect[3] && yy+10>rect[1]) {overlap=true;break;}
            if(overlap) continue;labels.add(new int[]{xx,yy,xx+w,yy+10});Gui.drawRect(xx,yy,xx+w,yy+10,0xDD0C1720);fontRenderer.drawString(text,xx+2,yy+1,color);return;
        }
    }
    private void requestSnapshot() {if(command!=null && !command.isDead && mc.getConnection()!=null && mc.getConnection().getNetworkManager().isChannelOpen()) {viewX=Math.max(command.posX-22000,Math.min(command.posX+22000,viewX));viewZ=Math.max(command.posZ-22000,Math.min(command.posZ+22000,viewZ));WarTechNetwork.CHANNEL.sendToServer(new FactionCommandRequestMessage(command.getEntityId(),viewX,viewZ,radius()));}}
    @Override public void drawScreen(int x,int y,float partial) {VehiclePanelControls.update(buttonList,command);drawDefaultBackground();super.drawScreen(x,y,partial);renderHoveredToolTip(x,y);
        if(inside(x,y)) for(Mark mark:marks) if(sq(x-mark.x,y-mark.y)<81) {
            FactionCommandSnapshot.Node n=mark.n;List<String> tooltip=new ArrayList<>();tooltip.add(name(n));tooltip.add(tr("position3",(int)n.x,(int)n.y,(int)n.z));
            tooltip.add(n.type==3?tr("tier_range",n.value,n.range):tr("range",n.range));if(n.ammo>=0)tooltip.add(tr("ammo",n.ammo));tooltip.add(tr("state."+n.status));drawHoveringText(tooltip,x,y);break;
        }
    }
}
