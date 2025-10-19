package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.ErrorLog.CreateErrorLogDTO;
import com.danielagapov.spawn.DTOs.ErrorLog.ErrorLogDTO;
import com.danielagapov.spawn.Models.ErrorLog;
import org.springframework.stereotype.Component;

@Component
public class ErrorLogMapper {
    
    public ErrorLogDTO toDTO(ErrorLog errorLog) {
        if (errorLog == null) {
            return null;
        }
        
        return new ErrorLogDTO(
            errorLog.getId(),
            errorLog.getErrorMessage(),
            errorLog.getStackTrace(),
            errorLog.getSourceClass(),
            errorLog.getSourceMethod(),
            errorLog.getLineNumber(),
            errorLog.getErrorLevel(),
            errorLog.getOccurredAt(),
            errorLog.getResolvedAt(),
            errorLog.getStatus(),
            errorLog.getAdminComment(),
            errorLog.getEmailSent(),
            errorLog.getUserContext()
        );
    }
    
    public ErrorLog toEntity(CreateErrorLogDTO dto) {
        if (dto == null) {
            return null;
        }
        
        return new ErrorLog(
            dto.getErrorMessage(),
            dto.getStackTrace(),
            dto.getSourceClass(),
            dto.getSourceMethod(),
            dto.getLineNumber(),
            dto.getErrorLevel(),
            dto.getUserContext()
        );
    }
}

