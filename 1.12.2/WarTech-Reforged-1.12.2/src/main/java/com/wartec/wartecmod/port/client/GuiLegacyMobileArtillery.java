package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.entity.EntityWarTechGroundVehicle;
import com.wartec.wartecmod.port.gui.ContainerLegacyEntity;
import com.wartec.wartecmod.port.network.ArtilleryWhitelistMessage;
import com.wartec.wartecmod.port.network.WarTechNetwork;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public final class GuiLegacyMobileArtillery extends GuiContainer {
    private static final ResourceLocation GREG = new ResourceLocation(
            "wartecmod", "textures/gui/weapon/gui_turret_arty.png");
    private static final ResourceLocation HENRY = new ResourceLocation(
            "wartecmod", "textures/gui/weapon/gui_turret_himars.png");

    private final EntityWarTechGroundVehicle artillery;
    private GuiTextField whitelistField;
    private int whitelistIndex;

    public GuiLegacyMobileArtillery(InventoryPlayer inventory,
            EntityWarTechGroundVehicle artillery) {
        super(new ContainerLegacyEntity(inventory, artillery,
                ContainerLegacyEntity.Layout.ARTILLERY));
        this.artillery = artillery;
        xSize = 176;
        ySize = 222;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        whitelistField = new GuiTextField(0, fontRenderer,
                guiLeft + 10, guiTop + 65, 50, 14);
        whitelistField.setTextColor(-1);
        whitelistField.setDisabledTextColour(-1);
        whitelistField.setEnableBackgroundDrawing(false);
        whitelistField.setMaxStringLength(25);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks,
            int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(
                artillery.getVisualVariant() == 2 ? HENRY : GREG);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

        int height = Math.min(53, Math.max(0,
                53 * artillery.getLegacyPower()
                        / Math.max(1, artillery.getEnergyCapacity())));
        drawTexturedModalRect(guiLeft + 152, guiTop + 97 - height,
                194, 52 - height, 16, height);
        if (artillery.isLegacyEnabled()) {
            drawTexturedModalRect(guiLeft + 115, guiTop + 26,
                    176, 40, 18, 18);
        }
        drawTargetToggle(8, 0, artillery.targetsArtilleryPlayers());
        drawTargetToggle(22, 10, artillery.targetsArtilleryAnimals());
        drawTargetToggle(36, 20, artillery.targetsArtilleryMobs());
        drawTargetToggle(50, 30, artillery.targetsArtilleryMachines());
        int mode = artillery.getLegacyFireMode();
        if (artillery.getVisualVariant() == 1) {
            if (mode == 1) {
                drawTexturedModalRect(guiLeft + 151, guiTop + 16,
                        210, 0, 18, 18);
            } else if (mode == 2) {
                drawTexturedModalRect(guiLeft + 151, guiTop + 16,
                        210, 18, 18, 18);
            }
        } else if (mode == 1) {
            drawTexturedModalRect(guiLeft + 151, guiTop + 16,
                    210, 0, 18, 18);
        }
        drawHoverOverlay(mouseX, mouseY, 7, 80, 176, 58);
        drawHoverOverlay(mouseX, mouseY, 43, 80, 194, 58);
        drawHoverOverlay(mouseX, mouseY, 7, 98, 176, 76);
        drawHoverOverlay(mouseX, mouseY, 43, 98, 194, 76);
    }

    private void drawTargetToggle(int x, int textureY, boolean active) {
        if (active) {
            drawTexturedModalRect(guiLeft + x, guiTop + 30,
                    176, textureY, 10, 10);
        }
    }

    private void drawHoverOverlay(int mouseX, int mouseY,
            int x, int y, int textureX, int textureY) {
        if (inside(mouseX, mouseY, x, y, 18, 18)) {
            drawTexturedModalRect(guiLeft + x, guiTop + y,
                    textureX, textureY, 18, 18);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        whitelistField.drawTextBox();
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = I18n.format(artillery.getVisualVariant() == 2
                ? "container.turretHIMARS" : "container.turretArty");
        title=GuiTheme.clip(fontRenderer,title,xSize-16);
        fontRenderer.drawString(title,
                xSize / 2 - fontRenderer.getStringWidth(title) / 2,
                6, 0x404040);
        fontRenderer.drawString(I18n.format("container.inventory"),
                8, ySize - 94, 0x404040);
        List<String> names = artillery.getArtilleryWhitelist();
        if (!names.isEmpty()) {
            whitelistIndex = Math.min(whitelistIndex, names.size() - 1);
            GL11.glPushMatrix();
            GL11.glScalef(0.5F, 0.5F, 1.0F);
            GuiTheme.text(fontRenderer,names.get(whitelistIndex),24,102,280,0x00FF00);
            GL11.glPopMatrix();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button)
            throws IOException {
        super.mouseClicked(mouseX, mouseY, button);
        whitelistField.mouseClicked(mouseX, mouseY, button);
        if (inside(mouseX, mouseY, 115, 26, 18, 18)) {
            sendAction(0);
        } else if (inside(mouseX, mouseY, 8, 30, 10, 10)) {
            sendAction(1);
        } else if (inside(mouseX, mouseY, 22, 30, 10, 10)) {
            sendAction(2);
        } else if (inside(mouseX, mouseY, 36, 30, 10, 10)) {
            sendAction(3);
        } else if (inside(mouseX, mouseY, 50, 30, 10, 10)) {
            sendAction(4);
        } else if (inside(mouseX, mouseY, 151, 16, 18, 18)) {
            sendAction(5);
        } else if (inside(mouseX, mouseY, 7, 80, 18, 18)) {
            cycleWhitelist(-1);
        } else if (inside(mouseX, mouseY, 43, 80, 18, 18)) {
            cycleWhitelist(1);
        } else if (inside(mouseX, mouseY, 7, 98, 18, 18)
                && !whitelistField.getText().trim().isEmpty()) {
            WarTechNetwork.CHANNEL.sendToServer(new ArtilleryWhitelistMessage(
                    artillery.getEntityId(), whitelistField.getText()));
            whitelistField.setText("");
            click();
        } else if (inside(mouseX, mouseY, 43, 98, 18, 18)
                && !artillery.getArtilleryWhitelist().isEmpty()) {
            WarTechNetwork.CHANNEL.sendToServer(new ArtilleryWhitelistMessage(
                    artillery.getEntityId(), whitelistIndex));
            click();
        }
    }

    private void sendAction(int action) {
        mc.playerController.sendEnchantPacket(inventorySlots.windowId, action);
        click();
    }

    private void cycleWhitelist(int direction) {
        int count = artillery.getArtilleryWhitelist().size();
        if (count > 0) {
            whitelistIndex = (whitelistIndex + direction + count) % count;
        }
        click();
    }

    private void click() {
        mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(
                SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private boolean inside(int mouseX, int mouseY,
            int x, int y, int width, int height) {
        return mouseX >= guiLeft + x && mouseX < guiLeft + x + width
                && mouseY >= guiTop + y && mouseY < guiTop + y + height;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (!whitelistField.textboxKeyTyped(typedChar, keyCode)) {
            super.keyTyped(typedChar, keyCode);
        }
    }
}
