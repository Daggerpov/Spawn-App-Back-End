package com.danielagapov.spawn.Services.S3;


import com.danielagapov.spawn.DTOs.UserDTO;

import java.util.UUID;

public interface IS3Service {
    String putObject(byte[] file, String key);
    byte[] getObject(String key);
    void deleteObject(String key);
    String putObject(byte[] file);
    String getPresignedURL(String key);
    UserDTO putObjectWithUser(byte[] file, UserDTO user);
    UserDTO updateProfilePicture(byte[] file, UUID id);
    String refreshURL(String key);
}
