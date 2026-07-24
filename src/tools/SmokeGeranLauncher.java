import com.hbm.interfaces.IBomb.BombReturnCode;
import com.wartec.wartecmod.compat.AdvancedMissileContent;
import com.wartec.wartecmod.compat.TileEntityGeranLauncher;
import com.wartec.wartecmod.entity.missile.EntityGeran;
import java.util.ArrayList;
import java.util.Random;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;

public final class SmokeGeranLauncher {
    public static void main(String[] args) {
        ForgeChunkManager.resetTestState();
        TestWorld world = new TestWorld();
        TileEntityGeranLauncher launcher = new TileEntityGeranLauncher();
        launcher.func_145834_a(world);
        launcher.field_145851_c = 10;
        launcher.field_145848_d = 64;
        launcher.field_145849_e = 10;
        launcher.power = 100000L;
        launcher.shoot = 50;
        launcher.open = true;
        launcher.setOwnerTeam("alpha");

        AdvancedMissileContent.geranDrone = new Item();
        launcher.slots[0] = new ItemStack(AdvancedMissileContent.geranDrone);
        launcher.slots[1] = targetStack("x", "z", 210, 110);

        BombReturnCode result = launcher.shoot(world, 10, 64, 10);
        require(result == BombReturnCode.LAUNCHED,
                "Geran launcher rejected a valid coordinate target");
        require(world.spawned instanceof EntityGeran,
                "Geran launcher did not spawn its drone");
        require("alpha".equals(((EntityGeran) world.spawned).getOwnerTeam()),
                "Geran did not inherit the launcher's IFF team");
        require(launcher.slots[0] == null && launcher.power == 75000L,
                "successful launch did not consume drone and energy");
        require(launcher.shoot == 0 && !launcher.open,
                "successful launch left the launcher stuck in firing state");
        require(ForgeChunkManager.requested == 1,
                "Geran did not receive a chunk ticket immediately at launch");

        System.out.println("Geran launcher and immediate chunk-loading smoke test passed");
    }

    private static ItemStack targetStack(String xKey, String zKey,
            int x, int z) {
        ItemStack stack = new ItemStack(new Item());
        stack.field_77990_d = new NBTTagCompound();
        stack.field_77990_d.func_74768_a(xKey, x);
        stack.field_77990_d.func_74768_a(zKey, z);
        return stack;
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static final class TestWorld extends World {
        Entity spawned;

        TestWorld() {
            field_72995_K = false;
            field_73012_v = new Random(23L);
            field_72996_f = new ArrayList();
            field_147482_g = new ArrayList();
        }

        @Override public boolean func_72838_d(Entity entity) {
            spawned = entity;
            field_72996_f.add(entity);
            return true;
        }
    }
}
