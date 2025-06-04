package com.danielagapov.spawn.Services.S3;

import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Exceptions.ApplicationException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Services.User.UserService;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;


@Service
@Profile("!test") // Exclude this service from test profile
public class S3Service implements IS3Service {
    private static final String BUCKET = "spawn-pfp-store";
    private static final String CDN_BASE;
    private static final String DEFAULT_PFP;

    static {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        DEFAULT_PFP = dotenv.get("DEFAULT_PFP");
        CDN_BASE = dotenv.get("CDN_BASE");

    }

    private final S3Client s3;
    private final ILogger logger;
    private final UserService userService;

    public S3Service(S3Client s3, ILogger logger, UserService userService) {
        this.s3 = s3;
        this.logger = logger;
        this.userService = userService;
    }

    /**
     * This is the closest method directly to our S3Client, which puts an object to
     * our S3 bucket, given a key to map it to
     * <p>
     * Returns the profile picture url string where it's now hosted through a CDN
     */
    @Override
    public String putObjectWithKey(byte[] file, String key) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(BUCKET)
                .key(key)
                .contentType("image/jpeg")
                .build();
        try {
            s3.putObject(request, RequestBody.fromBytes(file));
            return CDN_BASE + key;
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    /**
     * Puts object to S3, by mapping a file to a randomly generated key
     */
    @Override
    public String putObject(byte[] file) {
        try {
            String key = UUID.randomUUID().toString();
            return putObjectWithKey(file, key);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }


    /**
     * This method, if given a `file` argument will put that profile picture object to S3
     * Otherwise, it will use our default pfp url string as the user's profile picture
     */
    @Override
    public UserDTO putProfilePictureWithUser(byte[] file, UserDTO user) {
        try {
            return new UserDTO(
                    user.getId(),
                    user.getFriendUserIds(),
                    user.getUsername(),
                    file == null ? DEFAULT_PFP : putObject(file),
                    user.getName(),
                    user.getBio(),
                    user.getFriendTagIds(),
                    user.getEmail()
            );
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    /**
     * Given an existing `userId`, this method will update the profile picture
     * attribute of that user, and also replace the image at its currently hosted
     * image url (through our CDN) with a supplied image, `file` argument
     * <p>
     * If given no `file` argument, we simply supply the default pfp url string
     */
    @Override
    public UserDTO updateProfilePicture(byte[] file, UUID userId) {
        try {
            User user = userService.getUserEntityById(userId);
            String urlString = user.getProfilePictureUrlString();
            // Default pfp url string is read only, new bucket entry should be made here
            if (urlString.equals(DEFAULT_PFP)) {
                return putProfilePictureWithUser(file, userService.getUserDTOByEntity(user));
            }
            String key = extractObjectKey(urlString);
            String newUrl;
            if (file == null) {
                newUrl = DEFAULT_PFP;
                deleteObject(key);
            } else {
                newUrl = putObjectWithKey(file, key);
            }
            user.setProfilePictureUrlString(newUrl);
            user = userService.saveEntity(user);
            return userService.getUserDTOByEntity(user);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    /**
     * Delete the associated profile picture object in S3, that pertains
     * to a given `userId`
     */
    @Override
    public void deleteObjectByUserId(UUID userId) {
        try {
            User user = UserMapper.toEntity(userService.getUserById(userId));
            String urlString = user.getProfilePictureUrlString();
            deleteObjectByURL(urlString);
            user.setProfilePictureUrlString(null);
            userService.saveEntity(user);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    public String getDefaultProfilePicture() {
        return DEFAULT_PFP;
    }


    @Override
    public void deleteObjectByURL(String urlString) {
        try {
            if (urlString == null) return;
            String key = extractObjectKey(urlString);
            deleteObject(key);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    /**
     * Deletes an object given the key (where it's stored)
     */
    private void deleteObject(String key) {
        if (key == null || key.equals(extractObjectKey(DEFAULT_PFP)))
            return; // Don't delete the default pfp! It is shared among many users

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(BUCKET)
                .key(key)
                .build();
        try {
            s3.deleteObject(request);
        } catch (Exception e) {
            logger.error(e.getMessage()); // TODO: decide correct behaviour
            throw e;
        }
    }


    /**
     * @param url - the profile picture url string
     * @return the object key part of the profile picture url string
     */
    // url is of the form: <cdn-base>/<object-key>
    private String extractObjectKey(String url) {
        try {
            return url.substring(url.lastIndexOf("/") + 1);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new ApplicationException("Invalid URL");
        }
    }
}
