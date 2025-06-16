package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConvertWithTargetTest {

    @Test
    void convertDelegatesToConvertWithTarget() {
        class DummyConvert implements ConvertWithTarget<String> {
            Object fromArg;
            Converter converterArg;
            Class<?> targetArg;
            @Override
            public String convertWithTarget(Object from, Converter converter, Class<?> target) {
                this.fromArg = from;
                this.converterArg = converter;
                this.targetArg = target;
                return "done";
            }
        }

        Converter converter = new Converter(new DefaultConverterOptions());
        DummyConvert dummy = new DummyConvert();

        String result = dummy.convert("source", converter);

        assertThat(result).isEqualTo("done");
        assertThat(dummy.fromArg).isEqualTo("source");
        assertThat(dummy.converterArg).isSameAs(converter);
        assertThat(dummy.targetArg).isNull();
    }
}
