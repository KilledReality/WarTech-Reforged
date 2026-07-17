package net.minecraft.client.renderer.entity;

import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

public abstract class Render {
    public abstract void func_76986_a(Entity entity, double x, double y, double z,
            float yaw, float partialTicks);
    protected abstract ResourceLocation func_110775_a(Entity entity);
    protected void func_110776_a(ResourceLocation texture) {}
}
