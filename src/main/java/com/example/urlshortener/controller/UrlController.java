package com.example.urlshortener.controller;

import com.example.urlshortener.dto.CreateUrlRequest;
import com.example.urlshortener.dto.CreateUrlResponse;
import com.example.urlshortener.dto.UrlStatsResponse;
import com.example.urlshortener.service.UrlService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/urls")
@Tag(name = "URLs", description = "Create, list, inspect and delete shortened URLs")
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

    @GetMapping
    @Operation(summary = "List all active short URLs with pagination")
    public ResponseEntity<Page<CreateUrlResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(urlService.listAll(page, size));
    }

    @GetMapping("/{shortCode}/stats")
    @Operation(summary = "Get access stats for a short URL (total + per day)")
    public ResponseEntity<UrlStatsResponse> stats(@PathVariable String shortCode) {
        return ResponseEntity.ok(urlService.getStats(shortCode));
    }

    @GetMapping("/{shortCode}/qr")
    @Operation(summary = "Generate QR code image for a short URL")
    public void qrCode(@PathVariable String shortCode, HttpServletResponse response) throws IOException, WriterException {
        String shortUrl = urlService.getShortUrl(shortCode);
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(shortUrl, BarcodeFormat.QR_CODE, 300, 300);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

        response.setContentType(MediaType.IMAGE_PNG_VALUE);
        response.setHeader("Cache-Control", CacheControl.maxAge(24, TimeUnit.HOURS).getHeaderValue());
        response.getOutputStream().write(outputStream.toByteArray());
        response.getOutputStream().flush();
    }

    @DeleteMapping("/{shortCode}")
    @Operation(summary = "Delete (deactivate) a short URL")
    public ResponseEntity<Void> delete(@PathVariable String shortCode) {
        urlService.delete(shortCode);
        return ResponseEntity.noContent().build();
    }
}
