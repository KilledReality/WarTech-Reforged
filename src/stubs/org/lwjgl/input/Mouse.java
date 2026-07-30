package org.lwjgl.input;

public final class Mouse {
    private static final boolean[] DOWN = new boolean[16];
    private static int eventButton = -1;
    private static boolean eventButtonState;

    private Mouse() {
    }

    public static boolean isButtonDown(int button) {
        return button >= 0 && button < DOWN.length && DOWN[button];
    }

    public static int getEventButton() {
        return eventButton;
    }

    public static boolean getEventButtonState() {
        return eventButtonState;
    }

    public static void stubSetButtonDown(int button, boolean down) {
        if (button >= 0 && button < DOWN.length) DOWN[button] = down;
        eventButton = button;
        eventButtonState = down;
    }
}
