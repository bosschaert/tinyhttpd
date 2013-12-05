package org.coderthoughts.tinyhttpd.itests;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

@RunWith(PaxExam.class)
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
		String content = tryReadURL("http://localhost:7070");
		Assert.assertTrue(content.contains("Allaert Joachim David Bosschaert"));
	}

    private String tryReadURL(String url) throws Exception {
        int retries = 20;
        while (--retries > 0) {
            try {
                System.out.println("Trying to read from: " + url);
                return new String(Streams.suck(new URL(url).openStream()));
            } catch (Exception e) {
                // ignore
            }
            Thread.sleep(500);
        }
        throw new IOException("Unable to read from URL: " + url);
    }
}
