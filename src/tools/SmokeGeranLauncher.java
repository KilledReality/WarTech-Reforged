import com.hbm.interfaces.IBomb.BombReturnCode;
import com.wartec.wartecmod.compat.AdvancedMissileContent;
import com.wartec.wartecmod.compat.NetworkTeamHelper;
import com.wartec.wartecmod.compat.RemoteControlNetwork;
import com.wartec.wartecmod.compat.TileEntityGeranLauncher;
import com.wartec.wartecmod.entity.missile.EntityGeran;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.ForgeChunkManager;

public final class SmokeGeranLauncher {
    public static void main(String[] args) throws Exception {
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
        require(ForgeChunkManager.requested >= 1,
                "Geran did not receive a chunk ticket immediately at launch");
        EntityGeran classic = (EntityGeran) world.spawned;
        for (int tick = 0; tick < 5; ++tick) classic.func_70071_h_();
        require(followsMotion(classic),
                "classic Geran nose rotation does not follow its flight vector");

        testRemoteFlightAndRestore();
        testRangeRelease();
        testNoProximityFuse();
        testClassicProximityFuse();
        testRemoteMissionPersistence();
        testTerrainContactFuse();
        System.out.println("Geran classic/remote fuse and flight smoke test passed");
    }

    private static void testRemoteFlightAndRestore() {
        TestWorld world = new TestWorld();
        EntityPlayerMP player = remotePlayer(world, 4.5D, 65.0D, 4.5D);
        TileEntityGeranLauncher launcher = launcher(world, 10, 64, 10,
                310, 110);
        launcher.setOwnerTeam(NetworkTeamHelper.getPlayerTeam(player));
        BombReturnCode result = launcher.launchRemote(player);
        require(result == BombReturnCode.LAUNCHED,
                "remote Geran launch rejected a valid loadout");
        EntityGeran geran = (EntityGeran) world.spawned;
        require(geran.isRemoteControlled(),
                "remote launch did not establish Geran control");
        require(player.field_70145_X && player.func_82150_aj(),
                "operator body was not protected during Geran control");
        float startYaw = geran.field_70177_z;
        double startY = geran.field_70163_u;
        for (int tick = 0; tick < 14; ++tick) {
            geran.handleRemoteInput(player, startYaw, -18.0F, 0.8F,
                    RemoteControlNetwork.FLAG_TURN_LEFT);
            geran.func_70071_h_();
        }
        require(angleDifference(geran.field_70177_z, startYaw) < -8.0F,
                "manual Geran left turn did not rotate smoothly: start="
                + startYaw + " end=" + geran.field_70177_z);
        require(geran.field_70163_u > startY + 0.5D,
                "manual Geran did not climb clear of the launcher");
        require(followsMotion(geran),
                "remote Geran nose rotation does not follow its flight vector");

        geran.handleRemoteInput(player, geran.field_70177_z,
                geran.field_70125_A, 0.8F, RemoteControlNetwork.FLAG_EXIT);
        require(!geran.isRemoteControlled(),
                "Geran remained remote-controlled after exit");
        require(Math.abs(player.field_70165_t - 4.5D) < 0.001D
                        && Math.abs(player.field_70163_u - 65.0D) < 0.001D
                        && Math.abs(player.field_70161_v - 4.5D) < 0.001D,
                "operator was not restored to the Geran launch position");
        require(!player.field_70145_X && !player.func_82150_aj(),
                "operator protection was not restored after Geran exit");
        require(!geran.field_70128_L,
                "manual exit destroyed the Geran instead of resuming autopilot");
    }

    private static void testRangeRelease() {
        TestWorld world = new TestWorld();
        EntityPlayerMP player = remotePlayer(world, 8.0D, 65.0D, 8.0D);
        EntityGeran geran = new EntityGeran(world, 10.5F, 65.35F, 10.5F,
                900, 10);
        geran.setOwnerTeam(NetworkTeamHelper.getPlayerTeam(player));
        world.spawned = geran;
        world.field_72996_f.add(geran);
        require(geran.beginRemoteControl(player),
                "direct Geran remote link failed");
        geran.func_70107_b(1008.6D, 85.0D, 10.5D);
        geran.func_70071_h_();
        require(!geran.isRemoteControlled(),
                "Geran remote link exceeded its 1000 block radius");
        require(!geran.field_70128_L,
                "range limit destroyed Geran instead of resuming autopilot");
        require(Math.abs(player.field_70165_t - 8.0D) < 0.001D,
                "range release did not restore the operator");
    }

    private static void testNoProximityFuse() {
        TestWorld world = new TestWorld();
        EntityPlayerMP player = remotePlayer(world, 8.0D, 65.0D, 8.0D);
        EntityGeran geran = new EntityGeran(world, 10.5F, 65.35F, 10.5F,
                210, 110);
        require(geran.beginRemoteControl(player),
                "manual Geran mission was not marked remote");
        geran.handleRemoteInput(player, geran.field_70177_z,
                geran.field_70125_A, 0.8F, RemoteControlNetwork.FLAG_EXIT);
        geran.func_70107_b(210.5D, 68.0D, 110.5D);
        geran.field_70173_aa = 40;
        geran.func_70071_h_();
        require(!geran.field_70128_L,
                "Geran still detonated from coordinate proximity without contact");
    }

    private static void testClassicProximityFuse() throws Exception {
        TestWorld world = new TestWorld();
        EntityGeran geran = new EntityGeran(world, 10.5F, 65.35F, 10.5F,
                210, 110);
        geran.func_70107_b(210.5D, 68.0D, 110.5D);
        Field targetGround = EntityGeran.class.getDeclaredField("targetGroundY");
        targetGround.setAccessible(true);
        targetGround.setInt(geran, 64);
        Method reached = EntityGeran.class.getDeclaredMethod(
                "hasReachedClassicTarget", double.class);
        reached.setAccessible(true);
        require(((Boolean) reached.invoke(geran, Double.valueOf(0.0D)))
                        .booleanValue(),
                "classic Geran did not restore its old proximity fuse");
    }

    private static void testRemoteMissionPersistence() throws Exception {
        TestWorld world = new TestWorld();
        EntityPlayerMP player = remotePlayer(world, 8.0D, 65.0D, 8.0D);
        EntityGeran original = new EntityGeran(world,
                10.5F, 65.35F, 10.5F, 210, 110);
        require(original.beginRemoteControl(player),
                "remote Geran mission could not start for persistence test");
        original.handleRemoteInput(player, original.field_70177_z,
                original.field_70125_A, 0.8F,
                RemoteControlNetwork.FLAG_EXIT);
        NBTTagCompound tag = new NBTTagCompound();
        Method write = EntityGeran.class.getDeclaredMethod(
                "func_70014_b", NBTTagCompound.class);
        write.setAccessible(true);
        write.invoke(original, tag);

        EntityGeran restored = new EntityGeran(world);
        Method read = EntityGeran.class.getDeclaredMethod(
                "func_70037_a", NBTTagCompound.class);
        read.setAccessible(true);
        read.invoke(restored, tag);
        restored.func_70107_b(210.5D, 68.0D, 110.5D);
        Field targetGround = EntityGeran.class.getDeclaredField("targetGroundY");
        targetGround.setAccessible(true);
        targetGround.setInt(restored, 64);
        Method reached = EntityGeran.class.getDeclaredMethod(
                "hasReachedClassicTarget", double.class);
        reached.setAccessible(true);
        require(!((Boolean) reached.invoke(restored, Double.valueOf(0.0D)))
                        .booleanValue(),
                "saved remote Geran reverted to classic proximity fuse");
    }

    private static void testTerrainContactFuse() throws Exception {
        TestWorld world = new TestWorld();
        EntityGeran geran = new EntityGeran(world, 10.5F, 65.35F, 10.5F,
                210, 110);
        Method contact = EntityGeran.class.getDeclaredMethod(
                "hasTerrainContact", double.class, double.class, double.class);
        contact.setAccessible(true);
        require(((Boolean) contact.invoke(geran, Double.valueOf(80.5D),
                        Double.valueOf(64.20D), Double.valueOf(40.5D)))
                        .booleanValue(),
                "Geran contact fuse did not detect physical terrain impact");
        require(!((Boolean) contact.invoke(geran, Double.valueOf(80.5D),
                        Double.valueOf(66.0D), Double.valueOf(40.5D)))
                        .booleanValue(),
                "Geran contact fuse triggered above physical terrain");
    }

    private static TileEntityGeranLauncher launcher(TestWorld world,
            int x, int y, int z, int targetX, int targetZ) {
        TileEntityGeranLauncher launcher = new TileEntityGeranLauncher();
        launcher.func_145834_a(world);
        launcher.field_145851_c = x;
        launcher.field_145848_d = y;
        launcher.field_145849_e = z;
        launcher.power = 100000L;
        launcher.slots[0] = new ItemStack(AdvancedMissileContent.geranDrone);
        launcher.slots[1] = targetStack("x", "z", targetX, targetZ);
        return launcher;
    }

    private static EntityPlayerMP remotePlayer(TestWorld world,
            double x, double y, double z) {
        EntityPlayerMP player = new EntityPlayerMP(world);
        player.field_71135_a = new NetHandlerPlayServer();
        player.field_71135_a.field_147369_b = player;
        player.func_70107_b(x, y, z);
        NetworkTeamHelper.setPlayerTeam(player, "alpha");
        world.field_73010_i.add(player);
        return player;
    }

    private static float angleDifference(float value, float origin) {
        float difference = value - origin;
        while (difference <= -180.0F) difference += 360.0F;
        while (difference > 180.0F) difference -= 360.0F;
        return difference;
    }

    private static boolean followsMotion(EntityGeran geran) {
        double horizontal = Math.sqrt(geran.field_70159_w * geran.field_70159_w
                + geran.field_70179_y * geran.field_70179_y);
        float expectedYaw = (float) Math.toDegrees(Math.atan2(
                -geran.field_70159_w, geran.field_70179_y));
        float expectedPitch = (float) -Math.toDegrees(Math.atan2(
                geran.field_70181_x, Math.max(0.0001D, horizontal)));
        return Math.abs(angleDifference(geran.field_70177_z, expectedYaw)) < 0.01F
                && Math.abs(geran.field_70125_A - expectedPitch) < 0.01F;
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
            field_73010_i = new ArrayList();
            field_147482_g = new ArrayList();
        }

        @Override public boolean func_72838_d(Entity entity) {
            spawned = entity;
            field_72996_f.add(entity);
            return true;
        }

        @Override public Entity func_73045_a(int id) {
            if (spawned != null && spawned.func_145782_y() == id) return spawned;
            return null;
        }

        @Override public int func_72976_f(int x, int z) {
            return 64;
        }

        @Override public List func_72839_b(Entity excluded, AxisAlignedBB box) {
            return Collections.emptyList();
        }

        @Override public Chunk func_72964_e(int x, int z) {
            return new Chunk();
        }
    }
}
