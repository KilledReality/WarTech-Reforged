package com.wartec.wartecmod.port.client;

import com.wartec.wartecmod.port.entity.*;
import com.wartec.wartecmod.port.content.MissileProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.Entity;
import net.minecraft.util.*;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import java.util.*;

/** Local, budgeted smoke and one continuous motor per nearby burning rocket. */
final class StrategicLaunchEffects {
    private static final Map<Entity,Motor> motors=new WeakHashMap<>();
    private static final Map<Entity,Integer> ejections=new WeakHashMap<>();
    private static World currentWorld;
    private StrategicLaunchEffects() { }
    static boolean burning(Entity entity) {
        if(entity.isDead) return false;
        if(entity instanceof EntityStrategicTel) {
            EntityStrategicTel tel=(EntityStrategicTel)entity;
            return tel.getLaunchTicks()>=tel.getStrategicSystem().getIgnitionTick()
                && tel.getLaunchTicks()<tel.getStrategicSystem().getSeparationTick();
        }
        if(entity instanceof EntityStrategicMissile) {
            EntityStrategicMissile missile=(EntityStrategicMissile)entity;
            return !missile.isFallingDebris() && !missile.isReentryVehicle() && !missile.isSiloCold();
        }
        return entity instanceof EntityWarTechMissile && (((EntityWarTechMissile)entity).getMissileProfile()==MissileProfile.ISKANDER
            || ModernMissileProfile.of(((EntityWarTechMissile)entity).getMissileProfile())!=null || ((EntityWarTechMissile)entity).hasModernVisual())
            && ((EntityWarTechMissile)entity).isArmed() && ((EntityWarTechMissile)entity).getFlightStage()!=3;
    }
    static void tick(World world) {
        if(world!=currentWorld) { for(Motor motor:motors.values()) motor.stop();motors.clear();ejections.clear();currentWorld=world; }
        Entity camera=Minecraft.getMinecraft().getRenderViewEntity();if(camera==null) return;
        for(Entity entity:world.loadedEntityList) if(entity instanceof EntityStrategicTel && !entity.isDead) {
            EntityStrategicTel tel=(EntityStrategicTel)entity;int age=tel.getLaunchTicks(),start=tel.getStrategicSystem().getEjectionTick();
            if(!tel.getStrategicSystem().isTactical() && age>=start && age<start+5 && !Objects.equals(ejections.get(entity),age)) {
                CruiseEffects.coldEjectionCloud(entity,tel.getEjectionCloudPosition());ejections.put(entity,age);
            } else if(age==0) ejections.remove(entity);
        }
        for(Entity entity:world.loadedEntityList)if(entity instanceof EntityStrategicMissile && !entity.isDead){
            EntityStrategicMissile missile=(EntityStrategicMissile)entity;
            if(missile.isSiloCold() && missile.ticksExisted%5==0 && !Objects.equals(ejections.get(entity),missile.ticksExisted)){
                CruiseEffects.coldEjectionCloud(entity,new Vec3d(entity.posX,missile.getSiloRoof()+1,entity.posZ));ejections.put(entity,missile.ticksExisted);
            }
        }
        for(Entity entity:world.loadedEntityList) if(burning(entity) && camera.getDistanceSq(entity)<192*192) {
            if(!motors.containsKey(entity)) {
                SoundEvent sound=SoundEvent.REGISTRY.getObject(new ResourceLocation("wartecmod","weapon.cruisemissileengine"));
                if(sound!=null) { Motor motor=new Motor(entity,sound);motors.put(entity,motor);Minecraft.getMinecraft().getSoundHandler().playSound(motor); }
            }
            boolean tactical=entity instanceof EntityWarTechMissile || entity instanceof EntityStrategicTel && ((EntityStrategicTel)entity).getStrategicSystem().isTactical();
            Vec3d nozzle,direction=new Vec3d(entity.motionX,entity.motionY,entity.motionZ);boolean ground=false;
            if(entity instanceof EntityStrategicTel) {
                EntityStrategicTel tel=(EntityStrategicTel)entity;
                nozzle=tel.getLaunchPosition(tel.getLaunchSlot(),tel.getAnimatedLaunch(1));direction=new Vec3d(0,1,0);ground=true;
            } else {
                nozzle=entity.getPositionVector();if(direction.lengthSquared()<1.0E-6) direction=new Vec3d(0,1,0);else direction=direction.normalize();
                if(entity instanceof EntityWarTechMissile) {
                    EntityWarTechMissile missile=(EntityWarTechMissile)entity;ModernMissileProfile profile=ModernMissileProfile.of(missile.getMissileProfile());
                    if(missile.hasModernVisual()) nozzle=nozzle.subtract(direction.scale(missile.getModernVisual().getFloat("ScaleY")*.5));
                    else if(profile!=null) {
                        Vec3d local=profile==ModernMissileProfile.FP5?new Vec3d(0,.46,-3.48):new Vec3d(0,0,-profile.length*.4975);
                        nozzle=nozzle.add(com.wartec.wartecmod.port.cruise.CruiseVisuals.worldOffset(local,-missile.rotationYaw,missile.rotationPitch));
                    }
                }
            }
            CruiseEffects.heavyRocketSmoke(entity,nozzle,direction,tactical?6F:9.5F,ground);
        }
        for(Iterator<Map.Entry<Entity,Motor>> it=motors.entrySet().iterator();it.hasNext();) {
            Map.Entry<Entity,Motor> entry=it.next();
            if(!burning(entry.getKey()) || camera.getDistanceSq(entry.getKey())>224*224 || entry.getKey().world!=world) { entry.getValue().stop();it.remove(); }
        }
    }
    private static final class Motor extends MovingSound {
        private final Entity source;
        Motor(Entity source,SoundEvent sound) {
            super(sound,SoundCategory.PLAYERS);this.source=source;repeat=true;repeatDelay=0;volume=source instanceof EntityStrategicMissile && ((EntityStrategicMissile)source).isSiloLaunch()?7:5;pitch=source instanceof EntityStrategicMissile && ((EntityStrategicMissile)source).isSiloLaunch()?.48F:.60F;update();
        }
        void stop() { donePlaying=true; }
        @Override public void update() {
            if(!burning(source) || Minecraft.getMinecraft().world!=source.world) { stop();return; }
            xPosF=(float)source.posX;yPosF=(float)source.posY;zPosF=(float)source.posZ;
        }
    }
}
