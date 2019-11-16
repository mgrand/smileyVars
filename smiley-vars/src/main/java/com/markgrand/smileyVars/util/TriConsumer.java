package com.markgrand.smileyVars.util;

/**
 * This is like BiConsumer, but with three parameters.
 */
@FunctionalInterface
public interface TriConsumer<T, U, V> {
    void accept(T t, U u, V v);
}
