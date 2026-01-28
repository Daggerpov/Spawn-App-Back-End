package com.danielagapov.spawn.analytics.internal.repositories;

import com.danielagapov.spawn.analytics.internal.domain.FeedbackSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IFeedbackSubmissionRepository extends JpaRepository<FeedbackSubmission, UUID> {
}
