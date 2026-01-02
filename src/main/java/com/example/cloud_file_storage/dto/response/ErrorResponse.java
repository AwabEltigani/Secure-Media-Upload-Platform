package com.example.cloud_file_storage.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
//only include feilds that are non null
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    LocalDateTime timestamp,
    int status,
    String error,
    String message,
    String path,
    Map<String,String> validationErrors
) 
{
    public ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path
    ){
        this(timestamp, status, error, message, path, null);
    }
}
