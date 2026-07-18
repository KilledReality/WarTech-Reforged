package net.minecraft.client.gui.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.inventory.Container;

public abstract class GuiContainer extends GuiScreen {
    protected int field_146999_f = 176;
    protected int field_147000_g = 166;
    protected int field_147003_i;
    protected int field_147009_r;
    protected Minecraft field_146297_k;
    protected Container field_147002_h;

    public GuiContainer(Container container) { field_147002_h = container; }
    protected abstract void func_146976_a(float partialTicks, int mouseX, int mouseY);
    protected void func_146979_b(int mouseX, int mouseY) {}
    protected void func_73729_b(int x, int y, int u, int v, int width, int height) {}
}
