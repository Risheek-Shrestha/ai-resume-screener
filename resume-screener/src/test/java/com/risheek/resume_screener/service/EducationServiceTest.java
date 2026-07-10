package com.risheek.resume_screener.service;

import com.risheek.resume_screener.dto.EducationRequest;
import com.risheek.resume_screener.dto.EducationResponse;
import com.risheek.resume_screener.entity.Education;
import com.risheek.resume_screener.entity.EducationLevel;
import com.risheek.resume_screener.entity.User;
import com.risheek.resume_screener.exception.EducationNotFound;
import com.risheek.resume_screener.exception.UnauthorizedAccessException;
import com.risheek.resume_screener.repository.EducationRepository;
import com.risheek.resume_screener.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EducationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EducationRepository educationRepository;

    private EducationService educationService;

    @BeforeEach
    void setUp() {
        educationService = new EducationService(userRepository, educationRepository);

        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication())
                .thenReturn(new UsernamePasswordAuthenticationToken("test@example.com", null));
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User buildUser(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        return user;
    }

    private Education buildEducation(Long id, User owner) {
        Education education = new Education();
        education.setId(id);
        education.setUser(owner);
        education.setLevel(EducationLevel.MASTERS);
        education.setInstitution("Shoolini University");
        education.setStartDate(LocalDate.of(2025, 7, 1));
        education.setEndDate(null);
        education.setGrade("A");
        education.setIsCurrent(true);
        return education;
    }

    @Test
    void getAllEducations_returnsMappedResponses() {

        User user = buildUser(1L, "test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        Education education = buildEducation(5L, user);
        when(educationRepository.findByUser(user)).thenReturn(List.of(education));

        List<EducationResponse> result = educationService.getAllEducations();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getInstitution()).isEqualTo("Shoolini University");
        assertThat(result.getFirst().getUserId()).isEqualTo(1L);
    }

    @Test
    void addEducation_happyPath() {

        User user = buildUser(1L, "test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        EducationRequest request = new EducationRequest();
        request.setLevel(EducationLevel.BACHELORS);
        request.setInstitution("College A");
        request.setStartDate(LocalDate.of(2020, 7, 1));
        request.setEndDate(LocalDate.of(2024, 6, 30));
        request.setGrade("B");
        request.setIsCurrent(false);

        when(educationRepository.save(any(Education.class))).thenAnswer(invocation -> {
            Education e = invocation.getArgument(0);
            e.setId(20L);
            return e;
        });

        EducationResponse response = educationService.addEducation(request);

        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getInstitution()).isEqualTo("College A");
        assertThat(response.getUserId()).isEqualTo(1L);
    }

    @Test
    void updateEducation_happyPath() {

        User user = buildUser(1L, "test@example.com");
        Education existing = buildEducation(5L, user);

        when(educationRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(educationRepository.save(any(Education.class))).thenReturn(existing);

        EducationRequest request = new EducationRequest();
        request.setLevel(EducationLevel.DOCTORATE);
        request.setInstitution("Updated Institution");
        request.setStartDate(LocalDate.of(2021, 1, 1));
        request.setEndDate(LocalDate.of(2025, 1, 1));
        request.setGrade("A+");
        request.setIsCurrent(false);

        EducationResponse response = educationService.updateEducation(5L, request);

        assertThat(response.getInstitution()).isEqualTo("Updated Institution");
        assertThat(response.getLevel()).isEqualTo(EducationLevel.DOCTORATE);
    }

    @Test
    void updateEducation_notFound_throwsException() {

        when(educationRepository.findById(99L)).thenReturn(Optional.empty());

        EducationRequest request = new EducationRequest();
        request.setLevel(EducationLevel.MASTERS);
        request.setInstitution("Doesn't matter");
        request.setStartDate(LocalDate.now());
        request.setGrade("A");
        request.setIsCurrent(true);

        assertThrows(EducationNotFound.class, () -> educationService.updateEducation(99L, request));
    }

    @Test
    void updateEducation_notOwner_throwsUnauthorized() {

        User owner = buildUser(2L, "owner@example.com");
        User requester = buildUser(1L, "test@example.com");
        Education existing = buildEducation(5L, owner);

        when(educationRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(requester));

        EducationRequest request = new EducationRequest();
        request.setLevel(EducationLevel.MASTERS);
        request.setInstitution("Hack Attempt");
        request.setStartDate(LocalDate.now());
        request.setGrade("A");
        request.setIsCurrent(true);

        assertThrows(UnauthorizedAccessException.class, () -> educationService.updateEducation(5L, request));

        verify(educationRepository, never()).save(any());
    }

    @Test
    void deleteEducation_notFound_throwsException() {

        when(educationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EducationNotFound.class, () -> educationService.deleteEducation(99L));
    }

    @Test
    void deleteEducation_notOwner_throwsUnauthorized() {

        User owner = buildUser(2L, "owner@example.com");
        User requester = buildUser(1L, "test@example.com");
        Education existing = buildEducation(5L, owner);

        when(educationRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(requester));

        assertThrows(UnauthorizedAccessException.class, () -> educationService.deleteEducation(5L));

        verify(educationRepository, never()).delete(any());
    }

    @Test
    void deleteEducation_happyPath() {

        User user = buildUser(1L, "test@example.com");
        Education existing = buildEducation(5L, user);

        when(educationRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        educationService.deleteEducation(5L);

        verify(educationRepository).delete(existing);
    }
}