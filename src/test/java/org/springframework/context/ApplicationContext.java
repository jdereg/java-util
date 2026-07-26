package org.springframework.context;

/**
 * Stand-in for Spring's {@code ApplicationContext} so
 * {@link com.cedarsoftware.util.ClassUtilitiesIndirectLoaderSecurityTest} can prove the
 * family-level block without java-util taking a dependency on Spring.
 * <p>
 * The block is keyed on this fully-qualified name and matched against every supertype, so a class
 * declared here reproduces the real classpath condition exactly: java-util never loads Spring, it
 * only recognizes the name.
 */
public interface ApplicationContext {
}
