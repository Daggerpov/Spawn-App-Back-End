package com.danielagapov.spawn.Services.ErrorLog;

import com.danielagapov.spawn.DTOs.ErrorLog.CreateErrorLogDTO;
import com.danielagapov.spawn.DTOs.ErrorLog.ErrorLogDTO;
import com.danielagapov.spawn.DTOs.ErrorLog.UpdateErrorLogStatusDTO;
import com.danielagapov.spawn.Enums.ResolutionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface IErrorLogService {
    
    /**
     * Create a new error log entry
     */
    ErrorLogDTO createErrorLog(CreateErrorLogDTO createErrorLogDTO);
    
    /**
     * Log an error from an exception with automatic context extraction
     */
    ErrorLogDTO logError(String message, Throwable throwable, String userContext);
    
    /**
     * Get all error logs with pagination
     */
    Page<ErrorLogDTO> getAllErrorLogs(Pageable pageable);
    
    /**
     * Get error logs by status
     */
    List<ErrorLogDTO> getErrorLogsByStatus(ResolutionStatus status);
    
    /**
     * Get error logs by status with pagination
     */
    Page<ErrorLogDTO> getErrorLogsByStatus(ResolutionStatus status, Pageable pageable);
    
    /**
     * Get a specific error log by ID
     */
    ErrorLogDTO getErrorLogById(UUID id);
    
    /**
     * Update error log status and admin comment
     */
    ErrorLogDTO updateErrorLogStatus(UUID id, UpdateErrorLogStatusDTO updateDTO);
    
    /**
     * Delete an error log
     */
    void deleteErrorLog(UUID id);
    
    /**
     * Get count of unresolved errors
     */
    long getUnresolvedErrorCount();
    
    /**
     * Process and send email notifications for new errors
     */
    void processEmailNotifications();
}

