package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.DTOs.ChatMessageDTO;
import com.danielagapov.spawn.DTOs.FullEventChatMessageDTO;
import com.danielagapov.spawn.DTOs.FullUserDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Base.BaseDeleteException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Models.ChatMessage;
import com.danielagapov.spawn.Models.ChatMessageLikes;
import com.danielagapov.spawn.Models.Event;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IChatMessageLikesRepository;
import com.danielagapov.spawn.Repositories.IChatMessageRepository;
import com.danielagapov.spawn.Repositories.IEventRepository;
import com.danielagapov.spawn.Repositories.IUserRepository;
import com.danielagapov.spawn.Services.ChatMessage.ChatMessageService;
import com.danielagapov.spawn.Services.Event.IEventService;
import com.danielagapov.spawn.Services.FriendTag.IFriendTagService;
import com.danielagapov.spawn.Services.User.IUserService;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataAccessException;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ChatMessageServiceTests {

    @Mock
    private IChatMessageRepository chatMessageRepository;

    @Mock
    private IUserService userService;

    @Mock
    private IChatMessageLikesRepository chatMessageLikesRepository;

    @Mock
    private IEventRepository eventRepository;

    @Mock
    private IEventService eventService;

    @Mock
    private IFriendTagService ftService;

    @Mock
    private IUserRepository userRepository;

    @Mock
    private ILogger logger;

    @InjectMocks
    private ChatMessageService chatMessageService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    // Helper method to create a dummy Event with a random ID.
    private Event createDummyEvent() {
        Event event = new Event();
        event.setId(UUID.randomUUID());
        return event;
    }

    // Helper method to create a dummy User with a given ID.
    private User createDummyUser(UUID id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    @Test
    void deleteChatMessage_ShouldDeleteMessage_WhenMessageExists() {
        UUID chatMessageId = UUID.randomUUID();
        when(chatMessageRepository.existsById(chatMessageId)).thenReturn(true);
        doNothing().when(chatMessageRepository).deleteById(chatMessageId);
        assertDoesNotThrow(() -> chatMessageService.deleteChatMessageById(chatMessageId));
        verify(chatMessageRepository, times(1)).existsById(chatMessageId);
        verify(chatMessageRepository, times(1)).deleteById(chatMessageId);
    }

    @Test
    void deleteChatMessage_ShouldThrowException_WhenMessageDoesNotExist() {
        UUID chatMessageId = UUID.randomUUID();
        when(chatMessageRepository.existsById(chatMessageId)).thenReturn(false);
        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class,
                () -> chatMessageService.deleteChatMessageById(chatMessageId));
        assertTrue(exception.getMessage().contains(chatMessageId.toString()));
        assertTrue(exception.getMessage().toLowerCase().contains("not found"));
        verify(chatMessageRepository, times(1)).existsById(chatMessageId);
        verify(chatMessageRepository, never()).deleteById(chatMessageId);
    }

    @Test
    void saveChatMessage_ShouldThrowException_WhenEventNotFound() {
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
        // Stub user exists but event is missing
        User dummyUser = createDummyUser(userDTO.id());
        when(userRepository.findById(userDTO.id())).thenReturn(Optional.of(dummyUser));
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());
        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class,
                () -> chatMessageService.saveChatMessage(chatMessageDTO));
        assertTrue(exception.getMessage().contains(eventId.toString()));
        assertTrue(exception.getMessage().toLowerCase().contains("not found"));
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    void getAllChatMessages_ShouldReturnChatMessages_WhenMessagesExist() {
        ChatMessage chatMessage1 = new ChatMessage();
        ChatMessage chatMessage2 = new ChatMessage();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        chatMessage1.setId(id1);
        chatMessage1.setContent("Message 1");
        chatMessage2.setId(id2);
        chatMessage2.setContent("Message 2");
        // Set a dummy sender on each
        User dummyUser = createDummyUser(UUID.randomUUID());
        chatMessage1.setUserSender(dummyUser);
        chatMessage2.setUserSender(dummyUser);
        // Also set a dummy event to avoid NPE in mapping
        Event dummyEvent = createDummyEvent();
        chatMessage1.setEvent(dummyEvent);
        chatMessage2.setEvent(dummyEvent);
        List<ChatMessage> messages = List.of(chatMessage1, chatMessage2);
        when(chatMessageRepository.findAll()).thenReturn(messages);
        when(chatMessageRepository.findById(id1)).thenReturn(Optional.of(chatMessage1));
        when(chatMessageRepository.findById(id2)).thenReturn(Optional.of(chatMessage2));
        when(chatMessageLikesRepository.findByChatMessage(chatMessage1)).thenReturn(new ArrayList<>());
        when(chatMessageLikesRepository.findByChatMessage(chatMessage2)).thenReturn(new ArrayList<>());
        List<ChatMessageDTO> result = chatMessageService.getAllChatMessages();
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(dto -> dto.id().equals(id1)));
        assertTrue(result.stream().anyMatch(dto -> dto.id().equals(id2)));
    }

    @Test
    void getAllChatMessages_ShouldThrowBasesNotFoundException_WhenDataAccessExceptionOccurs() {
        DataAccessException dae = new DataAccessException("DB error") {};
        when(chatMessageRepository.findAll()).thenThrow(dae);
        BasesNotFoundException ex = assertThrows(BasesNotFoundException.class, () -> chatMessageService.getAllChatMessages());
        verify(logger, times(1)).log(dae.getMessage());
    }

    @Test
    void getChatMessageLikeUserIds_ShouldReturnUserIds_WhenLikesExist() {
        UUID chatMessageId = UUID.randomUUID();
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setId(chatMessageId);
        // Set a dummy sender (even if not used in this method)
        chatMessage.setUserSender(createDummyUser(UUID.randomUUID()));
        // Set a dummy event to avoid NPE if mapper is used indirectly
        chatMessage.setEvent(createDummyEvent());
        when(chatMessageRepository.findById(chatMessageId)).thenReturn(Optional.of(chatMessage));
        ChatMessageLikes like1 = new ChatMessageLikes();
        ChatMessageLikes like2 = new ChatMessageLikes();
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        User user1 = createDummyUser(userId1);
        User user2 = createDummyUser(userId2);
        like1.setUser(user1);
        like2.setUser(user2);
        List<ChatMessageLikes> likes = List.of(like1, like2);
        when(chatMessageLikesRepository.findByChatMessage(chatMessage)).thenReturn(likes);
        List<UUID> result = chatMessageService.getChatMessageLikeUserIds(chatMessageId);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(userId1));
        assertTrue(result.contains(userId2));
    }

    @Test
    void getChatMessageLikeUserIds_ShouldThrowBaseNotFoundException_WhenChatMessageNotFound() {
        UUID chatMessageId = UUID.randomUUID();
        when(chatMessageRepository.findById(chatMessageId)).thenReturn(Optional.empty());
        BaseNotFoundException ex = assertThrows(BaseNotFoundException.class, () -> chatMessageService.getChatMessageLikeUserIds(chatMessageId));
        assertTrue(ex.getMessage().contains(chatMessageId.toString()));
    }

    @Test
    void getChatMessageById_ShouldReturnChatMessageDTO_WhenMessageExists() {
        UUID chatMessageId = UUID.randomUUID();
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setId(chatMessageId);
        chatMessage.setContent("Test message");
        // Set dummy sender and event
        chatMessage.setUserSender(createDummyUser(UUID.randomUUID()));
        chatMessage.setEvent(createDummyEvent());
        when(chatMessageRepository.findById(chatMessageId)).thenReturn(Optional.of(chatMessage));
        when(chatMessageLikesRepository.findByChatMessage(chatMessage)).thenReturn(new ArrayList<>());
        ChatMessageDTO dto = chatMessageService.getChatMessageById(chatMessageId);
        assertNotNull(dto);
        assertEquals(chatMessageId, dto.id());
        assertEquals("Test message", dto.content());
    }

    @Test
    void getChatMessageById_ShouldThrowBaseNotFoundException_WhenMessageNotFound() {
        UUID chatMessageId = UUID.randomUUID();
        when(chatMessageRepository.findById(chatMessageId)).thenReturn(Optional.empty());
        BaseNotFoundException ex = assertThrows(BaseNotFoundException.class, () -> chatMessageService.getChatMessageById(chatMessageId));
        assertTrue(ex.getMessage().contains(chatMessageId.toString()));
    }

    @Test
    void getFullChatMessageById_ShouldReturnFullEventChatMessageDTO_WhenMessageExists() {
        UUID chatMessageId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Instant timestamp = Instant.now();
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setId(chatMessageId);
        chatMessage.setContent("Full message");
        chatMessage.setTimestamp(timestamp);
        // Set dummy sender with expected senderId and event with expected eventId
        chatMessage.setUserSender(createDummyUser(senderId));
        Event dummyEvent = new Event();
        dummyEvent.setId(eventId);
        chatMessage.setEvent(dummyEvent);
        when(chatMessageRepository.findById(chatMessageId)).thenReturn(Optional.of(chatMessage));
        when(chatMessageLikesRepository.findByChatMessage(chatMessage)).thenReturn(new ArrayList<>());
        FullUserDTO fullUser = new FullUserDTO(senderId, List.of(), "username", "avatar.jpg", "First", "Last", "bio", List.of(), "email@example.com");
        when(userService.getFullUserById(any(UUID.class))).thenReturn(fullUser);
        when(userService.convertUsersToFullUsers(any(), new HashSet<>())).thenReturn(new ArrayList<>());
        FullEventChatMessageDTO fullDto = chatMessageService.getFullChatMessageById(chatMessageId);
        assertNotNull(fullDto);
        assertEquals(chatMessageId, fullDto.id());
        assertEquals("Full message", fullDto.content());
        assertEquals(fullUser, fullDto.senderUser());
    }

    @Test
    void getFullChatMessagesByEventId_ShouldReturnListOfFullEventChatMessageDTOs() {
        UUID eventId = UUID.randomUUID();
        ChatMessage chatMessage1 = new ChatMessage();
        ChatMessage chatMessage2 = new ChatMessage();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        chatMessage1.setId(id1);
        chatMessage1.setContent("Message 1");
        chatMessage1.setTimestamp(Instant.now());
        chatMessage2.setId(id2);
        chatMessage2.setContent("Message 2");
        chatMessage2.setTimestamp(Instant.now());
        // Set dummy sender and event for both messages
        User dummyUser = createDummyUser(UUID.randomUUID());
        chatMessage1.setUserSender(dummyUser);
        chatMessage2.setUserSender(dummyUser);
        Event dummyEvent = createDummyEvent();
        chatMessage1.setEvent(dummyEvent);
        chatMessage2.setEvent(dummyEvent);
        List<ChatMessage> messages = List.of(chatMessage1, chatMessage2);
        when(chatMessageRepository.getChatMessagesByEventId(eventId)).thenReturn(messages);
        when(chatMessageRepository.findById(id1)).thenReturn(Optional.of(chatMessage1));
        when(chatMessageRepository.findById(id2)).thenReturn(Optional.of(chatMessage2));
        when(chatMessageLikesRepository.findByChatMessage(chatMessage1)).thenReturn(new ArrayList<>());
        when(chatMessageLikesRepository.findByChatMessage(chatMessage2)).thenReturn(new ArrayList<>());
        FullUserDTO fullUser = new FullUserDTO(UUID.randomUUID(), List.of(), "user", "avatar.jpg", "First", "Last", "bio", List.of(), "user@example.com");
        when(userService.getFullUserById(any(UUID.class))).thenReturn(fullUser);
        when(userService.convertUsersToFullUsers(any(), new HashSet<>())).thenReturn(new ArrayList<>());
        List<FullEventChatMessageDTO> result = chatMessageService.getFullChatMessagesByEventId(eventId);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(dto -> dto.id().equals(id1)));
        assertTrue(result.stream().anyMatch(dto -> dto.id().equals(id2)));
    }

    @Test
    void saveChatMessage_ShouldSaveMessage_WhenValid() {
        UUID userId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        ChatMessageDTO chatMessageDTO = new ChatMessageDTO(
                UUID.randomUUID(),
                "Saving message",
                Instant.now(),
                userId,
                eventId,
                List.of()
        );
        // Create dummy user and event so the save proceeds
        User dummyUser = createDummyUser(userId);
        Event dummyEvent = new Event();
        dummyEvent.setId(eventId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(dummyUser));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(dummyEvent));
        ChatMessage dummyChatMessage = new ChatMessage();
        dummyChatMessage.setId(chatMessageDTO.id());
        dummyChatMessage.setContent(chatMessageDTO.content());
        // Set sender and event on the saved entity
        dummyChatMessage.setUserSender(dummyUser);
        dummyChatMessage.setEvent(dummyEvent);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(dummyChatMessage);
        ChatMessageDTO savedDTO = chatMessageService.saveChatMessage(chatMessageDTO);
        assertNotNull(savedDTO);
        assertEquals(chatMessageDTO.id(), savedDTO.id());
        assertEquals("Saving message", savedDTO.content());
    }

    @Test
    void getChatMessageIdsByEventId_ShouldReturnIds_WhenEventExists() {
        UUID eventId = UUID.randomUUID();
        Event dummyEvent = new Event();
        dummyEvent.setId(eventId);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(dummyEvent));
        ChatMessage chatMessage1 = new ChatMessage();
        ChatMessage chatMessage2 = new ChatMessage();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        chatMessage1.setId(id1);
        chatMessage2.setId(id2);
        // Set a dummy sender and event on each message
        User dummyUser = createDummyUser(UUID.randomUUID());
        chatMessage1.setUserSender(dummyUser);
        chatMessage2.setUserSender(dummyUser);
        Event eventForMessages = createDummyEvent();
        chatMessage1.setEvent(eventForMessages);
        chatMessage2.setEvent(eventForMessages);
        List<ChatMessage> messages = List.of(chatMessage1, chatMessage2);
        when(chatMessageRepository.getChatMessagesByEventId(eventId)).thenReturn(messages);
        List<UUID> ids = chatMessageService.getChatMessageIdsByEventId(eventId);
        assertNotNull(ids);
        assertEquals(2, ids.size());
        assertTrue(ids.contains(id1));
        assertTrue(ids.contains(id2));
    }

    @Test
    void getChatMessageIdsByEventId_ShouldThrowBaseNotFoundException_WhenEventNotFound() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());
        BaseNotFoundException ex = assertThrows(BaseNotFoundException.class, () -> chatMessageService.getChatMessageIdsByEventId(eventId));
        assertTrue(ex.getMessage().contains(eventId.toString()));
    }

    @Test
    void createChatMessageLike_ShouldReturnChatMessageLikesDTO_WhenLikeIsCreated() {
        UUID chatMessageId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(chatMessageLikesRepository.existsByChatMessage_IdAndUser_Id(chatMessageId, userId)).thenReturn(false);
        ChatMessage dummyChatMessage = new ChatMessage();
        dummyChatMessage.setId(chatMessageId);
        // Set a dummy sender and event
        dummyChatMessage.setUserSender(createDummyUser(UUID.randomUUID()));
        dummyChatMessage.setEvent(createDummyEvent());
        when(chatMessageRepository.findById(chatMessageId)).thenReturn(Optional.of(dummyChatMessage));
        User dummyUser = createDummyUser(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(dummyUser));
        ChatMessageLikes dummyLike = new ChatMessageLikes();
        dummyLike.setChatMessage(dummyChatMessage);
        dummyLike.setUser(dummyUser);
        when(chatMessageLikesRepository.save(any(ChatMessageLikes.class))).thenReturn(dummyLike);
        var result = chatMessageService.createChatMessageLike(chatMessageId, userId);
        assertNotNull(result);
    }

    @Test
    void createChatMessageLike_ShouldThrowEntityAlreadyExistsException_WhenLikeAlreadyExists() {
        UUID chatMessageId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(chatMessageLikesRepository.existsByChatMessage_IdAndUser_Id(chatMessageId, userId)).thenReturn(true);
        // Due to the try-catch, the thrown exception is wrapped as BaseSaveException.
        BaseSaveException ex = assertThrows(BaseSaveException.class,
                () -> chatMessageService.createChatMessageLike(chatMessageId, userId));
        assertTrue(ex.getMessage().contains(chatMessageId.toString()));
    }

    @Test
    void getChatMessageLikes_ShouldReturnUserDTOs_WhenLikesExist() {
        UUID chatMessageId = UUID.randomUUID();
        ChatMessage dummyChatMessage = new ChatMessage();
        dummyChatMessage.setId(chatMessageId);
        // Set a dummy sender and event on the chat message
        dummyChatMessage.setUserSender(createDummyUser(UUID.randomUUID()));
        dummyChatMessage.setEvent(createDummyEvent());
        when(chatMessageRepository.findById(chatMessageId)).thenReturn(Optional.of(dummyChatMessage));
        ChatMessageLikes dummyLike = new ChatMessageLikes();
        User dummyUser = createDummyUser(UUID.randomUUID());
        dummyLike.setUser(dummyUser);
        List<ChatMessageLikes> likes = List.of(dummyLike);
        when(chatMessageLikesRepository.findByChatMessage(dummyChatMessage)).thenReturn(likes);
        List<UUID> friendIds = List.of(UUID.randomUUID());
        List<UUID> friendTagIds = List.of(UUID.randomUUID());
        when(userService.getFriendUserIdsByUserId(dummyUser.getId())).thenReturn(friendIds);
        when(ftService.getFriendTagIdsByOwnerUserId(dummyUser.getId())).thenReturn(friendTagIds);
        List<UserDTO> result = chatMessageService.getChatMessageLikes(chatMessageId);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(dummyUser.getId(), result.get(0).id());
    }

    @Test
    void getChatMessageLikes_ShouldThrowBaseNotFoundException_WhenChatMessageNotFound() {
        UUID chatMessageId = UUID.randomUUID();
        when(chatMessageRepository.findById(chatMessageId)).thenReturn(Optional.empty());
        BaseNotFoundException ex = assertThrows(BaseNotFoundException.class, () -> chatMessageService.getChatMessageLikes(chatMessageId));
        assertTrue(ex.getMessage().contains(chatMessageId.toString()));
    }

    @Test
    void deleteChatMessageLike_ShouldDeleteLike_WhenLikeExists() {
        UUID chatMessageId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(chatMessageLikesRepository.existsByChatMessage_IdAndUser_Id(chatMessageId, userId)).thenReturn(true);
        assertDoesNotThrow(() -> chatMessageService.deleteChatMessageLike(chatMessageId, userId));
        verify(chatMessageLikesRepository, times(1)).deleteByChatMessage_IdAndUser_Id(chatMessageId, userId);
    }

    @Test
    void deleteChatMessageLike_ShouldThrowBasesNotFoundException_WhenLikeDoesNotExist() {
        UUID chatMessageId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(chatMessageLikesRepository.existsByChatMessage_IdAndUser_Id(chatMessageId, userId)).thenReturn(false);
        // The service wraps the not-found exception into a BaseDeleteException.
        BaseDeleteException ex = assertThrows(BaseDeleteException.class, () -> chatMessageService.deleteChatMessageLike(chatMessageId, userId));
        assertTrue(ex.getMessage().toLowerCase().contains("an error occurred while deleting"));
        verify(chatMessageLikesRepository, never()).deleteByChatMessage_IdAndUser_Id(chatMessageId, userId);
    }

    @Test
    void getChatMessagesByEventId_ShouldReturnChatMessageDTOs_WhenEventExists() {
        UUID eventId = UUID.randomUUID();
        ChatMessage chatMessage1 = new ChatMessage();
        ChatMessage chatMessage2 = new ChatMessage();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        chatMessage1.setId(id1);
        chatMessage1.setContent("Event message 1");
        chatMessage2.setId(id2);
        chatMessage2.setContent("Event message 2");
        // Set a dummy sender and event on both messages
        User dummyUser = createDummyUser(UUID.randomUUID());
        chatMessage1.setUserSender(dummyUser);
        chatMessage2.setUserSender(dummyUser);
        Event dummyEvent = createDummyEvent();
        chatMessage1.setEvent(dummyEvent);
        chatMessage2.setEvent(dummyEvent);
        List<ChatMessage> messages = List.of(chatMessage1, chatMessage2);
        when(chatMessageRepository.getChatMessagesByEventId(eventId)).thenReturn(messages);
        when(chatMessageRepository.findById(id1)).thenReturn(Optional.of(chatMessage1));
        when(chatMessageRepository.findById(id2)).thenReturn(Optional.of(chatMessage2));
        when(chatMessageLikesRepository.findByChatMessage(chatMessage1)).thenReturn(new ArrayList<>());
        when(chatMessageLikesRepository.findByChatMessage(chatMessage2)).thenReturn(new ArrayList<>());
        List<ChatMessageDTO> result = chatMessageService.getChatMessagesByEventId(eventId);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(dto -> dto.id().equals(id1)));
        assertTrue(result.stream().anyMatch(dto -> dto.id().equals(id2)));
    }

    @Test
    void getChatMessagesByEventId_ShouldThrowBasesNotFoundException_WhenDataAccessExceptionOccurs() {
        UUID eventId = UUID.randomUUID();
        DataAccessException dae = new DataAccessException("DB error") {};
        when(chatMessageRepository.getChatMessagesByEventId(eventId)).thenThrow(dae);
        BasesNotFoundException ex = assertThrows(BasesNotFoundException.class, () -> chatMessageService.getChatMessagesByEventId(eventId));
        verify(logger, times(1)).log(dae.getMessage());
    }

    @Test
    void getFullChatMessageByChatMessage_ShouldReturnFullEventChatMessageDTO() {
        UUID chatMessageId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Instant timestamp = Instant.now();
        ChatMessageDTO chatMessageDTO = new ChatMessageDTO(
                chatMessageId,
                "Full chat message",
                timestamp,
                senderId,
                eventId,
                List.of()
        );
        ChatMessage dummyChatMessage = new ChatMessage();
        dummyChatMessage.setId(chatMessageId);
        // Set dummy sender with the same senderId and event with the same eventId
        dummyChatMessage.setUserSender(createDummyUser(senderId));
        Event dummyEvent = new Event();
        dummyEvent.setId(eventId);
        dummyChatMessage.setEvent(dummyEvent);
        when(chatMessageRepository.findById(chatMessageId)).thenReturn(Optional.of(dummyChatMessage));
        when(chatMessageLikesRepository.findByChatMessage(dummyChatMessage)).thenReturn(new ArrayList<>());
        FullUserDTO fullUser = new FullUserDTO(senderId, List.of(), "username", "avatar.jpg", "First", "Last", "bio", List.of(), "email@example.com");
        when(userService.getFullUserById(senderId)).thenReturn(fullUser);
        when(userService.convertUsersToFullUsers(any(), new HashSet<>())).thenReturn(new ArrayList<>());
        FullEventChatMessageDTO fullDto = chatMessageService.getFullChatMessageByChatMessage(chatMessageDTO);
        assertNotNull(fullDto);
        assertEquals(chatMessageId, fullDto.id());
        assertEquals("Full chat message", fullDto.content());
        assertEquals(timestamp, fullDto.timestamp());
        assertEquals(eventId, fullDto.eventId());
        assertEquals(fullUser, fullDto.senderUser());
    }

    @Test
    void convertChatMessagesToFullFeedEventChatMessages_ShouldReturnConvertedList() {
        UUID chatMessageId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Instant timestamp = Instant.now();
        ChatMessageDTO chatMessageDTO = new ChatMessageDTO(
                chatMessageId,
                "Chat message conversion",
                timestamp,
                senderId,
                eventId,
                List.of()
        );
        List<ChatMessageDTO> chatMessageDTOs = List.of(chatMessageDTO);
        ChatMessage dummyChatMessage = new ChatMessage();
        dummyChatMessage.setId(chatMessageId);
        // Set dummy sender and event on the entity
        dummyChatMessage.setUserSender(createDummyUser(senderId));
        Event dummyEvent = new Event();
        dummyEvent.setId(eventId);
        dummyChatMessage.setEvent(dummyEvent);
        when(chatMessageRepository.findById(chatMessageId)).thenReturn(Optional.of(dummyChatMessage));
        when(chatMessageLikesRepository.findByChatMessage(dummyChatMessage)).thenReturn(new ArrayList<>());
        FullUserDTO fullUser = new FullUserDTO(senderId, List.of(), "username", "avatar.jpg", "First", "Last", "bio", List.of(), "email@example.com");
        when(userService.getFullUserById(senderId)).thenReturn(fullUser);
        when(userService.convertUsersToFullUsers(any(), new HashSet<>())).thenReturn(new ArrayList<>());
        List<FullEventChatMessageDTO> result = chatMessageService.convertChatMessagesToFullFeedEventChatMessages(chatMessageDTOs);
        assertNotNull(result);
        assertEquals(1, result.size());
        FullEventChatMessageDTO fullDto = result.get(0);
        assertEquals(chatMessageId, fullDto.id());
        assertEquals("Chat message conversion", fullDto.content());
        assertEquals(timestamp, fullDto.timestamp());
        assertEquals(eventId, fullDto.eventId());
        assertEquals(fullUser, fullDto.senderUser());
    }
}
