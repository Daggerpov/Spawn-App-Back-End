package com.danielagapov.spawn.analytics.api.dto;

import com.danielagapov.spawn.shared.util.EntityType;
import com.danielagapov.spawn.shared.util.ReportType;
import com.danielagapov.spawn.shared.util.ResolutionStatus;
import com.danielagapov.spawn.analytics.internal.domain.ReportedContent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FetchReportedContentDTO implements Serializable {
    private UUID id;
    private UUID reporterUserId;
    private String reporterUsername;
    private UUID contentId;
    private EntityType contentType;
    private OffsetDateTime timeReported;
    private ResolutionStatus resolution;
    private ReportType reportType;
    private String description;
    private UUID reportedUserId;
    private String reportedUsername;

    public static FetchReportedContentDTO fromEntity(ReportedContent reportedContentEntity) {
        return new FetchReportedContentDTO(
                reportedContentEntity.getId(),
                reportedContentEntity.getReporter() != null ? reportedContentEntity.getReporter().getId() : null,
                reportedContentEntity.getReporter() != null ? reportedContentEntity.getReporter().getUsername() : null,
                reportedContentEntity.getContentId(),
                reportedContentEntity.getContentType(),
                reportedContentEntity.getTimeReported(),
                reportedContentEntity.getResolution(),
                reportedContentEntity.getReportType(),
                reportedContentEntity.getDescription(),
                reportedContentEntity.getContentOwner() != null ? reportedContentEntity.getContentOwner().getId() : null,
                reportedContentEntity.getContentOwner() != null ? reportedContentEntity.getContentOwner().getUsername() : null
        );
    }

    public static List<FetchReportedContentDTO> fromEntityList(List<ReportedContent> reportedContentEntityList) {
        return reportedContentEntityList.stream().map(FetchReportedContentDTO::fromEntity).toList();
    }
} 