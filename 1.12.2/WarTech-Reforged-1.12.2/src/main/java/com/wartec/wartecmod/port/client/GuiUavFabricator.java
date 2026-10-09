package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.gameplay.TileEntityUavFabricator;
import com.wartec.wartecmod.port.gui.ContainerUavFabricator;
import com.wartec.wartecmod.port.network.UavFabricatorActionMessage;
import com.wartec.wartecmod.port.network.WarTechNetwork;
import com.wartec.wartecmod.port.uav.UavBuild;
import com.wartec.wartecmod.port.uav.UavPartDefinition;
import com.wartec.wartecmod.port.uav.UavSlot;
import com.wartec.wartecmod.port.uav.UavStats;
import java.io.IOException;
import java.util.Locale;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import org.lwjgl.input.Keyboard;

/** Compact, localized workbench UI with visible slot and range semantics. */
public final class GuiUavFabricator extends GuiContainer {
    private static final String[] SLOT_NAMES = {
        "AIRFRAME", "ENGINE", "FUEL / POWER", "FLIGHT CTRL",
        "DATA LINK", "SENSOR", "PAYLOAD", "DEFENSE"
    };
    private final TileEntityUavFabricator tile;
    private GuiTextField nameField;
    private boolean nameEdited;
    private final java.util.List<String> clippedText=new java.util.ArrayList<>();
    private final java.util.List<int[]> clippedBounds=new java.util.ArrayList<>();

    public GuiUavFabricator(InventoryPlayer inventory,
            TileEntityUavFabricator tile) {
        super(new ContainerUavFabricator(inventory, tile));
        this.tile = tile;
        xSize = 304;
        ySize = 296;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        nameField = new GuiTextField(20, fontRenderer,
                guiLeft + 202, guiTop + 71, 94, 16);
        nameField.setMaxStringLength(32);
        nameField.setText(tile.getCurrentBuild().getName());
        nameEdited = false;
        buttonList.add(new WarTechGuiButton(0, guiLeft + 202, guiTop + 91,
                94, 20, com.wartec.wartecmod.port.uav.UavText.ui("SAVE DESIGN")));
        buttonList.add(new WarTechGuiButton(1, guiLeft + 202, guiTop + 116,
                94, 20, com.wartec.wartecmod.port.uav.UavText.ui("BUILD FROM PLAN")));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.enabled && (button.id == 0 || button.id == 1)) {
            WarTechNetwork.CHANNEL.sendToServer(
                    new UavFabricatorActionMessage(tile.getPos(), button.id,
                            nameEdited ? nameField.getText() : ""));
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (!nameEdited && !nameField.isFocused()) {
            String suggested = tile.getCurrentBuild().getName();
            if (!suggested.equals(nameField.getText())) {
                nameField.setText(suggested);
            }
        }
        nameField.updateCursorCounter();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (nameField.textboxKeyTyped(typedChar, keyCode)) {
            nameEdited = true;
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
            throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        nameField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        nameField.drawTextBox();
        renderHoveredToolTip(mouseX, mouseY);
        if(mc.player.inventory.getItemStack().isEmpty() && getSlotUnderMouse()==null)
            for(int i=0;i<clippedText.size();i++) {
                int[] b=clippedBounds.get(i);
                if(isPointInRegion(b[0],b[1],b[2],9,mouseX,mouseY)) {
                    drawHoveringText(fontRenderer.listFormattedStringToWidth(clippedText.get(i),250),mouseX,mouseY);break;
                }
            }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks,
            int mouseX, int mouseY) {
        GuiTheme.frame(guiLeft,guiTop,xSize,ySize);
        Gui.drawRect(guiLeft + 7, guiTop + 26, guiLeft + 194,
                guiTop + 129, 0xFF252D30);
        Gui.drawRect(guiLeft + 199, guiTop + 26, guiLeft + 297,
                guiTop + 140, 0xFF172126);
        Gui.drawRect(guiLeft + 7, guiTop + 144, guiLeft + 297,
                guiTop + 200, 0xFF172126);
        GuiTheme.panel(guiLeft+65,guiTop+209,174,85);

        for (int index = 0; index < inventorySlots.inventorySlots.size(); ++index) {
            Slot slot = inventorySlots.inventorySlots.get(index);
            int outer = 0xFF454B48;
            int inner = 0xFFA0A49F;
            if (index == TileEntityUavFabricator.BLUEPRINT_SLOT) {
                inner = 0xFF617A8C;
            } else if (index == TileEntityUavFabricator.OUTPUT_SLOT) {
                inner = 0xFF4F8061;
            }
            Gui.drawRect(guiLeft + slot.xPos - 1, guiTop + slot.yPos - 1,
                    guiLeft + slot.xPos + 17, guiTop + slot.yPos + 17, outer);
            Gui.drawRect(guiLeft + slot.xPos, guiTop + slot.yPos,
                    guiLeft + slot.xPos + 16, guiTop + slot.yPos + 16, inner);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        clippedText.clear();clippedBounds.clear();
        label(com.wartec.wartecmod.port.uav.UavText.ui("UAV CONSTRUCTOR"), 9, 7, 0xE8F1EC);
        for (UavSlot slot : UavSlot.values()) {
            int index = slot.ordinal();
            int x = index < 4 ? 37 : 133;
            int y = 37 + index % 4 * 25;
            label(fontRenderer.trimStringToWidth(com.wartec.wartecmod.port.uav.UavText.ui(SLOT_NAMES[index]),index<4?88:60), x, y, 0xD0D7D2);
        }
        label(com.wartec.wartecmod.port.uav.UavText.ui("PLAN"), 202, 29, 0xBFC8C1);
        label(com.wartec.wartecmod.port.uav.UavText.ui("OUT"), 270, 29, 0xBFC8C1);
        label(com.wartec.wartecmod.port.uav.UavText.ui("NAME"), 202, 61, 0xBFC8C1);

        UavBuild build = tile.getCurrentBuild();
        UavStats stats = build.calculateStats();
        if (stats.getAirframe() == null) {
            label(com.wartec.wartecmod.port.uav.UavText.ui("Install an airframe to begin."),
                    13, 151, 0xFF7777);
            label(com.wartec.wartecmod.port.uav.UavText.ui("Each labeled slot accepts one matching component."),
                    13, 165, 0xD8DFD8);
        } else {
            int light = 0xD8DFD8;
            label(com.wartec.wartecmod.port.uav.UavText.ui("AIRFRAME: ")
                    + airframeLabel(stats).toUpperCase(Locale.ROOT),
                    13, 149, light);
            label(com.wartec.wartecmod.port.uav.UavText.ui("MAX FLIGHT RANGE: ")
                    + number(stats.getRange()) + com.wartec.wartecmod.port.uav.UavText.ui(" blocks"), 13, 158, light);
            label(com.wartec.wartecmod.port.uav.UavText.ui("REMOTE LINK: ")
                    + number(stats.getLinkRange()) + com.wartec.wartecmod.port.uav.UavText.ui(" blocks"), 13, 167, light);
            label(String.format(Locale.US,
                    decimalTemplate("TOP SPEED: %.1f blocks/sec",1), stats.getSpeed() * 20.0D),
                    13, 176, light);
            label(String.format(Locale.US,
                    decimalTemplate("ENDURANCE: %.1f min",1), stats.getEnduranceMinutes()),
                    13, 185, light);

            label(String.format(Locale.US,
                    decimalTemplate("MASS: %.1f / %.1f kg",1), stats.getMass(),
                    stats.getMaximumMass()), 172, 149, massColor(stats));
            UavPartDefinition engine = build.get(UavSlot.PROPULSION);
            double thrustToMass = engine == null ? 0.0D
                    : engine.getPrimary() / Math.max(1.0D, stats.getMass());
            label(String.format(Locale.US,
                    decimalTemplate("THRUST/MASS: %.2f",2), thrustToMass), 172, 158, light);
            label(com.wartec.wartecmod.port.uav.UavText.ui("MISSION: ")
                    + (stats.getBlastStrength() > 0.0F
                            ? com.wartec.wartecmod.port.uav.UavText.ui("KAMIKAZE") : com.wartec.wartecmod.port.uav.UavText.ui("REUSABLE")),
                    172, 167, light);
            label(com.wartec.wartecmod.port.uav.UavText.ui("PYLONS: ") + stats.getHardpoints()
                    + com.wartec.wartecmod.port.uav.UavText.ui("   FLARES: ") + stats.getFlares(),
                    172, 176, light);
            label(com.wartec.wartecmod.port.uav.UavText.ui("HP ") + Math.round(stats.getHealth())
                    + com.wartec.wartecmod.port.uav.UavText.ui("   BLAST ") + String.format(Locale.US, "%.1f",
                            stats.getBlastStrength()), 172, 185, light);
            label(com.wartec.wartecmod.port.uav.UavText.ui("STATUS: ") + status(stats), 13, 194,
                    stats.isValid() ? 0x55EE99 : 0xFF7777);
        }
        label(com.wartec.wartecmod.port.uav.UavText.ui("PLAYER INVENTORY"), 71, 203, GuiTheme.MUTED);
    }

    private static String number(int value) {
        return String.format(Locale.US, "%,d", value);
    }
    private static String decimalTemplate(String english,int precision) {
        // Minecraft Locale rewrites numeric format specifiers to %s while loading lang files.
        return com.wartec.wartecmod.port.uav.UavText.ui(english).replace("%s","%."+precision+"f");
    }

    private static String airframeLabel(UavStats stats) {
        switch (stats.getAirframe()) {
            case ONE_WAY: return com.wartec.wartecmod.port.uav.UavText.ui("ONE-WAY");
            case RECON: return com.wartec.wartecmod.port.uav.UavText.ui("REUSABLE RECON");
            case STRIKE: return com.wartec.wartecmod.port.uav.UavText.ui("REUSABLE STRIKE");
            default: return com.wartec.wartecmod.port.uav.UavText.ui("UNKNOWN");
        }
    }

    private static String status(UavStats stats) {
        if (stats.isValid()) return com.wartec.wartecmod.port.uav.UavText.ui("READY TO ASSEMBLE");
        String error = stats.getErrors().isEmpty()
                ? "invalid_build" : stats.getErrors().get(0);
        if (error.startsWith("missing_")) {
            String component=net.minecraft.client.resources.I18n.format("uav.slot."+error.substring("missing_".length()));
            return com.wartec.wartecmod.port.uav.UavText.ui("MISSING ") + component.toUpperCase(Locale.ROOT);
        }
        if ("overweight".equals(error)) return com.wartec.wartecmod.port.uav.UavText.ui("OVER MAXIMUM MASS");
        if ("insufficient_thrust".equals(error)) return com.wartec.wartecmod.port.uav.UavText.ui("ENGINE TOO WEAK");
        if (error.startsWith("incompatible_")) return com.wartec.wartecmod.port.uav.UavText.ui("INCOMPATIBLE COMPONENT");
        return com.wartec.wartecmod.port.uav.UavText.ui("INVALID COMPONENT SET");
    }

    private static int massColor(UavStats stats) {
        return stats.getMass() > stats.getMaximumMass()
                ? 0xFF7777 : 0xD8DFD8;
    }
    private void label(String text,int x,int y,int color) {
        int width=y>=149 && y<194?(x<170?153:120):xSize-x-10;
        if(fontRenderer.getStringWidth(text)>width) {
            clippedText.add(text);clippedBounds.add(new int[]{x,y,width});
        }
        GuiTheme.text(fontRenderer,text,x,y,width,color);
    }
}
