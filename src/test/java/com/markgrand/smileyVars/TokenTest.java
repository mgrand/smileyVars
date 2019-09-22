package com.markgrand.smileyVars;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("WeakerAccess")
public class TokenTest {
    @Test
    public void toStringTest() {
        Token t = new Token(TokenType.TEXT, "abcd", 1, 3);
        assertEquals("Token[TEXT, \"bc\"]", t.toString());
    }
}
