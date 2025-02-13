package com.ahicode.factories;

import com.ahicode.storage.entities.ProjectEntity;
import com.ahicode.storage.entities.ProjectMemberEntity;
import com.ahicode.storage.enums.ProjectRole;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ProjectMemberEntityFactory {

    public ProjectMemberEntity makeProjectMemberEntity(ProjectEntity entity, ProjectRole role, String userNickname) {
        return ProjectMemberEntity.builder()
                .userId(entity.getOwnerId())
                .userNickname(userNickname)
                .projectId(entity.getId())
                .role(role)
                .joinedAt(Instant.now())
                .build();
    }
}
