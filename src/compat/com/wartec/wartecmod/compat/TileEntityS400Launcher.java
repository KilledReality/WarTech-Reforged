package com.wartec.wartecmod.compat;

import com.wartec.wartecmod.tileentity.vls.TileEntityVlsExhaust;

public final class TileEntityS400Launcher extends TileEntityVlsExhaust {
    private boolean cleanedLegacyHeight;

    @Override
    public void func_145845_h() {
        super.func_145845_h();
        if (!cleanedLegacyHeight && field_145850_b != null && !field_145850_b.field_72995_K) {
            cleanedLegacyHeight = true;
            clearLegacyTop(PatriotContent.s400Launcher);
        }
    }

    private void clearLegacyTop(net.minecraft.block.Block ownBlock) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = 8; dy <= 10; dy++) {
                    int x = field_145851_c + dx;
                    int y = field_145848_d + dy;
                    int z = field_145849_e + dz;
                    if (field_145850_b.func_147439_a(x, y, z) == ownBlock) {
                        field_145850_b.func_147468_f(x, y, z);
                    }
                }
            }
        }
    }
}
