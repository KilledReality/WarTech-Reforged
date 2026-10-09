package com.wartec.wartecmod.port.client;

import net.minecraft.client.particle.Particle;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Vanilla translucent smoke atlas, smooth fade/growth, bounded by CruiseEffects. */
final class ParticleCruisePuff extends Particle {
    private final float size,r,g,b;
    private final boolean hot;
    private final int generation;
    private boolean released;
    ParticleCruisePuff(World world,Vec3d p,Vec3d velocity,float size,int life,float r,float g,float b,boolean hot,int generation) {
        super(world,p.x,p.y,p.z,0,0,0);
        motionX=velocity.x;motionY=velocity.y;motionZ=velocity.z;
        this.size=size;this.r=r;this.g=g;this.b=b;this.hot=hot;this.generation=generation;
        particleScale=size;particleMaxAge=life;particleAlpha=0;canCollide=false;
        particleRed=r;particleGreen=g;particleBlue=b;setParticleTextureIndex(7);
    }
    @Override public void onUpdate() {
        prevPosX=posX;prevPosY=posY;prevPosZ=posZ;
        if(generation!=CruiseEffects.generation() || ++particleAge>=particleMaxAge) { setExpired();return; }
        float t=particleAge/(float)particleMaxAge;
        particleScale=size*(.65F+1.25F*t);particleAlpha=Math.min(1,particleAge/3F)*(1-t)*.62F;
        float cooling=hot && r>.8F?Math.min(1,particleAge/12F):0;
        particleRed=r+(0.38F-r)*cooling;particleGreen=g+(0.34F-g)*cooling;particleBlue=b+(0.30F-b)*cooling;
        setParticleTextureIndex(Math.min(7,(int)((1-t)*8)));
        posX+=motionX;posY+=motionY;posZ+=motionZ;
        motionX*=.98;motionZ*=.98;motionY=motionY*.985+.0015;
    }
    @Override public int getBrightnessForRender(float partial) { return hot && particleAge<10?15728880:super.getBrightnessForRender(partial); }
    @Override public void setExpired() {
        super.setExpired();if(!released) { released=true;CruiseEffects.releaseParticle(generation); }
    }
}
