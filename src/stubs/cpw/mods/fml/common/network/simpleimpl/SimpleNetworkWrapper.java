package cpw.mods.fml.common.network.simpleimpl;

import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayerMP;

public class SimpleNetworkWrapper {
    public SimpleNetworkWrapper(String name) {}
    public <REQ extends IMessage, REPLY extends IMessage> void registerMessage(
            Class<? extends IMessageHandler<REQ, REPLY>> handler,
            Class<REQ> request, int discriminator, Side side) {}
    public void sendToServer(IMessage message) {}
    public void sendTo(IMessage message, EntityPlayerMP player) {}
}
