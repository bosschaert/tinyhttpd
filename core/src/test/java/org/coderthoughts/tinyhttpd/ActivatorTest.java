package org.coderthoughts.tinyhttpd;

import java.lang.reflect.Field;
import java.util.Hashtable;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedService;

public class ActivatorTest {
    @Test
    public void testActivatorStart() {
        BundleContext bc = Mockito.mock(BundleContext.class);

        Activator a = new Activator();
        a.start(bc);

        // Verify that the activator registers the right Managed Service.
        Matcher<ManagedService> intfMatcher = new BaseMatcher<ManagedService>() {
            @Override
            public boolean matches(Object item) {
                return item instanceof ManagedService && item instanceof ServerController;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Not implementing the right classes");
            }
        };

        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put(Constants.SERVICE_PID, "org.coderthoughts.tinyhttpd");
        Mockito.verify(bc).registerService(Mockito.eq(ManagedService.class), Mockito.argThat(intfMatcher), Mockito.eq(properties));
    }

    @Test
    public void testActivatorStop() throws Exception {
        Activator a = new Activator();

        ServerController mockController = Mockito.mock(ServerController.class);

        // Use reflection to set the mock controller in the activator, so we can test it
        Field controllerField = a.getClass().getDeclaredField("controller");
        controllerField.setAccessible(true);
        controllerField.set(a, mockController);

        a.stop(null);
        Mockito.verify(mockController, Mockito.times(1)).shutdown();
    }

}
