package com.danielagapov.spawn.UserTests;

import com.danielagapov.spawn.Controllers.UserController;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.FriendTag;
import com.danielagapov.spawn.Models.User;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserControllerTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private User user1;
    private User user2;

    private UserDTO user1DTO;
    private UserDTO user2DTO;

    private UserController controller;

    private FriendTag user1All;
    private FriendTag user2All;

    @BeforeEach
    void setup() {
        user1All = new FriendTag(UUID.randomUUID(), "all", "#ffffff", user1, Arrays.asList(user2));
        user2All = new FriendTag(UUID.randomUUID(), "all", "#ffffff", user2, Arrays.asList(user1));
        user1 = new User(UUID.randomUUID(), "username1", "examplepfp",
                "John", "Doe", "I like turtles.", Arrays.asList(user1All) ,"john@doe.com");
        user2 = new User(UUID.randomUUID(), "username2", "examplepfp2",
                "Jane", "Does", "I like baboons.", Arrays.asList(user2All), "jane@does.com");
        user1DTO = UserMapper.toDTO(user1);
        user2DTO = UserMapper.toDTO(user2);
    }

    @Test
    void createUserCreatesUserProperly() {
        controller.createUser(user1DTO);
        // also tests DTO
        User responseUser = UserMapper.toEntity(this.restTemplate.getForObject("http://localhost:" + port + "/" + user1DTO.id(),
                UserDTO.class));

        assertEquals(responseUser.getId(), user1.getId());
        assertEquals(responseUser.getUsername(), user1.getUsername());
        assertEquals(responseUser.getProfilePicture(), user1.getProfilePicture());
        assertEquals(responseUser.getFirstName(), user1.getFirstName());
        assertEquals(responseUser.getLastName(), user1.getLastName());
        assertEquals(responseUser.getBio(), user1.getBio());
        responseUser.getFriendTags().stream()
                        .forEach(friendTag ->
                            assertEquals(friendTag.getId(), user1All.getId()));
        assertEquals(responseUser.getEmail(), user1.getEmail());
    }
}
