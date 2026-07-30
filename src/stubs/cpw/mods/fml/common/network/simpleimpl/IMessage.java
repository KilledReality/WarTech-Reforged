package cpw.mods.fml.common.network.simpleimpl;

import io.netty.buffer.ByteBuf;

public interface IMessage {
    void fromBytes(ByteBuf buffer);
    void toBytes(ByteBuf buffer);
}
