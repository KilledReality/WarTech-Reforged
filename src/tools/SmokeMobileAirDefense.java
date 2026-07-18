import com.wartec.wartecmod.compat.ContainerMobileAirDefense;
import com.wartec.wartecmod.compat.ItemPantsirAmmoBelt;
import com.wartec.wartecmod.compat.RadarNetworkContent;
import com.wartec.wartecmod.entity.vehicle.EntityMobileAirDefense;
import com.wartec.wartecmod.items.wartecmodItems;
import api.hbm.entity.IRadarDetectable;
import java.util.ArrayList;
import java.util.Random;
import net.minecraft.entity.Entity;
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
        RadarNetworkContent.pantsirAmmoBelt = new ItemPantsirAmmoBelt();

        TestWorld world = new TestWorld();
        EntityMobileAirDefense tor = new EntityMobileAirDefense(world);
        require(tor.isTor() && tor.getMissileCapacity() == 8,
                "Tor must expose eight missile cells");
        require(tor.getRequiredInterceptorTier() == 2
                        && tor.getEngagementRange() == 220,
                "Tor must use tier-2 interceptors at short range");
        require("WTI-2 LANCE".equals(tor.getRequiredInterceptorName()),
                "Tor GUI must identify its compatible missile");
        require(tor.func_94041_b(0,
                        new TestStack(wartecmodItems.itemMissileAntiAirTier2)),
                "Tor must accept WTI-2");
        require(!tor.func_94041_b(0,
                        new TestStack(wartecmodItems.itemMissileAntiAirTier1)),
                "Tor must reject WTI-1");

        EntityPlayer player = new EntityPlayer(world);
        ContainerMobileAirDefense container = new ContainerMobileAirDefense(
                player.field_71071_by, tor);
        require(container.field_75151_b.size() == 50,
                "SHORAD GUI must expose missiles, battery, gun belt, and player inventory");
        require(!tor.func_94041_b(EntityMobileAirDefense.GUN_AMMO_SLOT,
                        new TestStack(RadarNetworkContent.pantsirAmmoBelt)),
                "Tor must reject Pantsir cannon ammunition");
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
        require("WTI-1 FALCON".equals(pantsir.getRequiredInterceptorName()),
                "Pantsir GUI must identify its compatible missile");
        pantsir.func_70299_a(0,
                new TestStack(wartecmodItems.itemMissileAntiAirTier1));
        pantsir.func_70299_a(1,
                new TestStack(wartecmodItems.itemMissileAntiAirTier1));
        require(pantsir.getAmmoCount() == 2,
                "Pantsir ammunition watcher must count loaded missiles");
        ItemStack belt = new TestStack(RadarNetworkContent.pantsirAmmoBelt);
        require(pantsir.func_94041_b(EntityMobileAirDefense.GUN_AMMO_SLOT, belt),
                "Pantsir must accept its 30 mm belt");
        pantsir.func_70299_a(EntityMobileAirDefense.GUN_AMMO_SLOT, belt);
        require(pantsir.getGunRounds() == ItemPantsirAmmoBelt.CAPACITY,
                "a fresh Pantsir belt must contain 600 rounds");
        ItemStack testBelt = new TestStack(RadarNetworkContent.pantsirAmmoBelt);
        require(ItemPantsirAmmoBelt.consume(testBelt, 10) == 10
                        && ItemPantsirAmmoBelt.getRounds(testBelt) == 590,
                "Pantsir belt must persist consumed rounds in NBT");
        require(pantsir.isGunsEnabled(), "Pantsir guns must default to automatic mode");
        pantsir.handleGuiAction(2, player);
        require(!pantsir.isGunsEnabled(), "gun GUI control must switch guns to hold");
        pantsir.handleGuiAction(2, player);
        require(pantsir.isGunsEnabled(), "gun GUI control must restore automatic mode");
        java.lang.reflect.Field gunYaw = EntityMobileAirDefense.class
                .getDeclaredField("gunAimYaw");
        java.lang.reflect.Field gunPitch = EntityMobileAirDefense.class
                .getDeclaredField("gunAimPitch");
        java.lang.reflect.Method syncGunAim = EntityMobileAirDefense.class
                .getDeclaredMethod("syncGunAim");
        gunYaw.setAccessible(true);
        gunPitch.setAccessible(true);
        syncGunAim.setAccessible(true);
        gunYaw.setFloat(pantsir, -123.4F);
        gunPitch.setFloat(pantsir, 45.6F);
        syncGunAim.invoke(pantsir);
        require(Math.abs(pantsir.getGunAimYaw() + 123.4F) < 0.11F
                        && Math.abs(pantsir.getGunAimPitch() - 45.6F) < 0.11F,
                "packed gun aim must survive entity data synchronization");
        gunYaw.setFloat(pantsir, 0.0F);
        gunPitch.setFloat(pantsir, 0.0F);
        syncGunAim.invoke(pantsir);
        java.lang.reflect.Method updateClientGunState = EntityMobileAirDefense.class
                .getDeclaredMethod("updateClientGunState");
        updateClientGunState.setAccessible(true);
        updateClientGunState.invoke(pantsir);
        int soundsBeforeBurst = world.sounds;
        TestMissile incoming = new TestMissile(world, 71);
        incoming.field_70165_t = 0.0D;
        incoming.field_70163_u = 3.0D;
        incoming.field_70161_v = 70.0D;
        incoming.field_70179_y = -4.0D;
        world.field_72996_f.add(incoming);
        java.lang.reflect.Method tickGuns = EntityMobileAirDefense.class
                .getDeclaredMethod("tickPantsirGuns");
        tickGuns.setAccessible(true);
        for (int tick = 0; tick < 6 && !incoming.field_70128_L; ++tick) {
            tickGuns.invoke(pantsir);
        }
        require(incoming.field_70128_L,
                "Pantsir guns must destroy a tier-1 missile within six reaction ticks");
        require(pantsir.getGunRounds() <= 590,
                "a real gun interception must consume a 30 mm burst");
        world.field_72995_K = true;
        updateClientGunState.invoke(pantsir);
        world.field_72995_K = false;
        require(world.particles >= 30,
                "a synchronized gun burst must create visible client tracers");
        require(world.sounds > soundsBeforeBurst,
                "a synchronized gun burst must play an audible client sound");

        EntityPlayer driver = new EntityPlayer(world);
        driver.field_70701_bs = 1.0F;
        pantsir.field_70122_E = true;
        pantsir.field_70153_n = driver;
        java.lang.reflect.Method updateDriving = EntityMobileAirDefense.class
                .getDeclaredMethod("updateDriving");
        updateDriving.setAccessible(true);
        for (int i = 0; i < 15; ++i) {
            updateDriving.invoke(pantsir);
        }
        require(Math.abs(pantsir.field_70159_w) + Math.abs(pantsir.field_70179_y) > 0.30D,
                "mobile air-defense vehicle must build useful road speed quickly");
        pantsir.func_70097_a(new DamageSource(), 600.0F);
        require(pantsir.field_70128_L,
                "a severe missile-scale hit must destroy mobile air defense");

        System.out.println("Mobile air-defense smoke test passed");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static final class TestWorld extends World {
        int particles;
        int sounds;

        TestWorld() {
            field_72995_K = false;
            field_73012_v = new Random(11L);
            field_72996_f = new ArrayList();
            field_147482_g = new ArrayList();
        }

        @Override
        public void func_72869_a(String particle, double x, double y, double z,
                double velocityX, double velocityY, double velocityZ) {
            ++particles;
        }

        @Override
        public void func_72956_a(Entity entity, String sound, float volume, float pitch) {
            ++sounds;
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

    private static final class TestMissile extends Entity implements IRadarDetectable {
        private final int id;

        TestMissile(World world, int id) {
            super(world);
            this.id = id;
        }

        @Override public int func_145782_y() { return id; }
        @Override public RadarTargetType getTargetType() {
            return RadarTargetType.MISSILE_TIER1;
        }
    }
}
