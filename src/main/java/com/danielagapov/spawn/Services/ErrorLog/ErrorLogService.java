package com.danielagapov.spawn.Services.ErrorLog;

import com.danielagapov.spawn.DTOs.ErrorLog.CreateErrorLogDTO;
import com.danielagapov.spawn.DTOs.ErrorLog.ErrorLogDTO;
import com.danielagapov.spawn.DTOs.ErrorLog.UpdateErrorLogStatusDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.ResolutionStatus;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.ErrorLogMapper;
import com.danielagapov.spawn.Models.ErrorLog;
import com.danielagapov.spawn.Repositories.IErrorLogRepository;
import com.danielagapov.spawn.Services.Email.IEmailService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ErrorLogService implements IErrorLogService {
    
    private final IErrorLogRepository repository;
    private final ErrorLogMapper mapper;
    private final IEmailService emailService;
    private final ILogger logger;
    
    @Override
    @Transactional
    public ErrorLogDTO createErrorLog(CreateErrorLogDTO createErrorLogDTO) {
        ErrorLog errorLog = mapper.toEntity(createErrorLogDTO);
        ErrorLog savedErrorLog = repository.save(errorLog);
        
        // Asynchronously send email notification
        sendErrorNotificationEmail(savedErrorLog);
        
        return mapper.toDTO(savedErrorLog);
    }
    
    @Override
    @Transactional
    public ErrorLogDTO logError(String message, Throwable throwable, String userContext) {
        // Extract stack trace information
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        String sourceClass = "Unknown";
        String sourceMethod = "Unknown";
        Integer lineNumber = 0;
        
        if (stackTrace.length > 0) {
            StackTraceElement firstElement = stackTrace[0];
            sourceClass = firstElement.getClassName();
            sourceMethod = firstElement.getMethodName();
            lineNumber = firstElement.getLineNumber();
        }
        
        // Convert stack trace to string
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String stackTraceString = sw.toString();
        
        // Check for recent similar errors to avoid spam
        Instant oneDayAgo = Instant.now().minusSeconds(24 * 60 * 60);
        List<ErrorLog> recentSimilar = repository.findRecentSimilarErrors(
            oneDayAgo, sourceClass, sourceMethod, lineNumber
        );
        
        CreateErrorLogDTO createDTO = new CreateErrorLogDTO(
            message,
            stackTraceString,
            sourceClass,
            sourceMethod,
            lineNumber,
            "ERROR",
            userContext
        );
        
        ErrorLog errorLog = mapper.toEntity(createDTO);
        
        // If we have recent similar errors, don't send another email
        if (!recentSimilar.isEmpty()) {
            errorLog.setEmailSent(true);
        }
        
        ErrorLog savedErrorLog = repository.save(errorLog);
        
        // Only send email if no recent similar errors
        if (recentSimilar.isEmpty()) {
            sendErrorNotificationEmail(savedErrorLog);
        }
        
        return mapper.toDTO(savedErrorLog);
    }
    
    @Override
    public Page<ErrorLogDTO> getAllErrorLogs(Pageable pageable) {
        Page<ErrorLog> errorLogs = repository.findAllByOrderByOccurredAtDesc(pageable);
        return errorLogs.map(mapper::toDTO);
    }
    
    @Override
    public List<ErrorLogDTO> getErrorLogsByStatus(ResolutionStatus status) {
        List<ErrorLog> errorLogs = repository.findByStatusOrderByOccurredAtDesc(status);
        return errorLogs.stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public Page<ErrorLogDTO> getErrorLogsByStatus(ResolutionStatus status, Pageable pageable) {
        Page<ErrorLog> errorLogs = repository.findByStatusOrderByOccurredAtDesc(status, pageable);
        return errorLogs.map(mapper::toDTO);
    }
    
    @Override
    public ErrorLogDTO getErrorLogById(UUID id) {
        ErrorLog errorLog = repository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(EntityType.ErrorLog, id.toString(), "id"));
        return mapper.toDTO(errorLog);
    }
    
    @Override
    @Transactional
    public ErrorLogDTO updateErrorLogStatus(UUID id, UpdateErrorLogStatusDTO updateDTO) {
        ErrorLog errorLog = repository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(EntityType.ErrorLog, id.toString(), "id"));
        
        errorLog.setStatus(updateDTO.getStatus());
        errorLog.setAdminComment(updateDTO.getAdminComment());
        
        ErrorLog updatedErrorLog = repository.save(errorLog);
        return mapper.toDTO(updatedErrorLog);
    }
    
    @Override
    @Transactional
    public void deleteErrorLog(UUID id) {
        ErrorLog errorLog = repository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(EntityType.ErrorLog, id.toString(), "id"));
        repository.delete(errorLog);
    }
    
    @Override
    public long getUnresolvedErrorCount() {
        return repository.countByStatus(ResolutionStatus.PENDING) + 
               repository.countByStatus(ResolutionStatus.IN_PROGRESS);
    }
    
    @Override
    @Transactional
    public void processEmailNotifications() {
        List<ErrorLog> unsentErrors = repository.findByEmailSentFalseOrderByOccurredAtAsc();
        
        for (ErrorLog errorLog : unsentErrors) {
            try {
                sendErrorNotificationEmail(errorLog);
                errorLog.setEmailSent(true);
                repository.save(errorLog);
            } catch (Exception e) {
                logger.warn("Failed to send email notification for error log: " + errorLog.getId());
            }
        }
    }
    
    /**
     * Scheduled task to process email notifications every 5 minutes
     */
    @Scheduled(fixedRate = 5 * 60 * 1000) // 5 minutes
    public void scheduledEmailNotificationProcessing() {
        try {
            processEmailNotifications();
        } catch (Exception e) {
            logger.warn("Error during scheduled email notification processing: " + e.getMessage());
        }
    }
    
    @Async
    private void sendErrorNotificationEmail(ErrorLog errorLog) {
        try {
            String subject = String.format("🚨 System Error Alert - %s", 
                errorLog.getSourceClass().substring(errorLog.getSourceClass().lastIndexOf('.') + 1));
            
            String emailBody = buildErrorNotificationEmailBody(errorLog);
            
            emailService.sendEmail("spawnappmarketing@gmail.com", subject, emailBody);
            
            // Update the email sent flag
            errorLog.setEmailSent(true);
            repository.save(errorLog);
            
        } catch (Exception e) {
            logger.warn("Failed to send error notification email for error: " + errorLog.getId());
        }
    }
    
    private String buildErrorNotificationEmailBody(ErrorLog errorLog) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>System Error Alert</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }
                    .container { background-color: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                    .header { background-color: #dc3545; color: white; padding: 15px; border-radius: 5px; margin-bottom: 20px; }
                    .error-details { background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 10px 0; }
                    .stack-trace { background-color: #f1f1f1; padding: 10px; font-family: monospace; font-size: 12px; border-radius: 5px; overflow-x: auto; white-space: pre-wrap; }
                    .footer { margin-top: 20px; padding-top: 20px; border-top: 1px solid #eee; font-size: 12px; color: #666; }
                    .button { background-color: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; display: inline-block; margin-top: 15px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>🚨 System Error Alert</h2>
                        <p>A new error has been detected in the Spawn application</p>
                    </div>
                    
                    <div class="error-details">
                        <h3>Error Details</h3>
                        <p><strong>Message:</strong> %s</p>
                        <p><strong>Source:</strong> %s.%s() (Line %d)</p>
                        <p><strong>Level:</strong> %s</p>
                        <p><strong>Occurred At:</strong> %s</p>
                        %s
                    </div>
                    
                    <div class="stack-trace">
                        <h4>Stack Trace:</h4>
                        %s
                    </div>
                    
                    <a href="%s/admin" class="button">View in Admin Dashboard</a>
                    
                    <div class="footer">
                        <p>This is an automated message from the Spawn error monitoring system.</p>
                        <p>Error ID: %s</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            errorLog.getErrorMessage(),
            errorLog.getSourceClass(),
            errorLog.getSourceMethod(),
            errorLog.getLineNumber(),
            errorLog.getErrorLevel(),
            formatter.format(errorLog.getOccurredAt()),
            errorLog.getUserContext() != null ? "<p><strong>Context:</strong> " + errorLog.getUserContext() + "</p>" : "",
            errorLog.getStackTrace(),
            "https://spawn-app-front-end.vercel.app", // Replace with actual frontend URL
            errorLog.getId()
        );
    }
}
