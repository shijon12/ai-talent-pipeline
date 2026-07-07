package com.pipeline.ingestion;

public enum OutboxStatus {
    PENDING,
    PROCESSED,
    FAILED
}
