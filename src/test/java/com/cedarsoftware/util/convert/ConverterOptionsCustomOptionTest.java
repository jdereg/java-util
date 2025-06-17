package com.cedarsoftware.util.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class ConverterOptionsCustomOptionTest {

    @Test
    void defaultGetCustomOptionReturnsNull() {
        ConverterOptions options = new ConverterOptions() { };
        Object value = options.getCustomOption("missing");
        assertThat(value).isNull();
    }

    @Test
    void defaultImplementationReturnsEmptyMap() {
        ConverterOptions options = new ConverterOptions() { };
        Map<String, Object> map = options.getCustomOptions();
        assertThat(map).isEmpty();
    }

    @Test
    void mapIsLiveForDefaultConverterOptions() {
        DefaultConverterOptions options = new DefaultConverterOptions();
        options.getCustomOptions().put("answer", 42);
        assertThat((Object) options.getCustomOption("answer")).isEqualTo(42);
        assertThat(options.getCustomOptions()).containsEntry("answer", 42);
    }
}
