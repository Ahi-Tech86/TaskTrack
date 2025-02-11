package com.ahicode.storage.repositories;

import com.ahicode.storage.entities.ProjectMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberRepository extends JpaRepository<Long, ProjectMemberEntity> {
}
