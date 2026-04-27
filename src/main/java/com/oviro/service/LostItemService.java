package com.oviro.service;

import com.oviro.dto.request.LostItemReportRequest;
import com.oviro.dto.response.LostItemReportResponse;
import com.oviro.enums.NotificationType;
import com.oviro.exception.ResourceNotFoundException;
import com.oviro.model.LostItemReport;
import com.oviro.model.Ride;
import com.oviro.repository.LostItemReportRepository;
import com.oviro.repository.RideRepository;
import com.oviro.util.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LostItemService {

    private final LostItemReportRepository lostItemReportRepository;
    private final RideRepository rideRepository;
    private final SecurityContextHelper securityHelper;
    private final NotificationService notificationService;

    @Transactional
    public LostItemReportResponse report(LostItemReportRequest request) {
        UUID userId = securityHelper.getCurrentUserId();
        Ride ride = rideRepository.findById(request.getRideId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", request.getRideId().toString()));

        LostItemReport report = LostItemReport.builder()
                .reporterId(userId)
                .ride(ride)
                .itemDescription(request.getItemDescription())
                .itemColor(request.getItemColor())
                .contactPhone(request.getContactPhone())
                .build();

        report = lostItemReportRepository.save(report);

        if (ride.getDriver() != null) {
            notificationService.sendToUser(ride.getDriver().getUser().getId(),
                    NotificationType.RIDE_COMPLETED,
                    "Objet oublié signalé",
                    "Un passager a signalé avoir oublié un objet dans votre véhicule : " + request.getItemDescription(),
                    Map.of("reportId", report.getId().toString()), 3600L, true);
        }

        log.info("Objet oublié signalé pour ride={} par userId={}", request.getRideId(), userId);
        return mapResponse(report);
    }

    @Transactional(readOnly = true)
    public Page<LostItemReportResponse> getMyReports(Pageable pageable) {
        UUID userId = securityHelper.getCurrentUserId();
        return lostItemReportRepository.findByReporterId(userId, pageable).map(this::mapResponse);
    }

    @Transactional(readOnly = true)
    public Page<LostItemReportResponse> getAllUnresolved(Pageable pageable) {
        return lostItemReportRepository.findByResolved(false, pageable).map(this::mapResponse);
    }

    @Transactional
    public LostItemReportResponse resolve(UUID reportId, String notes) {
        LostItemReport report = lostItemReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport", reportId.toString()));
        report.setResolved(true);
        report.setResolutionNotes(notes);
        return mapResponse(lostItemReportRepository.save(report));
    }

    private LostItemReportResponse mapResponse(LostItemReport r) {
        return LostItemReportResponse.builder()
                .id(r.getId())
                .reporterId(r.getReporterId())
                .rideId(r.getRide().getId())
                .itemDescription(r.getItemDescription())
                .itemColor(r.getItemColor())
                .contactPhone(r.getContactPhone())
                .resolved(r.isResolved())
                .resolutionNotes(r.getResolutionNotes())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
