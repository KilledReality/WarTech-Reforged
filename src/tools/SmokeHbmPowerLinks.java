import api.hbm.energy.IEnergyUser;
import com.wartec.wartecmod.compat.HbmEntityPowerLink;
import com.wartec.wartecmod.compat.IWirePoweredEntity;
import com.wartec.wartecmod.compat.TileEntityCommunicationRelay;
import java.lang.reflect.Constructor;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

public final class SmokeHbmPowerLinks {
    public static void main(String[] args) throws Exception {
        TileEntityCommunicationRelay relay = new TileEntityCommunicationRelay();
        relay.setPower(0L);
        require(relay.transferPower(750L) == 0L && relay.getPower() == 750L,
                "relay did not accept HBM power");
        require(relay.transferPower(relay.getMaxPower()) == 750L,
                "relay overflow accounting is wrong");

        World world = new World();
        FakePoweredEntity entity = new FakePoweredEntity(world);
        Constructor<HbmEntityPowerLink> constructor =
                HbmEntityPowerLink.class.getDeclaredConstructor(
                        Entity.class, IWirePoweredEntity.class);
        constructor.setAccessible(true);
        IEnergyUser link = constructor.newInstance(entity, entity);
        require(link.transferPower(400L) == 0L && entity.power == 400,
                "entity did not accept HBM power");
        require(link.isLoaded(), "live entity receiver reported unloaded");
        entity.func_70106_y();
        require(!link.isLoaded(), "dead entity receiver stayed loaded");
        System.out.println("HBM power link smoke test passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class FakePoweredEntity extends Entity
            implements IWirePoweredEntity {
        int power;

        FakePoweredEntity(World world) {
            super(world);
            func_70105_a(3.0F, 2.0F);
        }

        @Override public int wartecGetWirePower() { return power; }
        @Override public void wartecSetWirePower(int value) {
            power = Math.max(0, Math.min(1000, value));
        }
        @Override public int wartecGetWireCapacity() { return 1000; }
    }
}
