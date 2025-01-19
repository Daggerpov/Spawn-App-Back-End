//package com.danielagapov.spawn.ChatMessageTests;
//
//import com.danielagapov.spawn.DTOs.UserDTO;
//import com.danielagapov.spawn.Exceptions.Base.BaseDeleteException;
//import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
//import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
//import com.danielagapov.spawn.Models.ChatMessage;
//import com.danielagapov.spawn.Models.ChatMessageLikes;
//import com.danielagapov.spawn.Models.User;
//import com.danielagapov.spawn.Repositories.IChatMessageLikesRepository;
//import com.danielagapov.spawn.Repositories.IChatMessageRepository;
//import com.danielagapov.spawn.Repositories.IUserRepository;
//import com.danielagapov.spawn.Services.ChatMessage.ChatMessageService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//
//public class ChatMessageLikesTest {
//
//    @Mock
//    private IChatMessageLikesRepository chatMessageLikesRepository;
//
//    @Mock
//    private IChatMessageRepository chatMessageRepository;
//
//    @Mock
//    private IUserRepository userRepository;
//
//    @InjectMocks
//    private ChatMessageService chatMessageService;
//
//    @BeforeEach
//    void setup() {
//        MockitoAnnotations.openMocks(this); // Initialize mocks
//    }
//
//    // Delete ChatmessageLikes Tests!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//    @Test
//    void deleteChatMessageLike_ShouldDeleteLike_WhenLikeExists() {
//        // Sample data
//        UUID chatMessageId = UUID.randomUUID();
//        UUID userId = UUID.randomUUID();
//
//        // "tells" the mock object to return true when the method is called with the given parameters
//        when(chatMessageLikesRepository.existsByChatMessage_IdAndUser_Id(chatMessageId, userId)).thenReturn(true);
//
//        assertDoesNotThrow(() -> chatMessageService.deleteChatMessageLike(chatMessageId, userId));
//
//        verify(chatMessageLikesRepository, times(1))
//                .deleteByChatMessage_IdAndUser_Id(chatMessageId, userId);
//    }
//
//    @Test
//    void deleteChatMessageLike_ShouldThrowException_WhenLikeDoesNotExist() {
//
//        UUID chatMessageId = UUID.randomUUID();
//        UUID userId = UUID.randomUUID();
//
//        when(chatMessageLikesRepository.existsByChatMessage_IdAndUser_Id(chatMessageId, userId)).thenReturn(false);
//
//        BaseDeleteException exception = assertThrows(BaseDeleteException.class,
//                () -> chatMessageService.deleteChatMessageLike(chatMessageId, userId));
//
//        assertTrue(exception.getMessage().contains("An error occurred while deleting the like for chatMessageId:"));
//
//        verify(chatMessageLikesRepository, never()).deleteByChatMessage_IdAndUser_Id(any(), any());
//    }
//
//    @Test
//    void deleteChatMessageLike_ShouldThrowException_WhenDatabaseErrorOccurs() {
//
//        UUID chatMessageId = UUID.randomUUID();
//        UUID userId = UUID.randomUUID();
//
//        when(chatMessageLikesRepository.existsByChatMessage_IdAndUser_Id(chatMessageId, userId)).thenReturn(true);
//        doThrow(new RuntimeException("Database error")).when(chatMessageLikesRepository)
//                .deleteByChatMessage_IdAndUser_Id(chatMessageId, userId);
//
//        BaseDeleteException exception = assertThrows(BaseDeleteException.class,
//                () -> chatMessageService.deleteChatMessageLike(chatMessageId, userId));
//
//        assertTrue(exception.getMessage().contains("An error occurred while deleting the like for chatMessageId"));
//        verify(chatMessageLikesRepository, times(1))
//                .deleteByChatMessage_IdAndUser_Id(chatMessageId, userId);
//    }
//
//    // Create ChatmessageLikes Tests!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//    @Test
//    void createChatMessageLike_ShouldCreateLike_WhenNotExists() {
//
//        UUID chatMessageId = UUID.randomUUID();
//        UUID userId = UUID.randomUUID();
//
//        ChatMessage chatMessage = new ChatMessage();
//        User user = new User();
//
//        when(chatMessageLikesRepository.existsByChatMessage_IdAndUser_Id(chatMessageId, userId)).thenReturn(false);
//        when(chatMessageRepository.findById(chatMessageId)).thenReturn(Optional.of(chatMessage));
//        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
//
//        assertDoesNotThrow(() -> chatMessageService.createChatMessageLike(chatMessageId, userId));
//
//        verify(chatMessageLikesRepository, times(1)).save(any(ChatMessageLikes.class));
//    }
//
//    @Test
//    void createChatMessageLike_ShouldThrowException_WhenLikeAlreadyExists() {
//
//        UUID chatMessageId = UUID.randomUUID();
//        UUID userId = UUID.randomUUID();
//
//        when(chatMessageLikesRepository.existsByChatMessage_IdAndUser_Id(chatMessageId, userId)).thenReturn(true);
//
//        BaseSaveException exception = assertThrows(BaseSaveException.class,
//                () -> chatMessageService.createChatMessageLike(chatMessageId, userId));
//
//        assertTrue(exception.getMessage().contains("failed to save an entity: Like already exists for chatMessageId:"));
//        verify(chatMessageLikesRepository, never()).save(any(ChatMessageLikes.class));
//    }
//
//    @Test
//    void createChatMessageLike_ShouldThrowException_WhenChatMessageNotFound() {
//
//        UUID chatMessageId = UUID.randomUUID();
//        UUID userId = UUID.randomUUID();
//
//        when(chatMessageLikesRepository.existsByChatMessage_IdAndUser_Id(chatMessageId, userId)).thenReturn(false);
//        when(chatMessageRepository.findById(chatMessageId)).thenReturn(Optional.empty());
//
//        BaseSaveException exception = assertThrows(BaseSaveException.class,
//                () -> chatMessageService.createChatMessageLike(chatMessageId, userId));
//
//        assertTrue(exception.getMessage().contains("failed to save an entity: ChatMessageId:"));
//
//        verify(chatMessageLikesRepository, never()).save(any(ChatMessageLikes.class));
//    }
//
//    @Test
//    void createChatMessageLike_ShouldThrowException_WhenUserNotFound() {
//
//        UUID chatMessageId = UUID.randomUUID();
//        UUID userId = UUID.randomUUID();
//
//        ChatMessage chatMessage = new ChatMessage();
//
//        when(chatMessageLikesRepository.existsByChatMessage_IdAndUser_Id(chatMessageId, userId)).thenReturn(false);
//        when(chatMessageRepository.findById(chatMessageId)).thenReturn(Optional.of(chatMessage));
//        when(userRepository.findById(userId)).thenReturn(Optional.empty());
//
//        BaseSaveException exception = assertThrows(BaseSaveException.class,
//                () -> chatMessageService.createChatMessageLike(chatMessageId, userId));
//
//        assertTrue(exception.getMessage().contains("failed to save an entity: UserId:"));
//
//        verify(chatMessageLikesRepository, never()).save(any(ChatMessageLikes.class));
//    }
//
//    @Test
//    void createChatMessageLike_ShouldThrowException_WhenDatabaseErrorOccurs() {
//
//        UUID chatMessageId = UUID.randomUUID();
//        UUID userId = UUID.randomUUID();
//
//        ChatMessage chatMessage = new ChatMessage();
//        User user = new User();
//
//        when(chatMessageLikesRepository.existsByChatMessage_IdAndUser_Id(chatMessageId, userId)).thenReturn(false);
//        when(chatMessageRepository.findById(chatMessageId)).thenReturn(Optional.of(chatMessage));
//        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
//        doThrow(new RuntimeException("Database error")).when(chatMessageLikesRepository).save(any(ChatMessageLikes.class));
//
//        BaseSaveException exception = assertThrows(BaseSaveException.class,
//                () -> chatMessageService.createChatMessageLike(chatMessageId, userId));
//
//        assertTrue(exception.getMessage().contains("failed to save an entity: Like: chatMessageId: "));
//        verify(chatMessageLikesRepository, times(1)).save(any(ChatMessageLikes.class));
//    }
//
//    // Get ChatmessageLikes Tests!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//    @Test
//    void getChatMessageLikes_ShouldReturnLikes_WhenChatMessageExists() {
//
//        UUID chatMessageId = UUID.randomUUID();
//        ChatMessage chatMessage = new ChatMessage();
//        chatMessage.setId(chatMessageId);
//
//        User user1 = new User();
//        user1.setId(UUID.randomUUID());
//        user1.setFirstName("John");
//        user1.setLastName("Doe");
//
//        User user2 = new User();
//        user2.setId(UUID.randomUUID());
//        user2.setFirstName("Jane");
//        user2.setLastName("Smith");
//
//        ChatMessageLikes like1 = new ChatMessageLikes();
//        like1.setUser(user1);
//        like1.setChatMessage(chatMessage);
//
//        ChatMessageLikes like2 = new ChatMessageLikes();
//        like2.setUser(user2);
//        like2.setChatMessage(chatMessage);
//
//        when(chatMessageRepository.findById(chatMessageId)).thenReturn(Optional.of(chatMessage));
//        when(chatMessageLikesRepository.findByChatMessage(chatMessage)).thenReturn(List.of(like1, like2));
//
//        List<UserDTO> result = chatMessageService.getChatMessageLikes(chatMessageId);
//
//        assertEquals(2, result.size());
//        assertEquals("John", result.get(0).firstName());
//        assertEquals("Jane", result.get(1).firstName());
//        verify(chatMessageRepository, times(1)).findById(chatMessageId);
//        verify(chatMessageLikesRepository, times(1)).findByChatMessage(chatMessage);
//    }
//
//    @Test
//    void getChatMessageLikes_ShouldThrowException_WhenChatMessageDoesNotExist() {
//
//        UUID chatMessageId = UUID.randomUUID();
//        when(chatMessageRepository.findById(chatMessageId)).thenReturn(Optional.empty());
//
//        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class,
//                () -> chatMessageService.getChatMessageLikes(chatMessageId));
//
//        assertEquals("Entity not found with ID: " + chatMessageId, exception.getMessage());
//        verify(chatMessageRepository, times(1)).findById(chatMessageId);
//        verify(chatMessageLikesRepository, never()).findByChatMessage(any());
//    }
//
//    @Test
//    void getChatMessageLikes_ShouldReturnEmptyList_WhenNoLikesExist() {
//
//        UUID chatMessageId = UUID.randomUUID();
//        ChatMessage chatMessage = new ChatMessage();
//        chatMessage.setId(chatMessageId);
//
//        when(chatMessageRepository.findById(chatMessageId)).thenReturn(Optional.of(chatMessage));
//        when(chatMessageLikesRepository.findByChatMessage(chatMessage)).thenReturn(List.of());
//
//        List<UserDTO> result = chatMessageService.getChatMessageLikes(chatMessageId);
//
//        assertTrue(result.isEmpty());
//        verify(chatMessageRepository, times(1)).findById(chatMessageId);
//        verify(chatMessageLikesRepository, times(1)).findByChatMessage(chatMessage);
//    }
//}
