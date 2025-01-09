package com.danielagapov.spawn.ChatMessageTests;

import com.danielagapov.spawn.DTOs.ChatMessageDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Models.ChatMessage;
import com.danielagapov.spawn.Models.Event;
import com.danielagapov.spawn.Models.Location;
import com.danielagapov.spawn.Models.User;
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
import java.time.OffsetDateTime;
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
    private IChatMessageLikesRepository chatMessageLikesRepository;

    @InjectMocks
    private ChatMessageService chatMessageService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this); // Initialize mocks
    }

    @Test
    void getAllChatMessages_ShouldReturnList_WhenMessagesExist() {
        // Create User entity with complete details
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "john_doe", "profile.jpg", "John", "Doe", "A bio", "john.doe@example.com");

        // Create UserDTO with the necessary fields (some fields like friends and friendTags might be left empty in the test)
        UserDTO userDTO = new UserDTO(
                userId,
                List.of(), // No friends in this test
                "john_doe",
                "profile.jpg",
                "John",
                "Doe",
                "A bio",
                List.of(), // No friend tags in this test
                "john.doe@example.com"
        );

        // Create Event entity with required fields
        Location location = new Location(UUID.randomUUID(), "Test Location", 0.0, 0.0);
        Event event = new Event(UUID.randomUUID(), "Test Event", OffsetDateTime.now(), OffsetDateTime.now().plusHours(1), location, "Test note", user);

        // Create a ChatMessage and associate it with the user and event
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setUserSender(user);
        chatMessage.setContent("Test message");
        chatMessage.setTimestamp(Instant.now());
        chatMessage.setEvent(event); // Set the event

        // Mock repository and service interactions
        when(chatMessageRepository.findAll()).thenReturn(List.of(chatMessage));
        when(userService.getUserById(userId)).thenReturn(userDTO);
        when(chatMessageLikesRepository.findByChatMessage(chatMessage)).thenReturn(List.of());

        // Call the service method to get chat messages
        List<ChatMessageDTO> result = chatMessageService.getAllChatMessages();

        // Assert that the result is not empty and contains the correct user data
        assertFalse(result.isEmpty());
        assertEquals("John", result.get(0).userSender().firstName());

        // Verify interactions with the repository
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

    @Test
    void getChatMessageById_ShouldReturnChatMessage_WhenMessageExists() {
        // Create User entity with complete details
        UUID userId = UUID.randomUUID();
        User user = new User(
                userId,
                "janedoe", // username
                "profile.jpg", // profile picture
                "Jane", // first name
                "Doe", // last name
                "A bio", // bio
                "jane.doe@example.com" // email
        );

        // Create UserDTO with necessary fields (friends and friendTags can be empty)
        UserDTO userDTO = new UserDTO(
                userId,
                List.of(), // No friends in this test
                "janedoe",
                "profile.jpg",
                "Jane",
                "Doe",
                "A bio",
                List.of(), // No friend tags in this test
                "jane.doe@example.com"
        );

        // Create ChatMessage and associate it with the user
        UUID chatMessageId = UUID.randomUUID();
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setId(chatMessageId);
        chatMessage.setUserSender(user); // Set the user as the sender of the message
        chatMessage.setContent("Test message");
        chatMessage.setTimestamp(Instant.now());

        // Mock repository and service interactions
        when(chatMessageRepository.findById(chatMessageId)).thenReturn(Optional.of(chatMessage));
        when(userService.getUserById(userId)).thenReturn(userDTO);
        when(chatMessageLikesRepository.findByChatMessage(chatMessage)).thenReturn(List.of());

        // Call the service method to get the chat message by ID
        ChatMessageDTO result = chatMessageService.getChatMessageById(chatMessageId);

        // Assert that the result is correct and contains the expected user data
        assertEquals("Jane", result.userSender().firstName());

        // Verify the repository interaction
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

    @Test
    void saveChatMessage_ShouldSaveMessage_WhenValidData() {
        UUID userId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();


        // Create Friends for UserDTO
        UserDTO friend = new UserDTO(
                friendId,
                List.of(),
                "JohnDoe",
                "profile2.jpg",
                "John",
                "Doe",
                "Loves programming",
                List.of(),
                "john.doe@example.com"
        );

        // Create UserDTO
        UserDTO userDTO = new UserDTO(
                userId,
                List.of(friend),          // Friends
                "JaneDoeski",
                "profile.jpg",
                "Jane",
                "Doe",
                "A bio",
                List.of(),
                "jane.doe@example.com"
        );

        // Create Location and User for the Event
        Location location = new Location(locationId, "Park", 40.7128, -74.0060);
        User creator = new User(userId, "JaneDoeski", "profile.jpg", "Jane", "Doe", "A bio", "jane.doe@example.com");

        // Create Event with required fields
        Event event = new Event(
                eventId,                        // ID
                "Birthday Party",               // Title
                OffsetDateTime.now(),           // Start Time
                OffsetDateTime.now().plusHours(2), // End Time
                location,                       // Location
                "Bring your own snacks!",       // Note
                creator                         // Creator
        );

        // Create ChatMessage with required fields
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setUserSender(creator);
        chatMessage.setContent("Hello!");
        chatMessage.setEvent(event);

        // Create ChatMessageDTO
        ChatMessageDTO chatMessageDTO = new ChatMessageDTO(
                null,                              // ID (null for new message)
                "Hello!",                          // Content
                null,                              // Timestamp
                userDTO,                           // UserSender
                eventId,                           // Event ID
                List.of()                          // LikedBy (empty list for this test)
        );

        // Mock repository behavior
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(chatMessage);

        // Perform the test
        assertDoesNotThrow(() -> chatMessageService.saveChatMessage(chatMessageDTO));

        // Verify repository interaction
        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
    }

    @Test
    void saveChatMessage_ShouldThrowException_WhenDatabaseErrorOccurs() {
        // Create a valid user ID
        UUID userId = UUID.randomUUID();

        // Create a UserDTO with all necessary fields, including empty lists for friends and friendTags
        UserDTO userDTO = new UserDTO(
                userId,
                List.of(), // Empty list for friends
                "janedoe", // username
                "profile.jpg", // profile picture
                "Jane", // first name
                "Doe", // last name
                "A bio", // bio
                List.of(), // Empty list for friendTags
                "jane.doe@example.com" // email
        );

        // Create a valid event ID (UUID) for the event
        UUID eventId = UUID.randomUUID();

        // Create a list for likedBy (can be empty or contain UserDTOs)
        List<UserDTO> likedBy = List.of(); // Empty list for this case

        // Create a ChatMessageDTO with all necessary fields
        ChatMessageDTO chatMessageDTO = new ChatMessageDTO(
                UUID.randomUUID(), // generate a random ID for the chat message
                "Hello!", // message content
                Instant.now(), // timestamp
                userDTO, // userSender
                eventId, // eventId
                likedBy // likedBy
        );

        // Mock the save method to throw an exception
        when(chatMessageRepository.save(any(ChatMessage.class))).thenThrow(new RuntimeException("Database error"));

        // Assert that the exception is thrown when trying to save the chat message
        BaseSaveException exception = assertThrows(BaseSaveException.class,
                () -> chatMessageService.saveChatMessage(chatMessageDTO));

        // Verify that the exception message contains the expected text
        assertTrue(exception.getMessage().contains("Error saving chat message"));

        // Verify the save method was called once
        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
    }


}

