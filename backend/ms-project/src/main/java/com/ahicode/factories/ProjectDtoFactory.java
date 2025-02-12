package com.ahicode.factories;

import com.ahicode.dtos.ProjectDto;
import com.ahicode.storage.entities.ProjectEntity;
import org.springframework.stereotype.Component;

@Component
public class ProjectDtoFactory {

    public ProjectDto makeProjectDto(ProjectEntity entity) {
        return ProjectDto.builder()
                .name(entity.getName())
                .description(entity.getDescription())
                .startDate(entity.getStartDate())
                .createAt(entity.getCreateAt())
                .build();
    }
}
