package com.danielagapov.spawn.shared.validation;

import com.danielagapov.spawn.shared.util.InputValidationUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator implementation for @ValidPhoneNumber annotation
 */
public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {
    
    private boolean optional;
    
    @Override
    public void initialize(ValidPhoneNumber constraintAnnotation) {
        this.optional = constraintAnnotation.optional();
    }
    
    @Override
    public boolean isValid(String phoneNumber, ConstraintValidatorContext context) {
        // If optional and null/empty, it's valid
        if (optional && (phoneNumber == null || phoneNumber.trim().isEmpty())) {
            return true;
        }
        
        // If not optional and null/empty, it's invalid
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        
        // Use existing validation utility
        return InputValidationUtil.isValidPhoneNumber(phoneNumber);
    }
}

