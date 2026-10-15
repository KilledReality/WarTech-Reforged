package com.wartec.wartecmod.port.silo;

import com.wartec.wartecmod.port.gameplay.TileEntityMissileSilo;
import com.wartec.wartecmod.port.integration.*;
import net.minecraft.entity.player.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

public final class SiloDetonator {
    public static final String KEY="WarTechSiloLinks";
    private SiloDetonator(){}
    private static NBTTagList links(ItemStack s){return s.hasTagCompound()?s.getTagCompound().getTagList(KEY,10):new NBTTagList();}
    public static int count(ItemStack s){return links(s).tagCount();}
    public static void clear(ItemStack s){if(s.hasTagCompound())s.getTagCompound().removeTag(KEY);}
    public static void bind(EntityPlayer p,ItemStack s,TileEntityMissileSilo t){if(!t.mayUse(p) || !t.isStarter() || !t.hasTarget()){t.tell(p,"silo.error.target");return;}
        UavChainDetonator.prepareBinding(s);NBTTagList list=links(s);for(int i=0;i<list.tagCount();i++)if(list.getCompoundTagAt(i).getUniqueId("Id").equals(t.instance()))return;if(list.tagCount()>=64)return;
        NBTTagCompound link=new NBTTagCompound();link.setUniqueId("Id",t.instance());link.setLong("Pos",t.getPos().toLong());link.setInteger("Dimension",p.dimension);list.appendTag(link);
        if(!s.hasTagCompound())s.setTagCompound(new NBTTagCompound());s.getTagCompound().setTag(KEY,list);p.inventory.markDirty();t.tell(p,"silo.bound");
    }
    public static void launch(EntityPlayerMP p,ItemStack s){NBTTagList list=links(s),keep=new NBTTagList();
        for(int i=0;i<Math.min(64,list.tagCount());i++){NBTTagCompound n=list.getCompoundTagAt(i);BlockPos pos=BlockPos.fromLong(n.getLong("Pos"));boolean ok=false;
            if(n.getInteger("Dimension")==p.dimension && p.world.isBlockLoaded(pos)){
                TileEntity t=p.world.getTileEntity(pos);if(t instanceof TileEntityMissileSilo && ((TileEntityMissileSilo)t).instance().equals(n.getUniqueId("Id")))ok=((TileEntityMissileSilo)t).beginLaunch(p);
            }
            if(!ok){keep.appendTag(n.copy());p.sendStatusMessage(new net.minecraft.util.text.TextComponentTranslation("silo.error.remote"),true);}
        }if(s.hasTagCompound())s.getTagCompound().setTag(KEY,keep);p.inventory.markDirty();
    }
}
