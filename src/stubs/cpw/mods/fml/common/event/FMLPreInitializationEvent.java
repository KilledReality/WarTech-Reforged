package cpw.mods.fml.common.event;

import java.io.File;

public class FMLPreInitializationEvent {
    public File getModConfigurationDirectory() {
        return new File("config");
    }
}
