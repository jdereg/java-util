package com.cedarsoftware.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * TypeHolder captures a generic Type (including parameterized types) at runtime.
 * It is typically used via anonymous subclassing to capture generic type information.
 * However, when you already have a Type (such as a raw Class or a fully parameterized type),
 * you can use the static {@code of()} method to create a TypeHolder instance.
 *
 * <p>Example usage via anonymous subclassing:</p>
 * <pre>
 *     TypeHolder&lt;List&lt;Point&gt;&gt; holder = new TypeHolder&lt;List&lt;Point&gt;&gt;() {};
 *     Type captured = holder.getType();
 * </pre>
 *
 * <p>Example usage using the {@code of()} method:</p>
 * <pre>
 *     // With a raw class:
 *     TypeHolder&lt;Point&gt; holder = TypeHolder.of(Point.class);
 *
 *     // With a parameterized type (if you already have one):
 *     Type type = new TypeReference&lt;List&lt;Point&gt;&gt;() {}.getType();
 *     TypeHolder&lt;List&lt;Point&gt;&gt; holder2 = TypeHolder.of(type);
 * </pre>
 *
 * @param <T> the type that is being captured
 */
public class TypeHolder<T> {
    private final Type type;

    @SuppressWarnings("unchecked")
    protected TypeHolder() {
        // The anonymous subclass's generic superclass is a ParameterizedType,
        // from which we can extract the actual type argument.
        Type superClass = getClass().getGenericSuperclass();
        if (superClass instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) superClass;
            // We assume the type parameter T is the first argument.
            this.type = pt.getActualTypeArguments()[0];
        } else {
            throw new IllegalArgumentException("TypeHolder must be created with a type parameter.");
        }
    }

    /**
     * Returns the captured Type, which may be a raw Class, a ParameterizedType,
     * a GenericArrayType, or another Type.
     *
     * @return the captured Type
     */
    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return type.toString();
    }

    /**
     * Creates a TypeHolder instance that wraps the given Type.
     * This factory method is useful when you already have a Type (or Class) and
     * wish to use the generic API without anonymous subclassing.
     *
     * <p>Example usage:</p>
     * <pre>
     * // For a raw class:
     * TypeHolder&lt;Point&gt; holder = TypeHolder.of(Point.class);
     *
     * // For a parameterized type:
     * Type type = new TypeReference&lt;List&lt;Point&gt;&gt;() {}.getType();
     * TypeHolder&lt;List&lt;Point&gt;&gt; holder2 = TypeHolder.of(type);
     * </pre>
     *
     * @param type the Type to wrap in a TypeHolder
     * @param <T> the type parameter
     * @return a TypeHolder instance that returns the given type via {@link #getType()}
     */
    public static <T> TypeHolder<T> of(final Type type) {
        return new TypeHolder<T>() {
            @Override
            public Type getType() {
                return type;
            }
        };
    }
}
