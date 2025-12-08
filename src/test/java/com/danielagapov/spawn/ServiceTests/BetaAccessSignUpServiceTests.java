package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.analytics.api.dto.BetaAccessSignUpDTO;
import com.danielagapov.spawn.shared.exceptions.BaseSaveException;
import com.danielagapov.spawn.shared.exceptions.BasesNotFoundException;
import com.danielagapov.spawn.shared.exceptions.ILogger;
import com.danielagapov.spawn.analytics.internal.domain.BetaAccessSignUp;
import com.danielagapov.spawn.analytics.internal.repositories.IBetaAccessSignUpRepository;
import com.danielagapov.spawn.analytics.internal.services.BetaAccessSignUpService;
import com.danielagapov.spawn.auth.internal.services.IEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataAccessException;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BetaAccessSignUpServiceTests {

    @Mock
    private IBetaAccessSignUpRepository repository;

    @Mock
    private ILogger logger;

    @Mock
    private IEmailService emailService;

    @InjectMocks
    private BetaAccessSignUpService service;

    private BetaAccessSignUpDTO testDTO;
    private BetaAccessSignUp testEntity;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        testDTO = new BetaAccessSignUpDTO();
        testDTO.setEmail("test@example.com");

        testEntity = new BetaAccessSignUp();
        testEntity.setEmail("test@example.com");
    }

    @Test
    public void testGetAllBetaAccessSignUpRecords_Success() {
        when(repository.findAll()).thenReturn(Collections.singletonList(testEntity));

        List<BetaAccessSignUpDTO> result = service.getAllBetaAccessSignUpRecords();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("test@example.com", result.get(0).getEmail());
    }

    @Test
    public void testGetAllBetaAccessSignUpRecords_DataAccessException() {
        DataAccessException dae = new DataAccessException("DB error") {
        };
        when(repository.findAll()).thenThrow(dae);

        assertThrows(BasesNotFoundException.class, () -> service.getAllBetaAccessSignUpRecords());
        verify(logger, times(1)).warn(dae.getMessage());
    }

    @Test
    public void testGetAllEmails_Success() {
        when(repository.findAll()).thenReturn(Collections.singletonList(testEntity));

        List<String> emails = service.getAllEmails();

        assertNotNull(emails);
        assertEquals(1, emails.size());
        assertEquals("test@example.com", emails.get(0));
    }

    @Test
    public void testGetAllEmails_Exception() {
        RuntimeException re = new RuntimeException("Unexpected error");
        when(repository.findAll()).thenThrow(re);

        assertThrows(RuntimeException.class, () -> service.getAllEmails());
        verify(logger, times(2)).error(re.getMessage());
    }

    @Test
    public void testSignUp_Success() {
        when(repository.save(any(BetaAccessSignUp.class))).thenReturn(testEntity);

        BetaAccessSignUpDTO result = service.signUp(testDTO);

        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    public void testSignUp_DataAccessException() {
        DataAccessException dae = new DataAccessException("Save failed") {
        };
        when(repository.save(any(BetaAccessSignUp.class))).thenThrow(dae);

        BaseSaveException exception = assertThrows(BaseSaveException.class, () -> service.signUp(testDTO));
        assertTrue(exception.getMessage().contains("Failed to save beta access sign up record"));
        verify(logger, times(1)).error(dae.getMessage());
    }

    @Test
    public void testSignUp_GenericException() {
        RuntimeException re = new RuntimeException("Unexpected save error");
        when(repository.save(any(BetaAccessSignUp.class))).thenThrow(re);

        assertThrows(RuntimeException.class, () -> service.signUp(testDTO));
        verify(logger, times(1)).error(re.getMessage());
    }
}
