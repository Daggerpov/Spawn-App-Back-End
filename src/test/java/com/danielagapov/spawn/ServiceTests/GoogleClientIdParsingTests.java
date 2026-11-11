package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.auth.internal.services.GoogleOAuthStrategy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GoogleClientIdParsingTests {

    @Test
    void singleClientIdStillWorks() {
        assertEquals(List.of("ios.apps.googleusercontent.com"),
                GoogleOAuthStrategy.parseClientIds("ios.apps.googleusercontent.com"));
    }

    @Test
    void commaSeparatedClientIdsAreSplitTrimmedAndDeduplicated() {
        assertEquals(List.of("ios.apps.googleusercontent.com", "web.apps.googleusercontent.com"),
                GoogleOAuthStrategy.parseClientIds(" ios.apps.googleusercontent.com, web.apps.googleusercontent.com ,,ios.apps.googleusercontent.com"));
    }

    @Test
    void blankValueGivesNoClientIds() {
        assertEquals(List.of(), GoogleOAuthStrategy.parseClientIds(" , "));
        assertEquals(List.of(), GoogleOAuthStrategy.parseClientIds(null));
    }
}
