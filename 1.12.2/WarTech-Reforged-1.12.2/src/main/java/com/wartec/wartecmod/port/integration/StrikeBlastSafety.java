package com.wartec.wartecmod.port.integration;

import com.wartec.wartecmod.WarTechReforged;
import com.wartec.wartecmod.port.entity.*;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Only allied, airborne strike ordnance is protected from another allied strike.
 * Not a radius/time immunity: unrelated explosions and interceptors remain lethal. */
@Mod.EventBusSubscriber(modid = WarTechReforged.MODID)
public final class StrikeBlastSafety {
    private static final String TAG = "WarTechStrikeOrigin";
    private static final ThreadLocal<Origin> ACTIVE = new ThreadLocal<>();
    private static final java.lang.reflect.Field EXPLODER = net.minecraftforge.fml.relauncher.ReflectionHelper.findField(
            net.minecraft.world.Explosion.class,new String[]{"exploder","field_77283_e"});
    private StrikeBlastSafety() { }

    public static boolean allied(UUID a, String ta, UUID b, String tb) {
        if(ta!=null && !ta.isEmpty() && tb!=null && !tb.isEmpty()) return ta.equals(tb);
        return a != null && a.equals(b) || ta != null && !ta.isEmpty() && ta.equals(tb);
    }
    private static boolean strike(Entity entity) {
        if (entity instanceof EntityWarTechArtilleryProjectile) return ((EntityWarTechArtilleryProjectile)entity).isCruiseSubmunition();
        if (entity instanceof EntityCustomCruise) return true;
        if (entity instanceof EntityCustomUav) return ((EntityCustomUav) entity).hasStrikeWarhead();
        if (entity instanceof EntityWarTechOrdnance)
            return ((EntityWarTechOrdnance)entity).getOrdnanceFamily()!=EntityWarTechOrdnance.FAMILY_AIR_TO_AIR;
        if (entity instanceof EntityWarTechMissile) {
            if (((EntityWarTechMissile)entity).getMissileSpecification().getFlightFamily()
                    == LegacyMissileSpecification.FlightFamily.INTERCEPTOR) return false;
            LegacyMissileSpecification.Payload payload = ((EntityWarTechMissile) entity).getMissileSpecification().getPayload();
            return payload != LegacyMissileSpecification.Payload.NONE && payload != LegacyMissileSpecification.Payload.INTERCEPTOR;
        }
        return false;
    }
    private static Origin origin(Entity entity) {
        if (entity == null) return null;
        NBTTagCompound tag = entity.getEntityData().getCompoundTag(TAG);
        if (tag.hasKey("Team", 8)) {
            UUID owner = tag.hasUniqueId("Owner") ? tag.getUniqueId("Owner") : null;
            return new Origin(entity.world, owner, tag.getString("Team"));
        }
        if (!strike(entity)) return null;
        EntityWarTechBase vehicle = (EntityWarTechBase) entity;
        return new Origin(entity.world, vehicle.getOwnerUuid(), vehicle.getOwnerTeam());
    }
    public static Scope enter(Entity source) {
        Origin previous = ACTIVE.get();
        // A nested unowned/hostile explosion must never inherit the parent's allegiance.
        Origin current = origin(source);
        if (current == null) ACTIVE.remove(); else ACTIVE.set(current);
        return new Scope(previous);
    }
    public static final class Scope implements AutoCloseable {
        private final Origin previous;
        private boolean closed;
        private Scope(Origin previous) { this.previous = previous; }
        @Override public void close() {
            if (closed) return;
            closed = true;
            if (previous == null) ACTIVE.remove(); else ACTIVE.set(previous);
        }
    }
    private static final class Origin {
        final World world;
        final UUID owner;
        final String team;
        Origin(World world, UUID owner, String team) { this.world=world; this.owner=owner; this.team=team; }
    }
    private static boolean protects(Entity entity, Origin source) {
        if (source == null || entity.world != source.world || entity.isDead || !strike(entity)) return false;
        EntityWarTechBase vehicle = (EntityWarTechBase) entity;
        // Cruise bomblets deliberately have the generic kinetic-rod fuse disabled.
        // Their own impact payload is live: sibling blasts must not erase it or
        // kick it out of its dispersion pattern. Other artillery remains vulnerable.
        if (!vehicle.isArmed() && !(entity instanceof EntityWarTechArtilleryProjectile
                && ((EntityWarTechArtilleryProjectile)entity).isCruiseSubmunition())) return false;
        return allied(source.owner, source.team, vehicle.getOwnerUuid(), vehicle.getOwnerTeam());
    }
    public static boolean protectsFromActiveStrike(Entity entity) { return protects(entity,ACTIVE.get()); }
    public static boolean ignores(Entity entity, DamageSource damage) {
        Origin explicit = origin(damage.getImmediateSource());
        if (explicit == null) explicit = origin(damage.getTrueSource());
        if (explicit != null) return protects(entity, explicit);
        if (damage.getImmediateSource()!=null || damage.getTrueSource()!=null) return false;
        // Legacy HBM effects construct anonymous explosion sources synchronously.
        return damage.isExplosion() && protects(entity, ACTIVE.get());
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void explosion(ExplosionEvent.Detonate event) {
        if (event.getWorld().isRemote) return;
        Entity exploder;
        try { exploder = (Entity) EXPLODER.get(event.getExplosion()); }
        catch (IllegalAccessException exception) { return; }
        Origin source = exploder == null ? ACTIVE.get() : origin(exploder);
        // Remove before both damage AND vanilla/VNT knockback, not just attackEntityFrom.
        event.getAffectedEntities().removeIf(entity -> protects(entity, source));
    }
    @SubscribeEvent
    public static void child(EntityJoinWorldEvent event) {
        Origin source = ACTIVE.get();
        Entity entity = event.getEntity();
        if (source == null || event.getWorld().isRemote || event.getWorld() != source.world) return;
        if (!(entity instanceof EntityWarTechArtilleryProjectile)
                && !entity.getClass().getName().startsWith("com.hbm.entity.projectile.")) return;
        NBTTagCompound tag = new NBTTagCompound();
        if (source.owner != null) tag.setUniqueId("Owner", source.owner);
        tag.setString("Team", source.team);
        entity.getEntityData().setTag(TAG, tag);
        if (entity instanceof EntityWarTechBase)
            ((EntityWarTechBase) entity).setOwnerIdentity(source.owner, source.team);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void fragment(net.minecraftforge.event.entity.ProjectileImpactEvent event) {
        // CE shrapnel/rubble use a static anonymous DamageSource; intercept the actual
        // projectile contact while its persistent provenance is still available.
        if (!event.getEntity().world.isRemote && event.getRayTraceResult().entityHit!=null
                && protects(event.getRayTraceResult().entityHit,origin(event.getEntity()))) event.setCanceled(true);
    }
}
