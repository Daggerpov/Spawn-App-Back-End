package com.danielagapov.spawn.Services;

import com.danielagapov.spawn.DTOs.User.Profile.CreateUserInterestDTO;
import com.danielagapov.spawn.DTOs.User.Profile.UserInterestDTO;
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
public class UserInterestService {

    private final UserInterestRepository userInterestRepository;
    private final IUserRepository userRepository;

    @Autowired
    public UserInterestService(UserInterestRepository userInterestRepository, IUserRepository userRepository) {
        this.userInterestRepository = userInterestRepository;
        this.userRepository = userRepository;
    }

    public List<UserInterestDTO> getUserInterests(UUID userId) {
        List<UserInterest> interests = userInterestRepository.findByUserId(userId);
        return interests.stream()
                .map(interest -> new UserInterestDTO(
                        interest.getId(),
                        interest.getUser().getId(),
                        interest.getInterest()
                ))
                .collect(Collectors.toList());
    }

    public UserInterestDTO addUserInterest(CreateUserInterestDTO createUserInterestDTO) {
        User user = userRepository.findById(createUserInterestDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + createUserInterestDTO.getUserId()));

        UserInterest userInterest = new UserInterest(user, createUserInterestDTO.getInterest());
        userInterest = userInterestRepository.save(userInterest);

        return new UserInterestDTO(
                userInterest.getId(),
                userInterest.getUser().getId(),
                userInterest.getInterest()
        );
    }
} 