package com.ahicode.controllers;

import com.ahicode.dtos.ProjectCreationRequestDto;
import com.ahicode.services.JwtService;
import com.ahicode.services.ProjectService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.WebUtils;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/project")
public class ProjectController {

    private final JwtService jwtService;
    private final ProjectService service;

    @PostMapping("/create")
    public ResponseEntity<?> createProject(HttpServletRequest request, @Valid @RequestBody ProjectCreationRequestDto requestDto) {
        String accessToken = extractCookieValue(request, "accessToken");
        Long userId = jwtService.extractUserIdFromAccessToken(accessToken);
        return ResponseEntity.ok(service.createProject(requestDto, userId));
    }

    private String extractCookieValue(HttpServletRequest request, String cookieName) {
        Cookie cookie = WebUtils.getCookie(request, cookieName);

        if (cookie != null) {
            return cookie.getValue();
        } else {
            log.warn("Cookie {} not found in request", cookieName);
            return null;
        }
    }
}
