package com.wartec.wartecmod.port.network;

/** North is negative Z; one block has the same scale along both map axes. */
public final class CommandMapViewport {
    public static final int WIDTH = 320, HEIGHT = 180;
    public static final int[] RADII = {64,128,256,512,1024,2048,4096,8192,16384};
    private CommandMapViewport() { }
    public static double pixelX(double x, double center, int radius) { return WIDTH*.5+(x-center)*WIDTH/(2.0*radius); }
    public static double pixelY(double z, double center, int radius) { return HEIGHT*.5+(z-center)*WIDTH/(2.0*radius); }
    public static double worldX(double pixel, double center, int radius) { return center+(pixel-WIDTH*.5)*2.0*radius/WIDTH; }
    public static double worldZ(double pixel, double center, int radius) { return center+(pixel-HEIGHT*.5)*2.0*radius/WIDTH; }
    public static int fit(FactionCommandSnapshot.Node[] nodes, double x, double z) {
        double extent=64;
        for(FactionCommandSnapshot.Node n:nodes) extent=Math.max(extent,Math.max(Math.abs(n.x-x),Math.abs(n.z-z)*WIDTH/HEIGHT)*1.2);
        for(int i=0;i<RADII.length;i++) if(RADII[i]>=extent) return i;
        return RADII.length-1;
    }
}
