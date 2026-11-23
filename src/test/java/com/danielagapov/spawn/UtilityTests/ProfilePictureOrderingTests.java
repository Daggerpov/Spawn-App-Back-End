package com.danielagapov.spawn.UtilityTests;

import com.danielagapov.spawn.media.internal.services.S3Service;
import com.danielagapov.spawn.shared.util.ProfilePictureOrdering;
import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfilePictureOrderingTests {

    private static BaseUserDTO user(String username, String picture) {
        return new BaseUserDTO(UUID.randomUUID(), username, username + "@example.com", username, "", picture);
    }

    @Test
    void customPicturesComeFirstAndOrderIsOtherwiseKept() {
        String defaultPicture = S3Service.getDefaultProfilePictureUrlString();
        List<BaseUserDTO> users = List.of(
                user("noPicture", null),
                user("photoA", "https://cdn.example.com/a.jpg"),
                user("blank", " "),
                user("photoB", "https://cdn.example.com/b.jpg"),
                user("defaultPicture", defaultPicture)
        );

        List<String> order = ProfilePictureOrdering.customPicturesFirst(users).stream()
                .map(BaseUserDTO::getUsername)
                .toList();

        assertEquals(List.of("photoA", "photoB", "noPicture", "blank", "defaultPicture"), order);
    }

    @Test
    void defaultPictureIsNotCustom() {
        assertFalse(ProfilePictureOrdering.hasCustomPicture(null));
        assertFalse(ProfilePictureOrdering.hasCustomPicture(""));
        assertTrue(ProfilePictureOrdering.hasCustomPicture("https://cdn.example.com/a.jpg"));
        String defaultPicture = S3Service.getDefaultProfilePictureUrlString();
        if (defaultPicture != null) {
            assertFalse(ProfilePictureOrdering.hasCustomPicture(defaultPicture));
        }
    }
}
