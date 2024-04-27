package helper.kura.socket.handler;

import helper.kura.socket.packet.PacketBuffer;
import helper.kura.socket.packet.Packet;
import helper.kura.socket.packet.PacketFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * @author DiaoLing
 * @since 4/7/2024
 */
public class PacketDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int packetId = in.readInt();

        Packet packet = PacketFactory.createPacket(packetId);
        PacketBuffer buffer = new PacketBuffer(in);

        if (packet != null) {
            packet.decode(buffer);

            out.add(packet);
        }
    }
}