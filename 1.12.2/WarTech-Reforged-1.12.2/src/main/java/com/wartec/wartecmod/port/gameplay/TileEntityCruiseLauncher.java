package com.wartec.wartecmod.port.gameplay;

import com.wartec.wartecmod.port.content.*;
import com.wartec.wartecmod.port.cruise.*;
import com.wartec.wartecmod.port.entity.EntityCustomCruise;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.*;

/** Stores an actual missile item. Loading never creates a flying entity. */
public final class TileEntityCruiseLauncher extends TileEntity implements IInventory {
    private final NonNullList<ItemStack> inventory=NonNullList.withSize(1,ItemStack.EMPTY);
    private UUID owner,loadId;
    private String team="";
    public UUID getLoadId() { return loadId; }
    @Override public net.minecraft.util.math.AxisAlignedBB getRenderBoundingBox() { return new net.minecraft.util.math.AxisAlignedBB(pos).grow(11); }
    @Override public double getMaxRenderDistanceSquared() { return 16384; }
    public boolean mayUse(EntityPlayer player) {
        return player!=null && (owner==null || owner.equals(player.getUniqueID())
            || com.wartec.wartecmod.port.integration.NetworkTeamHelper.areFriendly(team,
                com.wartec.wartecmod.port.integration.NetworkTeamHelper.getPlayerTeam(player)));
    }
    public void claim(EntityPlayer player) {
        if(world!=null && !world.isRemote && owner==null) {
            owner=player.getUniqueID();team=com.wartec.wartecmod.port.integration.NetworkTeamHelper.getPlayerTeam(player);changed();
        }
    }
    public static boolean accepts(boolean rail,ItemStack stack) {
        if(stack.isEmpty() || stack.getItem()!=WarTechContent.ASSEMBLED_CRUISE) return false;
        CruiseBuild build=CruiseBuild.fromStack(stack);
        return build.calculateStats().isValid() && build.get(CruiseSlot.LAUNCH)==(rail?CruisePartDefinition.LAUNCH_RAIL:CruisePartDefinition.LAUNCH_BOOSTER);
    }
    public boolean load(EntityPlayer player,ItemStack held) {
        if(world==null || world.isRemote || !mayUse(player) || !isEmpty()) return false;
        boolean rail=((CruiseLaunchPointBlock)world.getBlockState(pos).getBlock()).isRail();
        if(!accepts(rail,held)) { CruiseText.tell(player,loadError(held));return false; }
        claim(player);ItemStack copy=held.copy();copy.setCount(1);setInventorySlotContents(0,copy);
        if(!player.capabilities.isCreativeMode) held.shrink(1);
        CruiseText.tell(player,"launcher.loaded");return true;
    }
    public boolean launch(EntityPlayer player) {
        if(world==null || world.isRemote || !mayUse(player) || isEmpty()) return false;
        if(!(world.getBlockState(pos).getBlock() instanceof CruiseLaunchPointBlock)) return false;
        boolean rail=((CruiseLaunchPointBlock)world.getBlockState(pos).getBlock()).isRail();
        ItemStack stack=inventory.get(0);
        if(!accepts(rail,stack)) { CruiseText.tell(player,loadError(stack));return false; }
        CruiseBuild build=CruiseBuild.fromStack(stack);CruisePartDefinition body=build.getAirframe();
        float yaw=world.getBlockState(pos).getValue(CruiseLaunchPointBlock.FACING).getHorizontalAngle();
        Vec3d start=new Vec3d(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5)
                .add(CruiseVisuals.worldOffset(CruiseVisuals.launchOrigin(build),yaw,0));
        String error=EntityCustomCruise.launchError(world,stack,start);
        if(error!=null) { player.sendMessage(new TextComponentTranslation(error));return false; }
        claim(player);
        EntityCustomCruise missile=new EntityCustomCruise(world);missile.configure(stack,null);missile.setOwnerIdentity(owner,team);
        missile.setLocationAndAngles(start.x,start.y,start.z,yaw,rail?-12:-65);
        if(!world.getCollisionBoxes(missile,CruiseVisuals.launchClearance(build,start,yaw)).isEmpty()) { CruiseText.tell(player,"error.clearance");return false; }
        if(!com.wartec.wartecmod.port.integration.MissileChunkLoader.prepare(missile)) {
            player.sendMessage(new TextComponentTranslation("flight.error.chunks"));return false;
        }
        if(!world.spawnEntity(missile)) { com.wartec.wartecmod.port.integration.MissileChunkLoader.untrack(missile);return false; }
        inventory.set(0,ItemStack.EMPTY);loadId=null;changed();return true;
    }
    private void changed() {
        markDirty();
        if(world!=null && !world.isRemote) world.notifyBlockUpdate(pos,world.getBlockState(pos),world.getBlockState(pos),3);
    }
    @Override public int getSizeInventory() { return 1; }
    @Override public boolean isEmpty() { return inventory.get(0).isEmpty(); }
    @Override public ItemStack getStackInSlot(int index) { return index==0?inventory.get(0):ItemStack.EMPTY; }
    @Override public ItemStack decrStackSize(int index,int count) {
        if(index!=0) return ItemStack.EMPTY;
        ItemStack result=ItemStackHelper.getAndSplit(inventory,index,count);
        if(!result.isEmpty()) { loadId=null;changed(); }return result;
    }
    @Override public ItemStack removeStackFromSlot(int index) { return decrStackSize(index,1); }
    @Override public void setInventorySlotContents(int index,ItemStack stack) {
        if(index!=0) return;
        inventory.set(0,stack);if(!stack.isEmpty()) stack.setCount(1);
        loadId=stack.isEmpty()?null:UUID.randomUUID();changed();
    }
    @Override public int getInventoryStackLimit() { return 1; }
    @Override public boolean isUsableByPlayer(EntityPlayer player) {
        return world!=null && world.getTileEntity(pos)==this && mayUse(player) && player.getDistanceSq(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64;
    }
    @Override public String getName() { return "cruise.launcher.title"; }
    @Override public boolean hasCustomName() { return false; }
    @Override public ITextComponent getDisplayName() { return new TextComponentTranslation(getName()); }
    @Override public void openInventory(EntityPlayer player) { }
    @Override public void closeInventory(EntityPlayer player) { }
    public boolean isRail() {
        return world!=null && world.getBlockState(pos).getBlock() instanceof CruiseLaunchPointBlock
            && ((CruiseLaunchPointBlock)world.getBlockState(pos).getBlock()).isRail();
    }
    public String loadError(ItemStack stack) {
        if(stack.isEmpty() || stack.getItem()!=WarTechContent.ASSEMBLED_CRUISE
                || !CruiseBuild.fromStack(stack).calculateStats().isValid()) return "error.invalid_build";
        if(CruiseBuild.fromStack(stack).get(CruiseSlot.LAUNCH)==CruisePartDefinition.LAUNCH_AIR) return "error.ground_air_adapter";
        return isRail()?"error.rail_adapter":"error.booster_adapter";
    }
    @Override public boolean isItemValidForSlot(int slot,ItemStack stack) {
        return slot==0 && world!=null && world.getBlockState(pos).getBlock() instanceof CruiseLaunchPointBlock
            && accepts(isRail(),stack);
    }
    @Override public int getField(int id) { return 0; }
    @Override public void setField(int id,int value) { }
    @Override public int getFieldCount() { return 0; }
    @Override public void clear() { inventory.set(0,ItemStack.EMPTY);loadId=null;changed(); }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);ItemStackHelper.saveAllItems(tag,inventory);
        if(owner!=null) tag.setUniqueId("Owner",owner);
        if(loadId!=null) tag.setUniqueId("LoadId",loadId);
        tag.setString("Team",team);return tag;
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);ItemStackHelper.loadAllItems(tag,inventory);
        owner=tag.hasUniqueId("Owner")?tag.getUniqueId("Owner"):null;
        loadId=tag.hasUniqueId("LoadId")?tag.getUniqueId("LoadId"):null;team=tag.getString("Team");
        if(team.isEmpty() && owner!=null) team="player:"+owner.toString();
    }
    @Override public NBTTagCompound getUpdateTag() { return writeToNBT(new NBTTagCompound()); }
    @Override public SPacketUpdateTileEntity getUpdatePacket() { return new SPacketUpdateTileEntity(pos,0,getUpdateTag()); }
    @Override public void onDataPacket(NetworkManager manager,SPacketUpdateTileEntity packet) { readFromNBT(packet.getNbtCompound()); }
    @Override public boolean shouldRefresh(net.minecraft.world.World w,net.minecraft.util.math.BlockPos p,net.minecraft.block.state.IBlockState oldState,net.minecraft.block.state.IBlockState newState) { return oldState.getBlock()!=newState.getBlock(); }
}
