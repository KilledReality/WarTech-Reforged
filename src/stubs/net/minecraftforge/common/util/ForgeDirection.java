package net.minecraftforge.common.util;

public class ForgeDirection {
    public static final ForgeDirection DOWN = new ForgeDirection(0, -1, 0);
    public static final ForgeDirection UP = new ForgeDirection(0, 1, 0);
    public static final ForgeDirection NORTH = new ForgeDirection(0, 0, -1);
    public static final ForgeDirection SOUTH = new ForgeDirection(0, 0, 1);
    public static final ForgeDirection WEST = new ForgeDirection(-1, 0, 0);
    public static final ForgeDirection EAST = new ForgeDirection(1, 0, 0);
    public static final ForgeDirection UNKNOWN = new ForgeDirection(0, 0, 0);
    public static final ForgeDirection[] VALID_DIRECTIONS = {
            DOWN, UP, NORTH, SOUTH, WEST, EAST
    };
    public final int offsetX;
    public final int offsetY;
    public final int offsetZ;

    private ForgeDirection(int x, int y, int z) {
        offsetX = x;
        offsetY = y;
        offsetZ = z;
    }
}
