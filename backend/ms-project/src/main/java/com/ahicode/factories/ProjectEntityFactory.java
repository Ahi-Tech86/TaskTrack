package com.ahicode.factories;

import com.ahicode.dtos.ProjectCreationRequestDto;
import com.ahicode.storage.entities.ProjectEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ProjectEntityFactory {

    public ProjectEntity makeProjectEntity(ProjectCreationRequestDto requestDto, Long userId) {
        return ProjectEntity.builder()
                .name(requestDto.getName())
                .description(requestDto.getDescription())
                .startDate(requestDto.getStartDate())
                .ownerId(userId)
                .createAt(Instant.now())
                .build();
    }
}
