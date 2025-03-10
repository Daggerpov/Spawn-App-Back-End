package com.danielagapov.spawn.Repositories;

import com.danielagapov.spawn.Models.FeedbackSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IFeedbackSubmissionRepository extends JpaRepository<FeedbackSubmission, UUID> {
}
