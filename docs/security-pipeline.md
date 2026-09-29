# Security pipeline policy

The failed run 36569551837 reported 28 HIGH/CRITICAL dependency findings:
23 in the deliberately vulnerable XStream 1.4.5 teaching dependency and five
findings affecting the two Jackson generations and Tomcat.

## Remediation

Maven overrides update Jackson 2's BOM to 2.21.6, Jackson 3's BOM to 3.1.6,
and Tomcat to 11.0.25, the patch lines identified by that scan.
Both Jackson BOMs are necessary because the HTTP layer uses Jackson 3 while
some lessons still use Jackson 2. Reassess overrides when upgrading Spring Boot.

CI runs clean package without skipping unit tests, builds the Docker image with
--pull, and blocks on HIGH/CRITICAL dependency or container findings. These are
unit tests, not the separate Failsafe/browser integration suite.
Scanner execution errors also fail. Reports upload even after a failure.

## Intentional lesson exceptions

XStream is deliberately pinned to 1.4.5 in webgoat/pom.xml.
VulnerableComponentsLesson.java explicitly demonstrates CVE-2013-7285 using
XStream.fromXML. Updating it would change the lesson's exploit behavior.

.trivyignore.yaml lists only the 23 observed CVEs, scoped to the exact Maven
package/version. Exceptions expire on 2026-10-29 and are owned for review by
the repository maintainers. New CVEs, different packages/versions and other
HIGH/CRITICAL findings are not exempt. Expiration is a review deadline,
not an instruction to renew automatically.

This is risk acceptance for a local training lab, not a vulnerability fix or
a claim that XStream is safe. Keep Compose bindings on 127.0.0.1 and do not
deploy this image publicly. All suppressed findings remain available in
Trivy JSON under ExperimentalModifiedFindings through --show-suppressed.
The exceptions must be removed for a hardened non-teaching deployment.

## Other checks and limitations

Gitleaks is blocking and retains the repository's existing fixture allowlist
in .gitleaks.toml. That allowlist covers broad lesson paths and should be
narrowed separately; this change does not establish that every path is
checked for secrets.

Semgrep's previous action found 17 blocking findings that were masked by
continue-on-error. The replacement is explicitly an advisory inventory with
a JSON artifact and warning/summary, using a pinned CLI version. Scanner
errors fail; findings remain advisory until reviewed and remediated or
individually baselined. A green workflow does not mean SAST findings are fixed.
The registry rule pack p/java is fetched at runtime.

## Validation and operation

In Actions, open CI/CD Security Pipeline, then inspect each job and download
security-and-test-reports and semgrep-report. Check both actionable and
suppressed findings. The workflow can also be started with workflow_dispatch.
All third-party actions in this workflow are pinned to commit SHAs.
Trivy CLI is pinned to v0.74.0; vulnerability databases stay current.
