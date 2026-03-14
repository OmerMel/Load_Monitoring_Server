package com.finalproject.load_monitoring.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Custom exception to indicate that a requested resource was not found
@Getter
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    private String resourceName;
    private String fieldName;
    private Object fieldValue;

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Constructor for ResourceNotFoundException
    public ResourceNotFoundException(String message) {
        super(message);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Constructor for ResourceNotFoundException with resource name, field name, and field value
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
}