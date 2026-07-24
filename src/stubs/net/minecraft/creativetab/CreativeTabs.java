package net.minecraft.creativetab;

import net.minecraft.item.Item;

public class CreativeTabs {
    public static CreativeTabs[] field_78032_a = new CreativeTabs[0];
    private int field_78033_n;
    private final String field_78034_o;

    public CreativeTabs(String label) {
        field_78033_n = field_78032_a.length;
        field_78034_o = label;
        CreativeTabs[] next = new CreativeTabs[field_78032_a.length + 1];
        System.arraycopy(field_78032_a, 0, next, 0, field_78032_a.length);
        next[field_78033_n] = this;
        field_78032_a = next;
    }
    public int func_78021_a() { return field_78033_n; }
    public String func_78024_c() { return field_78034_o; }
    public Item func_78016_d() { return null; }
}
