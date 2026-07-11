package com.risheek.resume_screener.service;

import com.risheek.resume_screener.dto.JobPageResponse;
import com.risheek.resume_screener.dto.JobRequest;
import com.risheek.resume_screener.dto.JobResponse;
import com.risheek.resume_screener.entity.Job;
import com.risheek.resume_screener.entity.JobSkill;
import com.risheek.resume_screener.entity.User;
import com.risheek.resume_screener.exception.InvalidApplicationWindowException;
import com.risheek.resume_screener.exception.JobNotFoundException;
import com.risheek.resume_screener.exception.UserNotFoundException;
import com.risheek.resume_screener.repository.ApplicationRepository;
import com.risheek.resume_screener.repository.JobRepository;
import com.risheek.resume_screener.repository.JobSkillRepository;
import com.risheek.resume_screener.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {
    @Mock
    private JobRepository jobRepository;
    @Mock
    private JobSkillRepository jobSkillRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ApplicationRepository applicationRepository;

    private JobService jobService;

    @BeforeEach
    void setUp() {
        jobService = new JobService(jobRepository, jobSkillRepository, userRepository, notificationService, applicationRepository);

        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication())
                .thenReturn(new UsernamePasswordAuthenticationToken("test@example.com", null));
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createJob_happyPath() {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        JobRequest request = new JobRequest();
        request.setTitle("Backend Developer");
        request.setDescription(".....");
        request.setExperienceLevel(Job.ExperienceLevel.JUNIOR);
        request.setJobType(Job.JobType.FULL_TIME);
        request.setSkills(List.of("Java", "Python", "C++"));
        request.setApplicationStartsAt(LocalDateTime.of(2026, 6, 26, 0, 0, 0 ));
        request.setApplicationDeadline(LocalDateTime.of(2026, 7, 1, 17, 0, 0 ));

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
            Job j = invocation.getArgument(0);
            j.setId(10L);
            return j;
        });

        JobSkill skill1 = new JobSkill(null, null, "Java");
        JobSkill skill2 = new JobSkill(null, null, "Python");
        JobSkill skill3 = new JobSkill(null, null, "C++");
        when(jobSkillRepository.findByJobId(10L)).thenReturn(List.of(skill1, skill2, skill3));

        JobResponse response = jobService.createJob(request);

        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(jobCaptor.capture());
        Job savedJob = jobCaptor.getValue();

        assertThat(savedJob.getTitle()).isEqualTo("Backend Developer");
        assertThat(savedJob.getUser()).isEqualTo(user);
        assertThat(savedJob.getJobType()).isEqualTo(Job.JobType.FULL_TIME);
        assertThat(savedJob.getExperienceLevel()).isEqualTo(Job.ExperienceLevel.JUNIOR);

        ArgumentCaptor<List<JobSkill>> skillsCaptor = ArgumentCaptor.forClass(List.class);
        verify(jobSkillRepository).saveAll(skillsCaptor.capture());
        List<JobSkill> capturedSkills = skillsCaptor.getValue();
        List<String> skillNames = capturedSkills.stream()
                .map(JobSkill::getSkillName)
                .toList();

        assertThat(skillNames).containsExactly("Java", "Python", "C++");

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("Backend Developer");
        assertThat(response.getSkills()).containsExactly("Java", "Python", "C++");
    }

    @Test
    void createJob_UserNotFound() {

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setEmail("test@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        JobRequest request = new JobRequest();
        request.setTitle("Backend Developer");
        request.setDescription(".....");
        request.setExperienceLevel(Job.ExperienceLevel.JUNIOR);
        request.setJobType(Job.JobType.FULL_TIME);
        request.setSkills(List.of("Java", "Python", "C++"));
        request.setApplicationStartsAt(LocalDateTime.of(2026, 6, 26, 0, 0, 0 ));
        request.setApplicationDeadline(LocalDateTime.of(2026, 7, 1, 17, 0, 0 ));


        assertThrows(UserNotFoundException.class,
                () -> jobService.createJob(request));

        verify(jobRepository, never()).save(any());

    }

    @Test
    void createJob_EmptySkills() {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        JobRequest request = new JobRequest();
        request.setTitle("Backend Developer");
        request.setDescription(".....");
        request.setExperienceLevel(Job.ExperienceLevel.JUNIOR);
        request.setJobType(Job.JobType.FULL_TIME);
        request.setSkills(List.of());
        request.setApplicationStartsAt(LocalDateTime.of(2026, 6, 26, 0, 0, 0 ));
        request.setApplicationDeadline(LocalDateTime.of(2026, 7, 1, 17, 0, 0 ));

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(jobRepository.save(any(Job.class)))
                .thenAnswer(invocation -> {
                    Job j = invocation.getArgument(0);
                    j.setId(10L);
                    return j;
                });

        when(jobSkillRepository.findByJobId(10L))
                .thenReturn(List.of());

        JobResponse response = jobService.createJob(request);

        verify(jobSkillRepository, never()).saveAll(any());

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getSkills()).isEmpty();
    }

    @Test
    void updateJob_HappyPath() {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");


        Job job = new Job();
        job.setUser(user);
        job.setId(10L);
        job.setTitle("Backend Developer");
        job.setDescription(".....");
        job.setExperienceLevel(Job.ExperienceLevel.JUNIOR);
        job.setJobType(Job.JobType.FULL_TIME);
        job.setApplicationStartsAt(LocalDateTime.of(2026, 6, 26, 0, 0, 0 ));
        job.setApplicationDeadline(LocalDateTime.of(2026, 7, 1, 17, 0, 0 ));

        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));

        JobRequest request = new JobRequest();
        request.setJobType(Job.JobType.FULL_TIME);
        request.setTitle("Backend Developer");
        request.setDescription("Need Backend Developer");
        request.setExperienceLevel(Job.ExperienceLevel.SENIOR);
        request.setSkills(List.of("Java", "Spring Boot"));
        request.setApplicationStartsAt(LocalDateTime.of(2026, 6, 26, 0, 0, 0 ));
        request.setApplicationDeadline(LocalDateTime.of(2026, 7, 11, 17, 0, 0 ));

        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
            Job j = invocation.getArgument(0);
            return j;
        });

        JobResponse response = jobService.updateJob(10L, request);

        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(jobCaptor.capture());
        verify(jobSkillRepository).deleteByJobId(10L);

        Job savedJob = jobCaptor.getValue();

        ArgumentCaptor<List<JobSkill>> skillsCaptor =
                ArgumentCaptor.forClass(List.class);

        verify(jobSkillRepository).saveAll(skillsCaptor.capture());

        List<JobSkill> savedSkills = skillsCaptor.getValue();

        assertThat(savedSkills)
                .extracting(JobSkill::getSkillName)
                .containsExactly("Java", "Spring Boot");


        assertThat(savedJob.getTitle()).isEqualTo("Backend Developer");
        assertThat(savedJob.getDescription()).isEqualTo("Need Backend Developer");
        assertThat(savedJob.getJobType()).isEqualTo(Job.JobType.FULL_TIME);
        assertThat(savedJob.getExperienceLevel()).isEqualTo(Job.ExperienceLevel.SENIOR);
        assertThat(savedJob.getUser()).isEqualTo(user);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("Backend Developer");

        assertThat(savedJob.getApplicationStartsAt())
                .isEqualTo(request.getApplicationStartsAt());

        assertThat(savedJob.getApplicationDeadline())
                .isEqualTo(request.getApplicationDeadline());

    }

    @Test
    void updateJob_JobNotFound() {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");


        Job job = new Job();
        job.setUser(user);
        job.setId(10L);

        when(jobRepository.findById(10L)).thenReturn(Optional.empty());

        JobRequest request = new JobRequest();
        request.setJobType(Job.JobType.FULL_TIME);
        request.setTitle("Backend Developer");
        request.setDescription("Need Backend Developer");
        request.setExperienceLevel(Job.ExperienceLevel.SENIOR);
        request.setSkills(List.of("Java", "Spring Boot"));
        request.setApplicationStartsAt(LocalDateTime.of(2026, 6, 26, 0, 0, 0 ));
        request.setApplicationDeadline(LocalDateTime.of(2026, 7, 17, 17, 0, 0 ));

        assertThrows(JobNotFoundException.class,
                () -> jobService.updateJob(10L, request));

        verify(jobRepository, never()).save(any());

    }

    @Test
    void updateJob_EmptySkills() {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        Job job = new Job();
        job.setId(10L);
        job.setUser(user);
        job.setTitle("Backend Developer");

        when(jobRepository.findById(10L))
                .thenReturn(Optional.of(job));

        when(jobRepository.save(any(Job.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(jobSkillRepository.findByJobId(10L))
                .thenReturn(List.of());

        JobRequest request = new JobRequest();
        request.setTitle("Updated Backend Developer");
        request.setDescription("Updated Description");
        request.setJobType(Job.JobType.FULL_TIME);
        request.setExperienceLevel(Job.ExperienceLevel.SENIOR);
        request.setSkills(List.of());
        request.setApplicationStartsAt(LocalDateTime.of(2026, 6, 26, 0, 0, 0 ));
        request.setApplicationDeadline(LocalDateTime.of(2026, 7, 1, 17, 0, 0 ));

        JobResponse response = jobService.updateJob(10L, request);

        verify(jobSkillRepository).deleteByJobId(10L);
        verify(jobSkillRepository, never()).saveAll(any());

        assertThat(response.getTitle())
                .isEqualTo("Updated Backend Developer");

        assertThat(response.getSkills()).isEmpty();
    }

    @Test
    void deleteJob_HappyPath(){

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");


        Job job = new Job();
        job.setUser(user);
        job.setId(10L);

        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));

        jobService.deleteJob(10L);

        verify(jobSkillRepository).deleteByJobId(10L);
        verify(jobRepository).delete(job);

    }

    @Test
    void deleteJob_JobNotFound(){

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");


        Job job = new Job();
        job.setUser(user);
        job.setId(10L);

        when(jobRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(JobNotFoundException.class,
                () -> jobService.deleteJob(10L));

        verify(jobRepository, never()).delete(any(Job.class));

    }

    @Test
    void getJobById_happyPath(){

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        Job job = new Job();
        job.setUser(user);
        job.setId(10L);
        job.setTitle("Backend Developer");
        job.setDescription(".....");
        job.setExperienceLevel(Job.ExperienceLevel.SENIOR);
        job.setJobType(Job.JobType.FULL_TIME);
        job.setApplicationStartsAt(LocalDateTime.of(2026, 6, 26, 0, 0, 0 ));
        job.setApplicationDeadline(LocalDateTime.of(2026, 7, 1, 17, 0, 0 ));

        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));

        when(jobSkillRepository.findByJobId(10L))
                .thenReturn(List.of(
                        new JobSkill(null, job, "Java"),
                        new JobSkill(null, job, "Spring Boot")
                ));

        JobResponse savedJob = jobService.getJobById(10L);

        assertThat(savedJob.getTitle()).isEqualTo("Backend Developer");
        assertThat(savedJob.getDescription()).isEqualTo(".....");
        assertThat(savedJob.getJobType()).isEqualTo(Job.JobType.FULL_TIME);
        assertThat(savedJob.getExperienceLevel()).isEqualTo(Job.ExperienceLevel.SENIOR);
        assertThat(savedJob.getSkills())
                .containsExactly("Java", "Spring Boot");
    }

    @Test
    void getJob_JobNotFound(){

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");


        Job job = new Job();
        job.setUser(user);
        job.setId(10L);

        when(jobRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(JobNotFoundException.class,
                () -> jobService.getJobById(10L));
    }

    @Test
    void getAllJobs_happyPath() {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        Job job1 = new Job();
        job1.setUser(user);
        job1.setId(10L);
        job1.setTitle("Backend Developer");
        job1.setDescription(".....");
        job1.setExperienceLevel(Job.ExperienceLevel.JUNIOR);
        job1.setJobType(Job.JobType.INTERNSHIP);
        job1.setApplicationStartsAt(LocalDateTime.of(2026, 6, 26, 0, 0, 0 ));
        job1.setApplicationDeadline(LocalDateTime.of(2026, 7, 1, 17, 0, 0 ));

        Job job2 = new Job();
        job2.setUser(user);
        job2.setId(11L);
        job2.setTitle("Frontend Developer");
        job2.setDescription(".....");
        job2.setExperienceLevel(Job.ExperienceLevel.MID);
        job2.setJobType(Job.JobType.PART_TIME);
        job2.setApplicationStartsAt(LocalDateTime.of(2026, 6, 26, 0, 0, 0 ));
        job2.setApplicationDeadline(LocalDateTime.of(2026, 7, 1, 17, 0, 0 ));

        Page<Job> page = new PageImpl<>(List.of(job1, job2));

        when(jobSkillRepository.findByJobId(10L))
                .thenReturn(List.of(
                        new JobSkill(null, job1, "Java")
                ));

        when(jobSkillRepository.findByJobId(11L))
                .thenReturn(List.of(
                        new JobSkill(null, job2, "React")
                ));

        when(jobRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        JobPageResponse jobs = jobService.getAllJobs(0, 10, null, null, null, null);

        assertThat(jobs.getContent()).hasSize(2);

        assertThat(jobs.getContent().get(0).getTitle())
                .isEqualTo("Backend Developer");

        assertThat(jobs.getContent().get(1).getTitle())
                .isEqualTo("Frontend Developer");
        assertThat(jobs.getContent().get(0).getSkills())
                .containsExactly("Java");

        assertThat(jobs.getContent().get(1).getSkills())
                .containsExactly("React");

        verify(jobRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getAllJobs_EmptyList() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Job> page = new PageImpl<>(List.of(), pageable, 0);

        when(jobRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        JobPageResponse jobs = jobService.getAllJobs(0, 10, null, null, null, null);

        assertThat(jobs).isNotNull();
        assertThat(jobs.getContent()).isEmpty();
        assertThat(jobs.getTotalElements()).isZero();
        assertThat(jobs.getTotalPages()).isZero();
        verify(jobRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void createJob_InvalidApplicationWindow() {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        JobRequest request = new JobRequest();
        request.setTitle("Backend Developer");
        request.setDescription("...");
        request.setJobType(Job.JobType.FULL_TIME);
        request.setExperienceLevel(Job.ExperienceLevel.JUNIOR);
        request.setSkills(List.of("Java"));

        request.setApplicationStartsAt(LocalDateTime.of(2026, 7, 5, 0, 0));
        request.setApplicationDeadline(LocalDateTime.of(2026, 7, 1, 0, 0));

        assertThrows(InvalidApplicationWindowException.class,
                () -> jobService.createJob(request));

        verify(jobRepository, never()).save(any());
        verify(jobSkillRepository, never()).saveAll(any());
    }

    @Test
    void updateJob_InvalidApplicationWindow() {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        Job job = new Job();
        job.setId(10L);
        job.setUser(user);

        when(jobRepository.findById(10L))
                .thenReturn(Optional.of(job));

        JobRequest request = new JobRequest();
        request.setTitle("Backend Developer");
        request.setDescription("...");
        request.setJobType(Job.JobType.FULL_TIME);
        request.setExperienceLevel(Job.ExperienceLevel.SENIOR);
        request.setSkills(List.of("Java"));

        request.setApplicationStartsAt(LocalDateTime.of(2026, 7, 5, 0, 0));
        request.setApplicationDeadline(LocalDateTime.of(2026, 7, 1, 0, 0));

        assertThrows(InvalidApplicationWindowException.class,
                () -> jobService.updateJob(10L, request));

        verify(jobRepository, never()).save(any());
    }

    @Test
    void createJob_NullSkills() {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(jobRepository.save(any(Job.class)))
                .thenAnswer(invocation -> {
                    Job j = invocation.getArgument(0);
                    j.setId(10L);
                    return j;
                });

        when(jobSkillRepository.findByJobId(10L))
                .thenReturn(List.of());

        JobRequest request = new JobRequest();
        request.setTitle("Backend Developer");
        request.setDescription("...");
        request.setJobType(Job.JobType.FULL_TIME);
        request.setExperienceLevel(Job.ExperienceLevel.JUNIOR);
        request.setSkills(null);
        request.setApplicationStartsAt(LocalDateTime.now());
        request.setApplicationDeadline(LocalDateTime.now().plusDays(5));

        JobResponse response = jobService.createJob(request);

        assertThat(response.getSkills()).isEmpty();

        verify(jobSkillRepository, never()).saveAll(any());
    }

    @Test
    void updateJob_NullSkills() {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        Job job = new Job();
        job.setId(10L);
        job.setUser(user);

        when(jobRepository.findById(10L))
                .thenReturn(Optional.of(job));

        when(jobRepository.save(any(Job.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(jobSkillRepository.findByJobId(10L))
                .thenReturn(List.of());

        JobRequest request = new JobRequest();
        request.setTitle("Updated");
        request.setDescription("Updated");
        request.setJobType(Job.JobType.FULL_TIME);
        request.setExperienceLevel(Job.ExperienceLevel.SENIOR);
        request.setSkills(null);
        request.setApplicationStartsAt(LocalDateTime.now());
        request.setApplicationDeadline(LocalDateTime.now().plusDays(5));

        JobResponse response = jobService.updateJob(10L, request);

        assertThat(response.getSkills()).isEmpty();

        verify(jobSkillRepository).deleteByJobId(10L);
        verify(jobSkillRepository, never()).saveAll(any());
    }

    @Test
    void getOpenJobsForUser_HappyPath() {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        Job job = new Job();
        job.setId(10L);
        job.setTitle("Backend Developer");
        job.setApplicationStartsAt(LocalDateTime.now().minusDays(1));
        job.setApplicationDeadline(LocalDateTime.now().plusDays(5));

        when(jobRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(job)));

        when(jobSkillRepository.findByJobId(10L))
                .thenReturn(List.of(new JobSkill(null, job, "Java")));

        JobPageResponse response = jobService.getOpenJobsForUser(0, 10, null, null, null, null);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().getTitle())
                .isEqualTo("Backend Developer");
    }

    @Test
    void getOpenJobsForUser_UserNotFound() {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> jobService.getOpenJobsForUser(0, 10, null, null, null, null));

        verify(jobRepository, never())
                .findAll(any(Specification.class), any(Pageable.class));
    }
}