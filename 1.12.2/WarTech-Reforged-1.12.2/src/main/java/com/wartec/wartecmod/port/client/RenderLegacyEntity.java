package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.entity.EntityWarTechBase;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

public final class RenderLegacyEntity<T extends EntityWarTechBase> extends Render<T> {
    private static final ResourceLocation FALLBACK =
            new ResourceLocation("wartecmod", "textures/models/storm_shadow/storm_shadow.png");

    public RenderLegacyEntity(RenderManager manager) {
        super(manager);
        this.shadowSize = 0.8F;
    }

    @Override
    public void doRender(T entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (!RemoteControlClient.shouldHideControlledEntity(entity)) {
            LegacyRenderLibrary.renderEntity(entity, x, y, z, partialTicks);
        }
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(T entity) {
        return FALLBACK;
    }
}
