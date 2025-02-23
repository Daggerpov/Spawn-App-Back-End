package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.ReportedContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IReportedContentRepository extends JpaRepository<ReportedContent, UUID> {
}
