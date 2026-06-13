package com.linktrack.repository;

import com.linktrack.model.ClickEvent;
import com.linktrack.model.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ClickEventRepository extends JpaRepository<ClickEvent, UUID> {
    long countByUrl(Url url);

    @Query("SELECT DATE(c.clickedAt), COUNT(c) FROM ClickEvent c WHERE c.url = :url AND c.clickedAt >= :from GROUP BY DATE(c.clickedAt) ORDER BY DATE(c.clickedAt)")
    List<Object[]> countClicksPerDaySince(Url url, LocalDateTime from);

    @Query("SELECT c.country, COUNT(c) FROM ClickEvent c WHERE c.url = :url GROUP BY c.country ORDER BY COUNT(c) DESC")
    List<Object[]> countClicksByCountry(Url url);

    @Query("SELECT c.deviceType, COUNT(c) FROM ClickEvent c WHERE c.url = :url GROUP BY c.deviceType ORDER BY COUNT(c) DESC")
    List<Object[]> countClicksByDevice(Url url);
}
