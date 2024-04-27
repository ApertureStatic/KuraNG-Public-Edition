package helper.kura.socket;

import helper.kura.socket.client.SocketClient;
import helper.kura.socket.packet.ChatMessagePacket;
import helper.kura.socket.packet.Packet;
import helper.kura.socket.utils.ChannelType;

/**
 * @author DiaoLing
 * @since 4/8/2024
 */
public class SocketManager {
    private static final String prefix = "!";
    private final SocketClient client = new SocketClient();

    public SocketClient getClient() {
        return client;
    }

    public String getPrefix() {
        return prefix;
    }

    public void send(Packet packet) {
        client.send(packet);
    }

    // 我去发你
    public void chat(String message) {
        this.send(new ChatMessagePacket(
                ChannelType.GLOBAL,
                message,
                System.currentTimeMillis()));
    }

//    public void operation(Operation operation, String targetUsername, String message) {
//        this.send(new OperationPacket(
//                UserManager.getUser().getUsername(),
//                targetUsername,
//                message,
//                operation
//        ));
//    }
}
