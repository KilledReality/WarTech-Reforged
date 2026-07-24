import com.wartec.wartecmod.compat.ContainerCommunicationRelay;
import com.wartec.wartecmod.compat.DroneStrikeContent;
import com.wartec.wartecmod.compat.RadarNetworkContent;
import com.wartec.wartecmod.compat.SalvageWrenchCompat;
import com.wartec.wartecmod.compat.TacticalAviationContent;
import com.wartec.wartecmod.compat.TileEntityCommunicationRelay;
import com.wartec.wartecmod.entity.missile.EntityMq9Drone;
import com.wartec.wartecmod.entity.missile.EntityTacticalAircraft;
import com.wartec.wartecmod.entity.vehicle.EntityRadarTruck;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

public final class SmokeUiRecovery {
    public static void main(String[] args) {
        RadarNetworkContent.register();
        DroneStrikeContent.register();
        TacticalAviationContent.register();
        TestWorld world = new TestWorld();
        TestPlayer player = new TestPlayer(world);
        SalvageWrenchCompat handler = new SalvageWrenchCompat();

        EntityRadarTruck radar = new EntityRadarTruck(world);
        handler.onEntityInteract(new EntityInteractEvent(player, radar));
        require(radar.field_70128_L, "ground installation was not dismantled");

        TestMq9 airborne = new TestMq9(world);
        airborne.makeAirborne();
        handler.onEntityInteract(new EntityInteractEvent(player, airborne));
        require(!airborne.field_70128_L,
                "airborne aircraft must not be dismantled");

        EntityTacticalAircraft wreck = new EntityTacticalAircraft(world);
        markAsLandedWreck(wreck);
        handler.onEntityInteract(new EntityInteractEvent(player, wreck));
        require(wreck.field_70128_L,
                "landed tactical-aircraft wreck must be recoverable");

        world.blocks.put(key(0, 64, 0),
                RadarNetworkContent.communicationRelay);
        for (int offset = 1; offset <= 6; ++offset) {
            world.blocks.put(key(0, 64 + offset, 0),
                    RadarNetworkContent.communicationMastSegment);
        }
        handler.onBlockInteract(new PlayerInteractEvent(player,
                PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK,
                0, 70, 0, 1, world));
        require(world.func_147437_c(0, 64, 0),
                "mast base was not recovered through its upper segment");

        TileEntityCommunicationRelay relay =
                new TileEntityCommunicationRelay();
        relay.func_145834_a(world);
        relay.field_145851_c = 4;
        relay.field_145848_d = 64;
        relay.field_145849_e = 4;
        world.tile = relay;
        ContainerCommunicationRelay container =
                new ContainerCommunicationRelay(player.field_71071_by, relay);
        require(container.field_75151_b.size() == 37,
                "mast container must expose one battery and 36 player slots");
        boolean before = relay.isEnabled();
        require(container.func_75140_a(player, 0),
                "mast toggle action was not accepted");
        require(relay.isEnabled() != before, "mast toggle did not change state");

        System.out.println("Communication GUI and salvage-wrench smoke test passed");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static String key(int x, int y, int z) {
        return x + ":" + y + ":" + z;
    }

    private static final class TestPlayer extends EntityPlayer {
        private final ItemStack wrench =
                new ItemStack(DroneStrikeContent.salvageWrench);
        TestPlayer(World world) { super(world); }
        @Override public ItemStack func_71045_bC() { return wrench; }
        @Override public boolean func_70093_af() { return true; }
    }

    private static final class TestMq9 extends EntityMq9Drone {
        TestMq9(World world) { super(world); }
        void makeAirborne() { setState(STATE_OUTBOUND); }
    }

    private static void markAsLandedWreck(EntityTacticalAircraft aircraft) {
        try {
            java.lang.reflect.Method setState =
                    EntityMq9Drone.class.getDeclaredMethod(
                            "setState", Integer.TYPE);
            setState.setAccessible(true);
            setState.invoke(aircraft, EntityMq9Drone.STATE_CRASHED);
            java.lang.reflect.Field landed =
                    EntityMq9Drone.class.getDeclaredField("wreckLanded");
            landed.setAccessible(true);
            landed.setBoolean(aircraft, true);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private static final class TestWorld extends World {
        final Map<String, Block> blocks = new HashMap<String, Block>();
        TileEntity tile;
        TestWorld() { field_72995_K = false; }
        @Override public Block func_147439_a(int x, int y, int z) {
            return blocks.get(key(x, y, z));
        }
        @Override public boolean func_147437_c(int x, int y, int z) {
            return !blocks.containsKey(key(x, y, z));
        }
        @Override public boolean func_147468_f(int x, int y, int z) {
            blocks.remove(key(x, y, z));
            return true;
        }
        @Override public TileEntity func_147438_o(int x, int y, int z) {
            return tile != null && tile.field_145851_c == x
                    && tile.field_145848_d == y
                    && tile.field_145849_e == z ? tile : null;
        }
    }
}
