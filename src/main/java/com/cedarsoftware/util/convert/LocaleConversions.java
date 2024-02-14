package com.cedarsoftware.util.convert;

import java.util.Locale;

public final class LocaleConversions {
    private LocaleConversions() {}

    static String toString(Object from, Converter converter) {
        return ((Locale)from).toLanguageTag();
    }
}
