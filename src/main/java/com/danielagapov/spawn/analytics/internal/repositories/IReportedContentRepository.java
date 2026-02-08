package com.danielagapov.spawn.analytics.internal.repositories;

import com.danielagapov.spawn.shared.util.EntityType;
import com.danielagapov.spawn.shared.util.ReportType;
import com.danielagapov.spawn.analytics.internal.domain.ReportedContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IReportedContentRepository extends JpaRepository<ReportedContent, UUID> {
    List<ReportedContent> getAllByContentTypeAndReportType(EntityType contentType, ReportType reportType);

    List<ReportedContent> getAllByReportType(ReportType reportType);

    List<ReportedContent> getAllByContentType(EntityType contentType);

    List<ReportedContent> getAllByReporterId(UUID reporterId);

    List<ReportedContent> getAllByContentOwnerId(UUID reporterId);
}
