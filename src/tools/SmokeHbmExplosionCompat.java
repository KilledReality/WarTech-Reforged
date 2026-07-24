import java.util.Random;
import java.util.Collections;

import com.wartec.wartecmod.compat.HbmExplosionCompat;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

public final class SmokeHbmExplosionCompat {

    public static void main(String[] args) {
        TestWorld world = new TestWorld();
        HbmExplosionCompat.burn(world, 10, 64, 10, 8);
        if (world.firePlacements <= 0) {
            throw new AssertionError("Fallback ignition did not place any fire");
        }
        HbmExplosionCompat.spawnChlorine(world, 10.0D, 64.0D, 10.0D,
                750, 2.5D, 0);
        if (world.sounds <= 0) {
            throw new AssertionError("Gas micro impact did not produce feedback");
        }
        HbmExplosionCompat.neutronMicroImpact(world, 10.0D, 64.0D, 10.0D);
        if (world.explosions != 1) {
            throw new AssertionError("Neutron micro impact must use one bounded explosion");
        }
        System.out.println("HBM-version-neutral explosion compatibility smoke test passed");
    }

    private static final class TestWorld extends World {
        int firePlacements;
        int sounds;
        int explosions;

        TestWorld() {
            field_73012_v = new Random(42L);
        }

        @Override
        public boolean func_147437_c(int x, int y, int z) {
            return y >= 64;
        }

        @Override
        public boolean func_147465_d(int x, int y, int z, Block block,
                int metadata, int flags) {
            firePlacements++;
            return true;
        }

        @Override
        public void func_72908_a(double x, double y, double z, String sound,
                float volume, float pitch) {
            sounds++;
        }

        @Override
        public Explosion func_72885_a(Entity source, double x, double y,
                double z, float strength, boolean flaming,
                boolean damagesTerrain) {
            explosions++;
            return null;
        }

        @Override
        public java.util.List func_72839_b(Entity excluded,
                AxisAlignedBB box) {
            return Collections.emptyList();
        }
    }
}
