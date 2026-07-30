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
        int top = height / 2 - 54;
        for (int index = 0; index < TEAMS.length; ++index) {
            int x = left + index % 2 * 108;
            int y = top + index / 2 * 24;
            buttonList.add(new GuiButton(index, x, y, 104, 20,
                    TEAMS[index].toUpperCase()));
        }
        buttonList.add(new GuiButton(4, left, top + 52, 104, 20, "PERSONAL"));
        buttonList.add(new GuiButton(5, left + 108, top + 52, 104, 20, "STATUS"));
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
        int top = height / 2 - 82;
        Gui.drawRect(left, top, left + 232, top + 112, 0xE0181E1B);
        Gui.drawRect(left + 3, top + 3, left + 229, top + 109, 0xE02F3832);
        fontRenderer.drawString("WARTECH IFF NETWORK", left + 54, top + 10, 0x7CFF91);
        fontRenderer.drawString("Select a persistent friendly network",
                left + 21, top + 24, 0xE8E8E0);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
