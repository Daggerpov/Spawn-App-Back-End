package com.danielagapov.spawn.Services.ActivityType.DefaultActivityType;

import com.danielagapov.spawn.Models.ActivityType;
import com.danielagapov.spawn.Models.User.User;

public interface IDefaultActivityType {

    ActivityType getDefaultActivityType(User user);
}
