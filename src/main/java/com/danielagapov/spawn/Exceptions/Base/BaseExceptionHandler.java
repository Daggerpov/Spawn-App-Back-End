package com.danielagapov.spawn.Exceptions.Base;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class BaseExceptionHandler {

    @ExceptionHandler(BaseNotFoundException.class)
    public ResponseEntity<String> handleBaseNotFoundException(BaseNotFoundException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BasesNotFoundException.class)
    public ResponseEntity<String> handleBasesNotFoundException(BasesNotFoundException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BaseSaveException.class)
    public ResponseEntity<String> handleBaseSaveException(BaseSaveException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

