package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Conversions to and from {@link XMLGregorianCalendar} and {@link javax.xml.datatype.Duration}, the types JAXB
 * generates for {@code xs:dateTime}, {@code xs:date}, {@code xs:time}, the {@code xs:g...} types and
 * {@code xs:duration}. A value's text is its XML Schema lexical form, which holds the fields the value has and no
 * others: {@code "2026-10-15T10:20:30.123-04:00"}, {@code "2020-06-28"} (a date, no time, no zone),
 * {@code "P1DT2H"}. Its Map form holds that text, {@code {"xmlGregorianCalendar": text}} or
 * {@code {"xmlDuration": text}}; {@code "value"} and {@code "_v"} are read as well.
 * <p>
 * A Map may instead hold the fields one by one -- {@code eon}, {@code year}, {@code month}, {@code day},
 * {@code timezone}, {@code hour}, {@code minute}, {@code second}, {@code fractionalSecond}; and {@code signum},
 * {@code years}, {@code months}, {@code days}, {@code hours}, {@code minutes}, {@code seconds} -- which is how
 * json-io wrote these values through 4.112.0, by reflecting on the JDK's implementation classes. That form reads,
 * so JSON stored then still does.
 * <p>
 * Values are made by {@link DatatypeFactory}: the result is whatever implementation the runtime has (the JDK's
 * lives in {@code com.sun.org.apache.xerces.internal.jaxp.datatype}), and nothing here names one. java-util
 * requires the java.xml module statically, so a modular application may run without it; these conversions are
 * registered only when it is present.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
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
final class XmlDatatypeConversions {
    static final String XML_GREGORIAN_CALENDAR = "xmlGregorianCalendar";
    static final String XML_DURATION = "xmlDuration";
    private static final String[] XML_GREGORIAN_CALENDAR_KEYS =
            {XML_GREGORIAN_CALENDAR, MapConversions.VALUE, MapConversions.V};
    private static final String[] XML_DURATION_KEYS = {XML_DURATION, MapConversions.VALUE, MapConversions.V};
    private static final String[] CALENDAR_FIELDS = {"year", "month", "day", "hour", "minute", "second", "timezone"};
    private static final String[] DURATION_FIELDS = {"signum", "years", "months", "days", "hours", "minutes", "seconds"};

    // The JDK's DatatypeFactory keeps no state between calls; one serves every thread
    private static volatile DatatypeFactory factory;

    private XmlDatatypeConversions() {
    }

    /** Adds the conversions to {@code db}. Called only when the java.xml module is present. */
    static void register(Converter.TriConsumer<Class<?>, Class<?>, Convert<?>> db) {
        db.accept(Void.class, XMLGregorianCalendar.class, VoidConversions::toNull);
        db.accept(XMLGregorianCalendar.class, XMLGregorianCalendar.class, XmlDatatypeConversions::copy);
        db.accept(String.class, XMLGregorianCalendar.class, XmlDatatypeConversions::toXMLGregorianCalendar);
        db.accept(Map.class, XMLGregorianCalendar.class, XmlDatatypeConversions::mapToXMLGregorianCalendar);
        db.accept(XMLGregorianCalendar.class, String.class, XmlDatatypeConversions::calendarToString);
        db.accept(XMLGregorianCalendar.class, Map.class, XmlDatatypeConversions::calendarToMap);

        db.accept(Void.class, Duration.class, VoidConversions::toNull);
        db.accept(Duration.class, Duration.class, (from, converter) -> from);      // immutable
        db.accept(String.class, Duration.class, XmlDatatypeConversions::toDuration);
        db.accept(Map.class, Duration.class, XmlDatatypeConversions::mapToDuration);
        db.accept(Duration.class, String.class, XmlDatatypeConversions::durationToString);
        db.accept(Duration.class, Map.class, XmlDatatypeConversions::durationToMap);
    }

    private static DatatypeFactory factory() {
        DatatypeFactory made = factory;
        if (made == null) {
            try {
                made = DatatypeFactory.newInstance();
            } catch (DatatypeConfigurationException e) {
                throw new IllegalStateException("No javax.xml.datatype.DatatypeFactory in this runtime", e);
            }
            factory = made;
        }
        return made;
    }

    /** An XMLGregorianCalendar can be changed by its setters, so a conversion to one gives a copy. */
    static XMLGregorianCalendar copy(Object from, Converter converter) {
        return (XMLGregorianCalendar) ((XMLGregorianCalendar) from).clone();
    }

    static XMLGregorianCalendar toXMLGregorianCalendar(Object from, Converter converter) {
        String text = ((String) from).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return factory().newXMLGregorianCalendar(text);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Not an XML Schema date or time: '" + text + "'", e);
        }
    }

    static String calendarToString(Object from, Converter converter) {
        return ((XMLGregorianCalendar) from).toXMLFormat();
    }

    static Map<String, Object> calendarToMap(Object from, Converter converter) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(XML_GREGORIAN_CALENDAR, calendarToString(from, converter));
        return map;
    }

    static XMLGregorianCalendar mapToXMLGregorianCalendar(Object from, Converter converter) {
        Map<?, ?> map = (Map<?, ?>) from;
        if (!hasText(map, XML_GREGORIAN_CALENDAR_KEYS) && hasAny(map, CALENDAR_FIELDS)) {
            return calendarFromFields(map, converter);
        }
        return MapConversions.dispatch(from, converter, XMLGregorianCalendar.class, XML_GREGORIAN_CALENDAR_KEYS);
    }

    /** The fields as XMLGregorianCalendarImpl holds them: the year is eon + year; a field absent or null is unset. */
    private static XMLGregorianCalendar calendarFromFields(Map<?, ?> map, Converter converter) {
        BigInteger year = null;
        int lowYear = intField(map, "year", converter);
        if (lowYear != DatatypeConstants.FIELD_UNDEFINED) {
            year = BigInteger.valueOf(lowYear);
            Object eon = map.get("eon");
            if (eon != null) {
                year = year.add(converter.convert(eon, BigInteger.class));
            }
        }
        Object fraction = map.get("fractionalSecond");
        return factory().newXMLGregorianCalendar(year, intField(map, "month", converter),
                intField(map, "day", converter), intField(map, "hour", converter), intField(map, "minute", converter),
                intField(map, "second", converter),
                fraction == null ? null : converter.convert(fraction, BigDecimal.class),
                intField(map, "timezone", converter));
    }

    static Duration toDuration(Object from, Converter converter) {
        String text = ((String) from).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return factory().newDuration(text);
        } catch (IllegalArgumentException | UnsupportedOperationException e) {
            throw new IllegalArgumentException("Not an XML Schema duration: '" + text + "'", e);
        }
    }

    static String durationToString(Object from, Converter converter) {
        return from.toString();         // Duration.toString() is its lexical form
    }

    static Map<String, Object> durationToMap(Object from, Converter converter) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(XML_DURATION, durationToString(from, converter));
        return map;
    }

    static Duration mapToDuration(Object from, Converter converter) {
        Map<?, ?> map = (Map<?, ?>) from;
        if (!hasText(map, XML_DURATION_KEYS) && hasAny(map, DURATION_FIELDS)) {
            return durationFromFields(map, converter);
        }
        return MapConversions.dispatch(from, converter, Duration.class, XML_DURATION_KEYS);
    }

    /** The fields as DurationImpl holds them: a sign, and each part unset (null) or not negative. */
    private static Duration durationFromFields(Map<?, ?> map, Converter converter) {
        Object signum = map.get("signum");
        boolean positive = signum == null || converter.convert(signum, int.class) >= 0;
        return factory().newDuration(positive, bigIntegerField(map, "years", converter),
                bigIntegerField(map, "months", converter), bigIntegerField(map, "days", converter),
                bigIntegerField(map, "hours", converter), bigIntegerField(map, "minutes", converter),
                map.get("seconds") == null ? null : converter.convert(map.get("seconds"), BigDecimal.class));
    }

    private static int intField(Map<?, ?> map, String key, Converter converter) {
        Object value = map.get(key);
        return value == null ? DatatypeConstants.FIELD_UNDEFINED : converter.convert(value, int.class);
    }

    private static BigInteger bigIntegerField(Map<?, ?> map, String key, Converter converter) {
        Object value = map.get(key);
        return value == null ? null : converter.convert(value, BigInteger.class);
    }

    private static boolean hasAny(Map<?, ?> map, String[] keys) {
        for (String key : keys) {
            if (map.containsKey(key)) {
                return true;
            }
        }
        return false;
    }

    /** Whether one of {@code keys} holds a value: text with something in it, or anything else not null. */
    private static boolean hasText(Map<?, ?> map, String[] keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null && (!(value instanceof String) || !((String) value).trim().isEmpty())) {
                return true;
            }
        }
        return false;
    }
}
