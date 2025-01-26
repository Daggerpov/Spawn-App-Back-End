package com.danielagapov.spawn.Services.S3;

import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Helpers.Logger.ILogger;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Services.User.UserService;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class S3Service implements IS3Service {
    private final S3Client s3;
    private static final String BUCKET = "spawn-pfp-store";
    private final ILogger logger;
    private final UserService userService;

    public S3Service(S3Client s3, ILogger logger, UserService userService) {
        this.s3 = s3;
        this.logger = logger;
        this.userService = userService;
    }


    public String putObject(byte[] file, String key) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(BUCKET)
                .key(key)
                .contentType("image/jpeg")
                .build();
        try {
            s3.putObject(request, RequestBody.fromBytes(file));
            return getPresignedURL(key);
        } catch (Exception e) {
            logger.log(e.getMessage()); // TODO: decide correct behaviour
            throw e;
        }
    }

    public byte[] getObject(String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(BUCKET)
                .key(key)
                .build();
        try {
            ResponseInputStream<GetObjectResponse> response = s3.getObject(request);
            return response.readAllBytes();
        } catch (Exception e) {
            logger.log(e.getMessage()); // TODO: decide correct behaviour
            return null;
        }
    }

    public void deleteObject(String key) {
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

    public String putObject(byte[] file) {
        String key = UUID.randomUUID().toString();
        return putObject(file, key);
    }

    public String getPresignedURL(String key) {
        try (S3Presigner presigner = S3Presigner.builder().region(Region.US_WEST_2).build()) {
            GetObjectRequest getObjRequest = getObjectRequest(key);
            GetObjectPresignRequest presignRequest = getObjectPresignRequest(getObjRequest);

            PresignedGetObjectRequest urlRequest = presigner.presignGetObject(presignRequest);
            return urlRequest.url().toString();
        }
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
        String newUrl = putObject(file, key);
        user.setProfilePicture(newUrl);
        return userService.getUserById(userService.saveEntity(user).getId()); // because converting user -> dto is hard
    }

    public String refreshURL(String key) {
        return "";
    }

    // url is of the form: https://<base>/<object-key>?<headers>
    private String extractObjectKey(String url) {
        Pattern pattern = Pattern.compile("/([^/]*)\\?");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new IllegalArgumentException("Invalid URL: " + url);
        }
    }

    private GetObjectRequest getObjectRequest(String key) {
        return GetObjectRequest.builder()
                .bucket(BUCKET)
                .key(key).responseContentType("image/jpeg")
                .build();
    }

    private GetObjectPresignRequest getObjectPresignRequest(GetObjectRequest request) {
        return GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofDays(7)) // max duration
                .getObjectRequest(request)
                .build();
    }
}
