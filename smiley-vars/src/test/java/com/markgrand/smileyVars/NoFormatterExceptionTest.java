package com.markgrand.smileyVars;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class NoFormatterExceptionTest {
    @Test()
    void constructor() {
        Assertions.assertThrows(NoFormatterException.class, () -> {
            throw new NoFormatterException("foo");
        });
    }
}