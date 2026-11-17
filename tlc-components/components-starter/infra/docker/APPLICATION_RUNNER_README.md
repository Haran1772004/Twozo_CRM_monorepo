# Application Runner v2.0

> Interactive build & deploy script for the **TWOZO CRM** platform.

`application_runner.sh` automates the full lifecycle of Maven-building, Docker-image-building, and deploying the TWOZO CRM microservice stack via Docker Compose to your local environment. It supports both fully automated (CLI flags) and interactive (prompted) modes, and optional AI-assisted build error recovery.

---

## What's New in v2.0

| Feature | Impact |
|---------|--------|
| **Selective container restart** | Only stops services being rebuilt — infrastructure (postgres, redis, kafka, …) stays alive, saving **2–5 minutes** per deployment |
| **Docker BuildKit** (`DOCKER_BUILDKIT=1`) | Parallel layer builds + inline caching, **~20–40% faster** image builds |
| **Parallel Maven builds** (`-T 1C`) | 1 thread per CPU core by default, **~30–50% faster** Maven compilation |
| **Confirmation prompt** | Review all settings and confirm before any changes begin |
| **`--yes` / `-y` flag** | Skip confirmation for CI/CD pipelines |
| **`--stop-for-build`** | Opt-in: stop selected service containers to free memory during Maven |
| **`--parallel`** | Control parallel Maven builds (`-T MVN_THREADS`); default on |
| **Error classification** | Build failures categorized (OOM, MAVEN_COMPILE, DOCKER_BUILD, NETWORK, AUTH, DISK) with fix hints — even without AI |
| **Enhanced AI responses** | Claude now returns `user_message` and `manual_steps` for clearer developer guidance |

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Architecture Overview](#architecture-overview)
- [Modules](#modules)
- [CLI Reference](#cli-reference)
- [Interactive Mode](#interactive-mode)
- [Build Modes](#build-modes)
- [Clean Modes](#clean-modes)
- [Deployment Targets](#deployment-targets)
- [CRM Submodule Selection](#crm-submodule-selection)
- [Skip Components Build](#skip-components-build)
- [Stop for Build](#stop-for-build)
- [Parallel Maven Builds](#parallel-maven-builds)
- [Docker BuildKit](#docker-buildkit)
- [Ngrok Tunnel](#ngrok-tunnel)
- [AI-Assisted Error Handling](#ai-assisted-error-handling)
- [Execution Pipeline](#execution-pipeline)
- [Examples](#examples)
- [Project Structure](#project-structure)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

| Tool               | Minimum Version | Purpose |
|--------------------|-----------------|---------|
| **Bash**           | 4.0+            | Associative arrays, `set -euo` |
| **Docker**         | 20.10+          | Container runtime + BuildKit support |
| **Docker Compose** | v2 (plugin)     | Multi-container orchestration |
| **Maven**          | 3.8+            | Java project builds |
| **Java / JDK**     | 17+             | Compiling backend services |
| **curl**           | any             | Ngrok tunnel URL detection |
| **python3** *(optional)* | 3.6+      | JSON parsing (AI fix + ngrok) |
| **jq** *(optional)*      | any       | Required only for `--ai-fix yes` |

---

## Quick Start

```bash
# Make the script executable (one-time)
chmod +x application_runner.sh

# Full interactive mode (all options prompted, with confirmation)
./application_runner.sh

# Full build and deploy everything (non-interactive)
./application_runner.sh --modules all --build-mode full --clean none --yes

# Fastest iteration: rebuild one changed service only
./application_runner.sh -m crm-server -b full --clean none --skip-components yes --yes

# Redeploy only – no build, no data loss
./application_runner.sh --build-mode skip --clean none --yes
```

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        Docker Compose                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐ │
│  │ crm-client  │  │ iam-server  │  │     crm-server          │ │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘ │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐ │
│  │ file-server  │  │email-server │  │       social            │ │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘ │
│  Infrastructure (stays alive during partial deploys):           │
│  PostgreSQL · Redis · Kafka · Zookeeper · OpenSearch            │
│  Hazelcast · Azurite · ClickHouse · PgAdmin · Ngrok             │
└─────────────────────────────────────────────────────────────────┘
```

**Maven Build Order:**
```
tlc-parent → tlc-components → components-starter → service libraries → starter fat-jars
```

---

## Modules

| Module         | Maven Source             | Docker Image                  | Description |
|----------------|--------------------------|-------------------------------|-------------|
| `crm-client`   | *(static build)*         | `accountId/crm-client:latest`  | React frontend |
| `iam-server`   | `tlc-iam/`               | `accountId/iam-server:latest`  | Identity & access |
| `file-server`  | `tlc-storage-server/`    | `accountId/file-server:latest` | File storage |
| `email-server` | `tlc-email-service/`     | `accountId/email-server:latest`| Email service |
| `social`       | `tlc-social/`            | `accountId/social:latest`      | Social integrations |
| `crm-server`   | `tlc-crm-server/`        | `accountId/crm-server:latest`  | Core CRM backend |

Aliases: `crm` → `crm-server`, `iam` → `iam-server`, `email` → `email-server`, `file` → `file-server`, `client` → `crm-client`.

---

## CLI Reference

### Flags

| Flag                | Short | Values | Default | Description |
|---------------------|-------|--------|---------|-------------|
| `--build-mode`      | `-b`  | `full`, `docker-only`, `skip` | *(prompted)* | Build depth |
| `--modules`         | `-m`  | `all` or comma-separated | *(prompted)* | Modules to process |
| `--clean`           |       | `none`, `all` | *(prompted)* | Volume management |
| `--tunnel`          |       | *(flag)* | `false` | Enable ngrok tunnel |
| `--crm-sub`         |       | `all` or comma-separated | *(prompted)* | CRM submodules |
| `--skip-components` |       | `yes`, `no` | *(prompted)* | Skip shared lib build |
| `--stop-for-build`  |       | `yes`, `no` | `no` | Stop services during build |
| `--parallel`        |       | `yes`, `no` | `yes` | Parallel Maven (`-T MVN_THREADS`) |
| `--yes`             | `-y`  | *(flag)* | `false` | Skip confirmation prompt |
| `--ai-fix`          |       | `yes`, `no` | `no` | AI-assisted error fixing |
| `--ai-max-retries`  |       | integer | `3` | Max AI retry attempts |
| `--help`            | `-h`  |  | | Show usage |

### Environment Variables

| Variable           | Default                    | Description |
|--------------------|----------------------------|-------------|
| `DOCKER_BIN`       | `docker`                   | Docker binary |
| `MVN_BIN`          | `mvn`                      | Maven binary |
| `MVN_OPTS`         | `-DskipTests`              | Extra Maven flags |
| `MVN_THREADS`      | `1C`                       | Parallel threads (`1C` = 1 per core) |
| `BUILDKIT_ENABLED` | `1`                        | Docker BuildKit (`1`=on, `0`=off) |
| `ANTHROPIC_API_KEY`|                            | Claude API key (for `--ai-fix yes`) |
| `AI_MODEL`         | `claude-sonnet-4-20250514` | Claude model |

---

## Interactive Mode

All required flags are prompted with numbered menus when omitted. A **confirmation summary** is shown before any changes:

```
[INFO]  Server(s)       : local
[INFO]  Modules         : crm-server
[INFO]  Build mode      : full
[INFO]  Parallel Maven  : yes (-T 1C)
[INFO]  Docker BuildKit : 1
...

  Proceed with deployment? [Y/n]:
```

Use `--yes` / `-y` to skip the confirmation in CI/CD or scripted environments.

---

## Build Modes

### `full`
1. Optionally stops selected services (`--stop-for-build yes`)
2. Pre-build `docker system prune`
3. Installs `tlc-parent` POM (serial)
4. Builds `tlc-components` + `components-starter` (parallel, unless `--skip-components yes`)
5. Builds service library modules (parallel)
6. Packages starter fat-jars (parallel)
7. Builds Docker images (BuildKit + inline cache)

### `docker-only`
Skips Maven. Reuses existing JARs. Rebuilds Docker images and redeploys.

### `skip`
No Maven, no Docker build. Redeploys containers from existing images.

---

## Clean Modes

### `none` *(default)*
- DB and app data volumes survive.
- Kafka/Zookeeper gracefully stopped before redeploy (prevents ZooKeeper `NodeExists` errors).
- **Only selected services** are stopped and recreated — infrastructure stays running.

### `all`
- Full `docker compose down --volumes`.
- All data (PostgreSQL, OpenSearch, Redis, ClickHouse, app files) is wiped.

---

## Deployment Targets

### Local
Default and only target. Uses the local Docker Engine. No extra setup needed.

---

## CRM Submodule Selection

Build only the submodules that changed:
```bash
./application_runner.sh -m crm-server -b full --clean none --crm-sub core,workflow
```
Submodules are discovered dynamically from `tlc-crm-server/pom.xml` — no script changes needed when new modules are added.

---

## Skip Components Build

```bash
./application_runner.sh -m crm-server -b full --clean none --skip-components yes
```
Safe when only service-level code changed and `tlc-components` / `components-starter` are already up to date.

---

## Stop for Build

```bash
./application_runner.sh --modules all -b full --clean none --stop-for-build yes
```
Stops **only selected service containers** before Maven build to free memory. Infrastructure always stays running. Recommended on machines with ≤ 8 GB RAM.

---

## Parallel Maven Builds

Enabled by default (`--parallel yes`, `-T 1C`). Override thread count:
```bash
MVN_THREADS=4 ./application_runner.sh -m crm-server -b full --clean none
```
Disable for debugging:
```bash
./application_runner.sh -m crm-server -b full --clean none --parallel no
```

---

## Docker BuildKit

Enabled by default (`DOCKER_BUILDKIT=1`). Provides parallel layer execution and inline caching. Disable if needed:
```bash
BUILDKIT_ENABLED=0 ./application_runner.sh ...
```

---

## Ngrok Tunnel

```bash
./application_runner.sh --build-mode skip --clean none --tunnel
```
Set `NGROK_DOMAIN` in `config/env/app.env` for a static URL; otherwise a random URL is assigned per restart.

---

## AI-Assisted Error Handling

### Error Classification (always on)
Every build failure is classified automatically:

| Category           | Trigger | Fix hint |
|--------------------|---------|----------|
| `OOM`              | OutOfMemoryError | `--stop-for-build yes`, increase `-Xmx` |
| `MAVEN_COMPILE`    | cannot find symbol | Check imports, rebuild tlc-components |
| `MAVEN_DEPENDENCY` | Could not resolve | Check network / Maven Central |
| `DOCKER_BUILD`     | COPY failed | Run `--build-mode full` |
| `NETWORK`          | Connection refused | Check network / Docker Hub |
| `DISK`             | No space left | `docker system prune -af` |
| `PERMISSION`       | permission denied | `sudo usermod -aG docker $USER` |

### AI Auto-Fix
```bash
export ANTHROPIC_API_KEY=sk-ant-...
./application_runner.sh -m crm-server -b full --clean none --ai-fix yes
```
Claude analyzes the error, applies file patches, runs commands, and retries the build automatically. AI logs are saved to `.ai-fix-logs/`.

---

## Execution Pipeline

```
Step 0 ─ Docker Prune (dangling images; volumes if --clean all)
Step 1 ─ Selective Stop (only if --stop-for-build yes, full build only)
Step 2 ─ Maven Builds (full only)
│   ├── Pre-build system prune
│   ├── tlc-parent (serial)
│   ├── tlc-components + components-starter (parallel)
│   ├── Service library modules (parallel, per module)
│   └── Starter fat-jars (parallel)
Step 3 ─ Docker Image Builds (full + docker-only)
│   BuildKit enabled; BUILDKIT_INLINE_CACHE=1
Step 4 ─ Deploy (local)
│   ├── --clean all → compose down --volumes
│   └── --clean none → Kafka/ZK graceful stop
│                    + stop only selected services
│   └── compose up --force-recreate [selected services only]
Step 5 ─ Ngrok URL display (if --tunnel)
```

---

## Examples

```bash
# Full build and deploy everything (local, interactive)
./application_runner.sh

# Full build, non-interactive
./application_runner.sh --modules all -b full --clean none --yes

# Rebuild one service fast (skip shared libs, targeted submodules)
./application_runner.sh -m crm-server -b full --clean none \
  --skip-components yes --crm-sub core,workflow --yes

# Rebuild Docker image only (JARs already built)
./application_runner.sh -m crm-server -b docker-only --clean none --yes

# Restart containers only
./application_runner.sh --build-mode skip --clean none --yes


# Full build with AI error fixing
export ANTHROPIC_API_KEY=sk-ant-...
./application_runner.sh --modules all -b full --clean none --ai-fix yes --ai-max-retries 5

# Memory-constrained machine (stop services during Maven build)
./application_runner.sh --modules all -b full --clean none --stop-for-build yes --yes

# Deploy with tunnel
./application_runner.sh --build-mode skip --clean none --tunnel --yes

# Custom binaries and thread count
DOCKER_BIN="sudo docker" MVN_THREADS=8 \
  ./application_runner.sh --modules all -b full --clean none --yes
```

---

## Project Structure

```
infra/docker/
├── application_runner.sh          ← Build & deploy script (v2.0)
├── APPLICATION_RUNNER_README.md   ← This file
├── docker-compose.yml             ← Compose stack definition
├── hazelcast.yml                  ← Hazelcast cluster configuration
├── nginx.conf                     ← Nginx reverse-proxy configuration
└── config/
    ├── env/
    │   └── app.env                ← Application environment variables
    ├── init-azurite-containers.sh ← Azurite blob storage initialization
    └── pgadmin/                   ← PgAdmin server configuration
```

---

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| `docker compose is not available` | v2 plugin missing | Install `docker-compose-plugin` or set `DOCKER_BIN` |
| `Maven project directory not found` | Source missing | Ensure all repos are cloned under workspace root |
| ZooKeeper `NodeExists` errors | Leftover ephemeral nodes | Script handles it automatically; use `--clean all` if it persists |
| `Could not discover CRM submodules` | `pom.xml` missing | Ensure `tlc-crm-server` is cloned |
| OOM during Maven build | Too little free memory | Use `--stop-for-build yes` or increase Docker memory limit |
| Ngrok URL not displayed | Container still starting | Check `http://localhost:4040` |
| `Unknown module: <name>` | Typo | Use a valid name or alias (see Modules table) |
| `docker load` fails | Out of disk space | Free up disk space or use `docker system prune` |
| BuildKit warnings | Older Docker | Set `BUILDKIT_ENABLED=0` |
| AI fix returns invalid JSON | API error | Check `ANTHROPIC_API_KEY`; see `.ai-fix-logs/` |

---

## Performance Reference

| Optimization | Typical Savings |
|-------------|----------------|
| Selective restart (vs stop-all) | **2–5 min** per deployment |
| Docker BuildKit | **20–40%** faster image builds |
| Parallel Maven (`-T 1C`) | **30–50%** faster on multi-core |
| `--skip-components` | **3–8 min** when shared libs unchanged |
| CRM submodule selection | **Proportional** to modules skipped |
