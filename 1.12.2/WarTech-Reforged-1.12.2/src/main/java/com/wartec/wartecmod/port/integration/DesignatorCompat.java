package com.wartec.wartecmod.port.integration;

import com.wartec.wartecmod.port.content.WarTechContent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Reads both NTM Extended 1.12 designators and the WarTech target finder.
 */
public final class DesignatorCompat {
    private static final String TARGET_SET = "WarTechPortTargetSet";
    private static final String TARGET_X = "WarTechPortTargetX";
    private static final String TARGET_Y = "WarTechPortTargetY";
    private static final String TARGET_Z = "WarTechPortTargetZ";

    private DesignatorCompat() {
    }

    /** X/Z-only HBM designators are valid inputs; elevation is resolved on the server. */
    public static net.minecraft.util.math.Vec3d getHorizontalTarget(ItemStack stack) {
        if(!isDesignator(stack) || !stack.hasTagCompound()) return null;
        NBTTagCompound tag=stack.getTagCompound();
        for(String[] keys:new String[][]{{"xCoord","zCoord"},{"x","z"},{"targetX","targetZ"}}) {
            if(!tag.hasKey(keys[0],99) || !tag.hasKey(keys[1],99)) continue;
            double x=tag.getDouble(keys[0]),z=tag.getDouble(keys[1]);
            if(Double.isFinite(x) && Double.isFinite(z) && Math.abs(x)<=29999984 && Math.abs(z)<=29999984)
                return new net.minecraft.util.math.Vec3d(x,0,z);
        }return null;
    }
    public static boolean resolveSavedTarget(net.minecraft.world.WorldServer world,ItemStack stack,double explicitY,
            java.util.function.BooleanSupplier valid,java.util.function.Consumer<net.minecraft.util.math.Vec3d> complete) {
        net.minecraft.util.math.Vec3d point=getSavedTarget(world,stack,explicitY);
        if(point!=null) { if(valid.getAsBoolean()) complete.accept(point);return true; }
        net.minecraft.util.math.Vec3d horizontal=getHorizontalTarget(stack);
        if(horizontal==null) return false;
        return OperationalChunks.request(world,new BlockPos(horizontal),0,"elevation:"+java.util.UUID.randomUUID(),
            ()->valid.getAsBoolean() && horizontal.equals(getHorizontalTarget(stack)),()->{
                net.minecraft.util.math.Vec3d resolved=getSavedTarget(world,stack,Double.NaN);
                if(resolved!=null) complete.accept(resolved);
            });
    }
    /** Right-click import remains valid only while the same item and nearby carrier remain. */
    public static boolean resolveForEntity(net.minecraft.entity.Entity entity,EntityPlayer player,
            net.minecraft.util.EnumHand hand,java.util.function.Consumer<BlockPos> complete) {
        if(!(entity.world instanceof net.minecraft.world.WorldServer)) return false;
        ItemStack stack=player.getHeldItem(hand);
        return resolveSavedTarget((net.minecraft.world.WorldServer)entity.world,stack,Double.NaN,
            ()->!entity.isDead && !player.isDead && entity.world==player.world
                && entity.getDistanceSq(player)<=64 && player.getHeldItem(hand)==stack,
            point->complete.accept(new BlockPos(point)));
    }

    public static boolean isDesignator(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() == WarTechContent.ITEM_TARGET_FINDER
                || stack.getItem()
                        == WarTechContent.ITEM_MISSILE_STRIKE_CALLER) {
            return true;
        }
        String className = stack.getItem().getClass().getName();
        return "com.hbm.items.tool.ItemDesignator".equals(className)
                || "com.hbm.items.tool.ItemDesignatorManual".equals(className)
                || "com.hbm.items.tool.ItemDesignatorRange".equals(className)
                || "com.hbm.items.tool.ItemDesignatorArtyRange".equals(className)
                || className.endsWith(".ItemDesignator")
                || className.endsWith("DesignatorItem");
    }

    /** Complete stored coordinates only; never queries/generates a distant chunk. */
    public static net.minecraft.util.math.Vec3d getSavedTarget(ItemStack stack) {
        if(!isDesignator(stack) || !stack.hasTagCompound()) return null;
        NBTTagCompound tag=stack.getTagCompound();
        for(String[] keys:new String[][]{{"xCoord","yCoord","zCoord"},{"x","y","z"},{"targetX","targetY","targetZ"}}) {
            if(tag.hasKey("WarTechResolvedX",99) && (tag.getDouble("WarTechResolvedX")!=tag.getDouble(keys[0])
                    || tag.getDouble("WarTechResolvedZ")!=tag.getDouble(keys[2]))) continue;
            if(tag.hasKey(keys[0],99) && tag.hasKey(keys[1],99) && tag.hasKey(keys[2],99)) {
                double x=tag.getDouble(keys[0]),y=tag.getDouble(keys[1]),z=tag.getDouble(keys[2]);
                if(Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z) && y>=0 && y<=255
                        && Math.abs(x)<=29999984 && Math.abs(z)<=29999984)
                    return new net.minecraft.util.math.Vec3d(x,y,z);
            }
        }
        return null;
    }

    /** HBM long-range items intentionally store X/Z only. Resolve Y without loading chunks. */
    public static net.minecraft.util.math.Vec3d getSavedTarget(World world,ItemStack stack,double explicitY) {
        net.minecraft.util.math.Vec3d complete=getSavedTarget(stack);
        if(complete!=null) return complete;
        if(!isDesignator(stack) || !stack.hasTagCompound()) return null;
        NBTTagCompound tag=stack.getTagCompound();
        for(String[] keys:new String[][]{{"xCoord","yCoord","zCoord"},{"x","y","z"},{"targetX","targetY","targetZ"}}) {
            if(!tag.hasKey(keys[0],99) || !tag.hasKey(keys[2],99)) continue;
            double x=tag.getDouble(keys[0]),z=tag.getDouble(keys[2]);
            if(!Double.isFinite(x) || !Double.isFinite(z) || Math.abs(x)>29999984 || Math.abs(z)>29999984) return null;
            double y=explicitY;
            BlockPos column=new BlockPos(x,0,z);
            if(!Double.isFinite(y)) {
                if(world==null || !world.isBlockLoaded(column)) return null;
                y=Math.max(0,world.getHeight(column).getY()-1);
            }
            if(y<0 || y>255) return null;
            tag.setDouble(keys[1],y);
            tag.setDouble("WarTechResolvedX",x);tag.setDouble("WarTechResolvedZ",z);
            return new net.minecraft.util.math.Vec3d(x,y,z);
        }
        return null;
    }

    public static BlockPos getTarget(World world, EntityPlayer player,
            ItemStack stack) {
        if (world == null || !isDesignator(stack)) {
            return null;
        }
        net.minecraft.util.math.Vec3d stored = getSavedTarget(world, stack.copy(), Double.NaN);
        if (stored != null) return new BlockPos(stored);
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && (tag.hasKey("xCoord", 99) || tag.hasKey("targetX", 99)
                || tag.hasKey("x", 99))) return null;
        if (player != null) {
            NBTTagCompound playerData = player.getEntityData();
            if (playerData.getBoolean(TARGET_SET)) {
                return new BlockPos(playerData.getInteger(TARGET_X),
                        playerData.getInteger(TARGET_Y),
                        playerData.getInteger(TARGET_Z));
            }
        }
        return null;
    }
}
