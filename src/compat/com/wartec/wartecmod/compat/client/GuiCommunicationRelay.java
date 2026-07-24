package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.compat.ContainerCommunicationRelay;
import com.wartec.wartecmod.compat.TileEntityCommunicationRelay;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;

public final class GuiCommunicationRelay extends GuiContainer {
    private final TileEntityCommunicationRelay relay;

    public GuiCommunicationRelay(InventoryPlayer inventory,
            TileEntityCommunicationRelay relay) {
        super(new ContainerCommunicationRelay(inventory, relay));
        this.relay = relay;
        field_146999_f = 256;
        field_147000_g = 222;
    }

    @Override
    public void func_73866_w_() {
        super.func_73866_w_();
        field_146292_n.add(new GuiButton(0, field_147003_i + 142,
                field_147009_r + 108, 72, 20, "TOGGLE"));
    }

    @Override
    protected void func_146284_a(GuiButton button) {
        if (button.field_146124_l && button.field_146127_k == 0) {
            field_146297_k.field_71442_b.func_78756_a(
                    field_147002_h.field_75152_c, 0);
        }
    }

    @Override
    protected void func_146976_a(float partialTicks, int mouseX, int mouseY) {
        int left = field_147003_i;
        int top = field_147009_r;
        Gui.func_73734_a(left, top, left + 256, top + 222, 0xFF77776F);
        Gui.func_73734_a(left + 4, top + 4, left + 252, top + 136, 0xFF151B19);
        Gui.func_73734_a(left + 8, top + 8, left + 136, top + 132, 0xFF23322C);
        Gui.func_73734_a(left + 140, top + 8, left + 248, top + 104, 0xFF2E352F);
        drawSignal(left + 20, top + 29);
        int powerHeight = (int) Math.round(58.0D * relay.getPower()
                / Math.max(1, TileEntityCommunicationRelay.ENERGY_CAPACITY));
        Gui.func_73734_a(left + 222, top + 34, left + 238, top + 94, 0xFF101510);
        Gui.func_73734_a(left + 224, top + 92 - powerHeight,
                left + 236, top + 92, 0xFF28D75C);
        drawSlot(left + 221, top + 104);
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                drawSlot(left + 7 + column * 18, top + 139 + row * 18);
            }
        }
        for (int column = 0; column < 9; ++column) {
            drawSlot(left + 7 + column * 18, top + 197);
        }
    }

    private void drawSignal(int x, int y) {
        int green = relay.isOnline() ? 0xFF55F278 : 0xFF53645A;
        for (int ring = 0; ring < 4; ++ring) {
            int radius = 13 + ring * 12;
            Gui.func_73734_a(x + 48 - radius, y + 48,
                    x + 49 - radius, y + 50, green);
            Gui.func_73734_a(x + 48 + radius, y + 48,
                    x + 49 + radius, y + 50, green);
            Gui.func_73734_a(x + 48, y + 48 - radius,
                    x + 50, y + 49 - radius, green);
            Gui.func_73734_a(x + 48, y + 48 + radius,
                    x + 50, y + 49 + radius, green);
        }
        Gui.func_73734_a(x + 46, y + 46, x + 52, y + 52, 0xFFFFFFFF);
        Gui.func_73734_a(x + 48, y + 22, x + 50, y + 77, green);
    }

    private void drawSlot(int x, int y) {
        Gui.func_73734_a(x, y, x + 18, y + 18, 0xFF3A3A36);
        Gui.func_73734_a(x + 1, y + 1, x + 17, y + 17, 0xFF9D9D91);
    }

    @Override
    protected void func_146979_b(int mouseX, int mouseY) {
        int green = 0x62FF75;
        int white = 0xE6E6DF;
        field_146289_q.func_78276_b("COMMUNICATION MAST", 142, 12, white);
        String status = relay.isOnline() ? "ONLINE"
                : relay.isEnabled() ? "NO POWER" : "STANDBY";
        field_146289_q.func_78276_b(status, 142, 26,
                relay.isOnline() ? green : 0xFFB34F);
        field_146289_q.func_78276_b("RANGE   "
                + TileEntityCommunicationRelay.LINK_RANGE, 142, 46, white);
        field_146289_q.func_78276_b("LINKS   "
                + relay.getLinkedRelayCount(), 142, 59, green);
        int percent = (int) Math.round(100.0D * relay.getPower()
                / TileEntityCommunicationRelay.ENERGY_CAPACITY);
        field_146289_q.func_78276_b("POWER   " + percent + "%", 142, 72, white);
        field_146289_q.func_78276_b("LOAD    20 HE/t", 142, 85, white);
        field_146289_q.func_78276_b("BATTERY", 207, 96, white);
        field_146289_q.func_78276_b("INVENTORY", 8, 129, white);
        for (Object value : field_146292_n) {
            if (value instanceof GuiButton
                    && ((GuiButton) value).field_146127_k == 0) {
                ((GuiButton) value).field_146126_j =
                        relay.isEnabled() ? "DISABLE" : "ENABLE";
            }
        }
    }
}
