# 🎓 School Eligibility & Admission System Backend

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.2-brightgreen)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A robust, enterprise-grade Spring Boot backend designed to streamline school admission processes, student eligibility verification, and payment management. This system provides a secure and scalable API for educational institutions to manage programs, track applications, and automate student notifications.

---

## 🚀 Key Features

### 🔐 Security & Authentication
- **Multi-Auth Support**: Integrated with **JWT (JSON Web Tokens)** and **Google OAuth2**.
- **Role-Based Access Control (RBAC)**: Distinct permissions for `ADMIN`, `USER`, and `GUEST`.
- **Account Activation**: Secure email-based account activation flow.

### 📊 Eligibility & Academic Management
- **Eligibility Engine**: Intelligent check for student eligibility based on program requirements.
- **WAEC Integration**: Real-time verification of results via the WAEC API.
- **Program Management**: Comprehensive CRUD for categories, universities, and academic programs.
- **PDF Reports**: Automated generation of eligibility reports using **OpenPDF**.

### 💳 Payment & Notifications
- **Moolre Integration**: Mobile Money (MoMo) payment processing with STK Push and Webhook support.
- **MNotify SMS**: Automated SMS alerts for important updates and transaction statuses.
- **Email System**: Rich HTML email templates (Thymeleaf) for activations, discounts, and successes.

### 🤖 Intelligent Features
- **AI Chatbot**: Integrated with **Google Gemini** and **OpenAI** for student assistance.
- **Logging & Monitoring**: Detailed logging for security auditing and debugging.

---

## 🛠 Tech Stack

- **Backend**: Java 21, Spring Boot 3.2.2
- **Persistence**: MySQL, Spring Data JPA, Hibernate
- **Security**: Spring Security, JJWT, OAuth2 Client/Resource Server
- **Documentation**: Springdoc OpenAPI (Swagger UI)
- **Utilities**: Lombok, MapStruct, Apache HttpClient 5
- **Reporting**: OpenPDF, Thymeleaf
- **DevOps**: Docker, Docker Compose, Maven

---

## 📋 Prerequisites

- **Java JDK 21**
- **Maven 3.8+**
- **MySQL 8.0+**
- **Docker** (Optional, for Maildev/Keycloak)

---

## ⚙️ Getting Started

### 1. Clone the Repository
```bash
git clone <repository-url>
cd SchoolBackend
```

### 2. Configuration
The application uses `application.yml` and environment-specific profiles (`dev`, `prod`). Create a `.env` file or update `src/main/resources/application-dev.yml` with your credentials:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/school
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

# Third-party Integrations
moolre:
  public-key: ${MOOLRE_PUBLIC_KEY}
waec:
  api:
    token: ${WAEC_TOKEN}
google:
  gemini:
    api:
      key: ${GEMINI_KEY}
```

### 3. Run with Maven
```bash
./mvnw clean spring-boot:run
```

### 4. Run with Docker (Infrastructure)
To start services like **Maildev** (for email testing) and **Keycloak**:
```bash
docker-compose up -d
```

---

## 📖 API Documentation

Once the application is running, you can access the interactive Swagger documentation at:
- **Swagger UI**: `http://localhost:8088/api/v1/swagger-ui.html`
- **OpenAPI Docs**: `http://localhost:8088/api/v1/v3/api-docs`

---

## 📂 Project Structure

```text
src/main/java/com/alibou/book/
├── auth/           # Authentication logic
├── config/         # Security & App configurations
├── Controllers/    # REST API Endpoints
├── DTO/            # Data Transfer Objects
├── Entity/         # Database Entities
├── Repositories/   # JPA Data Repositories
├── Services/       # Business Logic
├── email/          # Email service implementation
├── handler/        # Exception handlers
└── security/       # JWT & Security filters
```

---

## 📧 Contact & Support
For any inquiries or support regarding this backend, please reach out to the project administrator.

---
*Created with ❤️ by the School Project Team*
