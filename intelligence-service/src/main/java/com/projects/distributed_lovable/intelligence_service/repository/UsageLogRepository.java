package com.projects.distributed_lovable.intelligence_service.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projects.distributed_lovable.intelligence_service.entity.UsageLog;

public interface UsageLogRepository extends JpaRepository<UsageLog, Long> {

    Optional<UsageLog> findByUserIdAndDate(Long userId, LocalDate today);

}
