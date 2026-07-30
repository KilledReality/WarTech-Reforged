package io.netty.buffer;

public class ByteBuf {
    private byte[] data = new byte[128];
    private int readerIndex;
    private int writerIndex;

    public int readInt() {
        return readByteRaw() << 24 | readByteRaw() << 16
                | readByteRaw() << 8 | readByteRaw();
    }

    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    public short readUnsignedByte() {
        return (short) readByteRaw();
    }

    public boolean readBoolean() {
        return readByteRaw() != 0;
    }

    public int readUnsignedShort() {
        return readByteRaw() << 8 | readByteRaw();
    }

    public ByteBuf readBytes(byte[] output) {
        for (int i = 0; i < output.length; i++) output[i] = (byte) readByteRaw();
        return this;
    }

    public ByteBuf writeInt(int value) {
        writeByteRaw(value >>> 24);
        writeByteRaw(value >>> 16);
        writeByteRaw(value >>> 8);
        writeByteRaw(value);
        return this;
    }

    public ByteBuf writeDouble(double value) {
        writeLong(Double.doubleToLongBits(value));
        return this;
    }

    public ByteBuf writeFloat(float value) {
        writeInt(Float.floatToIntBits(value));
        return this;
    }

    public ByteBuf writeByte(int value) {
        writeByteRaw(value);
        return this;
    }

    public ByteBuf writeBoolean(boolean value) {
        writeByteRaw(value ? 1 : 0);
        return this;
    }

    public ByteBuf writeShort(int value) {
        writeByteRaw(value >>> 8);
        writeByteRaw(value);
        return this;
    }

    public ByteBuf writeBytes(byte[] input, int offset, int length) {
        for (int i = 0; i < length; i++) writeByteRaw(input[offset + i]);
        return this;
    }

    private long readLong() {
        return (long) readByteRaw() << 56 | (long) readByteRaw() << 48
                | (long) readByteRaw() << 40 | (long) readByteRaw() << 32
                | (long) readByteRaw() << 24 | (long) readByteRaw() << 16
                | (long) readByteRaw() << 8 | (long) readByteRaw();
    }

    private void writeLong(long value) {
        writeByteRaw((int) (value >>> 56));
        writeByteRaw((int) (value >>> 48));
        writeByteRaw((int) (value >>> 40));
        writeByteRaw((int) (value >>> 32));
        writeByteRaw((int) (value >>> 24));
        writeByteRaw((int) (value >>> 16));
        writeByteRaw((int) (value >>> 8));
        writeByteRaw((int) value);
    }

    private int readByteRaw() {
        return data[readerIndex++] & 255;
    }

    private void writeByteRaw(int value) {
        ensure(1);
        data[writerIndex++] = (byte) value;
    }

    private void ensure(int bytes) {
        if (writerIndex + bytes <= data.length) return;
        byte[] expanded = new byte[Math.max(data.length * 2, writerIndex + bytes)];
        System.arraycopy(data, 0, expanded, 0, writerIndex);
        data = expanded;
    }
}
