package com.risheek.resume_screener.service;

import com.risheek.resume_screener.dto.ParsedTextResponse;
import com.risheek.resume_screener.dto.ResumeRequest;
import com.risheek.resume_screener.dto.ResumeResponse;
import com.risheek.resume_screener.entity.Job;
import com.risheek.resume_screener.entity.Resume;
import com.risheek.resume_screener.entity.User;
import com.risheek.resume_screener.exception.JobNotFoundException;
import com.risheek.resume_screener.exception.ResumeNotFoundException;
import com.risheek.resume_screener.exception.UnauthorizedAccessException;
import com.risheek.resume_screener.repository.JobRepository;
import com.risheek.resume_screener.repository.ResumeRepository;
import com.risheek.resume_screener.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {

    @Mock
    private ResumeRepository resumeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private ScoreService scoreService;
    @Mock
    private WebClient.Builder webClientBuilder;

    private WebClient webClient;

    private ResumeService resumeService;

    @BeforeEach
    void setUp() {
        webClient = mock(WebClient.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS));

        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        resumeService = new ResumeService(
                resumeRepository, userRepository, jobRepository,
                scoreService, webClientBuilder, "http://fake-ml-service"
        );

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
    void uploadResume_happyPath_savesResumeWithParsedTextAndTriggersScoring() {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        Job job = new Job();
        job.setId(10L);

        ResumeRequest request = new ResumeRequest();
        request.setJobId(10L);
        request.setResumeName("Backend Developer Resume");
        request.setFileName("resume.pdf");
        request.setFileType("pdf");
        request.setFileData(new byte[]{1, 2, 3});

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));

        ParsedTextResponse parsedResponse = new ParsedTextResponse();
        parsedResponse.setParsedText("Experienced Java developer...");
        when(webClient.post()
                .uri("/extract-text")
                .bodyValue(any())
                .retrieve()
                .bodyToMono(ParsedTextResponse.class)
                .block())
                .thenReturn(parsedResponse);

        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> {
            Resume r = invocation.getArgument(0);
            r.setId(100L);
            return r;
        });

        ResumeResponse response = resumeService.uploadResume(request);

        ArgumentCaptor<Resume> resumeCaptor = ArgumentCaptor.forClass(Resume.class);
        verify(resumeRepository).save(resumeCaptor.capture());
        Resume savedResume = resumeCaptor.getValue();

        assertThat(savedResume.getParsedText()).isEqualTo("Experienced Java developer...");
        assertThat(savedResume.getParsedTextAvailable()).isTrue();
        assertThat(savedResume.getUser()).isEqualTo(user);
        assertThat(savedResume.getJob()).isEqualTo(job);

        verify(scoreService).generateScore(savedResume);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getJobId()).isEqualTo(10L);
    }

    @Test
    void uploadResume_mlfailurePath() {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        Job job = new Job();
        job.setId(10L);

        ResumeRequest request = new ResumeRequest();
        request.setJobId(10L);
        request.setResumeName("Backend Developer Resume");
        request.setFileName("resume.pdf");
        request.setFileType("pdf");
        request.setFileData(new byte[]{1, 2, 3});

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));

        when(webClient.post().uri("/extract-text").bodyValue(any()).retrieve()
                .bodyToMono(ParsedTextResponse.class).block())
                .thenThrow(WebClientResponseException.create(500, "Internal Server Error", null, null, null));

        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> {
            Resume r = invocation.getArgument(0);
            r.setId(100L);
            return r;
        });

        ResumeResponse response = resumeService.uploadResume(request);

        ArgumentCaptor<Resume> resumeCaptor = ArgumentCaptor.forClass(Resume.class);
        verify(resumeRepository).save(resumeCaptor.capture());
        Resume savedResume = resumeCaptor.getValue();

        assertThat(savedResume.getParsedText()).isEqualTo("");
        assertThat(savedResume.getParsedTextAvailable()).isFalse();
        verify(scoreService, never()).generateScore(any());
    }

    @Test
    void uploadResume_jobNotFound(){

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jobRepository.findById(10L)).thenReturn(Optional.empty());

        ResumeRequest request = new ResumeRequest();
        request.setResumeName("Backend Developer Resume");
        request.setFileName("resume.pdf");
        request.setFileType("pdf");
        request.setFileData(new byte[]{1, 2, 3, 4});
        request.setJobId(10L);

        assertThrows(JobNotFoundException.class,
                () -> resumeService.uploadResume( request));

        verify(resumeRepository, never()).save(any());
    }

    @Test
    void updateResume_happyPath(){

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        Job job = new Job();
        job.setId(10L);

        Resume resume = new Resume();
        resume.setUser(user);
        resume.setJob(job);
        resume.setId(100L);
        resume.setResumeName("Backend Developer Resume");
        resume.setFileName("resume.pdf");
        resume.setFileType("pdf");
        resume.setFileData(new byte[]{1, 2, 3});

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(resumeRepository.findById(100L)).thenReturn(Optional.of(resume));

        ResumeRequest request = new ResumeRequest();
        request.setFileName("resume.pdf");
        request.setResumeName("Backend Developer Resume");
        request.setFileType("pdf");
        request.setFileData(new byte[]{1, 2, 3, 4});
        request.setJobId(job.getId());

        ParsedTextResponse parsedResponse = new ParsedTextResponse();
        parsedResponse.setParsedText("Very Experienced Java developer...");

        when(webClient.post()
                .uri("/extract-text")
                .bodyValue(any())
                .retrieve()
                .bodyToMono(ParsedTextResponse.class)
                .block())
                .thenReturn(parsedResponse);

        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> {
            Resume r = invocation.getArgument(0);
            r.setId(100L);
            return r;
        });

        ResumeResponse response = resumeService.updateResume(100L,request);

        ArgumentCaptor<Resume> resumeCaptor = ArgumentCaptor.forClass(Resume.class);
        verify(resumeRepository).save(resumeCaptor.capture());
        Resume savedResume = resumeCaptor.getValue();

        assertThat(savedResume.getParsedText()).isEqualTo("Very Experienced Java developer...");
        assertThat(savedResume.getParsedTextAvailable()).isTrue();
        assertThat(savedResume.getUser()).isEqualTo(user);
        assertThat(savedResume.getJob()).isEqualTo(job);

        verify(scoreService).generateScore(savedResume);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getJobId()).isEqualTo(10L);

    }

    @Test
    void updateResume_mlFailPath(){

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        Job job = new Job();
        job.setId(10L);

        Resume resume = new Resume();
        resume.setUser(user);
        resume.setJob(job);
        resume.setId(100L);
        resume.setResumeName("Backend Developer Resume");
        resume.setFileName("resume.pdf");
        resume.setFileType("pdf");
        resume.setFileData(new byte[]{1, 2, 3});

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(resumeRepository.findById(100L)).thenReturn(Optional.of(resume));

        ResumeRequest request = new ResumeRequest();
        request.setFileName("resume.pdf");
        request.setFileType("pdf");
        request.setFileData(new byte[]{1, 2, 3, 4});
        request.setJobId(job.getId());

        when(webClient.post().uri("/extract-text").bodyValue(any()).retrieve()
                .bodyToMono(ParsedTextResponse.class).block())
                .thenThrow(WebClientResponseException.create(500, "Internal Server Error", null, null, null));

        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> {
            Resume r = invocation.getArgument(0);
            r.setId(100L);
            return r;
        });

        ResumeResponse response = resumeService.updateResume(100L,request);

        ArgumentCaptor<Resume> resumeCaptor = ArgumentCaptor.forClass(Resume.class);
        verify(resumeRepository).save(resumeCaptor.capture());
        Resume savedResume = resumeCaptor.getValue();

        assertThat(savedResume.getParsedText()).isEqualTo("");
        assertThat(savedResume.getParsedTextAvailable()).isFalse();
        verify(scoreService, never()).generateScore(any());

    }

    @Test
    void updateResume_unauthorized(){

        User user1 = new User();
        user1.setId(1L);
        user1.setEmail("test@example.com");

        User user2 = new User();
        user2.setId(2L);

        Job job = new Job();
        job.setId(10L);

        Resume resume = new Resume();
        resume.setUser(user2);
        resume.setJob(job);
        resume.setId(100L);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user1));
        when(jobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(resumeRepository.findById(100L)).thenReturn(Optional.of(resume));

        ResumeRequest request = new ResumeRequest();
        request.setResumeName("Backend Developer Resume");
        request.setFileName("resume.pdf");
        request.setFileType("pdf");
        request.setFileData(new byte[]{1, 2, 3, 4});
        request.setJobId(job.getId());

        assertThrows(UnauthorizedAccessException.class,
                () -> resumeService.updateResume(100L, request));

        verify(resumeRepository, never()).save(any());
    }

    @Test
    void updateResume_resumeNotFound() {
        when(resumeRepository.findById(100L)).thenReturn(Optional.empty());

        ResumeRequest request = new ResumeRequest();
        request.setJobId(10L);
        request.setResumeName("Backend Developer Resume");
        request.setFileName("resume.pdf");
        request.setFileType("pdf");
        request.setFileData(new byte[]{1, 2, 3, 4});

        assertThrows(ResumeNotFoundException.class,
                () -> resumeService.updateResume(100L, request));

        verify(resumeRepository, never()).save(any());
    }

    @Test
    void deleteResume_happyPath() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        Resume resume = new Resume();
        resume.setUser(user);
        resume.setId(100L);

        when(resumeRepository.findById(100L)).thenReturn(Optional.of(resume));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        resumeService.deleteResume(100L);

        verify(resumeRepository).delete(resume);
    }

    @Test
    void deleteResume_ResumeNotFound(){

        when(resumeRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(ResumeNotFoundException.class,
                () -> resumeService.deleteResume(100L));

        verify(resumeRepository, never()).delete(any());
    }

    @Test
    void deleteResume_unauthorized() {
        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setEmail("test@example.com");

        User owner = new User();
        owner.setId(2L);

        Resume resume = new Resume();
        resume.setUser(owner);
        resume.setId(10L);

        when(resumeRepository.findById(10L)).thenReturn(Optional.of(resume));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(currentUser));

        assertThrows(UnauthorizedAccessException.class,
                () -> resumeService.deleteResume(10L));

        verify(resumeRepository, never()).delete(any());
    }

    @Test
    void getResumeById_HappyPath(){

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        Job job = new Job();
        job.setId(10L);

        Resume resume = new Resume();

        resume.setUser(user);
        resume.setJob(job);
        resume.setId(100L);
        resume.setResumeName("Backend Developer Resume");
        resume.setFileName("resume.pdf");
        resume.setFileType("pdf");
        resume.setFileData(new byte[]{1, 2, 3});

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(resumeRepository.findByIdAndUserId(100L, user.getId())).thenReturn(Optional.of(resume));

        ResumeResponse response = resumeService.getResumeById(100L);

        assertThat(response.getFileName()).isEqualTo("resume.pdf");
        assertThat(response.getFileType()).isEqualTo("pdf");
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getJobId()).isEqualTo(10L);

    }

    @Test
    void getResumeById_belongsToAnotherUser_throwsResumeNotFound() {
        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setEmail("test@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(currentUser));
        when(resumeRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResumeNotFoundException.class,
                () -> resumeService.getResumeById(10L));
    }

    @Test
    void getMyResume(){

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        Job job = new Job();
        job.setId(10L);

        Resume resume1 = new Resume();

        resume1.setUser(user);
        resume1.setJob(job);
        resume1.setId(100L);
        resume1.setResumeName("Backend Developer Resume");
        resume1.setFileName("resume.pdf");
        resume1.setFileType("pdf");
        resume1.setFileData(new byte[]{1, 2, 3});

        Resume resume2 = new Resume();

        resume2.setUser(user);
        resume2.setJob(job);
        resume2.setId(101L);
        resume2.setResumeName("Backend Developer Resume");
        resume2.setFileName("resume2.pdf");
        resume2.setFileType("pdf");
        resume2.setFileData(new byte[]{1, 2, 3, 4});

        Resume resume3 = new Resume();

        resume3.setUser(user);
        resume3.setJob(job);
        resume3.setId(102L);
        resume3.setResumeName("Backend Developer Resume");
        resume3.setFileName("resume2.pdf");
        resume3.setFileType("pdf");
        resume3.setFileData(new byte[]{2, 3});

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(resumeRepository.findByUserIdOrderByUploadedAtDesc(1L)).thenReturn(List.of(resume1, resume2, resume3));

        List<ResumeResponse> response = resumeService.getMyResumes();

        assertThat(response.size()).isEqualTo(3);
        assertThat(response).hasSize(3);
        assertThat(response.get(0).getId()).isEqualTo(100L);
        assertThat(response.get(0).getFileName()).isEqualTo("resume.pdf");
        assertThat(response.get(0).getJobId()).isEqualTo(10L);
        assertThat(response.get(1).getId()).isEqualTo(101L);
        assertThat(response.get(2).getFileName()).isEqualTo("resume2.pdf");

    }

    @Test
    void getMyResumes_emptyList() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(resumeRepository.findByUserIdOrderByUploadedAtDesc(1L)).thenReturn(List.of());

        List<ResumeResponse> response = resumeService.getMyResumes();

        assertThat(response).isEmpty();
    }
}