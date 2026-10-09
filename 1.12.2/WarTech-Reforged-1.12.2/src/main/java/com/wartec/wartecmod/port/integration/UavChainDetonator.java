package com.wartec.wartecmod.port.integration;

import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemMultiDetonator;
import com.hbm.lib.HBMSoundHandler;
import com.wartec.wartecmod.WarTechReforged;
import com.wartec.wartecmod.port.entity.EntityCustomUav;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Adds entity-based UAV groups without replacing NTM's block bindings. */
@Mod.EventBusSubscriber(modid = WarTechReforged.MODID)
public final class UavChainDetonator {
    private static final String LINKS_KEY = "WarTechUavChain";
    private static final String UUID_KEY = "Uav";
    private static final String DIMENSION_KEY = "Dimension";
    private static final String X_KEY = "ChunkX";
    private static final String Z_KEY = "ChunkZ";
    private static final int MAX_LINKS = 64;

    private UavChainDetonator() {
    }

    public static boolean isMultiDetonator(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == ModItems.detonator_multi;
    }

    public static boolean isDetonator(ItemStack stack) {
        return isMultiDetonator(stack) || !stack.isEmpty() && stack.getItem() == ModItems.detonator;
    }

    /** The ordinary detonator has one binding, not an invisible mixed salvo. */
    public static void prepareBinding(ItemStack stack) {
        if (isMultiDetonator(stack)) return;
        clear(stack);
        CruiseChainDetonator.clear(stack);
        NBTTagCompound tag = getOrCreateTag(stack);
        tag.removeTag("x");tag.removeTag("y");tag.removeTag("z");
    }

    public static boolean mayControl(EntityPlayer player, EntityCustomUav uav) {
        return player != null && (player.getUniqueID().equals(uav.getOwnerUuid())
            || NetworkTeamHelper.areFriendly(uav.getOwnerTeam(), NetworkTeamHelper.getPlayerTeam(player)));
    }

    public static int linkCount(ItemStack stack) {
        return getLinks(stack).tagCount();
    }

    public static void bind(EntityPlayer player, ItemStack detonator,
            EntityCustomUav uav) {
        if (player == null || uav == null || !isDetonator(detonator)) {
            return;
        }
        if (!player.isSneaking()) {
            activate(player, detonator);
            return;
        }
        if (uav.getLegacyState() != 0 || uav.isDead) {
            message(player, "uav.chain.not_ready");
            return;
        }
        if (!uav.hasGuidanceTarget()) {
            message(player, "uav.chain.missing_target");
            return;
        }
        if (!mayControl(player, uav)) {
            message(player, "uav.chain.iff_denied");
            return;
        }

        prepareBinding(detonator);
        NBTTagList links = getLinks(detonator);
        UUID id = uav.getUniqueID();
        for (int index = 0; index < Math.min(MAX_LINKS,links.tagCount()); ++index) {
            if (id.equals(readUuid(links.getCompoundTagAt(index)))) {
                message(player, "uav.chain.duplicate", uav.getName());
                return;
            }
        }
        if (links.tagCount() >= MAX_LINKS) {
            message(player, "uav.chain.full", MAX_LINKS);
            return;
        }

        NBTTagCompound link = new NBTTagCompound();
        link.setUniqueId(UUID_KEY, id);
        link.setInteger(DIMENSION_KEY, uav.world.provider.getDimension());
        link.setInteger(X_KEY, MathHelper.floor(uav.posX) >> 4);
        link.setInteger(Z_KEY, MathHelper.floor(uav.posZ) >> 4);
        links.appendTag(link);
        getOrCreateTag(detonator).setTag(LINKS_KEY, links);
        message(player, "uav.chain.bound", uav.getName(), links.tagCount(),
                uav.getTargetX(), uav.getTargetY(), uav.getTargetZ());
    }

    public static void clear(ItemStack stack) {
        if (stack.hasTagCompound()) {
            stack.getTagCompound().removeTag(LINKS_KEY);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        boolean customData=stack.hasTagCompound() && (stack.getTagCompound().hasKey(LINKS_KEY,9)
            || stack.getTagCompound().hasKey("WarTechCruiseLaunchers",9));
        if (!isDetonator(stack) || !customData) return;

        boolean hasNtmBlockLinks = isMultiDetonator(stack) && ItemMultiDetonator.getLocations(stack) != null;
        EntityPlayer player = event.getEntityPlayer();
        if (!event.getWorld().isRemote) {
            if (player.isSneaking()) {
                int removed = linkCount(stack)+CruiseChainDetonator.count(stack);
                clear(stack);
                CruiseChainDetonator.clear(stack);
                if(stack.hasTagCompound() && stack.getTagCompound().hasNoTags()) stack.setTagCompound(null);
                message(player, "uav.chain.cleared", removed);
            } else {
                activate(player, stack);
            }
        }

        // A chain-only detonator would otherwise show NTM's "no block set"
        // error. Mixed UAV/block groups continue into the original NTM item.
        if (!hasNtmBlockLinks) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.SUCCESS);
        }
    }

    public static void activate(EntityPlayer player, ItemStack stack) {
        if (!(player instanceof EntityPlayerMP) || player.world.isRemote || !isDetonator(stack)) return;
        if (linkCount(stack)+CruiseChainDetonator.count(stack)==0) {
            message(player,"uav.chain.bind_hint");return;
        }
        if(linkCount(stack)>0) launchChain((EntityPlayerMP)player,stack);
        if(CruiseChainDetonator.count(stack)>0) CruiseChainDetonator.launch((EntityPlayerMP)player,stack);
    }

    @SubscribeEvent
    public static void onLaunchPoint(PlayerInteractEvent.RightClickBlock event) {
        if(isDetonator(event.getItemStack()) && !isMultiDetonator(event.getItemStack())
                && event.getEntityPlayer().isSneaking() && !event.getWorld().isRemote
                && event.getWorld().getBlockState(event.getPos()).getBlock()!=com.wartec.wartecmod.port.content.WarTechContent.UAV_LAUNCH_POINT
                && !(event.getWorld().getTileEntity(event.getPos()) instanceof com.wartec.wartecmod.port.gameplay.TileEntityCruiseLauncher)) {
            clear(event.getItemStack());CruiseChainDetonator.clear(event.getItemStack());
        }
        if (!isDetonator(event.getItemStack()) || event.getWorld().getBlockState(event.getPos()).getBlock()
                != com.wartec.wartecmod.port.content.WarTechContent.UAV_LAUNCH_POINT) return;
        event.setCanceled(true);event.setCancellationResult(EnumActionResult.SUCCESS);
        if(event.getWorld().isRemote) return;
        if(!event.getEntityPlayer().isSneaking()) { activate(event.getEntityPlayer(),event.getItemStack());return; }
        java.util.List<EntityCustomUav> candidates=event.getWorld().getEntitiesWithinAABB(EntityCustomUav.class,
            new net.minecraft.util.math.AxisAlignedBB(event.getPos()).grow(0.65,2,0.65),
            u->!u.isDead && u.getLegacyState()==0 && Math.abs(u.posX-event.getPos().getX()-.5)<.8
                && Math.abs(u.posZ-event.getPos().getZ()-.5)<.8);
        if(candidates.size()!=1) { message(event.getEntityPlayer(),"uav.chain.point_empty");return; }
        bind(event.getEntityPlayer(),event.getItemStack(),candidates.get(0));
    }

    private static void launchChain(EntityPlayerMP player, ItemStack stack) {
        NBTTagList links = getLinks(stack);
        NBTTagList retained = new NBTTagList();
        MinecraftServer server = player.getServer();
        int launched = 0;
        int skipped = 0;
        int queued = 0;
        for (int index = 0; index < Math.min(MAX_LINKS,links.tagCount()); ++index) {
            NBTTagCompound link = links.getCompoundTagAt(index);
            UUID id = readUuid(link);
            WorldServer world = id == null || server == null ? null
                    : server.getWorld(link.getInteger(DIMENSION_KEY));
            if (world == null) {
                ++skipped;
                retained.appendTag(link.copy());
                continue;
            }
            // Load remote launch sites through the same finite I/O queue as flight.
            Entity entity = world.getEntityFromUuid(id);
            if(!(entity instanceof EntityCustomUav)) {
                net.minecraft.util.math.BlockPos site=new net.minecraft.util.math.BlockPos(link.getInteger(X_KEY)*16,64,link.getInteger(Z_KEY)*16);
                boolean accepted=OperationalChunks.requestUntil(world,site,2,"uav:"+id,
                    ()->!player.isDead && player.connection!=null && hasLink(stack,id),()->{
                        Entity loaded=world.getEntityFromUuid(id);
                        // A loaded block is not proof that async entity deserialization finished.
                        if(!(loaded instanceof EntityCustomUav)) return false;
                        boolean ok=loaded instanceof EntityCustomUav && ((EntityCustomUav)loaded).launchFromChain(player);
                        if(ok) removeLink(stack,id);
                        message(player,ok?"uav.chain.remote_launched":"uav.chain.remote_failed");player.inventory.markDirty();
                        return true;
                    });
                if(accepted) queued++;else skipped++;
                retained.appendTag(link.copy());continue;
            }
            if (entity instanceof EntityCustomUav
                    && ((EntityCustomUav) entity).launchFromChain(player)) {
                ++launched;
            } else {
                ++skipped;
                retained.appendTag(link.copy());
            }
        }
        getOrCreateTag(stack).setTag(LINKS_KEY,retained);
        player.world.playSound(null, player.posX, player.posY, player.posZ,
                HBMSoundHandler.techBleep, SoundCategory.AMBIENT,
                1.2F, 1.0F);
        message(player, "uav.chain.launched", launched, skipped);
        if(queued>0) message(player,"uav.chain.queued",queued);
    }

    private static boolean hasLink(ItemStack stack,UUID id) {
        NBTTagList list=getLinks(stack);for(int i=0;i<list.tagCount();i++) if(id.equals(readUuid(list.getCompoundTagAt(i)))) return true;return false;
    }
    private static void removeLink(ItemStack stack,UUID id) {
        NBTTagList keep=new NBTTagList(),list=getLinks(stack);
        for(int i=0;i<list.tagCount();i++) if(!id.equals(readUuid(list.getCompoundTagAt(i)))) keep.appendTag(list.getCompoundTagAt(i).copy());
        getOrCreateTag(stack).setTag(LINKS_KEY,keep);
    }

    private static NBTTagCompound getOrCreateTag(ItemStack stack) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        return stack.getTagCompound();
    }

    private static NBTTagList getLinks(ItemStack stack) {
        return stack.hasTagCompound()
                ? stack.getTagCompound().getTagList(LINKS_KEY, 10)
                : new NBTTagList();
    }

    private static UUID readUuid(NBTTagCompound link) {
        return link.hasUniqueId(UUID_KEY) ? link.getUniqueId(UUID_KEY) : null;
    }

    private static void message(EntityPlayer player, String key,
            Object... arguments) {
        player.sendMessage(new TextComponentTranslation(key, arguments));
    }
}
