import api.hbm.energymk2.IEnergyConductorMK2;
import api.hbm.energymk2.IEnergyReceiverMK2;
import com.wartec.wartecmod.compat.HbmTilePowerLink;
import java.util.ArrayList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public final class SmokeHbmConnectorDiscovery {
    public static void main(String[] args) {
        World world = new World();
        world.field_147482_g = new ArrayList();
        FakeReceiver receiver = new FakeReceiver();
        place(world, receiver, 0, 64, 0);
        place(world, new FakeConductor(), 8, 66, -3);
        place(world, new FakeConductor(), 11, 64, 0);

        HbmTilePowerLink.subscribeNearby(receiver, receiver, 10, 6);
        require(receiver.subscriptions == 6,
                "near connector must be tried from all six sides");
        require(receiver.lastX == 8 && receiver.lastY == 66
                        && receiver.lastZ == -3,
                "wrong HBM connector selected");
        System.out.println("HBM connector discovery smoke test passed");
    }

    private static void place(World world, TileEntity tile,
            int x, int y, int z) {
        tile.func_145834_a(world);
        tile.field_145851_c = x;
        tile.field_145848_d = y;
        tile.field_145849_e = z;
        world.field_147482_g.add(tile);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class FakeConductor extends TileEntity
            implements IEnergyConductorMK2 {
    }

    private static final class FakeReceiver extends TileEntity
            implements IEnergyReceiverMK2 {
        int subscriptions;
        int lastX;
        int lastY;
        int lastZ;
        long power;

        @Override
        public void trySubscribe(World world, int x, int y, int z,
                ForgeDirection direction) {
            subscriptions++;
            lastX = x;
            lastY = y;
            lastZ = z;
        }

        @Override public long getPower() { return power; }
        @Override public void setPower(long value) { power = value; }
        @Override public long getMaxPower() { return 1000L; }
        @Override public boolean isLoaded() { return true; }
    }
}
