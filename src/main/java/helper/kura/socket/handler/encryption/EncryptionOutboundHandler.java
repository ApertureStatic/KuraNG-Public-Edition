package helper.kura.socket.handler.encryption;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author DiaoLing
 * @since 4/8/2024
 */
public class EncryptionOutboundHandler extends ChannelOutboundHandlerAdapter {
    private final SecretKeySpec key;
    private final IvParameterSpec iv;
    private final Cipher cipher;

    public EncryptionOutboundHandler() {
        String algorithm = "AES/CTR/NoPadding";
        byte[] keyBytes = "78WYsdGDUsYFa1DF".getBytes();
        byte[] ivBytes = new byte[16];
        key = new SecretKeySpec(keyBytes, "AES");
        iv = new IvParameterSpec(ivBytes);
        try {
            cipher = Cipher.getInstance(algorithm);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ByteBuf in = (ByteBuf) msg;
        byte[] bytes = new byte[in.readableBytes()];
        in.readBytes(bytes);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        byte[] encrypted = cipher.doFinal(bytes);
        ByteBuf out = Unpooled.wrappedBuffer(encrypted);
        ctx.write(out, promise);
    }
}