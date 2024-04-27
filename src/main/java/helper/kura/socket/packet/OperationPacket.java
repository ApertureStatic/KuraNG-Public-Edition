package helper.kura.socket.packet;

import helper.kura.socket.handler.ClientHandler;
import helper.kura.socket.utils.Operation;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author DiaoLing
 * @since 4/10/2024
 */
public class OperationPacket extends Packet {
    private String initiatorUsername;
    private String targetUsername;
    private String message;
    private Operation operation;

    public OperationPacket() {
    }

    public OperationPacket(String initiatorUsername, String targetUsername, String message, Operation operation) {
        this.initiatorUsername = initiatorUsername;
        this.targetUsername = targetUsername;
        this.message = message;
        this.operation = operation;
    }

    public String getInitiatorUsername() {
        return initiatorUsername;
    }

    public void setInitiatorUsername(String initiatorUsername) {
        this.initiatorUsername = initiatorUsername;
    }

    public String getTargetUsername() {
        return targetUsername;
    }

    public void setTargetUsername(String targetUsername) {
        this.targetUsername = targetUsername;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    @Override
    public void encode(PacketBuffer buf) {
        buf.writeString(initiatorUsername);
        buf.writeString(targetUsername);
        buf.writeString(message);
        buf.writeEnum(operation);
    }

    @Override
    public void decode(PacketBuffer buf) {
        this.initiatorUsername = buf.readString();
        this.targetUsername = buf.readString();
        this.message = buf.readString();
        this.operation = buf.readEnum(Operation.class);
    }

    @Override
    public void handler(ChannelHandlerContext ctx, ClientHandler handler) {
        getOperation().handler(getMessage());
    }
}