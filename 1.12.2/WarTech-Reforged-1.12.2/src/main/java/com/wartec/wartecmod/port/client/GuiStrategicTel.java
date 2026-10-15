package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.entity.EntityStrategicTel;
import com.wartec.wartecmod.port.entity.StrategicSystemProfile;
import com.wartec.wartecmod.port.gui.ContainerLegacyEntity;
import com.wartec.wartecmod.port.network.TelTargetMessage;
import com.wartec.wartecmod.port.network.WarTechNetwork;
import java.io.IOException;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.input.Keyboard;

/** Each tactical tube has its own visible coordinate row and server-side TGT import. */
public final class GuiStrategicTel extends GuiContainer {
    private final EntityStrategicTel tel;
    private final GuiTextField[][] fields;
    private final BlockPos[] displayed;
    private final boolean[] displayedSet,dirty;
    private final int[] displayedRevision;
    private final int offset;
    public GuiStrategicTel(InventoryPlayer inventory,EntityStrategicTel tel) {
        super(new ContainerLegacyEntity(inventory,tel,ContainerLegacyEntity.Layout.STRATEGIC));this.tel=tel;
        fields=new GuiTextField[tel.getTargetCount()][3];displayed=new BlockPos[fields.length];
        displayedSet=new boolean[fields.length];dirty=new boolean[fields.length];
        displayedRevision=new int[fields.length];
        offset=tel.getStrategicSystem().isTactical()?54:0;xSize=320;ySize=298+offset;
    }
    private static String tr(String key,Object... values) { return I18n.format("wartec.tel.ui."+key,values); }
    @Override public void initGui() {
        super.initGui();Keyboard.enableRepeatEvents(true);
        for(int slot=0;slot<fields.length;slot++) {
            int row=slot*54;
            fields[slot][0]=field(slot*3,24,74+row,64);
            fields[slot][1]=field(slot*3+1,112,74+row,36);
            fields[slot][2]=field(slot*3+2,172,74+row,60);
            loadRow(slot);
            buttonList.add(new WarTechGuiButton(4+slot,guiLeft+240,guiTop+72+row,64,20,tr("save")));
            buttonList.add(new WarTechGuiButton(6+slot,guiLeft+240,guiTop+94+row,64,20,tr("import")));
        }
        buttonList.add(new WarTechGuiButton(1,guiLeft+16,guiTop+190+offset,100,20,tr("deploy")));
        buttonList.add(new WarTechGuiButton(3,guiLeft+124,guiTop+190+offset,76,20,tr("drive")));
        buttonList.add(new WarTechGuiButton(0,guiLeft+174,guiTop+145+offset,130,20,tr(tel.getStrategicSystem().isTactical()?"launch_left":"launch")));
        if(tel.getStrategicSystem().isTactical()) buttonList.add(new WarTechGuiButton(2,guiLeft+174,guiTop+167+offset,130,20,tr("launch_right")));
        updateScreen();
    }
    private GuiTextField field(int id,int px,int py,int w) {
        GuiTextField f=new GuiTextField(id,fontRenderer,guiLeft+px,guiTop+py,w,16);f.setMaxStringLength(10);return f;
    }
    private void loadRow(int slot) {
        BlockPos p=tel.getTarget(slot);boolean set=tel.hasTarget(slot);
        fields[slot][0].setText(Integer.toString(set?p.getX():0));
        fields[slot][1].setText(Integer.toString(set?p.getY():64));
        fields[slot][2].setText(Integer.toString(set?p.getZ():0));
        displayed[slot]=p;displayedSet[slot]=set;dirty[slot]=false;
        displayedRevision[slot]=tel.getTargetRevision(slot);
    }
    @Override public void updateScreen() {
        super.updateScreen();boolean busy=tel.getLaunchTicks()>0;
        for(int slot=0;slot<fields.length;slot++) {
            // A successful import updates only its row; another draft is preserved.
            if(!tel.getTarget(slot).equals(displayed[slot]) || tel.hasTarget(slot)!=displayedSet[slot] || displayedRevision[slot]!=tel.getTargetRevision(slot)) loadRow(slot);
            for(GuiTextField field:fields[slot]) { field.updateCursorCounter();field.setEnabled(!busy); }
        }
        for(GuiButton button:buttonList) {
            if(button.id==1) { button.displayString=tr(tel.isDeployed()?"stow":"deploy");button.enabled=!busy; }
            else if(button.id==3) button.enabled=!tel.isDeployed() && tel.getErectionProgress()==0 && !busy;
            else if(button.id>=4) button.enabled=!busy && (button.id<6 || !((ContainerLegacyEntity)inventorySlots).getDesignator().isEmpty());
            else {
                int slot=button.id==2?1:0;
                button.enabled=!busy && tel.isDeployed() && tel.getErectionProgress()==100 && tel.hasTarget(slot)
                    && !dirty[slot] && (tel.getMissileMask()&(1<<slot))!=0;
            }
        }
    }
    @Override protected void actionPerformed(GuiButton button) throws IOException {
        if(!button.enabled) return;
        if(button.id>=6) WarTechNetwork.CHANNEL.sendToServer(TelTargetMessage.fromDesignator(tel.getEntityId(),button.id-6));
        else if(button.id>=4) {
            int slot=button.id-4;
            try {
                WarTechNetwork.CHANNEL.sendToServer(new TelTargetMessage(tel.getEntityId(),slot,
                    Integer.parseInt(fields[slot][0].getText()),Integer.parseInt(fields[slot][1].getText()),Integer.parseInt(fields[slot][2].getText())));
                // Saving an unchanged target need not trigger another metadata packet.
                dirty[slot]=!tel.hasTarget(slot) || !tel.getTarget(slot).equals(new BlockPos(Integer.parseInt(fields[slot][0].getText()),Integer.parseInt(fields[slot][1].getText()),Integer.parseInt(fields[slot][2].getText())));
            } catch(NumberFormatException ignored) { }
        } else mc.playerController.sendEnchantPacket(inventorySlots.windowId,button.id);
    }
    @Override protected void mouseClicked(int mx,int my,int button) throws IOException {
        super.mouseClicked(mx,my,button);for(GuiTextField[] row:fields) for(GuiTextField field:row) field.mouseClicked(mx,my,button);
    }
    @Override protected void keyTyped(char c,int code) throws IOException {
        if(code==Keyboard.KEY_ESCAPE) { super.keyTyped(c,code);return; }
        if(code==Keyboard.KEY_TAB) {
            int current=-1;for(int i=0;i<fields.length*3;i++) if(fields[i/3][i%3].isFocused()) current=i;
            int next=(current+(isShiftKeyDown()?fields.length*3-1:1))%(fields.length*3);
            for(int i=0;i<fields.length*3;i++) fields[i/3][i%3].setFocused(i==next);return;
        }
        for(int slot=0;slot<fields.length;slot++) for(GuiTextField field:fields[slot]) {
            String before=field.getText();
            if(field.textboxKeyTyped(c,code)) { if(!before.equals(field.getText())) dirty[slot]=true;return; }
        }
        super.keyTyped(c,code);
    }
    @Override public void onGuiClosed() { super.onGuiClosed();Keyboard.enableRepeatEvents(false); }
    @Override public void drawScreen(int mx,int my,float partial) {
        drawDefaultBackground();super.drawScreen(mx,my,partial);renderHoveredToolTip(mx,my);
    }
    @Override protected void drawGuiContainerBackgroundLayer(float partial,int mx,int my) {
        GuiTheme.frame(guiLeft,guiTop,xSize,ySize);GuiTheme.panel(guiLeft+8,guiTop+26,304,30);
        GuiTheme.bar(guiLeft+16,guiTop+49,288,4,tel.getErectionProgress()/100D);
        GuiTheme.panel(guiLeft+8,guiTop+122+offset,304,65);GuiTheme.slot(guiLeft+14,guiTop+146+offset);
        if(tel.getStrategicSystem().isTactical()) GuiTheme.slot(guiLeft+14,guiTop+168+offset);
        GuiTheme.slot(guiLeft+15,guiTop+215+offset);
        GuiTheme.healthBar(guiLeft+210,guiTop+202+offset,94,6,tel.getHealthValue()/tel.getHealthCapacity());
        for(GuiTextField[] row:fields) for(GuiTextField field:row) field.drawTextBox();
        net.minecraft.client.renderer.GlStateManager.disableDepth();
        inventoryGrid(guiLeft+83,guiTop+215+offset,3);inventoryGrid(guiLeft+83,guiTop+273+offset,1);
    }
    private static void inventoryGrid(int px,int py,int rows) {
        Gui.drawRect(px,py,px+162,py+rows*18,0xFF82949B);
        for(int column=0;column<=9;column++) Gui.drawRect(px+column*18-1,py,px+column*18+1,py+rows*18,0xFF10191E);
        for(int row=0;row<=rows;row++) Gui.drawRect(px,py+row*18-1,px+162,py+row*18+1,0xFF10191E);
    }
    @Override protected void drawGuiContainerForegroundLayer(int mx,int my) {
        StrategicSystemProfile system=tel.getStrategicSystem();
        GuiTheme.text(fontRenderer,tr("title",tr("system."+system.getId())),12,8,296,GuiTheme.TEXT);
        GuiTheme.text(fontRenderer,tr("state",tel.getErectionProgress(),tel.getLaunchTicks()>0?tr("sequence"):tr("idle")),16,32,288,GuiTheme.MUTED);
        for(int slot=0;slot<fields.length;slot++) {
            BlockPos p=tel.getTarget(slot);
            GuiTheme.text(fontRenderer,tr("target_slot",slot+1,tel.hasTarget(slot)?p.getX()+" / "+p.getY()+" / "+p.getZ():tr("none")),16,60+slot*54,288,GuiTheme.ACCENT);
            fontRenderer.drawString("X",14,78+slot*54,GuiTheme.MUTED);fontRenderer.drawString("Y",100,78+slot*54,GuiTheme.MUTED);fontRenderer.drawString("Z",160,78+slot*54,GuiTheme.MUTED);
            if(dirty[slot]) GuiTheme.text(fontRenderer,tr("unsaved"),16,98+slot*54,216,GuiTheme.WARN);
        }
        GuiTheme.text(fontRenderer,tr("payload",system.isTactical()?tr("tactical"):system.isNuclear()?tr("nuclear"):tr("kinetic"),
            (int)system.getMinimumRange(),(int)system.getMaximumRange()),16,128+offset,288,system.isNuclear()?GuiTheme.WARN:GuiTheme.TEXT);
        GuiTheme.text(fontRenderer,tr("ammo",1,(tel.getMissileMask()&1)!=0?tr("loaded"):tr("empty")),40,151+offset,126,GuiTheme.TEXT);
        if(system.isTactical()) GuiTheme.text(fontRenderer,tr("ammo",2,(tel.getMissileMask()&2)!=0?tr("loaded"):tr("empty")),40,173+offset,126,GuiTheme.TEXT);
        else GuiTheme.text(fontRenderer,tr("warheads",system.getReentryVehicles()),40,173+offset,264,GuiTheme.MUTED);
        GuiTheme.text(fontRenderer,tr("health",(int)tel.getHealthValue(),(int)tel.getHealthCapacity()),210,191+offset,94,GuiTheme.MUTED);
        fontRenderer.drawString("TGT",16,237+offset,GuiTheme.ACCENT);
        int yy=250+offset;for(String line:fontRenderer.listFormattedStringToWidth(tr("designator"),62)) { fontRenderer.drawString(line,12,yy,GuiTheme.MUTED);yy+=10; }
    }
}
