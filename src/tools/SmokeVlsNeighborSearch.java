import com.wartec.wartecmod.compat.VlsDefenseCompat;
import com.wartec.wartecmod.tileentity.vls.TileEntityVlsExhaust;
import com.wartec.wartecmod.tileentity.vls.TileEntityVlsLaunchTube;
import java.util.ArrayList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public final class SmokeVlsNeighborSearch {
    public static void main(String[] args) {
        World world = new World();
        world.field_147482_g = new ArrayList();
        TileEntityVlsLaunchTube origin = tube(world, 0, 64, 0, false);

        // Dense loops used to produce an exponential recursive walk.
        for (int x = -10; x <= 10; ++x) {
            for (int z = -10; z <= 10; ++z) {
                tube(world, x, 64, z, false);
            }
        }
        tube(world, 11, 64, 0, true);
        int[] found = VlsDefenseCompat.findConnectedVlsExhaust(origin);
        require(found != null && found[0] == 11 && found[1] == 0,
                "connected exhaust not found");

        World isolated = new World();
        isolated.field_147482_g = new ArrayList();
        TileEntityVlsLaunchTube lone = tube(isolated, 0, 64, 0, false);
        tube(isolated, 31, 64, 0, true);
        require(VlsDefenseCompat.findConnectedVlsExhaust(lone) == null,
                "search escaped the 30 block limit");
        System.out.println("VLS neighbor BFS smoke test passed");
    }

    private static TileEntityVlsLaunchTube tube(
            World world, int x, int y, int z, boolean exhaust) {
        TileEntityVlsLaunchTube tile = exhaust
                ? new TileEntityVlsExhaust() : new TileEntityVlsLaunchTube();
        tile.func_145834_a(world);
        tile.field_145851_c = x;
        tile.field_145848_d = y;
        tile.field_145849_e = z;
        world.field_147482_g.add((TileEntity) tile);
        return tile;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
