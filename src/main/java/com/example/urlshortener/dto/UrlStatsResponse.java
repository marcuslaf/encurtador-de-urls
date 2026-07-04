package com.example.urlshortener.dto;

import java.time.LocalDate;
import java.util.List;

public record UrlStatsResponse(
        String shortCode,
        long totalAccesses,
        List<DailyAccess> dailyAccesses
) {
    public record DailyAccess(LocalDate date, long count) {}
}
