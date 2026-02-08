package com.danielagapov.spawn.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates that a username follows standard conventions:
 * - 3-30 characters long
 * - Only alphanumeric characters, dots, underscores, and hyphens
 * - No spaces allowed
 * - No dangerous content (SQL injection, XSS, etc.)
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UsernameValidator.class)
@Documented
public @interface ValidUsername {
    String message() default "Username must be 3-30 characters and contain only letters, numbers, dots, underscores, and hyphens (no spaces)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    boolean optional() default false;
}

