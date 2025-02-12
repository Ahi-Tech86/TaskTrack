package com.ahicode.controllers;

import com.ahicode.dtos.ProjectCreationRequestDto;
import com.ahicode.dtos.ProjectDto;
import com.ahicode.dtos.ProjectUpdateRequestDto;
import com.ahicode.services.JwtService;
import com.ahicode.services.ProjectService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
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
    public ResponseEntity<ProjectDto> createProject(HttpServletRequest request, @Valid @RequestBody ProjectCreationRequestDto requestDto) {
        String accessToken = extractCookieValue(request, "accessToken");
        Long userId = jwtService.extractUserIdFromAccessToken(accessToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createProject(requestDto, userId));
    }

    @PatchMapping("/{projectId}/update")
    public ResponseEntity<ProjectDto> updateProject(@PathVariable Long projectId, @Valid @RequestBody ProjectUpdateRequestDto requestDto) {
        return ResponseEntity.ok(service.updateProjectInfo(projectId, requestDto));
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
