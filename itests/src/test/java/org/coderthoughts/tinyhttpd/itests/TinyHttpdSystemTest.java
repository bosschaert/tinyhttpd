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
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

/**
 * This is a system test which is normally run from Maven through Pax Exam. The system test will run in an OSGi framework with
 * the web server bundle and all its dependencies deployed. The test exercises the functionality of the web server
 * programmatically.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TinyHttpdSystemTest {
    @Configuration
    public Option[] config() {
        return CoreOptions.options(
            // CoreOptions.vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"), // uncomment for debugging
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
        } finally {
            // Empty the stream, otherwise we get an ugly exception on the server...
            Streams.suck(connection.getInputStream());
        }
	}

	@Test
	public void testUpload() throws Exception {
        // Make sure the server is up and running
        tryReadURL(new URL("http://localhost:7070/uploaddir/test.txt"));

	    // This is where the upload file will end up...
        File uploadedFileRes = new File(System.getProperty("felix.fileinstall.dir") + "/../web-root/uploaddir/index.html");
        if (uploadedFileRes.exists()) {
            // Delete it if it was there from an earlier test run.
            Assert.assertTrue("Precondition", uploadedFileRes.delete());
        }

        URI postURI = new URI("/uploaddir/");
        File fileRes = new File(System.getProperty("felix.fileinstall.dir") + "/../web-root/index.html");

        // Upload the file
        HttpUploadTestResponseHandler responseHandler = uploadFile(postURI, fileRes);
        Assert.assertEquals(200, responseHandler.status.code());
        Assert.assertEquals("text/html; charset=UTF-8", responseHandler.headers.get("Content-Type"));
        String content = responseHandler.content.toString();
        int idx1 = content.indexOf("test.txt");
        Assert.assertTrue("Returned directory listing should contain 'test.txt'", idx1 > 0);
        int idx2 = content.indexOf("index.html");
        Assert.assertTrue("Returned directory listing should contain 'index.html'", idx2 > 0);
        Assert.assertTrue("index.html should be ordered before test.txt", idx2 < idx1);

        // Check that the uploaded file, as written out is identical to its original
        byte[] bytes1 = Streams.suck(new FileInputStream(fileRes)); // suck closes the input stream when done
        byte[] bytes2 = Streams.suck(new FileInputStream(uploadedFileRes));
        Assert.assertTrue("Uploaded file content not identical to original", Arrays.equals(bytes1, bytes2));

        // upload another index.html, where one already exists - should be rejected with a 406
        File fileResAlt = new File(System.getProperty("felix.fileinstall.dir") + "/../alt-root/index.html");
        HttpUploadTestResponseHandler responseHandler2 = uploadFile(postURI, fileResAlt);
        Assert.assertEquals("Uploading a file that already exists should produce a HTTP 406 status code",
                HttpResponseStatus.NOT_ACCEPTABLE.code(), responseHandler2.status.code());
        byte[] bytes3 = Streams.suck(new FileInputStream(uploadedFileRes));
        Assert.assertTrue("Uploaded file content should not have been changed", Arrays.equals(bytes1, bytes3));
	}

	@Test
	public void testDynamicReconfigure() throws Exception {
        // Make sure the server is up and running
        String initialContent = tryReadURL(new URL("http://localhost:7070/index.html"));
        Assert.assertFalse(initialContent.contains("Foo"));
        Assert.assertTrue(initialContent.contains("information"));

        File cmFile = new File(System.getProperty("felix.fileinstall.dir") + "/org.coderthoughts.tinyhttpd.cfg");
        Properties config = new Properties();

        // Use the Java7 try-with-resources auto close on the input stream.
        try (FileInputStream fis = new FileInputStream(cmFile)) {
            // Load the existing configuration
            config.load(fis);
        }

        // Reconfigure the web server with a new port number and a new web root.
        Properties newConfig = new Properties();
        newConfig.setProperty("port", "7654");
        newConfig.setProperty("root", config.getProperty("root") + "/../alt-root");

        try (FileOutputStream fos = new FileOutputStream(cmFile)) {
            newConfig.store(fos, "Testing dynamic reconfiguration");
        }

        try {
            // Check that the content from the newly configured webserver can be read
            String newContent = tryReadURL(new URL("http://localhost:7654/index.html"));
            Assert.assertTrue(newContent.contains("Foo"));
            Assert.assertFalse(newContent.contains("information"));
        } finally {
            // Put the original configuration back
            try (FileOutputStream fos2 = new FileOutputStream(cmFile)) {
                config.store(fos2, "Restoring original configuration");
            }

            // Use 127.0.0.1 to avoid getting the result via a cache
            String resetContent = tryReadURL(new URL("http://127.0.0.1:7070/index.html"));
            Assert.assertFalse("Should serve the original content again", resetContent.contains("Foo"));
            Assert.assertTrue("Should serve the original content again", resetContent.contains("information"));
        }
	}

	@Test
    public void testDirectory() throws Exception {
        URL url = new URL("http://localhost:7070/");
        String rootContent = tryReadURL(url);
        Assert.assertFalse("This location contains an index.html so no directory listing is provided",
                rootContent.contains("Directory"));

        URL dirURL = new URL("http://localhost:7070/images/");
        String imagesContent = tryReadURL(dirURL);
        Assert.assertTrue("Should provide a directory listing.", imagesContent.contains("Directory"));
    }

	/**
	 * The web server is configured and started asynchronously, therefore we may have to retry a read from a given
	 * location a few times, until the server has been configured and started.
	 * @param url The url to read from. If reading causes an error, this method waits a little and then tries again
	 * to a maximum of 20 retries.
	 */
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

    /**
     * This method uses the Netty HTTP client to upload a file, like a user would do with the HTML form in the
     * directory listing.
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

    /**
     * This class is used by the Netty File uploader in the {@link #uploadFile} method.
     */
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

    /**
     * This class is used by the Netty File uploader in the {@link #uploadFile} method and can be used
     * by the test to inspect the returned HTTP message.
     */
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
