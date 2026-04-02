# Antigravity Agents Configuration

## Project Context

This project is a full-stack web application utilizing a modern, scalable architecture. The AI agents operating within the Antigravity framework (using Pro-tier models) must adhere strictly to the rules, tech stack, and coding conventions defined in this document.

## Tech Stack

- **Backend:** Java 21, Spring Boot (Version compatible with Java 21, e.g., 3.2+)
- **Frontend:** React, TypeScript
- **Database:** Firebase (Firestore / Realtime Database)
- **Language:** All code, comments, variables, commit messages, and documentation MUST be in **English**.

---

## Global Coding Rules & Conventions

### General Rules for All Agents

1.  **Language Strictness:** English only. No exceptions.
2.  **Proactive Problem Solving:** Anticipate edge cases. Write robust error-handling code.
3.  **Clean Code:** Follow SOLID principles. Keep functions small, testable, and focused on a single responsibility.
4.  **Security First:** Never hardcode sensitive credentials (especially Firebase keys). Always use environment variables (`.env`).

### Backend Rules (Java 21 & Spring)

1.  **Modern Java Features:** Actively utilize Java 21 features such as Records, Pattern Matching for `switch`, Virtual Threads (if applicable to the Spring version), and enhanced Collections.
2.  **Spring Conventions:** \* Follow standard layered architecture: `Controller` -> `Service` -> `Repository`.
    - Use constructor injection (`@RequiredArgsConstructor` with Lombok or standard constructors) instead of `@Autowired` on fields.
    - Keep controllers lightweight; business logic belongs in the Service layer.
3.  **Firebase Integration:** Use the official Firebase Admin SDK for Java. Handle async operations and Firebase exceptions gracefully.

### Frontend Rules (React & TypeScript)

1.  **TypeScript Strictness:** Enable `strict` mode in `tsconfig.json`. Avoid using `any` type at all costs; define proper interfaces or types for all props, state, and API responses.
2.  **React Conventions:**
    - Use Functional Components and Hooks exclusively (no Class components).
    - Keep components modular and reusable.
    - Extract custom hooks for complex logic or API calls.
3.  **State Management:** Keep local state where possible. Use Context API or a modern state manager (like Zustand or Redux Toolkit) only when global state is strictly necessary.

---

## Agent Roles

### 1. `@BackendAgent`

- **Responsibility:** Develop RESTful APIs, manage business logic, and integrate Firebase.
- **Prompting Guide:** When invoked, always verify the Java 21 syntax. Ensure Firebase data models map correctly to Java Records/Classes. Create proper DTOs (Data Transfer Objects) for request/response payloads to avoid exposing internal entities.

### 2. `@FrontendAgent`

- **Responsibility:** Build the user interface, manage client-side routing, state, and consume backend APIs.
- **Prompting Guide:** When invoked, prioritize component reusability and responsive design. Ensure TypeScript interfaces strictly match the backend DTOs.

### 3. `@DevOpsAgent`

- **Responsibility:** Manage environment configuration, build scripts (Maven/Gradle for backend, Vite/Webpack for frontend), and Firebase deployment rules.
- **Prompting Guide:** Ensure `.gitignore` is properly configured for both ecosystems (Node.js and Java) and that Firebase security rules are strict and well-documented.
