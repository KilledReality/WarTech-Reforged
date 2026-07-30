package cpw.mods.fml.common.network.simpleimpl;

import net.minecraft.network.NetHandlerPlayServer;

public class MessageContext {
    private final NetHandlerPlayServer server = new NetHandlerPlayServer();
    public NetHandlerPlayServer getServerHandler() { return server; }
}
