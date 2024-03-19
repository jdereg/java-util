package com.cedarsoftware.util.convert;

import java.util.Locale;
import java.util.Map;

import com.cedarsoftware.util.CompactLinkedMap;
import com.cedarsoftware.util.StringUtilities;

import static com.cedarsoftware.util.convert.MapConversions.COUNTRY;
import static com.cedarsoftware.util.convert.MapConversions.LANGUAGE;
import static com.cedarsoftware.util.convert.MapConversions.SCRIPT;
import static com.cedarsoftware.util.convert.MapConversions.VARIANT;

public final class LocaleConversions {
    private LocaleConversions() {}

    static String toString(Object from, Converter converter) {
        return ((Locale)from).toLanguageTag();
    }

    static Map<?, ?> toMap(Object from, Converter converter) {
        Locale locale = (Locale) from;
        Map<String, Object> map = new CompactLinkedMap<>();

        String language = locale.getLanguage();
        map.put(LANGUAGE, language);

        String country = locale.getCountry();
        if (StringUtilities.hasContent(country)) {
            map.put(COUNTRY, country);
        }

        String script = locale.getScript();
        if (StringUtilities.hasContent(script)) {
            map.put(SCRIPT, script);
        }

        String variant = locale.getVariant();
        if (StringUtilities.hasContent(variant)) {
            map.put(VARIANT, variant);
        }
        return map;
    }
}
