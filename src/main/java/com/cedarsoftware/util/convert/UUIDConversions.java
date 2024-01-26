package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;

public final class UUIDConversions {

    private UUIDConversions() {
    }

    static BigDecimal toBigDecimal(Object from, Converter converter, ConverterOptions options) {
        UUID uuid = (UUID) from;
        BigInteger mostSignificant = BigInteger.valueOf(uuid.getMostSignificantBits());
        BigInteger leastSignificant = BigInteger.valueOf(uuid.getLeastSignificantBits());
        // Shift the most significant bits to the left and add the least significant bits
        return new BigDecimal(mostSignificant.shiftLeft(64).add(leastSignificant));
    }

    static BigInteger toBigInteger(Object from, Converter converter, ConverterOptions options) {
        UUID uuid = (UUID) from;
        BigInteger mostSignificant = BigInteger.valueOf(uuid.getMostSignificantBits());
        BigInteger leastSignificant = BigInteger.valueOf(uuid.getLeastSignificantBits());
        // Shift the most significant bits to the left and add the least significant bits
        return mostSignificant.shiftLeft(64).add(leastSignificant);
    }
}

