package com.danielagapov.spawn.Services.Report;

import com.danielagapov.spawn.DTOs.ReportedContentDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.ReportType;
import com.danielagapov.spawn.Enums.ResolutionStatus;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.Logger;
import com.danielagapov.spawn.Models.ReportedContent;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IReportedContentRepository;
import com.danielagapov.spawn.Services.ChatMessage.IChatMessageService;
import com.danielagapov.spawn.Services.Event.IEventService;
import com.danielagapov.spawn.Services.User.IUserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
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
                logger.log("Getting reports by report type and content type");
                reports = repository.getAllByContentTypeAndReportType(contentType, reportType);
            } else if (reportType != null) {
                // only report type filter
                logger.log("Getting reports by report type");
                reports = repository.getAllByReportType(reportType);
            } else if (contentType != null) {
                // only content type filter
                logger.log("Getting reports by content type");
                reports = repository.getAllByContentType(contentType);
            } else {
                // no filter
                reports = repository.findAll();
            }
            return ReportedContentDTO.fromEntityList(reports);
        } catch (Exception e) {
            logger.log("Unexpected error while getting reports: " + e.getMessage());
            throw e;
        }
    }

    @Override
    /*
     - create reported content
     - find reporting user
     - save report
     */
    public ReportedContentDTO fileReport(ReportedContentDTO reportDTO) {
        ReportedContent report = reportDTO.toEntity();

        report.setTimeReported(Instant.now());
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
                logger.log("Unexpected error while resolving report: " + e.getMessage());
            }
            throw e;
        }
    }

    @Override
    public List<ReportedContentDTO> getReportsByReporterId(UUID reporterId) {
        try {
            List<ReportedContent> reports = repository.getAllByReporterId(reporterId);
            return ReportedContentDTO.fromEntityList(reports);
        } catch (Exception e) {
            logger.log("Unexpected error while getting reports by reporter id: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public List<ReportedContentDTO> getReportsByReportedUserId(UUID reportedUserId) {
        try {
            List<ReportedContent> reports = repository.getAllByContentOwnerId(reportedUserId);
            return ReportedContentDTO.fromEntityList(reports);
        } catch (Exception e) {
            logger.log("Unexpected error while getting reports by reported user id: " + e.getMessage());
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
                logger.log("Unexpected error while deleting report by id: " + e.getMessage());
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
                logger.log("Unexpected error while deleting report by id: " + e.getMessage());
            }
            throw e;
        }
    }


    /* ------------------------------ HELPERS ------------------------------ */


    private User findContentOwnerByContentId(UUID contentId, EntityType contentType) {
        return switch (contentType) {
            case User -> userService.getUserEntityById(contentId);
            case Event -> getEventOwnerByContentId(contentId);
            case ChatMessage -> getChatMessageOwnerByContentId(contentId);
            default -> throw new IllegalArgumentException("Unsupported content type: " + contentType);
        };
    }

    private User getEventOwnerByContentId(UUID contentId) {
        return userService.getUserEntityById(eventService.getEventById(contentId).getCreatorUserId());
    }

    private User getChatMessageOwnerByContentId(UUID contentId) {
        return userService.getUserEntityById(chatMessageService.getChatMessageById(contentId).getSenderUserId());
    }
}
