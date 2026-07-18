package com.wartec.wartecmod.compat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

public interface IRadarGuiTarget extends IInventory {
    Entity wartecGetEntity();
    int wartecGetPower();
    void wartecSetPower(int power);
    int wartecGetCapacity();
    int wartecGetContacts();
    int wartecGetRange();
    int wartecGetCeiling();
    int wartecGetBlipCount();
    int wartecGetPackedBlip(int index);
    boolean wartecIsEnabled();
    boolean wartecIsOperational();
    boolean wartecToggle(EntityPlayer player);
    String wartecGetRadarName();
}
