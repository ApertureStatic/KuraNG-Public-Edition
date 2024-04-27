package helper.kura.socket.client;

import helper.kura.socket.packet.Packet;
import helper.kura.socket.utils.ConnectionState;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author DiaoLing
 * @since 4/7/2024
 */
public class SocketClient {
    private final String host;
    private final int port;
    private Channel channel;
    private ExecutorService executorService;
    private ConnectionState connectionState = ConnectionState.DISCONNECTED;

    public SocketClient() {
        this("localhost", 45600);
    }

    public SocketClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.executorService = Executors.newFixedThreadPool(2);
    }

    public void start() {
        start(this.host, this.port);
    }

    public void start(String host, int port) {
        if (isConnected() || isConnecting()) {
            Log.info(LogCategory.LOG, "Client is already connected or connecting.");
            return;
        }
        if (executorService.isShutdown() || executorService.isTerminated()) {
            executorService = Executors.newFixedThreadPool(2);
        }
        setConnectionState(ConnectionState.CONNECTING);
        executorService.submit(() -> {
            EventLoopGroup group = new NioEventLoopGroup();
            try {
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(group)
                        .channel(NioSocketChannel.class)
                        .handler(new ClientInitializer())
                        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

                while (true) {
                    try {
                        ChannelFuture future = bootstrap.connect(host, port).sync();
                        this.channel = future.channel();
                        setConnectionState(ConnectionState.CONNECTED);

                        channel.closeFuture().sync();
                        break;
                    } catch (InterruptedException e) {
                        // 你帮忙改下
                        Log.info(LogCategory.LOG, "Interrupted during connection. Exiting.");
                        break;
                    } catch (Exception e) {
                        Log.info(LogCategory.LOG, "Connection failed. Retrying in 5 seconds...");
                        Thread.sleep(5000);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.info(LogCategory.LOG, "Thread was interrupted, Failed to complete operation.");
            }
        });
    }

    public void send(Packet packet) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(packet).addListener(future -> {
                if (!future.isSuccess()) {
                    Log.info(LogCategory.LOG, "Failed to send packet: " + future.cause().getMessage());
                }
            });
        } else {
            Log.info(LogCategory.LOG, "Channel is not active. Cannot send packet.");
        }
    }

    public void disconnect() {
        if (channel != null && channel.isOpen()) {
            channel.close().addListener(future -> {
                if (future.isSuccess()) {
                    Log.info(LogCategory.LOG, "Disconnected successfully.");
                } else {
                    Log.info(LogCategory.LOG, "Failed to disconnect.");
                }
                setConnectionState(ConnectionState.DISCONNECTED);
            });
            this.shutdown();
        } else {
            Log.info(LogCategory.LOG, "Channel is already closed or not initialized.");
        }
    }

    public void shutdown() {
        executorService.shutdownNow();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                Log.info(LogCategory.LOG, "Executor did not terminate in the allotted time.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean isConnected() {
        return connectionState == ConnectionState.CONNECTED && this.channel.isActive();
    }

    public boolean isConnecting() {
        return connectionState == ConnectionState.CONNECTING;
    }

    public Channel getChannel() {
        return channel;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public ConnectionState getConnectionState() {
        return connectionState;
    }

    private void setConnectionState(ConnectionState newState) {
        this.connectionState = newState;
        Log.info(LogCategory.LOG, "Connection state changed to: " + newState);
    }
}
