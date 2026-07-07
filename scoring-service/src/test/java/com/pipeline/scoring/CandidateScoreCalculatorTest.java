package com.pipeline.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CandidateScoreCalculatorTest {

    private final CandidateScoreCalculator calculator = new CandidateScoreCalculator();

    @Test
    void assignsBaselineScoreWithoutResumeText() {
        CandidateApplication candidate = new CandidateApplication(
                "candidate-1",
                "Test Candidate",
                "candidate@example.com",
                "",
                "job-1"
        );

        assertThat(calculator.calculate(candidate)).isEqualTo(30);
    }

    @Test
    void rewardsRelevantSkills() {
        CandidateApplication baseline = new CandidateApplication(
                "candidate-1",
                "Test Candidate",
                "candidate@example.com",
                "General software development experience",
                "job-1"
        );
        CandidateApplication skilled = new CandidateApplication(
                "candidate-1",
                "Test Candidate",
                "candidate@example.com",
                "Java, Spring Boot, Kafka, PostgreSQL, and microservice experience",
                "job-1"
        );

        assertThat(calculator.calculate(skilled))
                .isGreaterThan(calculator.calculate(baseline));
    }

    @Test
    void capsScoreAtOneHundred() {
        CandidateApplication candidate = new CandidateApplication(
                "candidate-1",
                "Test Candidate",
                "candidate@example.com",
                ("Java Spring Kafka PostgreSQL microservice ").repeat(100),
                "job-1"
        );

        assertThat(calculator.calculate(candidate)).isEqualTo(100);
    }
}
