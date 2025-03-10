package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.ReportType;
import com.danielagapov.spawn.Models.ReportedContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IReportedContentRepository extends JpaRepository<ReportedContent, UUID> {
    Optional<List<ReportedContent>> getAllByContentTypeAndReportType(EntityType contentType, ReportType reportType);

    Optional<List<ReportedContent>> getAllByReportType(ReportType reportType);

    Optional<List<ReportedContent>> getAllByContentType(EntityType contentType);

    Optional<List<ReportedContent>> getAllByReporterId(UUID reporterId);

    Optional<List<ReportedContent>> getAllByContentOwnerId(UUID reporterId);
}
