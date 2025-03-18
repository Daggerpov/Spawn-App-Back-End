package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.ReportType;
import com.danielagapov.spawn.Models.ReportedContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IReportedContentRepository extends JpaRepository<ReportedContent, UUID> {
    List<ReportedContent> getAllByContentTypeAndReportType(EntityType contentType, ReportType reportType);

    List<ReportedContent> getAllByReportType(ReportType reportType);

    List<ReportedContent> getAllByContentType(EntityType contentType);

    List<ReportedContent> getAllByReporterId(UUID reporterId);

    List<ReportedContent> getAllByContentOwnerId(UUID reporterId);
}
