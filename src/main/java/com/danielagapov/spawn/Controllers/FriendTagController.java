package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.FriendTagDTO;
import com.danielagapov.spawn.Services.FriendTag.IFriendTagService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController()
@RequestMapping("api/v1/friendTags")
public class FriendTagController {
    private final IFriendTagService friendTagService;

    public FriendTagController(IFriendTagService friendTagService) {
        this.friendTagService = friendTagService;
    }

    // full path: /api/v1/friendTags
    @GetMapping
    public String getFriendTags() {
        return "These are the friendTags: " + friendTagService.getAllFriendTags();
    }

    // full path: /api/v1/friendTags/{id}
    @GetMapping("{id}")
    public FriendTagDTO getFriendTag(@PathVariable UUID id) {
        return friendTagService.getFriendTagById(id);
    }

    // full path: /api/v1/friendTags/mock-endpoint
    @GetMapping("mock-endpoint")
    public String getMockEndpoint() {
        return "This is the mock endpoint for friendTags. Everything is working with it.";
    }

    // full path: /api/v1/friendTags
    @PostMapping
    public FriendTagDTO createFriendTag(@RequestBody FriendTagDTO newFriendTag) {
        return friendTagService.saveFriendTag(newFriendTag);
    }

    // full path: /api/v1/friendTags/{id}
    @PutMapping("{id}")
    public FriendTagDTO replaceFriendTag(@RequestBody FriendTagDTO newFriendTag, @PathVariable UUID id) {
        return friendTagService.replaceFriendTag(newFriendTag, id);
    }
}







