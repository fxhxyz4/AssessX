<div align="center">
  <h1>🎓 AssessX</h1>
  <p><strong>Student assessment system — tests, code practice assignments and automatic verification</strong></p>

  ![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=java)
  ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.4-brightgreen?style=flat-square&logo=springboot)
  ![JavaFX](https://img.shields.io/badge/JavaFX-Desktop-blue?style=flat-square)
  ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?style=flat-square&logo=postgresql)
  ![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker)
  ![GitHub](https://img.shields.io/github/license/M-it-2/AssessX)
  ![Backend](https://img.shields.io/website?url=https%3A%2F%2Fassessx-backend.onrender.com&label=backend&style=flat-square)
  ![GitHub issues](https://img.shields.io/github/issues/M-it-2/AssessX)

</div>

---

## 📋 Table of Contents

- [About](#-about)
- [Features](#-features)
- [Screenshots](#-screenshots)
- [Tech Stack](#-tech-stack)
- [Quick Start](#-quick-start)
- [API](#-api)

---

## 🧠 About

**AssessX** is a desktop student assessment system with role-based access for **Teacher** / **Student**.

Teachers create tests and code practice assignments, assign them to student groups with deadlines. Students complete assignments in real time: take timed tests or write code that is automatically verified by unit tests in an isolated environment.

---

## ✨ Features

### 👨‍🏫 Teacher
- Create tests (multiple-choice questions)
- Create code practice assignments + unit tests
- Assign tasks to specific groups with deadlines
- View student results by group
- Delete tests, practices and assignments (cascading cleanup)

### 👨‍🎓 Student
- View assigned tasks with deadlines
- Take tests with a countdown timer
- Write code in an editor with syntax highlighting
- Automatic verification via unit tests
- View personal results and attempt history

### 🔒 Authentication
- OAuth2 via GitHub
- JWT tokens
- Role-based access control (TEACHER / STUDENT)

---

## 📸 Screenshots

### Assignments Page (student)
![Assignments Page](assets/screens/assignments.png)

### Taking a Test
![Take Test](assets/screens/take_test.png)

### Code Practice Assignment
![Code Practice](assets/screens/code_practice.png)

### Results Page
![Results Page](assets/screens/results.png)

### Teacher Panel — Tests
![Teacher Tests](assets/screens/teacher_tests.png)

### Teacher Panel — Practice
![Teacher Practice](assets/screens/teacher_practice.png)

---

**Data flow:**

```
JavaFX Client → HTTP (JWT) → Spring Boot API → PostgreSQL
                                    ↓
                          Docker (code-runner) ← unit tests
```

---

## 🛠 Tech Stack

| Component | Technology |
|-----------|-----------|
| Backend | Spring Boot 4.0.4, Spring Security 7, Spring Data JPA |
| ORM | Hibernate 7.2.7 |
| Database | PostgreSQL 15 |
| Auth | OAuth2 GitHub, JWT (Nimbus JOSE) |
| Frontend | JavaFX (desktop), NextJS (web) |
| JSON | Jackson 3.x (tools.jackson) |
| Code Execution | Docker container with isolated JVM |
| Build | Maven, Docker Compose |
| Tests | JUnit 6, Mockito 5 |

---

## 🚀 Quick Start

### Requirements

- Docker & Docker Compose
- Java 21+
- Maven 3.9+

### Run with Docker Compose

```bash
git clone https://github.com/fxhxyz4/AssessX-backend.git
cd AssessX-backend

cp .env.example .env

docker compose -f docker-compose.dev.yml up --build
```

Backend will start at `http://localhost:8080`.

### Run JavaFX Client

```bash
cd AssessX
mvn javafx:run
```

### Backend Environment Variables

| Variable | Description |
|----------|-------------|
| `GITHUB_CLIENT_ID` | GitHub OAuth App Client ID |
| `GITHUB_CLIENT_SECRET` | GitHub OAuth App Client Secret |
| `JWT_SECRET` | Secret key for signing JWT tokens |
| `PORT` | Backend port |
| `DB_NAME` | Database name |
| `DB_PORT` | Database port |
| `DB_URL` | JDBC URL to PostgreSQL |
| `DB_USER` | Database user |
| `DB_PASS` | Database password |

---

### Frontend Environment Variables

| Variable | Description |
|----------|-------------|
| `API_PORT` | Backend port |
| `API_URL` | Backend URL |

---

## 🔌 API

### Auth
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/oauth2/authorization/github` | Redirect to GitHub OAuth |
| GET | `/api/auth/me` | Get current user |

### Tests
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/tests` | List all tests |
| POST | `/api/tests` | Create a test |
| GET | `/api/tests/{id}` | Get test by ID |
| PUT | `/api/tests/{id}` | Update test |
| DELETE | `/api/tests/{id}` | Delete test |
| POST | `/api/tests/{id}/submit` | Submit test answers |

### Practice
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/practices` | List all practices |
| POST | `/api/practices` | Create a practice |
| DELETE | `/api/practices/{id}` | Delete practice |
| POST | `/api/practices/{id}/submit` | Submit code solution |

### Assignments
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/assignments` | All assignments |
| GET | `/api/assignments/my` | Student's assignments |
| POST | `/api/assignments` | Create assignment |
| DELETE | `/api/assignments/{id}` | Delete assignment |

### Results
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/results/my` | My results |
| GET | `/api/results/group/{id}` | Group results |

---

<div align="center">
  <sub>License: MPL-2.0</sub>
</div>
