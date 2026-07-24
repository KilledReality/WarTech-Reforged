package com.wartec.wartecmod.compat.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

/** Small player-facing selector; commands are handled authoritatively by the server. */
public final class GuiIffTeamSelector extends GuiScreen {
    private static final String[] TEAMS = {
        "alpha", "bravo", "charlie", "delta"
    };

    public static void open() {
        Minecraft.func_71410_x().func_147108_a(new GuiIffTeamSelector());
    }

    @Override
    public void func_73866_w_() {
        field_146292_n.clear();
        int left = field_146294_l / 2 - 106;
        int top = field_146295_m / 2 - 54;
        for (int index = 0; index < TEAMS.length; ++index) {
            int x = left + (index % 2) * 108;
            int y = top + (index / 2) * 24;
            field_146292_n.add(new GuiButton(index, x, y, 104, 20,
                    TEAMS[index].toUpperCase()));
        }
        field_146292_n.add(new GuiButton(4, left, top + 52, 104, 20,
                "PERSONAL"));
        field_146292_n.add(new GuiButton(5, left + 108, top + 52, 104, 20,
                "STATUS"));
    }

    @Override
    protected void func_146284_a(GuiButton button) {
        if (!button.field_146124_l) {
            return;
        }
        String argument = button.field_146127_k < TEAMS.length
                ? TEAMS[button.field_146127_k]
                : button.field_146127_k == 4 ? "personal" : "status";
        sendChat("!wtteam " + argument);
        Minecraft.func_71410_x().func_147108_a(null);
    }

    @Override
    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        func_146276_q_();
        int left = field_146294_l / 2 - 116;
        int top = field_146295_m / 2 - 82;
        Gui.func_73734_a(left, top, left + 232, top + 112, 0xE0181E1B);
        Gui.func_73734_a(left + 3, top + 3, left + 229, top + 109,
                0xE02F3832);
        field_146289_q.func_78276_b("WARTECH IFF NETWORK",
                left + 54, top + 10, 0x7CFF91);
        field_146289_q.func_78276_b("Select a persistent friendly network",
                left + 21, top + 24, 0xE8E8E0);
        super.func_73863_a(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean func_73868_f() {
        return false;
    }

    private static void sendChat(String message) {
        try {
            Minecraft minecraft = Minecraft.func_71410_x();
            Field playerField = Minecraft.class.getField("field_71439_g");
            Object player = playerField.get(minecraft);
            Method send = player.getClass().getMethod(
                    "func_71165_d", String.class);
            send.invoke(player, message);
        } catch (Throwable ignored) {
        }
    }
}
