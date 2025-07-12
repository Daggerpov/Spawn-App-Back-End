package com.danielagapov.spawn.Services.S3;


import com.danielagapov.spawn.DTOs.User.UserDTO;

import java.util.UUID;

/**
 * Service interface for managing AWS S3 operations related to file storage and retrieval.
 * Primarily handles profile picture storage and management for users.
 */
public interface IS3Service {
    /**
     * Given key, puts the given file into the spawn s3 bucket
     *
     * @param file byte array representation of a file (usually a .jpeg)
     * @param key  string key to map the file to in S3
     * @return a cdn url string to access the newly added object
     * @throws RuntimeException if S3 operation fails
     */
    String putObjectWithKey(byte[] file, String key);

    /**
     * Given a user id, deletes a user's profile picture from S3 and updates the user's profile picture URL to null
     *
     * @param userId id of user whose profile picture is to be deleted
     * @throws RuntimeException if S3 operation or user update fails
     */
    void deleteObjectByUserId(UUID userId);

    /**
     * Puts the file into an s3 bucket using a randomly generated key
     *
     * @param file byte array representation of a file (usually a .jpeg)
     * @return a cdn url string to access the newly added object
     * @throws RuntimeException if S3 operation fails
     */
    String putObject(byte[] file);

    /**
     * Puts the file into an s3 bucket and attaches the cdn url string to the given user dto.
     * If file is null, a default url string is attached
     *
     * @param file byte array representation of a file (usually a .jpeg), can be null for default picture
     * @param user user to attach url string with
     * @return UserDTO with a set profilePictureUrlString
     * @throws RuntimeException if S3 operation fails
     */
    UserDTO putProfilePictureWithUser(byte[] file, UserDTO user);

    /**
     * Updates an existing user's profile picture by replacing the image at their current URL
     * or setting it to the default profile picture URL if file is null
     *
     * @param file byte array representation of a file (usually a .jpeg), can be null for default picture
     * @param userId the ID of the user whose profile picture should be updated
     * @return UserDTO with updated profile picture URL
     * @throws RuntimeException if S3 operation or user update fails
     */
    UserDTO updateProfilePicture(byte[] file, UUID userId);

    /**
     * Returns the default profile picture url string
     * 
     * @return the default profile picture URL string
     */
    String getDefaultProfilePicture();

    /**
     * Deletes the object by the given url string
     * Since the cdn is read-only, the object key is extracted from the url string and deleted
     * 
     * @param urlString the CDN URL string of the object to delete
     * @throws RuntimeException if S3 operation fails
     */
    void deleteObjectByURL(String urlString);
}
