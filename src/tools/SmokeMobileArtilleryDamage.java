import com.wartec.wartecmod.entity.vehicle.EntityMobileArtillery;
import java.lang.reflect.Field;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public final class SmokeMobileArtilleryDamage {
    public static void main(String[] args) throws Exception {
        EntityMobileArtillery vehicle = new EntityMobileArtillery(new World());
        vehicle.field_70173_aa = 100;
        DamageSource hit = new DamageSource();
        Field healthField = EntityMobileArtillery.class.getDeclaredField("vehicleHealth");
        healthField.setAccessible(true);
        for (int i = 0; i < 6; i++) {
            vehicle.func_70097_a(hit, 1000.0F);
            if (healthField.getDouble(vehicle) <= 0.0D) {
                throw new IllegalStateException("vehicle destroyed too early at hit " + (i + 1));
            }
        }
        if (healthField.getDouble(vehicle) != 50.0D) {
            throw new IllegalStateException("unexpected health after six hits: "
                    + healthField.getDouble(vehicle));
        }
        vehicle.func_70097_a(hit, 1000.0F);
        if (healthField.getDouble(vehicle) > 0.0D) {
            throw new IllegalStateException("vehicle did not reach lethal damage");
        }
        System.out.println("mobile artillery damage: OK");
    }
}
