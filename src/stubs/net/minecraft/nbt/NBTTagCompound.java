package net.minecraft.nbt;

import java.util.HashMap;
import java.util.Map;

public class NBTTagCompound extends NBTBase {
    private final Map<String, Object> values = new HashMap<String, Object>();

    public int func_74762_e(String key) { return number(key).intValue(); }
    public void func_74768_a(String key, int value) { values.put(key, Integer.valueOf(value)); }
    public void func_74757_a(String key, boolean value) { values.put(key, Boolean.valueOf(value)); }
    public boolean func_74767_n(String key) {
        Object value = values.get(key);
        return value instanceof Boolean && ((Boolean) value).booleanValue();
    }
    public void func_74780_a(String key, double value) { values.put(key, Double.valueOf(value)); }
    public double func_74769_h(String key) { return number(key).doubleValue(); }
    public void func_74782_a(String key, NBTBase value) { values.put(key, value); }
    public boolean func_74764_b(String key) { return values.containsKey(key); }
    public NBTTagCompound func_74775_l(String key) {
        Object value = values.get(key);
        return value instanceof NBTTagCompound ? (NBTTagCompound) value : new NBTTagCompound();
    }
    public NBTBase func_74737_b() {
        NBTTagCompound copy = new NBTTagCompound();
        copy.values.putAll(values);
        return copy;
    }
    public void func_74774_a(String key, byte value) { values.put(key, Byte.valueOf(value)); }
    public byte func_74771_c(String key) { return number(key).byteValue(); }
    public void func_74772_a(String key, long value) { values.put(key, Long.valueOf(value)); }
    public long func_74763_f(String key) { return number(key).longValue(); }
    public void func_74778_a(String key, String value) { values.put(key, value); }
    public String func_74779_i(String key) {
        Object value = values.get(key);
        return value instanceof String ? (String) value : "";
    }

    private Number number(String key) {
        Object value = values.get(key);
        return value instanceof Number ? (Number) value : Integer.valueOf(0);
    }
}
