package com.danielagapov.spawn.Services.ActivityType.DefaultActivityType;

import com.danielagapov.spawn.Models.ActivityType;
import com.danielagapov.spawn.Models.User.User;
import org.springframework.stereotype.Component;

@Component
public class DefaultGymActivityType implements IDefaultActivityType {

    @Override
    public ActivityType getDefaultActivityType(User user) {
        return new ActivityType(user, "Active", "🏃");
    }
}
