package net.minecraft.util;

public final class MathHelper {
    private MathHelper() {}

    public static float func_76133_a(double value) {
        return (float) Math.sqrt(value);
    }

    public static int func_76128_c(double value) {
        return (int) Math.floor(value);
    }

    public static int func_76123_f(float value) {
        return (int) Math.ceil(value);
    }
}
