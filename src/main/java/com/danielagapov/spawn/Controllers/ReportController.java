package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.Models.ReportedContent;
import com.danielagapov.spawn.Services.Report.IReportContentService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/reports")
@AllArgsConstructor
public class ReportController {
    private final IReportContentService reportService;

    @GetMapping
    public ResponseEntity<List<ReportedContent>> getAllReports() {
        return ResponseEntity.ok().body(List.of());
    }

    @PostMapping
    public ResponseEntity<ReportedContent> createReport(@RequestBody ReportedContent report) {
        return ResponseEntity.ok().body(new ReportedContent());
    }

}
