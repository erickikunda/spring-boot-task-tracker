# TaskFlow (Spring Boot Task Tracker)

TaskFlow is a production-realistic REST API designed as a study guide for building robust backend services. It is a Task Tracker where users belong to projects, projects contain tasks, and tasks have comments.

This project exercises every layer of the Spring Boot framework in a single monolith, demonstrating best practices in domain modeling, security, configuration, and data access.

## Features

- **Domain Model**: Comprehensive task tracking with projects, tasks, comments, and role-based access.
- **Strict Layered Architecture**: Controller → Service → Repository with explicit transactional boundaries.
- **Security**: Stateless JWT-based authentication and role-based authorization (ADMIN, MANAGER, MEMBER).
- **Database Migrations**: Flyway for plain-SQL schema migrations.
- **Multi-Profile Configuration**: H2 in-memory database for rapid development, and PostgreSQL via Docker Compose for production.

## Technology Stack

- **Java 21**
- **Spring Boot 4.0** (Spring Framework 7, Virtual Threads enabled)
- **Spring Security & JJWT** (JWT generation and validation)
- **Spring Data JPA & Hibernate**
- **PostgreSQL 16** (Production) / **H2 Database** (Development)
- **Flyway** (Schema migrations)
- **Maven** (Build tool)
- **Lombok** (Boilerplate reduction)

## Architecture

TaskFlow uses a strict three-layer architecture to enforce clear boundaries:

1.  **Web Layer (`web/`)**: REST Controllers and DTOs. Handles HTTP/JSON, validation, and security context.
2.  **Service Layer (`service/`)**: Business logic and transaction management. Violations throw domain exceptions.
3.  **Repository Layer (`repository/`)**: Data access via Spring Data JPA and custom queries.

## Domain Model

-   **User**: `id`, `email`, `display_name`, `password_hash`, `role`
-   **Project**: `id`, `name`, `description`, `status`, `owner_id` (User)
-   **Task**: `id`, `title`, `description`, `status`, `priority`, `due_date`, `project_id`, `assignee_id`, `reporter_id`
-   **Comment**: `id`, `body`, `task_id`, `author_id`

## Getting Started

### Development Profile (Default)

The `dev` profile uses an in-memory H2 database. This provides a clean slate on every restart and requires no external infrastructure.

```bash
./mvnw spring-boot:run
```

- **H2 Console**: Available at `/h2-console`
- **Configuration**: `application-dev.properties`

### Production Profile

The `prod` profile requires a PostgreSQL database. You can start one locally using Docker Compose.

1.  Start PostgreSQL:
    ```bash
    docker-compose up -d
    ```
2.  Run the application with the `prod` profile:
    ```bash
    SPRING_PROFILES_ACTIVE=prod \
      DB_URL=jdbc:postgresql://localhost:5432/taskflow \
      DB_USER=appuser \
      DB_PASSWORD=changeme \
      JWT_SECRET=your-256-bit-secret-here \
      ./mvnw spring-boot:run
    ```
- **Configuration**: `application-prod.properties` (uses Flyway for migrations)
