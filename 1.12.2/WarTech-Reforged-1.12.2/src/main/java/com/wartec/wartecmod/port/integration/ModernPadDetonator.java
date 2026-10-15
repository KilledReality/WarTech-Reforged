package com.wartec.wartecmod.port.integration;

import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.gameplay.TileEntityWarTechMachine;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.entity.player.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.*;

/** Core-addressed FP-5/FP-7 bindings survive dummy blocks and unloaded launch sites. */
public final class ModernPadDetonator {
    public static final String KEY="WarTechModernPads";
    private ModernPadDetonator() {}
    private static NBTTagList links(ItemStack stack) { return stack.hasTagCompound()?stack.getTagCompound().getTagList(KEY,10):new NBTTagList(); }
    public static int count(ItemStack stack) { return links(stack).tagCount(); }
    public static void clear(ItemStack stack) { if(stack.hasTagCompound()) stack.getTagCompound().removeTag(KEY); }
    public static TileEntityWarTechMachine site(World world,BlockPos pos) {
        Block block=world.getBlockState(pos).getBlock();
        TileEntity value=block instanceof LegacyLauncherBlock?((LegacyLauncherBlock)block).findMachineAt(world,pos):world.getTileEntity(pos);
        if(!(value instanceof TileEntityWarTechMachine)) return null;
        TileEntityWarTechMachine tile=(TileEntityWarTechMachine)value;
        return tile.isGeranLauncher() || tile.isBallisticLauncher()?tile:null;
    }
    public static void bind(EntityPlayer player,ItemStack stack,TileEntityWarTechMachine tile) {
        ItemStack ammo=tile.getStackInSlot(0),target=tile.getStackInSlot(1);
        if(!tile.mayRemoteLaunch(player)) { tell(player,"denied");return; }
        if(ammo.isEmpty() || ammo.getItem()!=WarTechContent.FP5_MISSILE && ammo.getItem()!=WarTechContent.FP7_MISSILE
            || DesignatorCompat.getHorizontalTarget(target)==null) { tell(player,"load");return; }
        if(!tile.getTileData().hasUniqueId("ModernPadId")) { tile.getTileData().setUniqueId("ModernPadId",UUID.randomUUID());tile.markDirty(); }
        UUID id=tile.getTileData().getUniqueId("ModernPadId");
        UavChainDetonator.prepareBinding(stack);NBTTagList list=links(stack);
        for(int i=0;i<list.tagCount();i++) if(id.equals(list.getCompoundTagAt(i).getUniqueId("Id"))) { tell(player,"duplicate");return; }
        if(list.tagCount()>=64) { tell(player,"full");return; }
        NBTTagCompound link=new NBTTagCompound();link.setUniqueId("Id",id);link.setInteger("Dimension",player.dimension);
        link.setLong("Position",tile.getPos().toLong());link.setTag("Ammo",ammo.writeToNBT(new NBTTagCompound()));link.setTag("Target",target.writeToNBT(new NBTTagCompound()));
        list.appendTag(link);if(!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());stack.getTagCompound().setTag(KEY,list);tell(player,"bound");
    }
    private static boolean contains(ItemStack stack,UUID id) {
        NBTTagList list=links(stack);for(int i=0;i<list.tagCount();i++) if(id.equals(list.getCompoundTagAt(i).getUniqueId("Id"))) return true;return false;
    }
    private static void remove(ItemStack stack,UUID id) {
        NBTTagList retained=new NBTTagList(),list=links(stack);
        for(int i=0;i<list.tagCount();i++) if(!id.equals(list.getCompoundTagAt(i).getUniqueId("Id"))) retained.appendTag(list.getCompoundTagAt(i).copy());
        if(stack.hasTagCompound()) stack.getTagCompound().setTag(KEY,retained);
    }
    public static void launch(EntityPlayerMP player,ItemStack stack) {
        NBTTagList list=links(stack).copy();
        for(int i=0;i<Math.min(64,list.tagCount());i++) {
            NBTTagCompound link=list.getCompoundTagAt(i);if(!link.hasUniqueId("Id")) continue;
            UUID id=link.getUniqueId("Id");WorldServer world=player.getServer().getWorld(link.getInteger("Dimension"));
            if(world==null || world!=player.world) { tell(player,"failed");continue; }
            BlockPos pos=BlockPos.fromLong(link.getLong("Position"));
            boolean queued=OperationalChunks.request(world,pos,2,"modern-pad:"+id,
                ()->!player.isDead && player.connection!=null && contains(stack,id),()->{
                    TileEntityWarTechMachine tile=site(world,pos);
                    if(tile==null || !id.equals(tile.getTileData().getUniqueId("ModernPadId"))
                        || !ItemStack.areItemStacksEqual(tile.getStackInSlot(0),new ItemStack(link.getCompoundTag("Ammo")))
                        || !ItemStack.areItemStacksEqual(tile.getStackInSlot(1),new ItemStack(link.getCompoundTag("Target")))) { tell(player,"failed");return; }
                    boolean accepted=tile.launchFromDetonator(player,()->contains(stack,id),success->{
                        if(success) remove(stack,id);tell(player,success?"launched":"failed");player.inventory.markDirty();
                    });
                    if(!accepted) tell(player,"failed");
                });
            if(!queued) tell(player,"failed");
        }
    }
    private static void tell(EntityPlayer player,String key) { player.sendMessage(new TextComponentTranslation("wartec.modern.detonator."+key)); }
}
