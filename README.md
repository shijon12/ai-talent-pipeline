# AI Talent Pipeline

A local, event-driven Java microservices demo for candidate ingestion, AI-assisted scoring, human review, and mock scheduling outreach.

The system accepts a candidate resume PDF, extracts text, routes the application through Kafka, scores it against a target job, records lifecycle events in PostgreSQL, and lets a human reviewer approve or reject the candidate from a dashboard.

## Services

| Service | Port | Purpose |
| --- | ---: | --- |
| Dashboard / scoring-service | 8082 | Main UI, scoring, AI analysis, human review |
| ingestion-service | 8081 | PDF upload, Apache Tika extraction, ingestion outbox |
| scheduling-service | 8083 | Consumes approved candidate events and simulates outreach |
| PostgreSQL | 5432 | Shared local database, `pipeline_db` |
| Kafka | 9092 | Local event broker |
| Kafdrop | 9000 | Kafka topic/message inspection |

Recommended dashboard URL:

```text
http://127.0.0.1:8082/
```

Using `127.0.0.1` avoids occasional Windows `localhost`/IPv6 weirdness.

## Prerequisites

- Docker Desktop with WSL2 backend enabled
- Java 17
- PowerShell
- Ollama, optional but recommended for local AI analysis
- `llama3.2:1b` pulled locally if using Ollama

Check Java:

```powershell
java -version
```

Check Docker:

```powershell
docker version
docker compose version
```

Check Ollama:

```powershell
ollama --version
ollama list
```

If `llama3.2:1b` is missing:

```powershell
ollama pull llama3.2:1b
```

## Start the stack

From the project root:

```powershell
cd "D:\Build With Codex"
docker compose up -d --build
```

Check status:

```powershell
docker compose ps
```

Health checks:

```powershell
curl.exe http://127.0.0.1:8081/actuator/health
curl.exe http://127.0.0.1:8082/actuator/health
curl.exe http://127.0.0.1:8083/actuator/health
```

## Main URLs

```text
Dashboard: http://127.0.0.1:8082/
Kafdrop:   http://127.0.0.1:9000/
Ingestion: http://127.0.0.1:8081/actuator/health
Scoring:   http://127.0.0.1:8082/actuator/health
Scheduling:http://127.0.0.1:8083/actuator/health
```

## Demo flow

1. Open the dashboard:

   ```text
   http://127.0.0.1:8082/?v=latest
   ```

2. Use the `Ingest New Candidate` form.
3. Select a target job from the dropdown.
4. Upload a PDF resume.
5. Wait for the candidate to appear in the table.
6. Click `View packet`.
7. Review scores, job details, AI analysis, and lifecycle timeline.
8. Approve or reject the candidate.
9. If approved, scheduling-service should emit mock outreach and the lifecycle should move to `OUTREACH SENT`.

## Kafka topics

| Topic | Producer | Consumer |
| --- | --- | --- |
| `candidate-events` | ingestion-service | scoring-service |
| `candidate-lifecycle-events` | ingestion-service, scoring-service, scheduling-service | scoring-service lifecycle projection |
| `candidate-qualified-events` | scoring-service after human approval | scheduling-service |
| `candidate-scoring-dlq` | Kafka binder DLQ | troubleshooting |
| `candidate-lifecycle-dlq` | Kafka binder DLQ | troubleshooting |
| `candidate-scheduling-dlq` | Kafka binder DLQ | troubleshooting |

Inspect topics at:

```text
http://127.0.0.1:9000/
```

## Database

Database:

```text
pipeline_db
```

User/password:

```text
postgres / password123
```

Connect from PowerShell:

```powershell
docker compose exec postgres psql -U postgres -d pipeline_db
```

Useful tables:

```sql
select * from jobs;
select candidate_id, candidate_name, target_job_id, score, ai_analysis_status, scored_at from applicant_scores order by scored_at desc;
select * from candidate_reviews order by created_at desc;
select * from candidate_lifecycle_events order by occurred_at desc;
select * from ingestion.transactional_outbox order by created_at desc;
select * from scheduling.scheduling_outbox_events order by created_at desc;
```

## AI retry

If AI analysis falls back because Ollama is unavailable or memory is low, open `View packet` and use `Retry AI Analysis`.

Notes:

- Retry works for candidates uploaded after resume snapshot storage was added.
- Older records may show a message asking you to re-upload the resume.
- If system RAM is low, retry may still fall back.

Check available RAM:

```powershell
$os = Get-CimInstance Win32_OperatingSystem
[math]::Round($os.FreePhysicalMemory / 1MB, 2)
```

## Common troubleshooting

### Dashboard looks stale

Use a hard refresh:

```text
Ctrl + F5
```

Or open:

```text
http://127.0.0.1:8082/?v=latest
```

### Docker permission denied

Restart Docker Desktop, then sign out/sign in to Windows if needed.

### AI fallback used

Ollama may be unavailable or RAM may be low.

Check Ollama:

```powershell
curl.exe http://127.0.0.1:11434/api/tags
```

### Rebuild one service

```powershell
docker compose up -d --build scoring-service
docker compose up -d --build ingestion-service
docker compose up -d --build scheduling-service
```

### View logs

```powershell
docker compose logs --tail=200 scoring-service
docker compose logs --tail=200 ingestion-service
docker compose logs --tail=200 scheduling-service
```

## Stop the stack

```powershell
docker compose down
```

To remove local volumes and data:

```powershell
docker compose down -v
```

Only run `down -v` if you intentionally want to delete local Kafka/PostgreSQL data.

## GitHub CI/CD

This repository includes GitHub Actions workflows for CI checks and Docker image publishing.

See:

```text
docs/github-ci-cd.md
```

