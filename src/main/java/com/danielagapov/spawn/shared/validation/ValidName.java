package com.danielagapov.spawn.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates that a name follows standard conventions:
 * - 1-100 characters long
 * - Only letters, spaces, hyphens, and apostrophes
 * - No dangerous content (SQL injection, XSS, etc.)
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NameValidator.class)
@Documented
public @interface ValidName {
    String message() default "Name must be 1-100 characters and contain only letters, spaces, hyphens, and apostrophes";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    boolean optional() default false;
}

