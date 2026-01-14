package co.singularit.az104simulator.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service to manage student identity using cookies.
 * Each student gets a unique UUID stored in a cookie for tracking their attempts.
 */
@Service
@Slf4j
public class StudentIdentityService {

    private static final String STUDENT_ID_COOKIE_NAME = "studentId";
    private static final int COOKIE_MAX_AGE = 365 * 24 * 60 * 60; // 1 year

    /**
     * Get or create student ID from cookies
     *
     * @param request HTTP request
     * @param response HTTP response to set cookie if needed
     * @return Student ID (UUID)
     */
    public String getOrCreateStudentId(HttpServletRequest request, HttpServletResponse response) {
        // Try to find existing cookie
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (STUDENT_ID_COOKIE_NAME.equals(cookie.getName())) {
                    String studentId = cookie.getValue();
                    if (studentId != null && !studentId.isEmpty()) {
                        log.debug("Found existing studentId: {}", studentId);
                        return studentId;
                    }
                }
            }
        }

        // Create new student ID
        String newStudentId = UUID.randomUUID().toString();
        log.info("Creating new studentId: {}", newStudentId);

        // Set cookie
        Cookie cookie = new Cookie(STUDENT_ID_COOKIE_NAME, newStudentId);
        cookie.setMaxAge(COOKIE_MAX_AGE);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        return newStudentId;
    }

    /**
     * Get student ID from request (without creating if missing)
     *
     * @param request HTTP request
     * @return Student ID or null if not found
     */
    public String getStudentId(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (STUDENT_ID_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
