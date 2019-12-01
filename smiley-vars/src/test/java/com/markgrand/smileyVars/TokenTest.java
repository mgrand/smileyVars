package com.markgrand.smileyVars;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("WeakerAccess")
public class TokenTest {
    @Test
    public void toStringTest() {
        @NotNull Token t = new Token(TokenType.TEXT, "redo", 1, 3);
        assertEquals("Token[TEXT, \"ed\"]", t.toString());
    }
}
