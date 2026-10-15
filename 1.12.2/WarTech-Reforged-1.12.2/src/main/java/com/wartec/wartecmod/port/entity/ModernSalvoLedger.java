package com.wartec.wartecmod.port.entity;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;

/** World ledger reconciles a saved queue with shots already admitted before reload. */
public final class ModernSalvoLedger extends WorldSavedData {
    private static final String NAME="wartech_modern_salvos";
    private final Map<UUID,Long> shots=new LinkedHashMap<>();
    public ModernSalvoLedger() { super(NAME); }
    public ModernSalvoLedger(String name) { super(name); }
    private static ModernSalvoLedger get(World world) {
        ModernSalvoLedger data=(ModernSalvoLedger)world.getPerWorldStorage().getOrLoadData(ModernSalvoLedger.class,NAME);
        if(data==null) { data=new ModernSalvoLedger();world.getPerWorldStorage().setData(NAME,data); }
        return data;
    }
    public static boolean contains(World world,UUID shot) { return get(world).shots.containsKey(shot); }
    public static void commit(World world,UUID shot) {
        ModernSalvoLedger data=get(world);long tick=world.getTotalWorldTime();
        data.shots.entrySet().removeIf(entry->tick-entry.getValue()>24000);
        data.shots.put(shot,tick);data.markDirty();
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        shots.clear();NBTTagList list=tag.getTagList("Shots",10);
        for(int i=0;i<list.tagCount();i++) { NBTTagCompound row=list.getCompoundTagAt(i);if(row.hasUniqueId("Id")) shots.put(row.getUniqueId("Id"),row.getLong("Tick")); }
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        NBTTagList list=new NBTTagList();for(Map.Entry<UUID,Long> entry:shots.entrySet()) {
            NBTTagCompound row=new NBTTagCompound();row.setUniqueId("Id",entry.getKey());row.setLong("Tick",entry.getValue());list.appendTag(row);
        }
        tag.setTag("Shots",list);return tag;
    }
}
