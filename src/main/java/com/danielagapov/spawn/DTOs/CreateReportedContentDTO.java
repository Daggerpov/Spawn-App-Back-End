package com.danielagapov.spawn.DTOs;

import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.ReportType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateReportedContentDTO implements Serializable {
    private UUID reporterUserId;
    private UUID contentId;
    private EntityType contentType;
    private ReportType reportType;
    private String description;
} 