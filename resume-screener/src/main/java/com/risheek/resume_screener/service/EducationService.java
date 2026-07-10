package com.risheek.resume_screener.service;

import com.risheek.resume_screener.dto.EducationRequest;
import com.risheek.resume_screener.dto.EducationResponse;
import com.risheek.resume_screener.entity.Education;
import com.risheek.resume_screener.entity.User;
import com.risheek.resume_screener.exception.EducationNotFound;
import com.risheek.resume_screener.exception.UnauthorizedAccessException;
import com.risheek.resume_screener.repository.EducationRepository;
import com.risheek.resume_screener.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EducationService {

    private final UserRepository userRepository;
    private final EducationRepository educationRepository;

    public EducationService(UserRepository userRepository, EducationRepository educationRepository){
        this.userRepository = userRepository;
        this.educationRepository = educationRepository;
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found"));
    }

    private EducationResponse toResponse(Education education){
        return new EducationResponse(
                education.getId(),
                education.getUser().getId(),
                education.getLevel(),
                education.getInstitution(),
                education.getStartDate(),
                education.getEndDate(),
                education.getGrade(),
                education.getIsCurrent()
        );
    }

    public List<EducationResponse> getAllEducations() {
        User user = getCurrentUser();

        List<Education> educations = educationRepository.findByUser(user);
        return educations.stream().map(this::toResponse).toList();
    }


    public EducationResponse addEducation(@Valid EducationRequest request) {
        User user = getCurrentUser();
        Education education = new Education();
        education.setUser(user);
        education.setLevel(request.getLevel());
        education.setInstitution(request.getInstitution());
        education.setStartDate(request.getStartDate());
        education.setEndDate(request.getEndDate());
        education.setGrade(request.getGrade());
        education.setIsCurrent(request.getIsCurrent());
        Education savedEducation = educationRepository.save(education);
        return toResponse(savedEducation);
    }


    public EducationResponse updateEducation(Long id, @Valid EducationRequest request) {
        Education education = educationRepository.findById(id)
                .orElseThrow(() -> new EducationNotFound("Education not found with id: " + id));
        User user = getCurrentUser();
        if(!user.getId().equals(education.getUser().getId())){
            throw new UnauthorizedAccessException("You are not authorized to update this education");
        }

        education.setLevel(request.getLevel());
        education.setInstitution(request.getInstitution());
        education.setStartDate(request.getStartDate());
        education.setEndDate(request.getEndDate());
        education.setGrade(request.getGrade());
        education.setIsCurrent(request.getIsCurrent());
        Education updatedEducation = educationRepository.save(education);
        return toResponse(updatedEducation);
    }


    public void deleteEducation(Long id) {
        Education education = educationRepository.findById(id)
                .orElseThrow(() -> new EducationNotFound("Education not found with id: " + id));
        User user = getCurrentUser();
        if(!user.getId().equals(education.getUser().getId())) {
            throw new UnauthorizedAccessException("You are not authorized to delete this education");
        }
        educationRepository.delete(education);
    }

    public EducationResponse getEducation(Long id) {
        Education education = educationRepository.findById(id)
                .orElseThrow(() -> new EducationNotFound("Education not found with id: " + id));
        User user = getCurrentUser();
        if(!user.getId().equals(education.getUser().getId())) {
            throw new UnauthorizedAccessException("You are not authorized to view this education");
        }
        return toResponse(education);
    }
}
