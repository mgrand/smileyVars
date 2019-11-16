package com.markgrand.smileyVars.util;

/**
 * This is like BiConsumer, but with four parameters.
 */
@FunctionalInterface
public interface QuadConsumer<T, U, V, W> {
    void accept(T t, U u, V v, W w);
}
