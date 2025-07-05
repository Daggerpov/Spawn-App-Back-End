package com.danielagapov.spawn.Services.Report;

import com.danielagapov.spawn.DTOs.CreateReportedContentDTO;
import com.danielagapov.spawn.DTOs.FetchReportedContentDTO;
import com.danielagapov.spawn.DTOs.ReportedContentDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.ReportType;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Models.ReportedContent;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.IReportedContentRepository;
import com.danielagapov.spawn.Services.ChatMessage.IChatMessageService;
import com.danielagapov.spawn.Services.Activity.IActivityService;
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
    private final IActivityService ActivityService;
    private final IChatMessageService chatMessageService;
    private final Logger logger;



    @Override
    public ReportedContentDTO fileReport(CreateReportedContentDTO createReportDTO) {
        ReportedContent report = new ReportedContent();
        
        report.setContentId(createReportDTO.getContentId());
        report.setContentType(createReportDTO.getContentType());
        report.setReportType(createReportDTO.getReportType());
        report.setDescription(createReportDTO.getDescription());
        report.setTimeReported(OffsetDateTime.now());
        report.setResolution(PENDING);

        // Set the reporter User entity
        User reporter = userService.getUserEntityById(createReportDTO.getReporterUserId());
        report.setReporter(reporter);

        // Set the content owner
        User contentOwner = findContentOwnerByContentId(createReportDTO.getContentId(), createReportDTO.getContentType());
        report.setContentOwner(contentOwner);

        ReportedContent savedReport = repository.save(report);
        return ReportedContentDTO.fromEntity(savedReport);
    }



    @Override
    public List<FetchReportedContentDTO> getFetchReportsByReporterId(UUID reporterId) {
        try {
            return repository.getAllByReporterId(reporterId)
                    .stream()
                    .map(FetchReportedContentDTO::fromEntity)
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
    public List<FetchReportedContentDTO> getFetchReportsByContentOwnerId(UUID contentOwnerId) {
        try {
            return repository.getAllByContentOwnerId(contentOwnerId)
                    .stream()
                    .map(FetchReportedContentDTO::fromEntity)
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
    public List<FetchReportedContentDTO> getFetchReportsByFilters(ReportType reportType, EntityType contentType) {
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
            return FetchReportedContentDTO.fromEntityList(reports);
        } catch (Exception e) {
            logger.error("Unexpected error while getting reports: " + e.getMessage());
            throw e;
        }
    }



    /* ------------------------------ HELPERS ------------------------------ */

    /**
     * Given the id of the content and contentType (which is one of a chat message, Activity, or user account), this method
     * returns the User entity that owns the content
     */
    private User findContentOwnerByContentId(UUID contentId, EntityType contentType) {
        return switch (contentType) {
            case User -> userService.getUserEntityById(contentId);
            case Activity -> getActivityOwnerByContentId(contentId);
            case ChatMessage -> getChatMessageOwnerByContentId(contentId);
            default -> throw new IllegalArgumentException("Unsupported content type: " + contentType);
        };
    }

    /**
     * This is a wrapper method to getting the owner (a user) of an activity with the given id.
     * Made a wrapper method for improved readability in the caller method.
     */
    private User getActivityOwnerByContentId(UUID activityId) {
        return userService.getUserEntityById(ActivityService.getActivityById(activityId).getCreatorUserId());
    }

    /**
     * This is a wrapper method to getting the owner (a user) of a chat message with the given id.
     * Made a wrapper method for improved readability in the caller method.
     */
    private User getChatMessageOwnerByContentId(UUID chatMessageId) {
        return userService.getUserEntityById(chatMessageService.getChatMessageById(chatMessageId).getSenderUserId());
    }
}
