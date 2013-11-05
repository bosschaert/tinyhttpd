package org.coderthoughts.tinyhttpd;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.util.Dictionary;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

public class ServerController implements ManagedService {
    private int port = 8080; // TODO Start with no port?
    private volatile Channel channel;

    ServerController() {}

    void start() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

//        try {
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup).
            channel(NioServerSocketChannel.class).
            childHandler(new Initializer());

        System.out.println("Server starting on port: " + port);
        channel = b.bind(port).sync().channel(); // .closeFuture().sync();
        System.out.println("$$$$ Bind returned");

//            // TODO possibly better
//            // channel = b.bind(port).sync().channel()
//        } catch (InterruptedException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } finally {
//            bossGroup.shutdownGracefully();
//            workerGroup.shutdownGracefully();
//        }
    }

    void stop() throws InterruptedException {
        if (channel != null) {
            System.out.println("Server stopping port: " + port);
            channel.close().sync();
            System.out.println("Server stopped port: " + port);
        }
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        System.out.println("*** Updated: " + properties);
        Object p = properties.get("port");
        if (p != null) {
            try {
                int newPort = Integer.parseInt(p.toString());
                if (newPort != port) {
                    stop();
                    port = newPort;
                    start();
                }
            } catch (Exception e) {
                throw new ConfigurationException("port", "Unable to change port number: " + p, e);
            }
        }

    }

    public static void main(String [] args) throws InterruptedException {
        new ServerController().start();
    }

    /*
    private final NioSocketAcceptor acceptor;

    // Configuration properties
    private int port = 8080;

    public ServerController() {
        acceptor = new NioSocketAcceptor();
        // acceptor.setCloseOnDeactivation(true); TODO
        acceptor.getFilterChain().addLast("logger", new LoggingFilter());
        acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new TextLineCodecFactory(Charset.forName("UTF-8"))));

        acceptor.setHandler(new RequestHandler());
        acceptor.getSessionConfig().setReadBufferSize(2048);
        acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 10);
    }



    public void start() throws IOException {
        System.out.println("Starting:" + port);
        acceptor.bind(new InetSocketAddress(port));
    }

    public void stop() {
        // maybe add isActive... TODO
        System.out.println("Stopping:" + port);
        acceptor.unbind();
    }
    */

    static class Initializer extends ChannelInitializer<SocketChannel> {
        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast("decoder", new HttpRequestDecoder());
            pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
            pipeline.addLast("encoder", new HttpResponseEncoder());
            pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
            pipeline.addLast("handler", new HttpHandler());
        }
    }
}
