# Privacy Policy

**Effective Date:** June 3, 2025
**Last Updated:** June 3, 2025
**Application:** HRIS Mobile Application
**Operated by:** PT Niger ("the Company", "we", "us", or "our")

---

## 1. Introduction

This Privacy Policy describes how PT Niger collects, uses, stores, discloses, and protects personal data and personally identifiable information processed through the HRIS Mobile Application ("the Application"). The Application is an internal Human Resource Information System deployed exclusively for authorized employees, administrators, and management personnel of PT Niger and its affiliated entities.

This Policy is established in compliance with and by reference to:

- **Republic of Indonesia Law No. 27 of 2022 on Personal Data Protection (UU PDP)**
- **Government Regulation of Indonesia No. 71 of 2019 on Electronic System and Transaction Operations (PP PSTE)**
- **Minister of Communication and Information Technology Regulation No. 20 of 2016 on Personal Data Protection in Electronic Systems (Permenkominfo 20/2016)**
- **EU General Data Protection Regulation (GDPR) 2016/679**, applied where processing involves data subjects in the European Economic Area
- **APEC Cross-Border Privacy Rules (CBPR) Framework**, applied where applicable to cross-border data flows in Asia Pacific jurisdictions

By accessing or using the Application, you acknowledge that you have read, understood, and agreed to the terms of this Privacy Policy. If you do not agree with the practices described herein, you must immediately discontinue use of the Application and notify your HR administrator.

---

## 2. Scope and Application

This Privacy Policy applies to:

- All personal data collected through the Application on Android devices
- Personal data stored in cloud-based services integrated with the Application
- Processing activities performed by authorized personnel using the Application
- Data subjects whose information is managed through the Application, including current employees, former employees, and contractors

This Policy does not apply to third-party websites, services, or applications that may be linked to or integrated with the Application beyond the scope described herein.

---

## 3. Data Controller and Data Processor

**Data Controller:**
PT Niger is the Data Controller responsible for determining the purposes and means of processing personal data through the Application.

**Contact for Data Protection Inquiries:**
Data Protection Officer
PT Niger
Email: privacy@ptniger.id
Address: [Company Address]

**Data Processor:**
Google LLC (Firebase) acts as a Data Processor on behalf of PT Niger. Firebase services process personal data according to the terms of the Google Cloud Data Processing Addendum.

---

## 4. Categories of Personal Data Collected

### 4.1 Identity and Contact Data
- Full legal name
- Employee Identification Number (NIK/Employee ID)
- Personal and work email address
- Assigned organizational role and department
- Branch or work location assignment

### 4.2 Employment and Organizational Data
- Job title and position
- Department and reporting structure
- Employment status (active, probation, resigned)
- Employment contract type
- Start date and employment history

### 4.3 Attendance and Location Data
- Daily check-in and check-out timestamps
- Geographic coordinates (GPS latitude and longitude) captured at the time of attendance recording
- Geofencing validation status relative to registered office locations
- Late arrival and early departure records
- Attendance photographs captured through device camera at check-in or check-out (where applicable)

### 4.4 Leave and Absence Data
- Leave request submissions including leave type, dates, duration, and stated reason
- Remaining leave quota balance
- Leave approval status and approver identity
- Historical leave records

### 4.5 Payroll and Financial Data
- Base salary and salary components
- Allowances and overtime compensation
- Statutory deductions (BPJS Ketenagakerjaan, BPJS Kesehatan, PPh 21)
- Bonus and incentive records including KPI-linked bonus calculations
- Net salary amounts
- Payroll period records and approval history
- Salary slip access logs

### 4.6 Performance Data
- Key Performance Indicator (KPI) configuration assigned to the employee
- KPI scoring records submitted by evaluators
- Performance period results and historical scores
- Bonus eligibility calculated from performance scores

### 4.7 Authentication and Security Data
- Firebase Authentication User ID (UID)
- Hashed credentials managed by Firebase Authentication (passwords are never stored in plain text by the Application or PT Niger)
- Session tokens and authentication timestamps
- Login attempt records

### 4.8 Profile and Communication Data
- Profile photograph uploaded by the user (stored in Firebase Storage)
- Notification preferences and in-app notification records
- System notification delivery logs

### 4.9 System and Audit Data
- All administrative actions performed within the Application (creates, updates, deletions, approvals)
- User agent and device identifiers where technically necessary for session management
- Audit log entries including timestamp, acting user, module, and affected record identifiers

---

## 5. Purposes of Processing and Legal Bases

### 5.1 Employment Contract Performance
We process identity, employment, attendance, leave, and payroll data to fulfill our obligations and enforce rights under the employment contract. This processing is necessary for the performance of the contract to which the data subject is a party (GDPR Art. 6(1)(b); UU PDP Art. 20(b)).

### 5.2 Legal Compliance
We process payroll, attendance, tax deduction, and employment data to comply with applicable Indonesian labor laws, tax regulations, and social security obligations under:
- Manpower Law No. 13 of 2003 and its amendments
- Income Tax Law (PPh 21) provisions
- BPJS Ketenagakerjaan and BPJS Kesehatan regulations
- Financial Services Authority (OJK) reporting requirements where applicable

This processing is necessary for compliance with legal obligations (GDPR Art. 6(1)(c); UU PDP Art. 20(c)).

### 5.3 Legitimate Interests
We process performance (KPI), attendance monitoring, and audit log data for the purposes of:
- Workforce management and organizational efficiency
- Prevention of fraud and unauthorized access
- Internal security monitoring and compliance auditing
- Maintaining the integrity and accuracy of employment records

This processing is based on the legitimate interests of the Company, provided that such interests are not overridden by the fundamental rights of the data subject (GDPR Art. 6(1)(f); UU PDP Art. 20(f)).

### 5.4 Consent-Based Processing
Profile photographs and optional personal data submitted voluntarily by users are processed on the basis of explicit consent. Data subjects may withdraw consent at any time by contacting the Data Protection Officer. Withdrawal of consent does not affect the lawfulness of processing prior to withdrawal.

---

## 6. Location Data and Geofencing

The Application requests access to device location (GPS) exclusively for the purpose of validating attendance records against registered office locations (geofencing). Specific disclosures:

- Location data is captured **only at the moment of attendance recording** and is not tracked continuously or in the background
- Precise GPS coordinates are transmitted to Cloud Firestore and retained as part of the attendance record
- Location access requires explicit permission granted by the user through the Android permission system
- Users may deny location permission, in which case attendance validation will default to manual review status (`NEED_REVIEW`)
- Location data is not sold to, shared with, or used by any third party for advertising or profiling purposes

---

## 7. Photography and Camera Data

Where the Application captures a photograph at check-in or check-out:

- Camera access is requested only at the moment of capture and is not used for continuous monitoring or background recording
- Photographs are transmitted to Firebase Storage and are linked to the corresponding attendance record
- Attendance photographs are accessible only to authorized HR and administrative personnel
- Device camera permission may be granted or denied through the Android permission system

---

## 8. Firebase and Third-Party Services

The Application is built on Google Firebase, a platform provided by Google LLC (1600 Amphitheatre Parkway, Mountain View, CA 94043, USA). The following Firebase services are used:

| Service | Purpose | Data Processed |
|---|---|---|
| Firebase Authentication | User identity and session management | Email address, hashed password, UID |
| Cloud Firestore | Primary application database | All structured application data |
| Firebase Storage | File storage for profile and attendance photos | Images uploaded by users |

**Cross-Border Data Transfer:** Google Firebase data infrastructure may process and store data on servers located outside the Republic of Indonesia, including in the United States and other jurisdictions. PT Niger ensures that such transfers are subject to Google's standard contractual clauses and data processing agreements compliant with applicable data protection laws.

Google's data processing practices are governed by the [Google Cloud Privacy Notice](https://cloud.google.com/terms/cloud-privacy-notice) and the [Google Cloud Data Processing Addendum](https://cloud.google.com/terms/data-processing-addendum).

---

## 9. Data Retention

| Data Category | Retention Period | Basis |
|---|---|---|
| Employee master data | Duration of employment + 5 years after separation | Indonesian Manpower Law obligations |
| Attendance records | 5 years from the date of record | Tax and labor compliance requirements |
| Payroll and salary data | 10 years from the applicable payroll period | Tax authority audit requirements |
| Leave records | 5 years from the date of record | Labor compliance |
| KPI and performance records | 5 years from the evaluation period | Internal policy |
| Audit log records | 3 years from the date of event | Security and compliance |
| Profile photographs | Until deleted by the user or upon account termination | Consent |
| Authentication data | Deleted within 30 days of account termination | Minimal retention |
| Attendance photographs | 2 years from the date of capture | Operational necessity |

Data will be securely deleted or anonymized upon expiration of the applicable retention period, unless further retention is required by law or ongoing legal proceedings.

---

## 10. Data Subject Rights

In accordance with UU PDP Article 5–16 and GDPR Articles 15–22, data subjects have the following rights:

### 10.1 Right of Access
You have the right to request confirmation of whether we process your personal data and to receive a copy of that data in a structured and readable format.

### 10.2 Right to Rectification
You have the right to request correction of inaccurate or incomplete personal data held about you. Corrections to employment records must be submitted through your HR administrator.

### 10.3 Right to Erasure
You have the right to request deletion of your personal data where it is no longer necessary for the purpose for which it was collected, subject to overriding legal retention obligations.

### 10.4 Right to Restriction of Processing
You have the right to request that we restrict the processing of your personal data under certain circumstances, including where you contest the accuracy of the data or object to processing based on legitimate interests.

### 10.5 Right to Data Portability
You have the right to receive your personal data in a structured, commonly used, and machine-readable format, and to transmit that data to another controller where technically feasible.

### 10.6 Right to Object
You have the right to object to processing based on legitimate interests where you believe your rights and freedoms override those interests. You may not object to processing required by law or for the performance of your employment contract.

### 10.7 Right to Withdraw Consent
Where processing is based on consent, you have the right to withdraw that consent at any time without affecting the lawfulness of prior processing.

### 10.8 Right to Lodge a Complaint
If you believe that the processing of your personal data violates applicable data protection law, you have the right to lodge a complaint with the relevant supervisory authority:

- **Indonesia:** Kominfo (Kementerian Komunikasi dan Informatika)
- **European Union:** Your national Data Protection Authority

To exercise any of the above rights, contact the Data Protection Officer at privacy@ptniger.id. We will respond within 14 calendar days for routine requests and within 30 calendar days for complex requests, as required by UU PDP and GDPR.

---

## 11. Security Measures

PT Niger and its data processors implement the following technical and organizational security measures:

- **Authentication:** All user access is protected by Firebase Authentication. Passwords are hashed using industry-standard algorithms and are never stored or transmitted in plain text
- **Encryption in Transit:** All communication between the Application and Firebase services is encrypted using TLS 1.2 or higher
- **Encryption at Rest:** Cloud Firestore and Firebase Storage encrypt data at rest using AES-256
- **Access Control:** Role-based access control (RBAC) is enforced at the application layer. Users may only access data corresponding to their assigned organizational role
- **Audit Logging:** All administrative and data modification actions are recorded in the audit log with timestamp, acting user identity, and affected record
- **Geofencing Validation:** Attendance records include GPS-based validation to prevent fraudulent submissions from unauthorized locations
- **Minimal Privilege:** The Application requests only the Android permissions necessary for its documented functionality

Despite these measures, no electronic system can guarantee absolute security. PT Niger will notify affected data subjects and relevant authorities in the event of a personal data breach in accordance with applicable law, within the timeframes prescribed by UU PDP and GDPR.

---

## 12. Automated Decision-Making

The Application uses automated processing in the following limited contexts:

- **Leave Quota Enforcement:** The Automation Rules module may automatically reject leave requests that exceed the employee's remaining quota. This constitutes automated decision-making that produces a legal or similarly significant effect
- **KPI Bonus Calculation:** Bonus eligibility and amounts are calculated automatically from KPI scores using predefined formulas

Data subjects have the right to request human review of any automated decision that significantly affects them by contacting their HR administrator or the Data Protection Officer.

---

## 13. Children's Data

The Application is designed exclusively for use by adult employees, contractors, and administrators. The Application does not knowingly collect personal data from persons under the age of 18. If you believe that data belonging to a minor has been processed through the Application, please contact the Data Protection Officer immediately.

---

## 14. Changes to This Privacy Policy

PT Niger reserves the right to update or amend this Privacy Policy at any time to reflect changes in applicable law, Firebase service terms, or organizational data practices. Material changes will be communicated to users through in-app notifications or email. Continued use of the Application following notification of a material change constitutes acceptance of the revised Policy. We encourage data subjects to review this Policy periodically.

---

## 15. Contact

For any questions, concerns, or requests relating to this Privacy Policy or the processing of your personal data:

**Data Protection Officer**
PT Niger
Email: privacy@ptniger.id
Address: [Company Address]

---

*This Privacy Policy was drafted with reference to UU No. 27 Tahun 2022 tentang Perlindungan Data Pribadi, GDPR 2016/679, Permenkominfo No. 20 Tahun 2016, PP No. 71 Tahun 2019, and APEC CBPR Framework principles.*
