<div align="center">
  <h1>Secure File Upload & Malware Scanning System</h1>

  <p>Zero trust, event driven secure file upload system using React, Spring Boot, and AWS GuardDuty.</p>

  <p>
    A cloud-native, security first file upload pipeline built with React, Spring Boot,
    and AWS. Designed to safely handle untrusted uploads using quarantine storage,
    automated malware scanning, and event driven validation.
  </p>

  <br/>

 
  <img src="https://img.shields.io/badge/React-18+-61DAFB?logo=react&logoColor=black" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2+-6DB33F?logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/PostgreSQL-Database-336791?logo=postgresql&logoColor=white" />
  <img src="https://img.shields.io/badge/AWS-Cloud-FF9900?logo=amazonaws&logoColor=white" />
  <img src="https://img.shields.io/badge/Security-Zero--Trust-red" />
</div>

<br/>

<div align="center">
  <h2>System Architecture</h2>
</div>

<pre style="font-family: monospace; font-size: 14px; line-height: 1.3;">
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
</pre>

<br/>

<div align="center">
  <h2>Technology Stack</h2>
</div>

<h3>Frontend</h3>
<ul>
  <li><b>React 18+</b> – UI framework</li>
  <li><b>Vite</b> – Build tool and development server</li>
  <li><b>CSS</b> – Application styling and layout</li>
  <li><b>Axios</b> – HTTP client for API communication</li>
  <li><b>React Dropzone</b> – Drag-and-drop file upload</li>
  <li><b>React Hot Toast</b> – User notifications</li>
</ul>

<h3>Backend</h3>
<ul>
  <li><b>Spring Boot 3.2+</b> – REST API framework</li>
  <li><b>Spring Security</b> – JWT authentication</li>
  <li><b>Spring Data JPA</b> – ORM and persistence layer</li>
  <li><b>PostgreSQL</b> – Relational database</li>
  <li><b>Maven</b> – Dependency and build management</li>
</ul>

<h3>AWS Services</h3>
<ul>
  <li><b>Amazon S3</b> – Object storage (Quarantine & Permanent)</li>
  <li><b>AWS GuardDuty</b> – Malware detection</li>
  <li><b>Amazon EventBridge</b> – Event routing</li>
  <li><b>AWS Lambda</b> – Serverless validation</li>
  <li><b>Amazon SES</b> – Email alerts</li>
  <li><b>Amazon RDS</b> – Managed PostgreSQL (production)</li>
  <li><b>AWS App Runner</b> – Backend deployment</li>
  <li><b>AWS Amplify</b> – Frontend hosting & CI/CD</li>
</ul>
