package redis.server.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import redis.netty4.Command;

import java.io.IOException;
import java.util.List;

import static redis.netty4.RedisReplyDecoder.readLong;

/**
 * Decode commands.
 */
public class RedisCommandDecoder extends ReplayingDecoder<Void> {

  private byte[][] bytes;
  private int arguments = 0;

  /**
   * Decode the from one {@link io.netty.buffer.ByteBuf} to an other. This method will be called till either the input
   * {@link io.netty.buffer.ByteBuf} has nothing to read anymore, till nothing was read from the input {@link io.netty.buffer.ByteBuf} or till
   * this method returns {@code null}.
   *
   * @param ctx the {@link io.netty.channel.ChannelHandlerContext} which this decoder belongs to
   * @param in  the {@link io.netty.buffer.ByteBuf} from which to read data
   * @param out the {@link java.util.List} to which decoded messages should be added
   * @throws Exception is thrown if an error accour
   */
  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    if (bytes != null) {
      int numArgs = bytes.length;
      for (int i = arguments; i < numArgs; i++) {
        if (in.readByte() == '$') {
          long l = readLong(in);
          if (l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Java only supports arrays up to " + Integer.MAX_VALUE + " in size");
          }
          int size = (int) l;
          bytes[i] = new byte[size];
          in.readBytes(bytes[i]);
          if (in.forEachByte(ByteBufProcessor.FIND_CRLF) != 0) {
            throw new RedisException("Argument doesn't end in CRLF");
          }
          in.skipBytes(2);
          arguments++;
          checkpoint();
        } else {
          throw new IOException("Unexpected character");
        }
      }
      try {
        out.add(new Command(bytes));
      } finally {
        bytes = null;
        arguments = 0;
      }
    } else if (in.readByte() == '*') {
      long l = readLong(in);
      if (l > Integer.MAX_VALUE) {
        throw new IllegalArgumentException("Java only supports arrays up to " + Integer.MAX_VALUE + " in size");
      }
      int numArgs = (int) l;
      if (numArgs < 0) {
        throw new RedisException("Invalid size: " + numArgs);
      }
      bytes = new byte[numArgs][];
      checkpoint();
      decode(ctx, in, out);
    } else {
      // Go backwards one
      in.readerIndex(in.readerIndex() - 1);
      // Read command -- can't be interupted
      byte[][] b = new byte[1][];
      b[0] = new byte[in.forEachByte(ByteBufProcessor.FIND_CRLF) - in.readerIndex()];
      in.readBytes(b[0]);
      in.skipBytes(2);
      out.add(new Command(b, true));
    }
  }
}
