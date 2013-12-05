package org.coderthoughts.tinyhttpd.itests;

import java.io.File;
import java.net.URL;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

//@RunWith(JUnit4TestRunner.class)
@RunWith(PaxExam.class)
public class TinyHttpdSystemTest {
    @Inject
    private BundleContext ctx;

    @Configuration
    public Option[] config() {
        return CoreOptions.options(
            // CoreOptions.equinox(),
            CoreOptions.vmOption("-Dfelix.fileinstall.dir=" + System.getProperty("user.dir")
                + File.separator + "target" + File.separator + "test-classes" + File.separator + "load"),
            // CoreOptions.vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),
            CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.configadmin").versionAsInProject(),
            CoreOptions.mavenBundle("org.apache.felix", "org.apache.felix.fileinstall").versionAsInProject(),
            CoreOptions.mavenBundle("io.netty", "netty-buffer").versionAsInProject(),
            CoreOptions.mavenBundle("io.netty", "netty-codec").versionAsInProject(),
            CoreOptions.mavenBundle("io.netty", "netty-codec-http").versionAsInProject(),
            CoreOptions.mavenBundle("io.netty", "netty-common").versionAsInProject(),
            CoreOptions.mavenBundle("io.netty", "netty-handler").versionAsInProject(),
            CoreOptions.mavenBundle("io.netty", "netty-transport").versionAsInProject(),
            CoreOptions.mavenBundle("org.coderthoughts.tinyhttpd", "core").versionAsInProject()
            );
//        ,
//            CoreOptions.junitBundles());
    }

    @Before
    public void configureFileInstall() {
//        System.setProperty("felix.fileinstall.dir", System.getProperty("user.dir") +
//                File.separator + "target" + File.separator + "test-classes");
    }

	@Test
	public void testFoo() throws Exception {
	    System.out.println("%%% " + System.getProperty("felix.fileinstall.dir"));

		for (Bundle b : ctx.getBundles()) {
            if (b.getBundleId() == 0) {
                System.out.println("### " + b + " - " + b.getVersion());
            }
		    if (b.getSymbolicName().contains("felix")) {
		        System.out.println("### " + b + " - " + b.getVersion());
		    }
            if (b.getSymbolicName().contains("netty")) {
                System.out.println("### " + b + " - " + b.getVersion());
            }
            if (b.getSymbolicName().contains("coderthoughts")) {
                System.out.println("### " + b + " - " + b.getVersion());
            }
		}

		System.out.println("$$$ " + System.getProperty("user.dir"));
		Thread.sleep(1000000);
		String content = new String(Streams.suck(new URL("http://localhost:7070").openStream()));
		System.out.println("Loaded: " + content);
	}
}
