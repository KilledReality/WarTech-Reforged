package io.netty.buffer;

public final class Unpooled {
    private Unpooled() {
    }

    public static ByteBuf buffer() {
        return new ByteBuf();
    }
}
