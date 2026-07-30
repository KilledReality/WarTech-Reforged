package com.wartec.wartecmod.port.client;

import com.hbm.particle.ParticleRBMKMush;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

/**
 * The dev66 particle used additive blending. NTM Extended's 1.12 renderer
 * omitted that state, making the original black atlas background opaque.
 */
public final class ParticleLegacyMushroom extends ParticleRBMKMush {
    public ParticleLegacyMushroom(World world, double x, double y, double z,
            float scale) {
        super(world, x, y, z, scale);
        particleMaxAge = 50;
    }

    @Override
    public void renderParticle(BufferBuilder buffer, Entity camera,
            float partialTicks, float rotationX, float rotationZ,
            float rotationYZ, float rotationXY, float rotationXZ) {
        boolean fog = GL11.glIsEnabled(GL11.GL_FOG);
        try {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.0F);
            GL11.glDisable(GL11.GL_FOG);
            super.renderParticle(buffer, camera, partialTicks,
                    rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
        } finally {
            if (fog) {
                GL11.glEnable(GL11.GL_FOG);
            }
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA,
                    GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
            GlStateManager.depthMask(true);
        }
    }
}
