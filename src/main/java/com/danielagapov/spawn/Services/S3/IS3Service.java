package com.danielagapov.spawn.Services.S3;


import com.danielagapov.spawn.DTOs.User.UserDTO;

import java.util.UUID;

public interface IS3Service {
    /**
     * Given key, puts the given file into the spawn s3 bucket
     *
     * @param file byte array representation of a file (usually a .jpeg)
     * @param key  string
     * @return a cdn url string to access the newly added object
     */
    String putObjectWithKey(byte[] file, String key);

    /**
     * Given a user id, deletes a user's profile picture
     *
     * @param userId id of user whose pfp is to delete
     */
    void deleteObjectByUserId(UUID userId);

    /**
     * Puts the file into an s3 bucket
     *
     * @param file byte array representation of a file (usually a .jpeg)
     * @return a cdn url string to access the newly added object
     */
    String putObject(byte[] file);

    /**
     * Puts the file into an s3 bucket and attaches the cdn url string to the given user dto.
     * If file is null, a default url string is attached
     *
     * @param file byte array representation of a file (usually a .jpeg)
     * @param user user to attach url string with
     * @return UserDTO with a set profilePictureUrlString
     */
    UserDTO putProfilePictureWithUser(byte[] file, UserDTO user);

    UserDTO updateProfilePicture(byte[] file, UUID userId);

    /**
     * Returns the default profile picture url string
     */
    String getDefaultProfilePicture();

    /**
     * Deletes the object by the given url string
     * Since the cdn is read-only, the object key is extracted from the url string and deleted
     */
    void deleteObjectByURL(String urlString);

    String updateProfilePictureWithUserId(byte[] file, UUID userId);
}
