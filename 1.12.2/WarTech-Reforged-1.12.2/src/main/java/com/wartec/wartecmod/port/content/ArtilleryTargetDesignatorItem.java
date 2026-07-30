package com.wartec.wartecmod.port.content;

import com.hbm.lib.HBMSoundHandler;
import com.wartec.wartecmod.port.entity.EntityWarTechGroundVehicle;
import com.wartec.wartecmod.port.entity.WarTechEntityProfile;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

/**
 * 1.12.2 replacement for HBM's removed ItemDesignatorArtyRange.
 */
public final class ArtilleryTargetDesignatorItem extends PortItem {
    private static final String ENTITY_ID = "WarTechArtilleryEntityId";
    private static final String ENTITY_UUID = "WarTechArtilleryUuid";
    private static final String DIMENSION = "WarTechArtilleryDimension";

    public ArtilleryTargetDesignatorItem(String legacyRegistryName,
            CreativeTabs tab) {
        super(legacyRegistryName, tab, 1);
        setFull3D();
    }

    public boolean linkTo(EntityWarTechGroundVehicle artillery,
            EntityPlayer player, ItemStack stack) {
        if (artillery.getProfile() != WarTechEntityProfile.MOBILE_ARTILLERY
                || artillery.getVisualVariant() == 0) {
            player.sendMessage(new TextComponentString(
                    "This chassis has no artillery module."));
            return false;
        }
        if (!artillery.isDeployed()) {
            player.sendMessage(new TextComponentString(
                    "Deploy the artillery platform before linking."));
            return false;
        }
        NBTTagCompound tag = getOrCreateTag(stack);
        BlockPos pos = artillery.getPosition();
        tag.setInteger("x", pos.getX());
        tag.setInteger("y", pos.getY());
        tag.setInteger("z", pos.getZ());
        tag.setInteger(ENTITY_ID, artillery.getEntityId());
        tag.setUniqueId(ENTITY_UUID, artillery.getUniqueID());
        tag.setInteger(DIMENSION, artillery.dimension);
        if (!artillery.world.isRemote) {
            player.sendMessage(new TextComponentString(
                    "Linked to " + pos.getX() + ", " + pos.getY()
                            + ", " + pos.getZ()));
        }
        artillery.world.playSound(null, artillery.posX, artillery.posY,
                artillery.posZ, HBMSoundHandler.techBleep,
                SoundCategory.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world,
            EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (world.isRemote) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        NBTTagCompound tag = stack.getTagCompound();
        EntityWarTechGroundVehicle artillery =
                resolveLinkedArtillery(world, tag);
        if (artillery == null) {
            player.sendMessage(new TextComponentString("No turret linked!"));
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        RayTraceResult trace = player.rayTrace(500.0D, 1.0F);
        if (trace == null || trace.typeOfHit != RayTraceResult.Type.BLOCK) {
            return new ActionResult<>(EnumActionResult.PASS, stack);
        }
        if (!artillery.acceptArtilleryDesignatorTarget(
                player, trace.getBlockPos())) {
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        world.playSound(null, player.posX, player.posY, player.posZ,
                HBMSoundHandler.techBoop, SoundCategory.PLAYERS,
                1.0F, 1.0F);
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
            List<String> tooltip, ITooltipFlag flag) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey("x", 99)
                || !tag.hasKey("y", 99) || !tag.hasKey("z", 99)) {
            tooltip.add(TextFormatting.RED + "No turret linked!");
            return;
        }
        tooltip.add(TextFormatting.YELLOW + "Linked to "
                + tag.getInteger("x") + ", " + tag.getInteger("y")
                + ", " + tag.getInteger("z"));
    }

    private static NBTTagCompound getOrCreateTag(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        return tag;
    }

    private static EntityWarTechGroundVehicle resolveLinkedArtillery(
            World world, @Nullable NBTTagCompound tag) {
        if (tag == null) {
            return null;
        }
        if (tag.hasKey(DIMENSION, 99)
                && tag.getInteger(DIMENSION)
                        != world.provider.getDimension()) {
            return null;
        }
        if (tag.hasUniqueId(ENTITY_UUID)) {
            UUID expected = tag.getUniqueId(ENTITY_UUID);
            Entity byId = world.getEntityByID(tag.getInteger(ENTITY_ID));
            if (isLinkedArtillery(byId, expected)) {
                return (EntityWarTechGroundVehicle) byId;
            }
            for (Entity entity : world.loadedEntityList) {
                if (isLinkedArtillery(entity, expected)) {
                    return (EntityWarTechGroundVehicle) entity;
                }
            }
        }
        if (!tag.hasKey("x", 99) || !tag.hasKey("y", 99)
                || !tag.hasKey("z", 99)) {
            return null;
        }
        double x = tag.getInteger("x") + 0.5D;
        double y = tag.getInteger("y") + 0.5D;
        double z = tag.getInteger("z") + 0.5D;
        EntityWarTechGroundVehicle nearest = null;
        double nearestDistance = 16.0D * 16.0D;
        for (Entity entity : world.loadedEntityList) {
            if (!(entity instanceof EntityWarTechGroundVehicle)) {
                continue;
            }
            EntityWarTechGroundVehicle artillery =
                    (EntityWarTechGroundVehicle) entity;
            if (artillery.getProfile()
                    != WarTechEntityProfile.MOBILE_ARTILLERY) {
                continue;
            }
            double distance = artillery.getDistanceSq(x, y, z);
            if (distance <= nearestDistance) {
                nearest = artillery;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static boolean isLinkedArtillery(Entity entity, UUID expected) {
        return entity instanceof EntityWarTechGroundVehicle
                && expected.equals(entity.getUniqueID())
                && ((EntityWarTechGroundVehicle) entity).getProfile()
                        == WarTechEntityProfile.MOBILE_ARTILLERY;
    }
}
