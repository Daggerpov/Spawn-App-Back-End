package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.Models.FriendTag;
import com.danielagapov.spawn.Services.FriendTag.IFriendTagService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController()
@RequestMapping("/api/v1/friendTags")
public class FriendTagController {
    private final IFriendTagService friendTagService;

    public FriendTagController(IFriendTagService friendTagService) {
        this.friendTagService = friendTagService;
    }

    @GetMapping("/")
    public String getFriendTags() {
        return "These are the friendTags: " + friendTagService.getAllFriendTags();
    }

    @GetMapping("/mock-endpoint")
    public String getMockEndpoint() {
        return "This is the mock endpoint for friendTags. Everything is working with it.";
    }

    @GetMapping("/{id}")
    public FriendTag getFriendTag(@PathVariable Long id) {
        return friendTagService.getFriendTagById(id);
    }

    // get friendTag by tag
    @GetMapping("/tag/{id}")
    public List<FriendTag> getFriendTagsByTagId(@PathVariable Long id) {
        return friendTagService.getFriendTagsByTagId(id);
    }

    @PostMapping("/")
    public FriendTag createFriendTag(@RequestBody FriendTag newFriendTag) {
        return friendTagService.saveFriendTag(newFriendTag);
    }
}

