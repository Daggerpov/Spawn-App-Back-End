package com.danielagapov.spawn.Services.S3;


public interface IS3Service {
    String putObject(byte[] file, String key);
    byte[] getObject(String key);
    void deleteObject(String key);
    String putObject(byte[] file);
    String getPresignedURL(String key);
}
