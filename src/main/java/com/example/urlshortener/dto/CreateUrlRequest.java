package com.example.urlshortener.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateUrlRequest(
        @NotBlank
        @Pattern(regexp = "^(https?)://.+", message = "originalUrl must be a valid http(s) URL")
        String originalUrl,

        @Min(1) @Max(43200)
        Integer expirationMinutes
) {}
