package com.pipeline.scoring;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Job {

    @Id
    @Column(name = "job_id", nullable = false, length = 100)
    private String jobId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "required_skills_json", nullable = false, columnDefinition = "TEXT")
    private String requiredSkillsJson;

    @Column(name = "preferred_skills_json", nullable = false, columnDefinition = "TEXT")
    private String preferredSkillsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Job(
            String jobId,
            String title,
            String description,
            String requiredSkillsJson,
            String preferredSkillsJson,
            Instant now
    ) {
        this.jobId = jobId;
        this.createdAt = now;
        update(title, description, requiredSkillsJson, preferredSkillsJson, now);
    }

    public void update(
            String title,
            String description,
            String requiredSkillsJson,
            String preferredSkillsJson,
            Instant now
    ) {
        this.title = title;
        this.description = description;
        this.requiredSkillsJson = requiredSkillsJson;
        this.preferredSkillsJson = preferredSkillsJson;
        this.updatedAt = now;
    }
}
