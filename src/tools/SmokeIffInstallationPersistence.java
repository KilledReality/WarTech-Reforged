package tools;

import com.wartec.wartecmod.compat.ITeamOwned;
import com.wartec.wartecmod.compat.NetworkTeamHelper;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public final class SmokeIffInstallationPersistence {
    private static final String[] ENTITY_CLASSES = {
        "com.wartec.wartecmod.entity.vehicle.EntityRadarTruck",
        "com.wartec.wartecmod.entity.vehicle.EntityS400Radar",
        "com.wartec.wartecmod.entity.vehicle.EntityElectronicWarfareUnit",
        "com.wartec.wartecmod.entity.vehicle.EntityMobileAirDefense"
    };
    private static final String[] TILE_CLASSES = {
        "com.wartec.wartecmod.compat.TileEntityCommunicationRelay",
        "com.wartec.wartecmod.compat.TileEntityAirRaidRelay",
        "com.wartec.wartecmod.compat.TileEntityGeranLauncher",
        "com.wartec.wartecmod.compat.TileEntityStrategicRadar",
        "com.wartec.wartecmod.compat.TileEntityPatriotLauncher",
        "com.wartec.wartecmod.compat.TileEntityS400Launcher"
    };

    public static void main(String[] args) throws Exception {
        for (String className : ENTITY_CLASSES) {
            verifyEntity(className);
        }
        for (String className : TILE_CLASSES) {
            verifyTile(className);
        }
        System.out.println("IFF_INSTALLATION_PERSISTENCE_PASS");
    }

    private static void verifyEntity(String className) throws Exception {
        Class<?> type = Class.forName(className);
        Constructor<?> constructor = type.getConstructor(World.class);
        Object original = constructor.newInstance(new World());
        ((ITeamOwned) original).setOwnerTeam("alpha");

        NBTTagCompound written = new NBTTagCompound();
        invokeDeclared(type, original, "func_70014_b", written);
        NBTTagCompound readFromDisk = teamOnlyRoundTrip(written);

        Object restored = constructor.newInstance(new World());
        invokeDeclared(type, restored, "func_70037_a", readFromDisk);
        assertTeam(className, "alpha",
                NetworkTeamHelper.getEntityTeam((Entity) restored));
    }

    private static void verifyTile(String className) throws Exception {
        Class<?> type = Class.forName(className);
        Object original = type.newInstance();
        ((ITeamOwned) original).setOwnerTeam("bravo");

        NBTTagCompound written = new NBTTagCompound();
        invokeDeclared(type, original, "func_145841_b", written);
        NBTTagCompound readFromDisk = teamOnlyRoundTrip(written);

        Object restored = type.newInstance();
        invokeDeclared(type, restored, "func_145839_a", readFromDisk);
        assertTeam(className, "bravo",
                ((ITeamOwned) restored).getOwnerTeam());
        if (!(restored instanceof TileEntity)) {
            throw new AssertionError(className + " is not a tile entity");
        }
    }

    private static void invokeDeclared(Class<?> type, Object target,
            String name, NBTTagCompound tag) throws Exception {
        Method method = type.getDeclaredMethod(name, NBTTagCompound.class);
        method.setAccessible(true);
        method.invoke(target, tag);
    }

    private static NBTTagCompound teamOnlyRoundTrip(NBTTagCompound source) {
        NBTTagCompound restored = new NBTTagCompound();
        restored.func_74778_a("WarTechOwnerTeam",
                source.func_74779_i("WarTechOwnerTeam"));
        return restored;
    }

    private static void assertTeam(String owner, String expected,
            String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError(owner + ": expected " + expected
                    + " but got " + actual);
        }
    }
}
