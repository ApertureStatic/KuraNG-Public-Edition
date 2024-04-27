package helper.kura.socket.packet;

import helper.kura.socket.handler.ClientHandler;
import helper.kura.socket.utils.ClientType;
import helper.kura.socket.utils.Rank;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author DiaoLing
 * @since 4/8/2024
 */
public class UserInfoPacket extends Packet {
    private ClientType client;
    private int userId;
    private String username;
    private Rank rank;
    private long expiryDate;
    private double balance;

    public UserInfoPacket() {
    }

    public UserInfoPacket(ClientType client, int userId, String username, Rank rank, long expiryDate, double balance) {
        this.client = client;
        this.userId = userId;
        this.username = username;
        this.rank = rank;
        this.expiryDate = expiryDate;
        this.balance = balance;
    }

    @Override
    public void encode(PacketBuffer buf) {
        buf.writeEnum(client);
        buf.writeInt(userId);
        buf.writeString(username);
        buf.writeEnum(rank);
        buf.writeLong(expiryDate);
        buf.writeDouble(balance);
    }

    @Override
    public void decode(PacketBuffer buf) {
        this.client = buf.readEnum(ClientType.class);
        this.userId = buf.readInt();
        this.username = buf.readString();
        this.rank = buf.readEnum(Rank.class);
        this.expiryDate = buf.readLong();
        this.balance = buf.readDouble();
    }

    @Override
    public void handler(ChannelHandlerContext ctx, ClientHandler handler) {

    }

    public ClientType getClient() {
        return client;
    }

    public void setClient(ClientType client) {
        this.client = client;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Rank getRank() {
        return rank;
    }

    public void setRank(Rank rank) {
        this.rank = rank;
    }

    public long getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(long expiryDate) {
        this.expiryDate = expiryDate;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
}