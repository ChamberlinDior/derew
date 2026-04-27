package com.oviro.repository;

import com.oviro.model.LostItemReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LostItemReportRepository extends JpaRepository<LostItemReport, UUID> {

    Page<LostItemReport> findByReporterId(UUID reporterId, Pageable pageable);

    Page<LostItemReport> findByResolved(boolean resolved, Pageable pageable);
}
