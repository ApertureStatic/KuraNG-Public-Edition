package helper.kura.socket.packet;

import helper.kura.socket.handler.ClientHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author DiaoLing
 * @since 4/8/2024
 */
public class GameInfoPacket extends Packet {
    private String inGameName;
    //   private String token;
    private String uuid;
    private long lastUpdateTime;

    public GameInfoPacket() {
    }

    public GameInfoPacket(String inGameName, String token, String uuid, long lastUpdateTime) {
        this.inGameName = inGameName;
        //    this.token = token;
        this.uuid = uuid;
        this.lastUpdateTime = lastUpdateTime;
    }

    @Override
    public void encode(PacketBuffer buf) {
        buf.writeString(inGameName);
        //    buf.writeString(token);
        buf.writeString(uuid);
        buf.writeLong(lastUpdateTime);
    }

    @Override
    public void decode(PacketBuffer buf) {
        this.inGameName = buf.readString();
        //    this.token = buf.readString();
        this.uuid = buf.readString();
        this.lastUpdateTime = buf.readLong();
    }

    @Override
    public void handler(ChannelHandlerContext ctx, ClientHandler handler) {

    }

    public String getInGameName() {
        return inGameName;
    }

    public void setInGameName(String inGameName) {
        this.inGameName = inGameName;
    }

//    public String getToken() {
//        return token;
//    }

//    public void setToken(String token) {
//        this.token = token;
//    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
}