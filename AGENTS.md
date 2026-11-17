# AGENTS Guide - TLC Monorepo

## What this repo is
- Multi-repo style platform workspace for Thelaunchclub CRM services, centered on Java 17 + Maven modules.
- Core backend domains live in `tlc-crm-server/`; shared building blocks live in `tlc-components/`.
- Identity, file storage, email, and social capabilities are split into sibling services (`tlc-iam/`, `tlc-storage-server/`, `tlc-email-service/`, `tlc-social/`).
- UI is split: `crm-client/` (React static app) and `tlc-crm-ui-server/` (Java UI server).

## Architecture map (start here)
- `tlc-parent/pom.xml`: parent conventions (Java level, plugin management, release behavior).
- `tlc-components/pom.xml`: reusable cross-service modules (cache, queue, scheduler, search, sql, notification, validator, etc.).
- `tlc-crm-server/pom.xml`: CRM domain modules (workflow, fields, messenger, notifications, security, analytics, zapier, webform, AI).
- Service pattern repeated in multiple repos: `client/`, `server/`, `common/`, `*-starter/` for API contracts + runtime separation.
- Typical flow: UI (`crm-client` or UI server) -> CRM server -> service client modules (IAM/File/Email/Social) -> external integrations.

## Build and CI workflows used in this codebase
- Standard local build from a service root: `mvn clean install -e`.
- Quality-gate build used in pipelines (develop/pre-prod branches):
  `mvn clean verify sonar:sonar -Dsonar.token=... -Dsonar.projectName=... -Dsonar.projectKey=... -Dsonar.qualitygate.wait=true`
- Jenkins pipelines are service-local (`*/Jenkinsfile`) and read service settings from `*/ci-cd/jenkins-config.yml`.
- Toolchain assumptions seen in config/docs: Maven 3, Java 17, SonarQube, and internal package feeds (Azure Artifacts).

## Project conventions that matter for edits
- Keep module boundaries strict: depend on `*-client` artifacts for cross-service calls; do not couple to another service's `server` module.
- Respect parent-managed versions and plugin config; prefer changing parent/dependencyManagement over ad hoc child overrides.
- Group IDs encode domain ownership (`com.tlc.*`, `com.tlc.crm.*`, `com.tlc.iam.*`, etc.); keep new modules consistent.
- Versioning and releases are Maven-driven; release plugin/tag conventions are defined in parent POMs.
- Many modules are OSGi/Karaf aware (bundle/features setup in parent/component build config); avoid breaking manifest/bundle assumptions.

## Integration points to check before changing behavior
- Authentication/authorization boundaries: `tlc-iam/` + CRM security modules.
- File and attachment flows: `tlc-storage-server/` + component modules like attachment.
- Messaging/notifications/email: CRM notification modules + `tlc-email-service/`.
- Third-party connectors are explicit modules (for example `tlc-crm-server/zapier/`, payment/exchange modules under `tlc-components/`).
- Secrets/config often assume cloud/enterprise infra (for example Azure/Sonar/Jenkins wiring in POMs and CI files).

## Fast navigation hints for agents
- Read these first for orientation: `tlc-parent/README.md`, `tlc-components/README.md`, `tlc-crm-server/README.md`.
- Then inspect each touched service's `pom.xml` + `Jenkinsfile` before code edits.
- For frontend-impacting changes, check both `crm-client/` and backend API modules in `tlc-crm-server/`.
- Prefer small, module-local changes; if a contract changes, update corresponding `client` module and dependent services together.

