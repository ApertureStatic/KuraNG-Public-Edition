package helper.kura.socket.handler;

import base.utils.Wrapper;
import helper.kura.socket.packet.Packet;
import helper.kura.socket.packet.UserInfoPacket;
import helper.kura.socket.utils.ClientType;
import helper.kura.socket.utils.Rank;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

import java.net.InetAddress;
import java.net.SocketException;

/**
 * @author DiaoLing
 * @since 4/7/2024
 */
public class ClientHandler extends SimpleChannelInboundHandler<Packet> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) {
        Log.info(LogCategory.LOG, "Received packet: " + packet.getClass().getSimpleName());
        packet.handler(ctx, this);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Log.info(LogCategory.LOG, "Connected to server: " + ctx.channel().remoteAddress());
        InetAddress address = InetAddress.getLocalHost();
        try {
            ctx.writeAndFlush(new UserInfoPacket(
                    ClientType.Kura,
                    0,
                    "§d" + address.getHostAddress() + "§f" + Wrapper.getPlayer().getName().getString(),
                    Rank.USER,
                    0,
                    114514));
        } catch (Exception e) {
            Log.error(LogCategory.LOG, e.getMessage());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Log.error(LogCategory.LOG, "Disconnected from server.");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof SocketException) {
            Log.error(LogCategory.LOG, "Connection reset by peer or server shutdown.");
        } else {
            cause.printStackTrace();
        }
        ctx.close();
    }
}