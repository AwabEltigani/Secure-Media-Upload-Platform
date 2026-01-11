package com.example.cloud_file_storage.exceptions;

public class UserDoesNotExistException extends RuntimeException{

    public UserDoesNotExistException(String message) {
        super(message);
    }

    public UserDoesNotExistException(Long id){
        super(String.format("User with id:%s not found", id));
    }
}