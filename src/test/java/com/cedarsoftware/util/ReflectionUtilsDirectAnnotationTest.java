package com.cedarsoftware.util;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for {@link ReflectionUtils#getDirectClassAnnotation(Class, Class)} and the
 * semantic contrast with {@link ReflectionUtils#getClassAnnotation(Class, Class)}.
 * <p>
 * The two methods deliberately differ:
 * <ul>
 *     <li>{@code getDirectClassAnnotation} matches JDK {@link Class#getAnnotation(Class)}
 *         exactly — respects {@code @Inherited} on the superclass chain only, never
 *         walks interfaces.</li>
 *     <li>{@code getClassAnnotation} walks superclasses AND interfaces regardless of
 *         {@code @Inherited} — framework-style lookup, like Spring's
 *         {@code AnnotationUtils.findAnnotation}.</li>
 * </ul>
 * Each test below asserts BOTH methods' behavior so the semantic boundary is visible.
 */
class ReflectionUtilsDirectAnnotationTest {

    // ---------- Inherited annotation (the JDK walks superclass chain) ----------

    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface InheritedMark { String value() default ""; }

    @InheritedMark("base")
    static class InheritedBase {}

    static class InheritedChild extends InheritedBase {}

    // ---------- Non-@Inherited annotation (JDK does NOT walk anything) ----------

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface IdentityMark { String value() default ""; }

    @IdentityMark("base")
    static class IdentityBase {}

    static class IdentityChild extends IdentityBase {}

    // ---------- Interface-declared annotations ----------

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface RoleMark { String value() default ""; }

    @RoleMark("auditable")
    interface Auditable {}

    static class AuditableImpl implements Auditable {}

    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface InheritedRoleMark { String value() default ""; }

    @InheritedRoleMark("auditable")
    interface InheritedRole {}

    static class InheritedRoleImpl implements InheritedRole {}

    // ---------- Tests: null handling ----------

    @Test
    void nullClassReturnsNull() {
        assertNull(ReflectionUtils.getDirectClassAnnotation(null, IdentityMark.class));
    }

    @Test
    void nullAnnotationClassThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> ReflectionUtils.getDirectClassAnnotation(IdentityBase.class, null));
    }

    // ---------- Tests: direct annotation ----------

    @Test
    void directAnnotationPresent() {
        IdentityMark anno = ReflectionUtils.getDirectClassAnnotation(IdentityBase.class, IdentityMark.class);
        assertNotNull(anno);
        assertEquals("base", anno.value());

        // Hierarchy-walker finds it too
        IdentityMark anno2 = ReflectionUtils.getClassAnnotation(IdentityBase.class, IdentityMark.class);
        assertNotNull(anno2);
        assertSame(anno, anno2);
    }

    // ---------- Tests: @Inherited annotation on superclass ----------

    @Test
    void inheritedAnnotationOnSuperclass_directRespectsInherited() {
        // JDK walks superclass chain for @Inherited annotations, so child sees it
        InheritedMark anno = ReflectionUtils.getDirectClassAnnotation(InheritedChild.class, InheritedMark.class);
        assertNotNull(anno);
        assertEquals("base", anno.value());
    }

    @Test
    void inheritedAnnotationOnSuperclass_walkerSeesItToo() {
        InheritedMark anno = ReflectionUtils.getClassAnnotation(InheritedChild.class, InheritedMark.class);
        assertNotNull(anno);
        assertEquals("base", anno.value());
    }

    // ---------- Tests: non-@Inherited annotation on superclass (the key divergence) ----------

    @Test
    void nonInheritedOnSuperclass_directReturnsNull() {
        // JDK's getAnnotation returns null for non-@Inherited annotations on parent classes
        IdentityMark anno = ReflectionUtils.getDirectClassAnnotation(IdentityChild.class, IdentityMark.class);
        assertNull(anno, "Direct lookup must respect the annotation author's @Inherited choice");
    }

    @Test
    void nonInheritedOnSuperclass_walkerStillFindsIt() {
        // Hierarchy walker deliberately walks superclasses regardless of @Inherited
        IdentityMark anno = ReflectionUtils.getClassAnnotation(IdentityChild.class, IdentityMark.class);
        assertNotNull(anno, "Hierarchy walker deliberately goes beyond @Inherited");
        assertEquals("base", anno.value());
    }

    // ---------- Tests: annotation on interface (JDK never walks interfaces) ----------

    @Test
    void nonInheritedOnInterface_directReturnsNull() {
        // Even when the interface's annotation is non-@Inherited, JDK doesn't walk interfaces
        RoleMark anno = ReflectionUtils.getDirectClassAnnotation(AuditableImpl.class, RoleMark.class);
        assertNull(anno, "JDK getAnnotation never looks at interfaces for class annotations");
    }

    @Test
    void nonInheritedOnInterface_walkerFindsIt() {
        RoleMark anno = ReflectionUtils.getClassAnnotation(AuditableImpl.class, RoleMark.class);
        assertNotNull(anno, "Hierarchy walker deliberately walks interfaces");
        assertEquals("auditable", anno.value());
    }

    @Test
    void inheritedOnInterface_directStillReturnsNull() {
        // @Inherited has no effect on interfaces per the JLS — JDK still returns null
        InheritedRoleMark anno = ReflectionUtils.getDirectClassAnnotation(InheritedRoleImpl.class, InheritedRoleMark.class);
        assertNull(anno, "@Inherited does not apply to interface annotations in the JDK");
    }

    @Test
    void inheritedOnInterface_walkerFindsIt() {
        InheritedRoleMark anno = ReflectionUtils.getClassAnnotation(InheritedRoleImpl.class, InheritedRoleMark.class);
        assertNotNull(anno);
        assertEquals("auditable", anno.value());
    }

    // ---------- Tests: direct lookup is not polluting the hierarchy-walker cache ----------

    @Test
    void directLookupDoesNotPolluteWalkerCache() {
        // Call the direct method many times; then verify the walker still returns
        // the correct (different) result from its own cache path.
        for (int i = 0; i < 50; i++) {
            assertNull(ReflectionUtils.getDirectClassAnnotation(IdentityChild.class, IdentityMark.class));
        }
        IdentityMark anno = ReflectionUtils.getClassAnnotation(IdentityChild.class, IdentityMark.class);
        assertNotNull(anno, "Walker must still find the annotation on the superclass — the direct "
                + "method must not have poisoned the cache with a null-sentinel entry for this key");
        assertEquals("base", anno.value());
    }

    // ---------- Smoke: identity stability across repeat calls ----------

    @Test
    void directReturnsSameAnnotationInstanceAcrossCalls() {
        // JDK caches the annotation proxy internally; same call should return the same instance
        IdentityMark first = ReflectionUtils.getDirectClassAnnotation(IdentityBase.class, IdentityMark.class);
        IdentityMark second = ReflectionUtils.getDirectClassAnnotation(IdentityBase.class, IdentityMark.class);
        assertSame(first, second);
    }
}
