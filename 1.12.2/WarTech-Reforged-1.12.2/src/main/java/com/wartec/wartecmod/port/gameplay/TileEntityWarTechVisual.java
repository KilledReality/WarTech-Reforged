package com.wartec.wartecmod.port.gameplay;

import net.minecraft.tileentity.TileEntity;

public class TileEntityWarTechVisual extends TileEntity {
    public net.minecraft.nbt.NBTTagCompound displayPayload;
    @Override public net.minecraft.nbt.NBTTagCompound writeToNBT(net.minecraft.nbt.NBTTagCompound n){super.writeToNBT(n);if(displayPayload!=null)n.setTag("WarTechDisplayPayload",displayPayload.copy());return n;}
    @Override public void readFromNBT(net.minecraft.nbt.NBTTagCompound n){super.readFromNBT(n);displayPayload=n.hasKey("WarTechDisplayPayload",10)?n.getCompoundTag("WarTechDisplayPayload").copy():null;}
}
