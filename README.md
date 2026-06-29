# ChessAIssistant - TFG

![Java](https://img.shields.io/badge/Java-21%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Gemini API](https://img.shields.io/badge/Google_Gemini-API-8E75B2?style=for-the-badge&logo=google-bard&logoColor=white)
![Lichess](https://img.shields.io/badge/Lichess-Data-white?style=for-the-badge&logo=lichess&logoColor=black)

<p align="center">
  <img src="frontend/src/assets/logo.png" alt="ChessAIssistant logo" width="180" />
</p>

ChessAIssistant is a full-stack chess tactics trainer built as a Final Degree Project. It combines real tactical puzzles with AI-generated guidance so users can train calculation, understand mistakes, and request conceptual hints without immediately revealing the full solution.

## Features

- Real chess tactics based on Lichess puzzle data.
- AI tutor responses for contextual explanations and hint generation.
- User authentication through Firebase.
- Personalized puzzle progression with rating and attempt tracking.
- Responsive React interface with an interactive chessboard.
- Spring Boot REST API for puzzle, user, and training-session workflows.

## Tech Stack

| Layer | Technology | Purpose |
| :-- | :-- | :-- |
| Backend | Java 21, Spring Boot 3.2 | REST API, business logic, security, persistence |
| Frontend | React, TypeScript, Vite | User interface and chessboard experience |
| Authentication | Firebase | User sign-in and identity verification |
| Data | PostgreSQL, Lichess puzzle data | Application storage and tactical puzzle source |
| AI | Google Gemini / local Ollama profile | Tutor explanations and hint generation |
| Deployment | Docker Compose, Caddy | Local or server deployment with reverse proxy |

## Project Structure

```text
.
|-- backend/          Spring Boot API
|-- frontend/         React and TypeScript client
|-- reverse-proxy/    Caddy reverse proxy configuration
`-- docker-compose.yml
```

## Local Development

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend is served by Vite, usually at `http://localhost:5173`.

### Full Stack With Docker

Create a root `.env` file with the required Firebase, database, CORS, and AI provider settings, then run:

```bash
docker compose up --build
```

The Compose stack starts PostgreSQL, the backend, the frontend, and the Caddy reverse proxy. The optional Ollama services are available through the `ollama` profile.

## Quality Checks

Backend tests:

```bash
cd backend
./mvnw test
```

Frontend checks:

```bash
cd frontend
npm run lint
npm run build
```

## Academic Context

- Title: Development of an Intelligent Chess Trainer Based on LLMs.
- Student: Juan Lopez Alvarez
- Advisor: Pablo Gonzalez Gonzalez
- University: University of Oviedo
- Year: 2026
