package redis.server.netty;

import com.google.common.base.Charsets;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import redis.netty4.Command;
import redis.netty4.ErrorReply;
import redis.netty4.InlineReply;
import redis.netty4.Reply;
import redis.util.BytesKey;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static redis.netty4.ErrorReply.NYI_REPLY;
import static redis.netty4.StatusReply.QUIT;

/**
 * Handle decoded commands
 */
@ChannelHandler.Sharable
public class RedisCommandHandler extends ChannelInboundHandlerAdapter {

  private Map<BytesKey, Wrapper> methods = new HashMap<BytesKey, Wrapper>();

  interface Wrapper {
    Reply execute(Command command) throws RedisException;
  }

  public RedisCommandHandler(final RedisServer rs) {
    Class<? extends RedisServer> aClass = rs.getClass();
    for (final Method method : aClass.getMethods()) {
      final Class<?>[] types = method.getParameterTypes();
      methods.put(new BytesKey(method.getName().getBytes()), new Wrapper() {
        @Override
        public Reply execute(Command command) throws RedisException {
          Object[] objects = new Object[types.length];
          try {
            command.toArguments(objects, types);
            return (Reply) method.invoke(rs, objects);
          } catch (IllegalAccessException e) {
            throw new RedisException("Invalid server implementation");
          } catch (InvocationTargetException e) {
            Throwable te = e.getTargetException();
            if (!(te instanceof RedisException)) {
              te.printStackTrace();
            }
            return new ErrorReply("ERR " + te.getMessage());
          } catch (Exception e) {
            return new ErrorReply("ERR " + e.getMessage());
          }
        }
      });
    }
  }

  private static final byte LOWER_DIFF = 'a' - 'A';

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.flush();
    super.channelReadComplete(ctx);
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object _msg) throws Exception {
    Command msg = (Command) _msg;
    byte[] name = msg.getName();
    for (int i = 0; i < name.length; i++) {
      byte b = name[i];
      if (b >= 'A' && b <= 'Z') {
        name[i] = (byte) (b + LOWER_DIFF);
      }
    }
    Wrapper wrapper = methods.get(new BytesKey(name));
    Reply reply;
    if (wrapper == null) {
      reply = new ErrorReply("unknown command '" + new String(name, Charsets.US_ASCII) + "'");
    } else {
      reply = wrapper.execute(msg);
    }
    if (reply == QUIT) {
      ctx.close();
    } else {
      if (msg.isInline()) {
        if (reply == null) {
          reply = new InlineReply(null);
        } else {
          reply = new InlineReply(reply.data());
        }
      }
      if (reply == null) {
        reply = NYI_REPLY;
      }
      ctx.write(reply);
    }
  }
}
