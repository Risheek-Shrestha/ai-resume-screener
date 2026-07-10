package com.risheek.resume_screener.service;

import com.risheek.resume_screener.dto.CourseRequest;
import com.risheek.resume_screener.dto.CourseResponse;
import com.risheek.resume_screener.entity.Course;
import com.risheek.resume_screener.exception.CourseInUseException;
import com.risheek.resume_screener.exception.CourseNotFoundException;
import com.risheek.resume_screener.repository.CourseRepository;
import com.risheek.resume_screener.repository.JobRepository;
import com.risheek.resume_screener.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;

    public CourseService(CourseRepository courseRepository, UserRepository userRepository, JobRepository jobRepository) {
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.courseRepository = courseRepository;
    }

    public List<CourseResponse> getAllCourses() {

        List<Course> courses = courseRepository.findAll();
        return courses.stream()
                .map(course -> new CourseResponse(course.getId(), course.getName(), course.getTotalYears()))
                .toList();
    }


    public CourseResponse createCourse(CourseRequest request) {
        Course course = new Course();
        course.setName(request.getName());
        course.setTotalYears(request.getTotalYears());
        courseRepository.save(course);
        return new CourseResponse(course.getId(), course.getName(), course.getTotalYears());
    }

    public CourseResponse updateCourse(Long id, CourseRequest request) {

        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException("Course not found with id: " + id));
        course.setName(request.getName());
        course.setTotalYears(request.getTotalYears());
        courseRepository.save(course);
        return new CourseResponse(course.getId(), course.getName(), course.getTotalYears());
    }


    public void deleteCourse(Long id) {

        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException("Course not found with id: " + id));

        if (userRepository.existsByCurrentCourse(course)) {
            throw new CourseInUseException(
                    "Cannot delete course: one or more users currently have this as their course");
        }

        if (jobRepository.existsByEligibleCoursesId(id)) {
            throw new CourseInUseException(
                    "Cannot delete course: one or more jobs reference this course as eligible");
        }

        courseRepository.delete(course);
    }

    public CourseResponse getCourseById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() ->
                        new CourseNotFoundException(
                                "Course not found with id: " + id));

        return CourseResponse.from(course);
    }
}

