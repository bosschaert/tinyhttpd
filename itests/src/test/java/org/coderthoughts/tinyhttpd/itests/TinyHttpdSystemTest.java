package org.coderthoughts.tinyhttpd.itests;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

@RunWith(JUnit4TestRunner.class)
public class TinyHttpdSystemTest {
    @Inject
    private BundleContext ctx;

    @Configuration
    public Option[] config() {
        return CoreOptions.options(
            CoreOptions.equinox(),
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
	public void testFoo() {
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
	}
}
