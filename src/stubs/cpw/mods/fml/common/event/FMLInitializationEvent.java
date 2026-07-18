package cpw.mods.fml.common.event;

import cpw.mods.fml.relauncher.Side;

public class FMLInitializationEvent {
    public Side getSide() {
        return Side.SERVER;
    }
}
