package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.entity.EntitySatelliteMissileNuclear;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

public final class RenderSatelliteMissileNuclear
        extends Render<EntitySatelliteMissileNuclear> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            "wartecmod",
            "textures/models/entity_sat_nuclear_missile_tex.png");

    public RenderSatelliteMissileNuclear(RenderManager manager) {
        super(manager);
    }

    @Override
    public void doRender(EntitySatelliteMissileNuclear entity, double x,
            double y, double z, float entityYaw, float partialTicks) {
        LegacyRenderLibrary.renderSatelliteNuclear(
                entity, x, y, z, partialTicks);
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(
            EntitySatelliteMissileNuclear entity) {
        return TEXTURE;
    }
}
