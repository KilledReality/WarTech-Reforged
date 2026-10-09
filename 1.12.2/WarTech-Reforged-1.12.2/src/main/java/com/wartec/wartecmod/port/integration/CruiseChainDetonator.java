package com.wartec.wartecmod.port.integration;

import com.wartec.wartecmod.WarTechReforged;
import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.gameplay.TileEntityCruiseLauncher;
import java.util.UUID;
import net.minecraft.entity.player.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid=WarTechReforged.MODID)
public final class CruiseChainDetonator {
    private static final String KEY="WarTechCruiseLaunchers";
    private CruiseChainDetonator() { }
    private static NBTTagList links(ItemStack stack) { return stack.hasTagCompound()?stack.getTagCompound().getTagList(KEY,10):new NBTTagList(); }
    public static int count(ItemStack stack) { return links(stack).tagCount(); }
    public static void clear(ItemStack stack) { if(stack.hasTagCompound()) stack.getTagCompound().removeTag(KEY); }
    @SubscribeEvent public static void bind(PlayerInteractEvent.RightClickBlock event) {
        if(!UavChainDetonator.isDetonator(event.getItemStack())) return;
        TileEntity value=event.getWorld().getTileEntity(event.getPos());
        if(!(value instanceof TileEntityCruiseLauncher)) return;
        event.setCanceled(true);event.setCancellationResult(EnumActionResult.SUCCESS);
        if(event.getWorld().isRemote) return;
        EntityPlayer player=event.getEntityPlayer();TileEntityCruiseLauncher tile=(TileEntityCruiseLauncher)value;
        if(!player.isSneaking()) { UavChainDetonator.activate(player,event.getItemStack());return; }
        if(!tile.mayUse(player)) { CruiseText.tell(player,"error.owner");return; }
        if(tile.isEmpty() || tile.getLoadId()==null) { CruiseText.tell(player,"launcher.empty");return; }
        ItemStack missile=tile.getStackInSlot(0);
        if(!CruiseMission.fromStack(missile).isValidFor(CruiseBuild.fromStack(missile),player.dimension)) { CruiseText.tell(player,"error.invalid_program");return; }
        ItemStack detonator=event.getItemStack();UavChainDetonator.prepareBinding(detonator);NBTTagList list=links(detonator);
        for(int i=0;i<list.tagCount();i++) if(tile.getLoadId().equals(readId(list.getCompoundTagAt(i)))) { CruiseText.tell(player,"chain.duplicate");return; }
        if(list.tagCount()>=64) { CruiseText.tell(player,"chain.full");return; }
        NBTTagCompound link=new NBTTagCompound();link.setInteger("Dimension",player.dimension);link.setLong("Position",tile.getPos().toLong());link.setUniqueId("LoadId",tile.getLoadId());list.appendTag(link);
        if(!detonator.hasTagCompound()) detonator.setTagCompound(new NBTTagCompound());detonator.getTagCompound().setTag(KEY,list);
        CruiseText.tell(player,"chain.bound",list.tagCount());
    }
    private static UUID readId(NBTTagCompound tag) { return tag.hasUniqueId("LoadId")?tag.getUniqueId("LoadId"):null; }
    private static boolean hasLink(ItemStack stack,UUID id) {
        NBTTagList list=links(stack);for(int i=0;i<list.tagCount();i++) if(id.equals(readId(list.getCompoundTagAt(i)))) return true;return false;
    }
    private static void removeLink(ItemStack stack,UUID id) {
        NBTTagList list=links(stack),keep=new NBTTagList();
        for(int i=0;i<list.tagCount();i++) if(!id.equals(readId(list.getCompoundTagAt(i)))) keep.appendTag(list.getCompoundTagAt(i).copy());
        if(stack.hasTagCompound()) stack.getTagCompound().setTag(KEY,keep);
    }
    public static void launch(EntityPlayerMP player,ItemStack detonator) {
        NBTTagList list=links(detonator),retained=new NBTTagList();int launched=0,skipped=0,queued=0;
        for(int i=0;i<Math.min(64,list.tagCount());i++) {
            NBTTagCompound link=list.getCompoundTagAt(i);WorldServer world=player.getServer().getWorld(link.getInteger("Dimension"));BlockPos position=BlockPos.fromLong(link.getLong("Position"));
            if(world!=null && !world.isBlockLoaded(position)) {
                UUID pendingId=readId(link);
                boolean accepted=pendingId!=null && OperationalChunks.request(world,position,2,"cruise:"+pendingId,
                    ()->!player.isDead && player.connection!=null && hasLink(detonator,pendingId),()->{
                        TileEntity loaded=world.getTileEntity(position);
                        boolean ok=loaded instanceof TileEntityCruiseLauncher && pendingId.equals(((TileEntityCruiseLauncher)loaded).getLoadId())
                            && ((TileEntityCruiseLauncher)loaded).launch(player);
                        if(ok) removeLink(detonator,pendingId);
                        CruiseText.tell(player,ok?"chain.remote_launched":"chain.remote_failed");
                        player.inventory.markDirty();
                    });
                if(accepted) queued++;else skipped++;
                retained.appendTag(link.copy());continue;
            }
            if(world==null) { skipped++;retained.appendTag(link.copy());continue; }
            TileEntity value=world.getTileEntity(position);UUID id=readId(link);
            if(value instanceof TileEntityCruiseLauncher && id!=null && id.equals(((TileEntityCruiseLauncher)value).getLoadId()) && ((TileEntityCruiseLauncher)value).launch(player)) launched++;
            else { skipped++;retained.appendTag(link.copy()); }
        }
        detonator.getTagCompound().setTag(KEY,retained);
        CruiseText.tell(player,"chain.launched",launched,skipped);
        if(queued>0) CruiseText.tell(player,"chain.queued",queued);
    }
}
