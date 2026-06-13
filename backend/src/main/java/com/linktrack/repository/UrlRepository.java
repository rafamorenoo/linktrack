package com.linktrack.repository;

import com.linktrack.model.Url;
import com.linktrack.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UrlRepository extends JpaRepository<Url, UUID> {
    Optional<Url> findByShortCode(String shortCode);
    boolean existsByShortCode(String shortCode);
    Page<Url> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}
