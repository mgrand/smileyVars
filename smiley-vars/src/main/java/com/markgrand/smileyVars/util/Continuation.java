package com.markgrand.smileyVars.util;

/**
 * Functional interface for "functions" that take no arguments and return void.
 */
@FunctionalInterface
public interface Continuation {
    void apply();
}
