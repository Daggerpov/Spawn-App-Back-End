package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.ReportedContentDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.ReportType;
import com.danielagapov.spawn.Enums.ResolutionStatus;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Services.Report.IReportContentService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/reports")
@AllArgsConstructor
public class ReportController {
    private final IReportContentService reportService;

    // full path: /api/v1/reports?type=?<ReportType>&content-type=<EntityType>
    @GetMapping
    public ResponseEntity<List<ReportedContentDTO>> getReports(
            @RequestParam(value = "report-type", required = false) ReportType reportType,
            @RequestParam(value = "content-type", required = false) EntityType contentType
    ) {
        try {
            List<ReportedContentDTO> reports = reportService.getReportsByFilters(reportType, contentType);
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // full path: /api/v1/reports
    @PostMapping
    public ResponseEntity<ReportedContentDTO> createReport(@RequestBody ReportedContentDTO report) {
        try {
            ReportedContentDTO newReport = reportService.fileReport(report);
            return ResponseEntity.status(HttpStatus.CREATED).body(newReport);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // full path: /api/v1/reports/{report-id}?resolution=<resolution
    @PostMapping("{reportId}")
    public ResponseEntity<ReportedContentDTO> resolveReport(@PathVariable UUID reportId, @RequestParam("report-id") ResolutionStatus resolution) {
        try {
            ReportedContentDTO report = reportService.resolveReport(reportId, resolution);
            return ResponseEntity.ok(report);
        } catch (BaseNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // full path: /api/v1/reports/reporters/{reporter-id}
    @GetMapping("reporter/{reporterId}")
    public ResponseEntity<List<ReportedContentDTO>> getReportsByReporter(@PathVariable UUID reporterId) {
        try {
            List<ReportedContentDTO> reports = reportService.getReportsByReporterId(reporterId);
            return ResponseEntity.ok(reports);
        } catch (BaseNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // full path: /api/v1/reports/reported-users/{content-owner-id}
    @GetMapping("{contentOwnerId}")
    public ResponseEntity<List<ReportedContentDTO>> getReportsByContentOwner(@PathVariable UUID contentOwnerId) {
        try {
            List<ReportedContentDTO> reports = reportService.getReportsByContentOwnerId(contentOwnerId);
            return ResponseEntity.ok(reports);
        } catch (BaseNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // full path: /api/v1/reports/{report-id}
    @DeleteMapping("{reportId}")
    public ResponseEntity<Void> deleteReport(@PathVariable UUID reportId) {
        try {
            reportService.deleteReportById(reportId);
            return ResponseEntity.noContent().build();
        } catch (BaseNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // full path: /api/v1/reports/{report-id}
    @GetMapping("{reportId}")
    public ResponseEntity<ReportedContentDTO> getReportById(@PathVariable UUID reportId) {
        try {
            ReportedContentDTO report = reportService.getReportById(reportId);
            return ResponseEntity.ok(report);
        } catch (BaseNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

}
