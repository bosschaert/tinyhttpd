package org.coderthoughts.tinyhttpd.itests;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.DiskAttribute;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.CharsetUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TinyHttpdSystemTest {
    @Configuration
    public Option[] config() {
        return CoreOptions.options(
            // CoreOptions.vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"), // for debugging
            CoreOptions.vmOption("-Dfelix.fileinstall.dir=" + System.getProperty("user.dir")
                + File.separator + "target" + File.separator + "test-classes" + File.separator + "load"),
            CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.configadmin").versionAsInProject(),
            CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.fileinstall").versionAsInProject(),
            CoreOptions.mavenBundle("io.netty", "netty-buffer").versionAsInProject(),
            CoreOptions.mavenBundle("io.netty", "netty-codec").versionAsInProject(),
            CoreOptions.mavenBundle("io.netty", "netty-codec-http").versionAsInProject(),
            CoreOptions.mavenBundle("io.netty", "netty-common").versionAsInProject(),
            CoreOptions.mavenBundle("io.netty", "netty-handler").versionAsInProject(),
            CoreOptions.mavenBundle("io.netty", "netty-transport").versionAsInProject(),
            CoreOptions.mavenBundle("org.coderthoughts.tinyhttpd", "core").versionAsInProject(),
            CoreOptions.junitBundles());
    }

	@Test
	public void testReadWebResource() throws Exception {
		URL url = new URL("http://localhost:7070");
        String content = tryReadURL(url);
		Assert.assertTrue(content.contains("Allaert Joachim David Bosschaert"));
	}

	@Test
	public void testReadHeaders() throws Exception {
	    // Make sure the server is up and running
        tryReadURL(new URL("http://localhost:7070/images/david.png"));

        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:7070/images/david.png").openConnection();
        try {
    		Assert.assertEquals(200, connection.getResponseCode());
    		Assert.assertEquals("image/png", connection.getHeaderField("Content-Type"));
    		Assert.assertEquals("keep-alive", connection.getHeaderField("Connection"));
    		Assert.assertEquals("private, max-age=60", connection.getHeaderField("Cache-Control"));

    		File fileRes = new File(System.getProperty("felix.fileinstall.dir") + "/../web-root/images/david.png");
    		Assert.assertEquals(fileRes.length(), connection.getHeaderFieldInt("Content-Length", -1));
    		Assert.assertEquals(fileRes.lastModified(), connection.getHeaderFieldDate("Last-Modified", -1));
        } finally {
            connection.disconnect();
        }
	}

	@Test
	public void testUpload() throws Exception {
        // Make sure the server is up and running
        tryReadURL(new URL("http://localhost:7070/images/david.png"));

	    // This is where the upload file will end up...
        File uploadedFileRes = new File(System.getProperty("felix.fileinstall.dir") + "/../web-root/images/index.html");
        if (uploadedFileRes.exists()) {
            // Delete it if it was there from an earlier test run.
            Assert.assertTrue("Precondition", uploadedFileRes.delete());
        }

        URI postURI = new URI("/images/");
        File fileRes = new File(System.getProperty("felix.fileinstall.dir") + "/../web-root/index.html");

        HttpUploadTestResponseHandler responseHandler = uploadFile(postURI, fileRes);
        Assert.assertEquals(200, responseHandler.status.code());
        Assert.assertEquals("text/html; charset=UTF-8", responseHandler.headers.get("Content-Type"));
        String content = responseHandler.content.toString();
        int idx1 = content.indexOf("david.png");
        Assert.assertTrue("Returned directory listing should contain 'david.png'", idx1 > 0);
        int idx2 = content.indexOf("index.html");
        Assert.assertTrue("Returned directory listing should contain 'index.html'", idx2 > 0);
        Assert.assertTrue("david.png should be ordered before index.html", idx1 < idx2);

        Assert.assertEquals(fileRes.length(), uploadedFileRes.length());

        byte[] bytes1 = Streams.suck(new FileInputStream(fileRes)); // suck closes the input stream when done
        byte[] bytes2 = Streams.suck(new FileInputStream(uploadedFileRes));
        Assert.assertTrue("Uploaded file content not identical to original", Arrays.equals(bytes1, bytes2));

        // upload another index.html, where one already exists - should produce a 406
        File fileResAlt = new File(System.getProperty("felix.fileinstall.dir") + "/../alt-root/index.html");
        HttpUploadTestResponseHandler responseHandler2 = uploadFile(postURI, fileResAlt);
        Assert.assertEquals("Uploading a file that already exists should produce a HTTP 406 status code",
                HttpResponseStatus.NOT_ACCEPTABLE.code(), responseHandler2.status.code());
        byte[] bytes3 = Streams.suck(new FileInputStream(uploadedFileRes));
        Assert.assertTrue("Uploaded file content should not have been changed", Arrays.equals(bytes1, bytes3));
	}

    /*
    @Test
    public void testDirectory() throws Exception {
        URL url = new URL("http://localhost:7070/images/david.png");
        tryReadURL(url);

        URL dirURL = new URL("http://localhost:7070/images/");
        String s = tryReadURL(dirURL);
        System.out.println(s);
    }


    // TODO test reconfigure
    // TODO test upload
    */

    private HttpUploadTestResponseHandler uploadFile(URI postURI, File fileToUpload) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        HttpDataFactory factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE);

        // The following are boilerplate Netty configuration settings
        DiskFileUpload.deleteOnExitTemporaryFile = true; // should delete file on exit (in normal exit)
        DiskFileUpload.baseDirectory = null; // system temp directory
        DiskAttribute.deleteOnExitTemporaryFile = true; // should delete file on exit (in normal exit)
        DiskAttribute.baseDirectory = null; // system temp directory
        try {
            HttpUploadTestResponseHandler responseHandler = new HttpUploadTestResponseHandler();

            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group).channel(NioSocketChannel.class).handler(new HttpUploadTestClientInitializer(responseHandler));

            Channel channel = bootstrap.connect("localhost", 7070).sync().channel();
            HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, postURI.toASCIIString());
            request.headers().set(HttpHeaders.Names.HOST, "localhost");
            request.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
            request.headers().set(HttpHeaders.Names.ACCEPT_CHARSET, "UTF-8");
            request.headers().set(HttpHeaders.Names.REFERER, postURI.toString());
            request.headers().set(HttpHeaders.Names.USER_AGENT, "tinyhttpd itests via Netty");
            request.headers().set(HttpHeaders.Names.ACCEPT, "text/html,text/plain");

            HttpPostRequestEncoder bodyRequestEncoder = new HttpPostRequestEncoder(factory, request, true);
            bodyRequestEncoder.addBodyFileUpload("file", fileToUpload, "text/plain", true);
            request = bodyRequestEncoder.finalizeRequest();
            channel.write(request);

            if (bodyRequestEncoder.isChunked()) {
                channel.writeAndFlush(bodyRequestEncoder).awaitUninterruptibly();
            } else {
                channel.flush();
            }
            bodyRequestEncoder.cleanFiles();
            channel.closeFuture().sync();

            return responseHandler;
        } finally {
            group.shutdownGracefully();
            factory.cleanAllHttpDatas();
        }
    }

    private String tryReadURL(URL url) throws Exception {
        int retries = 20;
        while (--retries > 0) {
            try {
                System.out.println("Trying to read from: " + url);
                return new String(Streams.suck(url.openStream()));
            } catch (Exception e) {
                // ignore
            }
            Thread.sleep(500);
        }
        throw new IOException("Unable to read from URL: " + url);
    }

    static class HttpUploadTestClientInitializer extends ChannelInitializer<SocketChannel> {
        private final HttpUploadTestResponseHandler handler;

        HttpUploadTestClientInitializer(HttpUploadTestResponseHandler h) {
            handler = h;
        }

        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            // Set up a Netty pipe line...
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast("codec", new HttpClientCodec());
            pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
            pipeline.addLast("handler", handler);
        }
    }

    static class HttpUploadTestResponseHandler extends SimpleChannelInboundHandler<HttpObject> {
        HttpResponseStatus status;
        Map<String, String> headers;
        StringBuilder content = new StringBuilder();

        @Override
        public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
            if (msg instanceof HttpResponse) {
                HttpResponse response = (HttpResponse) msg;

                status = response.getStatus();

                if (!response.headers().isEmpty()) {
                    headers = new HashMap<>();
                    for (String name : response.headers().names()) {
                        for (String value : response.headers().getAll(name)) {
                            headers.put(name, value);
                        }
                    }
                }
            }
            if (msg instanceof HttpContent) {
                HttpContent chunk = (HttpContent) msg;
                content.append(chunk.content().toString(CharsetUtil.UTF_8));
            }
        }
    }
}
