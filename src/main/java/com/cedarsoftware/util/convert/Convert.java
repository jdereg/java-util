package com.cedarsoftware.util.convert;


@FunctionalInterface
public interface Convert<T> {
    T convert(Object from, Converter converter, ConverterOptions options);
}
