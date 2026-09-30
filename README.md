# WebGoat Security Pipeline

**IE3142 — DevOps Security | SLIIT | 2026**

A group coursework project that builds and secures a DevSecOps pipeline around [OWASP WebGoat](https://github.com/WebGoat/WebGoat), a deliberately vulnerable Java and Spring Boot application.

The project combines containerisation, threat modelling, secure coding, and automated security checks. WebGoat's source is included so vulnerabilities can be investigated, fixed in Java, rebuilt, and retested.

> **Educational use only:** Run this application in an authorised local lab. The Compose configuration below binds the application to `127.0.0.1` so it is accessible only from the host computer. Use dedicated practice credentials.

## Project Scope

| Area | Objective |
| --- | --- |
| Architecture and containerisation | Provide a reproducible source build and local Docker deployment. |
| Threat modelling | Apply STRIDE, assess risks, and map threats to controls. |
| Secure coding | Demonstrate four vulnerabilities, implement fixes, and collect before-and-after evidence. |
| Security automation | Integrate source, dependency, secrets, and container scanning into GitHub Actions. |
| Documentation | Record the design, findings, remediation, validation, and limitations. |

Local source compilation and application startup have been verified during setup. Pipeline implementation and exploit-and-fix evidence are tracked separately; a successful application build does not establish that the application is secure.

## Threat Model Summary

A STRIDE-based threat model was carried out against the WebGoat deployment architecture. Four application-specific threats were identified, each tied to a vulnerability class demonstrated during the exploit-and-fix work. Full justifications, the risk matrix, and the threat-to-control mapping are recorded in the technical report and in `docs/Threat-Modelling.pdf`.

| ID | Threat | STRIDE Category | Likelihood | Impact | Primary Control |
| --- | --- | --- | --- | --- | --- |
| T1 | SQL Injection — unsanitised string-concatenated queries in the SQL Injection lesson | Tampering / Information Disclosure | High | High | Parameterised queries (`PreparedStatement`) in `SqlInjectionLesson8.java`; validate with exploit retests and SAST evidence |
| T2 | Stored Cross-Site Scripting (XSS) — unencoded user input rendered back to other users | Tampering / Information Disclosure | High | Medium–High | HTML escaping in `StoredXssComments.java`; validate in the browser and with separate SAST evidence |
| T3 | JWT forgery using a weak or hardcoded signing secret | Spoofing / Elevation of Privilege | Medium | High | Environment-provided signing secret or secure random fallback in `JWTSecretKeyEndpoint.java`; this fix does not harden every JWT lesson |
| T4 | Insecure Direct Object Reference (IDOR) — missing server-side object-ownership checks | Elevation of Privilege / Information Disclosure | Medium | Medium–High | Server-side ownership check on every object request |

**Risk matrix (Likelihood × Impact):**

| Likelihood \ Impact | Low | Medium | High |
| --- | --- | --- | --- |
| High | — | — | T1 SQLi, T2 XSS |
| Medium | — | T4 IDOR | T3 JWT Bypass |
| Low | — | — | — |

T1 (SQL Injection) and T2 (Stored XSS) carry the highest combined risk due to their high likelihood of exploitation. T3 (JWT Bypass) has a lower likelihood but a high potential impact, since a successful forgery grants full impersonation. T4 (IDOR) sits at medium likelihood and medium-to-high impact.

These threats cover injection, authentication/cryptographic weaknesses, and broken access control. Map each demonstrated exploit to its precise CWE and OWASP category in the report; a scanner result alone is not proof of remediation.

## Repository Layout

| Path | Purpose |
| --- | --- |
| `webgoat/` | Included upstream WebGoat source and project modifications. |
| `webgoat/pom.xml` | Maven build configuration and dependencies. |
| `webgoat/mvnw` | Maven wrapper used inside the Linux build container. |
| `webgoat/Dockerfile` | Runtime image definition; expects a compiled JAR in `target/`. |
| `docker-compose.yml` | Local application configuration and port mappings. |
| `.github/workflows/` | GitHub Actions workflow definitions, as implemented. |
| `docs/` | Supporting diagrams and evidence, including the threat model report. |
| `README.md` | Setup, operation, and contribution guidance. |

## Prerequisites

- [Git](https://git-scm.com/downloads).
- [Docker Desktop](https://www.docker.com/products/docker-desktop/), running with Linux containers.
- On Windows, a working WSL2 backend and enabled Virtual Machine Platform.
- Internet access for the initial container image and Maven dependency downloads.
- Available disk space for the source, dependencies, build output, and Docker images.

Java and Maven do not need to be installed on the host. The build uses a Java 25 container and the Maven wrapper included in the repository.

Verify that Docker's engine is available:

```shell
docker info
docker compose version
```

`docker info` should display a **Server** section without a connection error.

## Setup and Run

### 1. Clone the repository

```shell
git clone https://github.com/Ajitha-2001/Webgoat-security-pipeline.git
cd Webgoat-security-pipeline
```

Run the following commands from this repository root. The included `webgoat/` directory contains the application source; a separate upstream clone is not required.

### 2. Check script line endings on Windows

The `webgoat/mvnw` script must use **LF** line endings.

In VS Code, open `webgoat/mvnw`, click **CRLF** in the status bar if shown, choose **LF**, and save. Keep the filename `mvnw` without an extension. The Windows wrapper `mvnw.cmd` is a different file.

### 3. Build the application JAR

The runtime Dockerfile copies an existing JAR; it does not compile Java. Complete this step before building the application image.

**Windows PowerShell — including the VS Code PowerShell terminal:**

```powershell
docker run --rm --mount "type=bind,source=$((Get-Location).Path)\webgoat,target=/workspace" --mount "type=volume,source=webgoat-maven-cache,target=/root/.m2" -w /workspace eclipse-temurin:25-jdk-noble bash ./mvnw clean package -DskipTests
```

**macOS / Linux — Bash or Zsh:**

```bash
docker run --rm --mount "type=bind,source=$(pwd)/webgoat,target=/workspace" --mount "type=volume,source=webgoat-maven-cache,target=/root/.m2" -w /workspace eclipse-temurin:25-jdk-noble bash ./mvnw clean package -DskipTests
```

The temporary container is removed when it exits. Downloaded Maven dependencies remain in the `webgoat-maven-cache` Docker volume to speed up subsequent builds. The JAR is written into the host's `webgoat/target/` directory.

Wait for **`BUILD SUCCESS`** before continuing. Initial downloads can take considerable time, depending on the connection.

Verify the output:

```powershell
# Windows PowerShell
Get-ChildItem .\webgoat\target\webgoat-*.jar
```

```bash
# macOS / Linux
ls webgoat/target/webgoat-*.jar
```

> `-DskipTests` is used for this initial setup build. Tests must be run separately when validating changes; this build is not test or security evidence.

### 4. Configure local access and JWT secrets

Use the committed `docker-compose.yml`; it builds `webgoat-app:ci`, binds ports
8080 and 9090 to `127.0.0.1`, and forwards `JWT_SECRET` and `JWT_SECRET_KEY`
from your environment. Do not replace it with an older example that omits these settings.

Set fresh random values in the same terminal before running Compose. These
commands keep the values out of source files and do not print them.

**Windows PowerShell:**

```powershell
$jwtRng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtBytes = New-Object byte[] 48
$jwtRng.GetBytes($jwtBytes)
$env:JWT_SECRET_KEY = [Convert]::ToBase64String($jwtBytes)
$jwtRng.GetBytes($jwtBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
$jwtRng.Dispose()
Remove-Variable jwtBytes, jwtRng
```

**macOS / Linux (requires OpenSSL):**

```bash
export JWT_SECRET_KEY="$(openssl rand -base64 48)"
export JWT_SECRET="$(openssl rand -base64 48)"
```

`JWTSecretKeyEndpoint` specifically reads `JWT_SECRET_KEY` and requires at least
32 characters when supplied. If it is missing or blank, that endpoint generates a
random key on startup; tokens signed with the previous key will no longer verify
after a restart. Compose also forwards `JWT_SECRET`, but setting either variable
does not replace hardcoded values in other, intentionally vulnerable lessons.

These terminal values last only for the current shell session. For a stable lab
key across sessions, keep it in a password manager and provision it privately.
Never commit keys or include them in screenshots, logs, or the report. Environment
variables are configuration injection, not a vault; people with Docker access
can inspect container configuration.

### 5. Build the image and start WebGoat

```shell
docker compose up --build -d
docker compose logs -f webgoat
```

Wait for startup to complete. Press **Ctrl + C** to exit the log view; the detached application remains running.

### 6. Open the application

| Application | Local URL |
| --- | --- |
| WebGoat | [http://localhost:8080/WebGoat/](http://localhost:8080/WebGoat/) |
| WebWolf | [http://localhost:9090/WebWolf/](http://localhost:9090/WebWolf/) |

On the WebGoat login page, select **register yourself as a new user**, create a practice account, and sign in.

## Everyday Commands

Run these commands from the repository root while Docker Desktop is running.

| Action | Command |
| --- | --- |
| Check container status | `docker compose ps` |
| Follow application logs | `docker compose logs -f webgoat` |
| Stop without removing the container | `docker compose stop` |
| Start the existing stopped container | `docker compose start` |
| Remove the application's container and Compose network | `docker compose down` |
| Create and start the application again | `docker compose up -d` |

The example configuration does not define a persistent application data volume. Stopping and starting the same container retains its writable data, but removing or recreating the container can discard accounts and lesson progress. The Maven cache stores build dependencies, not application data.

## Rebuilding After Source Changes

After editing Java source:

1. Run the application JAR build command from step 3 again.
2. Confirm **`BUILD SUCCESS`**.
3. Rebuild and recreate the application container:

   ```shell
   docker compose up --build -d
   ```

4. Check startup logs and repeat the relevant exploit and normal-use tests.

Running `docker compose up --build` alone does not compile changed Java source with the supplied Dockerfile.

For Maven verification, use the same build-container command but replace `clean package -DskipTests` with `clean verify`. Some tests may require additional configuration or services; inspect the results and document any unmet prerequisites. Do not report skipped or failing tests as passed.

## CI/CD and Security Automation

[The workflow](.github/workflows/pipeline.yml) runs on pushes to all branches and
pull requests targeting `main`. It runs `./mvnw clean test`, then
`./mvnw package -DskipTests` (tests have already run), followed by the scans and
Docker image build. It does not run the separate Failsafe/browser integration
suite or deploy the application.

| Check | Current implementation | Effect on workflow |
| --- | --- | --- |
| Build and unit tests | Maven wrapper, Temurin Java 25 | Blocking |
| Secrets | Gitleaks with `.gitleaks.toml` | Blocking |
| Dependencies | Trivy filesystem scan of `./webgoat`, HIGH/CRITICAL | Advisory: `continue-on-error: true` |
| SAST | Semgrep `p/java` | Advisory: `continue-on-error: true` |
| Docker build | `docker compose build` | Blocking |
| Container image | Trivy scan of `webgoat-app:ci`, HIGH/CRITICAL | Advisory: `continue-on-error: true` |

A green workflow does not mean all scans are clean: advisory scan failures,
including tool errors, can be tolerated. Gitleaks is the blocking security gate
on `main`. The separate
[SAST gate demonstration](https://github.com/Ajitha-2001/Webgoat-security-pipeline/actions/runs/36615706718)
on `demo/security-gate-test` failed at Semgrep and skipped later Docker steps.
Keep its screenshot and run link as evidence; do not describe that demo's
blocking SAST configuration as the current configuration on `main`.

The assignment requires all four scan types and at least one genuinely blocking
gate, with a failed-run demonstration. A failed workflow does not by itself
prevent merging unless repository rules require the check; that protection is
not established by this YAML file.

The workflow uses only `p/java`; it does not select
`.semgrep/jwt-hardcoded-secret.yml` or `xss-custom-rule.yml`.
Retain the commands and before/after results for any separate custom-rule scans
used in the report.

Read [the security pipeline policy](docs/security-pipeline.md) for evidence,
secrets handling, scan limitations, and report retention. Results are available
in the [Actions tab](https://github.com/Ajitha-2001/Webgoat-security-pipeline/actions).
The current workflow has no explicit Trivy/Semgrep/test-report artifact upload
steps; retain the relevant logs and screenshots for submission.

## Vulnerability Documentation

Each vulnerability corresponds directly to a threat identified in the Threat Model Summary above (T1–T4). The technical report and supporting evidence record, for each:

- Affected component, source location, and the corresponding threat ID (T1–T4).
- Reproduction steps and observed behaviour in the local lab.
- Root cause and security impact.
- The code change and why it addresses the root cause.
- Before-and-after exploit results and normal-use regression checks.
- Relevant scanner results, screenshots, and commit references.
- Remaining limitations or accepted risks.

Store supporting materials in `docs/` when added, and submit the final technical report separately as required by the assessment brief.

## Troubleshooting

| Symptom | Action |
| --- | --- |
| Docker daemon or named-pipe connection error | Open Docker Desktop, wait for the engine to start, and check `docker info`. |
| Virtualisation or WSL2 startup error | Check hardware virtualisation, Virtual Machine Platform, and `wsl --status`. |
| `mvnw` reports `$'\r': command not found` | Save `webgoat/mvnw` with LF line endings, then rebuild. |
| Dockerfile cannot copy `target/webgoat-*.jar` | Complete the Maven build and verify the JAR exists before building the image. |
| Port 8080 or 9090 is already allocated | Stop the conflicting application or adjust the host-side port mapping and browser URL. |
| Login page does not load | Check `docker compose ps` and `docker compose logs --tail 100 webgoat` for startup failures. |

## Contribution Guidelines

- Use focused branches and commits so each change can be reviewed and reproduced.
- Preserve a clearly identified baseline before applying security fixes.
- Keep generated JARs, `target/` output, local credentials, and runtime data out of Git.
- Preserve upstream copyright and licence notices.
- Document changes to build steps, scanner configuration, or application behaviour.
- Review staged changes before committing, especially when importing upstream source.

## Team

| Student ID | Name | Responsibility |
| --- | --- | --- |
| IT24100092 | Kularathne I M A G | CI/CD Pipeline and Security Automation (2.4, 2.5); Secure Coding: Exploit-and-Fix, Vulnerability 1 — SQL Injection (T1) (2.3). |
| IT24100020 | Gangodawila G P I C | Threat Modelling and Risk Assessment (2.2): STRIDE analysis, risk matrix, and threat-to-control mapping; Secure Coding: Exploit-and-Fix, Vulnerability 2 — Stored XSS (T2) (2.3). |
| IT24100192 | Induwara K A A K | Project coordination; Secure Coding: Exploit-and-Fix, Vulnerability 3 — JWT Bypass (T3) (2.3). |
| IT24101738 | Upeja M.A.T | Architecture and Containerisation (2.1); Secure Coding: Exploit-and-Fix, Vulnerability 4 — IDOR (T4) (2.3). |

## Attribution and Licence

This project includes source from [OWASP WebGoat](https://github.com/WebGoat/WebGoat). Credit for the original application belongs to the WebGoat project and its contributors.

The included WebGoat source is governed by its upstream GNU General Public License terms. Refer to [`webgoat/LICENSE.txt`](webgoat/LICENSE.txt) and [`webgoat/COPYRIGHT.txt`](webgoat/COPYRIGHT.txt) for the authoritative notices. Coursework modifications do not replace those notices. Third-party dependencies retain their respective licences.

## Academic Context

**Module:** IE3142 — DevOps Security
**Institution:** Sri Lanka Institute of Information Technology (SLIIT)
**Programme:** BSc (Hons) Information Technology
**Year / Semester:** Year 3, Semester 1
**Academic year:** 2026

This repository supports the group assessment. The assessment brief and submitted technical report define the final requirements, evidence, and scope.
