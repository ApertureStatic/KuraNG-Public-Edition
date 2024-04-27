package helper.kura.socket.handler.encryption;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author DiaoLing
 * @since 4/8/2024
 */
public class EncryptionInboundHandler extends ChannelInboundHandlerAdapter {
    private final SecretKeySpec key;
    private final IvParameterSpec iv;
    private final Cipher cipher;

    public EncryptionInboundHandler() {
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
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf in = (ByteBuf) msg;
        byte[] bytes = new byte[in.readableBytes()];
        in.readBytes(bytes);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        byte[] decrypted = cipher.doFinal(bytes);
        ByteBuf out = Unpooled.wrappedBuffer(decrypted);
        ctx.fireChannelRead(out);
    }
}