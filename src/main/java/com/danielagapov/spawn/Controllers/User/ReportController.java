package com.danielagapov.spawn.Controllers.User;

import com.danielagapov.spawn.DTOs.ReportedContentDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.ReportType;
import com.danielagapov.spawn.Enums.ResolutionStatus;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Services.Report.IReportContentService;
import com.danielagapov.spawn.Util.LoggingUtils;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/reports")
public class ReportController {
    private final IReportContentService reportService;
    private final ILogger logger;

    public ReportController(IReportContentService reportService, ILogger logger) {
        this.reportService = reportService;
        this.logger = logger;
    }

    @Deprecated(since = "Not being used on mobile currently. " +
            "Pending mobile feature implementation, per:" +
            "https://github.com/Daggerpov/Spawn-App-iOS-SwiftUI/issues/142")
    // full path: /api/v1/reports?reportType=?<ReportType>&contentType=<EntityType>
    @GetMapping
    public ResponseEntity<List<ReportedContentDTO>> getReports(
            @RequestParam(value = "reportType", required = false) ReportType reportType,
            @RequestParam(value = "contentType", required = false) EntityType contentType
    ) {
        logger.info("Getting reports with filters - reportType: " + reportType + ", contentType: " + contentType);
        try {
            List<ReportedContentDTO> reports = reportService.getReportsByFilters(reportType, contentType);
            logger.info("Reports retrieved successfully (count: " + reports.size() + ")");
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            logger.error("Error getting reports with filters: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Deprecated(since = "Not being used on mobile currently. " +
            "Pending mobile feature implementation, per:" +
            "https://github.com/Daggerpov/Spawn-App-iOS-SwiftUI/issues/142")
    // full path: /api/v1/reports
    @PostMapping
    public ResponseEntity<ReportedContentDTO> createReport(@RequestBody ReportedContentDTO report) {
        logger.info("Creating new report for content type: " + (report != null ? report.getContentType() : "null"));
        try {
            ReportedContentDTO newReport = reportService.fileReport(report);
            logger.info("Report created successfully with ID: " + newReport.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(newReport);
        } catch (Exception e) {
            logger.error("Error creating report: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Deprecated(since = "Not being used on mobile currently. " +
            "Pending mobile feature implementation, per:" +
            "https://github.com/Daggerpov/Spawn-App-iOS-SwiftUI/issues/142")
    // full path: /api/v1/reports/{reportId}?resolution=<ResolutionStatus>
    @PutMapping("{reportId}")
    public ResponseEntity<ReportedContentDTO> resolveReport(@PathVariable UUID reportId, @RequestParam("resolution") ResolutionStatus resolution) {
        logger.info("Resolving report with ID: " + reportId + " to resolution: " + resolution);
        if (reportId == null) {
            logger.error("Invalid parameter: reportId is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            ReportedContentDTO report = reportService.resolveReport(reportId, resolution);
            logger.info("Report resolved successfully with ID: " + reportId + " to resolution: " + resolution);
            return ResponseEntity.ok(report);
        } catch (BaseNotFoundException e) {
            logger.error("Report not found with ID: " + reportId + ": " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error resolving report with ID: " + reportId + ": " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Deprecated(since = "Not being used on mobile currently. " +
            "Pending mobile feature implementation, per:" +
            "https://github.com/Daggerpov/Spawn-App-iOS-SwiftUI/issues/142")
    // full path: /api/v1/reports/reporters/{reporterId}
    @GetMapping("reporter/{reporterId}")
    public ResponseEntity<?> getReportsByReporter(@PathVariable UUID reporterId) {
        logger.info("Getting reports by reporter: " + LoggingUtils.formatUserIdInfo(reporterId));
        if (reporterId == null) {
            logger.error("Invalid parameter: reporterId is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            List<ReportedContentDTO> reports = reportService.getReportsByReporterId(reporterId);
            logger.info("Reports retrieved successfully for reporter: " + LoggingUtils.formatUserIdInfo(reporterId) + " (count: " + reports.size() + ")");
            return ResponseEntity.ok(reports);
        } catch (BaseNotFoundException e) {
            logger.error("Reporter not found: " + LoggingUtils.formatUserIdInfo(reporterId) + ": " + e.getMessage());
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (BasesNotFoundException e) {
            if (e.entityType == EntityType.ReportedContent) {
                logger.info("No reports found for reporter: " + LoggingUtils.formatUserIdInfo(reporterId) + ", returning empty list");
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
            } else {
                logger.error("Unexpected entity type in BasesNotFoundException for reporter: " + LoggingUtils.formatUserIdInfo(reporterId) + ": " + e.getMessage());
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            logger.error("Error getting reports for reporter: " + LoggingUtils.formatUserIdInfo(reporterId) + ": " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Deprecated(since = "Not being used on mobile currently. " +
            "Pending mobile feature implementation, per:" +
            "https://github.com/Daggerpov/Spawn-App-iOS-SwiftUI/issues/142")
    // full path: /api/v1/reports/reported-users/{contentOwnerId}
    @GetMapping("{contentOwnerId}")
    public ResponseEntity<?> getReportsByContentOwner(@PathVariable UUID contentOwnerId) {
        logger.info("Getting reports by content owner: " + LoggingUtils.formatUserIdInfo(contentOwnerId));
        if (contentOwnerId == null) {
            logger.error("Invalid parameter: contentOwnerId is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            List<ReportedContentDTO> reports = reportService.getReportsByContentOwnerId(contentOwnerId);
            logger.info("Reports retrieved successfully for content owner: " + LoggingUtils.formatUserIdInfo(contentOwnerId) + " (count: " + reports.size() + ")");
            return ResponseEntity.ok(reports);
        } catch (BaseNotFoundException e) {
            logger.error("Content owner not found: " + LoggingUtils.formatUserIdInfo(contentOwnerId) + ": " + e.getMessage());
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (BasesNotFoundException e) {
            if (e.entityType == EntityType.ReportedContent) {
                logger.info("No reports found for content owner: " + LoggingUtils.formatUserIdInfo(contentOwnerId) + ", returning empty list");
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
            } else {
                logger.error("Unexpected entity type in BasesNotFoundException for content owner: " + LoggingUtils.formatUserIdInfo(contentOwnerId) + ": " + e.getMessage());
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            logger.error("Error getting reports for content owner: " + LoggingUtils.formatUserIdInfo(contentOwnerId) + ": " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Deprecated(since = "Not being used on mobile currently. " +
            "Pending mobile feature implementation, per:" +
            "https://github.com/Daggerpov/Spawn-App-iOS-SwiftUI/issues/142")
    // full path: /api/v1/reports/{reportId}
    @DeleteMapping("{reportId}")
    public ResponseEntity<?> deleteReport(@PathVariable UUID reportId) {
        logger.info("Deleting report with ID: " + reportId);
        if (reportId == null) {
            logger.error("Invalid parameter: reportId is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            reportService.deleteReportById(reportId);
            logger.info("Report deleted successfully with ID: " + reportId);
            return ResponseEntity.noContent().build();
        } catch (BaseNotFoundException e) {
            logger.error("Report not found for deletion with ID: " + reportId + ": " + e.getMessage());
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error deleting report with ID: " + reportId + ": " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Deprecated(since = "Not being used on mobile currently. " +
            "Pending mobile feature implementation, per:" +
            "https://github.com/Daggerpov/Spawn-App-iOS-SwiftUI/issues/142")
    // full path: /api/v1/reports/{reportId}
    @GetMapping("{reportId}")
    public ResponseEntity<?> getReportById(@PathVariable UUID reportId) {
        logger.info("Getting report by ID: " + reportId);
        if (reportId == null) {
            logger.error("Invalid parameter: reportId is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            ReportedContentDTO report = reportService.getReportById(reportId);
            logger.info("Report retrieved successfully with ID: " + reportId);
            return ResponseEntity.ok(report);
        } catch (BaseNotFoundException e) {
            logger.error("Report not found with ID: " + reportId + ": " + e.getMessage());
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error getting report with ID: " + reportId + ": " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

}
