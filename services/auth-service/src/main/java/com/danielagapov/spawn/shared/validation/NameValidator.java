package com.danielagapov.spawn.shared.validation;

import com.danielagapov.spawn.shared.util.InputValidationUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator implementation for @ValidName annotation
 */
public class NameValidator implements ConstraintValidator<ValidName, String> {
    
    private boolean optional;
    
    @Override
    public void initialize(ValidName constraintAnnotation) {
        this.optional = constraintAnnotation.optional();
    }
    
    @Override
    public boolean isValid(String name, ConstraintValidatorContext context) {
        // If optional and null/empty, it's valid
        if (optional && (name == null || name.trim().isEmpty())) {
            return true;
        }
        
        // If not optional and null/empty, it's invalid
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        // Use existing validation utility
        return InputValidationUtil.isValidName(name);
    }
}

