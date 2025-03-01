package com.danielagapov.spawn.Services.Report;

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
     * Creates and saves a new report with the given report DTO
     */
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
     * Deletes report with the given report id
     */
    void deleteReportById(UUID reportId);

    /**
     * Gets report with the given report id
     */
    ReportedContentDTO getReportById(UUID reportId);
}
