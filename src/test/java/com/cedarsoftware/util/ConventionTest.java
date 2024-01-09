package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

class ConventionTest {

    @Test
    void testThrowIfNull_whenNull() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Convention.throwIfNull(null, "foo"))
                .withMessageContaining("foo");
    }

    @Test
    void testThrowIfNull_whenNotNull() {
        assertThatNoException()
                .isThrownBy(() -> Convention.throwIfNull("qux", "foo"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testThrowIfNull_whenNullOrEmpty(String foo) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Convention.throwIfNullOrEmpty(foo, "foo"))
                .withMessageContaining("foo");
    }

    @Test
    void testThrowIfNull_whenNotNullOrEmpty() {
        assertThatNoException()
                .isThrownBy(() -> Convention.throwIfNullOrEmpty("qux", "foo"));
    }

    @Test
    void testThrowIfFalse_whenFalse() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Convention.throwIfFalse(false, "foo"))
                .withMessageContaining("foo");
    }

    @Test
    void testThrowIfFalse_whenTrue() {
        assertThatNoException()
                .isThrownBy(() -> Convention.throwIfFalse(true, "foo"));
    }

    @ParameterizedTest
    @NullSource
    void testThrowIfKeyExists_whenMapIsNull_throwsException(Map map) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Convention.throwIfKeyExists(map, "key", "foo"))
                .withMessageContaining("map cannot be null");
    }

    @ParameterizedTest
    @NullSource
    void testThrowIfKeyExists_whenKeyIsNull_throwsException(String key) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Convention.throwIfKeyExists(new HashMap(), key, "foo"))
                .withMessageContaining("key cannot be null");
    }

    @Test
    void testThrowIfKeyExists_whenKeyExists_throwsException() {
        Map map = new HashMap();
        map.put("qux", "bar");

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Convention.throwIfKeyExists(map, "qux", "foo"))
                .withMessageContaining("foo");
    }

    @Test
    void testThrowIfKeyExists_whenKeyDoesNotExists_doesNotThrow() {
        assertThatNoException()
                .isThrownBy(() -> Convention.throwIfKeyExists(new HashMap(), "qux", "foo"));
    }

    @Test
    void testThrowIfClassNotFound_whenClassIsNotFound_throwsException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Convention.throwIfClassNotFound("foo.bar.Class", ConventionTest.class.getClassLoader()))
                .withMessageContaining("Unknown class");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testThrowIfClassNotFound_whenClassIsNotFound_throwsException(String fqName) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Convention.throwIfClassNotFound(fqName, ConventionTest.class.getClassLoader()))
                .withMessageContaining("fully qualified ClassName cannot be null or empty");
    }

    @Test
    void testThrowIfClassNotFound_whenClassLoaderIsNull_throwsException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Convention.throwIfClassNotFound("java.lang.String", null))
                .withMessageContaining("loader cannot be null");
    }

    @Test
    void testThrowIfClassNotFound_withValidClassName_andNonNullClassLoader_doesNotThrowException() {
        assertThatNoException()
                .isThrownBy(() -> Convention.throwIfClassNotFound("java.lang.String", ConventionTest.class.getClassLoader()));
    }


}
