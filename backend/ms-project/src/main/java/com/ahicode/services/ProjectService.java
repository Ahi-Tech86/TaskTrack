package com.ahicode.services;

import com.ahicode.dtos.ProjectCreationRequestDto;
import com.ahicode.dtos.ProjectDto;
import com.ahicode.dtos.ProjectUpdateRequestDto;

import java.util.List;

public interface ProjectService {
    ProjectDto createProject(ProjectCreationRequestDto requestDto, Long userId, String userNickname);
    ProjectDto updateProjectInfo(Long projectId, Long userId, ProjectUpdateRequestDto requestDto);
    List<ProjectDto> getAllProjects(Long userId);
    ProjectDto getProject(Long projectId, Long userId);
    void deleteProject(Long projectId, Long userId);
}
