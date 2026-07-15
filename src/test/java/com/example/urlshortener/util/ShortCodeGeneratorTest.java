package com.example.urlshortener.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShortCodeGeneratorTest {

    private final ShortCodeGenerator generator = new ShortCodeGenerator(7);

    @Test
    void shouldGenerateCodeOfConfiguredLength() {
        String code = generator.generate();
        assertNotNull(code);
        assertEquals(7, code.length());
    }

    @Test
    void shouldOnlyContainAlphanumericCharacters() {
        for (int i = 0; i < 100; i++) {
            String code = generator.generate();
            assertTrue(code.matches("[a-zA-Z0-9]+"), "Invalid characters in code: " + code);
        }
    }

    @Test
    void shouldRejectLengthBelowMinimum() {
        assertThrows(IllegalArgumentException.class, () -> new ShortCodeGenerator(3));
    }

    @Test
    void shouldRejectLengthAboveMaximum() {
        assertThrows(IllegalArgumentException.class, () -> new ShortCodeGenerator(17));
    }

    @Test
    void shouldAcceptBoundaryLengths() {
        assertDoesNotThrow(() -> new ShortCodeGenerator(4));
        assertDoesNotThrow(() -> new ShortCodeGenerator(16));
    }

    @Test
    void shouldProduceHighEntropy() {
        java.util.Set<String> codes = new java.util.HashSet<>();
        for (int i = 0; i < 1000; i++) {
            codes.add(generator.generate());
        }
        assertTrue(codes.size() > 990, "Codes should be virtually unique across 1000 iterations");
    }
}
