package com.risheek.resume_screener.service;

import com.risheek.resume_screener.dto.ParsedTextResponse;
import com.risheek.resume_screener.dto.ResumeResponse;
import com.risheek.resume_screener.entity.Application;
import com.risheek.resume_screener.entity.Resume;
import com.risheek.resume_screener.entity.Job;
import com.risheek.resume_screener.entity.User;
import com.risheek.resume_screener.exception.ApplicationNotFoundException;
import com.risheek.resume_screener.exception.ResumeNotFoundException;
import com.risheek.resume_screener.exception.UnauthorizedAccessException;
import com.risheek.resume_screener.repository.ApplicationRepository;
import com.risheek.resume_screener.repository.ResumeRepository;
import com.risheek.resume_screener.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {

    @Mock private ResumeRepository resumeRepository;
    @Mock private UserRepository userRepository;
    @Mock private WebClient.Builder webClientBuilder;
    @Mock
    private ApplicationRepository applicationRepository;

    private WebClient webClient;
    private ResumeService resumeService;

    @BeforeEach
    void setUp() {
        webClient = mock(WebClient.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS));

        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        resumeService = new ResumeService(
                resumeRepository,
                userRepository,
                applicationRepository,
                webClientBuilder,
                "http://fake-ml-service"
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
    void uploadResume_happyPath_savesResumeWithParsedText() throws IOException {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", new byte[]{1, 2, 3});

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

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

        ResumeResponse response = resumeService.uploadResume(file, "Backend Developer Resume");

        ArgumentCaptor<Resume> resumeCaptor = ArgumentCaptor.forClass(Resume.class);
        verify(resumeRepository).save(resumeCaptor.capture());
        Resume savedResume = resumeCaptor.getValue();

        assertThat(savedResume.getParsedText()).isEqualTo("Experienced Java developer...");
        assertThat(savedResume.getParsedTextAvailable()).isTrue();
        assertThat(savedResume.getUser()).isEqualTo(user);
        assertThat(savedResume.getFileName()).isEqualTo("resume.pdf");
        assertThat(savedResume.getFileType()).isEqualTo("application/pdf");

        assertThat(response.getId()).isEqualTo(100L);
    }

    @Test
    void uploadResume_mlFailurePath() throws IOException {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", new byte[]{1, 2, 3});

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        when(webClient.post().uri("/extract-text").bodyValue(any()).retrieve()
                .bodyToMono(ParsedTextResponse.class).block())
                .thenThrow(WebClientResponseException.create(500, "Internal Server Error", null, null, null));

        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> {
            Resume r = invocation.getArgument(0);
            r.setId(100L);
            return r;
        });

        resumeService.uploadResume(file, "Backend Developer Resume");

        ArgumentCaptor<Resume> resumeCaptor = ArgumentCaptor.forClass(Resume.class);
        verify(resumeRepository).save(resumeCaptor.capture());
        Resume savedResume = resumeCaptor.getValue();

        assertThat(savedResume.getParsedText()).isEqualTo("");
        assertThat(savedResume.getParsedTextAvailable()).isFalse();
    }

    @Test
    void updateResume_happyPath() throws IOException {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        Resume resume = new Resume();
        resume.setUser(user);
        resume.setId(100L);
        resume.setResumeName("Backend Developer Resume");
        resume.setFileName("resume.pdf");
        resume.setFileType("application/pdf");
        resume.setFileData(new byte[]{1, 2, 3});

        MockMultipartFile file = new MockMultipartFile(
                "file", "resume-updated.pdf", "application/pdf", new byte[]{1, 2, 3, 4});

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(resumeRepository.findById(100L)).thenReturn(Optional.of(resume));

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

        ResumeResponse response = resumeService.updateResume(100L, file, "Updated Resume");

        ArgumentCaptor<Resume> resumeCaptor = ArgumentCaptor.forClass(Resume.class);
        verify(resumeRepository).save(resumeCaptor.capture());
        Resume savedResume = resumeCaptor.getValue();

        assertThat(savedResume.getParsedText()).isEqualTo("Very Experienced Java developer...");
        assertThat(savedResume.getParsedTextAvailable()).isTrue();
        assertThat(savedResume.getUser()).isEqualTo(user);
        assertThat(response.getId()).isEqualTo(100L);
    }

    @Test
    void updateResume_mlFailPath() throws IOException {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        Resume resume = new Resume();
        resume.setUser(user);
        resume.setId(100L);

        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", new byte[]{1, 2, 3, 4});

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(resumeRepository.findById(100L)).thenReturn(Optional.of(resume));

        when(webClient.post().uri("/extract-text").bodyValue(any()).retrieve()
                .bodyToMono(ParsedTextResponse.class).block())
                .thenThrow(WebClientResponseException.create(500, "Internal Server Error", null, null, null));

        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> {
            Resume r = invocation.getArgument(0);
            r.setId(100L);
            return r;
        });

        resumeService.updateResume(100L, file, "Updated Resume");

        ArgumentCaptor<Resume> resumeCaptor = ArgumentCaptor.forClass(Resume.class);
        verify(resumeRepository).save(resumeCaptor.capture());
        Resume savedResume = resumeCaptor.getValue();

        assertThat(savedResume.getParsedText()).isEqualTo("");
        assertThat(savedResume.getParsedTextAvailable()).isFalse();
    }

    @Test
    void updateResume_unauthorized() throws IOException {

        User user1 = new User();
        user1.setId(1L);
        user1.setEmail("test@example.com");

        User user2 = new User();
        user2.setId(2L);

        Resume resume = new Resume();
        resume.setUser(user2);
        resume.setId(100L);

        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", new byte[]{1, 2, 3, 4});

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user1));
        when(resumeRepository.findById(100L)).thenReturn(Optional.of(resume));

        assertThrows(UnauthorizedAccessException.class,
                () -> resumeService.updateResume(100L, file, "Resume"));

        verify(resumeRepository, never()).save(any());
    }

    @Test
    void updateResume_resumeNotFound() {
        when(resumeRepository.findById(100L)).thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", new byte[]{1, 2, 3, 4});

        assertThrows(ResumeNotFoundException.class,
                () -> resumeService.updateResume(100L, file, "Resume"));

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
    void deleteResume_resumeNotFound() {
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
    void getResumeById_happyPath() {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        Resume resume = new Resume();
        resume.setUser(user);
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
    void getMyResumes() {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        Resume resume1 = new Resume();
        resume1.setUser(user);
        resume1.setId(100L);
        resume1.setResumeName("Backend Developer Resume");
        resume1.setFileName("resume.pdf");
        resume1.setFileType("pdf");
        resume1.setFileData(new byte[]{1, 2, 3});

        Resume resume2 = new Resume();
        resume2.setUser(user);
        resume2.setId(101L);
        resume2.setResumeName("Backend Developer Resume");
        resume2.setFileName("resume2.pdf");
        resume2.setFileType("pdf");
        resume2.setFileData(new byte[]{1, 2, 3, 4});

        Resume resume3 = new Resume();
        resume3.setUser(user);
        resume3.setId(102L);
        resume3.setResumeName("Backend Developer Resume");
        resume3.setFileName("resume3.pdf");
        resume3.setFileType("pdf");
        resume3.setFileData(new byte[]{2, 3});

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(resumeRepository.findByUserIdOrderByUploadedAtDesc(1L)).thenReturn(List.of(resume1, resume2, resume3));

        List<ResumeResponse> response = resumeService.getMyResumes();

        assertThat(response).hasSize(3);
        assertThat(response.get(0).getId()).isEqualTo(100L);
        assertThat(response.get(0).getFileName()).isEqualTo("resume.pdf");
        assertThat(response.get(1).getId()).isEqualTo(101L);
        assertThat(response.get(2).getFileName()).isEqualTo("resume3.pdf");
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

    @Test
    void getResumeForApplication_happyPath() {

        User employer = new User();
        employer.setId(1L);
        employer.setEmail("test@example.com");

        User candidate = new User();
        candidate.setId(2L);

        Job job = new Job();
        job.setUser(employer);

        Resume resume = new Resume();
        resume.setId(10L);
        resume.setResumeName("Backend Resume");
        resume.setFileName("resume.pdf");
        resume.setFileType("application/pdf");
        resume.setFileData(new byte[]{1,2,3});
        resume.setUser(candidate);

        Application application = new Application();
        application.setJob(job);
        application.setResume(resume);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(employer));

        when(applicationRepository.findById(100L))
                .thenReturn(Optional.of(application));

        ResumeResponse response =
                resumeService.getResumeForApplication(100L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getFileName()).isEqualTo("resume.pdf");
    }

    @Test
    void getResumeForApplication_unauthorized() {

        User employer = new User();
        employer.setId(1L);
        employer.setEmail("test@example.com");

        User anotherEmployer = new User();
        anotherEmployer.setId(2L);

        Job job = new Job();
        job.setUser(anotherEmployer);

        Resume resume = new Resume();

        Application application = new Application();
        application.setJob(job);
        application.setResume(resume);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(employer));

        when(applicationRepository.findById(100L))
                .thenReturn(Optional.of(application));

        assertThrows(
                UnauthorizedAccessException.class,
                () -> resumeService.getResumeForApplication(100L)
        );
    }

    @Test
    void getResumeForApplication_notFound() {

        when(applicationRepository.findById(100L))
                .thenReturn(Optional.empty());

        assertThrows(
                ApplicationNotFoundException.class,
                () -> resumeService.getResumeForApplication(100L)
        );
    }

}
