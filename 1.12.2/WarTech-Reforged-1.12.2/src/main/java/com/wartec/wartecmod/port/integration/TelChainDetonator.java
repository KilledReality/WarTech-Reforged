package com.wartec.wartecmod.port.integration;

import com.wartec.wartecmod.port.entity.EntityStrategicTel;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;

/** UUID bindings coexist with the existing NTM block, UAV and cruise groups. */
public final class TelChainDetonator {
    public static final String KEY="WarTechTelLaunchers";
    private TelChainDetonator() { }
    private static NBTTagList links(ItemStack stack) {
        return stack.hasTagCompound()?stack.getTagCompound().getTagList(KEY,10):new NBTTagList();
    }
    public static int count(ItemStack stack) { return links(stack).tagCount(); }
    public static void clear(ItemStack stack) { if(stack.hasTagCompound()) stack.getTagCompound().removeTag(KEY); }
    private static NBTTagCompound tag(ItemStack stack) {
        if(!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());return stack.getTagCompound();
    }
    private static UUID id(NBTTagCompound link) { return link.hasUniqueId("Tel")?link.getUniqueId("Tel"):null; }
    private static boolean contains(ItemStack stack,UUID target) {
        NBTTagList list=links(stack);for(int i=0;i<list.tagCount();i++) if(target.equals(id(list.getCompoundTagAt(i)))) return true;return false;
    }
    private static void remove(ItemStack stack,UUID target) {
        NBTTagList keep=new NBTTagList(),list=links(stack);
        for(int i=0;i<list.tagCount();i++) if(!target.equals(id(list.getCompoundTagAt(i)))) keep.appendTag(list.getCompoundTagAt(i).copy());
        tag(stack).setTag(KEY,keep);
    }
    private static boolean held(EntityPlayer player,ItemStack stack) {
        return player.getHeldItemMainhand()==stack || player.getHeldItemOffhand()==stack;
    }
    private static void tell(EntityPlayer player,String key,Object... args) {
        player.sendMessage(new TextComponentTranslation("wartec.tel.chain."+key,args));
    }
    public static void bind(EntityPlayer player,ItemStack detonator,EntityStrategicTel tel) {
        if(player==null || tel==null || tel.world.isRemote || !UavChainDetonator.isDetonator(detonator)) return;
        if(tel.getStrategicSystem().isTactical()) { tell(player,"strategic_only");return; }
        if(!player.isSneaking()) { UavChainDetonator.activate(player,detonator);return; }
        if(!tel.canOperate(player)) { tell(player,"denied");return; }
        if(tel.getLaunchTicks()>0 || !tel.isMissileLoaded() || !tel.hasTarget(0)) { tell(player,"not_ready");return; }
        if(contains(detonator,tel.getUniqueID())) { tell(player,"duplicate");return; }
        if(UavChainDetonator.isMultiDetonator(detonator) && count(detonator)>=64) { tell(player,"full");return; }
        UavChainDetonator.prepareBinding(detonator);NBTTagList list=links(detonator);
        NBTTagCompound link=new NBTTagCompound();link.setUniqueId("Tel",tel.getUniqueID());
        link.setInteger("Dimension",tel.dimension);link.setLong("Position",tel.getPosition().toLong());list.appendTag(link);
        tag(detonator).setTag(KEY,list);tell(player,"bound",tel.getName(),list.tagCount());player.inventory.markDirty();
    }
    public static void launch(EntityPlayerMP player,ItemStack detonator) {
        if(player==null || player.world.isRemote || !UavChainDetonator.isDetonator(detonator) || !held(player,detonator)) return;
        NBTTagList list=links(detonator),keep=new NBTTagList();int started=0,skipped=0,queued=0;
        for(int i=0;i<Math.min(64,list.tagCount());i++) {
            NBTTagCompound link=list.getCompoundTagAt(i);UUID target=id(link);
            WorldServer world=player.getServer()==null?null:player.getServer().getWorld(link.getInteger("Dimension"));
            if(target==null || world==null || player.world!=world) { skipped++;keep.appendTag(link.copy());continue; }
            Entity entity=world.getEntityFromUuid(target);
            if(entity instanceof EntityStrategicTel) {
                if(((EntityStrategicTel)entity).beginLaunchFromDetonator(player)) started++;
                else { skipped++;keep.appendTag(link.copy()); }
                continue;
            }
            BlockPos site=BlockPos.fromLong(link.getLong("Position"));
            boolean accepted=OperationalChunks.requestUntil(world,site,2,"tel:"+player.getUniqueID()+":"+target,
                ()->!player.isDead && player.connection!=null && held(player,detonator) && contains(detonator,target),()->{
                    Entity loaded=world.getEntityFromUuid(target);
                    if(loaded==null) return false; // Async entity loading may follow terrain loading.
                    boolean ok=loaded instanceof EntityStrategicTel && ((EntityStrategicTel)loaded).beginLaunchFromDetonator(player);
                    if(ok) remove(detonator,target);tell(player,ok?"remote_launched":"remote_failed");player.inventory.markDirty();return true;
                });
            if(accepted) queued++;else skipped++;keep.appendTag(link.copy());
        }
        tag(detonator).setTag(KEY,keep);tell(player,"launched",started,skipped);
        if(queued>0) tell(player,"queued",queued);player.inventory.markDirty();
    }
}
