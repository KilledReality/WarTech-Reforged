package com.wartec.wartecmod.savedata.satellites;

import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import com.wartec.wartecmod.compat.MissileTrackingService;
import com.wartec.wartecmod.entity.missile.EntityKineticRod;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

/** Four-shot, coordinate-controlled kinetic bombardment satellite. */
public final class SatelliteKinetic extends Satellite {
    public static final int ROD_CAPACITY = 4;
    public static final int COOLDOWN_TICKS = 1200;

    private int rodsLeft = ROD_CAPACITY;
    private long nextStrikeTick;

    public SatelliteKinetic() {
        satIface = Interfaces.SAT_COORD;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.func_74768_a("Rods", rodsLeft);
        tag.func_74772_a("NextStrike", nextStrikeTick);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        rodsLeft = tag.func_74764_b("Rods")
                ? Math.max(0, Math.min(ROD_CAPACITY, tag.func_74762_e("Rods")))
                : ROD_CAPACITY;
        nextStrikeTick = tag.func_74764_b("NextStrike")
                ? tag.func_74763_f("NextStrike") : 0L;
    }

    @Override
    public void onCoordAction(World world, EntityPlayer player, int x, int y, int z) {
        if (world == null || world.field_72995_K) {
            return;
        }
        if (rodsLeft <= 0) {
            message(player, "Orbital platform: no kinetic rods remaining.");
            return;
        }
        long now = world.func_82737_E();
        if (now < nextStrikeTick) {
            long seconds = (nextStrikeTick - now + 19L) / 20L;
            message(player, "Orbital platform cooling down: " + seconds + " s.");
            return;
        }

        world.func_72964_e(x >> 4, z >> 4);
        int groundY = world.func_72976_f(x, z);
        EntityKineticRod rod = new EntityKineticRod(world, x, groundY, z);
        if (!world.func_72838_d(rod)) {
            message(player, "Orbital strike failed to deploy.");
            return;
        }

        MissileTrackingService.registerLaunch(rod, rod.field_70165_t,
                rod.field_70163_u, rod.field_70161_v, x, z);
        rodsLeft--;
        nextStrikeTick = now + COOLDOWN_TICKS;
        SatelliteSavedData.getData(world).func_76185_a();
        message(player, "Kinetic rod released. Remaining payload: " + rodsLeft + ".");
    }

    public int getRodsLeft() {
        return rodsLeft;
    }

    public long getNextStrikeTick() {
        return nextStrikeTick;
    }

    private static void message(EntityPlayer player, String text) {
        if (player != null) {
            player.func_145747_a(new ChatComponentText(text));
        }
    }
}
