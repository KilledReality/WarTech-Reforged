import com.wartec.wartecmod.compat.ContainerMobileAirDefense;
import com.wartec.wartecmod.entity.vehicle.EntityMobileAirDefense;
import com.wartec.wartecmod.items.wartecmodItems;
import java.util.ArrayList;
import java.util.Random;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public final class SmokeMobileAirDefense {
    public static void main(String[] args) throws Exception {
        wartecmodItems.itemMissileAntiAirTier1 = new Item();
        wartecmodItems.itemMissileAntiAirTier2 = new Item();
        wartecmodItems.itemMissileAntiAirTier3 = new Item();

        TestWorld world = new TestWorld();
        EntityMobileAirDefense tor = new EntityMobileAirDefense(world);
        require(tor.isTor() && tor.getMissileCapacity() == 8,
                "Tor must expose eight missile cells");
        require(tor.getRequiredInterceptorTier() == 2
                        && tor.getEngagementRange() == 220,
                "Tor must use tier-2 interceptors at short range");
        require(tor.func_94041_b(0,
                        new TestStack(wartecmodItems.itemMissileAntiAirTier2)),
                "Tor must accept WTI-2");
        require(!tor.func_94041_b(0,
                        new TestStack(wartecmodItems.itemMissileAntiAirTier1)),
                "Tor must reject WTI-1");

        EntityPlayer player = new EntityPlayer(world);
        ContainerMobileAirDefense container = new ContainerMobileAirDefense(
                player.field_71071_by, tor);
        require(container.field_75151_b.size() == 49,
                "SHORAD GUI must expose 12 missile cells, battery, and player inventory");
        require(tor.getFireMode() == EntityMobileAirDefense.FIRE_AUTO,
                "SHORAD must start in automatic fire mode");
        container.func_75140_a(player, 0);
        require(tor.getFireMode() == EntityMobileAirDefense.FIRE_EMERGENCY,
                "GUI mode button must cycle to emergency fire");
        container.func_75140_a(player, 0);
        require(tor.getFireMode() == EntityMobileAirDefense.FIRE_HOLD,
                "GUI mode button must cycle to hold fire");

        EntityMobileAirDefense pantsir = new EntityMobileAirDefense(world);
        pantsir.setVariant(EntityMobileAirDefense.VARIANT_PANTSIR);
        require(!pantsir.isTor() && pantsir.getMissileCapacity() == 12,
                "Pantsir must expose twelve missile cells");
        require(pantsir.getRequiredInterceptorTier() == 1
                        && pantsir.getEngagementRange() == 100,
                "Pantsir must use tier-1 interceptors for point defense");
        pantsir.func_70299_a(0,
                new TestStack(wartecmodItems.itemMissileAntiAirTier1));
        pantsir.func_70299_a(1,
                new TestStack(wartecmodItems.itemMissileAntiAirTier1));
        require(pantsir.getAmmoCount() == 2,
                "Pantsir ammunition watcher must count loaded missiles");

        EntityPlayer driver = new EntityPlayer(world);
        driver.field_70701_bs = 1.0F;
        pantsir.field_70122_E = true;
        pantsir.field_70153_n = driver;
        java.lang.reflect.Method updateDriving = EntityMobileAirDefense.class
                .getDeclaredMethod("updateDriving");
        updateDriving.setAccessible(true);
        updateDriving.invoke(pantsir);
        require(Math.abs(pantsir.field_70159_w) + Math.abs(pantsir.field_70179_y) > 0.0D,
                "mobile air-defense vehicle must respond to W input");
        pantsir.func_70097_a(new DamageSource(), 600.0F);
        require(pantsir.field_70128_L,
                "a severe missile-scale hit must destroy mobile air defense");

        System.out.println("Mobile air-defense smoke test passed");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static final class TestWorld extends World {
        TestWorld() {
            field_72995_K = false;
            field_73012_v = new Random(11L);
            field_72996_f = new ArrayList();
            field_147482_g = new ArrayList();
        }
    }

    private static final class TestStack extends ItemStack {
        private final Item item;

        TestStack(Item item) {
            super(item);
            this.item = item;
            field_77994_a = 1;
        }

        @Override
        public Item func_77973_b() {
            return item;
        }
    }
}
