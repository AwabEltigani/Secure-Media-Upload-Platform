# Secure-Media-Upload-Platform
Zero-trust, event-driven secure file upload system using React, Spring Boot, and AWS GuardDuty.

System Design 

┌─────────────┐
│   React     │ (1) Request pre-signed URL
│   Frontend  │────────────────────────────────┐
└─────────────┘                                │
       │                                       ▼
       │ (2) Upload directly               ┌──────────────┐
       │     to S3                         │ Spring Boot  │
       └──────────────────┐                │   Backend    │
                          ▼                └──────────────┘
                 ┌─────────────────┐              │
                 │  S3 Quarantine  │◄─────────────┘
                 │     Bucket      │
                 └─────────────────┘
                          │
                          │ (3) Trigger scan
                          ▼
                 ┌─────────────────┐
                 │   AWS GuardDuty │
                 │ Malware Scanner │
                 └─────────────────┘
                          │
                          │ (4) Scan complete
                          ▼
                 ┌─────────────────┐
                 │  EventBridge    │
                 │   Event Bus     │
                 └─────────────────┘
                          │
                          │ (5) Trigger validation
                          ▼
                 ┌─────────────────┐
                 │  AWS Lambda     │
                 │   Function      │
                 └─────────────────┘
                     │           │
        (6a) Clean   │           │   (6b) Threat
                     ▼           ▼
            ┌──────────────┐  ┌──────────────┐
            │ S3 Permanent │  │   Delete +   │
            │    Bucket    │  │ Send Alert   │
            └──────────────┘  └──────────────┘
                     │
                     │ (7) Save metadata
                     ▼
            ┌──────────────┐
            │  PostgreSQL  │
            │   Database   │
            └──────────────┘

Technology Stack
Frontend

React 18+ - UI framework
Vite - Build tool and dev server
CSS - For design
Axios - HTTP client
React Dropzone - File upload component
React Hot Toast - Notifications

Backend

Spring Boot 3.2+
Spring Security - JWT authentication
Spring Data JPA - ORM layer
PostgreSQL - Relational database
Maven - Dependency management

AWS Services

S3 - Object storage (quarantine + permanent buckets)
GuardDuty - Malware detection
EventBridge - Event routing
Lambda - Serverless validation
SES - Email notifications
RDS - Managed PostgreSQL (production)
App Runner - Container deployment
Amplify - Frontend hosting

