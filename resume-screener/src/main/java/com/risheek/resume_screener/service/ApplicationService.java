package com.risheek.resume_screener.service;

import com.risheek.resume_screener.repository.*;
import org.springframework.stereotype.Service;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;

    private final ResumeRepository resumeRepository;

    private final JobRepository jobRepository;

    private final ScoreRepository scoreRepository;

    private final SuggestionService suggestionService;

    private final ReportService reportService;

    private final UserRepository userRepository;

    public ApplicationService(ApplicationRepository applicationRepository, ResumeRepository resumeRepository,
                              JobRepository jobRepository, ScoreRepository scoreRepository,
                              SuggestionService suggestionService, ReportService reportService, UserRepository userRepository){
        this.applicationRepository = applicationRepository;
        this.resumeRepository = resumeRepository;
        this.jobRepository =jobRepository;
        this.scoreRepository = scoreRepository;
        this.suggestionService = suggestionService;
        this.reportService = reportService;
        this.userRepository = userRepository;
    }

}

