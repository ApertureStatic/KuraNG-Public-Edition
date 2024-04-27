package helper.kura.socket.packet;

import helper.kura.socket.handler.ClientHandler;
import helper.kura.socket.utils.ChannelType;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author DiaoLing
 * @since 4/8/2024
 */
public class ChatMessagePacket extends Packet {
    private ChannelType channel;
    private String message;
    private long timestamp;

    public ChatMessagePacket() {
    }

    public ChatMessagePacket(ChannelType channel, String message, long timestamp) {
        this.channel = channel;
        this.message = message;
        this.timestamp = timestamp;
    }

    public ChannelType getChannel() {
        return channel;
    }

    public void setChannel(ChannelType channel) {
        this.channel = channel;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public void encode(PacketBuffer buf) {
        buf.writeEnum(channel);
        buf.writeString(message);
        buf.writeLong(timestamp);
    }

    @Override
    public void decode(PacketBuffer buf) {
        this.channel = buf.readEnum(ChannelType.class);
        this.message = buf.readString();
        this.timestamp = buf.readLong();
    }

    @Override
    public void handler(ChannelHandlerContext ctx, ClientHandler handler) {

    }
}