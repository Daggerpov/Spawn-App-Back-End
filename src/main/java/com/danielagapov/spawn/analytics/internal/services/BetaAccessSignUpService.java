package com.danielagapov.spawn.analytics.internal.services;

import com.danielagapov.spawn.analytics.api.dto.BetaAccessSignUpDTO;
import com.danielagapov.spawn.shared.util.EntityType;
import com.danielagapov.spawn.shared.exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.shared.exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.shared.util.BetaAccessSignUpMapper;
import com.danielagapov.spawn.analytics.internal.domain.BetaAccessSignUp;
import com.danielagapov.spawn.analytics.internal.repositories.IBetaAccessSignUpRepository;
import com.danielagapov.spawn.auth.internal.services.IEmailService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Arrays;

@Service
public class BetaAccessSignUpService implements IBetaAccessSignUpService {
    private final IBetaAccessSignUpRepository repository;
    private final ILogger logger;
    private final IEmailService emailService;
    private final List<String> notificationEmails = Arrays.asList(
            "spawnappmarketing@gmail.com",
            "danielagapov1@gmail.com",
            "shane.mander31@gmail.com",
            "danieluhlee@gmail.com"
    );

    @Autowired
    public BetaAccessSignUpService(IBetaAccessSignUpRepository repository, ILogger logger, IEmailService emailService) {
        this.repository = repository;
        this.logger = logger;
        this.emailService = emailService;
    }

    /**
     * This would likely be used for internal use, just to check who's signed up
     *
     * @return all beta access sign up record DTOs
     */
    @Override
    @Cacheable(value = "betaAccessRecords")
    public List<BetaAccessSignUpDTO> getAllBetaAccessSignUpRecords() {
        try {
            return BetaAccessSignUpMapper.toDTOList(repository.findAll());
        } catch (DataAccessException e) {
            logger.warn(e.getMessage());
            throw new BasesNotFoundException(EntityType.BetaAccessSignUp);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    /**
     * @return Strings of all emails of beta access sign up records
     */
    @Override
    public List<String> getAllEmails() {
        try {
            return getAllBetaAccessSignUpRecords().stream()
                    .map(BetaAccessSignUpDTO::getEmail)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    /**
     * This is meant for saving a record to the database upon signing up through our site
     *
     * @param dto constructed in the front-end of our site via a form input
     * @return back the dto after persisting to database, if successful. Throws otherwise.
     */
    @Override
    public BetaAccessSignUpDTO signUp(BetaAccessSignUpDTO dto) {
        try {
            // First check if email already exists
            if (repository.existsByEmail(dto.getEmail())) {
                throw new IllegalArgumentException("Email already registered");
            }

            BetaAccessSignUp entity = BetaAccessSignUpMapper.toEntity(dto);
            entity = repository.save(entity);
            
            // Send email notifications to the team members
            notifyTeamAboutNewSignUp(dto.getEmail());
            
            return BetaAccessSignUpMapper.toDTO(entity);
        } catch (DataAccessException e) {
            logger.error(e.getMessage());
            throw new BaseSaveException("Failed to save beta access sign up record: " + e.getMessage());
        } catch (Exception e) { // also catches IllegalArgumentException for duplicate emails
            logger.error(e.getMessage());
            throw e;
        }
    }
    
    /**
     * Send email notifications to team members about a new beta access sign up
     * 
     * @param signupEmail The email that just signed up for beta access
     */
    private void notifyTeamAboutNewSignUp(String signupEmail) {
        String subject = "New Beta Access Sign Up";
        String content = "<h1>New Beta Access Sign Up</h1>" +
                         "<p>A new user has signed up for beta access with the email: <strong>" + signupEmail + "</strong></p>" +
                         "<p>Check out the admin dashboard for more information: <a href='https://getspawn.com/admin/dashboard'>Admin Dashboard</a></p>";
        
        for (String recipient : notificationEmails) {
            try {
                emailService.sendEmail(recipient, subject, content);
            } catch (MessagingException e) {
                logger.error("Failed to send notification email to " + recipient + ": " + e.getMessage());
                // Continue with other emails even if one fails
            }
        }
    }
    
    /**
     * Update the hasBeenEmailed flag for a beta access sign up
     *
     * @param id ID of the beta access sign up
     * @param hasBeenEmailed The new value for hasBeenEmailed
     * @return The updated beta access sign up DTO
     */
    @Override
    @CacheEvict(value = "betaAccessRecords", allEntries = true)
    public BetaAccessSignUpDTO updateEmailedStatus(UUID id, Boolean hasBeenEmailed) {
        try {
            BetaAccessSignUp entity = repository.findById(id)
                .orElseThrow(() -> new BasesNotFoundException(EntityType.BetaAccessSignUp));
            
            entity.setHasBeenEmailed(hasBeenEmailed);
            entity = repository.save(entity);
            
            return BetaAccessSignUpMapper.toDTO(entity);
        } catch (DataAccessException e) {
            logger.error(e.getMessage());
            throw new BaseSaveException("Failed to update beta access sign up emailed status: " + e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }
}
