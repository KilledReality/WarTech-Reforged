import com.wartec.wartecmod.compat.RemoteLaunchCompat;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public final class SmokeRemoteLaunchCompat {
    public static void main(String[] args) {
        TestWorld server = new TestWorld(false);
        RemoteLaunchCompat.getLoadedLauncherBlock(server, 320, 70, -160);
        require(server.blockLookups == 1, "launcher lookup must be preserved");
        require(server.loadedChunks == 9,
                "server must load the launcher chunk and its eight neighbors");

        TestWorld client = new TestWorld(true);
        RemoteLaunchCompat.getLoadedLauncherBlock(client, 320, 70, -160);
        require(client.loadedChunks == 0,
                "client must not perform synchronous chunk loading");
        System.out.println("Remote launcher chunk-load smoke test passed");
    }

    private static void require(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }

    private static final class TestWorld extends World {
        int loadedChunks;
        int blockLookups;

        TestWorld(boolean client) {
            field_72995_K = client;
        }

        @Override
        public Chunk func_72964_e(int x, int z) {
            ++loadedChunks;
            return null;
        }

        @Override
        public Block func_147439_a(int x, int y, int z) {
            ++blockLookups;
            return null;
        }
    }
}
