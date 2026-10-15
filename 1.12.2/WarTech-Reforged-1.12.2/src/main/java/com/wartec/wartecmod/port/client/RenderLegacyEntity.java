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
        if(entity instanceof com.wartec.wartecmod.port.entity.EntityWarTechGroundVehicle) {
            com.wartec.wartecmod.port.entity.EntityWarTechGroundVehicle vehicle=(com.wartec.wartecmod.port.entity.EntityWarTechGroundVehicle)entity;
            net.minecraft.util.math.Vec3d point=vehicle.visualPosition(partialTicks);
            x+=point.x-(entity.lastTickPosX+(entity.posX-entity.lastTickPosX)*partialTicks);
            y+=point.y-(entity.lastTickPosY+(entity.posY-entity.lastTickPosY)*partialTicks);
            z+=point.z-(entity.lastTickPosZ+(entity.posZ-entity.lastTickPosZ)*partialTicks);
            if(VehicleCockpitCamera.inside(vehicle)) {
                VehicleCockpitModel.render(vehicle,x,y,z,vehicle.visualYaw(partialTicks),vehicle.visualPitch(partialTicks));return;
            }
            LegacyRenderLibrary.renderEntity(entity,x,y,z,partialTicks,vehicle.visualYaw(partialTicks),vehicle.visualPitch(partialTicks));
            super.doRender(entity,x,y,z,entityYaw,partialTicks);return;
        }
        this.shadowSize=Math.max(.25F,Math.min(4.5F,entity.width*.55F));
        if (!RemoteControlClient.shouldHideControlledEntity(entity)) {
            RemoteControlClient.RemoteRenderPose pose =
                    RemoteControlClient.resolveControlledRenderPose(
                            entity, x, y, z, partialTicks);
            LegacyRenderLibrary.renderEntity(entity, pose.x, pose.y, pose.z,
                    partialTicks, pose.yaw, pose.pitch);
        }
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(T entity) {
        return FALLBACK;
    }
}
