package net.minecraft.client;

import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.EntityLivingBase;

public class Minecraft {
    private static Minecraft stubInstance;
    public TextureManager field_71446_o;
    public FontRenderer field_71466_p;
    public PlayerControllerMP field_71442_b;
    public WorldClient field_71441_e;
    public EntityClientPlayerMP field_71439_g;
    public EntityLivingBase field_71451_h;
    public GameSettings field_71474_y;
    public GuiScreen field_71462_r;
    public Minecraft() { stubInstance = this; }
    public static Minecraft func_71410_x() { return stubInstance; }
    public void func_147108_a(GuiScreen screen) {}
}
