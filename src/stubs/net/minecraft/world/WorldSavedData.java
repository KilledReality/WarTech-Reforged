package net.minecraft.world;

import net.minecraft.nbt.NBTTagCompound;

public abstract class WorldSavedData {
    public WorldSavedData(String name) {
    }

    public abstract void func_76184_a(NBTTagCompound tag);
    public abstract void func_76187_b(NBTTagCompound tag);
    public void func_76185_a() {
    }
}
