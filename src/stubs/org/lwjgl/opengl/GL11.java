package org.lwjgl.opengl;

public final class GL11 {
    public static void glPushMatrix() {}
    public static void glPopMatrix() {}
    public static void glTranslated(double x, double y, double z) {}
    public static void glTranslatef(float x, float y, float z) {}
    public static void glRotatef(float angle, float x, float y, float z) {}
    public static void glScalef(float x, float y, float z) {}
    public static void glEnable(int capability) {}
    public static void glDisable(int capability) {}
    public static int glGenLists(int range) { return 0; }
    public static void glNewList(int list, int mode) {}
    public static void glEndList() {}
    public static void glCallList(int list) {}
    public static void glPushAttrib(int mask) {}
    public static void glPopAttrib() {}
    public static void glDepthMask(boolean enabled) {}
    public static void glColor4f(float red, float green, float blue, float alpha) {}
    public static void glColor3f(float red, float green, float blue) {}
    public static void glShadeModel(int mode) {}
    public static void glBegin(int mode) {}
    public static void glEnd() {}
    public static void glVertex3f(float x, float y, float z) {}
}
