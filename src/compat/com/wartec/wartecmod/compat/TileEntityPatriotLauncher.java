package com.wartec.wartecmod.compat;

import com.wartec.wartecmod.tileentity.vls.TileEntityVlsExhaust;
import net.minecraft.nbt.NBTTagCompound;

public final class TileEntityPatriotLauncher extends TileEntityVlsExhaust
        implements ITeamOwned {
    private boolean cleanedLegacyHeight;
    private String ownerTeam = "";

    @Override
    public String getOwnerTeam() {
        return ownerTeam;
    }

    @Override
    public void setOwnerTeam(String team) {
        ownerTeam = team == null ? "" : team;
        func_70296_d();
    }

    @Override
    public void func_145845_h() {
        super.func_145845_h();
        if (!cleanedLegacyHeight && field_145850_b != null && !field_145850_b.field_72995_K) {
            cleanedLegacyHeight = true;
            clearLegacyTop(PatriotContent.patriotLauncher);
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

    @Override
    public void func_145841_b(NBTTagCompound tag) {
        super.func_145841_b(tag);
        tag.func_74778_a("WarTechOwnerTeam", ownerTeam);
    }

    @Override
    public void func_145839_a(NBTTagCompound tag) {
        super.func_145839_a(tag);
        ownerTeam = tag.func_74779_i("WarTechOwnerTeam");
    }
}
