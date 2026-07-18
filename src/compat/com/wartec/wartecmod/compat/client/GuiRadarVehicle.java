package com.wartec.wartecmod.compat.client;

import com.wartec.wartecmod.compat.ContainerRadarVehicle;
import com.wartec.wartecmod.compat.IRadarGuiTarget;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public final class GuiRadarVehicle extends GuiContainer {
    private static final ResourceLocation HBM_RADAR = new ResourceLocation(
            "hbm", "textures/gui/machine/gui_radar_nt.png");
    private final IRadarGuiTarget radar;

    public GuiRadarVehicle(InventoryPlayer inventory, IRadarGuiTarget radar) {
        super(new ContainerRadarVehicle(inventory, radar));
        this.radar = radar;
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
        Gui.func_73734_a(left, top, left + 256, top + 222, 0xFF8B8B82);
        Gui.func_73734_a(left + 4, top + 4, left + 136, top + 136, 0xFF242824);
        Gui.func_73734_a(left + 140, top + 4, left + 252, top + 136, 0xFF2E352F);
        Gui.func_73734_a(left + 4, top + 136, left + 166, top + 218, 0xFF77776F);

        field_146297_k.field_71446_o.func_110577_a(HBM_RADAR);
        GL11.glPushMatrix();
        GL11.glTranslatef(left + 5.0F, top + 5.0F, 0.0F);
        GL11.glScalef(0.62F, 0.62F, 1.0F);
        func_73729_b(0, 0, 5, 15, 204, 204);
        GL11.glPopMatrix();

        int capacity = Math.max(1, radar.wartecGetCapacity());
        int powerHeight = (int) Math.round(58.0D
                * Math.min(1.0D, radar.wartecGetPower() / (double) capacity));
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

    private void drawSlot(int x, int y) {
        Gui.func_73734_a(x, y, x + 18, y + 18, 0xFF3A3A36);
        Gui.func_73734_a(x + 1, y + 1, x + 17, y + 17, 0xFF9D9D91);
    }

    @Override
    protected void func_146979_b(int mouseX, int mouseY) {
        int green = 0x62FF75;
        int white = 0xE6E6DF;
        field_146289_q.func_78276_b(radar.wartecGetRadarName(), 142, 9, white);
        String status = radar.wartecIsOperational() ? "ONLINE"
                : radar.wartecIsEnabled() ? "NO POWER" : "STANDBY";
        field_146289_q.func_78276_b(status, 142, 23,
                radar.wartecIsOperational() ? green : 0xFFB34F);
        field_146289_q.func_78276_b("CONTACTS  " + radar.wartecGetContacts(),
                142, 43, green);
        field_146289_q.func_78276_b("RANGE     " + radar.wartecGetRange(),
                142, 55, white);
        field_146289_q.func_78276_b("CEILING   " + radar.wartecGetCeiling(),
                142, 67, white);
        int percent = (int) Math.round(100.0D * radar.wartecGetPower()
                / Math.max(1, radar.wartecGetCapacity()));
        field_146289_q.func_78276_b("POWER " + percent + "%", 142, 83, white);
        field_146289_q.func_78276_b("BATTERY", 207, 96, white);
        field_146289_q.func_78276_b("INVENTORY", 8, 129, white);
        for (Object value : field_146292_n) {
            if (value instanceof GuiButton && ((GuiButton) value).field_146127_k == 0) {
                ((GuiButton) value).field_146126_j = radar.wartecIsEnabled()
                        ? "DISABLE" : "ENABLE";
            }
        }
    }
}
