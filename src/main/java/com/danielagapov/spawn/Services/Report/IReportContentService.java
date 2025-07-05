package com.danielagapov.spawn.Services.Report;

import com.danielagapov.spawn.DTOs.CreateReportedContentDTO;
import com.danielagapov.spawn.DTOs.FetchReportedContentDTO;
import com.danielagapov.spawn.DTOs.ReportedContentDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.ReportType;

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


}
