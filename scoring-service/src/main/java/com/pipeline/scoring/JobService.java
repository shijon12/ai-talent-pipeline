package com.pipeline.scoring;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobService {

    private static final TypeReference<List<String>> STRING_LIST =
            new TypeReference<>() {
            };

    private final JobRepository jobRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public JobResponse save(JobRequest request) {
        List<String> required = normalize(request.requiredSkills());
        List<String> preferred = normalize(request.preferredSkills());
        Instant now = Instant.now();

        Job job = jobRepository.findById(request.jobId())
                .orElseGet(() -> new Job(
                        request.jobId(),
                        request.title().trim(),
                        request.description().trim(),
                        write(required),
                        write(preferred),
                        now
                ));

        job.update(
                request.title().trim(),
                request.description().trim(),
                write(required),
                write(preferred),
                now
        );
        return toResponse(jobRepository.save(job));
    }

    @Transactional(readOnly = true)
    public Optional<JobResponse> find(String jobId) {
        return jobRepository.findById(jobId).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<JobResponse> findAll() {
        return jobRepository.findAll().stream()
                .map(this::toResponse)
                .sorted((left, right) -> left.jobId().compareToIgnoreCase(right.jobId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<JobRequirements> findRequirements(String jobId) {
        return jobRepository.findById(jobId)
                .map(job -> new JobRequirements(
                        job.getJobId(),
                        job.getTitle(),
                        job.getDescription(),
                        read(job.getRequiredSkillsJson()),
                        read(job.getPreferredSkillsJson())
                ));
    }

    private JobResponse toResponse(Job job) {
        return new JobResponse(
                job.getJobId(),
                job.getTitle(),
                job.getDescription(),
                read(job.getRequiredSkillsJson()),
                read(job.getPreferredSkillsJson()),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }

    private List<String> normalize(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private String write(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize job skills", exception);
        }
    }

    private List<String> read(String value) {
        try {
            return objectMapper.readValue(value, STRING_LIST);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize job skills", exception);
        }
    }
}
