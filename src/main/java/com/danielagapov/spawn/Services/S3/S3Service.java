package com.danielagapov.spawn.Services.S3;

import com.danielagapov.spawn.Helpers.Logger.ILogger;
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

@Service
public class S3Service implements IS3Service {
    private final S3Client s3;
    private static final String BUCKET = "spawn-pfp-store";
    private final ILogger logger;

    public S3Service(S3Client s3, ILogger logger) {
        this.s3 = s3;
        this.logger = logger;
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
