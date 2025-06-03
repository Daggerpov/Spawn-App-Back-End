package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.DTOs.ChatMessage.ChatMessageDTO;
import com.danielagapov.spawn.DTOs.ChatMessage.FullActivityChatMessageDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Exceptions.Base.BaseDeleteException;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Models.ChatMessage;
import com.danielagapov.spawn.Models.ChatMessageLikes;
import com.danielagapov.spawn.Models.Activity;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.*;
import com.danielagapov.spawn.Repositories.User.IUserRepository;
import com.danielagapov.spawn.Services.ChatMessage.ChatMessageService;
import com.danielagapov.spawn.Services.Activity.IActivityService;
import com.danielagapov.spawn.Services.FriendTag.IFriendTagService;
import com.danielagapov.spawn.Services.User.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
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
    private IActivityRepository ActivityRepository;

    @Mock
    private IActivityService ActivityService;

    @Mock
    private IFriendTagService ftService;

    @Mock
    private IUserRepository userRepository;

    @Mock
    private ILogger logger;

    @Mock
    private IActivityUserRepository activityUserRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ChatMessageService chatMessageService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    // Helper method to create a dummy Activity with a random ID.
    private Activity createDummyActivity() {
        Activity Activity = new Activity();
        Activity.setId(UUID.randomUUID());
        return Activity;
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
    void saveChatMessage_ShouldThrowException_WhenActivityNotFound() {
        UUID userId = UUID.randomUUID();
        UUID ActivityId = UUID.randomUUID();
        UserDTO userDTO = new UserDTO(
                userId,
                List.of(),
                "johndoe",
                "profile.jpg",
                "John Doe",
                "A bio",
                List.of(),
                "john.doe@example.com"
        );
        ChatMessageDTO chatMessageDTO = new ChatMessageDTO(
                UUID.randomUUID(),
                "Hello!",
                Instant.now(),
                userDTO.getId(),
                ActivityId,
                List.of()
        );
        // Stub user exists but Activity is missing
        User dummyUser = createDummyUser(userDTO.getId());
        when(userRepository.findById(userDTO.getId())).thenReturn(Optional.of(dummyUser));
        when(ActivityRepository.findById(ActivityId)).thenReturn(Optional.empty());
        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class,
                () -> chatMessageService.saveChatMessage(chatMessageDTO));
        assertTrue(exception.getMessage().contains(ActivityId.toString()));
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
        // Also set a dummy Activity to avoid NPE in mapping
        Activity dummyActivity = createDummyActivity();
        chatMessage1.setActivity(dummyActivity);
        chatMessage2.setActivity(dummyActivity);
        List<ChatMessage> messages = List.of(chatMessage1, chatMessage2);
        when(chatMessageRepository.findAll()).thenReturn(messages);
        when(chatMessageRepository.findById(id1)).thenReturn(Optional.of(chatMessage1));
        when(chatMessageRepository.findById(id2)).thenReturn(Optional.of(chatMessage2));
        when(chatMessageLikesRepository.findByChatMessage(chatMessage1)).thenReturn(new ArrayList<>());
        when(chatMessageLikesRepository.findByChatMessage(chatMessage2)).thenReturn(new ArrayList<>());
        List<ChatMessageDTO> result = chatMessageService.getAllChatMessages();
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(dto -> dto.getId().equals(id1)));
        assertTrue(result.stream().anyMatch(dto -> dto.getId().equals(id2)));
    }

    @Test
    void getAllChatMessages_ShouldThrowBasesNotFoundException_WhenDataAccessExceptionOccurs() {
        DataAccessException dae = new DataAccessException("DB error") {
        };
        when(chatMessageRepository.findAll()).thenThrow(dae);
        assertThrows(BasesNotFoundException.class, () -> chatMessageService.getAllChatMessages());
        verify(logger, times(1)).error(dae.getMessage());
    }

    @Test
    void getChatMessageLikeUserIds_ShouldReturnUserIds_WhenLikesExist() {
        UUID chatMessageId = UUID.randomUUID();
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setId(chatMessageId);
        // Set a dummy sender (even if not used in this method)
        chatMessage.setUserSender(createDummyUser(UUID.randomUUID()));
        // Set a dummy Activity to avoid NPE if mapper is used indirectly
        chatMessage.setActivity(createDummyActivity());
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
        // Set dummy sender and Activity
        chatMessage.setUserSender(createDummyUser(UUID.randomUUID()));
        chatMessage.setActivity(createDummyActivity());
        when(chatMessageRepository.findById(chatMessageId)).thenReturn(Optional.of(chatMessage));
        when(chatMessageLikesRepository.findByChatMessage(chatMessage)).thenReturn(new ArrayList<>());
        ChatMessageDTO dto = chatMessageService.getChatMessageById(chatMessageId);
        assertNotNull(dto);
        assertEquals(chatMessageId, dto.getId());
        assertEquals("Test message", dto.getContent());
    }

    @Test
    void getChatMessageById_ShouldThrowBaseNotFoundException_WhenMessageNotFound() {
        UUID chatMessageId = UUID.randomUUID();
        when(chatMessageRepository.findById(chatMessageId)).thenReturn(Optional.empty());
        BaseNotFoundException ex = assertThrows(BaseNotFoundException.class, () -> chatMessageService.getChatMessageById(chatMessageId));
        assertTrue(ex.getMessage().contains(chatMessageId.toString()));
    }

    @Test
    void getFullChatMessageById_ShouldReturnFullActivityChatMessageDTO_WhenMessageExists() {
        UUID chatMessageId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID ActivityId = UUID.randomUUID();
        Instant timestamp = Instant.now();
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setId(chatMessageId);
        chatMessage.setContent("Full message");
        chatMessage.setTimestamp(timestamp);
        // Set dummy sender with expected senderId and Activity with expected ActivityId
        chatMessage.setUserSender(createDummyUser(senderId));
        Activity dummyActivity = new Activity();
        dummyActivity.setId(ActivityId);
        chatMessage.setActivity(dummyActivity);
        when(chatMessageRepository.findById(chatMessageId)).thenReturn(Optional.of(chatMessage));
        when(chatMessageLikesRepository.findByChatMessage(chatMessage)).thenReturn(new ArrayList<>());
        UserDTO userDTO = new UserDTO(senderId, List.of(), "username", "avatar.jpg", "John Doe", "bio", List.of(), "email@example.com");
        when(userService.getUserById(any(UUID.class))).thenReturn(userDTO);
        when(userService.getAllUsers()).thenReturn(new ArrayList<>());
        FullActivityChatMessageDTO result = chatMessageService.getFullChatMessageById(chatMessageId);
        assertNotNull(result);
        assertEquals(chatMessageId, result.getId());
        assertEquals("Full message", result.getContent());
        assertEquals(userDTO, result.getSenderUser());
    }

    @Test
    void getFullChatMessagesByActivityId_ShouldReturnListOfFullActivityChatMessageDTOs() {
        UUID ActivityId = UUID.randomUUID();
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
        // Set dummy sender and Activity for both messages
        User dummyUser = createDummyUser(UUID.randomUUID());
        chatMessage1.setUserSender(dummyUser);
        chatMessage2.setUserSender(dummyUser);
        Activity dummyActivity = createDummyActivity();
        chatMessage1.setActivity(dummyActivity);
        chatMessage2.setActivity(dummyActivity);
        List<ChatMessage> messages = List.of(chatMessage1, chatMessage2);
        when(chatMessageRepository.getChatMessagesByActivityIdOrderByTimestampDesc(ActivityId)).thenReturn(messages);
        when(chatMessageRepository.findById(id1)).thenReturn(Optional.of(chatMessage1));
        when(chatMessageRepository.findById(id2)).thenReturn(Optional.of(chatMessage2));
        when(chatMessageLikesRepository.findByChatMessage(chatMessage1)).thenReturn(new ArrayList<>());
        when(chatMessageLikesRepository.findByChatMessage(chatMessage2)).thenReturn(new ArrayList<>());
        BaseUserDTO baseUser = new BaseUserDTO(UUID.randomUUID(), "John Doe", "user@example.com", "user", "bio", "avatar.jpg");
        when(userService.getBaseUserById(any(UUID.class))).thenReturn(baseUser);
        when(userService.getAllUsers()).thenReturn(new ArrayList<>());
        List<FullActivityChatMessageDTO> result = chatMessageService.getFullChatMessagesByActivityId(ActivityId);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(dto -> dto.getId().equals(id1)));
        assertTrue(result.stream().anyMatch(dto -> dto.getId().equals(id2)));
    }

    @Test
    void saveChatMessage_ShouldSaveMessage_WhenValgetId() {
        UUID userId = UUID.randomUUID();
        UUID ActivityId = UUID.randomUUID();
        ChatMessageDTO chatMessageDTO = new ChatMessageDTO(
                UUID.randomUUID(),
                "Saving message",
                Instant.now(),
                userId,
                ActivityId,
                List.of()
        );
        // Create dummy user and Activity so the save proceeds
        User dummyUser = createDummyUser(userId);
        Activity dummyActivity = new Activity();
        dummyActivity.setId(ActivityId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(dummyUser));
        when(ActivityRepository.findById(ActivityId)).thenReturn(Optional.of(dummyActivity));
        ChatMessage dummyChatMessage = new ChatMessage();
        dummyChatMessage.setId(chatMessageDTO.getId());
        dummyChatMessage.setContent(chatMessageDTO.getContent());
        // Set sender and Activity on the saved entity
        dummyChatMessage.setUserSender(dummyUser);
        dummyChatMessage.setActivity(dummyActivity);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(dummyChatMessage);
        ChatMessageDTO savedDTO = chatMessageService.saveChatMessage(chatMessageDTO);
        assertNotNull(savedDTO);
        assertEquals(chatMessageDTO.getId(), savedDTO.getId());
        assertEquals("Saving message", savedDTO.getContent());
    }

    @Test
    void getChatMessageIdsByActivityId_ShouldReturnIds_WhenActivityExists() {
        UUID ActivityId = UUID.randomUUID();
        Activity dummyActivity = new Activity();
        dummyActivity.setId(ActivityId);
        when(ActivityRepository.findById(ActivityId)).thenReturn(Optional.of(dummyActivity));
        ChatMessage chatMessage1 = new ChatMessage();
        ChatMessage chatMessage2 = new ChatMessage();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        chatMessage1.setId(id1);
        chatMessage2.setId(id2);
        // Set a dummy sender and Activity on each message
        User dummyUser = createDummyUser(UUID.randomUUID());
        chatMessage1.setUserSender(dummyUser);
        chatMessage2.setUserSender(dummyUser);
        Activity ActivityForMessages = createDummyActivity();
        chatMessage1.setActivity(ActivityForMessages);
        chatMessage2.setActivity(ActivityForMessages);
        List<ChatMessage> messages = List.of(chatMessage1, chatMessage2);
        when(chatMessageRepository.getChatMessagesByActivityIdOrderByTimestampDesc(ActivityId)).thenReturn(messages);
        List<UUID> ids = chatMessageService.getChatMessageIdsByActivityId(ActivityId);
        assertNotNull(ids);
        assertEquals(2, ids.size());
        assertTrue(ids.contains(id1));
        assertTrue(ids.contains(id2));
    }

    @Test
    void getChatMessageIdsByActivityId_ShouldThrowBaseNotFoundException_WhenActivityNotFound() {
        UUID ActivityId = UUID.randomUUID();
        when(ActivityRepository.findById(ActivityId)).thenReturn(Optional.empty());
    }

    @Test
    void createChatMessageLike_ShouldReturnChatMessageLikesDTO_WhenLikeIsCreated() {
        UUID chatMessageId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(chatMessageLikesRepository.existsByChatMessage_IdAndUser_Id(chatMessageId, userId)).thenReturn(false);
        ChatMessage dummyChatMessage = new ChatMessage();
        dummyChatMessage.setId(chatMessageId);
        // Set a dummy sender and Activity
        dummyChatMessage.setUserSender(createDummyUser(UUID.randomUUID()));
        dummyChatMessage.setActivity(createDummyActivity());
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
        // Set a dummy sender and Activity on the chat message
        dummyChatMessage.setUserSender(createDummyUser(UUID.randomUUID()));
        dummyChatMessage.setActivity(createDummyActivity());
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
        List<BaseUserDTO> result = chatMessageService.getChatMessageLikes(chatMessageId);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(dummyUser.getId(), result.get(0).getId());
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
    void getChatMessagesByActivityId_ShouldReturnChatMessageDTOs_WhenActivityExists() {
        UUID ActivityId = UUID.randomUUID();
        ChatMessage chatMessage1 = new ChatMessage();
        ChatMessage chatMessage2 = new ChatMessage();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        chatMessage1.setId(id1);
        chatMessage1.setContent("Activity message 1");
        chatMessage2.setId(id2);
        chatMessage2.setContent("Activity message 2");
        // Set a dummy sender and Activity on both messages
        User dummyUser = createDummyUser(UUID.randomUUID());
        chatMessage1.setUserSender(dummyUser);
        chatMessage2.setUserSender(dummyUser);
        Activity dummyActivity = createDummyActivity();
        chatMessage1.setActivity(dummyActivity);
        chatMessage2.setActivity(dummyActivity);
        List<ChatMessage> messages = List.of(chatMessage1, chatMessage2);
        when(chatMessageRepository.getChatMessagesByActivityIdOrderByTimestampDesc(ActivityId)).thenReturn(messages);
        when(chatMessageRepository.findById(id1)).thenReturn(Optional.of(chatMessage1));
        when(chatMessageRepository.findById(id2)).thenReturn(Optional.of(chatMessage2));
        when(chatMessageLikesRepository.findByChatMessage(chatMessage1)).thenReturn(new ArrayList<>());
        when(chatMessageLikesRepository.findByChatMessage(chatMessage2)).thenReturn(new ArrayList<>());
        List<ChatMessageDTO> result = chatMessageService.getChatMessagesByActivityId(ActivityId);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(dto -> dto.getId().equals(id1)));
        assertTrue(result.stream().anyMatch(dto -> dto.getId().equals(id2)));
    }

    @Test
    void getChatMessagesByActivityId_ShouldThrowBasesNotFoundException_WhenDataAccessExceptionOccurs() {
        UUID ActivityId = UUID.randomUUID();
        DataAccessException dae = new DataAccessException("DB error") {
        };
        when(chatMessageRepository.getChatMessagesByActivityIdOrderByTimestampDesc(ActivityId)).thenThrow(dae);
        assertThrows(BasesNotFoundException.class, () -> chatMessageService.getChatMessagesByActivityId(ActivityId));
        verify(logger, times(1)).error(dae.getMessage());
    }

    @Test
    void getFullChatMessageByChatMessage_ShouldReturnFullActivityChatMessageDTO() {
        UUID chatMessageId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID ActivityId = UUID.randomUUID();
        Instant timestamp = Instant.now();
        ChatMessageDTO chatMessageDTO = new ChatMessageDTO(
                chatMessageId,
                "Full chat message",
                timestamp,
                senderId,
                ActivityId,
                List.of()
        );
        ChatMessage dummyChatMessage = new ChatMessage();
        dummyChatMessage.setId(chatMessageId);
        // Set dummy sender with the same senderId and Activity with the same ActivityId
        dummyChatMessage.setUserSender(createDummyUser(senderId));
        Activity dummyActivity = new Activity();
        dummyActivity.setId(ActivityId);
        dummyChatMessage.setActivity(dummyActivity);
        when(chatMessageRepository.findById(chatMessageId)).thenReturn(Optional.of(dummyChatMessage));
        when(chatMessageLikesRepository.findByChatMessage(dummyChatMessage)).thenReturn(new ArrayList<>());
        UserDTO userDTO = new UserDTO(senderId, List.of(), "username", "avatar.jpg", "John Doe", "bio", List.of(), "email@example.com");
        when(userService.getUserById(senderId)).thenReturn(userDTO);
        when(userService.getAllUsers()).thenReturn(new ArrayList<>());
        FullActivityChatMessageDTO fullDto = chatMessageService.getFullChatMessageByChatMessage(chatMessageDTO);
        assertNotNull(fullDto);
        assertEquals(chatMessageId, fullDto.getId());
        assertEquals("Full chat message", fullDto.getContent());
        assertEquals(timestamp, fullDto.getTimestamp());
        assertEquals(ActivityId, fullDto.getActivityId());
        assertEquals(userDTO, fullDto.getSenderUser());
    }

    @Test
    void convertChatMessagesToFullFeedActivityChatMessages_ShouldReturnConvertedList() {
        UUID chatMessageId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID ActivityId = UUID.randomUUID();
        Instant timestamp = Instant.now();
        ChatMessageDTO chatMessageDTO = new ChatMessageDTO(
                chatMessageId,
                "Chat message conversion",
                timestamp,
                senderId,
                ActivityId,
                List.of()
        );
        List<ChatMessageDTO> chatMessageDTOs = List.of(chatMessageDTO);
        ChatMessage dummyChatMessage = new ChatMessage();
        dummyChatMessage.setId(chatMessageId);
        // Set dummy sender and Activity on the entity
        dummyChatMessage.setUserSender(createDummyUser(senderId));
        Activity dummyActivity = new Activity();
        dummyActivity.setId(ActivityId);
        dummyChatMessage.setActivity(dummyActivity);
        when(chatMessageRepository.findById(chatMessageId)).thenReturn(Optional.of(dummyChatMessage));
        when(chatMessageLikesRepository.findByChatMessage(dummyChatMessage)).thenReturn(new ArrayList<>());
        UserDTO userDTO = new UserDTO(senderId, List.of(), "username", "avatar.jpg", "John Doe", "bio", List.of(), "email@example.com");
        when(userService.getUserById(senderId)).thenReturn(userDTO);
        when(userService.getAllUsers()).thenReturn(new ArrayList<>());
        List<FullActivityChatMessageDTO> result = chatMessageService.convertChatMessagesToFullFeedActivityChatMessages(chatMessageDTOs);
        assertNotNull(result);
        assertEquals(1, result.size());
        FullActivityChatMessageDTO fullDto = result.get(0);
        assertEquals(chatMessageId, fullDto.getId());
        assertEquals("Chat message conversion", fullDto.getContent());
        assertEquals(timestamp, fullDto.getTimestamp());
        assertEquals(ActivityId, fullDto.getActivityId());
        assertEquals(userDTO, fullDto.getSenderUser());
    }
}
