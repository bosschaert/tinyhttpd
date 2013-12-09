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

/**
 * The ServerController starts up the Netty components and thread pools.
 * It also registers a ManagedService in the OSGi Service Registry which
 * is used to receive the configuration for the server.
 * The server is only started once the configuration defining the port
 * number is received.
 * The configuration needs to be provided through the OSGi Configuration
 * Admin service on the PID org.coderthoughts.tinyhttpd where the
 * following values are recognised:
 *   port = port number for the server
 *   root = web root directory on local disk, defaults to the user.dir property
 * The configuration values can be changed dynamically at runtime.
 */
class ServerController implements ManagedService {
    static final int MAX_HTTP_CONTENT_LENGTH = 2 * 1024 * 1024; // 2mb

    private Channel channel;
    private final ServerBootstrap serverBootstrap;
    private final NioEventLoopGroup bossGroup;
    private final NioEventLoopGroup workerGroup;

    // Configuration properties
    private int port = -1; // Port must be configured before server is started
    private volatile String webRoot = System.getProperty("user.home");

    /**
     * The constructor creates the thread pools and bootstraps the server.
     */
    ServerController() {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();

        serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup).
            channel(NioServerSocketChannel.class).
            childHandler(new ChildHandlerInitializer());
    }

    /**
     * Start the server.
     */
    synchronized void start() throws InterruptedException {
        System.out.print("Server starting on port: " + port + "...");
        channel = serverBootstrap.bind(port).sync().channel();
        System.out.println("done");
    }

    /**
     * Stop the server.
     */
    synchronized void stop() throws InterruptedException {
        if (channel != null) {
            System.out.print("Server stopping port: " + port + "...");
            channel.close().sync();
            System.out.println("done");
            channel = null;
        }
    }

    /**
     * Shut down the server. This will also stop the server if needed.
     */
    void shutdown() throws InterruptedException {
        stop();
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        System.out.println("Server shut down");
    }

    /**
     * Callback mechanism used by the OSGi Configuration Admin service to provide configuration.
     * Note that configuration can change dynamically, so multiple calls to this method can be
     * expected and the system should reconfigure itself when this happens.
     * The server is not started until at least configuration for the 'port' is received.
     * @param properties The current configuration properties.
     */
    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        if (properties == null)
            return;

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

        Object root = properties.get("root");
        if (root instanceof String) {
            webRoot = (String) root;
        }
    }

    /**
     * Initialises the Netty pipe line.
     */
    class ChildHandlerInitializer extends ChannelInitializer<SocketChannel> {
        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast("decoder", new HttpRequestDecoder());
            pipeline.addLast("aggregator", new HttpObjectAggregator(MAX_HTTP_CONTENT_LENGTH));
            pipeline.addLast("encoder", new HttpResponseEncoder());
            pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
            pipeline.addLast("handler", new HttpHandler(webRoot));
        }
    }
}
