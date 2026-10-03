package com.cookify.moderation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarningRepository extends JpaRepository<Warning, Long> {

    List<Warning> findByUserIdOrderByCreatedAtDesc(Long userId);
}
