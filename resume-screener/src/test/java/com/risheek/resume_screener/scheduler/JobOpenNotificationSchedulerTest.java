package com.risheek.resume_screener.scheduler;

import com.risheek.resume_screener.entity.Job;
import com.risheek.resume_screener.entity.NotificationType;
import com.risheek.resume_screener.repository.JobRepository;
import com.risheek.resume_screener.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobOpenNotificationSchedulerTest {

    @Mock
    private JobRepository jobRepository;
    @Mock
    private NotificationService notificationService;

    private JobOpenNotificationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new JobOpenNotificationScheduler(jobRepository, notificationService);
    }

    @Test
    void notifyJobsNowOpenForApplications_noNewlyOpenJobs_doesNothing() {
        when(jobRepository.findByOpenNotificationSentFalseAndApplicationStartsAtLessThanEqual(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        scheduler.notifyJobsNowOpenForApplications();

        verifyNoInteractions(notificationService);
        verify(jobRepository, never()).save(any());
    }

    @Test
    void notifyJobsNowOpenForApplications_newlyOpenJob_sendsNotificationAndMarksSent() {
        Job job = new Job();
        job.setId(1L);
        job.setTitle("Backend Engineer");
        job.setOpenNotificationSent(false);

        when(jobRepository.findByOpenNotificationSentFalseAndApplicationStartsAtLessThanEqual(any(LocalDateTime.class)))
                .thenReturn(List.of(job));

        scheduler.notifyJobsNowOpenForApplications();

        verify(notificationService).notifyAllUsers(
                eq(NotificationType.JOB_OPEN_FOR_APPLY),
                eq("Applications are open: Backend Engineer"),
                eq("You can now submit your application for Backend Engineer."),
                eq(1L)
        );
        org.junit.jupiter.api.Assertions.assertTrue(job.isOpenNotificationSent());
        verify(jobRepository).save(job);
    }

    @Test
    void notifyJobsNowOpenForApplications_multipleNewlyOpenJobs_notifiesEachAndSavesEach() {
        Job job1 = new Job();
        job1.setId(1L);
        job1.setTitle("Backend Engineer");

        Job job2 = new Job();
        job2.setId(2L);
        job2.setTitle("Frontend Engineer");

        when(jobRepository.findByOpenNotificationSentFalseAndApplicationStartsAtLessThanEqual(any(LocalDateTime.class)))
                .thenReturn(List.of(job1, job2));

        scheduler.notifyJobsNowOpenForApplications();

        verify(notificationService, times(2)).notifyAllUsers(eq(NotificationType.JOB_OPEN_FOR_APPLY), anyString(), anyString(), any());
        verify(jobRepository).save(job1);
        verify(jobRepository).save(job2);
    }
}