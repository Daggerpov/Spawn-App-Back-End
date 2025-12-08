package com.danielagapov.spawn.user.api;

import com.danielagapov.spawn.analytics.api.dto.CreateReportedContentDTO;
import com.danielagapov.spawn.analytics.api.dto.FetchReportedContentDTO;
import com.danielagapov.spawn.analytics.api.dto.ReportedContentDTO;
import com.danielagapov.spawn.shared.util.EntityType;
import com.danielagapov.spawn.shared.util.ReportType;
import com.danielagapov.spawn.shared.exceptions.BaseNotFoundException;
import com.danielagapov.spawn.shared.exceptions.BasesNotFoundException;
import com.danielagapov.spawn.shared.exceptions.ILogger;
import com.danielagapov.spawn.analytics.internal.services.IReportContentService;
import com.danielagapov.spawn.shared.util.LoggingUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/reports")
public final class ReportController {
    private final IReportContentService reportService;
    private final ILogger logger;

    public ReportController(IReportContentService reportService, ILogger logger) {
        this.reportService = reportService;
        this.logger = logger;
    }

    // full path: /api/v1/reports/create
    @PostMapping("/create")
    public ResponseEntity<ReportedContentDTO> createReportSimplified(@RequestBody CreateReportedContentDTO createReportDTO) {
        try {
            ReportedContentDTO newReport = reportService.fileReport(createReportDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(newReport);
        } catch (Exception e) {
            logger.error("Error creating report: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // full path: /api/v1/reports/fetch?reportType=?<ReportType>&contentType=<EntityType>
    @GetMapping("/fetch")
    public ResponseEntity<List<FetchReportedContentDTO>> getFetchReports(
            @RequestParam(value = "reportType", required = false) ReportType reportType,
            @RequestParam(value = "contentType", required = false) EntityType contentType
    ) {
        try {
            List<FetchReportedContentDTO> reports = reportService.getFetchReportsByFilters(reportType, contentType);
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            logger.error("Error getting reports with filters: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // full path: /api/v1/reports/fetch/reporter/{reporterId}
    @GetMapping("/fetch/reporter/{reporterId}")
    public ResponseEntity<?> getFetchReportsByReporter(@PathVariable UUID reporterId) {
        if (reporterId == null) {
            logger.error("Invalid parameter: reporterId is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            List<FetchReportedContentDTO> reports = reportService.getFetchReportsByReporterId(reporterId);
            return ResponseEntity.ok(reports);
        } catch (BaseNotFoundException e) {
            logger.error("Reporter not found: " + LoggingUtils.formatUserIdInfo(reporterId) + ": " + e.getMessage());
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (BasesNotFoundException e) {
            if (e.entityType == EntityType.ReportedContent) {
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

    // full path: /api/v1/reports/fetch/content-owner/{contentOwnerId}
    @GetMapping("/fetch/content-owner/{contentOwnerId}")
    public ResponseEntity<?> getFetchReportsByContentOwner(@PathVariable UUID contentOwnerId) {
        if (contentOwnerId == null) {
            logger.error("Invalid parameter: contentOwnerId is null");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            List<FetchReportedContentDTO> reports = reportService.getFetchReportsByContentOwnerId(contentOwnerId);
            return ResponseEntity.ok(reports);
        } catch (BaseNotFoundException e) {
            logger.error("Content owner not found: " + LoggingUtils.formatUserIdInfo(contentOwnerId) + ": " + e.getMessage());
            return new ResponseEntity<>(e.entityType, HttpStatus.NOT_FOUND);
        } catch (BasesNotFoundException e) {
            if (e.entityType == EntityType.ReportedContent) {
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


}
