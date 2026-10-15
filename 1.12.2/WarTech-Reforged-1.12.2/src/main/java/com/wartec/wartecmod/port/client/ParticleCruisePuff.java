package com.wartec.wartecmod.port.client;

import net.minecraft.client.particle.Particle;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Vanilla translucent smoke atlas, smooth fade/growth, bounded by CruiseEffects. */
final class ParticleCruisePuff extends Particle {
    private final float size,r,g,b;
    private final boolean hot;
    private final boolean strategic;
    private final int generation;
    private boolean released;
    ParticleCruisePuff(World world,Vec3d p,Vec3d velocity,float size,int life,float r,float g,float b,boolean hot,int generation) {
        this(world,p,velocity,size,life,r,g,b,hot,generation,false);
    }
    ParticleCruisePuff(World world,Vec3d p,Vec3d velocity,float size,int life,float r,float g,float b,boolean hot,int generation,boolean strategic) {
        super(world,p.x,p.y,p.z,0,0,0);
        motionX=velocity.x;motionY=velocity.y;motionZ=velocity.z;
        this.size=size;this.r=r;this.g=g;this.b=b;this.hot=hot;this.generation=generation;this.strategic=strategic;
        particleScale=size;particleMaxAge=life;particleAlpha=0;canCollide=false;
        particleRed=r;particleGreen=g;particleBlue=b;
        if(strategic) setParticleTexture(net.minecraft.client.Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("wartecmod:particle/strategic_smoke"));
        else setParticleTextureIndex(7);
    }
    @Override public void onUpdate() {
        prevPosX=posX;prevPosY=posY;prevPosZ=posZ;
        if(generation!=CruiseEffects.generation() || ++particleAge>=particleMaxAge) { setExpired();return; }
        float t=particleAge/(float)particleMaxAge;
        particleScale=size*(.65F+1.25F*t);particleAlpha=Math.min(1,particleAge/3F)*(1-t)*.62F;
        float cooling=hot && r>.8F?Math.min(1,particleAge/12F):0;
        particleRed=r+(0.38F-r)*cooling;particleGreen=g+(0.34F-g)*cooling;particleBlue=b+(0.30F-b)*cooling;
        if(!strategic) setParticleTextureIndex(Math.min(7,(int)((1-t)*8)));
        posX+=motionX;posY+=motionY;posZ+=motionZ;
        motionX*=.98;motionZ*=.98;motionY=motionY*.985+.0015;
    }
    @Override public int getFXLayer() { return strategic?1:0; }
    @Override public boolean shouldDisableDepth() { return strategic || super.shouldDisableDepth(); }
    @Override public int getBrightnessForRender(float partial) {
        // The brief pale gas burst must read as white even before a world's lightmap updates.
        if(strategic && !hot && r>.9F && g>.9F && b>.9F) return 15728880;
        if(hot && particleAge<10) return 15728880;
        int light=super.getBrightnessForRender(partial);
        return strategic?(light&0xFFFF0000)|Math.max(light&0xFFFF,112):light;
    }
    @Override public void setExpired() {
        super.setExpired();if(!released) { released=true;CruiseEffects.releaseParticle(generation); }
    }
}
