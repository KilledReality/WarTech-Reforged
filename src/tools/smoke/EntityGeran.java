package smoke;

import api.hbm.entity.IRadarDetectable;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/** Lightweight test double whose class name exercises the Geran signature rule. */
public final class EntityGeran extends Entity implements IRadarDetectable {
    public EntityGeran(World world) {
        super(world);
    }

    @Override
    public RadarTargetType getTargetType() {
        return RadarTargetType.MISSILE_TIER1;
    }
}
