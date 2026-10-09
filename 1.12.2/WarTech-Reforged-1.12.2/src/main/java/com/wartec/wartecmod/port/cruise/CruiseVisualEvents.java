package com.wartec.wartecmod.port.cruise;

import com.wartec.wartecmod.port.network.CruiseVisualEventMessage;
import com.wartec.wartecmod.port.network.WarTechNetwork;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.network.NetworkRegistry;

public final class CruiseVisualEvents {
    private CruiseVisualEvents() { }
    public static void emit(Entity source,CruiseBuild build,CruiseVisuals.Event event,boolean reduced) {
        if(source.world==null || source.world.isRemote) return;
        CruiseVisualEventMessage message=new CruiseVisualEventMessage(build,event,source.getPositionVector(),
            new Vec3d(source.motionX,source.motionY,source.motionZ),source.rotationYaw,source.rotationPitch,
            source.dimension,source.world.rand.nextInt(),reduced);
        if(message.valid) WarTechNetwork.CHANNEL.sendToAllAround(message,new NetworkRegistry.TargetPoint(
            source.dimension,source.posX,source.posY,source.posZ,event==CruiseVisuals.Event.IMPACT?240:160));
    }
}
