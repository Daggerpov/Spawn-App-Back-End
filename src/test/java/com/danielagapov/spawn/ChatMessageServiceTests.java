package com.danielagapov.spawn;

import com.danielagapov.spawn.DTOs.ChatMessageDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Models.ChatMessage;
import com.danielagapov.spawn.Repositories.IChatMessageLikesRepository;
import com.danielagapov.spawn.Repositories.IChatMessageRepository;
import com.danielagapov.spawn.Services.ChatMessage.ChatMessageService;
import com.danielagapov.spawn.Services.User.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ChatMessageServiceTests {

    @Mock
    private IChatMessageRepository chatMessageRepository;

    @Mock
    private IUserService userService;

    @Mock
    private IChatMessageLikesRepository chatMessageLikesRepository;

    @InjectMocks
    private ChatMessageService chatMessageService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this); // Initialize mocks
    }

    @Test
    void deleteChatMessage_ShouldDeleteMessage_WhenMessageExists() {
        // Arrange
        UUID chatMessageId = UUID.randomUUID();

        // Mock repository behavior
        when(chatMessageRepository.existsById(chatMessageId)).thenReturn(true);
        doNothing().when(chatMessageRepository).deleteById(chatMessageId);

        // Act and Assert
        assertDoesNotThrow(() -> chatMessageService.deleteChatMessageById(chatMessageId));

        // Verify repository interactions
        verify(chatMessageRepository, times(1)).existsById(chatMessageId);
        verify(chatMessageRepository, times(1)).deleteById(chatMessageId);
    }

    @Test
    void deleteChatMessage_ShouldThrowException_WhenMessageDoesNotExist() {
        // Arrange
        UUID chatMessageId = UUID.randomUUID();

        // Mock repository behavior
        when(chatMessageRepository.existsById(chatMessageId)).thenReturn(false);

        // Act and Assert
        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class,
                () -> chatMessageService.deleteChatMessageById(chatMessageId));

        assertEquals("Chat message not found with ID: " + chatMessageId, exception.getMessage());

        // Verify repository interactions
        verify(chatMessageRepository, times(1)).existsById(chatMessageId);
        verify(chatMessageRepository, never()).deleteById(chatMessageId);
    }

    @Test
    void saveChatMessage_ShouldThrowException_WhenEventNotFound() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        UserDTO userDTO = new UserDTO(
                userId,
                List.of(),
                "johndoe",
                "profile.jpg",
                "John",
                "Doe",
                "A bio",
                List.of(),
                "john.doe@example.com"
        );

        ChatMessageDTO chatMessageDTO = new ChatMessageDTO(
                UUID.randomUUID(),
                "Hello!",
                Instant.now(),
                userDTO.id(),
                eventId,
                List.of()
        );

        // Mock repository behavior
        when(chatMessageRepository.save(any(ChatMessage.class))).thenThrow(new RuntimeException("Event not found"));

        // Act and Assert
        BaseSaveException exception = assertThrows(BaseSaveException.class,
                () -> chatMessageService.saveChatMessage(chatMessageDTO));

        assertTrue(exception.getMessage().contains("Error saving chat message"));

        // Verify repository interactions
        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
    }

}

