package com.risheek.resume_screener.scheduler;

import com.risheek.resume_screener.entity.Job;
import com.risheek.resume_screener.entity.NotificationType;
import com.risheek.resume_screener.repository.JobRepository;
import com.risheek.resume_screener.service.NotificationService;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

// Jobs are created with a future applicationStartsAt, but applications only
// truly "open" once that moment arrives. This periodically checks for jobs
// that have just crossed that threshold and sends the JOB_OPEN_FOR_APPLY
// broadcast notification exactly once per job.
@Component
public class JobOpenNotificationScheduler {

    private final JobRepository jobRepository;
    private final NotificationService notificationService;

    public JobOpenNotificationScheduler(JobRepository jobRepository, NotificationService notificationService) {
        this.jobRepository = jobRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedRate = 5 * 60 * 1000) // every 5 minutes
    @Transactional
    public void notifyJobsNowOpenForApplications() {
        List<Job> newlyOpenJobs = jobRepository
                .findByOpenNotificationSentFalseAndApplicationStartsAtLessThanEqual(LocalDateTime.now());

        for (Job job : newlyOpenJobs) {
            notificationService.notifyAllUsers(
                    NotificationType.JOB_OPEN_FOR_APPLY,
                    "Applications are open: " + job.getTitle(),
                    "You can now submit your application for " + job.getTitle() + ".",
                    job.getId()
            );

            job.setOpenNotificationSent(true);
            jobRepository.save(job);
        }
    }
}
