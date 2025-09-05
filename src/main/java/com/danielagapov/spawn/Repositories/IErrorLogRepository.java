package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Enums.ResolutionStatus;
import com.danielagapov.spawn.Models.ErrorLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface IErrorLogRepository extends JpaRepository<ErrorLog, UUID> {
    
    /**
     * Find all error logs by status
     */
    List<ErrorLog> findByStatusOrderByOccurredAtDesc(ResolutionStatus status);
    
    /**
     * Find all error logs with pagination, ordered by occurrence time
     */
    Page<ErrorLog> findAllByOrderByOccurredAtDesc(Pageable pageable);
    
    /**
     * Find error logs by status with pagination
     */
    Page<ErrorLog> findByStatusOrderByOccurredAtDesc(ResolutionStatus status, Pageable pageable);
    
    /**
     * Find error logs by error level
     */
    List<ErrorLog> findByErrorLevelOrderByOccurredAtDesc(String errorLevel);
    
    /**
     * Find error logs that occurred within a time range
     */
    @Query("SELECT e FROM ErrorLog e WHERE e.occurredAt BETWEEN :startTime AND :endTime ORDER BY e.occurredAt DESC")
    List<ErrorLog> findByOccurredAtBetween(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
    
    /**
     * Count unresolved error logs
     */
    long countByStatus(ResolutionStatus status);
    
    /**
     * Find error logs that haven't had emails sent yet
     */
    List<ErrorLog> findByEmailSentFalseOrderByOccurredAtAsc();
    
    /**
     * Find recent error logs (within last 24 hours) for duplicate detection
     */
    @Query("SELECT e FROM ErrorLog e WHERE e.occurredAt >= :since AND e.sourceClass = :sourceClass AND e.sourceMethod = :sourceMethod AND e.lineNumber = :lineNumber")
    List<ErrorLog> findRecentSimilarErrors(@Param("since") Instant since, 
                                         @Param("sourceClass") String sourceClass, 
                                         @Param("sourceMethod") String sourceMethod, 
                                         @Param("lineNumber") Integer lineNumber);
}

