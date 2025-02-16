package com.ahicode.services.impl;

import com.ahicode.dtos.ProjectCreationRequestDto;
import com.ahicode.dtos.ProjectDto;
import com.ahicode.dtos.ProjectUpdateRequestDto;
import com.ahicode.exceptions.AppException;
import com.ahicode.factories.ProjectDtoFactory;
import com.ahicode.factories.ProjectEntityFactory;
import com.ahicode.factories.ProjectMemberEntityFactory;
import com.ahicode.services.ProjectService;
import com.ahicode.storage.entities.ProjectEntity;
import com.ahicode.storage.entities.ProjectMemberEntity;
import com.ahicode.storage.enums.ProjectRole;
import com.ahicode.storage.repositories.ProjectMemberRepository;
import com.ahicode.storage.repositories.ProjectRepository;
import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectDtoFactory dtoFactory;
    private final ProjectRepository repository;
    private final ProjectEntityFactory entityFactory;
    private final ProjectMemberRepository memberRepository;
    private final ProjectMemberEntityFactory memberEntityFactory;

    @Override
    @Transactional
    public ProjectDto createProject(ProjectCreationRequestDto requestDto, Long userId, String userNickname) {
        ProjectEntity project = entityFactory.makeProjectEntity(requestDto, userId);

        ProjectEntity savedProject = repository.save(project);
        log.info("Project saved with ID: {}", savedProject.getId());
        ProjectMemberEntity projectMember = memberEntityFactory.makeProjectMemberEntity(
                savedProject, ProjectRole.PROJECT_MANAGER, userNickname
        );
        memberRepository.save(projectMember);
        log.info("Project member created with ID: {}", projectMember.getId());

        return dtoFactory.makeProjectDto(savedProject);
    }

    @Override
    @Transactional
    public ProjectDto updateProjectInfo(Long projectId, Long userId, ProjectUpdateRequestDto requestDto) {
        ProjectEntity project = isProjectExistsById(projectId);
        isUserProjectManager(projectId, userId);

        if (requestDto.getName() != null) {
            project.setName(requestDto.getName());
        }

        if (requestDto.getDescription() != null) {
            project.setDescription(requestDto.getDescription());
        }

        if (requestDto.getStartDate() != null) {
            project.setStartDate(requestDto.getStartDate());
        }

        ProjectEntity updatedProject = repository.save(project);
        log.info("Project info with id {} was updated", projectId);

        return dtoFactory.makeProjectDto(updatedProject);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDto> getAllProjects(Long userId) {
        List<Tuple> tuples = memberRepository.getAllProjectsByUserId(userId);

        return tuples.stream()
                .map(tuple -> ProjectDto.builder()
                        .name(tuple.get("name", String.class))
                        .description(tuple.get("description", String.class))
                        .createAt(tuple.get("create_at", Instant.class))
                        .startDate(tuple.get("start_date", Instant.class))
                        .build()
                )
                .collect(Collectors.toList());
    }

    @Override
    public ProjectDto getProject(Long projectId, Long userId) {
        Tuple tuple = memberRepository.getProjectByUserIdAndProjectId(userId, projectId);

        return dtoFactory.makeProjectDto(tuple);
    }

    @Override
    public void deleteProject(Long projectId, Long userId) {
        //todo
    }

    private ProjectEntity isProjectExistsById(Long projectId) {
        ProjectEntity project = repository.findById(projectId).orElseThrow(
                () -> {
                    String errorMessage = String.format("Project with id %s doesn't exists", projectId);
                    log.warn("Attempt to change info about non-existent project with id: {}", projectId);
                    return new AppException(errorMessage, HttpStatus.NOT_FOUND);
                }
        );

        return project;
    }

    private void isUserProjectManager(Long projectId, Long userId) {
        ProjectMemberEntity projectMember = memberRepository.getProjectMemberEntityByProjectIdAndUserId(userId, projectId);

        if (!projectMember.getRole().equals(ProjectRole.PROJECT_MANAGER)) {
            log.error("Attempt to change resource with not sufficient project permissions");
            throw new AppException("User does not have sufficient permissions", HttpStatus.FORBIDDEN);
        }
    }
}
