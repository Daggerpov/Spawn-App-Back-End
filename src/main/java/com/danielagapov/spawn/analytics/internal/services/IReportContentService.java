package com.danielagapov.spawn.analytics.internal.services;

import com.danielagapov.spawn.analytics.api.dto.CreateReportedContentDTO;
import com.danielagapov.spawn.analytics.api.dto.FetchReportedContentDTO;
import com.danielagapov.spawn.analytics.api.dto.ReportedContentDTO;
import com.danielagapov.spawn.shared.util.EntityType;
import com.danielagapov.spawn.shared.util.ReportType;

import java.util.List;
import java.util.UUID;

public interface IReportContentService {

    /**
     * Creates and saves a new report with the given simplified creation DTO
     */
    ReportedContentDTO fileReport(CreateReportedContentDTO createReportDTO);

    /**
     * Gets all reports by reporter id (simplified for "my reports" page)
     */
    List<FetchReportedContentDTO> getFetchReportsByReporterId(UUID reporterId);

    /**
     * Gets all reports by reported user id (simplified for admin dashboard)
     */
    List<FetchReportedContentDTO> getFetchReportsByContentOwnerId(UUID contentOwnerId);

    /**
     * Gets all reports with given reportType and contentType (simplified for admin dashboard)
     */
    List<FetchReportedContentDTO> getFetchReportsByFilters(ReportType reportType, EntityType contentType);

    /**
     * Updates the resolution status of a report
     */
    ReportedContentDTO updateReportStatus(UUID reportId, String resolution);

    /**
     * Deletes a report by id
     */
    void deleteReport(UUID reportId);

}
