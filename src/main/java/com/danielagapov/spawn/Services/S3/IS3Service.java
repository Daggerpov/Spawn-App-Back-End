package com.danielagapov.spawn.Services.S3;


import com.danielagapov.spawn.DTOs.UserDTO;

import java.util.UUID;

public interface IS3Service {
    String putObjectWithKey(byte[] file, String key);
    void deleteObjectByUserId(UUID id);
    String putObject(byte[] file);
    UserDTO putObjectWithUser(byte[] file, UserDTO user);
    UserDTO updateProfilePicture(byte[] file, UUID id);
}
