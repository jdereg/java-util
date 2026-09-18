package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.Converter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The String -&gt; Boolean truth set, and the whitespace policy around it.
 * <p>
 * Every case here is pinned deliberately, because an unrecognised String converts to {@code false}
 * rather than throwing: a word this set fails to recognise is not rejected, it is INVERTED, and
 * silently. That is what made the historical gaps ({@code "yes"}, then {@code "on"}, then a padded
 * {@code " true "}) costly rather than merely inconvenient.
 */
class StringBooleanTruthSetTest {
    @ParameterizedTest
    @ValueSource(strings = {"true", "TRUE", "True", "t", "T", "1",
            "y", "Y", "yes", "YES", "Yes",
            "on", "ON", "On"})
    void theseAllMeanTrue(String s) {
        assertThat(Converter.convert(s, Boolean.class)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"false", "FALSE", "False", "f", "F", "0",
            "n", "N", "no", "NO", "No",
            "off", "OFF", "Off"})
    void theseAllMeanFalse(String s) {
        assertThat(Converter.convert(s, Boolean.class)).isFalse();
    }

    // ---- behaviour CHANGED in 4.111.0: these returned false before ----

    @ParameterizedTest
    @ValueSource(strings = {"on", "On", "ON"})
    void onMeansTrueAsOfThisRelease(String s) {
        assertThat(Converter.convert(s, Boolean.class)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"  true  ", "\ttrue\n", " TRUE ", "  yes  ", "  y  ", "  on  ", "  1  ", "  t  "})
    void surroundingWhitespaceDoesNotInvertATruthWord(String s) {
        assertThat(Converter.convert(s, Boolean.class)).isTrue();
    }

    // ---- behaviour deliberately UNCHANGED ----

    @ParameterizedTest
    @ValueSource(strings = {"  false  ", "  no  ", "  off  ", "  n  ", "  0  "})
    void paddedFalsyWordsAreStillFalse(String s) {
        assertThat(Converter.convert(s, Boolean.class)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t\n", "banana", "2", "-1", "enabled", "disabled"})
    void anythingUnrecognisedIsFalse(String s) {
        assertThat(Converter.convert(s, Boolean.class)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"tr ue", "y es", "o n", "t rue"})
    void whitespaceInsideTheWordDoesNotMakeItATruthWord(String s) {
        // Trimming the ends must not be read as tolerating whitespace anywhere.
        assertThat(Converter.convert(s, Boolean.class)).isFalse();
    }

    @Test
    void nullConvertsToNullForTheBoxedTypeAndFalseForThePrimitive() {
        assertThat(Converter.convert(null, Boolean.class)).isNull();
        assertThat(Converter.convert(null, boolean.class)).isFalse();
    }
}
