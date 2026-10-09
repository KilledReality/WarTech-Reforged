package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.entity.EntityCustomCruise;
import com.wartec.wartecmod.port.network.CruiseVisualEventMessage;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.Entity;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

/** Cosmetic-only client scene. Hard caps apply across all missiles and simultaneous events. */
public final class CruiseEffects {
    public static final int MAX_EFFECTS=32,MAX_PARTICLES=1600,MAX_SPAWNS_PER_TICK=128;
    private static final List<Effect> effects=new ArrayList<>();
    private static final Map<EntityCustomCruise,Vec3d> trails=new WeakHashMap<>();
    private static final List<ParticleTicket> particles=new ArrayList<>();
    private static World currentWorld;
    private static long budgetTick=Long.MIN_VALUE;
    private static long lastUpdateTick=Long.MIN_VALUE;
    private static int epoch,liveParticles,spawned;
    public static int generation() { return epoch; }
    static void releaseParticle(int generation) { if(generation==epoch) liveParticles=Math.max(0,liveParticles-1); }
    private static void context() {
        World world=Minecraft.getMinecraft().world;
        if(world!=currentWorld) {
            currentWorld=world;effects.clear();trails.clear();particles.clear();epoch++;liveParticles=0;budgetTick=lastUpdateTick=Long.MIN_VALUE;
        }
        if(world!=null && budgetTick!=world.getTotalWorldTime()) { budgetTick=world.getTotalWorldTime();spawned=0; }
    }
    private static boolean nearby(Vec3d p,double range) {
        Entity camera=Minecraft.getMinecraft().getRenderViewEntity();
        return camera!=null && camera.getPositionVector().squareDistanceTo(p)<=range*range;
    }
    private static void puff(Vec3d at,Vec3d velocity,float size,int life,float r,float g,float b,boolean hot,Random random) {
        Minecraft mc=Minecraft.getMinecraft();
        if(currentWorld==null || !nearby(at,192) || spawned>=MAX_SPAWNS_PER_TICK || liveParticles>=MAX_PARTICLES) return;
        int density=mc.gameSettings.particleSetting;
        if(density==1 && random.nextBoolean() || density==2 && random.nextInt(4)!=0) return;
        ParticleCruisePuff particle=new ParticleCruisePuff(currentWorld,at,velocity,size,life,r,g,b,hot,epoch);
        particles.add(new ParticleTicket(particle,currentWorld.getTotalWorldTime()+life+2));
        mc.effectRenderer.addEffect(particle);liveParticles++;spawned++;
    }
    public static void trail(EntityCustomCruise missile) {
        context();if(currentWorld!=missile.world) return;
        CruiseBuild build=missile.getBuild();int stage=missile.getFlightStage();
        float age=missile.getVisualFlightTicks(0);
        if(build.getAirframe()==null || !nearby(missile.getPositionVector(),160) || stage!=3 && !CruiseVisuals.burning(build,age,stage)) {
            trails.remove(missile);return;
        }
        boolean boost=stage==0 && build.get(CruiseSlot.LAUNCH)==CruisePartDefinition.LAUNCH_BOOSTER;
        Vec3d at=missile.getPositionVector().add(CruiseVisuals.worldOffset(CruiseVisuals.exhaust(build,boost),missile.rotationYaw,missile.rotationPitch));
        Vec3d previous=trails.put(missile,at);if(previous==null || previous.squareDistanceTo(at)>1024) previous=at;
        int count=Math.min(6,Math.max(boost?3:1,(int)Math.ceil(previous.distanceTo(at)/.45)));
        Vec3d backwards=CruiseFlightMath.direction(missile.rotationYaw,missile.rotationPitch).scale(boost?-.09:-.035);
        Random random=currentWorld.rand;
        for(int i=1;i<=count;i++) {
            Vec3d point=previous.add(at.subtract(previous).scale(i/(double)count));
            float jitter=boost?.07F:.025F;
            point=point.addVector((random.nextDouble()-.5)*jitter,(random.nextDouble()-.5)*jitter,(random.nextDouble()-.5)*jitter);
            boolean crash=stage==3;
            float size=boost?.75F:crash?.70F:build.get(CruiseSlot.ENGINE)==CruisePartDefinition.ENGINE_FAST?.32F:.22F;
            puff(point,backwards.addVector(0,.012,0),size,boost?65:crash?70:38,
                crash?.17F:boost?.74F:.66F,crash?.18F:boost?.72F:.69F,crash?.19F:boost?.67F:.73F,false,random);
        }
    }
    public static void receive(CruiseVisualEventMessage message) {
        context();if(currentWorld==null || !message.valid || currentWorld.provider.getDimension()!=message.dimension || !nearby(message.position,240)) return;
        // The launch kit remains functional, but no separate booster mesh is drawn.
        if(message.event==CruiseVisuals.Event.BOOSTER_DETACH) return;
        {
            if(effects.size()>=MAX_EFFECTS) effects.remove(0);
            effects.add(new Effect(message));
            if(message.event==CruiseVisuals.Event.LAUNCH && message.build.get(CruiseSlot.LAUNCH)==CruisePartDefinition.LAUNCH_AIR)
                sound(message.position,SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN,.8F,.7F);
            if(message.event==CruiseVisuals.Event.IGNITION && message.build.get(CruiseSlot.LAUNCH)==CruisePartDefinition.LAUNCH_AIR)
                sound(message.position,SoundEvents.ENTITY_BLAZE_SHOOT,.8F,.85F);
        }
    }
    private static void sound(Vec3d p,net.minecraft.util.SoundEvent sound,float volume,float pitch) {
        currentWorld.playSound(p.x,p.y,p.z,sound,SoundCategory.BLOCKS,volume,pitch,false);
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END) return;
        context();if(currentWorld==null || Minecraft.getMinecraft().isGamePaused()) return;
        if(lastUpdateTick==currentWorld.getTotalWorldTime()) return;lastUpdateTick=currentWorld.getTotalWorldTime();
        // Vanilla may evict particles without updating them; timeout still releases our budget.
        for(Iterator<ParticleTicket> it=particles.iterator();it.hasNext();) {
            ParticleTicket ticket=it.next();
            if(!ticket.particle.isAlive() || ticket.expiry<=lastUpdateTick) { ticket.particle.setExpired();it.remove(); }
        }
        for(Iterator<Effect> it=effects.iterator();it.hasNext();) { Effect e=it.next();if(++e.age>e.life) it.remove();else e.emit(); }
    }
    @SubscribeEvent public void render(RenderWorldLastEvent event) {
        context();Entity camera=Minecraft.getMinecraft().getRenderViewEntity();if(currentWorld==null || camera==null) return;
        float partial=event.getPartialTicks();
        double x=camera.lastTickPosX+(camera.posX-camera.lastTickPosX)*partial,y=camera.lastTickPosY+(camera.posY-camera.lastTickPosY)*partial,z=camera.lastTickPosZ+(camera.posZ-camera.lastTickPosZ)*partial;
        GL11.glPushMatrix();GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glTranslated(-x,-y,-z);
        GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glDisable(GL11.GL_LIGHTING);GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE);GL11.glDepthMask(false);GL11.glAlphaFunc(GL11.GL_GREATER,0);
        float lightX=OpenGlHelper.lastBrightnessX,lightY=OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,240,240);
        for(Effect e:effects) if(e.message.event==CruiseVisuals.Event.IMPACT && nearby(e.message.position,240)) e.ring(partial);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,lightX,lightY);
        GL11.glPopAttrib();GL11.glPopMatrix();
    }
    private static final class ParticleTicket {
        final ParticleCruisePuff particle;final long expiry;
        ParticleTicket(ParticleCruisePuff particle,long expiry) { this.particle=particle;this.expiry=expiry; }
    }
    private static final class Effect {
        final CruiseVisualEventMessage message;final Random random;final int life;final float radius;int age;
        Effect(CruiseVisualEventMessage m) {
            message=m;random=new Random(m.seed);life=m.event==CruiseVisuals.Event.IMPACT?48:16;
            float power=(float)m.build.get(CruiseSlot.WARHEAD).getPrimary();radius=m.reduced?4:Math.min(32,4+power*1.4F);
        }
        void emit() {
            CruiseVisualEventMessage m=message;Vec3d at=m.position;
            if(m.event==CruiseVisuals.Event.LAUNCH) {
                if(m.build.get(CruiseSlot.LAUNCH)==CruisePartDefinition.LAUNCH_AIR || age>9) return;
                BlockPos cell=new BlockPos(at);if(!currentWorld.isBlockLoaded(cell)) return;
                RayTraceResult ground=currentWorld.rayTraceBlocks(at,at.addVector(0,-14,0),false,true,false);
                if(ground==null) return;Vec3d dust=ground.hitVec.addVector(0,.10,0);
                for(int i=0;i<8;i++) {
                    double angle=random.nextDouble()*Math.PI*2,v=.08+random.nextDouble()*.20;
                    puff(dust.addVector(Math.cos(angle)*age*.14,0,Math.sin(angle)*age*.14),new Vec3d(Math.cos(angle)*v,.02,Math.sin(angle)*v),.8F,55,.54F,.49F,.40F,false,random);
                }
            } else if(m.event==CruiseVisuals.Event.IGNITION) {
                if(age>3) return;
                Vec3d outlet=at.add(CruiseVisuals.worldOffset(CruiseVisuals.exhaust(m.build,m.build.get(CruiseSlot.LAUNCH)==CruisePartDefinition.LAUNCH_BOOSTER),m.yaw,m.pitch));
                for(int i=0;i<6;i++) puff(outlet,CruiseFlightMath.direction(m.yaw,m.pitch).scale(-.12).addVector(0,.025,0),.38F,24,1,.68F,.26F,true,random);
            } else if(m.event==CruiseVisuals.Event.CRASH) {
                if(age>4) return;
                for(int i=0;i<6;i++) puff(at,spread(.10,.03),.65F,55,.18F,.18F,.19F,false,random);
            } else if(m.event==CruiseVisuals.Event.IMPACT) impact();
        }
        private Vec3d spread(double speed,double rise) {
            double a=random.nextDouble()*Math.PI*2;return new Vec3d(Math.cos(a)*speed,rise+random.nextDouble()*speed,Math.sin(a)*speed);
        }
        private void impact() {
            if(age>16) return;
            CruiseWarheads.Effect style=CruiseWarheads.effect(message.build.get(CruiseSlot.WARHEAD));
            boolean emp=style==CruiseWarheads.Effect.EMP,hot=age<5 && !emp;
            int count=message.reduced?5:emp?5:style==CruiseWarheads.Effect.THERMOBARIC?18:12;
            for(int i=0;i<count;i++) {
                double angle=random.nextDouble()*Math.PI*2,distance=random.nextDouble()*radius*.32;
                Vec3d at=message.position.addVector(Math.cos(angle)*distance,random.nextDouble()*radius*.10,Math.sin(angle)*distance);
                float size=message.reduced?.75F:emp?.55F:style==CruiseWarheads.Effect.THERMOBARIC?2.3F:1.4F;
                boolean narrow=style==CruiseWarheads.Effect.PENETRATOR || style==CruiseWarheads.Effect.SHAPED;
                Vec3d velocity=spread(narrow?.05:.14,narrow?.20:.04);
                float red=emp?.14F:hot?1:.30F,green=emp?.72F:hot?.48F:.28F,blue=emp?1:hot?.10F:.25F;
                puff(at,velocity,size,emp?24:65+random.nextInt(30),red,green,blue,hot||emp,random);
                if(age<5 && (style==CruiseWarheads.Effect.FRAGMENTATION || style==CruiseWarheads.Effect.CLUSTER || style==CruiseWarheads.Effect.INCENDIARY))
                    puff(at,spread(.35+random.nextDouble()*.25,.08),.20F,style==CruiseWarheads.Effect.INCENDIARY?65:24,1,.72F,.23F,true,random);
            }
        }
        void ring(float partial) {
            float t=(age+partial)/life;if(t>=1 || t<0) return;
            boolean emp=CruiseWarheads.effect(message.build.get(CruiseSlot.WARHEAD))==CruiseWarheads.Effect.EMP;
            double outer=radius*Math.sqrt(t),inner=Math.max(0,outer-(emp?.7:1.4));
            Vec3d p=message.position;GL11.glColor4f(emp?.15F:1,emp?.8F:.70F,emp?1:.35F,(1-t)*(emp?.45F:.22F));
            GL11.glBegin(GL11.GL_QUAD_STRIP);
            for(int i=0;i<=64;i++) {
                double a=i*Math.PI*2/64,c=Math.cos(a),s=Math.sin(a);
                GL11.glVertex3d(p.x+c*inner,p.y+.12,p.z+s*inner);GL11.glVertex3d(p.x+c*outer,p.y+.12,p.z+s*outer);
            }
            GL11.glEnd();
            // Brief world-space core, never a full-screen flash or camera shake.
            float flash=1-(age+partial)/5;
            if(flash>0) {
                double size=(message.reduced?.6:Math.min(3,radius*.12))*(1-flash*.3);
                GL11.glColor4f(emp?.25F:1,emp?.85F:.86F,emp?1:.62F,flash*.55F);
                GL11.glBegin(GL11.GL_QUAD_STRIP);
                for(int ring=0;ring<8;ring++) {
                    double low=-Math.PI/2+ring*Math.PI/8,high=low+Math.PI/8;
                    for(int i=0;i<=24;i++) {
                        double a=i*Math.PI*2/24;
                        for(double latitude:new double[]{low,high}) GL11.glVertex3d(p.x+size*Math.cos(latitude)*Math.cos(a),p.y+size*Math.sin(latitude),p.z+size*Math.cos(latitude)*Math.sin(a));
                    }
                }
                GL11.glEnd();
            }
        }
    }
}
