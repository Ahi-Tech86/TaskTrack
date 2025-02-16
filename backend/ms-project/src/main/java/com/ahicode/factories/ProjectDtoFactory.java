package com.ahicode.factories;

import com.ahicode.dtos.ProjectDto;
import com.ahicode.storage.entities.ProjectEntity;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Component;

import java.time.Instant;

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

    public ProjectDto makeProjectDto(Tuple tuple) {
        return ProjectDto.builder()
                .name(tuple.get("name", String.class))
                .description(tuple.get("description", String.class))
                .createAt(tuple.get("create_at", Instant.class))
                .startDate(tuple.get("start_date", Instant.class))
                .build();
    }
}
