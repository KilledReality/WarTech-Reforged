package com.wartec.wartecmod.tileentity.vls;

import com.wartec.wartecmod.port.gameplay.TileEntityWarTechMachine;

/**
 * Original dev66 type name retained for missile constructor compatibility.
 */
public class TileEntityVlsExhaust extends TileEntityWarTechMachine {
    @Override
    public double getMaxRenderDistanceSquared() {
        return 65536.0D;
    }
}
