package com.cedarsoftware.util.convert;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import com.cedarsoftware.util.ClassUtilities;

/**
 * Configuration options for the Converter class, providing customization of type conversion behavior.
 * This interface defines default settings and allows overriding of conversion parameters like timezone,
 * locale, and character encoding.
 *
 * <p>The interface provides default implementations for all methods, allowing implementations to
 * override only the settings they need to customize.</p>
 *
 * <p>Key features include:</p>
 * <ul>
 *   <li>Time zone and locale settings for date/time conversions</li>
 *   <li>Character encoding configuration</li>
 *   <li>Custom ClassLoader specification</li>
 *   <li>Boolean-to-Character conversion mapping</li>
 *   <li>Custom conversion override capabilities</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * ConverterOptions options = new ConverterOptions() {
 *     @Override
 *     public ZoneId getZoneId() {
 *         return ZoneId.of("UTC");
 *     }
 *
 *     @Override
 *     public Locale getLocale() {
 *         return Locale.US;
 *     }
 * };
 * }</pre>
 *
 * @see java.time.ZoneId
 * @see java.util.Locale
 * @see java.nio.charset.Charset
 * @see java.util.TimeZone
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         Kenny Partlow (kpartlow@gmail.com)
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
     * @return {@link ZoneId} to use for source conversion when one is not provided and is required on the target
     * type. i.e. {@link LocalDateTime}, {@link LocalDate}, or {@link String} when no zone is provided.
     */
    default ZoneId getZoneId() { return ZoneId.systemDefault(); }

    /**
     * @return Locale to use as target when converting between types that require a Locale.
     */
    default Locale getLocale() { return Locale.getDefault(); }

    /**
     * @return Charset to use as target Charset on types that require a Charset during conversion (if required).
     */
    default Charset getCharset() { return StandardCharsets.UTF_8; }

    /**
     * @return ClassLoader for loading and initializing classes.
     */
    default ClassLoader getClassLoader() { return ClassUtilities.getClassLoader(ConverterOptions.class); }

    /**
     * @return Custom option
     */
    default <T> T getCustomOption(String name) { return null; }

    /**
     * Accessor for all custom options defined on this instance.
     *
     * @return the map of custom options
     */
    default Map<String, Object> getCustomOptions() { return Collections.emptyMap(); }

    /**
     * @return TimeZone expected on the target when finished (only for types that support ZoneId or TimeZone).
     */
    default TimeZone getTimeZone() { return TimeZone.getTimeZone(this.getZoneId()); }

    /**
     * Character to return for boolean to Character conversion when the boolean is true.
     * @return the Character representing true.
     */
    default Character trueChar() { return CommonValues.CHARACTER_ONE; }

    /**
     * Character to return for boolean to Character conversion when the boolean is false.
     * @return the Character representing false.
     */
    default Character falseChar() { return CommonValues.CHARACTER_ZERO; }

    /**
     * Overrides for converter conversions.
     * @return The Map of overrides.
     */
    default Map<Converter.ConversionPair, Convert<?>> getConverterOverrides() { return Collections.emptyMap(); }

    /**
     * Maximum length allowed for enum constant names during String to Enum conversion.
     * This is a security measure to prevent DoS attacks with excessively long strings.
     * @return the maximum allowed length for enum names (default 1000)
     */
    default int getMaxEnumNameLength() { return 1000; }
}
