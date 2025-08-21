package com.danielagapov.spawn.Services.ChatMessage;

import com.danielagapov.spawn.DTOs.ChatMessage.ChatMessageDTO;
import com.danielagapov.spawn.DTOs.ChatMessage.ChatMessageLikesDTO;
import com.danielagapov.spawn.DTOs.ChatMessage.CreateChatMessageDTO;
import com.danielagapov.spawn.DTOs.ChatMessage.FullActivityChatMessageDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing chat messages and their interactions within activities.
 * Provides CRUD operations for chat messages, likes management, and data conversion utilities.
 */
public interface IChatMessageService {
    
    /**
     * Retrieves all chat messages from the database with their associated likes.
     * 
     * @return List of ChatMessageDTO objects with liked by user IDs populated
     * @throws com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException if database access fails
     */
    List<ChatMessageDTO> getAllChatMessages();

    /**
     * Retrieves a specific chat message by its unique identifier.
     * 
     * @param id the unique identifier of the chat message
     * @return ChatMessageDTO object with liked by user IDs populated
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if chat message with given ID is not found
     */
    ChatMessageDTO getChatMessageById(UUID id);

    /**
     * Creates a new chat message from the provided DTO and publishes notification events.
     * 
     * @param newChatMessageDTO the DTO containing chat message creation data
     * @return the created ChatMessageDTO with generated ID and timestamp
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseSaveException if saving fails
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if referenced user or activity doesn't exist
     */
    FullActivityChatMessageDTO createChatMessage(CreateChatMessageDTO newChatMessageDTO);

    /**
     * Saves a chat message to the database.
     * 
     * @param chatMessage the ChatMessageDTO to save
     * @return the saved ChatMessageDTO
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseSaveException if saving fails
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if referenced user or activity doesn't exist
     */
    ChatMessageDTO saveChatMessage(ChatMessageDTO chatMessage);

    /**
     * Deletes a chat message by its unique identifier.
     * 
     * @param id the unique identifier of the chat message to delete
     * @return true if deletion was successful, false otherwise
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if chat message with given ID is not found
     */
    boolean deleteChatMessageById(UUID id);

    /**
     * Retrieves all chat messages associated with a specific activity, ordered by timestamp descending.
     * 
     * @param activityId the unique identifier of the activity
     * @return List of ChatMessageDTO objects for the specified activity
     * @throws com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException if database access fails
     */
    List<ChatMessageDTO> getChatMessagesByActivityId(UUID activityId);

    /**
     * Retrieves the IDs of all chat messages associated with a specific activity.
     * 
     * @param activityId the unique identifier of the activity
     * @return List of UUID objects representing chat message IDs
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if activity doesn't exist
     */
    List<UUID> getChatMessageIdsByActivityId(UUID activityId);

    /**
     * Creates a like for a chat message from a specific user.
     * 
     * @param chatMessageId the unique identifier of the chat message to like
     * @param userId the unique identifier of the user creating the like
     * @return ChatMessageLikesDTO representing the created like
     * @throws com.danielagapov.spawn.Exceptions.EntityAlreadyExistsException if user already liked the message
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseSaveException if saving fails
     */
    ChatMessageLikesDTO createChatMessageLike(UUID chatMessageId, UUID userId);

    /**
     * Retrieves all users who have liked a specific chat message.
     * 
     * @param chatMessageId the unique identifier of the chat message
     * @return List of BaseUserDTO objects representing users who liked the message
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if chat message doesn't exist
     */
    List<BaseUserDTO> getChatMessageLikes(UUID chatMessageId);

    /**
     * Removes a like from a chat message for a specific user.
     * 
     * @param chatMessageId the unique identifier of the chat message
     * @param userId the unique identifier of the user removing the like
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if like doesn't exist
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseDeleteException if deletion fails
     */
    void deleteChatMessageLike(UUID chatMessageId, UUID userId);

    /**
     * Retrieves the user IDs of all users who have liked a specific chat message.
     * 
     * @param chatMessageId the unique identifier of the chat message
     * @return List of UUID objects representing user IDs who liked the message
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if chat message doesn't exist
     */
    List<UUID> getChatMessageLikeUserIds(UUID chatMessageId);

    /**
     * Converts a ChatMessageDTO to a FullActivityChatMessageDTO with complete user information.
     * 
     * @param chatMessage the ChatMessageDTO to convert
     * @return FullActivityChatMessageDTO with populated user details and likes
     */
    FullActivityChatMessageDTO getFullChatMessageByChatMessage(ChatMessageDTO chatMessage);

    /**
     * Retrieves a full chat message by its unique identifier with complete user information.
     * 
     * @param id the unique identifier of the chat message
     * @return FullActivityChatMessageDTO with populated user details and likes
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if chat message with given ID is not found
     */
    FullActivityChatMessageDTO getFullChatMessageById(UUID id);

    /**
     * Retrieves all full chat messages for a specific activity with complete user information.
     * 
     * @param activityId the unique identifier of the activity
     * @return List of FullActivityChatMessageDTO objects for the specified activity
     * @throws com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException if database access fails
     */
    List<FullActivityChatMessageDTO> getFullChatMessagesByActivityId(UUID activityId);

    /**
     * Converts a list of ChatMessageDTO objects to FullActivityChatMessageDTO objects.
     * 
     * @param chatMessages the list of ChatMessageDTO objects to convert
     * @return List of FullActivityChatMessageDTO objects with populated user details
     */
    List<FullActivityChatMessageDTO> convertChatMessagesToFullFeedActivityChatMessages(List<ChatMessageDTO> chatMessages);
}
