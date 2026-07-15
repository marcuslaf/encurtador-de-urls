package com.example.urlshortener.repository;

import com.example.urlshortener.entity.Url;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    @Modifying
    @Query("UPDATE Url u SET u.active = false WHERE u.active = true AND u.expiresAt < :now")
    int deactivateExpired(@Param("now") Instant now);

    Page<Url> findAllByActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    Optional<Url> findByShortCodeAndActiveTrue(String shortCode);
}
