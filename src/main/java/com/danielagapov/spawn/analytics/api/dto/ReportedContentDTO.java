package com.danielagapov.spawn.analytics.api.dto;

import com.danielagapov.spawn.shared.util.EntityType;
import com.danielagapov.spawn.shared.util.ReportType;
import com.danielagapov.spawn.shared.util.ResolutionStatus;
import com.danielagapov.spawn.analytics.internal.domain.ReportedContent;
import com.danielagapov.spawn.user.internal.domain.User;
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
public class ReportedContentDTO implements Serializable {

    private UUID id;
    private User reporter;
    private UUID contentId;
    private EntityType contentType;
    private OffsetDateTime timeReported;
    private ResolutionStatus resolution;
    private ReportType reportType;
    private String description;
    private User reportedUser; // owner of the account/content that has been reported

    public static ReportedContentDTO fromEntity(ReportedContent reportedContentEntity) {
        return new ReportedContentDTO(
                reportedContentEntity.getId(),
                reportedContentEntity.getReporter(),
                reportedContentEntity.getContentId(),
                reportedContentEntity.getContentType(),
                reportedContentEntity.getTimeReported(),
                reportedContentEntity.getResolution(),
                reportedContentEntity.getReportType(),
                reportedContentEntity.getDescription(),
                reportedContentEntity.getContentOwner()
        );
    }

    public static List<ReportedContentDTO> fromEntityList(List<ReportedContent> reportedContentEntityList) {
        return reportedContentEntityList.stream().map(ReportedContentDTO::fromEntity).toList();
    }

    public ReportedContent toEntity() {
        return new ReportedContent(
                this.id,
                this.reporter,
                this.contentId,
                this.contentType,
                this.timeReported,
                this.resolution,
                this.reportType,
                this.description,
                this.reportedUser
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true; // Check if the same reference
        if (obj == null || getClass() != obj.getClass()) return false; // Null check and class check
        ReportedContentDTO that = (ReportedContentDTO) obj; // Safe cast
        return id != null && id.equals(that.id); // Compare IDs
    }
}
