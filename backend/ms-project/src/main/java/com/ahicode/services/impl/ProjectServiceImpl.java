package com.ahicode.services.impl;

import com.ahicode.dtos.ProjectCreationRequestDto;
import com.ahicode.dtos.ProjectDto;
import com.ahicode.factories.ProjectDtoFactory;
import com.ahicode.factories.ProjectEntityFactory;
import com.ahicode.factories.ProjectMemberEntityFactory;
import com.ahicode.services.ProjectService;
import com.ahicode.storage.entities.ProjectEntity;
import com.ahicode.storage.entities.ProjectMemberEntity;
import com.ahicode.storage.enums.ProjectRole;
import com.ahicode.storage.repositories.ProjectMemberRepository;
import com.ahicode.storage.repositories.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public ProjectDto createProject(ProjectCreationRequestDto requestDto, Long userId) {
        ProjectEntity project = entityFactory.makeProjectEntity(requestDto, userId);

        ProjectEntity savedProject = repository.save(project);
        log.info("Project saved with ID: {}", savedProject.getId());
        ProjectMemberEntity projectMember = memberEntityFactory.makeProjectMemberEntity(
                savedProject, ProjectRole.PROJECT_MANAGER
        );
        memberRepository.save(projectMember);
        log.info("Project member created with ID: {}", projectMember.getId());

        return dtoFactory.makeProjectDto(savedProject);
    }
}
