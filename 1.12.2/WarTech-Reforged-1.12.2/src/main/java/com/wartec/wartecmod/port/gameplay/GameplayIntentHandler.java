package com.wartec.wartecmod.port.gameplay;

import com.wartec.wartecmod.port.content.ContentHooks;
import com.wartec.wartecmod.port.content.IntentProvider;
import com.wartec.wartecmod.port.content.MissileItem;
import com.wartec.wartecmod.port.content.MissileProfile;
import com.wartec.wartecmod.port.entity.EntityWarTechBase;
import com.wartec.wartecmod.port.entity.EntityWarTechGroundVehicle;
import com.wartec.wartecmod.port.entity.EntityWarTechMissile;
import com.wartec.wartecmod.port.entity.EntityWarTechOrdnance;
import com.wartec.wartecmod.port.entity.LegacyEntityFactory;
import com.wartec.wartecmod.port.entity.WarTechEntityProfile;
import com.wartec.wartecmod.port.integration.ITeamOwned;
import com.wartec.wartecmod.port.integration.PlayerTeamPersistence;
import com.wartec.wartecmod.port.integration.OwnerTeamNbt;
import java.util.List;
import java.util.Locale;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.SoundCategory;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class GameplayIntentHandler implements ContentHooks.IntentHandler {
    private static final String TARGET_SET = "WarTechPortTargetSet";
    private static final String TARGET_X = "WarTechPortTargetX";
    private static final String TARGET_Y = "WarTechPortTargetY";
    private static final String TARGET_Z = "WarTechPortTargetZ";

    @Override
    public EnumActionResult handle(ContentHooks.IntentContext context) {
        if (context.getWorld().isRemote) {
            return EnumActionResult.SUCCESS;
        }

        NBTTagCompound intent = context.getIntent();
        String id = intent.getString(IntentProvider.TAG_ID).toLowerCase(Locale.ROOT);
        IntentProvider.IntentKind kind = context.getProvider().getIntentKind();
        switch (kind) {
            case DEPLOYMENT:
                return spawnDeployment(context, id, intent.getInteger(IntentProvider.TAG_VARIANT));
            default:
                return EnumActionResult.PASS;
        }
    }

    private EnumActionResult spawnMissile(ContentHooks.IntentContext context, String id) {
        MissileProfile profile = context.getProvider() instanceof MissileItem
                ? ((MissileItem) context.getProvider()).getProfile()
                : missileProfile(id);
        if (profile == MissileProfile.INVALID) {
            return EnumActionResult.FAIL;
        }
        EntityWarTechMissile missile =
            LegacyEntityFactory.missile(context.getWorld(), profile);
        missile.setVisual(id, 0);
        positionAtUse(missile, context.getPos());
        assignOwnerAndTarget(missile, context.getPlayer(), 768.0D);
        missile.motionY = Math.max(0.12D, missile.motionY);
        return spawnAndConsume(context, missile);
    }

    private EnumActionResult spawnDeployment(
        ContentHooks.IntentContext context,
        String id,
        int variant
    ) {
        EntityWarTechBase entity;
        if (id.contains("mq9")) {
            entity = LegacyEntityFactory.aircraft(
                    context.getWorld(), WarTechEntityProfile.MQ_9_REAPER);
        } else if (id.contains("f16")) {
            entity = LegacyEntityFactory.aircraft(
                    context.getWorld(), WarTechEntityProfile.F_16C);
        } else if (id.contains("su27")) {
            entity = LegacyEntityFactory.aircraft(
                    context.getWorld(), WarTechEntityProfile.SU_27);
        } else if (id.contains("tu95")) {
            entity = LegacyEntityFactory.aircraft(
                    context.getWorld(), WarTechEntityProfile.TU_95);
        } else {
            entity = LegacyEntityFactory.groundVehicle(context.getWorld(),
                    groundProfile(id, variant));
        }

        positionDeployment(entity, context.getPos(), context.getPlayer());
        entity.setVisual(id, variant);
        entity.setOwner(context.getPlayer());
        entity.setOwnerTeam(OwnerTeamNbt.resolvePlacementTeam(
                context.getStack(), context.getPlayer()));
        if (entity instanceof com.wartec.wartecmod.port.entity.EntityStrategicTel
                && context.getStack().hasTagCompound()
                && context.getStack().getTagCompound()
                        .hasKey("StrategicLoaded", 1)) {
            ((com.wartec.wartecmod.port.entity.EntityStrategicTel) entity)
                    .setMissileLoaded(context.getStack().getTagCompound()
                            .getBoolean("StrategicLoaded"));
        }
        EnumActionResult result = spawnAndConsume(context, entity);
        if (result == EnumActionResult.SUCCESS) {
            playDeploymentSound(context.getWorld(), context.getPos(), entity);
        }
        return result;
    }

    private EnumActionResult spawnOrdnance(
        ContentHooks.IntentContext context,
        String id,
        int variant
    ) {
        WarTechEntityProfile profile = variant == 1 || id.contains("kab")
            ? WarTechEntityProfile.KAB_3000
            : WarTechEntityProfile.FAB_5000;
        EntityWarTechOrdnance ordnance =
                LegacyEntityFactory.strategicBomb(context.getWorld(), profile);
        ordnance.setVisual(id, variant);
        positionAtUse(ordnance, context.getPos().up(2));
        ordnance.setOwner(context.getPlayer());
        ordnance.setOwnerTeam(PlayerTeamPersistence.getPlayerTeam(context.getPlayer()));
        assignTargetFromPlayer(ordnance, context.getPlayer(), 256.0D);
        return spawnAndConsume(context, ordnance);
    }

    private EnumActionResult executeSatellite(ContentHooks.IntentContext context, String id) {
        BlockPos target = getTarget(context.getPlayer(), context.getPos());
        if (!id.contains("kinetic")) {
            WarTechEntityProfile profile = id.contains("nuclear")
                ? WarTechEntityProfile.FAB_5000
                : WarTechEntityProfile.KAB_3000;
            EntityWarTechOrdnance strike =
                LegacyEntityFactory.strategicBomb(context.getWorld(), profile);
            strike.setPosition(
                target.getX() + 0.5D,
                Math.min(320.0D, target.getY() + 180.0D),
                target.getZ() + 0.5D
            );
            strike.setVisual("ordnance/strategic_bomb", id.contains("nuclear") ? 0 : 1);
            strike.setOwner(context.getPlayer());
            strike.setOwnerTeam(PlayerTeamPersistence.getPlayerTeam(context.getPlayer()));
            strike.setGuidanceTarget(
                target.getX() + 0.5D,
                target.getY(),
                target.getZ() + 0.5D
            );
            strike.motionY = -1.0D;
            return spawnAndConsume(context, strike);
        }
        EntityWarTechOrdnance rod =
            LegacyEntityFactory.kineticRod(context.getWorld());
        rod.setPosition(target.getX() + 0.5D, Math.min(320.0D, target.getY() + 180.0D),
            target.getZ() + 0.5D);
        rod.setVisual(id, 0);
        rod.setOwner(context.getPlayer());
        rod.setOwnerTeam(PlayerTeamPersistence.getPlayerTeam(context.getPlayer()));
        rod.setGuidanceTarget(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
        rod.motionY = -1.2D;
        return spawnAndConsume(context, rod);
    }

    private EnumActionResult executeTool(ContentHooks.IntentContext context, String id) {
        EntityPlayer player = context.getPlayer();
        if (id.contains("target_finder") || id.contains("missile_strike_caller")) {
            setTarget(player, context.getPos());
            return EnumActionResult.SUCCESS;
        }
        if (id.contains("iff_configurator")) {
            return bindNearbyOwner(context);
        }
        if (id.contains("salvage_wrench")) {
            return salvageNearbyVehicle(context);
        }
        return EnumActionResult.PASS;
    }

    private EnumActionResult bindNearbyOwner(ContentHooks.IntentContext context) {
        String team = PlayerTeamPersistence.getPlayerTeam(context.getPlayer());
        TileEntity tile = context.getWorld().getTileEntity(context.getPos().down());
        if (tile instanceof ITeamOwned) {
            ((ITeamOwned) tile).setOwnerTeam(team);
            tile.markDirty();
            return EnumActionResult.SUCCESS;
        }
        for (Entity entity : nearbyEntities(context.getWorld(), context.getPos(), 3.0D)) {
            if (entity instanceof EntityWarTechBase) {
                ((EntityWarTechBase) entity).setOwnerTeam(team);
                return EnumActionResult.SUCCESS;
            }
        }
        return EnumActionResult.PASS;
    }

    private EnumActionResult salvageNearbyVehicle(ContentHooks.IntentContext context) {
        for (Entity entity : nearbyEntities(context.getWorld(), context.getPos(), 3.0D)) {
            if (entity instanceof EntityWarTechGroundVehicle && !entity.isDead) {
                entity.setDead();
                return EnumActionResult.SUCCESS;
            }
        }
        return EnumActionResult.PASS;
    }

    private List<Entity> nearbyEntities(World world, BlockPos pos, double radius) {
        AxisAlignedBB box = new AxisAlignedBB(pos).grow(radius);
        return world.getEntitiesWithinAABBExcludingEntity(null, box);
    }

    private EnumActionResult spawnAndConsume(
        ContentHooks.IntentContext context,
        Entity entity
    ) {
        if (!context.getWorld().spawnEntity(entity)) {
            return EnumActionResult.FAIL;
        }
        EntityPlayer player = context.getPlayer();
        if (!player.capabilities.isCreativeMode) {
            ItemStack stack = context.getStack();
            stack.shrink(1);
        }
        return EnumActionResult.SUCCESS;
    }

    private void positionAtUse(Entity entity, BlockPos pos) {
        entity.setPosition(pos.getX() + 0.5D, pos.getY() + 0.15D, pos.getZ() + 0.5D);
    }

    private void positionDeployment(EntityWarTechBase entity, BlockPos pos,
            EntityPlayer player) {
        boolean artillery =
                entity.getProfile() == WarTechEntityProfile.MOBILE_ARTILLERY;
        float yaw = artillery ? player.rotationYaw
                : Math.round(player.rotationYaw / 90.0F) * 90.0F;
        double yOffset = entity.getProfile() == WarTechEntityProfile.ELECTRONIC_WARFARE
                ? 1.02D : 1.05D;
        entity.setLocationAndAngles(pos.getX() + 0.5D,
                pos.getY() + yOffset, pos.getZ() + 0.5D, yaw, 0.0F);
    }

    private void playDeploymentSound(World world, BlockPos pos,
            EntityWarTechBase entity) {
        float volume;
        float pitch;
        switch (entity.getProfile()) {
            case COMMAND_TRUCK:
                volume = 0.55F;
                pitch = 1.0F;
                break;
            case ELECTRONIC_WARFARE:
                volume = 0.45F;
                pitch = 1.25F;
                break;
            case MOBILE_AIR_DEFENSE:
                volume = 0.70F;
                pitch = 0.82F;
                break;
            case MOBILE_ARTILLERY:
                volume = 0.65F;
                pitch = 1.35F;
                break;
            case MQ_9_REAPER:
                volume = 0.65F;
                pitch = 1.08F;
                break;
            case RADAR_TRUCK:
                volume = 0.55F;
                pitch = 1.45F;
                break;
            case S400_RADAR:
                volume = 0.65F;
                pitch = 0.80F;
                break;
            case STRATEGIC_TOPOL_M:
            case STRATEGIC_YARS:
            case STRATEGIC_ORESHNIK:
                volume = 1.15F;
                pitch = 0.58F;
                break;
            case F_16C:
            case SU_27:
                volume = 0.80F;
                pitch = 0.92F;
                break;
            case TU_95:
                volume = 0.75F;
                pitch = 0.78F;
                break;
            default:
                return;
        }
        world.playSound(null, pos.getX() + 0.5D, pos.getY() + 1.0D,
                pos.getZ() + 0.5D, SoundEvents.BLOCK_ANVIL_LAND,
                SoundCategory.BLOCKS, volume, pitch);
    }

    private void assignOwnerAndTarget(
        EntityWarTechBase entity,
        EntityPlayer player,
        double fallbackRange
    ) {
        entity.setOwner(player);
        entity.setOwnerTeam(PlayerTeamPersistence.getPlayerTeam(player));
        assignTargetFromPlayer(entity, player, fallbackRange);
    }

    private void assignTargetFromPlayer(
        EntityWarTechBase entity,
        EntityPlayer player,
        double fallbackRange
    ) {
        BlockPos target = getTarget(player, null);
        if (target != null) {
            entity.setGuidanceTarget(
                target.getX() + 0.5D,
                target.getY() + 0.5D,
                target.getZ() + 0.5D
            );
            return;
        }
        Vec3d look = player.getLookVec();
        entity.setGuidanceTarget(
            player.posX + look.x * fallbackRange,
            Math.max(1.0D, player.posY + look.y * fallbackRange),
            player.posZ + look.z * fallbackRange
        );
    }

    private static void setTarget(EntityPlayer player, BlockPos target) {
        NBTTagCompound data = player.getEntityData();
        data.setBoolean(TARGET_SET, true);
        data.setInteger(TARGET_X, target.getX());
        data.setInteger(TARGET_Y, target.getY());
        data.setInteger(TARGET_Z, target.getZ());
    }

    private static BlockPos getTarget(EntityPlayer player, BlockPos fallback) {
        NBTTagCompound data = player.getEntityData();
        if (!data.getBoolean(TARGET_SET)) {
            return fallback;
        }
        return new BlockPos(
            data.getInteger(TARGET_X),
            data.getInteger(TARGET_Y),
            data.getInteger(TARGET_Z)
        );
    }

    private static MissileProfile missileProfile(String id) {
        for (MissileProfile profile : MissileProfile.values()) {
            if (id.endsWith("/" + profile.getIntentPath())
                    || id.equals(profile.getIntentPath())) {
                return profile;
            }
        }
        return MissileProfile.INVALID;
    }

    private static WarTechEntityProfile groundProfile(String id, int variant) {
        if (id.contains("strategic_topol_m")) {
            return WarTechEntityProfile.STRATEGIC_TOPOL_M;
        }
        if (id.contains("strategic_yars")) {
            return WarTechEntityProfile.STRATEGIC_YARS;
        }
        if (id.contains("strategic_oreshnik")) {
            return WarTechEntityProfile.STRATEGIC_ORESHNIK;
        }
        if (id.contains("air_defense_command_truck") || id.contains("command_truck")) {
            return WarTechEntityProfile.COMMAND_TRUCK;
        }
        if (id.contains("s400_long_range_radar")) {
            return WarTechEntityProfile.S400_RADAR;
        }
        if (id.contains("mobile_radar_truck")) {
            return WarTechEntityProfile.RADAR_TRUCK;
        }
        if (id.contains("mobile_air_defense")) {
            return WarTechEntityProfile.MOBILE_AIR_DEFENSE;
        }
        if (id.contains("artillery")) {
            return WarTechEntityProfile.MOBILE_ARTILLERY;
        }
        if (id.contains("electronic")) {
            return WarTechEntityProfile.ELECTRONIC_WARFARE;
        }
        return WarTechEntityProfile.COMMAND_TRUCK;
    }
}
