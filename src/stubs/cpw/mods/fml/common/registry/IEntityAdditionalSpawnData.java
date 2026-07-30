package cpw.mods.fml.common.registry;

import io.netty.buffer.ByteBuf;

public interface IEntityAdditionalSpawnData {
    void writeSpawnData(ByteBuf buffer);
    void readSpawnData(ByteBuf additionalData);
}
