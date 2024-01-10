package com.cedarsoftware.util.convert;

import java.nio.charset.Charset;
import java.time.ZoneId;
import java.util.Locale;
import java.util.TimeZone;

public interface ConverterOptions {

    /**
     * @return zoneId to use for source conversion when on is not provided on the source (Date, Instant, etc.)
     */
    ZoneId getSourceZoneId();

    /**
     * @return zoneId expected on the target when finished (only for types that support ZoneId or TimeZone)
     */
    ZoneId getTargetZoneId();

    /**
     * @return Locale to use as source locale when converting between types that require a Locale
     */
    Locale getSourceLocale();

    /**
     * @return Locale to use as target when converting between types that require a Locale.
     */
    Locale getTargetLocale();

    /**
     * @return Charset to use as source CharSet on types that require a Charset during conversion (if required).
     */
    Charset getSourceCharset();

    /**
     * @return Charset to use os target Charset on types that require a Charset during conversion (if required).
     */
    Charset getTargetCharset();


    /**
     * @return Classloader for loading and initializing classes.
     */
    default ClassLoader getClassLoader() { return ConverterOptions.class.getClassLoader(); }

    /**
     * @return custom option
     */
    <T> T getCustomOption(String name);

    /**
     * @return zoneId to use for source conversion when on is not provided on the source (Date, Instant, etc.)
     */
    default TimeZone getSourceTimeZone() { return TimeZone.getTimeZone(this.getSourceZoneId()); }

    /**
     * @return zoneId expected on the target when finished (only for types that support ZoneId or TimeZone)
     */
    default TimeZone getTargetTimeZone() { return TimeZone.getTimeZone(this.getTargetZoneId()); }
}
