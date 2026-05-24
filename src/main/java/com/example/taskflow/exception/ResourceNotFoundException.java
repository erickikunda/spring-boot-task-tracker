package com.example.taskflow.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super("%s '%s' not found".formatted(resource, id));
    }
}
