package com.risheek.resume_screener.service;

import com.risheek.resume_screener.dto.CourseRequest;
import com.risheek.resume_screener.dto.CourseResponse;
import com.risheek.resume_screener.entity.Course;
import com.risheek.resume_screener.exception.CourseInUseException;
import com.risheek.resume_screener.exception.CourseNotFoundException;
import com.risheek.resume_screener.repository.CourseRepository;
import com.risheek.resume_screener.repository.JobRepository;
import com.risheek.resume_screener.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JobRepository jobRepository;

    private CourseService courseService;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(courseRepository, userRepository, jobRepository);
    }

    @Test
    void getAllCourses_returnsMappedResponses() {

        Course course = new Course();
        course.setId(1L);
        course.setName("MCA");
        course.setTotalYears(2);

        when(courseRepository.findAll()).thenReturn(List.of(course));

        List<CourseResponse> result = courseService.getAllCourses();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("MCA");
        assertThat(result.getFirst().getTotalYears()).isEqualTo(2);
    }

    @Test
    void createCourse_happyPath() {

        CourseRequest request = new CourseRequest();
        request.setName("B.Tech");
        request.setTotalYears(4);

        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course c = invocation.getArgument(0);
            c.setId(10L);
            return c;
        });

        CourseResponse response = courseService.createCourse(request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("B.Tech");
        assertThat(response.getTotalYears()).isEqualTo(4);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("B.Tech");
    }

    @Test
    void updateCourse_happyPath() {

        Course existing = new Course();
        existing.setId(1L);
        existing.setName("Old Name");
        existing.setTotalYears(3);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(courseRepository.save(any(Course.class))).thenReturn(existing);

        CourseRequest request = new CourseRequest();
        request.setName("New Name");
        request.setTotalYears(2);

        CourseResponse response = courseService.updateCourse(1L, request);

        assertThat(response.getName()).isEqualTo("New Name");
        assertThat(response.getTotalYears()).isEqualTo(2);
    }

    @Test
    void updateCourse_notFound_throwsException() {

        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        CourseRequest request = new CourseRequest();
        request.setName("Doesn't matter");
        request.setTotalYears(1);

        assertThrows(CourseNotFoundException.class, () -> courseService.updateCourse(99L, request));
    }

    @Test
    void deleteCourse_notFound_throwsException() {

        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CourseNotFoundException.class, () -> courseService.deleteCourse(99L));
    }

    @Test
    void deleteCourse_inUseByUser_throwsException() {

        Course course = new Course();
        course.setId(1L);
        course.setName("MCA");
        course.setTotalYears(2);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(userRepository.existsByCurrentCourse(course)).thenReturn(true);

        assertThrows(CourseInUseException.class, () -> courseService.deleteCourse(1L));

        verify(courseRepository, never()).delete(any());
    }

    @Test
    void deleteCourse_inUseByJob_throwsException() {

        Course course = new Course();
        course.setId(1L);
        course.setName("MCA");
        course.setTotalYears(2);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(userRepository.existsByCurrentCourse(course)).thenReturn(false);
        when(jobRepository.existsByEligibleCoursesId(1L)).thenReturn(true);

        assertThrows(CourseInUseException.class, () -> courseService.deleteCourse(1L));

        verify(courseRepository, never()).delete(any());
    }

    @Test
    void deleteCourse_happyPath() {

        Course course = new Course();
        course.setId(1L);
        course.setName("MCA");
        course.setTotalYears(2);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(userRepository.existsByCurrentCourse(course)).thenReturn(false);
        when(jobRepository.existsByEligibleCoursesId(1L)).thenReturn(false);

        courseService.deleteCourse(1L);

        verify(courseRepository).delete(course);
    }
}