package com.cedarsoftware.util.convert;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.cedarsoftware.util.convert.Converter.ConversionPair;

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

    private final Map<ConversionPair, Convert<?>> converterOverrides;

    public DefaultConverterOptions() {
        this.customOptions = new ConcurrentHashMap<>();
        this.converterOverrides = new ConcurrentHashMap<>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCustomOption(String name) {
        return (T) this.customOptions.get(name);
    }

    @Override
    public Map<String, Object> getCustomOptions() { return this.customOptions; }

    @Override
    public Map<ConversionPair, Convert<?>> getConverterOverrides() { return this.converterOverrides; }
}
