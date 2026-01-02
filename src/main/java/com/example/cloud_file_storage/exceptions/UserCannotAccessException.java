package com.example.cloud_file_storage.exceptions;

public class UserCannotAccessException extends RuntimeException{
    
    public UserCannotAccessException(String message){
        super(message);
    }

    public UserCannotAccessException(){
        super("User not Authorized");
    }
}
