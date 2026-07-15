package com.example.urlshortener.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUrlRequest(
        @NotBlank
        @Size(max = 2048, message = "originalUrl must not exceed 2048 characters")
        @Pattern(regexp = "^(https?)://.+", message = "originalUrl must be a valid http(s) URL")
        String originalUrl,

        @Min(1) @Max(43200)
        Integer expirationMinutes,

        @Size(min = 3, max = 32, message = "customAlias must be between 3 and 32 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "customAlias must contain only alphanumeric, hyphens, or underscores")
        String customAlias
) {}
