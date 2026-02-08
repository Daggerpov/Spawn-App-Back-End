package com.danielagapov.spawn.media.internal.services;

import java.util.UUID;

/**
 * Service interface for managing AWS S3 operations related to file storage and retrieval.
 * This is a pure storage service - it handles S3 operations only, without any user entity management.
 * User entity management (updating profile picture URLs, etc.) should be handled by UserService.
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
     * Puts the file into an s3 bucket using a randomly generated key
     *
     * @param file byte array representation of a file (usually a .jpeg)
     * @return a cdn url string to access the newly added object
     * @throws RuntimeException if S3 operation fails
     */
    String putObject(byte[] file);

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

    /**
     * Uploads a profile picture for a user using their userId as the key.
     * If file is null, returns the default profile picture URL.
     *
     * @param file byte array representation of a file (usually a .jpeg), can be null for default picture
     * @param userId the ID of the user
     * @return the CDN URL for the uploaded profile picture, or default if file is null
     * @throws RuntimeException if S3 operation fails
     */
    String uploadProfilePicture(byte[] file, UUID userId);

    /**
     * Checks if the given URL is the default profile picture URL
     *
     * @param urlString the URL to check
     * @return true if it's the default profile picture URL
     */
    boolean isDefaultProfilePicture(String urlString);
}
