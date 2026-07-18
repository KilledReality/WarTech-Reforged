package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.compat.ContainerMq9Drone;
import com.wartec.wartecmod.compat.ItemMq9Payload;
import com.wartec.wartecmod.entity.missile.EntityMq9Drone;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;

public final class GuiMq9Drone extends GuiContainer {
    private final EntityMq9Drone drone;

    public GuiMq9Drone(InventoryPlayer inventory, EntityMq9Drone drone) {
        super(new ContainerMq9Drone(inventory, drone));
        this.drone = drone;
        field_146999_f = 236;
        field_147000_g = 221;
    }

    @Override
    public void func_73866_w_() {
        super.func_73866_w_();
        field_146292_n.add(new GuiButton(0, field_147003_i + 24,
                field_147009_r + 91, 92, 20, "LAUNCH"));
        field_146292_n.add(new GuiButton(1, field_147003_i + 120,
                field_147009_r + 91, 76, 20, "WEAPON"));
        field_146292_n.add(new GuiButton(2, field_147003_i + 198,
                field_147009_r + 91, 32, 20, "CLR"));
    }

    @Override
    protected void func_146284_a(GuiButton button) {
        if (!button.field_146124_l) return;
        if (button.field_146127_k >= 0 && button.field_146127_k <= 2) {
            field_146297_k.field_71442_b.func_78756_a(
                    field_147002_h.field_75152_c, button.field_146127_k);
        }
    }

    @Override
    protected void func_146976_a(float partialTicks, int mouseX, int mouseY) {
        int left = field_147003_i;
        int top = field_147009_r;
        Gui.func_73734_a(left, top, left + 236, top + 221, 0xFF777B7C);
        Gui.func_73734_a(left + 5, top + 5, left + 231, top + 129, 0xFF20282C);
        Gui.func_73734_a(left + 5, top + 131, left + 231, top + 216, 0xFF77776F);
        Gui.func_73734_a(left + 16, top + 31, left + 220, top + 80, 0xFF343F43);
        Gui.func_73734_a(left + 22, top + 47, left + 214, top + 50, 0xFF92A7A9);
        for (int slot = 0; slot < 6; ++slot) {
            drawSlot(left + 43 + slot * 21, top + 56);
            Gui.func_73734_a(left + 51 + slot * 21, top + 39,
                    left + 54 + slot * 21, top + 57, 0xFF69787B);
        }
        drawSlot(left + 177, top + 56);
        drawSlot(left + 204, top + 56);
        int powerWidth = (int) Math.round(168.0D * drone.getPower()
                / EntityMq9Drone.ENERGY_CAPACITY);
        Gui.func_73734_a(left + 24, top + 118, left + 194, top + 125, 0xFF101719);
        Gui.func_73734_a(left + 25, top + 119, left + 25 + powerWidth,
                top + 124, 0xFF43D47B);
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                drawSlot(left + 23 + column * 18, top + 138 + row * 18);
            }
        }
        for (int column = 0; column < 9; ++column) {
            drawSlot(left + 23 + column * 18, top + 196);
        }
    }

    private void drawSlot(int x, int y) {
        Gui.func_73734_a(x, y, x + 18, y + 18, 0xFF393B3A);
        Gui.func_73734_a(x + 1, y + 1, x + 17, y + 17, 0xFF9C9D96);
    }

    @Override
    protected void func_146979_b(int mouseX, int mouseY) {
        int white = 0xE7ECEC;
        int cyan = 0x77E3ED;
        field_146289_q.func_78276_b("MQ-9 REAPER GROUND CONTROL", 15, 10, white);
        field_146289_q.func_78276_b("STATUS: " + drone.getStateName(), 15, 21,
                drone.isReady() ? 0x65F28A : cyan);
        field_146289_q.func_78276_b(ItemMq9Payload.getPayloadStatus(
                drone.getSelectedPayload()), 15, 31, 0xF1C96B);
        String target = drone.hasTarget()
                ? drone.getTargetX() + " / " + drone.getTargetY() + " / " + drone.getTargetZ()
                : "NOT ASSIGNED";
        String queue = drone.hasTarget() ? (drone.getTargetIndex() + 1)
                + "/" + drone.getTargetCount() : "0/" + EntityMq9Drone.MAX_TARGETS;
        field_146289_q.func_78276_b("TARGET " + queue + ": " + target, 15, 81, white);
        field_146289_q.func_78276_b("LTC", 176, 47, 0xF1C96B);
        field_146289_q.func_78276_b("BAT", 203, 47, white);
        field_146289_q.func_78276_b("POWER " + drone.getPower() + "/"
                + EntityMq9Drone.ENERGY_CAPACITY, 24, 113, white);
        field_146289_q.func_78276_b("HP " + drone.getHealthPercent() + "%", 164, 113, white);
        field_146289_q.func_78276_b("INVENTORY", 24, 130, white);
        for (Object value : field_146292_n) {
            if (!(value instanceof GuiButton)) continue;
            GuiButton button = (GuiButton) value;
            if (button.field_146127_k == 0) {
                button.field_146126_j = drone.isReady() ? "LAUNCH MISSION" : "RETURN TO BASE";
            } else if (button.field_146127_k == 1) {
                int type = drone.getSelectedPayload();
                button.field_146126_j = type == ItemMq9Payload.HELLFIRE ? "AGM-114"
                        : type == ItemMq9Payload.GBU12 ? "GBU-12" : "MK 82";
            }
        }
    }
}
