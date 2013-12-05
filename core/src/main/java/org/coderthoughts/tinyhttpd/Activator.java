package org.coderthoughts.tinyhttpd;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedService;

/**
 * The Activator is invoked by the OSGi Framework to activate this bundle.
 * It registers the server with Configuration Admin and is also responsible
 * for stopping the server.
 */
public class Activator implements BundleActivator {
    private ServerController controller;

    @Override
    public void start(BundleContext context) {
        controller = new ServerController();
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put(Constants.SERVICE_PID, "org.coderthoughts.tinyhttpd");
        context.registerService(ManagedService.class, controller, properties);
        // The ServerController is started through configuration
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        controller.shutdown();
    }
}
