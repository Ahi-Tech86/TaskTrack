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

import java.util.List;

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
        String userNickname = jwtService.extractEmailFromAccessToken(accessToken);

        return ResponseEntity.status(HttpStatus.CREATED).body(service.createProject(requestDto, userId, userNickname));
    }

    @PatchMapping("/{projectId}/update")
    public ResponseEntity<ProjectDto> updateProject(HttpServletRequest request, @PathVariable Long projectId, @Valid @RequestBody ProjectUpdateRequestDto requestDto) {
        String accessToken = extractCookieValue(request, "accessToken");
        Long userId = jwtService.extractUserIdFromAccessToken(accessToken);

        return ResponseEntity.ok(service.updateProjectInfo(projectId, userId, requestDto));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectDto> getProject(HttpServletRequest request, @PathVariable Long projectId) {
        String accessToken = extractCookieValue(request, "accessToken");
        Long userId = jwtService.extractUserIdFromAccessToken(accessToken);

        return ResponseEntity.ok(service.getProject(projectId, userId));
    }

    @GetMapping("/projects")
    public ResponseEntity<List<ProjectDto>> getAllJoinedProjects(HttpServletRequest request) {
        String accessToken = extractCookieValue(request, "accessToken");
        Long userId = jwtService.extractUserIdFromAccessToken(accessToken);

        return ResponseEntity.ok(service.getAllProjects(userId));
    }

    @DeleteMapping("/{projectId}/delete")
    public ResponseEntity<Void> deleteProject(HttpServletRequest request, @PathVariable Long projectId) {
        String accessToken = extractCookieValue(request, "accessToken");
        Long userId = jwtService.extractUserIdFromAccessToken(accessToken);

        service.deleteProject(projectId, userId);

        return ResponseEntity.noContent().build();
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
