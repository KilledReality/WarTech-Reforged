import com.wartec.wartecmod.entity.vehicle.EntityMobileArtillery;
import net.minecraft.world.World;

public final class SmokeMobileArtilleryConstructor {
    public static void main(String[] args) {
        EntityMobileArtillery entity = new EntityMobileArtillery(new World());
        if (entity.func_70046_E() == null) {
            throw new IllegalStateException("Mobile artillery has no collision box");
        }
        System.out.println("mobile artillery constructor: OK");
    }
}
