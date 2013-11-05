package org.coderthoughts.tinyhttpd;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
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
    private int port = -1;
    private Channel channel;
    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;
    private ServerBootstrap serverBootstrap;

    ServerController() {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();

        serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup).
            channel(NioServerSocketChannel.class).
            childHandler(new Initializer());
    }

    synchronized void start() throws InterruptedException {
        System.out.print("Server starting on port: " + port + "...");
        channel = serverBootstrap.bind(port).sync().channel();
        System.out.println("done");
    }

    synchronized void stop() throws InterruptedException {
        if (channel != null) {
            System.out.print("Server stopping port: " + port + "...");
            channel.close().sync();
            System.out.println("done");
            channel = null;
        }
    }

    void shutdown() throws InterruptedException {
        stop();
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        System.out.println("Server shut down");
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        System.out.println("Server Configuration Updated: " + properties);
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

    /*
    public static void main(String [] args) throws InterruptedException {
        new ServerController().start();
    }*/

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
