package com.danielagapov.spawn.UserTests;

import com.danielagapov.spawn.Controllers.FriendTagController;
import com.danielagapov.spawn.Controllers.UserController;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.FriendTag;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IFriendTagRepository;
import com.danielagapov.spawn.Repositories.IUserFriendTagRepository;
import com.danielagapov.spawn.Repositories.IUserRepository;
import com.danielagapov.spawn.Services.User.IUserService;

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
    @Autowired
    private IUserFriendTagRepository uftRepository;
    @Autowired
    private IUserRepository userRepository;
    @Autowired
    private IFriendTagRepository ftRepository;

    private User user1;
    private User user2;

    private UserDTO user1DTO;
    private UserDTO user2DTO;

    @Autowired
    private UserController userController;
    @Autowired
    private FriendTagController friendTagController;

    @Autowired
    private IUserService userService;

    private FriendTag user1All;
    private FriendTag user2All;

    @BeforeEach
    void setup() {
        user1All = new FriendTag(UUID.randomUUID(), "all", "#ffffff", null, null);
        user2All = new FriendTag(UUID.randomUUID(), "all", "#ffffff", null, null);
        user1 = new User(UUID.randomUUID(), "username1", "examplepfp",
                "John", "Doe", "I like turtles.", Arrays.asList(user1All) ,"john@doe.com");
        user2 = new User(UUID.randomUUID(), "username2", "examplepfp2",
                "Jane", "Does", "I like baboons.", Arrays.asList(user2All), "jane@does.com");

        user1All.setOwner(user1);
        user2All.setOwner(user2);
        user1All.setFriends(Arrays.asList(user1));
        user2All.setFriends(Arrays.asList(user2));

        System.out.println("saving user1All: " + user1All.getId());
        System.out.println("saving user2All: " + user2All.getId());
        System.out.println("saving user1: " + user1.getId());
        System.out.println("saving user2: " + user2.getId());

        //userService.saveUser(user1DTO);
        //userService.saveUser(user2DTO);

        ftRepository.save(user1All);
        System.out.println("saved user1All: " + user1All.getId());
        //ftRepository.save(user2All);
        System.out.println("saved user2All: " + user2All.getId());
        //userRepository.save(user1);
        System.out.println("saved user1: " + user1.getId());
        //userRepository.save(user2);
        System.out.println("saved user2: " + user2.getId());

        user1DTO = UserMapper.toDTO(user1, uftRepository, userRepository);
        user2DTO = UserMapper.toDTO(user2, uftRepository, userRepository);
    }

    @Test
    void createUserCreatesUserProperly() {
        // also tests DTO
        /*
        User responseUser = UserMapper.toEntity(this.restTemplate.getForObject("http://localhost:" + port + "/" + user1DTO.id(),
                UserDTO.class), userRepository);

        assertEquals(responseUser.getId(), user1.getId());
        assertEquals(responseUser.getUsername(), user1.getUsername());
        assertEquals(responseUser.getProfilePicture(), user1.getProfilePicture());
        assertEquals(responseUser.getFirstName(), user1.getFirstName());
        assertEquals(responseUser.getLastName(), user1.getLastName());
        assertEquals(responseUser.getBio(), user1.getBio());
        responseUser.getFriendTags().stream()
                        .forEach(friendTag ->
                            assertEquals(friendTag.getId(), user1All.getId()));
        assertEquals(responseUser.getEmail(), user1.getEmail());*/
    }
}
