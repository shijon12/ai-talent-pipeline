# Architecture

The AI Talent Pipeline is built as three Spring Boot 3.5 microservices connected by Kafka and backed by PostgreSQL.

## High-level flow

```text
Dashboard
   |
   | multipart PDF upload
   v
ingestion-service
   |
   | Apache Tika extracts resume text
   | transactional outbox persists APPLICATION_INGESTED
   v
Kafka: candidate-events
   |
   v
scoring-service
   |
   | deterministic score
   | optional Ollama advisory analysis
   | human review record
   v
Dashboard human review
   |
   | approve
   v
Kafka: candidate-qualified-events
   |
   v
scheduling-service
   |
   | mock outreach
   v
Kafka: candidate-lifecycle-events
```

## Service responsibilities

### ingestion-service

- Receives multipart candidate applications at `POST /v1/applications`.
- Accepts `name`, `email`, `targetJobId`, and `file`.
- Uses Apache Tika to extract text from PDF resumes.
- Writes ingestion state to PostgreSQL using a transactional outbox.
- Publishes candidate application events to Kafka.

### scoring-service

- Serves the dashboard on port `8082`.
- Consumes `candidate-events`.
- Calculates deterministic fit scores.
- Calls local Ollama via Spring AI when available.
- Stores applicant scores, AI summaries, review state, jobs, and lifecycle projection.
- Exposes review endpoints and AI retry endpoint.
- Emits `candidate-qualified-events` only after human approval.

### scheduling-service

- Consumes approved candidate events from `candidate-qualified-events`.
- Simulates autonomous outreach.
- Emits lifecycle events showing outreach was sent.

## Data ownership

| Area | Owner |
| --- | --- |
| Raw ingestion outbox and lifecycle trail | ingestion-service |
| Scores, jobs, AI analysis, reviews, lifecycle projection | scoring-service |
| Scheduling/outreach state | scheduling-service |

## Kafka topics

| Topic | Event type |
| --- | --- |
| `candidate-events` | Candidate application payloads |
| `candidate-qualified-events` | Human-approved candidate events |
| `candidate-lifecycle-events` | Cross-service lifecycle status |

## Human review gate

The system intentionally does not auto-advance high scoring candidates.

Instead:

1. scoring-service scores the candidate.
2. scoring-service creates a pending review.
3. Dashboard shows `DECISION REQUIRED`.
4. Reviewer approves or rejects.
5. Only approval releases `CandidateQualifiedEvent`.
6. scheduling-service sends mock outreach.

This keeps AI advisory output out of final decision-making.

## AI analysis behavior

Ollama is optional. If unavailable, scoring still completes with deterministic rules.

AI statuses:

- `COMPLETED`
- `UNAVAILABLE`
- `JOB_NOT_FOUND`
- `NOT_ANALYZED`

If AI fails, the dashboard warns the reviewer and allows retry for records that have stored resume snapshots.

## Transactional outbox pattern

The project uses transactional outbox patterns to reduce the chance of database/Kafka inconsistencies.

Important outbox tables:

- `ingestion.transactional_outbox`
- `scoring_outbox_events`
- `scheduling.scheduling_outbox_events`

Each service writes local database state and outgoing events in one transaction, then a poller publishes events to Kafka.

