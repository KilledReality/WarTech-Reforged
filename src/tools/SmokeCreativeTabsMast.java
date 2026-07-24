import com.wartec.wartecmod.compat.DroneStrikeContent;
import com.wartec.wartecmod.compat.CreativeTabFix;
import com.wartec.wartecmod.compat.MobileArtilleryContent;
import com.wartec.wartecmod.compat.PatriotContent;
import com.wartec.wartecmod.compat.RadarNetworkContent;
import com.wartec.wartecmod.compat.ReforgedCreativeTabs;
import com.wartec.wartecmod.compat.StrategicAviationContent;
import com.wartec.wartecmod.compat.TacticalAviationContent;
import com.wartec.wartecmod.compat.TileEntityCommunicationRelay;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public final class SmokeCreativeTabsMast {
    public static void main(String[] args) {
        RadarNetworkContent.register();
        PatriotContent.register();
        DroneStrikeContent.register();
        MobileArtilleryContent.register();
        StrategicAviationContent.register();
        TacticalAviationContent.register();
        CreativeTabFix.reorderTabs();

        require(RadarNetworkContent.s400Radar.testCreativeTab
                        == ReforgedCreativeTabs.AIR_DEFENSE,
                "radar must be in air-defense tab");
        require(PatriotContent.patriotLauncher.testCreativeTab
                        == ReforgedCreativeTabs.AIR_DEFENSE,
                "Patriot must be in air-defense tab");
        require(DroneStrikeContent.mq9Drone.testCreativeTab
                        == ReforgedCreativeTabs.AVIATION,
                "MQ-9 must be in aviation tab");
        require(TacticalAviationContent.f16Aircraft.testCreativeTab
                        == ReforgedCreativeTabs.AVIATION,
                "F-16 must be in aviation tab");
        require(MobileArtilleryContent.mobileArtillery.testCreativeTab
                        == ReforgedCreativeTabs.SUPPORT,
                "mobile artillery must be in support tab");
        assertWarTechTabsAreContiguous();

        TestWorld world = new TestWorld();
        TileEntityCommunicationRelay tile = new TileEntityCommunicationRelay();
        tile.func_145834_a(world);
        tile.field_145851_c = 0;
        tile.field_145848_d = 64;
        tile.field_145849_e = 0;
        world.tile = tile;
        RadarNetworkContent.communicationRelay.func_149689_a(world,
                0, 64, 0, new EntityPlayer(world), null);
        for (int offset = 1; offset <= 6; ++offset) {
            require(world.func_147439_a(0, 64 + offset, 0)
                            == RadarNetworkContent.communicationMastSegment,
                    "mast segment " + offset + " was not generated");
        }
        RadarNetworkContent.communicationRelay.func_149749_a(world,
                0, 64, 0, null, 0);
        for (int offset = 1; offset <= 6; ++offset) {
            require(world.func_147437_c(0, 64 + offset, 0),
                    "mast segment " + offset + " was not removed");
        }
        System.out.println("Creative-tab sorting and seven-block mast smoke test passed");
    }

    private static void assertWarTechTabsAreContiguous() {
        CreativeTabs[] expected = {
                com.wartec.wartecmod.wartecmod.tabwartecmodcruisemissiles,
                com.wartec.wartecmod.wartecmod.tabwartecmodparts,
                com.wartec.wartecmod.wartecmod.tabwartecmodblocks,
                com.wartec.wartecmod.wartecmod.tabwartecmodgear,
                com.wartec.wartecmod.wartecmod.tabwartecmodcons,
                ReforgedCreativeTabs.AIR_DEFENSE,
                ReforgedCreativeTabs.AVIATION,
                ReforgedCreativeTabs.SUPPORT
        };
        int start = expected[0].func_78021_a();
        for (int i = 0; i < expected.length; ++i) {
            require(expected[i].func_78021_a() == start + i,
                    "WarTech creative tabs are not contiguous at index " + i
                            + ": expected " + (start + i) + ", got "
                            + expected[i].func_78021_a());
            require(CreativeTabs.field_78032_a[start + i] == expected[i],
                    "creative tab array order mismatch at index " + i);
        }
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static final class TestWorld extends World {
        final Map<String, Block> blocks = new HashMap<String, Block>();
        TileEntity tile;

        TestWorld() { field_72995_K = false; }

        @Override public TileEntity func_147438_o(int x, int y, int z) {
            return x == 0 && y == 64 && z == 0 ? tile : null;
        }

        @Override public boolean func_147437_c(int x, int y, int z) {
            return !blocks.containsKey(key(x, y, z));
        }

        @Override public boolean func_147465_d(int x, int y, int z,
                Block block, int metadata, int flags) {
            blocks.put(key(x, y, z), block);
            return true;
        }

        @Override public Block func_147439_a(int x, int y, int z) {
            return blocks.get(key(x, y, z));
        }

        @Override public boolean func_147468_f(int x, int y, int z) {
            blocks.remove(key(x, y, z));
            return true;
        }

        private static String key(int x, int y, int z) {
            return x + ":" + y + ":" + z;
        }
    }
}
