package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.cedarsoftware.util.Converter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Surrounding whitespace is not part of a number. Before this, the same padded value converted
 * cleanly to Double (whose JDK parser ignores surrounding whitespace by contract) and threw for
 * every other numeric type (whose parsers do not).
 */
class StringNumericTrimTest {
    @Test
    void paddedNumbersConvertForEveryNumericType() {
        assertThat(Converter.convert("  42  ", Byte.class)).isEqualTo((byte) 42);
        assertThat(Converter.convert("  42  ", Short.class)).isEqualTo((short) 42);
        assertThat(Converter.convert("  42  ", Integer.class)).isEqualTo(42);
        assertThat(Converter.convert("  42  ", Long.class)).isEqualTo(42L);
        assertThat(Converter.convert("  42  ", Float.class)).isEqualTo(42f);
        assertThat(Converter.convert("  42  ", Double.class)).isEqualTo(42d);
        assertThat(Converter.convert("  42  ", BigInteger.class)).isEqualTo(BigInteger.valueOf(42));
        assertThat(Converter.convert("  3.75  ", BigDecimal.class)).isEqualTo(new BigDecimal("3.75"));
    }

    @Test
    void tabsAndNewlinesCountAsWhitespace() {
        assertThat(Converter.convert("\t42\n", Integer.class)).isEqualTo(42);
        assertThat(Converter.convert(" \n -7 \t ", Long.class)).isEqualTo(-7L);
    }

    @Test
    void unpaddedValuesAreUnchanged() {
        assertThat(Converter.convert("42", Integer.class)).isEqualTo(42);
        assertThat(Converter.convert("-42", Integer.class)).isEqualTo(-42);
        assertThat(Converter.convert("3.75", BigDecimal.class)).isEqualTo(new BigDecimal("3.75"));
    }

    @Test
    void blankStillMeansZero() {
        // Unchanged: the isEmpty() guard is blank-aware, so trimming cannot alter these.
        assertThat(Converter.convert("", Integer.class)).isEqualTo(0);
        assertThat(Converter.convert("   ", Integer.class)).isEqualTo(0);
        assertThat(Converter.convert("\t\n", Long.class)).isEqualTo(0L);
        assertThat(Converter.convert("   ", BigDecimal.class)).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void whitespaceInsideTheNumberStillFails() {
        // Trimming the ends must not be read as tolerating whitespace anywhere.
        assertThatThrownBy(() -> Converter.convert("-  5", Integer.class))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Converter.convert("4 2", Integer.class))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Converter.convert("1 000", Long.class))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nonNumbersStillFail() {
        assertThatThrownBy(() -> Converter.convert("  x  ", Integer.class))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Converter.convert("  true  ", Integer.class))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aSingleSpaceIsStillACharacter() {
        // Character is deliberately NOT trimmed: a space is a legitimate char value, and trimming
        // would silently turn it into NUL.
        assertThat(Converter.convert(" ", Character.class)).isEqualTo(' ');
    }
}
