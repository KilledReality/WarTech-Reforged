package cpw.mods.fml.common.network;

public final class NetworkRegistry {
    public static final NetworkRegistry INSTANCE = new NetworkRegistry();

    public void registerGuiHandler(Object mod, IGuiHandler handler) {
    }
    public cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper newSimpleChannel(
            String name) {
        return new cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper(name);
    }
}
