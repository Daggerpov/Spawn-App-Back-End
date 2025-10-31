package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.ErrorLog.CreateErrorLogDTO;
import com.danielagapov.spawn.DTOs.ErrorLog.ErrorLogDTO;
import com.danielagapov.spawn.DTOs.ErrorLog.UpdateErrorLogStatusDTO;
import com.danielagapov.spawn.Enums.ResolutionStatus;
import com.danielagapov.spawn.Services.ErrorLog.IErrorLogService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/error-logs")
@AllArgsConstructor
@CrossOrigin(origins = "*")
public class ErrorLogController {
    
    private final IErrorLogService errorLogService;
    
    /**
     * Get all error logs with pagination
     */
    @GetMapping
    public ResponseEntity<Page<ErrorLogDTO>> getAllErrorLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ErrorLogDTO> errorLogs = errorLogService.getAllErrorLogs(pageable);
        return ResponseEntity.ok(errorLogs);
    }
    
    /**
     * Get error logs by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<ErrorLogDTO>> getErrorLogsByStatus(
            @PathVariable ResolutionStatus status) {
        
        List<ErrorLogDTO> errorLogs = errorLogService.getErrorLogsByStatus(status);
        return ResponseEntity.ok(errorLogs);
    }
    
    /**
     * Get error logs by status with pagination
     */
    @GetMapping("/status/{status}/paginated")
    public ResponseEntity<Page<ErrorLogDTO>> getErrorLogsByStatusPaginated(
            @PathVariable ResolutionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ErrorLogDTO> errorLogs = errorLogService.getErrorLogsByStatus(status, pageable);
        return ResponseEntity.ok(errorLogs);
    }
    
    /**
     * Get a specific error log by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ErrorLogDTO> getErrorLogById(@PathVariable UUID id) {
        ErrorLogDTO errorLog = errorLogService.getErrorLogById(id);
        return ResponseEntity.ok(errorLog);
    }
    
    /**
     * Create a new error log entry
     */
    @PostMapping
    public ResponseEntity<ErrorLogDTO> createErrorLog(@RequestBody CreateErrorLogDTO createErrorLogDTO) {
        ErrorLogDTO createdErrorLog = errorLogService.createErrorLog(createErrorLogDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdErrorLog);
    }
    
    /**
     * Update error log status and admin comment
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ErrorLogDTO> updateErrorLogStatus(
            @PathVariable UUID id,
            @RequestBody UpdateErrorLogStatusDTO updateDTO) {
        
        ErrorLogDTO updatedErrorLog = errorLogService.updateErrorLogStatus(id, updateDTO);
        return ResponseEntity.ok(updatedErrorLog);
    }
    
    /**
     * Delete an error log
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteErrorLog(@PathVariable UUID id) {
        errorLogService.deleteErrorLog(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Get count of unresolved errors
     */
    @GetMapping("/count/unresolved")
    public ResponseEntity<Long> getUnresolvedErrorCount() {
        long count = errorLogService.getUnresolvedErrorCount();
        return ResponseEntity.ok(count);
    }
    
    /**
     * Manually trigger email notifications processing
     */
    @PostMapping("/process-notifications")
    public ResponseEntity<String> processEmailNotifications() {
        errorLogService.processEmailNotifications();
        return ResponseEntity.ok("Email notifications processed");
    }
}

