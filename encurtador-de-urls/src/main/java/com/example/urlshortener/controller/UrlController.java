package com.example.urlshortener.controller;

import com.example.urlshortener.dto.CreateUrlRequest;
import com.example.urlshortener.dto.CreateUrlResponse;
import com.example.urlshortener.dto.UrlStatsResponse;
import com.example.urlshortener.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/urls")
@Tag(name = "URLs", description = "Create and inspect shortened URLs")
public class UrlController {

    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @PostMapping
    @Operation(summary = "Create a short URL")
    public ResponseEntity<CreateUrlResponse> create(@Valid @RequestBody CreateUrlRequest request) {
        CreateUrlResponse body = urlService.shorten(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/{shortCode}/stats")
    @Operation(summary = "Get access stats for a short URL (total + per day)")
    public ResponseEntity<UrlStatsResponse> stats(@PathVariable String shortCode) {
        return ResponseEntity.ok(urlService.getStats(shortCode));
    }
}
