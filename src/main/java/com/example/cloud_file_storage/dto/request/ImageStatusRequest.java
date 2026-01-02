package com.example.cloud_file_storage.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data                
@Builder            
@NoArgsConstructor  
@AllArgsConstructor  
public class ImageStatusRequest {
    private String fileName;
    private String scanStatus;
}