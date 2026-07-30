package net.minecraft.nbt;

import java.util.ArrayList;
import java.util.List;

public class NBTTagList extends NBTBase {
    private final List<NBTBase> values = new ArrayList<NBTBase>();
    public int func_74745_c() { return values.size(); }
    public NBTTagCompound func_150305_b(int index) {
        NBTBase value = values.get(index);
        return value instanceof NBTTagCompound
                ? (NBTTagCompound) value : new NBTTagCompound();
    }
    public void func_74742_a(NBTBase value) { values.add(value); }
}
