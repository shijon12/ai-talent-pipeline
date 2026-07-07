INSERT INTO jobs (
    job_id,
    title,
    description,
    required_skills_json,
    preferred_skills_json,
    created_at,
    updated_at
) VALUES
(
    'job-101',
    'Senior Java / Spring Boot Engineer',
    'Build event-driven Java microservices for the AI Talent Pipeline using Spring Boot, Kafka, PostgreSQL, Docker, and production-grade API practices.',
    '["Java 17","Spring Boot","Apache Kafka","PostgreSQL","REST APIs","Docker","Microservices"]',
    '["Spring Cloud Stream","Flyway","Transactional Outbox","Kubernetes","Observability","AI integration"]',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'job-102',
    'Data Platform Engineer',
    'Design reliable data pipelines, event streams, and analytical storage patterns for operational talent intelligence workloads.',
    '["SQL","PostgreSQL","Kafka","Data Modeling","ETL","Python","Distributed Systems"]',
    '["dbt","Airflow","Spark","Docker","Cloud Platforms","Monitoring"]',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'job-103',
    'AI Product Engineer',
    'Create user-facing AI workflow features that combine strong product sense, backend engineering, model integration, and thoughtful UX.',
    '["Spring Boot","REST APIs","JavaScript","AI Integration","Prompt Engineering","Product Thinking","User Experience"]',
    '["Spring AI","Ollama","OpenAI APIs","Tailwind CSS","Kafka","PostgreSQL"]',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (job_id) DO NOTHING;
