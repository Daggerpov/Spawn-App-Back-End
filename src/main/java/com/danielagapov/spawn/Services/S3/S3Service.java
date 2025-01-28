package com.danielagapov.spawn.Services.S3;

import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Exceptions.ApplicationException;
import com.danielagapov.spawn.Helpers.Logger.ILogger;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Services.User.UserService;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import java.util.UUID;


@Service
public class S3Service implements IS3Service {
    private static final String BUCKET = "spawn-pfp-store";
    private static final String CDN_BASE = Dotenv.load().get("CDN_BASE");
    private final S3Client s3;
    private final ILogger logger;
    private final UserService userService;

    public S3Service(S3Client s3, ILogger logger, UserService userService) {
        this.s3 = s3;
        this.logger = logger;
        this.userService = userService;
    }


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
            logger.log(e.getMessage()); // TODO: decide correct behaviour
            throw e;
        }
    }

    public String putObject(byte[] file) {
        String key = UUID.randomUUID().toString();
        return putObjectWithKey(file, key);
    }

    public UserDTO putObjectWithUser(byte[] file, UserDTO user) {
        return new UserDTO(
                user.id(),
                user.friendIds(),
                user.username(),
                putObject(file),
                user.firstName(),
                user.lastName(),
                user.bio(),
                user.friendTagIds(),
                user.email()
        );
    }

    public UserDTO updateProfilePicture(byte[] file, UUID id) {
        User user = UserMapper.toEntity(userService.getUserById(id));
        String urlString = user.getProfilePicture();
        String key = extractObjectKey(urlString);
        String newUrl = putObjectWithKey(file, key);
        user.setProfilePicture(newUrl);
        user = userService.saveEntity(user);
        return userService.getUserById(user.getId()); // because converting user -> dto is hard
    }

    public void deleteObjectByUserId(UUID id) {
        User user = UserMapper.toEntity(userService.getUserById(id));
        String urlString = user.getProfilePicture();
        String key = extractObjectKey(urlString);
        deleteObject(key);
    }

    private void deleteObject(String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(BUCKET)
                .key(key)
                .build();

        try {
            s3.deleteObject(request);
        } catch (Exception e) {
            logger.log(e.getMessage()); // TODO: decide correct behaviour
            throw e;
        }
    }


    // url is of the form: <cdn-base>/<object-key>
    private String extractObjectKey(String url) {
        try {
            return url.substring(url.lastIndexOf("/") + 1);
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw new ApplicationException("Invalid URL");
        }
    }
}
