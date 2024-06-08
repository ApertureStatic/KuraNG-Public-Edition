package helper.kura.socket.packet;

import dev.dyzjct.kura.manager.NotificationManager;
import base.utils.chat.ChatUtil;
import dev.dyzjct.kura.module.modules.client.IRC;
import helper.kura.socket.handler.ClientHandler;
import helper.kura.socket.utils.ChannelType;
import helper.kura.socket.utils.ChatType;
import helper.kura.socket.utils.ClientType;
import helper.kura.socket.utils.Rank;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.MinecraftClient;

/**
 * @author DiaoLing
 * @since 4/8/2024
 */
public class MessagePacket extends Packet {
    private ClientType client;
    private ChannelType channel;
    private Rank rank;
    private ChatType chat;
    private String username;
    private String message;
    private long timestamp;

    public MessagePacket() {
    }

    public MessagePacket(ClientType client, Rank rank, ChannelType channel, ChatType chat, String username, String message, long timestamp) {
        this.client = client;
        this.rank = rank;
        this.channel = channel;
        this.chat = chat;
        this.username = username;
        this.message = message;
        this.timestamp = timestamp;
    }

    public ClientType getClient() {
        return client;
    }

    public Rank getRank() {
        return rank;
    }

    public ChannelType getChannel() {
        return channel;
    }

    public ChatType getChat() {
        return chat;
    }

    public String getUsername() {
        return username;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public void encode(PacketBuffer buf) {
        buf.writeEnum(client);
        buf.writeEnum(rank);
        buf.writeEnum(channel);
        buf.writeEnum(chat);
        buf.writeString(username);
        buf.writeString(message);
        buf.writeLong(timestamp);
    }

    @Override
    public void decode(PacketBuffer buf) {
        this.client = buf.readEnum(ClientType.class);
        this.channel = buf.readEnum(ChannelType.class);
        this.chat = buf.readEnum(ChatType.class);
        this.username = buf.readString();
        this.message = buf.readString();
        this.timestamp = buf.readLong();
    }

    @Override
    public void handler(ChannelHandlerContext ctx, ClientHandler handler) {
        if (getUsername().equals(MinecraftClient.getInstance().getSession().getSessionId())) return;
        switch ((IRC.Mode) IRC.INSTANCE.getMode()) {
            case Both -> {
                ChatUtil.INSTANCE.sendMessage(ChatUtil.INSTANCE.getDARK_AQUA() + "[IRC] " + ChatUtil.INSTANCE.getDARK_BLUE()+ ChatUtil.INSTANCE.getWHITE() + getUsername()  + getMessage());
                NotificationManager.INSTANCE.addNotification(ChatUtil.INSTANCE.getDARK_AQUA() + "[IRC] " + ChatUtil.INSTANCE.getDARK_BLUE()  + ChatUtil.INSTANCE.getWHITE() + getUsername()  + getMessage());
            }
            case Chat ->
                    ChatUtil.INSTANCE.sendMessage(ChatUtil.INSTANCE.getDARK_AQUA() + "[IRC] " + ChatUtil.INSTANCE.getWHITE() + getUsername() + getMessage());
            case Notification ->
                    NotificationManager.INSTANCE.addNotification(ChatUtil.INSTANCE.getDARK_AQUA() + "[IRC] "  + ChatUtil.INSTANCE.getWHITE() + getUsername()  + getMessage());
        }
    }
}
