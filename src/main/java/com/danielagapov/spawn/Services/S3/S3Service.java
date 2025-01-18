package com.danielagapov.spawn.Services.S3;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3Service implements IS3Service {
    private final S3Client s3;
    private static final String BUCKET = "spawn-pfp-store";

    public S3Service(S3Client s3) {
        this.s3 = s3;
    }

    /**
     * @param file
     * @param key
     */
    public void putObject(byte[] file, String key) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(BUCKET)
                .key(key)
                .build();
        try {
            s3.putObject(request, RequestBody.fromBytes(file));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * @param key
     * @return
     */
    public byte[] getObject(String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(BUCKET)
                .key(key)
                .build();
        try {
            ResponseInputStream<GetObjectResponse> response = s3.getObject(request);
            return response.readAllBytes();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * @param key
     */
    public void deleteObject(String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(BUCKET)
                .key(key)
                .build();

        try {
            s3.deleteObject(request);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
