package net.minecraft.entity;

import java.util.HashMap;
import java.util.Map;

public class DataWatcher {
    private final Map<Integer, Object> values = new HashMap<Integer, Object>();
    public void func_75682_a(int id, Object value) { values.put(Integer.valueOf(id), value); }
    public void func_75692_b(int id, Object value) { values.put(Integer.valueOf(id), value); }
    public byte func_75683_a(int id) {
        Object value = values.get(Integer.valueOf(id));
        return value instanceof Number ? ((Number) value).byteValue() : 0;
    }
    public int func_75679_c(int id) {
        Object value = values.get(Integer.valueOf(id));
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }
    public float func_111145_d(int id) {
        Object value = values.get(Integer.valueOf(id));
        return value instanceof Number ? ((Number) value).floatValue() : 0.0F;
    }
}
