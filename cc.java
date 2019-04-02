
package example2.server;
 
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
 
import java.util.logging.Level;
import java.util.logging.Logger;
 
import com.google.protobuf.ExtensionRegistry;
 
import example2.proto.Equip;
import example2.proto.Example;
import example2.proto.Friend;
import example2.server.handler.ProtoBufServerHandler;
 
public class NettyServer {
 
    private static final int PORT = 1588;
 
    private static Logger logger = Logger.getLogger(NettyServer.class.getName());
 
    public void start(int port) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup);
            b.channel(NioServerSocketChannel.class);
            b.option(ChannelOption.TCP_NODELAY, true);
            b.option(ChannelOption.SO_BACKLOG, 1024);
            b.childHandler(new ChannelInitializer<SocketChannel>() {
 
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    //decoded
                    ch.pipeline().addLast(new ProtobufVarint32FrameDecoder());
                    ExtensionRegistry registry = ExtensionRegistry.newInstance();
                    Equip.registerAllExtensions(registry);
                    Friend.registerAllExtensions(registry);
                    ch.pipeline().addLast(new ProtobufDecoder(Example.BaseData.getDefaultInstance(), registry));
                    //encoded
                    ch.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
                    ch.pipeline().addLast(new ProtobufEncoder());
                    // 注册handler
                    ch.pipeline().addLast(new ProtoBufServerHandler());
                }
            });
            //绑定端口 同步等待成功
            ChannelFuture f = b.bind(port).sync();
            //等待服务端监听端口关闭
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
 
  