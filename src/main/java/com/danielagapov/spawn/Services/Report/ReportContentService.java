package com.danielagapov.spawn.Services.Report;

import com.danielagapov.spawn.Exceptions.Logger.Logger;
import com.danielagapov.spawn.Repositories.IReportedContentRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ReportContentService implements IReportContentService {
    private final IReportedContentRepository repository;
    private final Logger logger;
}
