package com.danielagapov.spawn.Services.S3;


import com.danielagapov.spawn.DTOs.UserDTO;

import java.util.UUID;

public interface IS3Service {
    String putObjectWithKey(byte[] file, String key);
    void deleteObjectByUserId(UUID userId);
    String putObject(byte[] file);
    UserDTO putProfilePictureWithUser(byte[] file, UserDTO user);
    UserDTO updateProfilePicture(byte[] file, UUID userId);
}
