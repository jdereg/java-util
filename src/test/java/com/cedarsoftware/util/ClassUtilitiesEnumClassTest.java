package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * getClassIfEnum() answers which enum an instance of a class is a constant of. A class merely declared inside an enum
 * is not one; getClassIfEnum() returned the enclosing enum for it, and json-io, which asks it before writing, wrote an
 * instance of such a class as an enum and failed with a ClassCastException.
 */
class ClassUtilitiesEnumClassTest {

    enum Op {
        PLUS {
            @Override
            int apply(int a, int b) {
                return a + b;
            }
        },
        TIMES {
            @Override
            int apply(int a, int b) {
                return a * b;
            }
        };

        abstract int apply(int a, int b);

        static class Helper {
        }

        enum Mode { FAST, SAFE }
    }

    @Test
    void anEnumAndItsConstantBodiesAreThatEnum() {
        assertSame(Op.class, ClassUtilities.getClassIfEnum(Op.class));
        assertNotSame(Op.class, Op.PLUS.getClass(), "a constant with a body is an anonymous subclass");
        assertSame(Op.class, ClassUtilities.getClassIfEnum(Op.PLUS.getClass()));
        assertSame(Op.class, ClassUtilities.getClassIfEnum(Op.TIMES.getClass()));
        assertSame(Op.Mode.class, ClassUtilities.getClassIfEnum(Op.Mode.class));
    }

    @Test
    void aClassDeclaredInsideAnEnumIsNotAnEnum() {
        assertNull(ClassUtilities.getClassIfEnum(Op.Helper.class));
        assertNull(ClassUtilities.getClassIfEnum(Enum.class));
        assertNull(ClassUtilities.getClassIfEnum(Object.class));
        assertNull(ClassUtilities.getClassIfEnum(String.class));
        assertNull(ClassUtilities.getClassIfEnum(null));
    }
}
