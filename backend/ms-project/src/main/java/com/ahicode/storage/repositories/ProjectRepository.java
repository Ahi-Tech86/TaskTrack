package com.ahicode.storage.repositories;

import com.ahicode.storage.entities.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Long, ProjectEntity> {
}
