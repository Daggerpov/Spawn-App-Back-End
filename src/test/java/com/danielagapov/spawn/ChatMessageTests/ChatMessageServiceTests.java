package com.danielagapov.spawn.ChatMessageTests;

import com.danielagapov.spawn.DTOs.ChatMessageDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Models.ChatMessage;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IChatMessageLikesRepository;
import com.danielagapov.spawn.Repositories.IChatMessageRepository;
import com.danielagapov.spawn.Repositories.IEventRepository;
import com.danielagapov.spawn.Repositories.IUserRepository;
import com.danielagapov.spawn.Services.ChatMessage.ChatMessageService;
import com.danielagapov.spawn.Services.FriendTag.IFriendTagService;
import com.danielagapov.spawn.Services.User.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ChatMessageServiceTests {

    @Mock
    private IChatMessageRepository chatMessageRepository;

    @Mock
    private IUserService userService;

    @Mock
    private IEventRepository eventRepository;

    @Mock
    private IFriendTagService ftService;

    @Mock
    private IUserRepository userRepository;

    @Mock
    private IChatMessageLikesRepository chatMessageLikesRepository;

    @InjectMocks
    private ChatMessageService chatMessageService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this); // Initialize mocks
    }

    // Get All Chat Messages Tests
    @Test
    void getAllChatMessages_ShouldReturnList_WhenMessagesExist() {
        UUID userId = UUID.randomUUID();
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setUserSender(new User(userId, "John", "Doe"));

        UserDTO userDTO = new UserDTO(userId, "John", "Doe");

        when(chatMessageRepository.findAll()).thenReturn(List.of(chatMessage));
        when(userService.getUserById(userId)).thenReturn(userDTO);
        when(chatMessageLikesRepository.findByChatMessage(chatMessage)).thenReturn(List.of());

        List<ChatMessageDTO> result = chatMessageService.getAllChatMessages();

        assertFalse(result.isEmpty());
        assertEquals("John", result.get(0).userSender().firstName());
        verify(chatMessageRepository, times(1)).findAll();
    }

    @Test
    void getAllChatMessages_ShouldThrowException_WhenDatabaseErrorOccurs() {
        when(chatMessageRepository.findAll()).thenThrow(new RuntimeException("Database error"));

        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class,
                () -> chatMessageService.getAllChatMessages());

        assertTrue(exception.getMessage().contains("Error fetching chat messages"));
        verify(chatMessageRepository, times(1)).findAll();
    }

    // Get Chat Message By ID Tests
    @Test
    void getChatMessageById_ShouldReturnChatMessage_WhenMessageExists() {
        UUID chatMessageId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setId(chatMessageId);
        chatMessage.setUserSender(new User(userId, "Jane", "Doe"));

        UserDTO userDTO = new UserDTO(userId, "Jane", "Doe");

        when(chatMessageRepository.findById(chatMessageId)).thenReturn(Optional.of(chatMessage));
        when(userService.getUserById(userId)).thenReturn(userDTO);
        when(chatMessageLikesRepository.findByChatMessage(chatMessage)).thenReturn(List.of());

        ChatMessageDTO result = chatMessageService.getChatMessageById(chatMessageId);

        assertEquals("Jane", result.userSender().firstName());
        verify(chatMessageRepository, times(1)).findById(chatMessageId);
    }

    @Test
    void getChatMessageById_ShouldThrowException_WhenMessageNotFound() {
        UUID chatMessageId = UUID.randomUUID();
        when(chatMessageRepository.findById(chatMessageId)).thenReturn(Optional.empty());

        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class,
                () -> chatMessageService.getChatMessageById(chatMessageId));

        assertEquals("Entity not found with ID: " + chatMessageId, exception.getMessage());
        verify(chatMessageRepository, times(1)).findById(chatMessageId);
    }

    // Save Chat Message Tests
    @Test
    void saveChatMessage_ShouldSaveMessage_WhenValidData() {
        UUID userId = UUID.randomUUID();
        ChatMessageDTO chatMessageDTO = new ChatMessageDTO("Hello!", new UserDTO(userId, "Jane", "Doe"));

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setUserSender(new User(userId, "Jane", "Doe"));
        chatMessage.setMessage("Hello!");

        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(chatMessage);

        assertDoesNotThrow(() -> chatMessageService.saveChatMessage(chatMessageDTO));

        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
    }

    @Test
    void saveChatMessage_ShouldThrowException_WhenDatabaseErrorOccurs() {
        UUID userId = UUID.randomUUID();
        ChatMessageDTO chatMessageDTO = new ChatMessageDTO("Hello!", new UserDTO(userId, "Jane", "Doe"));

        when(chatMessageRepository.save(any(ChatMessage.class))).thenThrow(new RuntimeException("Database error"));

        BaseSaveException exception = assertThrows(BaseSaveException.class,
                () -> chatMessageService.saveChatMessage(chatMessageDTO));

        assertTrue(exception.getMessage().contains("Error saving chat message"));
        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
    }
}

