package com.ahicode.storage.entities;

import com.ahicode.storage.enums.ProjectRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "project_memb")
public class ProjectMemberEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "user_id")
    private Long userId;

    @NotNull
    @Size(min = 3, max = 50)
    @Column(name = "nickname")
    private String userNickname;

    @NotNull
    @Column(name = "project_id")
    private Long projectId;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ProjectRole role;

    @NotNull
    @Column(name = "joined_at")
    private Instant joinedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    private ProjectEntity project;
}
