package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.Models.FriendTag.FriendTag;
import com.danielagapov.spawn.Services.FriendTag.IFriendTagService;
import org.springframework.web.bind.annotation.*;

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
    public FriendTag getFriendTag(@PathVariable Long id) {
        return friendTagService.getFriendTagById(id);
    }

    // full path: /api/v1/friendTags
    @GetMapping("mock-endpoint")
    public String getMockEndpoint() {
        return "This is the mock endpoint for friendTags. Everything is working with it.";
    }

    // full path: /api/v1/friendTags
    @PostMapping
    public FriendTag createFriendTag(@RequestBody FriendTag newFriendTag) {
        return friendTagService.saveFriendTag(newFriendTag);
    }
}

