package com.danielagapov.spawn.Services.UserInterest;

import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Models.User.Profile.UserInterest;
import com.danielagapov.spawn.Repositories.User.IUserRepository;
import com.danielagapov.spawn.Repositories.User.Profile.UserInterestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserInterestService implements IUserInterestService {

    private final UserInterestRepository userInterestRepository;
    private final IUserRepository userRepository;

    @Autowired
    public UserInterestService(UserInterestRepository userInterestRepository, IUserRepository userRepository) {
        this.userInterestRepository = userInterestRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<String> getUserInterests(UUID userId) {
        List<UserInterest> interests = userInterestRepository.findByUserId(userId);
        return interests.stream()
                .map(UserInterest::getInterest)
                .collect(Collectors.toList());
    }

    @Override
    public String addUserInterest(UUID userId, String interestName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        UserInterest userInterest = new UserInterest(user, interestName);
        userInterest = userInterestRepository.save(userInterest);

        return userInterest.getInterest();
    }
} 