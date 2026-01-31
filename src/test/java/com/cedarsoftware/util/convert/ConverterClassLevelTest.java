package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConverterClassLevelTest {

    @Test
    void equalsAndHashCodeWithSameValues() {
        Converter.ClassLevel first = new Converter.ClassLevel(String.class, 1);
        Converter.ClassLevel second = new Converter.ClassLevel(String.class, 1);
        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void equalsAndHashCodeWithDifferentValues() {
        Converter.ClassLevel base = new Converter.ClassLevel(String.class, 1);
        Converter.ClassLevel differentLevel = new Converter.ClassLevel(String.class, 2);
        Converter.ClassLevel differentClass = new Converter.ClassLevel(Integer.class, 1);

        assertThat(base).isNotEqualTo(differentLevel);
        assertThat(base).isNotEqualTo(differentClass);
        assertThat(base).isNotEqualTo("notClassLevel");

        assertThat(base.hashCode()).isNotEqualTo(differentLevel.hashCode());
        assertThat(base.hashCode()).isNotEqualTo(differentClass.hashCode());
    }
}
