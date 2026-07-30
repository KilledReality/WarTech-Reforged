package cpw.mods.fml.common.network.simpleimpl;

public interface IMessageHandler<REQ extends IMessage, REPLY extends IMessage> {
    REPLY onMessage(REQ message, MessageContext context);
}
