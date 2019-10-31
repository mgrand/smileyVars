package com.markgrand.smileyVars;

import org.junit.jupiter.api.Test;

class NoFormatterExceptionTest {
    @Test()
    void constructor() {
        //noinspection ThrowableNotThrown
        new NoFormatterException("foo");
    }
}