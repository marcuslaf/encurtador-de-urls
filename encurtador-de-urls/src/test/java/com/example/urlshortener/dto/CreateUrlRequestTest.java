package com.example.urlshortener.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CreateUrlRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void init() { factory = Validation.buildDefaultValidatorFactory(); validator = factory.getValidator(); }
    @AfterAll
    static void close() { if (factory != null) factory.close(); }

    @Test
    void shouldAcceptValidHttpUrl() {
        Set<ConstraintViolation<CreateUrlRequest>> violations = validator.validate(new CreateUrlRequest("http://example.com", 60));
        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldAcceptValidHttpsUrl() {
        Set<ConstraintViolation<CreateUrlRequest>> violations = validator.validate(new CreateUrlRequest("https://example.com/path", 43200));
        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldRejectBlankUrl() {
        Set<ConstraintViolation<CreateUrlRequest>> violations = validator.validate(new CreateUrlRequest("", 60));
        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldRejectUrlWithoutScheme() {
        Set<ConstraintViolation<CreateUrlRequest>> violations = validator.validate(new CreateUrlRequest("example.com", 60));
        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldRejectFtpScheme() {
        Set<ConstraintViolation<CreateUrlRequest>> violations = validator.validate(new CreateUrlRequest("ftp://example.com", 60));
        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldRejectExpirationTooSmall() {
        Set<ConstraintViolation<CreateUrlRequest>> violations = validator.validate(new CreateUrlRequest("https://x.com", 0));
        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldRejectExpirationTooLarge() {
        Set<ConstraintViolation<CreateUrlRequest>> violations = validator.validate(new CreateUrlRequest("https://x.com", 43201));
        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldAllowNullExpirationToUseDefault() {
        Set<ConstraintViolation<CreateUrlRequest>> violations = validator.validate(new CreateUrlRequest("https://x.com", null));
        assertTrue(violations.isEmpty());
    }
}
