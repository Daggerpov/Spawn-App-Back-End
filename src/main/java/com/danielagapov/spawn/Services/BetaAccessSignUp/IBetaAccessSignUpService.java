package com.danielagapov.spawn.Services.BetaAccessSignUp;

import com.danielagapov.spawn.DTOs.BetaAccessSignUpDTO;

import java.util.List;
import java.util.UUID;

public interface IBetaAccessSignUpService {
    /**
     * This is meant for saving a record to the database upon signing up through our site
     * @param dto constructed in the front-end of our site via a form input
     * @return back the dto after persisting to database, if successful. Throws otherwise.
     */
    BetaAccessSignUpDTO signUp(BetaAccessSignUpDTO dto);
    /**
     * This would likely be used for internal use, just to check who's signed up
     * @return all beta access sign up record DTOs
     */
    List<BetaAccessSignUpDTO> getAllBetaAccessSignUpRecords();
    /**
     * @return Strings of all emails of beta access sign up records
     */
    List<String> getAllEmails();
    /**
     * Update the hasBeenEmailed flag for a beta access sign up
     */
    BetaAccessSignUpDTO updateEmailedStatus(UUID id, Boolean hasBeenEmailed);
}