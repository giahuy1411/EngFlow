# Project Rules — EngFlow (English Learning Platform)

> [!IMPORTANT]
> You are the AI Agent assisting with the EngFlow project. You MUST follow these rules strictly for all code changes, configurations, and architectural decisions.

## 1. AI Agent Workflow

- **Context-First Approach**: Before writing code, you MUST explore the codebase to understand how existing components are structured and interact.
- **Explain Before Implementing**: Explain the planned changes and logic briefly before executing them. No surprise changes.
- **Identify Affected Modules**: Recognize which files/modules are impacted by a change and read them before modifying.
- **Testing Requirement**: Write tests (JUnit for backend, Vitest for frontend) to verify logic, or at minimum instruct the user on how to test manually if automated testing isn't applicable.
- **Ask Before Architecture Shifts**: Do not introduce new massive dependencies or change core architectural patterns without asking the user.
- **Conventional Commits**: Use conventional commits (feat, fix, refactor, docs, chore, etc.) when describing your changes or executing git commands.

## 2. Absolute Prohibitions (The Banned List)

- 🚫 **No secrets in code**: Never commit API keys, passwords, or secrets. Use environment variables.
- 🚫 **No `@Autowired` field injection**: Use `@RequiredArgsConstructor` for constructor injection in Spring.
- 🚫 **No direct Axios calls in Vue components**: All API calls must go through the `src/services/` layer.
- 🚫 **No hardcoded URLs/ports**: Use `VITE_` variables in frontend and `application.properties` in backend.
- 🚫 **No `SELECT *`**: Always specify the columns you need.
- 🚫 **No `System.out.println`**: Use SLF4J logger (`@Slf4j`) for all backend logging.
- 🚫 **No exposing sensitive data**: Never return password hashes or internal server paths in API responses.

## 3. Backend (Java 25 / Spring Boot 4.0.6)

### Naming Conventions
- **PascalCase**: Classes, Interfaces, Enums (`LessonService`, `UserRole`).
- **camelCase**: Methods, Variables (`getUserById`, `createLesson`).
- **UPPER_SNAKE_CASE**: Constants (`MAX_LOGIN_ATTEMPTS`).
- **Standard Suffixes**: `*Controller`, `*Service`, `*ServiceImpl`, `*Repository`, `*Request`, `*Response`, `*Exception`.

### Architecture & Packages (`com.datn.engflow`)
Maintain strict layering:
- `config/`: Spring `@Configuration` classes.
- `controller/`: `@RestController` classes handling HTTP requests.
- `service/`: Interfaces for business logic.
- `service/impl/`: `@Service` implementations.
- `repository/`: Spring Data JPA repositories.
- `model/entity/`: `@Entity` classes (JPA).
- `model/dto/request/`, `model/dto/response/`: Inbound/outbound DTOs.
- `security/`: Security configs, JWT filters.
- `exception/`: Custom exceptions and global handlers.

### Validation & Error Handling
- Use `@Valid` on all `@RequestBody` controller parameters.
- Handle all exceptions centrally via a `@ControllerAdvice` or `@RestControllerAdvice`.
- **Error Responses**: Use Spring Boot 4's native **RFC 7807 Problem Details** (`ProblemDetail` class). Avoid custom/inconsistent error response shapes. Do not leak stack traces to the client.

## 4. Frontend (Vue 3 / Vite)

### Architecture & Style
- **Composition API**: Use `<script setup>` for all Vue components. Do not use the Options API.
- **State Management**: Use **Pinia**. Avoid Vuex.
- **File Structure**: `components/`, `views/`, `services/` (Axios calls), `store/` (Pinia), `router/`.
- **CSS Strategy (TailwindCSS + Bootstrap)**: 
  - **TailwindCSS** is the primary styling tool for all new UI components and layouts.
  - **Bootstrap** is retained for legacy UI elements or specific complex components that haven't been migrated yet. Do not add new Bootstrap dependencies if Tailwind can easily accomplish the task.

### Coding Practices
- **Variables**: Use `VITE_` prefix for environment variables.
- **Naming**: PascalCase for components (`LessonCard.vue`), camelCase for composables (`useAuth.js`).
- **Loading & Error States**: Components fetching data must explicitly handle loading and error states.

## 5. REST API Design

- **URLs**: kebab-case, plural nouns, no verbs (`/api/v1/lessons`, not `/api/v1/getLesson`).
- **Methods**: GET (read), POST (create), PUT (replace), PATCH (partial update), DELETE (remove).
- **Pagination**: All endpoints returning lists must be paginated by default.
- **Auth**: Use `Authorization: Bearer <token>`. Do not send tokens in the body or query params.

## 6. Database (SQL Server)

- **Schema Management**: The project currently uses `hibernate.ddl-auto=update` and Flyway is disabled. While acceptable for early development, always plan schema changes carefully to avoid data loss.
- **Data Types**: Use `NVARCHAR` for strings to support Vietnamese text.
- **Performance**: Prevent N+1 query problems using `JOIN FETCH`, `@EntityGraph`, or appropriate projections.

## 7. Security

- **JWT Tokens**: Access tokens must have a short TTL (**15-30 minutes**). Refresh tokens can live longer (e.g., 7 days).
- **Passwords**: Hash passwords with BCrypt (strength 10+).
- **CORS**: Configure properly to only allow trusted frontend origins.
- **Rate Limiting**: Implement rate limiting for sensitive endpoints (like login/OTP) to prevent brute force attacks.

## 8. Testing (JUnit 5 + Vitest)

- **Backend**: Use JUnit 5 and Mockito. Target business logic (Services) for unit tests. Ensure proper isolation using `@Mock` and `@InjectMocks`.
- **Frontend**: Use Vitest for unit testing composables, Pinia stores, and complex utility functions.

## 9. Library Patterns

You must recognize and utilize the following established patterns in the project:
- **Redis**: Used for 4 main scenarios: Game sessions (TTL), Caching (`@Cacheable`), Rate limiting, and Streak tracking. Do not introduce alternative caching mechanisms.
- **Cloudinary**: Used for avatar image uploads via `CloudinaryService`.
- **Spring Mail**: Used for daily streak reminders and OTP emails.
- **WebFlux (WebClient)**: Used specifically in `AiVocabService` to communicate with the OpenRouter AI API.
- **Jsoup**: Used for sanitizing legacy HTML content during data seeding.
- **DOMPurify & Marked**: Used in the frontend to securely render markdown content.
