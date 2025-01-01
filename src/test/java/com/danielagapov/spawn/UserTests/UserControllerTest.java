package com.danielagapov.spawn.UserTests;

import com.danielagapov.spawn.DTOs.FriendTagDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IFriendTagRepository;
import com.danielagapov.spawn.Repositories.IUserRepository;
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

    private UserDTO user2DTO;

    @Autowired
    private UserService userService;

    @Autowired
    private IUserRepository userRepository;
    @Autowired
    private IFriendTagRepository ftRepository;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
        ftRepository.deleteAll();
    }

    @Test
    void saveUserCreatesEmptyUserGivenTag() {
        // Note the null tags because Java Spring will generate them from the @GeneratedValue annotation in entities
        UserDTO user1DTO = new UserDTO(null, null, "username1", "examplepfp",
                "John", "Doe", "I like turtles.",
                Arrays.asList(new FriendTagDTO(null, "all", "#ffffff", null, null)),
                "john@doe.com");
        UserDTO responseUser1 = userService.saveUser(user1DTO);

        assertEquals(responseUser1.friends(), Arrays.asList());
        assertEquals(responseUser1.username(), user1DTO.username());
        assertEquals(responseUser1.profilePicture(), user1DTO.profilePicture());
        assertEquals(responseUser1.firstName(), user1DTO.firstName());
        assertEquals(responseUser1.lastName(), user1DTO.lastName());
        assertEquals(responseUser1.bio(), user1DTO.bio());
        assertEquals(responseUser1.email(), user1DTO.email());

        User responseUser = UserMapper.toEntity(this.restTemplate.getForObject("http://localhost:" + port + "/api/v1/users/" + responseUser1.id(),
                UserDTO.class));

        assertEquals(responseUser1.friendTags().get(0).id(), responseUser.getFriends());
        assertEquals(user1DTO.username(), responseUser.getUsername());
        assertEquals(user1DTO.profilePicture(), responseUser.getProfilePicture());
        assertEquals(user1DTO.firstName(), responseUser.getFirstName());
        assertEquals(user1DTO.lastName(), responseUser.getLastName());
        assertEquals(user1DTO.bio(), responseUser.getBio());
        assertEquals(user1DTO.email(), responseUser.getEmail());
    }

    @Test
    void saveUserCreatesEmptyUserNullTag() {
        UserDTO user1DTO = new UserDTO(null, null, "username1", "examplepfp",
                "John", "Doe", "I like turtles.",
                null,
                "john@doe.com");
        UserDTO responseUser1 = userService.saveUser(user1DTO);

        assertEquals(Arrays.asList(), responseUser1.friends());
        assertEquals(1, responseUser1.friendTags().size());
        assertEquals(user1DTO.username(), responseUser1.username());
        assertEquals(user1DTO.profilePicture(), responseUser1.profilePicture());
        assertEquals(user1DTO.firstName(), responseUser1.firstName());
        assertEquals(user1DTO.lastName(), responseUser1.lastName());
        assertEquals(user1DTO.bio(), responseUser1.bio());
        assertEquals(user1DTO.email(), responseUser1.email());

        User responseUser = UserMapper.toEntity(this.restTemplate.getForObject("http://localhost:" + port + "/api/v1/users/" + responseUser1.id(),
                UserDTO.class));

        assertEquals(responseUser1.friendTags().get(0).id(), responseUser.getFriends());
        assertEquals(user1DTO.username(), responseUser.getUsername());
        assertEquals(user1DTO.profilePicture(), responseUser.getProfilePicture());
        assertEquals(user1DTO.firstName(), responseUser.getFirstName());
        assertEquals(user1DTO.lastName(), responseUser.getLastName());
        assertEquals(user1DTO.bio(), responseUser.getBio());
        assertEquals(user1DTO.email(), responseUser.getEmail());
    }
}
