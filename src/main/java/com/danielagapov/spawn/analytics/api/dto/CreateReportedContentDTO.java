package com.danielagapov.spawn.analytics.api.dto;

import com.danielagapov.spawn.shared.util.EntityType;
import com.danielagapov.spawn.shared.util.ReportType;
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