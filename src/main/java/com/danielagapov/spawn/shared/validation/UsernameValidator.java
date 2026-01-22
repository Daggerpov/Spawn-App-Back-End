package com.danielagapov.spawn.shared.validation;

import com.danielagapov.spawn.shared.util.InputValidationUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator implementation for @ValidUsername annotation
 */
public class UsernameValidator implements ConstraintValidator<ValidUsername, String> {
    
    private boolean optional;
    
    @Override
    public void initialize(ValidUsername constraintAnnotation) {
        this.optional = constraintAnnotation.optional();
    }
    
    @Override
    public boolean isValid(String username, ConstraintValidatorContext context) {
        // If optional and null/empty, it's valid
        if (optional && (username == null || username.trim().isEmpty())) {
            return true;
        }
        
        // If not optional and null/empty, it's invalid
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        
        // Use existing validation utility
        return InputValidationUtil.isValidUsername(username);
    }
}

