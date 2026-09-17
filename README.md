# WebGoat Security Pipeline — IE3142 DevOps Security

Group project for IE3142 (DevOps Security): building and securing a DevSecOps pipeline around **OWASP WebGoat**, a deliberately vulnerable Java/Spring Boot web application.

## Project Structure

.
├── webgoat/ # WebGoat source (Java/Spring Boot)
├── docker-compose.yml # Brings up the full application
├── .github/workflows/ # CI/CD pipeline (GitHub Actions)
└── README.md


## Prerequisites

- [Git](https://git-scm.com/downloads)
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (with WSL2 enabled, on Windows)
- No local Java or Maven installation needed — the build runs inside a temporary Docker container.

## Setup and Run Instructions

**1. Clone this repository:**
```bash
git clone https://github.com/Ajitha-2001/Webgoat-security-pipeline.git
cd Webgoat-security-pipeline
```

**2. Build the WebGoat application jar** (uses a throwaway Maven+JDK container, so nothing extra is installed on your machine):
```bash
docker run --rm -v ${PWD}/webgoat:/app -w /app eclipse-temurin:25-jdk-noble ./mvnw clean package -DskipTests
```
This step takes a while on first run (downloads Maven dependencies). Confirm it worked:
```bash
dir .\webgoat\target\*.jar        # Windows PowerShell
ls webgoat/target/*.jar           # Mac/Linux
```

**3. Build and start the container:**
```bash
docker compose up --build
```

**4. Open WebGoat in your browser:**

http://localhost:8080/WebGoat

WebWolf (WebGoat's companion tool for some lessons) runs on:

http://localhost:9090/WebWolf


**To stop the application:**
```bash
docker compose down
```

## Running the CI/CD Pipeline Locally

The GitHub Actions pipeline (`.github/workflows/`) runs automatically on every push. It includes:
- SAST scanning (Semgrep)
- Dependency/software composition scanning
- Secrets scanning (Gitleaks)
- Container image scanning (Trivy)

See the [Actions tab](../../actions) on GitHub for pipeline run history and results.

## Vulnerability Documentation

Exploit-and-fix demonstrations, before/after SAST results, and screenshots are documented in the technical report (submitted separately as PDF) and in [`docs/`](./docs) (if applicable).

## Team

| Student ID | Name | Role |
|---|---|---|
| IT24100092 | Kularathne I M A G (Ajitha) | Project coordination; Architecture & Containerisation (2.1); CI/CD Pipeline & Security Automation (2.4, 2.5) |
| IT24100020 | Gangodawila G P I C | Threat Modelling & Risk Assessment (2.2) — STRIDE analysis, risk matrix, threat-to-control mapping |
| IT24100192 | Induwara K A A K | Secure Coding: Exploit-and-Fix — Vulnerabilities 1 & 2 (2.3) |
| IT24101738 | Upeja M.A.T | Secure Coding: Exploit-and-Fix — Vulnerabilities 3 & 4 (2.3) |

## Attribution

This project uses [OWASP WebGoat](https://github.com/WebGoat/WebGoat), licensed under GPL-2.0. WebGoat's source is included in this repository under `webgoat/` for the purposes of this coursework assignment (IE3142 DevOps Security, SLIIT). All original WebGoat code and content is credited to the OWASP WebGoat project and contributors.

## Academic Context

This is a group assignment for **IE3142: DevOps Security** (BSc Hons Information Technology, SLIIT), Year 3 Semester 1, 2026. See the technical report for full threat modelling, secure coding evidence, and CI/CD pipeline design documentation.