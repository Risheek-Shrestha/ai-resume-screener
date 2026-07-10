package com.risheek.resume_screener.exception;

import com.risheek.resume_screener.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/test");
    }

    static Stream<Arguments> exceptionMappings() {
        return Stream.of(
                Arguments.of(new ResumeNotFoundException("Resume not found"),
                        HttpStatus.NOT_FOUND, "Resume Not Found"),
                Arguments.of(new InvalidCredentialsException("Bad credentials"),
                        HttpStatus.UNAUTHORIZED, "Authentication Failed"),
                Arguments.of(new JobNotFoundException("Job not found"),
                        HttpStatus.NOT_FOUND, "Job Not Found"),
                Arguments.of(new UserNotFoundException("User not found"),
                        HttpStatus.NOT_FOUND, "User Not Found"),
                Arguments.of(new UnauthorizedAccessException("Access denied"),
                        HttpStatus.FORBIDDEN, "Access Denied"),
                Arguments.of(new ScoreNotFoundException("Score not found"),
                        HttpStatus.NOT_FOUND, "Score Not Found"),
                Arguments.of(new InvalidApplicationWindowException("Invalid window"),
                        HttpStatus.BAD_REQUEST, "Invalid Application Window"),
                Arguments.of(new ApplicationClosedException("Closed"),
                        HttpStatus.BAD_REQUEST, "Application Closed"),
                Arguments.of(new ApplicationNotStartedException("Not started"),
                        HttpStatus.BAD_REQUEST, "Application Not Started"),
                Arguments.of(new DuplicateApplicationException("Duplicate"),
                        HttpStatus.CONFLICT, "Duplicate Application"),
                Arguments.of(new InvalidApplicationStatusException("Invalid status"),
                        HttpStatus.BAD_REQUEST, "Invalid Application Status"),
                Arguments.of(new ApplicationNotFoundException("App not found"),
                        HttpStatus.NOT_FOUND, "Application Not Found"),
                Arguments.of(new NotificationNotFoundException("Notif not found"),
                        HttpStatus.NOT_FOUND, "Notification Not Found"),
                Arguments.of(new CourseNotFoundException("Course not found"),
                        HttpStatus.NOT_FOUND, "Course Not Found"),
                Arguments.of(new CourseInUseException("Course in use"),
                        HttpStatus.BAD_REQUEST, "Course In Use"),
                Arguments.of(new EducationNotFound("Education not found"),
                        HttpStatus.NOT_FOUND, "Education Not Found"),
                Arguments.of(new EmailAlreadyExistsException("Email exists"),
                        HttpStatus.CONFLICT, "Email Already Exists")
        );
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("exceptionMappings")
    void handlesEachCustomException_withCorrectStatusAndBody(
            RuntimeException exception, HttpStatus expectedStatus, String expectedError) throws Exception {

        // Dispatch to the matching handler method by exception type, mirroring
        // how @ExceptionHandler resolution would route the same exception.
        ResponseEntity<ErrorResponse> response = invokeHandler(exception);

        assertNotNull(response);
        assertEquals(expectedStatus, response.getStatusCode());

        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(expectedStatus.value(), body.getStatus());
        assertEquals(expectedError, body.getError());
        assertEquals(exception.getMessage(), body.getMessage());
        assertEquals("/api/v1/test", body.getPath());
        assertNotNull(body.getTimestamp());
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<ErrorResponse> invokeHandler(RuntimeException exception) {
        if (exception instanceof ResumeNotFoundException e) return handler.handleResumeNotFound(e, request);
        if (exception instanceof InvalidCredentialsException e) return handler.handleInvalidCredentials(e, request);
        if (exception instanceof JobNotFoundException e) return handler.handleJobNotFound(e, request);
        if (exception instanceof UserNotFoundException e) return handler.handleUserNotFound(e, request);
        if (exception instanceof UnauthorizedAccessException e) return handler.handleUnauthorized(e, request);
        if (exception instanceof ScoreNotFoundException e) return handler.handleScoreNotFound(e, request);
        if (exception instanceof InvalidApplicationWindowException e) return handler.handleInvalidApplicationWindow(e, request);
        if (exception instanceof ApplicationClosedException e) return handler.handleApplicationClosed(e, request);
        if (exception instanceof ApplicationNotStartedException e) return handler.handleApplicationNotStarted(e, request);
        if (exception instanceof DuplicateApplicationException e) return handler.handleDuplicateApplication(e, request);
        if (exception instanceof InvalidApplicationStatusException e) return handler.handleInvalidApplicationStatus(e, request);
        if (exception instanceof ApplicationNotFoundException e) return handler.handleApplicationNotFound(e, request);
        if (exception instanceof NotificationNotFoundException e) return handler.handleNotificationNotFound(e, request);
        if (exception instanceof CourseNotFoundException e) return handler.handleCourseNotFound(e, request);
        if (exception instanceof CourseInUseException e) return handler.handleCourseInUse(e, request);
        if (exception instanceof EducationNotFound e) return handler.handleEducationNotFound(e, request);
        if (exception instanceof EmailAlreadyExistsException e) return handler.handleEmailAlreadyExists(e, request);
        throw new IllegalArgumentException("Unmapped exception type in test: " + exception.getClass());
    }

    @Test
    void handleMissingRequestParameter_returnsBadRequest() throws Exception {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("jobId", "Long");

        ResponseEntity<ErrorResponse> response = handler.handleMissingRequestParameter(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Bad Request", response.getBody().getError());
        assertEquals("/api/v1/test", response.getBody().getPath());
    }

    @Test
    void handleGenericException_returnsInternalServerError() {
        Exception ex = new RuntimeException("Something unexpected broke");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Internal Server Error", response.getBody().getError());
        assertEquals("Something unexpected broke", response.getBody().getMessage());
    }

    @Test
    void handleValidationErrors_returnsBadRequestWithFieldErrorMap() {
        MethodParameter methodParameter = mock(MethodParameter.class);
        BindingResult bindingResult = mock(BindingResult.class);

        FieldError fieldError1 = new FieldError("scoreRequest", "resumeId", "must not be null");
        FieldError fieldError2 = new FieldError("scoreRequest", "jobId", "must not be null");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<Map<String, String>> response = handler.handleValidationErrors(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("must not be null", response.getBody().get("resumeId"));
        assertEquals("must not be null", response.getBody().get("jobId"));
    }
}