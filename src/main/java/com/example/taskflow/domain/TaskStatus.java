package com.example.taskflow.domain;

import java.util.Set;

public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    IN_REVIEW,
    DONE,
    CANCELLED;

    // Allowed transitions encoded as a switch — exhaustive by compiler enforcement.
    // DONE and CANCELLED are terminal: no outbound transitions.
    public boolean canTransitionTo(TaskStatus next) {
        return switch (this) {
            case TODO        -> Set.of(IN_PROGRESS, CANCELLED).contains(next);
            case IN_PROGRESS -> Set.of(IN_REVIEW, TODO, CANCELLED).contains(next);
            case IN_REVIEW   -> Set.of(DONE, IN_PROGRESS, CANCELLED).contains(next);
            case DONE, CANCELLED -> false;
        };
    }
}
