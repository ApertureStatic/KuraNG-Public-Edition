package helper.kura.socket.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @author DiaoLing
 * @since 4/7/2024
 */
public class PacketBuffer {
    private final ByteBuf buffer;

    public PacketBuffer() {
        this.buffer = Unpooled.buffer();
    }

    public PacketBuffer(ByteBuf existingBuffer) {
        this.buffer = existingBuffer;
    }

    public void writeInt(int value) {
        buffer.writeInt(value);
    }

    public void writeLong(long value) {
        buffer.writeLong(value);
    }

    public void writeDouble(double value) {
        buffer.writeDouble(value);
    }

    public void writeString(String value) {
        byte[] data = value.getBytes(StandardCharsets.UTF_8);
        buffer.writeInt(data.length);
        buffer.writeBytes(data);
    }

    public int readInt() {
        return buffer.readInt();
    }

    public long readLong() {
        return buffer.readLong();
    }

    public double readDouble() {
        return buffer.readDouble();
    }

    public String readString() {
        int length = buffer.readInt();
        byte[] data = new byte[length];
        buffer.readBytes(data);
        return new String(data, StandardCharsets.UTF_8);
    }

    public <E extends Enum<E>> void writeEnum(E value) {
        buffer.writeInt(value.ordinal());
    }

    public <E extends Enum<E>> E readEnum(Class<E> enumClass) {
        int ordinal = buffer.readInt();
        return enumClass.getEnumConstants()[ordinal];
    }

    public <T> List<T> readList(Function<PacketBuffer, T> elementReader) {
        int size = readInt();
        List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            T element = elementReader.apply(this);
            list.add(element);
        }
        return list;
    }

    public <T> void writeList(List<T> list, BiConsumer<PacketBuffer, T> elementWriter) {
        writeInt(list.size());
        for (T element : list) {
            elementWriter.accept(this, element);
        }
    }

    public ByteBuf getInternalBuffer() {
        return buffer;
    }

    public void clear() {
        buffer.clear();
    }
}
