package org.lwjgl.input;

public final class Keyboard {
    public static final int KEY_W = 17;
    public static final int KEY_R = 19;
    public static final int KEY_A = 30;
    public static final int KEY_S = 31;
    public static final int KEY_D = 32;
    public static final int KEY_LSHIFT = 42;
    public static final int KEY_SPACE = 57;
    public static final int KEY_C = 46;
    public static final int KEY_F = 33;
    public static final int KEY_Z = 44;
    public static final int KEY_X = 45;
    private static final boolean[] DOWN = new boolean[256];
    private static int eventKey;
    private static boolean eventKeyState;

    public static boolean isKeyDown(int key) {
        return key >= 0 && key < DOWN.length && DOWN[key];
    }

    public static void stubSetKeyDown(int key, boolean down) {
        if (key >= 0 && key < DOWN.length) DOWN[key] = down;
        eventKey = key;
        eventKeyState = down;
    }

    public static int getEventKey() { return eventKey; }
    public static boolean getEventKeyState() { return eventKeyState; }
}
