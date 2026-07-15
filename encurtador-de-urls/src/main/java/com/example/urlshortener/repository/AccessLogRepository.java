package com.example.urlshortener.repository;

import com.example.urlshortener.entity.AccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {

    Optional<AccessLog> findByUrlIdAndAccessDate(Long urlId, LocalDate accessDate);

    @Query("SELECT a.accessDate as date, SUM(a.accessCount) as count FROM AccessLog a WHERE a.urlId = :urlId GROUP BY a.accessDate ORDER BY a.accessDate ASC")
    List<Object[]> findDailyStatsByUrlId(@Param("urlId") Long urlId);

    @Modifying
    @Query(value = """
        INSERT INTO access_logs (url_id, access_date, access_count)
        VALUES (:urlId, :accessDate, 1)
        ON CONFLICT (url_id, access_date)
        DO UPDATE SET access_count = access_logs.access_count + 1
        """, nativeQuery = true)
    void upsertAccessLog(@Param("urlId") Long urlId, @Param("accessDate") LocalDate accessDate);
}