package com.danielagapov.spawn.UserTests;

import com.danielagapov.spawn.DTOs.FriendTagDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Services.FriendTag.FriendTagService;

import com.danielagapov.spawn.Services.User.UserService;
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

    @Autowired
    private UserService userService;
    @Autowired
    private FriendTagService ftService;

    private FriendTagDTO user1All;
    private FriendTagDTO user2All;

    @BeforeEach
    void setup() {
        user1All = new FriendTagDTO(UUID.randomUUID(), "all", "#ffffff", user1DTO, Arrays.asList(user2DTO));
        user2All = new FriendTagDTO(UUID.randomUUID(), "all", "#ffffff", user2DTO, Arrays.asList(user1DTO));
        user1DTO = new UserDTO(UUID.randomUUID(), Arrays.asList(user2DTO), "username1", "examplepfp",
                "John", "Doe", "I like turtles.", Arrays.asList(user1All), "john@doe.com");
        user2DTO = new UserDTO(UUID.randomUUID(), Arrays.asList(user1DTO), "username2", "examplepfp2",
                "Jane", "Does", "I like baboons.", Arrays.asList(user2All),"jane@does.com");
    }

    @Test
    void createUserCreatesUserProperly() {
        System.out.println("saving user1: " + user1DTO.id());
        UserDTO responseUser1 = userService.saveUser(user1DTO);
        System.out.println("saving user2: " + user2DTO.id());
        UserDTO responseUser2 = userService.saveUser(user2DTO);

        System.out.println("saving user1All: " + user1All.id());
        FriendTagDTO responseUser1All = ftService.saveFriendTag(user1All);
        System.out.println("saving user2All: " + user2All.id());
        FriendTagDTO responseUser2All = ftService.saveFriendTag(user2All);

        /*User responseUser = UserMapper.toEntity(this.restTemplate.getForObject("http://localhost:" + port + "/" + user1DTO.id(),
                UserDTO.class));*/

        assertEquals(responseUser1.id(), user1DTO.id());
        assertEquals(responseUser1.friends(), user1DTO.friends());
        assertEquals(responseUser1.username(), user1DTO.username());
        assertEquals(responseUser1.profilePicture(), user1DTO.profilePicture());
        assertEquals(responseUser1.firstName(), user1DTO.firstName());
        assertEquals(responseUser1.lastName(), user1DTO.lastName());
        assertEquals(responseUser1.bio(), user1DTO.bio());
        assertEquals(responseUser1.email(), user1DTO.email());
    }
}
