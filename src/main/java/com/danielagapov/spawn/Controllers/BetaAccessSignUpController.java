package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.BetaAccessSignUpDTO;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Services.BetaAccessSignUp.IBetaAccessSignUpService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.Map;

@RestController()
@RequestMapping("api/v1/betaAccessSignUp")
public class BetaAccessSignUpController {
    private final IBetaAccessSignUpService service;
    private final ILogger logger;

    public BetaAccessSignUpController(IBetaAccessSignUpService service, ILogger logger) {
        this.service = service;
        this.logger = logger;
    }

    // full path: /api/v1/betaAccessSignUp/emails

    /**
     * Use case: when we'll want to send out emails for the beta access
     */
    @GetMapping("emails")
    public ResponseEntity<List<String>> getAllEmails() {
        try {
            return new ResponseEntity<>(service.getAllEmails(), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/betaAccessSignUp/records

    /**
     * @return returns all signed-up users, for our own internal reference,
     * without having to check the database
     */
    @GetMapping("records")
    public ResponseEntity<List<BetaAccessSignUpDTO>> getAllRecords() {
        try {
            return new ResponseEntity<>(service.getAllBetaAccessSignUpRecords(), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/betaAccessSignUp/{id}/emailed
    
    /**
     * Update the hasBeenEmailed flag for a beta access sign up
     * 
     * @param id The ID of the beta access sign up to update
     * @param requestBody Map containing the hasBeenEmailed flag
     * @return The updated beta access sign up
     */
    @PutMapping("{id}/emailed")
    public ResponseEntity<BetaAccessSignUpDTO> updateEmailedStatus(@PathVariable UUID id, @RequestBody Map<String, Boolean> requestBody) {
        try {
            Boolean hasBeenEmailed = requestBody.get("hasBeenEmailed");
            if (hasBeenEmailed == null) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            
            return new ResponseEntity<>(service.updateEmailedStatus(id, hasBeenEmailed), HttpStatus.OK);
        } catch (BaseNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error updating emailed status: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // full path: /api/v1/betaAccessSignUp

    /**
     * Sign-up endpoint for creating a new record
     *
     * @param dto constructed via front-end form submission
     * @return back the DTO after being persisted into our database if successful.
     * Otherwise, we'll throw a 500 (INTERNAL_SERVER_ERROR).
     */
    @PostMapping
    public ResponseEntity<BetaAccessSignUpDTO> signUp(@RequestBody BetaAccessSignUpDTO dto) {
        logger.info("New beta access sign up for this email: " + dto.getEmail());
        try {
            return new ResponseEntity<>(service.signUp(dto), HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.CONFLICT); // 409 status code
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}