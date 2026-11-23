package com.danielagapov.spawn.shared.util;

import com.danielagapov.spawn.media.internal.services.S3Service;
import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import com.danielagapov.spawn.user.internal.domain.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Lists of people look emptier when they open with the default Spawn picture, so people
 * who uploaded their own photo go first. The sort is stable: within each group the
 * existing order (score, recency, etc.) is kept.
 */
public final class ProfilePictureOrdering {

    private ProfilePictureOrdering() {}

    public static boolean hasCustomPicture(String profilePictureUrl) {
        return profilePictureUrl != null
                && !profilePictureUrl.isBlank()
                && !profilePictureUrl.equals(S3Service.getDefaultProfilePictureUrlString());
    }

    /** Sorts DTOs with a custom picture before those with the default (or none). */
    public static final Comparator<BaseUserDTO> CUSTOM_PICTURE_FIRST =
            Comparator.comparing((BaseUserDTO user) -> !hasCustomPicture(user.getProfilePicture()));

    /** Same ordering for user entities. */
    public static final Comparator<User> CUSTOM_PICTURE_FIRST_USERS =
            Comparator.comparing((User user) -> !hasCustomPicture(user.getProfilePictureUrlString()));

    public static <T extends BaseUserDTO> List<T> customPicturesFirst(List<T> users) {
        List<T> sorted = new ArrayList<>(users);
        sorted.sort(CUSTOM_PICTURE_FIRST);
        return sorted;
    }
}
