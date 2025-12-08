package com.danielagapov.spawn.analytics.internal.services;

import com.danielagapov.spawn.analytics.api.dto.BetaAccessSignUpDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing beta access sign-up operations.
 * Handles user registrations for beta access, email notifications, and administrative functions.
 */
public interface IBetaAccessSignUpService {
    /**
     * This is meant for saving a record to the database upon signing up through our site
     * 
     * @param dto constructed in the front-end of our site via a form input
     * @return back the dto after persisting to database, if successful. Throws otherwise.
     * @throws IllegalArgumentException if email already exists in the system
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseSaveException if saving to database fails
     */
    BetaAccessSignUpDTO signUp(BetaAccessSignUpDTO dto);
    
    /**
     * This would likely be used for internal use, just to check who's signed up
     * 
     * @return all beta access sign up record DTOs
     * @throws com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException if database access fails
     */
    List<BetaAccessSignUpDTO> getAllBetaAccessSignUpRecords();
    
    /**
     * Retrieves all email addresses from beta access sign up records
     * 
     * @return Strings of all emails of beta access sign up records
     * @throws RuntimeException if there's an error retrieving the records
     */
    List<String> getAllEmails();
    
    /**
     * Update the hasBeenEmailed flag for a beta access sign up
     * 
     * @param id the unique identifier of the beta access sign up record
     * @param hasBeenEmailed the new value for the hasBeenEmailed flag
     * @return the updated beta access sign up DTO
     * @throws com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException if record with given ID is not found
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseSaveException if updating the database fails
     */
    BetaAccessSignUpDTO updateEmailedStatus(UUID id, Boolean hasBeenEmailed);
}