# Demo Script

Use this script when presenting the project.

## 1. Start infrastructure and services

```powershell
cd "D:\Build With Codex"
docker compose up -d --build
docker compose ps
```

Confirm these services are healthy:

- postgres
- kafka
- ingestion-service
- scoring-service
- scheduling-service

## 2. Open the dashboard

```text
http://127.0.0.1:8082/?v=demo
```

Point out:

- service health indicator
- upload form
- job dropdown
- candidate evaluations table
- human review requirement

## 3. Upload a candidate

In `Ingest New Candidate`:

- Candidate name: `Alex Morgan`
- Email: `alex.demo@example.com`
- Target job: `Java Microservices Engineer — job-101`
- File: upload a software engineering PDF resume

Expected:

- Toast says the application was routed.
- Candidate appears in the table after processing.
- Status eventually reaches `REVIEW REQUIRED`.

## 4. Inspect Kafka

Open:

```text
http://127.0.0.1:9000/
```

Show:

- `candidate-events`
- `candidate-lifecycle-events`

## 5. Open Candidate Packet

Click `View packet`.

Show:

- target job details
- technical fit score
- experience alignment
- AI analysis/fallback message
- lifecycle timeline
- human review controls

## 6. Approve the candidate

Click `Approve`.

Reviewer:

```text
Demo Reviewer
```

Note:

```text
Candidate evidence matches the job requirements.
```

Expected:

- Review status becomes `APPROVED`.
- Candidate lifecycle moves toward `OUTREACH SENT`.
- scheduling-service logs mock outreach.

## 7. Inspect scheduling event

In Kafdrop, inspect:

```text
candidate-qualified-events
candidate-lifecycle-events
```

Expected lifecycle:

```text
INGESTED -> SCORED -> REVIEW REQUIRED -> APPROVED -> OUTREACH SENT
```

## 8. Show database state

```powershell
docker compose exec postgres psql -U postgres -d pipeline_db
```

Useful queries:

```sql
select candidate_id, candidate_name, target_job_id, score, ai_analysis_status
from applicant_scores
order by scored_at desc
limit 5;

select candidate_id, status, reviewer, review_note
from candidate_reviews
order by created_at desc
limit 5;

select candidate_id, status, source, occurred_at
from candidate_lifecycle_events
order by occurred_at desc
limit 10;
```

## 9. Show AI retry

If a candidate has `AI FALLBACK USED`, open `View packet`.

If retry is available, click:

```text
Retry AI Analysis
```

Explain:

- Retry uses the stored resume snapshot.
- Older records may require re-upload.
- Low RAM can still cause fallback.

## 10. Shut down

```powershell
docker compose down
```

