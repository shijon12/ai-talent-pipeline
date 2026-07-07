package com.pipeline.scoring;

public class AiRetryUnavailableException extends RuntimeException {

    public AiRetryUnavailableException(String message) {
        super(message);
    }
}
