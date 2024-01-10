package com.cedarsoftware.util.convert;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultConverterOptions implements ConverterOptions {

    private final Map<String, Object> customOptions;
    private final ZoneId zoneId;
    private final Locale locale;
    private final Charset charset;

    public DefaultConverterOptions() {
        this.customOptions = new ConcurrentHashMap<>();
        this.zoneId = ZoneId.systemDefault();
        this.locale = Locale.getDefault();
        this.charset = StandardCharsets.UTF_8;
    }

    @Override
    public ZoneId getSourceZoneId() {
        return zoneId;
    }

    @Override
    public ZoneId getTargetZoneId() {
        return zoneId;
    }

    @Override
    public Locale getSourceLocale() {
        return locale;
    }

    @Override
    public Locale getTargetLocale() {
        return locale;
    }

    @Override
    public Charset getSourceCharset() {
        return charset;
    }

    @Override
    public Charset getTargetCharset() {
        return charset;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCustomOption(String name) {
        return (T) this.customOptions.get(name);
    }
}
