package com.danielagapov.spawn.analytics.internal.repositories;

import com.danielagapov.spawn.analytics.internal.domain.FeedbackSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IFeedbackSubmissionRepository extends JpaRepository<FeedbackSubmission, UUID> {
}
