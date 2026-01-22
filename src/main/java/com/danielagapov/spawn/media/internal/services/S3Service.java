package com.danielagapov.spawn.media.internal.services;

import com.danielagapov.spawn.shared.exceptions.ApplicationException;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Pure S3 storage service - handles S3 operations only.
 * Does NOT depend on UserService to avoid circular dependencies.
 * User entity management should be handled by UserService.
 */
@Service
@Profile("!test") // Don't load this service in test profile
public class S3Service implements IS3Service {
    private static final String BUCKET = "spawn-pfp-store";
    private static final String CDN_BASE;
    private static final String DEFAULT_PFP;
    
    // Security constants
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp",
        "image/heic", "image/heif" // iOS native formats
    );
    private static final List<byte[]> MALICIOUS_SIGNATURES = Arrays.asList(
        // PHP signatures
        new byte[]{'<', '?', 'p', 'h', 'p'},
        new byte[]{'<', '?', 'P', 'H', 'P'},
        // Script signatures  
        new byte[]{'<', 's', 'c', 'r', 'i', 'p', 't'},
        new byte[]{'<', 'S', 'C', 'R', 'I', 'P', 'T'}
    );
    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");

    static {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        DEFAULT_PFP = dotenv.get("DEFAULT_PFP");
        CDN_BASE = dotenv.get("CDN_BASE");
    }

    private final S3Client s3;
    private final ILogger logger;

    public S3Service(S3Client s3, ILogger logger) {
        this.s3 = s3;
        this.logger = logger;
    }

    /**
     * Validates file security before upload
     */
    private void validateFileUpload(byte[] file, String contentType) {
        // Validate file size
        if (file == null || file.length == 0) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }
        
        if (file.length > MAX_FILE_SIZE) {
            logger.warn("File upload rejected: size " + file.length + " exceeds limit " + MAX_FILE_SIZE);
            throw new IllegalArgumentException("File size exceeds maximum allowed size of 5MB");
        }

        // Validate content type
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            logger.warn("File upload rejected: invalid content type " + contentType);
            throw new IllegalArgumentException("Invalid file type. Only JPEG, PNG, GIF, WebP, and HEIC/HEIF images are allowed");
        }

        // Check for malicious content signatures
        if (containsMaliciousContent(file)) {
            logger.warn("File upload rejected: potentially malicious content detected");
            throw new SecurityException("File contains potentially malicious content");
        }

        // Validate image file headers (magic numbers)
        if (!isValidImageFile(file, contentType)) {
            logger.warn("File upload rejected: file content does not match declared type");
            throw new IllegalArgumentException("File content does not match the declared image type");
        }
    }

    /**
     * Checks for malicious content signatures in the file
     */
    private boolean containsMaliciousContent(byte[] file) {
        for (byte[] signature : MALICIOUS_SIGNATURES) {
            if (containsSequence(file, signature)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates image file headers to ensure file is actually an image
     */
    private boolean isValidImageFile(byte[] file, String contentType) {
        if (file.length < 12) return false;
        
        switch (contentType.toLowerCase()) {
            case "image/jpeg":
            case "image/jpg":
                return file[0] == (byte) 0xFF && file[1] == (byte) 0xD8;
            case "image/png":
                return file[0] == (byte) 0x89 && file[1] == 'P' && file[2] == 'N' && file[3] == 'G';
            case "image/gif":
                return (file[0] == 'G' && file[1] == 'I' && file[2] == 'F');
            case "image/webp":
                return file[8] == 'W' && file[9] == 'E' && file[10] == 'B' && file[11] == 'P';
            case "image/heic":
            case "image/heif":
                // HEIC/HEIF uses ISO Base Media File Format with 'ftyp' box at bytes 4-7
                // followed by brand identifier (heic, heix, mif1, etc.)
                return file[4] == 'f' && file[5] == 't' && file[6] == 'y' && file[7] == 'p';
            default:
                return false;
        }
    }

    /**
     * Helper method to check if byte array contains a specific sequence
     */
    private boolean containsSequence(byte[] array, byte[] sequence) {
        for (int i = 0; i <= array.length - sequence.length; i++) {
            boolean found = true;
            for (int j = 0; j < sequence.length; j++) {
                if (array[i + j] != sequence[j]) {
                    found = false;
                    break;
                }
            }
            if (found) return true;
        }
        return false;
    }

    /**
     * Sanitizes filename to prevent path traversal and injection attacks
     */
    private String sanitizeKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return UUID.randomUUID().toString();
        }
        
        // Remove path traversal attempts
        key = key.replaceAll("\\.\\./", "").replaceAll("\\\\", "");
        
        // Keep only safe characters
        if (!SAFE_FILENAME_PATTERN.matcher(key).matches()) {
            // If key contains unsafe characters, generate a new UUID
            key = UUID.randomUUID().toString();
        }
        
        return key;
    }

    /**
     * This is the closest method directly to our S3Client, which puts an object to
     * our S3 bucket, given a key to map it to
     * <p>
     * Returns the profile picture url string where it's now hosted through a CDN
     */
    @Override
    public String putObjectWithKey(byte[] file, String key) {
        return putObjectWithKey(file, key, "image/jpeg"); // Default to JPEG
    }

    /**
     * Enhanced version with content type validation
     */
    public String putObjectWithKey(byte[] file, String key, String contentType) {
        // Validate file upload security
        validateFileUpload(file, contentType);
        
        // Sanitize the key
        String sanitizedKey = sanitizeKey(key);
        
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(BUCKET)
                .key(sanitizedKey)
                .contentType(contentType)
                .serverSideEncryption("AES256") // Enable server-side encryption
                .build();
        try {
            logger.info("Uploading file with key: " + sanitizedKey + ", size: " + file.length + " bytes");
            s3.putObject(request, RequestBody.fromBytes(file));
            return CDN_BASE + sanitizedKey;
        } catch (Exception e) {
            logger.error("Failed to upload file: " + e.getMessage());
            throw new ApplicationException("Failed to upload file to storage", e);
        }
    }

    /**
     * Puts object to S3, by mapping a file to a randomly generated key
     */
    @Override
    public String putObject(byte[] file) {
        return putObject(file, "image/jpeg"); // Default to JPEG
    }

    /**
     * Enhanced version with content type validation
     */
    public String putObject(byte[] file, String contentType) {
        try {
            String key = UUID.randomUUID().toString();
            return putObjectWithKey(file, key, contentType);
        } catch (Exception e) {
            logger.error("Failed to upload file: " + e.getMessage());
            throw new ApplicationException("Failed to upload file to storage", e);
        }
    }

    @Override
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

    @Override
    public String uploadProfilePicture(byte[] file, UUID userId) {
        try {
            if (file == null) {
                return DEFAULT_PFP;
            }
            return putObjectWithKey(file, userId.toString());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    public boolean isDefaultProfilePicture(String urlString) {
        if (urlString == null || DEFAULT_PFP == null) {
            return false;
        }
        return urlString.equals(DEFAULT_PFP);
    }

    public static String getDefaultProfilePictureUrlString() {
        return DEFAULT_PFP;
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
            logger.error(e.getMessage());
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
