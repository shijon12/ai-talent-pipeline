package com.pipeline.scoring;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @GetMapping
    public List<JobResponse> findAll() {
        return jobService.findAll();
    }

    @PostMapping
    public ResponseEntity<JobResponse> save(@Valid @RequestBody JobRequest request) {
        JobResponse response = jobService.save(request);
        return ResponseEntity
                .created(URI.create("/v1/jobs/" + response.jobId()))
                .body(response);
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<JobResponse> find(@PathVariable String jobId) {
        return jobService.find(jobId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
