package com.example.urlshortener.scheduler;

import com.example.urlshortener.service.UrlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExpirationScheduler.class);

    private final UrlService urlService;

    public ExpirationScheduler(UrlService urlService) {
        this.urlService = urlService;
    }

    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT1M")
    public void purgeExpired() {
        log.debug("Running expiration sweep");
        urlService.deactivateExpired();
    }
}
