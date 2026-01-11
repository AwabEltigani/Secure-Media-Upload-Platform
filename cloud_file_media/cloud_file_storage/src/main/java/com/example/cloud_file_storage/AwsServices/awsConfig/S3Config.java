package com.example.cloud_file_storage.AwsServices.awsConfig;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/*
Configuration for the S3 bucket
region -> where it is located
accessKey -> the credentials to the bucket 
secretKey -> the secret credentials to the bucket

S3Client is what creates and registers the AWS s3 client as a
Spring Bean which can be injected any where in the codebase
makes calls to AWS

s3Presigner is the bean that is used to generate the presignedUrls
for the opertations done on the S3 buckets uploud/download without 
exposing the users credentials 

*/
@Configuration // marks this file to be searched for beans
public class S3Config {

        @Value("${AWS_REGION}")
        private String region;

        @Value("${AWS_ACCESS_KEY}")
        private String accessKey;

        @Value("${AWS_SECRET_KEY}")
        private String secretKey;

        @Bean
        public S3Client s3Client() {
        AwsBasicCredentials credentials =
                AwsBasicCredentials.create(accessKey, secretKey);

        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(credentials)
                )
                .build();
        }

        @Bean
        public S3Presigner s3Presigner() {
        AwsBasicCredentials credentials =
                AwsBasicCredentials.create(accessKey, secretKey);
        
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
}
}
