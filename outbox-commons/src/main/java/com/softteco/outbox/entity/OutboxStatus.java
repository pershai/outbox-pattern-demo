package com.softteco.outbox.entity;

/**
 * Status of an outbox event in the publishing lifecycle.
 */
public enum OutboxStatus {
    /**
     * Event is newly created and ready to be published.
     */
    NEW,

    /**
     * Event has been successfully published to the message broker.
     */
    SENT,

    /**
     * Event publishing failed after all retry attempts.
     */
    ERROR
}
