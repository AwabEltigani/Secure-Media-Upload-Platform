package com.example.cloud_file_storage.exceptions;

public class InvalidPasswordException extends RuntimeException{
    
    public InvalidPasswordException(String message) {
        super(message);
    }

    public InvalidPasswordException(String resource, String field, Object value) {
        super(String.format("%s not found with %s: %s", resource, field, value));
    }


    
}
