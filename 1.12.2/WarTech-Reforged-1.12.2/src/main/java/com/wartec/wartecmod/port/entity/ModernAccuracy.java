package com.wartec.wartecmod.port.entity;

import java.util.Random;
import java.util.UUID;
import net.minecraft.util.math.Vec3d;

/** Deterministic CEP sample; nominal target remains intact for radar and save data. */
public final class ModernAccuracy {
    private ModernAccuracy() { }
    public static Vec3d aim(ModernMissileProfile profile,Vec3d start,Vec3d target,UUID id,boolean exact) {
        if(exact) return target;
        Random random=new Random(id.getMostSignificantBits()^id.getLeastSignificantBits());
        double sigma=profile.cep/Math.sqrt(2*Math.log(2));
        double dx=target.x-start.x+random.nextGaussian()*sigma,dz=target.z-start.z+random.nextGaussian()*sigma;
        double distance=Math.hypot(dx,dz),bounded=Math.max(profile.minimumRange,Math.min(profile.commandRange,distance));
        double factor=bounded/Math.max(1e-6,distance);
        return new Vec3d(start.x+dx*factor,target.y,start.z+dz*factor);
    }
}
