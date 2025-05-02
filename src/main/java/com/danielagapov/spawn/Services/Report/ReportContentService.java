package com.danielagapov.spawn.Services.Report;

import com.danielagapov.spawn.DTOs.ReportedContentDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.ReportType;
import com.danielagapov.spawn.Enums.ResolutionStatus;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Models.ReportedContent;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.IReportedContentRepository;
import com.danielagapov.spawn.Services.ChatMessage.IChatMessageService;
import com.danielagapov.spawn.Services.Event.IEventService;
import com.danielagapov.spawn.Services.User.IUserService;
import com.danielagapov.spawn.Exceptions.Logger.Logger;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static com.danielagapov.spawn.Enums.ResolutionStatus.PENDING;

@Service
@AllArgsConstructor
public class ReportContentService implements IReportContentService {
    private final IReportedContentRepository repository;
    private final IUserService userService;
    private final IEventService eventService;
    private final IChatMessageService chatMessageService;
    private final Logger logger;

    @Override
    public List<ReportedContentDTO> getReportsByFilters(ReportType reportType, EntityType contentType) {
        List<ReportedContent> reports;
        try {
            if (reportType != null && contentType != null) {
                // both filters
                logger.info("Getting reports by report type and content type");
                reports = repository.getAllByContentTypeAndReportType(contentType, reportType);
            } else if (reportType != null) {
                // only report type filter
                logger.info("Getting reports by report type");
                reports = repository.getAllByReportType(reportType);
            } else if (contentType != null) {
                // only content type filter
                logger.info("Getting reports by content type");
                reports = repository.getAllByContentType(contentType);
            } else {
                // no filter
                reports = repository.findAll();
            }
            return ReportedContentDTO.fromEntityList(reports);
        } catch (Exception e) {
            logger.error("Unexpected error while getting reports: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public ReportedContentDTO fileReport(ReportedContentDTO reportDTO) {
        ReportedContent report = reportDTO.toEntity();

        report.setTimeReported(OffsetDateTime.now());
        report.setResolution(PENDING);

        User contentOwner = findContentOwnerByContentId(reportDTO.getContentId(), reportDTO.getContentType());
        report.setContentOwner(contentOwner);

        report = repository.save(report);
        return ReportedContentDTO.fromEntity(report);
    }

    @Override
    public ReportedContentDTO resolveReport(UUID reportId, ResolutionStatus resolution) {
        try {
            ReportedContent report = repository.findById(reportId).
                    orElseThrow(() -> new BaseNotFoundException(EntityType.ReportedContent, reportId));
            report.setResolution(resolution);
            report = repository.save(report);
            return ReportedContentDTO.fromEntity(report);
        } catch (Exception e) {
            if (!(e instanceof BaseNotFoundException)) {
                logger.error("Unexpected error while resolving report: " + e.getMessage());
            }
            throw e;
        }
    }

    @Override
    public List<ReportedContentDTO> getReportsByReporterId(UUID reporterId) {
        try {
            return repository.getAllByReporterId(reporterId)
                    .stream()
                    .map(ReportedContentDTO::fromEntity)
                    .toList();
        } catch (DataAccessException e) {
            logger.error(e.getMessage());
            throw new BasesNotFoundException(EntityType.ReportedContent);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    public List<ReportedContentDTO> getReportsByContentOwnerId(UUID contentOwnerId) {
        try {
            return repository.getAllByContentOwnerId(contentOwnerId)
                    .stream()
                    .map(ReportedContentDTO::fromEntity)
                    .toList();
        } catch (DataAccessException e) {
            logger.error(e.getMessage());
            throw new BasesNotFoundException(EntityType.ReportedContent);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    public void deleteReportById(UUID reportId) {
        try {
            if (repository.existsById(reportId)) {
                repository.deleteById(reportId);
            } else {
                throw new BaseNotFoundException(EntityType.ReportedContent, reportId);
            }
        } catch (Exception e) {
            if (!(e instanceof BaseNotFoundException)) {
                logger.error("Unexpected error while deleting report by id: " + e.getMessage());
            }
            throw e;
        }
    }

    @Override
    public ReportedContentDTO getReportById(UUID reportId) {
        try {
            ReportedContent report = repository.findById(reportId)
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.ReportedContent, reportId));
            return ReportedContentDTO.fromEntity(report);
        } catch (Exception e) {
            if (!(e instanceof BaseNotFoundException)) {
                logger.error("Unexpected error while deleting report by id: " + e.getMessage());
            }
            throw e;
        }
    }


    /* ------------------------------ HELPERS ------------------------------ */

    /**
     * Given the id of the content and contentType (which is one of a chat message, event, or user account), this method
     * returns the User entity that owns the content
     */
    private User findContentOwnerByContentId(UUID contentId, EntityType contentType) {
        return switch (contentType) {
            case User -> userService.getUserEntityById(contentId);
            case Event -> getEventOwnerByContentId(contentId);
            case ChatMessage -> getChatMessageOwnerByContentId(contentId);
            default -> throw new IllegalArgumentException("Unsupported content type: " + contentType);
        };
    }

    /**
     * This is a wrapper method to getting the owner (a user) of an event with the given id.
     * Made a wrapper method for improved readability in the caller method.
     */
    private User getEventOwnerByContentId(UUID eventId) {
        return userService.getUserEntityById(eventService.getEventById(eventId).getCreatorUserId());
    }

    /**
     * This is a wrapper method to getting the owner (a user) of a chat message with the given id.
     * Made a wrapper method for improved readability in the caller method.
     */
    private User getChatMessageOwnerByContentId(UUID chatMessageId) {
        return userService.getUserEntityById(chatMessageService.getChatMessageById(chatMessageId).getSenderUserId());
    }
}
