# RoleMate

RoleMate is a platform designed to help job seekers practice interviews by instantly connecting them with other candidates preparing for similar roles.

## How to Run the Project

### Prerequisites
- **Java 25** or higher
- **Maven 3.9+**
- **PostgreSQL 18** (Required for the Phase 2 persistence features)

### Running the Backend Locally
1. Clone the repository and navigate to the project directory.
2. Ensure you have a local instance of PostgreSQL running and properly configured in `backend/src/main/resources/application.properties` (if applicable).
3. Navigate into the backend folder:
   ```bash
   cd backend
   ```
4. Run the application using the Spring Boot Maven Plugin:
   ```bash
   mvn spring-boot:run
   ```
   The backend service will start up and run on `localhost:8080`.

### Running Tests
To run the automated tests (unit and integration), execute:
```bash
mvn test
```

### Endpoints to Verify
Once the application is running, you can verify it by hitting these endpoints:
- **Health Check**: `http://localhost:8080/api/health`
- **Supported Roles**: `http://localhost:8080/api/roles`
- **Queue Status**: `http://localhost:8080/api/queue/status`
- **WebSocket Connection**: Connect via WebSocket to `ws://localhost:8080/ws/matchmaking`

> **Note on Documentation**: For comprehensive developer guides, project roadmap, and the product overview, refer to the local `rolemate documentation/` folder.
