package net.minecraft.client.settings;

public class KeyBinding {
    private boolean down;
    private int presses;

    public KeyBinding(String description, int keyCode, String category) {}
    public boolean func_151470_d() { return down; }
    public boolean func_151468_f() {
        if (presses <= 0) return false;
        presses--;
        return true;
    }

    public void stubSetDown(boolean value) { down = value; }
    public void stubPress() { presses++; }
}
