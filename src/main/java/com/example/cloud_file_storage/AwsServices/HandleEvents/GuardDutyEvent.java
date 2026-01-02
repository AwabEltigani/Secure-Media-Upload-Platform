package com.example.cloud_file_storage.AwsServices.HandleEvents;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GuardDutyEvent {
    private String id;
    private String region;
    private Detail detail;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Detail {
        private String scanId;
        private String scanType;
        private Long scanStartTime;
        private Long scanEndTime;
        private ScanResultDetails scanResultDetails;
        private Resource resource;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScanResultDetails {
        private String scanResultStatus;
        private List<Threat> threats;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Threat {
        private String name;
        private String severity;
        private Integer itemCount;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Resource {
        private S3Bucket s3Bucket;
        
        public S3Object getS3Object() { 
            return (s3Bucket != null) ? s3Bucket.getS3Object() : null; 
        }
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class S3Bucket {
        private String bucketName;
        private S3Object s3Object;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class S3Object {
        private String key;
        private String versionId;
        private String etag;
    }
}