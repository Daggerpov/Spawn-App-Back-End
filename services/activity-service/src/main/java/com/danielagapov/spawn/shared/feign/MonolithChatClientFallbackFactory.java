package com.danielagapov.spawn.shared.feign;

import com.danielagapov.spawn.chat.api.dto.FullActivityChatMessageDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Fallback factory for MonolithChatClient.
 * Returns empty lists instead of throwing exceptions — chat messages are
 * non-critical and the activity feed should still work without them.
 */
@Component
public class MonolithChatClientFallbackFactory implements FallbackFactory<MonolithChatClient> {

    private static final Logger log = LoggerFactory.getLogger(MonolithChatClientFallbackFactory.class);

    @Override
    public MonolithChatClient create(Throwable cause) {
        return new MonolithChatClient() {

            @Override
            public List<FullActivityChatMessageDTO> getChatMessagesByActivityId(UUID activityId) {
                log.warn("Fallback: could not fetch chat messages for activity {}. Cause: {}", activityId, cause.getMessage());
                return Collections.emptyList();
            }
        };
    }
}
