package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.compat.ContainerTu95Bomber;
import com.wartec.wartecmod.entity.missile.EntityTu95Bomber;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;

public final class GuiTu95Bomber extends GuiContainer {
    private final EntityTu95Bomber bomber;

    public GuiTu95Bomber(InventoryPlayer inventory, EntityTu95Bomber bomber) {
        super(new ContainerTu95Bomber(inventory, bomber));
        this.bomber = bomber;
        field_146999_f = 236;
        field_147000_g = 221;
    }

    @Override public void func_73866_w_() {
        super.func_73866_w_();
        field_146292_n.add(new GuiButton(0, field_147003_i + 12,
                field_147009_r + 91, 112, 20, "LAUNCH MISSION"));
        field_146292_n.add(new GuiButton(1, field_147003_i + 128,
                field_147009_r + 91, 46, 20, "LAST"));
        field_146292_n.add(new GuiButton(2, field_147003_i + 178,
                field_147009_r + 91, 46, 20, "ALL"));
    }

    @Override protected void func_146284_a(GuiButton button) {
        if (button.field_146124_l && button.field_146127_k >= 0
                && button.field_146127_k <= 2) {
            field_146297_k.field_71442_b.func_78756_a(
                    field_147002_h.field_75152_c, button.field_146127_k);
        }
    }

    @Override protected void func_146976_a(float partialTicks, int mouseX, int mouseY) {
        int left = field_147003_i;
        int top = field_147009_r;
        Gui.func_73734_a(left, top, left + 236, top + 221, 0xFF747778);
        Gui.func_73734_a(left + 5, top + 5, left + 231, top + 129, 0xFF20272A);
        Gui.func_73734_a(left + 5, top + 131, left + 231, top + 216, 0xFF77776F);
        Gui.func_73734_a(left + 16, top + 31, left + 220, top + 80, 0xFF343E41);
        Gui.func_73734_a(left + 22, top + 47, left + 214, top + 50, 0xFF98AAAC);
        for (int slot = 0; slot < 6; ++slot) drawSlot(left + 43 + slot * 21, top + 56);
        drawSlot(left + 177, top + 56);
        drawSlot(left + 204, top + 56);
        int powerWidth = (int) Math.round(168.0D * bomber.getPower()
                / EntityTu95Bomber.ENERGY_CAPACITY);
        Gui.func_73734_a(left + 24, top + 118, left + 194, top + 125, 0xFF101719);
        Gui.func_73734_a(left + 25, top + 119, left + 25 + powerWidth,
                top + 124, 0xFF43D47B);
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                drawSlot(left + 23 + column * 18, top + 138 + row * 18);
            }
        }
        for (int column = 0; column < 9; ++column) drawSlot(left + 23 + column * 18, top + 196);
    }

    private void drawSlot(int x, int y) {
        Gui.func_73734_a(x, y, x + 18, y + 18, 0xFF393B3A);
        Gui.func_73734_a(x + 1, y + 1, x + 17, y + 17, 0xFF9C9D96);
    }

    @Override protected void func_146979_b(int mouseX, int mouseY) {
        int white = 0xE7ECEC;
        int cyan = 0x77E3ED;
        field_146289_q.func_78276_b("TU-95 STRATEGIC AVIATION CONTROL", 15, 10, white);
        field_146289_q.func_78276_b("STATUS: " + bomber.getStateName(), 15, 21,
                bomber.isReady() ? 0x65F28A : cyan);
        field_146289_q.func_78276_b("MISSION: 8000", 151, 21, cyan);
        field_146289_q.func_78276_b("KH-555 / FAB-5000 / KAB-3000", 15, 31, 0xF1C96B);
        String target = bomber.hasTarget()
                ? bomber.getTargetX() + " / " + bomber.getTargetY() + " / " + bomber.getTargetZ()
                : "NOT ASSIGNED";
        String queue = bomber.hasTarget() ? (bomber.getTargetIndex() + 1)
                + "/" + bomber.getTargetCount() : "0/" + EntityTu95Bomber.MAX_TARGETS;
        field_146289_q.func_78276_b("TARGET " + queue + ": " + target, 15, 81, white);
        field_146289_q.func_78276_b("LTC", 176, 47, 0xF1C96B);
        field_146289_q.func_78276_b("BAT", 203, 47, white);
        field_146289_q.func_78276_b("POWER " + bomber.getPower() + "/"
                + EntityTu95Bomber.ENERGY_CAPACITY, 24, 113, white);
        field_146289_q.func_78276_b("HP " + bomber.getHealthPercent() + "%", 164, 113, white);
        field_146289_q.func_78276_b("INVENTORY", 24, 130, white);
        for (Object value : field_146292_n) {
            if (value instanceof GuiButton && ((GuiButton) value).field_146127_k == 0) {
                ((GuiButton) value).field_146126_j = bomber.isReady()
                        ? "LAUNCH MISSION" : "RETURN TO BASE";
            }
        }
    }
}
