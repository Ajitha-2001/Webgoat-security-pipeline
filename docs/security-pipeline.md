# Security pipeline policy

This document describes `.github/workflows/pipeline.yml` reviewed on 2026-09-30
at application commit `046d8e26540f3405c17bc96049f8dce7afc3c42f`.
Historical runs may use different workflow versions.

## Current execution and gates

The workflow runs on pushes to all branches and pull requests targeting main.
There is no workflow_dispatch trigger. One build-and-scan job performs:

1. Checkout with full Git history and configure Temurin Java 25/Maven caching.
2. Run `./mvnw clean test`.
3. Run `./mvnw package -DskipTests`; unit tests already ran in step 2.
4. Run Gitleaks, using GitHub's job token.
5. Run Trivy filesystem scanning and Semgrep.
6. Build the image with `docker compose build`.
7. Run Trivy against `webgoat-app:ci`.

| Security scan | Scope / threshold | Blocking on main? |
| --- | --- | --- |
| Gitleaks | Default rules extended by .gitleaks.toml | Yes |
| Trivy dependencies | ./webgoat; HIGH,CRITICAL; exit-code 1 | No: continue-on-error true |
| Semgrep | p/java | No: continue-on-error true |
| Trivy container | webgoat-app:ci; HIGH,CRITICAL; exit-code 1 | No: continue-on-error true |

Build, unit-test, packaging, Docker-build and Gitleaks failures stop the job.
Advisory steps can tolerate findings AND scanner execution errors, so inspect
their logs even when the workflow is green. Later ordinary steps are skipped
after a blocking failure.

This workflow does not run the separate Failsafe/browser integration suite,
DAST, a runtime smoke test, or a deployment. The Docker build does not specify
--pull. Repository rules must separately require a status check to block merges;
a failing job alone does not prove merge protection is configured.

## Assignment evidence

Section 2.4 requires all four scan types and at least one genuinely blocking
gate, with evidence of a failure. Gitleaks is configured to block on main.
The separate demo branch temporarily makes Semgrep blocking:

| Evidence | Result and interpretation |
| --- | --- |
| [Main run 36686070447](https://github.com/Ajitha-2001/Webgoat-security-pipeline/actions/runs/36686070447) | Passed; 306 tests reported, 0 failures/errors, 1 skipped (305 passed); Docker build succeeded. |
| [SAST demo run 36615706718](https://github.com/Ajitha-2001/Webgoat-security-pipeline/actions/runs/36615706718) | Failed at Semgrep on demo/security-gate-test; Docker build/container scan skipped. Demonstrates blocking in that demo configuration. |
| [Gitleaks demo run 36614528122](https://github.com/Ajitha-2001/Webgoat-security-pipeline/actions/runs/36614528122) | Passed. Do not present this run as proof of Gitleaks blocking a finding. |

The main run above still reported 23 dependency findings, 25 container findings
(23 Java plus 2 HIGH OS-package findings), and 1 Semgrep finding. Those are
historical counts for that run, not a permanent security baseline. A green build
does not establish that all vulnerabilities are fixed.

For each of the four coursework fixes, separately retain the vulnerable commit,
working exploit screenshot, fix commit, repeat of the same exploit after the
fix, and before/after SAST counts or diff. Pipeline execution is not a substitute
for exploit-and-fix evidence.

## Reports and tool versions

Read the individual step logs in Actions. This workflow defines no explicit
artifact upload steps for Trivy, Semgrep, or Maven test reports, and no guaranteed
report upload after a failure. Gitleaks may upload its own action-managed report;
check the individual run's artifacts. Download available logs and retain
screenshots before GitHub's configured retention expires.

Actions are referenced by tags or branches, not immutable commit SHAs:
checkout@v5, setup-java@v5, gitleaks-action@v3, semgrep-action@v1 and
trivy-action@master. The workflow does not explicitly pin the Trivy CLI version.
Semgrep selects the registry pack p/java only. The committed JWT and XSS custom
rule files are not included automatically by that selection.

## Dependency remediation and intentional lessons

The POM overrides Jackson 2's BOM to 2.21.6, Jackson 3's BOM to 3.1.6 and Tomcat
to 11.0.25. Both Jackson generations are used. These updates addressed findings
from an earlier run; they do not guarantee a clean scan against future databases.

XStream 1.4.5 remains intentionally vulnerable for the vulnerable-components
lesson. The repository's .trivyignore.yaml records 23 package/version-scoped
CVEs with a 2026-10-29 review expiry. The current workflow does not explicitly
pass that YAML file to Trivy and the reviewed run still reports those findings.
Do not describe these exceptions as an active, selective blocking policy.

The advisory setting tolerates all scan failures, not only the documented
XStream CVEs. Distinguish intentional lesson risks from unrelated OS/dependency
findings that need remediation. Keep this teaching application local with
Compose's 127.0.0.1 bindings; it is not a hardened production deployment.

## Secrets provisioning and limitations

### Pipeline

Gitleaks receives `secrets.GITHUB_TOKEN`, the automatic GitHub Actions job token.
The workflow permissions are contents: read and pull-requests: read. No
application JWT secret is injected into CI by the current YAML. Do not claim
that user-created encrypted application secrets or Vault injection have been
demonstrated merely because the workflow uses GITHUB_TOKEN.

.gitleaks.toml extends the default rules but allowlists broad lesson and test
paths. A passing scan does not prove that those excluded paths contain no
hardcoded values. Clearly distinguish upstream teaching fixtures from actual
application credentials in the report; never add real secrets to those paths.

### Local application

Compose forwards JWT_SECRET and JWT_SECRET_KEY from the host environment.
See [README setup](../README.md#4-configure-local-access-and-jwt-secrets)
for random-key generation without writing a committed file.

JWTSecretKeyEndpoint reads JWT_SECRET_KEY. Supplied values must be at least
32 characters; missing/blank values generate a secure random key at startup.
Restarting with a newly generated key invalidates previously signed tokens.
Setting these variables does not harden other JWT lessons: JWTVotesEndpoint,
for example, still has its upstream hardcoded teaching key. The implemented
JWTSecretKeyEndpoint fix concerns weak signing secrets and does not establish
that every alg:none, issuer or authentication issue is fixed across WebGoat.

The example uses terminal environment variables, not a persistent secret store.
Do not print keys, commit them, or include them in evidence screenshots.
Docker access can expose container environment values.

## Improvements not currently implemented

- Explicit, reviewed scan exceptions with blocking on unaccepted findings.
- Loading the custom SAST rules in CI and preserving scan/test artifacts.
- Immutable action/CLI pins and reliable base-image refresh.
- Narrower secrets allowlists and evidence of encrypted application-secret
  provisioning or a runtime secrets manager.
- Integration/browser regression tests and optional DAST.

These are limitations or future work, not claims about current functionality.
