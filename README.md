# Briefix — Backend

REST API backend for [Brief-Fix](https://www.brief-fix.de), a German letter creation web app.

**Live Demo:** [www.brief-fix.de](https://www.brief-fix.de) Generates professionally formatted DIN 5008 letters as PDFs compatible with DIN-A4 window envelopes.

## Tech Stack

- **Java 21** / **Spring Boot 3.4.3**
- **PostgreSQL** — persistent storage (users, letters, contacts, profiles)
- **Redis** — rate limiting, token blacklisting
- **JWT** — stateless authentication (access + refresh tokens)
- **Google OAuth** — sign-in with Google
- **Thymeleaf + OpenHTMLtoPDF** — HTML-to-PDF letter rendering
- **Google Gemini API** (`gemini-2.5-flash`) — AI letter generation
- **Docker** → deployed on **Google Cloud Run**

## Features

- Email/password and Google OAuth registration & login
- Email verification flow
- JWT access (15 min) + refresh (7 days) token rotation
- Sender profile management (individual & organization)
- Recipient contact management
- Letter creation with multiple template styles (`classic`, `professional`)
- AI-assisted letter body generation via Gemini
- Public letter preview endpoint (rate-limited)
- DIN 5008-compliant PDF output

## Project Structure

```
src/main/java/com/briefix/
├── ai/          # Gemini integration (controller + service)
├── auth/        # Registration, login, token refresh, Google OAuth
├── common/      # Shared utilities, base classes
├── contact/     # Recipient contact CRUD
├── letter/      # Letter CRUD + PDF rendering
├── profile/     # Sender profile CRUD
├── security/    # JWT filter, security config, rate limiting
└── user/        # User entity + service

src/main/resources/
├── application.yaml
└── templates/letters/
    ├── classic.html       # Classic letter PDF template
    └── professional.html  # Professional letter PDF template
```

## Getting Started

### Prerequisites

- Java 21
- Maven 3.9+
- PostgreSQL
- Redis

### Environment Variables

| Variable | Description |
|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | DB password |
| `SPRING_REDIS_HOST` | Redis host |
| `SPRING_REDIS_PORT` | Redis port |
| `JWT_SECRET` | JWT signing secret |
| `GEMINI_API_KEY` | Google Gemini API key |
| `GOOGLE_CLIENT_ID` | Google OAuth client ID |
| `FRONTEND_URL` | CORS allowed origin |
| `MAIL_USERNAME` | SMTP username |
| `MAIL_PASSWORD` | SMTP password |
| `MAIL_FROM` | Sender email address |

### Run Locally

```bash
# Start PostgreSQL and Redis (example with Docker)
docker compose up -d db redis

# Run the application
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`.

### Run with Docker

```bash
docker build -t briefix-backend .
docker run -p 8080:8080 --env-file .env briefix-backend
```

## API Overview

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Register with email/password |
| `POST` | `/api/v1/auth/login` | Login, returns JWT tokens |
| `POST` | `/api/v1/auth/google` | Google OAuth sign-in |
| `POST` | `/api/v1/auth/refresh` | Refresh access token |
| `GET` | `/api/v1/letters` | List user's letters |
| `POST` | `/api/v1/letters` | Create a letter |
| `GET` | `/api/v1/letters/{id}/pdf` | Download letter as PDF |
| `GET` | `/api/v1/contacts` | List contacts |
| `GET` | `/api/v1/profiles` | List sender profiles |
| `POST` | `/api/v1/ai/generate-letter` | AI letter generation |
| `POST` | `/api/v1/public/letters/preview` | Public PDF preview (rate-limited) |
