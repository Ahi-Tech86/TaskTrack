package com.ahicode.services;

import com.ahicode.dtos.ProjectCreationRequestDto;
import com.ahicode.dtos.ProjectDto;

public interface ProjectService {
    ProjectDto createProject(ProjectCreationRequestDto requestDto, Long userId);
}
