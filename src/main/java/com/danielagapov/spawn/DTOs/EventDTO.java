package com.danielagapov.spawn.DTOs;


import java.io.Serializable;
import java.util.List;

public record EventDTO(
        Long id,
        String title,
        String startTime, // TODO: investigate data type later
        String endTime, // TODO: investigate data type later
        String location, // TODO: investigate data type later
        String note,
        UserDTO creator,
        List<UserDTO> participants,
        List<UserDTO> invited,
        List<ChatMessageDTO> chatMessages
) implements Serializable {}