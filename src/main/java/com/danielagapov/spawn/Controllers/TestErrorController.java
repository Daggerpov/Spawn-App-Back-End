package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Test controller to demonstrate error logging functionality
 * Remove this in production
 */
@RestController
@RequestMapping("/api/v1/test")
@AllArgsConstructor
@CrossOrigin(origins = "*")
public class TestErrorController {
    
    private final ILogger logger;
    
    /**
     * Test endpoint to trigger error logging
     */
    @GetMapping("/trigger-error")
    public String triggerError(@RequestParam(defaultValue = "Test error message") String message) {
        try {
            // Simulate an error
            throw new RuntimeException(message);
        } catch (Exception e) {
            logger.error("Test error triggered via API", e);
            return "Error logged successfully. Check admin panel for details.";
        }
    }
    
    /**
     * Test endpoint to trigger simple error logging
     */
    @GetMapping("/trigger-simple-error")
    public String triggerSimpleError(@RequestParam(defaultValue = "Simple test error") String message) {
        logger.error(message);
        return "Simple error logged successfully. Check admin panel for details.";
    }
}

