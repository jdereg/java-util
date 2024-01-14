package com.cedarsoftware.util.convert;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.Locale;
import java.util.TimeZone;

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
public interface ConverterOptions {

    /**
     * @return zoneId to use for source conversion when on is not provided on the source (Date, Instant, etc.)
     */
    default ZoneId getSourceZoneId() { return ZoneId.systemDefault(); }

    /**
     * @return zoneId expected on the target when finished (only for types that support ZoneId or TimeZone)
     */
    default ZoneId getTargetZoneId() { return ZoneId.systemDefault(); }

    /**
     * @return Locale to use as source locale when converting between types that require a Locale
     */
    default Locale getSourceLocale() { return Locale.getDefault(); }

    /**
     * @return Locale to use as target when converting between types that require a Locale.
     */
    default Locale getTargetLocale() { return Locale.getDefault(); }

    /**
     * @return Charset to use as source CharSet on types that require a Charset during conversion (if required).
     */
    default Charset getSourceCharset() { return StandardCharsets.UTF_8; }

    /**
     * @return Charset to use os target Charset on types that require a Charset during conversion (if required).
     */
    default Charset getTargetCharset() { return StandardCharsets.UTF_8; }


    /**
     * @return Classloader for loading and initializing classes.
     */
    default ClassLoader getClassLoader() { return ConverterOptions.class.getClassLoader(); }

    /**
     * @return custom option
     */
    <T> T getCustomOption(String name);

    /**
     * @return TimeZone to use for source conversion when on is not provided on the source (Date, Instant, etc.)
     */
    default TimeZone getSourceTimeZone() { return TimeZone.getTimeZone(this.getSourceZoneId()); }

    /**
     * @return TimeZone expected on the target when finished (only for types that support ZoneId or TimeZone)
     */
    default TimeZone getTargetTimeZone() { return TimeZone.getTimeZone(this.getTargetZoneId()); }
}
