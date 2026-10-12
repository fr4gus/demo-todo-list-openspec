# Description

A TODO list app, where different users can keep their list separately, once an item is marked as completed it will go away. Each item will have an id, level of priority (high, medium-high, medium-low, low ) and text. For user it will collect their fullname (one field) email and password. The application will be a web app.

# 1. Tech Stack

## Frontend (Single Page Application)

- **Framework & Build Tool:** ReactJS built with Vite (TypeScript preferred)
- **State Management:** Redux Toolkit (RTK) with RTK Query for API data fetching
- **Routing:** React Router v7 (or latest stable)
- **Styling:** Tailwind CSS v4 (using the Vite plugin setup)

## Backend (REST API)

- **Framework:** Java Spring Boot 3.x (Java 21 LTS)
- **Database & Persistence:** PostgreSQL with Spring Data JPA & Liquibase/Flyway for database migrations
- **Security:** Spring Security with stateless JWT Authentication and Role-Based Access Control (RBAC)
- **Caching:** Redis (integrated via Spring Cache abstraction)
  •••••••••••••

## DevOps & Infrastructure

- **Containerization:** Multi-stage Dockerfiles for local development and production environments
- **Orchestration:** Docker Compose file linking Frontend, Backend, PostgreSQL, and Redis
- **CI/CD Pipeline:** [e.g., GitHub Actions / GitLab CI] config file targeting automated linting, testing, and Docker image compilation

# 2. ARCHITECTURAL & COMPLIANCE REQUIREMENTS

## Frontend Architecture

- Provide a clean, scalable folder structure separating components, features, hooks, routing, and the Redux store.
- Configure a base RTK Query API slice with automatic header injection for JWT authentication tokens.
- Include a protective `ProtectedRoute` wrapper component to restrict routes based on user authentication state.

## Backend Architecture

- Follow a standard multi-layer architecture: Controller -> Service -> Repository -> Entity.
- Include a global exception handler (`@ControllerAdvice`) that maps custom exceptions to standard RFC 7807 problem details.
- Implement structured logging (Logback/SLF4J) outputting JSON for easy log aggregation.
- Implement a Redis caching strategy on read-heavy endpoints with clear cache-eviction annotations (`@Cacheable`, `@CacheEvict`).

## Database & Security Constraints

- Database tables must use proper indexing, foreign keys, and audit fields (`created_at`, `updated_at`).
- Password storage must use BCrypt hashing.
- Enforce CORS configurations to strictly allow only the specified frontend origin.

---

# 4. EXPECTED OUTPUTS

Please deliver this step-by-step:

1. **Directory Tree Blueprint:** A visual layout of the entire workspace showing where the frontend, backend, docker, and CI/CD folders live.
2. **Configuration Files:** The `vite.config.ts`, `pom.xml`, `Dockerfile`s, and `docker-compose.yml`.
3. **Core Authentication Flow Code:** Provide the foundational code for the Spring Security JWT filter and the corresponding Redux Toolkit slice + React Router setup.
4. **Step-by-step Execution Guide:** Terminal commands required to spin up the entire ecosystem locally using Docker Compose.

# 5. Testing & Quality Assurance

- **Frontend Testing Stack:** Vitest, React Testing Library, and MSW (Mock Service Worker) for network mocking.
  - Provide a test for a component that dispatches a Redux action.
  - Provide a component integration test utilizing MSW to simulate a successful and failed backend API response.
- **Backend Testing Stack:** JUnit 5, Mockito, and AssertJ.
  - **Unit Tests:** Mock the Repositories using Mockito to test isolated Service layer logic and validation boundaries.
  - **Integration Tests:** Use `@SpringBootTest` with `@AutoConfigureMockMvc` to test authenticated Controller endpoints, verifying security filters (JWT validation) and correct HTTP response statuses.
- **Database Integration Testing:** Use Testcontainers with a real PostgreSQL container instance for running database repository integration tests, avoiding H2 in-memory databases to ensure dialetic compatibility.
- **Test Isolation:** Ensure all tests run completely isolated without shared state, and code coverage configurations target a baseline of 80% coverage.

# 6. Screen flow

## Sign in/up

- First screen is sign in page (email, password) with the option of signing up and password recovery.
- The sing up screen will collect all user information (full name, email) and password. If an account already exists (an email can only be used in one account) an error will be shown to the user saying that the email already is in use. If the account is created without issues, the user will be sent back to the login screen.
- If the password associated with the email doesn't match, an error is shown "invalid password".
- If the email doesn't exist, an error is shown "Email doesn't exist"
  On successful log in the user is sent to the main screen of the list of TODO items.

## Main Screen (TODO List)

- If there is no todo items, show a text in the middle of the screen saying "No tasks to do this time!". With a Floating Actionable Button (FAB) saying "Create TODO".
- If there is at least one item, it will show the list of items based in their priority. At the bottom a "Create TODO" FAB style.
- Each item in the list, have a checkbox. If the user clicks on it, it will mark the task as completed and a "Toast" message will appear at the bottom saying "Task Completed - Undo" where the Undo is an actionable link that will revert the item completion and will show back at the list.
- Any completed item will be grayedout and with strikeline and gone to the end of the list. Long press on any item will pop up a dialog to confirm put back the item as incompleted.

## Create TODO Item

- A dialog screen that will appear if the main screen FAB is clicked. Here the user can enter the text for the todo and the priority. If high priority is selected, red is the background color for the item. If medium-high is selected its color is orange, if medium-low is selected its color is green, if low is selected its color is blue. ID will be something used internally and not visible to the user.
