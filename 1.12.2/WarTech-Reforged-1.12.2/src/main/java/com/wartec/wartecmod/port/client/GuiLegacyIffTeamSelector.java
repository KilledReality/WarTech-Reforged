package com.wartec.wartecmod.port.client;

import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public final class GuiLegacyIffTeamSelector extends GuiScreen {
    private static final String[] TEAMS = {"alpha", "bravo", "charlie", "delta"};

    @Override
    public void initGui() {
        buttonList.clear();
        int left = width / 2 - 106;
        int top = height / 2 - 24;
        for (int index = 0; index < TEAMS.length; ++index) {
            int x = left + index % 2 * 108;
            int y = top + index / 2 * 24;
            buttonList.add(new WarTechGuiButton(index, x, y, 104, 20,
                    TEAMS[index].toUpperCase()));
        }
        buttonList.add(new WarTechGuiButton(4, left, top + 52, 104, 20, GuiTheme.tr("iff.personal")));
        buttonList.add(new WarTechGuiButton(5, left + 108, top + 52, 104, 20, GuiTheme.tr("iff.status")));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (!button.enabled) return;
        if (button.id < TEAMS.length) {
            mc.player.sendChatMessage("/wtteam join " + TEAMS[button.id]);
        } else if (button.id == 4) {
            mc.player.sendChatMessage("/wtteam clear");
        } else {
            mc.player.sendChatMessage("/wtteam status");
        }
        Minecraft.getMinecraft().displayGuiScreen(null);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int left = width / 2 - 116;
        int top = height / 2 - 72;
        GuiTheme.frame(left,top,232,128);
        drawCenteredString(fontRenderer,GuiTheme.clip(fontRenderer,GuiTheme.tr("iff.title"),212),width/2,top+8,GuiTheme.TEXT);
        drawCenteredString(fontRenderer,GuiTheme.clip(fontRenderer,GuiTheme.tr("iff.subtitle"),212),width/2,top+30,GuiTheme.MUTED);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
