package net.minecraft.util;

public class Vec3 {
    public double field_72450_a, field_72448_b, field_72449_c;
    public static Vec3 func_72443_a(double x, double y, double z) {
        Vec3 value = new Vec3();
        value.field_72450_a = x;
        value.field_72448_b = y;
        value.field_72449_c = z;
        return value;
    }
    public double func_72433_c() {
        return Math.sqrt(field_72450_a * field_72450_a
                + field_72448_b * field_72448_b
                + field_72449_c * field_72449_c);
    }
    public Vec3 func_72432_b() {
        double length = func_72433_c();
        return length < 1.0E-8D ? func_72443_a(0.0D, 0.0D, 0.0D)
                : func_72443_a(field_72450_a / length,
                        field_72448_b / length, field_72449_c / length);
    }
}
