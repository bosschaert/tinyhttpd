package org.coderthoughts.tinyhttpd;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedService;

/**
 * The Activator is invoked by the OSGi Framework to activate the component.
 * It registers the server with Configuration Admin and is also responsible
 * for stopping the server.
 */
public class Activator implements BundleActivator {
    private ServerController controller;

    /**
     * Called by the OSGi Framework when this bundle is started.
     * @param context The OSGi Bundle Context for this bundle.
     */
    @Override
    public void start(BundleContext context) {
        controller = new ServerController();
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put(Constants.SERVICE_PID, "org.coderthoughts.tinyhttpd");
        context.registerService(ManagedService.class, controller, properties);
        // The ServerController is started through configuration
    }

    /**
     * Called by the OSGi Framework when this bundle is stopped.
     * @param context The OSGi Bundle Context for this bundle.
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        controller.shutdown();
    }
}
