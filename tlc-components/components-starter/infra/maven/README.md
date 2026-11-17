# TLC Platform — Master Version Update Script

> `tlc-maven-versions.sh` — One script to update Maven versions across **all TLC Platform repositories**.

---

## Table of Contents

- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [Setup — Change BASE\_DIR to Your Own Path](#setup--change-base_dir-to-your-own-path)
- [Project Structure](#project-structure)
- [Quick Start](#quick-start)
- [Available Commands](#available-commands)
- [Version Variables Reference](#version-variables-reference)
- [How It Works — The 4-Step Pattern](#how-it-works--the-4-step-pattern)
- [All Modules at a Glance](#all-modules-at-a-glance)
- [Adding a New Submodule to an Existing Service](#adding-a-new-submodule-to-an-existing-service)
- [Adding a Completely New Top-Level Service](#adding-a-completely-new-top-level-service)
- [Adding a New Component to tlc-components](#adding-a-new-component-to-tlc-components)
- [Production Release (Remove -SNAPSHOT)](#production-release-remove--snapshot)
- [Troubleshooting](#troubleshooting)

---

## Overview

This script manages Maven artifact versions for all TLC Platform modules consistently. Every module follows the **same 4-step pattern**:

1. Update the **parent** pom version
2. Update `<parent><version>` reference in every **child** pom
3. Set each **submodule's own** independent version
4. Update **version properties** in the parent pom

This applies to all services: **CRM Server**, **IAM**, **Email Service**, **Social Service**, **File Storage**, and **Components**.

---

## Prerequisites

| Tool | Version |
|------|---------|
| `bash` | 4+ |
| `mvn` (Maven) | 3.6+ with `versions-maven-plugin 2.17.1` |
| `python3` | 3.6+ (used internally for XML parsing) |

---

## Setup — Change BASE_DIR to Your Own Path

> ⚠️ **First thing to do before running the script.** The `BASE_DIR` must point to the folder on **your machine** where all TLC repositories are checked out side-by-side.

Open `tlc-maven-version.sh` and find line 40:

```bash
BASE_DIR="/home/thalaimalai_pandiyan/Desktop/release"
```

Change it to match your local checkout path. Examples:

| Developer | Example BASE_DIR |
|-----------|-----------------|
| Linux/Mac | `/home/yourname/projects/tlc` |
| Mac (home) | `/Users/yourname/work/tlc-platform` |
| Windows (WSL) | `/mnt/c/Users/yourname/tlc` |

**Example:**
```bash
# Change this line in the script
BASE_DIR="/home/yourname/projects/tlc"
```

After changing, make sure your directory looks like this:

```
<BASE_DIR>/
├── tlc-maven-version.sh    ← this script
├── tlc-crm-server/
├── tlc-iam/
├── tlc-email-service/
├── tlc-social/
├── tlc-storage-server/
└── tlc-components/
```

---

## Project Structure

```
BASE_DIR/
│
├── tlc-crm-server/                   ← CRM Server (parent: crm-server)
│   ├── pom.xml                       ← parent pom  → CRM_SERVER_VERSION
│   ├── common/pom.xml                → CRM_COMMON_VERSION
│   ├── core/pom.xml                  → CRM_CORE_VERSION
│   ├── entity/pom.xml                → CRM_ENTITY_VERSION
│   ├── notifications/pom.xml         → CRM_NOTIFICATIONS_VERSION
│   ├── workflow/pom.xml              → CRM_WORKFLOW_VERSION
│   ├── webform/pom.xml               → CRM_WEBFORM_VERSION
│   ├── icon/pom.xml                  → CRM_ICON_VERSION
│   ├── fields/pom.xml                → CRM_FIELDS_VERSION
│   ├── security/pom.xml              → CRM_SECURITY_VERSION
│   ├── app-notification/pom.xml      → CRM_APP_NOTIFICATION_VERSION
│   ├── analytics/pom.xml             → CRM_ANALYTICS_VERSION
│   ├── email-notification/pom.xml    → CRM_EMAIL_NOTIFICATION_VERSION
│   ├── product-package/pom.xml       → CRM_PRODUCT_PACKAGE_VERSION
│   ├── zapier/pom.xml                → CRM_ZAPIER_VERSION
│   ├── email-core/pom.xml            → CRM_EMAIL_CORE_VERSION
│   ├── messenger/pom.xml             → CRM_MESSENGER_VERSION
│   ├── plan-and-billing/pom.xml      → CRM_PLAN_BILLING_VERSION
│   ├── conversation-handler/pom.xml  → CRM_CONVERSATION_HANDLER_VERSION
│   ├── crm-marketplace/pom.xml       → CRM_MARKETPLACE_VERSION
│   ├── auto-assignment/pom.xml       → CRM_AUTO_ASSIGNMENT_VERSION
│   ├── ai/pom.xml                    → CRM_AI_VERSION
│   └── server-starter/pom.xml        → CRM_SERVER_STARTER_VERSION (Spring Boot)
│
├── tlc-iam/                          ← IAM (parent: iam-core)
│   ├── pom.xml                       → IAM_CORE_VERSION
│   ├── client/pom.xml                → IAM_CLIENT_VERSION
│   ├── server/pom.xml                → IAM_SERVER_VERSION
│   ├── common/pom.xml                → IAM_COMMON_VERSION
│   ├── features/pom.xml              (inherits parent version)
│   └── iam-starter/pom.xml           → IAM_STARTER_VERSION (Spring Boot)
│
├── tlc-email-service/                ← Email Service (parent: email-service)
│   ├── pom.xml                       → EMAIL_SERVICE_VERSION
│   ├── email-common/pom.xml          → EMAIL_COMMON_VERSION
│   ├── email-server/pom.xml          → EMAIL_SERVER_VERSION
│   ├── email-client/pom.xml          → EMAIL_CLIENT_VERSION
│   └── email-starter/pom.xml         → EMAIL_STARTER_VERSION (Spring Boot)
│
├── tlc-social/                       ← Social Service (parent: social-service)
│   ├── pom.xml                       → SOCIAL_SERVICE_VERSION
│   ├── social-client/pom.xml         → SOCIAL_CLIENT_VERSION
│   ├── social-server/pom.xml         → SOCIAL_SERVER_VERSION
│   ├── social-common/pom.xml         → SOCIAL_COMMON_VERSION
│   └── social-starter/pom.xml        → SOCIAL_STARTER_VERSION (Spring Boot)
│
├── tlc-storage-server/               ← File Storage (parent: file-storage-server)
│   ├── pom.xml                       → FILE_STORAGE_VERSION
│   ├── client/pom.xml                → FILE_CLIENT_VERSION
│   ├── server/pom.xml                → FILE_SERVER_VERSION
│   ├── common/pom.xml                → FILE_COMMON_VERSION
│   └── file-starter/pom.xml          → FILE_STARTER_VERSION (Spring Boot)
│
└── tlc-components/                   ← Components (parent: components)
    ├── pom.xml                       → COMPONENTS_VERSION
    ├── commons/pom.xml               → TLC_COMMONS_VERSION
    ├── validator/pom.xml             → TLC_VALIDATOR_VERSION
    ├── cache/pom.xml                 → TLC_CACHE_VERSION
    ├── i18n/pom.xml                  → TLC_I18N_VERSION
    ├── web/pom.xml                   → TLC_WEB_VERSION
    ├── webclient/pom.xml             → TLC_WEB_CLIENT_VERSION
    ├── queue/pom.xml                 → TLC_QUEUE_VERSION
    ├── notification/pom.xml          → TLC_NOTIFICATION_VERSION
    ├── attachment/pom.xml            → TLC_ATTACHMENT_VERSION
    ├── scheduler/pom.xml             → TLC_SCHEDULER_VERSION
    ├── cluster/pom.xml               → TLC_CLUSTER_VERSION
    ├── search/pom.xml                → TLC_SEARCH_VERSION
    ├── exchange-rate/pom.xml         → TLC_EXCHANGE_RATE_VERSION
    ├── l10n/pom.xml                  → TLC_L10N_VERSION
    ├── entity/pom.xml                → TLC_ENTITY_VERSION
    ├── sync-service-oauth/pom.xml    → TLC_SYNC_SERVICE_OAUTH_VERSION
    ├── otp-processor/pom.xml         → TLC_OTP_PROCESSOR_VERSION
    ├── plans-and-billings/pom.xml    → TLC_PLANS_AND_BILLINGS_VERSION
    ├── payment-gateway/pom.xml       → TLC_PAYMENT_GATEWAY_VERSION
    ├── marketplace/pom.xml           → TLC_MARKETPLACE_VERSION
    ├── identity-provider/pom.xml     → TLC_IDENTITY_PROVIDER_VERSION
    ├── cdc-core/pom.xml              → TLC_CDC_CORE_VERSION
    ├── features/pom.xml              → TLC_FEATURES_VERSION
    ├── tools/pom.xml                 → TLC_TOOLS_VERSION
    ├── observability/pom.xml         → TLC_OBSERVABILITY_VERSION
    ├── sql/pom.xml                   → TLC_SQL_VERSION (sql sub-parent)
    │   ├── sql-common/pom.xml        → TLC_SQL_COMMON_VERSION
    │   ├── oltp/pom.xml              → TLC_SQL_OLTP_VERSION
    │   └── olap/pom.xml              → TLC_SQL_OLAP_VERSION
    └── components-starter/pom.xml    → COMPONENTS_STARTER_VERSION (Spring Boot)
```

---

## Quick Start

```bash
# 1. Set executable permission (only once)
chmod +x tlc-maven-version.sh

# 2. Edit versions in the script (see "Version Variables Reference" below)
nano tlc-maven-version.sh   # or open in your IDE

# 3. Run for specific module
./tlc-maven-version.sh crm
./tlc-maven-version.sh iam
./tlc-maven-version.sh social

# 4. Or update everything at once
./tlc-maven-version.sh all

# 5. Check what the current versions are
./tlc-maven-version.sh display
```

---

## Available Commands

| Command | What it does |
|---------|-------------|
| `display` | Show all current versions read from pom.xml files |
| `all` | Update **all** projects (components → crm → iam → email → social → file → all starters) |
| `crm` | Update CRM Server parent + all submodules + server-starter |
| `iam` | Update IAM parent + all submodules + iam-starter |
| `email` | Update Email Service parent + all submodules + email-starter |
| `social` | Update Social Service parent + all submodules + social-starter |
| `file` | Update File Storage parent + all submodules + file-starter |
| `components` | Update Components parent + all components (incl. sql/*) + components-starter |
| `starters` | Update **only** all starter modules (no library modules) |
| `release` | Strip `-SNAPSHOT` from all pom.xml files for production release |
| `help` | Show usage help |

**Examples:**
```bash
./tlc-maven-version.sh display
./tlc-maven-version.sh all
./tlc-maven-version.sh crm
./tlc-maven-version.sh release
```

---

## Version Variables Reference

All versions are defined at the **top of the script** (lines 42–136). Edit these before running.

### TLC-CRM-SERVER
```bash
CRM_SERVER_VERSION="9.31.0-SNAPSHOT"           # tlc-crm-server/pom.xml (parent)
CRM_COMMON_VERSION="6.23.0-SNAPSHOT"           # tlc-crm-server/common/
CRM_CORE_VERSION="7.40.0-SNAPSHOT"             # tlc-crm-server/core/
CRM_ENTITY_VERSION="6.25.0-SNAPSHOT"           # tlc-crm-server/entity/
CRM_NOTIFICATIONS_VERSION="6.16.0-SNAPSHOT"    # tlc-crm-server/notifications/
CRM_WORKFLOW_VERSION="2.25.0-SNAPSHOT"         # tlc-crm-server/workflow/
CRM_WEBFORM_VERSION="5.14.0-SNAPSHOT"          # tlc-crm-server/webform/
CRM_ICON_VERSION="2.11.0-SNAPSHOT"             # tlc-crm-server/icon/
CRM_FIELDS_VERSION="1.17.0-SNAPSHOT"           # tlc-crm-server/fields/
CRM_SECURITY_VERSION="1.27.0-SNAPSHOT"         # tlc-crm-server/security/
CRM_APP_NOTIFICATION_VERSION="1.7.0-SNAPSHOT"  # tlc-crm-server/app-notification/
CRM_ANALYTICS_VERSION="2.10.0-SNAPSHOT"        # tlc-crm-server/analytics/
CRM_EMAIL_NOTIFICATION_VERSION="1.12.0-SNAPSHOT"
CRM_PRODUCT_PACKAGE_VERSION="2.3.0-SNAPSHOT"
CRM_ZAPIER_VERSION="1.10.0-SNAPSHOT"
CRM_EMAIL_CORE_VERSION="1.9.0-SNAPSHOT"
CRM_MESSENGER_VERSION="1.12.0-SNAPSHOT"
CRM_MARKETPLACE_VERSION="1.2.0-SNAPSHOT"
CRM_PLAN_BILLING_VERSION="1.8.0-SNAPSHOT"
CRM_AI_VERSION="1.1.0-SNAPSHOT"
CRM_CONVERSATION_HANDLER_VERSION="1.4.0-SNAPSHOT"
CRM_AUTO_ASSIGNMENT_VERSION="1.4.0-SNAPSHOT"
CRM_SERVER_STARTER_VERSION="2.31.0-SNAPSHOT"   # tlc-crm-server/server-starter/ (Spring Boot)
```

### TLC-IAM
```bash
IAM_CORE_VERSION="2.27.0-SNAPSHOT"     # tlc-iam/pom.xml (parent)
IAM_CLIENT_VERSION="2.17.0-SNAPSHOT"   # tlc-iam/client/
IAM_SERVER_VERSION="2.21.0-SNAPSHOT"   # tlc-iam/server/
IAM_COMMON_VERSION="2.20.0-SNAPSHOT"   # tlc-iam/common/
IAM_STARTER_VERSION="1.14.0-SNAPSHOT"  # tlc-iam/iam-starter/ (Spring Boot)
```

### TLC-EMAIL-SERVICE
```bash
EMAIL_SERVICE_VERSION="1.15.0-SNAPSHOT"  # tlc-email-service/pom.xml (parent)
EMAIL_COMMON_VERSION="1.10.0-SNAPSHOT"   # tlc-email-service/email-common/
EMAIL_SERVER_VERSION="1.12.0-SNAPSHOT"   # tlc-email-service/email-server/
EMAIL_CLIENT_VERSION="1.8.0-SNAPSHOT"    # tlc-email-service/email-client/
EMAIL_STARTER_VERSION="1.11.0-SNAPSHOT"  # tlc-email-service/email-starter/ (Spring Boot)
```

### TLC-SOCIAL
```bash
SOCIAL_SERVICE_VERSION="1.15.0-SNAPSHOT"  # tlc-social/pom.xml (parent)
SOCIAL_CLIENT_VERSION="1.7.0-SNAPSHOT"    # tlc-social/social-client/
SOCIAL_SERVER_VERSION="1.11.0-SNAPSHOT"   # tlc-social/social-server/
SOCIAL_COMMON_VERSION="1.11.0-SNAPSHOT"   # tlc-social/social-common/
SOCIAL_STARTER_VERSION="1.9.0-SNAPSHOT"   # tlc-social/social-starter/ (Spring Boot)
```

### TLC-STORAGE-SERVER
```bash
FILE_STORAGE_VERSION="1.12.0-SNAPSHOT"  # tlc-storage-server/pom.xml (parent)
FILE_CLIENT_VERSION="1.6.0-SNAPSHOT"    # tlc-storage-server/client/
FILE_SERVER_VERSION="1.3.0-SNAPSHOT"    # tlc-storage-server/server/
FILE_COMMON_VERSION="1.6.0-SNAPSHOT"    # tlc-storage-server/common/
FILE_STARTER_VERSION="1.6.0-SNAPSHOT"   # tlc-storage-server/file-starter/ (Spring Boot)
```

### TLC-COMPONENTS
```bash
COMPONENTS_VERSION="6.45.0-SNAPSHOT"          # tlc-components/pom.xml (parent)
TLC_COMMONS_VERSION="5.34.0-SNAPSHOT"
TLC_VALIDATOR_VERSION="5.8.0-SNAPSHOT"
TLC_CACHE_VERSION="5.6.0-SNAPSHOT"
TLC_I18N_VERSION="5.3.0-SNAPSHOT"
TLC_WEB_VERSION="5.17.0-SNAPSHOT"
TLC_WEB_CLIENT_VERSION="5.4.0-SNAPSHOT"
TLC_SQL_VERSION="5.24.0-SNAPSHOT"             # tlc-components/sql/ (sql sub-parent)
TLC_SQL_COMMON_VERSION="5.24.0-SNAPSHOT"
TLC_SQL_OLTP_VERSION="1.1.0-SNAPSHOT"
TLC_SQL_OLAP_VERSION="1.1.0-SNAPSHOT"
TLC_QUEUE_VERSION="5.9.0-SNAPSHOT"
TLC_NOTIFICATION_VERSION="5.24.0-SNAPSHOT"
TLC_ATTACHMENT_VERSION="5.11.0-SNAPSHOT"
TLC_SCHEDULER_VERSION="5.4.0-SNAPSHOT"
TLC_CLUSTER_VERSION="5.2.0-SNAPSHOT"
TLC_SEARCH_VERSION="5.12.0-SNAPSHOT"
TLC_EXCHANGE_RATE_VERSION="2.2.0-SNAPSHOT"
TLC_L10N_VERSION="2.14.0-SNAPSHOT"
TLC_ENTITY_VERSION="1.4.0-SNAPSHOT"
TLC_SYNC_SERVICE_OAUTH_VERSION="1.10.0-SNAPSHOT"
TLC_OTP_PROCESSOR_VERSION="1.1.0-SNAPSHOT"
TLC_PLANS_AND_BILLINGS_VERSION="1.5.0-SNAPSHOT"
TLC_PAYMENT_GATEWAY_VERSION="1.4.0-SNAPSHOT"
TLC_MARKETPLACE_VERSION="1.1.0-SNAPSHOT"
TLC_IDENTITY_PROVIDER_VERSION="1.2.0-SNAPSHOT"
TLC_CDC_CORE_VERSION="1.1.0-SNAPSHOT"
TLC_FEATURES_VERSION="6.1.0-SNAPSHOT"
TLC_TOOLS_VERSION="5.6.0-SNAPSHOT"
TLC_OBSERVABILITY_VERSION="1.1.0-SNAPSHOT"
COMPONENTS_STARTER_VERSION="2.16.0-SNAPSHOT"  # tlc-components/components-starter/ (Spring Boot)
```

---

## How It Works — The 4-Step Pattern

Every module (`crm`, `iam`, `email`, `social`, `file`, `components`) follows **exactly the same 4 steps**:

```
Step 1: Update PARENT pom.xml version
         └─> e.g. tlc-crm-server/pom.xml → 9.31.0-SNAPSHOT

Step 2: Update <parent><version> inside each CHILD pom.xml
         └─> e.g. tlc-crm-server/common/pom.xml → <parent><version>9.31.0-SNAPSHOT</version>
         └─> tlc-crm-server/core/pom.xml        → <parent><version>9.31.0-SNAPSHOT</version>
         └─> ... all submodules

Step 3: Set each SUBMODULE's OWN version
         └─> e.g. tlc-crm-server/common/pom.xml   → <version>6.23.0-SNAPSHOT</version>
         └─> e.g. tlc-crm-server/core/pom.xml     → <version>7.40.0-SNAPSHOT</version>
         └─> ... each gets its own independent version

Step 4: Update VERSION PROPERTIES in parent pom.xml
         └─> e.g. <tlc.crm.common.version>6.23.0-SNAPSHOT</tlc.crm.common.version>
         └─> <tlc.crm.core.version>7.40.0-SNAPSHOT</tlc.crm.core.version>
         └─> ... so dependencyManagement blocks resolve correctly
```

---

## All Modules at a Glance

| Module | Parent pom artifactId | Submodules | Starter |
|--------|----------------------|------------|---------|
| `tlc-crm-server` | `crm-server` | common, core, entity, notifications, workflow, webform, icon, fields, security, app-notification, analytics, email-notification, product-package, zapier, email-core, messenger, plan-and-billing, conversation-handler, crm-marketplace, auto-assignment, ai | `server-starter` |
| `tlc-iam` | `iam-core` | client, server, common, features | `iam-starter` |
| `tlc-email-service` | `email-service` | email-common, email-server, email-client | `email-starter` |
| `tlc-social` | `social-service` | social-client, social-server, social-common | `social-starter` |
| `tlc-storage-server` | `file-storage-server` | client, server, common | `file-starter` |
| `tlc-components` | `components` | commons, validator, cache, i18n, web, webclient, queue, notification, attachment, scheduler, cluster, search, exchange-rate, l10n, entity, sync-service-oauth, otp-processor, plans-and-billings, payment-gateway, marketplace, identity-provider, cdc-core, features, tools, observability, sql (+ sql-common, oltp, olap) | `components-starter` |

---

## Adding a New Submodule to an Existing Service

> Example: Adding a new submodule `tlc-crm-server/reporting` to CRM Server.

### Step 1 — Add version variable (top of script)

```bash
# In the CRM_SERVER section (~line 46)
CRM_REPORTING_VERSION="1.0.0-SNAPSHOT"    # tlc-crm-server/reporting/
```

### Step 2 — Add to the Step 2 loop in `update_crm_server()`

Find the `for module in ...` loop in `update_crm_server()` and add `reporting`:

```bash
for module in common core entity notifications workflow webform icon fields security \
              app-notification analytics email-notification product-package zapier \
              email-core messenger plan-and-billing conversation-handler crm-marketplace \
              auto-assignment ai features reporting; do   # ← add here
    pom="$BASE_DIR/tlc-crm-server/$module/pom.xml"
    [ -f "$pom" ] && update_parent_ref "$pom" "crm-server" "$CRM_SERVER_VERSION"
done
```

### Step 3 — Add to the Step 3 version-set block

```bash
cd "$BASE_DIR/tlc-crm-server/reporting" && mvn_set_safe -DnewVersion="$CRM_REPORTING_VERSION" -DgenerateBackupPoms=false
```

### Step 4 — Add to the Step 4 properties block

```bash
mvn versions:set-property -Dproperty=tlc.crm.reporting.version -DnewVersion="$CRM_REPORTING_VERSION" -DgenerateBackupPoms=false
```

### Step 5 — Add to `update_crm_server_starter()` if the starter uses it

```bash
mvn versions:set-property -Dproperty=tlc.crm.reporting.version -DnewVersion="$CRM_REPORTING_VERSION" -DgenerateBackupPoms=false
```

### Step 6 — Add to `display_all_versions()` (optional but recommended)

```bash
grep -E "^\s*<version>" "$BASE_DIR/tlc-crm-server/reporting/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  reporting: \1/'
```

### Step 7 — Update the CRM parent pom.xml

In `tlc-crm-server/pom.xml`, add the version property and dependency:

```xml
<!-- In <properties> -->
<tlc.crm.reporting.version>1.0.0-SNAPSHOT</tlc.crm.reporting.version>

<!-- In <dependencyManagement> -->
<dependency>
    <groupId>com.tlc.crm</groupId>
    <artifactId>reporting</artifactId>
    <version>${tlc.crm.reporting.version}</version>
</dependency>

<!-- In <modules> -->
<module>reporting</module>
```

> The same pattern applies to adding submodules to **IAM**, **Email**, **Social**, or **File Storage** — just find the matching function.

---

## Adding a Completely New Top-Level Service

> Example: Adding a brand-new service `tlc-push-service` with submodules `push-client`, `push-server`, `push-common`, and `push-starter`.

### Step 1 — Add version variables (top of script)

```bash
# TLC-PUSH-SERVICE
# Parent + each submodule has its OWN version
PUSH_SERVICE_VERSION="1.0.0-SNAPSHOT"   # tlc-push-service/pom.xml (parent)
PUSH_CLIENT_VERSION="1.0.0-SNAPSHOT"    # tlc-push-service/push-client/pom.xml
PUSH_SERVER_VERSION="1.0.0-SNAPSHOT"    # tlc-push-service/push-server/pom.xml
PUSH_COMMON_VERSION="1.0.0-SNAPSHOT"    # tlc-push-service/push-common/pom.xml
PUSH_STARTER_VERSION="1.0.0-SNAPSHOT"   # tlc-push-service/push-starter/pom.xml (Spring Boot)
```

### Step 2 — Add two new functions following the 4-step pattern

```bash
#########################################################
#  TLC-PUSH-SERVICE
#  Parent has PUSH_SERVICE_VERSION
#  Each submodule has its OWN version
#########################################################
update_push_service() {
    print_header "UPDATING TLC-PUSH-SERVICE"

    # Step 1: Update parent version
    echo "Step 1: Setting push-service parent version to $PUSH_SERVICE_VERSION..."
    cd "$BASE_DIR/tlc-push-service"
    mvn_set_safe -DnewVersion="$PUSH_SERVICE_VERSION" -DgenerateBackupPoms=false

    # Step 2: Update <parent><version> in each child pom
    echo "Step 2: Updating parent references in child poms..."
    update_parent_ref "$BASE_DIR/tlc-push-service/push-client/pom.xml" "push-service" "$PUSH_SERVICE_VERSION"
    update_parent_ref "$BASE_DIR/tlc-push-service/push-server/pom.xml" "push-service" "$PUSH_SERVICE_VERSION"
    update_parent_ref "$BASE_DIR/tlc-push-service/push-common/pom.xml" "push-service" "$PUSH_SERVICE_VERSION"

    # Step 3: Set each submodule's OWN version
    echo "Step 3: Setting individual submodule versions..."
    cd "$BASE_DIR/tlc-push-service/push-client" && mvn_set_safe -DnewVersion="$PUSH_CLIENT_VERSION" -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-push-service/push-server" && mvn_set_safe -DnewVersion="$PUSH_SERVER_VERSION" -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-push-service/push-common" && mvn_set_safe -DnewVersion="$PUSH_COMMON_VERSION" -DgenerateBackupPoms=false

    # Step 4: Update version properties in parent pom
    echo "Step 4: Updating version properties in push-service parent pom..."
    cd "$BASE_DIR/tlc-push-service"
    mvn versions:set-property -Dproperty=tlc.push.client.version  -DnewVersion="$PUSH_CLIENT_VERSION"  -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.push.server.version  -DnewVersion="$PUSH_SERVER_VERSION"  -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.push.common.version  -DnewVersion="$PUSH_COMMON_VERSION"  -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.commons.version      -DnewVersion="$TLC_COMMONS_VERSION"  -DgenerateBackupPoms=false
    # ... add any other tlc.* component properties used in this service's pom

    echo "Push Service updated successfully!"
}

update_push_starter() {
    print_header "UPDATING PUSH STARTER"
    cd "$BASE_DIR/tlc-push-service/push-starter"

    mvn_set_safe -DnewVersion="$PUSH_STARTER_VERSION" -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.components.starter.version -DnewVersion="$COMPONENTS_STARTER_VERSION" -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.push.client.version        -DnewVersion="$PUSH_CLIENT_VERSION"        -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.push.server.version        -DnewVersion="$PUSH_SERVER_VERSION"        -DgenerateBackupPoms=false

    echo "Push Starter updated successfully!"
}
```

### Step 3 — Add to `update_all_starters()`

```bash
update_all_starters() {
    print_header "UPDATING ALL STARTERS"
    update_components_starter
    update_crm_server_starter
    update_iam_starter
    update_email_starter
    update_social_starter
    update_file_starter
    update_push_starter    # ← add here
    print_header "ALL STARTERS UPDATED"
}
```

### Step 4 — Add to `update_all()`

```bash
update_all() {
    print_header "UPDATING ALL TLC PROJECTS"
    update_components
    update_crm_server
    update_iam
    update_email_service
    update_social_service
    update_file_storage
    update_push_service    # ← add here
    update_all_starters
    print_header "ALL PROJECTS UPDATED SUCCESSFULLY!"
}
```

### Step 5 — Add a new command in the `case` block

```bash
case "${1:-help}" in
    # ... existing cases ...
    push)       update_push_service && update_push_starter ;;   # ← add here
    # ...
esac
```

### Step 6 — Add to `display_all_versions()`

```bash
echo ""
echo "--- TLC-PUSH-SERVICE ---"
grep -E "^\s*<version>" "$BASE_DIR/tlc-push-service/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  Parent (push-service): \1/'
grep -E "^\s*<version>" "$BASE_DIR/tlc-push-service/push-client/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  push-client: \1/'
grep -E "^\s*<version>" "$BASE_DIR/tlc-push-service/push-server/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  push-server: \1/'
grep -E "^\s*<version>" "$BASE_DIR/tlc-push-service/push-common/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  push-common: \1/'
grep -E "^\s*<version>" "$BASE_DIR/tlc-push-service/push-starter/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  push-starter (Spring Boot): \1/'
```

### Step 7 — Add to `remove_all_snapshots()`

```bash
for project in tlc-crm-server tlc-iam tlc-email-service tlc-social tlc-storage-server tlc-components tlc-push-service; do
```

### Step 8 — Update the help text

```bash
echo "  push        - Push Service + starter"
```

---

## Adding a New Component to tlc-components

> Example: Adding a new component `tlc-components/audit`.

### Step 1 — Add version variable

```bash
TLC_AUDIT_VERSION="1.0.0-SNAPSHOT"    # tlc-components/audit/pom.xml
```

### Step 2 — Add to the loop in `update_components()` (Step 2)

```bash
for module in commons validator cache i18n web webclient queue notification \
              attachment scheduler cluster search exchange-rate l10n entity \
              sync-service-oauth otp-processor plans-and-billings payment-gateway \
              marketplace identity-provider cdc-core features tools observability sql \
              audit; do    # ← add here
```

### Step 3 — Add to Step 3 version-set block

```bash
cd "$BASE_DIR/tlc-components/audit" && mvn_set_safe -DnewVersion="$TLC_AUDIT_VERSION" -DgenerateBackupPoms=false
```

### Step 4 — Add to Step 4 properties block

```bash
mvn versions:set-property -Dproperty=tlc.audit.version -DnewVersion="$TLC_AUDIT_VERSION" -DgenerateBackupPoms=false
```

### Step 5 — Add to `update_components_starter()` if the starter uses it

```bash
mvn versions:set-property -Dproperty=tlc.audit.version -DnewVersion="$TLC_AUDIT_VERSION" -DgenerateBackupPoms=false
```

### Step 6 — Update `tlc-components/pom.xml`

```xml
<!-- In <properties> -->
<tlc.audit.version>1.0.0-SNAPSHOT</tlc.audit.version>

<!-- In <dependencyManagement> -->
<dependency>
    <groupId>com.tlc</groupId>
    <artifactId>audit</artifactId>
    <version>${tlc.audit.version}</version>
</dependency>

<!-- In <modules> -->
<module>audit</module>
```

---

## Production Release (Remove -SNAPSHOT)

When ready to cut a production release, run:

```bash
./tlc-maven-version.sh release
```

This strips `-SNAPSHOT` from **all** pom.xml files across all projects using `sed`. After running:

1. Review the changes with `git diff`
2. Commit and tag
3. To go back to development, manually bump versions and add `-SNAPSHOT` again

---

## Troubleshooting

### `mvn: command not found`
Maven is not installed or not on your PATH.
```bash
# Check
mvn --version
# Install on Ubuntu/Debian
sudo apt-get install maven
```

### `python3: command not found`
Python 3 is required for XML parsing inside the script.
```bash
# Install on Ubuntu/Debian
sudo apt-get install python3
```

### `No such file or directory` errors
Your `BASE_DIR` is wrong or a module folder doesn't exist at the expected path. Run `./tlc-maven-version.sh display` first — if it shows errors, fix `BASE_DIR`.

### Version not updating / updating to wrong version
- Make sure you edited the version variable **at the top of the script**, not inside a function.
- Run `./tlc-maven-version.sh display` before and after to confirm what changed.

### `set -e` causes script to exit early
The script uses `set -e` — any failed Maven command will abort the script. Check the Maven output above the failure for the root cause (usually a missing `pom.xml` or network issue).

### Backup poms left behind (`.pom.xml.versionsBackup`)
The script always passes `-DgenerateBackupPoms=false` to avoid this. If you see backup files, they were created by a manual `mvn versions:set` call. Delete them:
```bash
find . -name "*.versionsBackup" -delete
```

---

## Summary Checklist When Adding a New Module

| Checklist Item | Location in Script |
|---|---|
| ☐ Add version variables | Top of script, `EDIT VERSIONS HERE` section |
| ☐ Add `update_<module>()` function (4 steps) | After the last module function |
| ☐ Add `update_<module>_starter()` function | After `update_<module>()` |
| ☐ Add starter call to `update_all_starters()` | `update_all_starters()` function |
| ☐ Add module call to `update_all()` | `update_all()` function |
| ☐ Add `case` entry in main switch | Bottom `case` block |
| ☐ Add display block to `display_all_versions()` | `display_all_versions()` function |
| ☐ Add project to `remove_all_snapshots()` loop | `remove_all_snapshots()` function |
| ☐ Add help text entry | `help` case in main switch |
| ☐ Update parent `pom.xml` (`<modules>`, `<properties>`, `<dependencyManagement>`) | The actual Maven pom.xml file |

