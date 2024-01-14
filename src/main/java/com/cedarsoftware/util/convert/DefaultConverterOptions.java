package com.cedarsoftware.util.convert;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Kenny Partlow (kpartlow@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
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
