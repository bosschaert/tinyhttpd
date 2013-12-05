package org.coderthoughts.tinyhttpd.itests;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
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
        URL url = new URL("http://localhost:7070/david.png");
        tryReadURL(url);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		Map<String, List<String>> headers = connection.getHeaderFields();
		Assert.assertEquals(200, connection.getResponseCode());
		Assert.assertEquals("image/png", connection.getHeaderField("Content-Type"));
		Assert.assertEquals("keep-alive", connection.getHeaderField("Connection"));
		Assert.assertEquals("private, max-age=60", connection.getHeaderField("Cache-Control"));

		File fileRes = new File(System.getProperty("felix.fileinstall.dir") + "/../web-root/david.png");
		Assert.assertEquals(fileRes.length(), connection.getHeaderFieldInt("Content-Length", -1));
		Assert.assertEquals(fileRes.lastModified(), connection.getHeaderFieldDate("Last-Modified", -1));
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
}
