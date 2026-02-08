package com.danielagapov.spawn.media.internal.services;

/**
 * Stub S3 service for the auth microservice.
 * Only provides the default profile picture URL constant.
 * Full S3 operations are handled by the media service.
 */
public class S3Service {

    private static final String DEFAULT_PFP = "https://spawn-app-bucket.s3.us-east-2.amazonaws.com/default_pfp.png";

    public static String getDefaultProfilePictureUrlString() {
        return DEFAULT_PFP;
    }
}
