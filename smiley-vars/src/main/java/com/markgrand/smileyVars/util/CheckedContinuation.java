package com.markgrand.smileyVars.util;

/**
 * Functional interface for "functions" that take no arguments, return void and throw a checked exception.
 */
@FunctionalInterface
public interface CheckedContinuation<E extends Throwable> {
    void apply() throws E;
}
