package com.danielagapov.spawn.Services.Report;

import com.danielagapov.spawn.DTOs.CreateReportedContentDTO;
import com.danielagapov.spawn.DTOs.FetchReportedContentDTO;
import com.danielagapov.spawn.DTOs.ReportedContentDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.ReportType;
import com.danielagapov.spawn.Enums.ResolutionStatus;

import java.util.List;
import java.util.UUID;

public interface IReportContentService {

    /**
     * Gets all reports with given reportType and contentType
     * If a filter (reportType/contentType) is null then no filter will be applied
     * So this method can be used to get all reports if both filters are null
     */
    List<ReportedContentDTO> getReportsByFilters(ReportType reportType, EntityType contentType);

    /**
     * Creates and saves a new report with the given simplified creation DTO
     */
    ReportedContentDTO fileReport(CreateReportedContentDTO createReportDTO);

    /**
     * Creates and saves a new report with the given report DTO
     * @deprecated Use fileReport(CreateReportedContentDTO) for new implementations
     */
    @Deprecated
    ReportedContentDTO fileReport(ReportedContentDTO report);

    /**
     * Resolves the report with the given reportId, with the given resolution
     */
    ReportedContentDTO resolveReport(UUID reportId, ResolutionStatus resolution);

    /**
     * Gets all reports by reporter id
     */
    List<ReportedContentDTO> getReportsByReporterId(UUID reporterId);

    /**
     * Gets all reports by reported user id
     */
    List<ReportedContentDTO> getReportsByContentOwnerId(UUID contentOwnerId);

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
     * Deletes report with the given report id
     */
    void deleteReportById(UUID reportId);

    /**
     * Gets report with the given report id
     */
    ReportedContentDTO getReportById(UUID reportId);
}
