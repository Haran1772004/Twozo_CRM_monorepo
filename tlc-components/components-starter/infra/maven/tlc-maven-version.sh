#!/bin/bash

#########################################################
# TLC PLATFORM - MASTER VERSION UPDATE SCRIPT
#
# ALL modules now follow the SAME consistent pattern:
#   Step 1: Update parent pom version
#   Step 2: Update <parent><version> in all child poms
#   Step 3: Set each submodule's OWN version
#   Step 4: Update version properties in parent pom
#
# This applies to:
#   - CRM Server: parent + each submodule has its OWN version
#   - IAM: parent + each submodule has its OWN version
#   - Email Service: parent + each submodule has its OWN version
#   - Social Service: parent + each submodule has its OWN version
#   - File Storage: parent + each submodule has its OWN version
#   - Components: parent + each component has its OWN version
#   - Starters: independent Spring Boot apps, own version
#
# Usage:
#   chmod +x tlc-update-all-versions.sh
#   ./tlc-update-all-versions.sh [command]
#
# Commands:
#   display     - Show all current versions
#   all         - Update ALL projects
#   crm         - Update CRM Server only
#   iam         - Update IAM only
#   email       - Update Email Service only
#   social      - Update Social Service only
#   file        - Update File Storage only
#   components  - Update Components only
#   starters    - Update ALL Starter modules only
#   release     - Remove -SNAPSHOT (production release)
#########################################################

set -e

BASE_DIR="/home/thalaimalai_pandiyan/Desktop/release"

#########################################################
#           EDIT VERSIONS HERE
#########################################################

# TLC-CRM-SERVER
# Parent + each submodule has its OWN version
CRM_SERVER_VERSION="9.31.0-SNAPSHOT"       # tlc-crm-server/pom.xml (parent)
CRM_COMMON_VERSION="6.23.0-SNAPSHOT"
CRM_CORE_VERSION="7.40.0-SNAPSHOT"
CRM_ENTITY_VERSION="6.25.0-SNAPSHOT"
CRM_NOTIFICATIONS_VERSION="6.16.0-SNAPSHOT"
CRM_WORKFLOW_VERSION="2.25.0-SNAPSHOT"
CRM_WEBFORM_VERSION="5.14.0-SNAPSHOT"
CRM_ICON_VERSION="2.11.0-SNAPSHOT"
CRM_FIELDS_VERSION="1.17.0-SNAPSHOT"
CRM_SECURITY_VERSION="1.27.0-SNAPSHOT"
CRM_APP_NOTIFICATION_VERSION="1.7.0-SNAPSHOT"
CRM_ANALYTICS_VERSION="2.10.0-SNAPSHOT"
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
CRM_SERVER_STARTER_VERSION="2.31.0-SNAPSHOT"

# TLC-IAM
# Parent + each submodule has its OWN version
IAM_CORE_VERSION="2.27.0-SNAPSHOT"       # tlc-iam/pom.xml (parent)
IAM_CLIENT_VERSION="2.17.0-SNAPSHOT"     # tlc-iam/client/pom.xml
IAM_SERVER_VERSION="2.21.0-SNAPSHOT"     # tlc-iam/server/pom.xml
IAM_COMMON_VERSION="2.20.0-SNAPSHOT"     # tlc-iam/common/pom.xml
IAM_STARTER_VERSION="1.14.0-SNAPSHOT"    # tlc-iam/iam-starter/pom.xml (Spring Boot)

# TLC-EMAIL-SERVICE
# Parent + each submodule has its OWN version
EMAIL_SERVICE_VERSION="1.15.0-SNAPSHOT"  # tlc-email-service/pom.xml (parent)
EMAIL_COMMON_VERSION="1.10.0-SNAPSHOT"   # tlc-email-service/email-common/pom.xml
EMAIL_SERVER_VERSION="1.12.0-SNAPSHOT"   # tlc-email-service/email-server/pom.xml
EMAIL_CLIENT_VERSION="1.8.0-SNAPSHOT"    # tlc-email-service/email-client/pom.xml
EMAIL_STARTER_VERSION="1.11.0-SNAPSHOT"  # tlc-email-service/email-starter/pom.xml (Spring Boot)

# TLC-SOCIAL
# Parent + each submodule has its OWN version
SOCIAL_SERVICE_VERSION="1.15.0-SNAPSHOT" # tlc-social/pom.xml (parent)
SOCIAL_CLIENT_VERSION="1.7.0-SNAPSHOT"   # tlc-social/social-client/pom.xml
SOCIAL_SERVER_VERSION="1.11.0-SNAPSHOT"  # tlc-social/social-server/pom.xml
SOCIAL_COMMON_VERSION="1.11.0-SNAPSHOT"  # tlc-social/social-common/pom.xml
SOCIAL_STARTER_VERSION="1.9.0-SNAPSHOT"  # tlc-social/social-starter/pom.xml (Spring Boot)

# TLC-STORAGE-SERVER (File)
# Parent + each submodule has its OWN version
FILE_STORAGE_VERSION="1.12.0-SNAPSHOT"   # tlc-storage-server/pom.xml (parent)
FILE_CLIENT_VERSION="1.6.0-SNAPSHOT"     # tlc-storage-server/client/pom.xml
FILE_SERVER_VERSION="1.3.0-SNAPSHOT"     # tlc-storage-server/server/pom.xml
FILE_COMMON_VERSION="1.6.0-SNAPSHOT"     # tlc-storage-server/common/pom.xml
FILE_STARTER_VERSION="1.6.0-SNAPSHOT"    # tlc-storage-server/file-starter/pom.xml (Spring Boot)

# TLC-COMPONENTS
# Parent + each component has its OWN version
COMPONENTS_VERSION="6.45.0-SNAPSHOT"           # tlc-components/pom.xml (parent)
TLC_COMMONS_VERSION="5.34.0-SNAPSHOT"
TLC_VALIDATOR_VERSION="5.8.0-SNAPSHOT"
TLC_CACHE_VERSION="5.6.0-SNAPSHOT"
TLC_I18N_VERSION="5.3.0-SNAPSHOT"
TLC_WEB_VERSION="5.17.0-SNAPSHOT"
TLC_WEB_CLIENT_VERSION="5.4.0-SNAPSHOT"
TLC_SQL_VERSION="5.24.0-SNAPSHOT"              # tlc-components/sql/pom.xml (sql parent)
TLC_SQL_COMMON_VERSION="5.24.0-SNAPSHOT"       # tlc-components/sql/sql-common/pom.xml
TLC_SQL_OLTP_VERSION="1.1.0-SNAPSHOT"          # tlc-components/sql/oltp/pom.xml
TLC_SQL_OLAP_VERSION="1.1.0-SNAPSHOT"          # tlc-components/sql/olap/pom.xml
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
COMPONENTS_STARTER_VERSION="2.16.0-SNAPSHOT"   # tlc-components/components-starter/pom.xml

#########################################################
#           HELPER FUNCTIONS
#########################################################

print_header() {
    echo ""
    echo "=========================================="
    echo "  $1"
    echo "=========================================="
}

# Update the <parent><version> inside a child pom.xml reliably using Python3.
# This directly edits the <parent> block to set the correct parent version.
# Usage: update_parent_ref <pom_file> <parent_artifactId> <new_parent_version>
# Safely set a single Maven module version without affecting parents, siblings, or submodules recursively
mvn_set_safe() {
    local has_version
    has_version=$(python3 -c "
import xml.etree.ElementTree as ET
try:
    tree = ET.parse('pom.xml')
    root = tree.getroot()
    if any(elem.tag.split('}')[-1] == 'version' for elem in root):
        print('true')
    else:
        print('false')
except Exception:
    print('false')
")

    if [ "$has_version" = "true" ]; then
        mvn versions:set -DprocessParent=false -DprocessAllModules=false -DprocessFromLocalAggregationRoot=false "$@"
    else
        echo "  Skipping versions:set in \$(pwd) because its version is inherited from parent."
    fi
}

update_parent_ref() {
    local pom_file="$1"
    local parent_artifact_id="$2"
    local new_parent_version="$3"

    python3 -c "
import re, sys

pom_file          = sys.argv[1]
parent_artifact   = sys.argv[2]
new_version       = sys.argv[3]

with open(pom_file, 'r') as f:
    content = f.read()

def replace_parent_version(match):
    block = match.group(0)
    if '<artifactId>' + parent_artifact + '</artifactId>' in block:
        block = re.sub(
            r'<version>[^<]*</version>',
            '<version>' + new_version + '</version>',
            block,
            count=1
        )
    return block

result = re.sub(r'<parent>.*?</parent>', replace_parent_version, content, flags=re.DOTALL)

with open(pom_file, 'w') as f:
    f.write(result)

print('  Updated parent ref in: ' + pom_file)
" "$pom_file" "$parent_artifact_id" "$new_parent_version"
}

#########################################################
#           DISPLAY CURRENT VERSIONS
#########################################################
display_all_versions() {
    print_header "CURRENT VERSIONS - ALL PROJECTS"

    echo ""
    echo "--- TLC-CRM-SERVER ---"
    grep -E "^\s*<version>" "$BASE_DIR/tlc-crm-server/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  Parent (crm-server): \1/'
    grep -E "^\s*<version>" "$BASE_DIR/tlc-crm-server/common/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  common: \1/'
    grep -E "^\s*<version>" "$BASE_DIR/tlc-crm-server/core/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  core: \1/'
    grep -E "^\s*<version>" "$BASE_DIR/tlc-crm-server/entity/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  entity: \1/'
    grep -E "^\s*<version>" "$BASE_DIR/tlc-crm-server/notifications/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  notifications: \1/'
    grep -E "^\s*<version>" "$BASE_DIR/tlc-crm-server/workflow/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  workflow: \1/'
    grep -E "^\s*<version>" "$BASE_DIR/tlc-crm-server/server-starter/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  server-starter (Spring Boot): \1/'

    echo ""
    echo "--- TLC-IAM ---"
    grep -E "^\s*<version>" "$BASE_DIR/tlc-iam/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  Parent (iam-core): \1/'
    grep -E "^\s*<version>" "$BASE_DIR/tlc-iam/client/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  client: \1/'
    grep -E "^\s*<version>" "$BASE_DIR/tlc-iam/server/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  server: \1/'
    grep -E "^\s*<version>" "$BASE_DIR/tlc-iam/common/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  common: \1/'
    grep -E "^\s*<version>" "$BASE_DIR/tlc-iam/iam-starter/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  iam-starter (Spring Boot): \1/'

    echo ""
    echo "--- TLC-EMAIL-SERVICE ---"
    grep -E "^\s*<version>" "$BASE_DIR/tlc-email-service/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  Parent (email-service): \1/'
    grep -E "^\s*<version>" "$BASE_DIR/tlc-email-service/email-common/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  email-common: \1/'
    grep -E "^\s*<version>" "$BASE_DIR/tlc-email-service/email-server/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  email-server: \1/'
    grep -E "^\s*<version>" "$BASE_DIR/tlc-email-service/email-client/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  email-client: \1/'
    grep -E "^\s*<version>" "$BASE_DIR/tlc-email-service/email-starter/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  email-starter (Spring Boot): \1/'

    echo ""
    echo "--- TLC-SOCIAL ---"
    grep -E "^\s*<version>" "$BASE_DIR/tlc-social/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  Parent (social-service): \1/'
    grep -E "^\s*<version>" "$BASE_DIR/tlc-social/social-client/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  social-client: \1/'
    grep -E "^\s*<version>" "$BASE_DIR/tlc-social/social-server/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  social-server: \1/'
    grep -E "^\s*<version>" "$BASE_DIR/tlc-social/social-common/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  social-common: \1/'
    grep -E "^\s*<version>" "$BASE_DIR/tlc-social/social-starter/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  social-starter (Spring Boot): \1/'

    echo ""
    echo "--- TLC-STORAGE-SERVER ---"
    grep -E "^\s*<version>" "$BASE_DIR/tlc-storage-server/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  Parent (file-storage-server): \1/'
    grep -E "^\s*<version>" "$BASE_DIR/tlc-storage-server/client/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  file-client: \1/'
    grep -E "^\s*<version>" "$BASE_DIR/tlc-storage-server/server/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  file-server: \1/'
    grep -E "^\s*<version>" "$BASE_DIR/tlc-storage-server/common/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  file-common: \1/'
    grep -E "^\s*<version>" "$BASE_DIR/tlc-storage-server/file-starter/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  file-starter (Spring Boot): \1/'

    echo ""
    echo "--- TLC-COMPONENTS ---"
    grep -E "^\s*<version>" "$BASE_DIR/tlc-components/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  Parent (components): \1/'
    grep -E "^\s*<version>" "$BASE_DIR/tlc-components/commons/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  commons: \1/'
    grep -E "^\s*<version>" "$BASE_DIR/tlc-components/sql/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  sql (parent): \1/'
    grep -E "^\s*<version>" "$BASE_DIR/tlc-components/sql/sql-common/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  sql-common: \1/'
    grep -E "^\s*<version>" "$BASE_DIR/tlc-components/sql/oltp/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  oltp: \1/'
    grep -E "^\s*<version>" "$BASE_DIR/tlc-components/sql/olap/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  olap: \1/'
    grep -E "^\s*<version>" "$BASE_DIR/tlc-components/components-starter/pom.xml" | head -1 | sed 's/.*<version>\(.*\)<\/version>.*/  components-starter (Spring Boot): \1/'
}

#########################################################
#  TLC-CRM-SERVER
#  Parent has CRM_SERVER_VERSION
#  Each submodule has its OWN version (same pattern as IAM/Social/Email/File)
#########################################################
update_crm_server() {
    print_header "UPDATING TLC-CRM-SERVER"

    # Step 1: Update parent version
    echo "Step 1: Setting crm-server parent version to $CRM_SERVER_VERSION..."
    cd "$BASE_DIR/tlc-crm-server"
    mvn_set_safe -DnewVersion="$CRM_SERVER_VERSION" -DgenerateBackupPoms=false

    # Step 2: Update <parent><version> in each child pom
    echo "Step 2: Updating parent references in child poms..."
    for module in common core entity notifications workflow webform icon fields security \
                  app-notification analytics email-notification product-package zapier \
                  email-core messenger plan-and-billing conversation-handler crm-marketplace \
                  auto-assignment ai features; do
        pom="$BASE_DIR/tlc-crm-server/$module/pom.xml"
        [ -f "$pom" ] && update_parent_ref "$pom" "crm-server" "$CRM_SERVER_VERSION"
    done

    # Step 3: Set each submodule's OWN version
    echo "Step 3: Setting individual submodule versions..."
    cd "$BASE_DIR/tlc-crm-server/common"         && mvn_set_safe -DnewVersion="$CRM_COMMON_VERSION"           -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-crm-server/core"           && mvn_set_safe -DnewVersion="$CRM_CORE_VERSION"             -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-crm-server/entity"         && mvn_set_safe -DnewVersion="$CRM_ENTITY_VERSION"           -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-crm-server/notifications"  && mvn_set_safe -DnewVersion="$CRM_NOTIFICATIONS_VERSION"    -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-crm-server/workflow"       && mvn_set_safe -DnewVersion="$CRM_WORKFLOW_VERSION"         -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-crm-server/webform"        && mvn_set_safe -DnewVersion="$CRM_WEBFORM_VERSION"          -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-crm-server/icon"           && mvn_set_safe -DnewVersion="$CRM_ICON_VERSION"             -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-crm-server/fields"         && mvn_set_safe -DnewVersion="$CRM_FIELDS_VERSION"           -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-crm-server/security"       && mvn_set_safe -DnewVersion="$CRM_SECURITY_VERSION"         -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-crm-server/app-notification" && mvn_set_safe -DnewVersion="$CRM_APP_NOTIFICATION_VERSION" -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-crm-server/analytics"      && mvn_set_safe -DnewVersion="$CRM_ANALYTICS_VERSION"        -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-crm-server/email-notification" && mvn_set_safe -DnewVersion="$CRM_EMAIL_NOTIFICATION_VERSION" -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-crm-server/product-package" && mvn_set_safe -DnewVersion="$CRM_PRODUCT_PACKAGE_VERSION" -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-crm-server/zapier"         && mvn_set_safe -DnewVersion="$CRM_ZAPIER_VERSION"           -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-crm-server/email-core"     && mvn_set_safe -DnewVersion="$CRM_EMAIL_CORE_VERSION"       -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-crm-server/messenger"      && mvn_set_safe -DnewVersion="$CRM_MESSENGER_VERSION"        -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-crm-server/plan-and-billing" && mvn_set_safe -DnewVersion="$CRM_PLAN_BILLING_VERSION"   -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-crm-server/conversation-handler" && mvn_set_safe -DnewVersion="$CRM_CONVERSATION_HANDLER_VERSION" -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-crm-server/crm-marketplace" && mvn_set_safe -DnewVersion="$CRM_MARKETPLACE_VERSION"     -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-crm-server/auto-assignment" && mvn_set_safe -DnewVersion="$CRM_AUTO_ASSIGNMENT_VERSION" -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-crm-server/ai"             && mvn_set_safe -DnewVersion="$CRM_AI_VERSION"               -DgenerateBackupPoms=false

    # Step 4: Update version properties in parent pom
    echo "Step 4: Updating version properties in CRM parent pom..."
    cd "$BASE_DIR/tlc-crm-server"
    mvn versions:set-property -Dproperty=tlc.crm.common.version           -DnewVersion="$CRM_COMMON_VERSION"           -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.core.version             -DnewVersion="$CRM_CORE_VERSION"             -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.entity.version           -DnewVersion="$CRM_ENTITY_VERSION"           -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.notifications.version    -DnewVersion="$CRM_NOTIFICATIONS_VERSION"    -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.workflow.version         -DnewVersion="$CRM_WORKFLOW_VERSION"         -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.webform.version          -DnewVersion="$CRM_WEBFORM_VERSION"          -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.icon.version             -DnewVersion="$CRM_ICON_VERSION"             -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.fields.version           -DnewVersion="$CRM_FIELDS_VERSION"           -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.security.version         -DnewVersion="$CRM_SECURITY_VERSION"         -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.app.notification.version -DnewVersion="$CRM_APP_NOTIFICATION_VERSION" -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.analytics.version        -DnewVersion="$CRM_ANALYTICS_VERSION"        -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.email.notification.version -DnewVersion="$CRM_EMAIL_NOTIFICATION_VERSION" -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.product.package.version  -DnewVersion="$CRM_PRODUCT_PACKAGE_VERSION"  -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.zapier.version           -DnewVersion="$CRM_ZAPIER_VERSION"           -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.email.core.version       -DnewVersion="$CRM_EMAIL_CORE_VERSION"       -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.messenger.version        -DnewVersion="$CRM_MESSENGER_VERSION"        -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.marketplace.version      -DnewVersion="$CRM_MARKETPLACE_VERSION"      -DgenerateBackupPoms=false

    echo "Updating TLC component dependency versions in CRM parent..."
    mvn versions:set-property -Dproperty=tlc.commons.version              -DnewVersion="$TLC_COMMONS_VERSION"          -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.validator.version            -DnewVersion="$TLC_VALIDATOR_VERSION"        -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.cache.version                -DnewVersion="$TLC_CACHE_VERSION"            -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.i18n.version                 -DnewVersion="$TLC_I18N_VERSION"             -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.web.version                  -DnewVersion="$TLC_WEB_VERSION"              -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.web.client.version           -DnewVersion="$TLC_WEB_CLIENT_VERSION"       -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.sql.common.version           -DnewVersion="$TLC_SQL_COMMON_VERSION"       -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.sql.oltp.version             -DnewVersion="$TLC_SQL_OLTP_VERSION"         -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.sql.olap.version             -DnewVersion="$TLC_SQL_OLAP_VERSION"         -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.queue.version                -DnewVersion="$TLC_QUEUE_VERSION"            -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.notification.version         -DnewVersion="$TLC_NOTIFICATION_VERSION"     -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.attachment.version           -DnewVersion="$TLC_ATTACHMENT_VERSION"       -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.scheduler.version            -DnewVersion="$TLC_SCHEDULER_VERSION"        -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.cluster.version              -DnewVersion="$TLC_CLUSTER_VERSION"          -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.search.version               -DnewVersion="$TLC_SEARCH_VERSION"           -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.exchange.rate.version        -DnewVersion="$TLC_EXCHANGE_RATE_VERSION"    -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.l10n.version                 -DnewVersion="$TLC_L10N_VERSION"             -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.entity.version               -DnewVersion="$TLC_ENTITY_VERSION"           -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.sync.service.oauth.version   -DnewVersion="$TLC_SYNC_SERVICE_OAUTH_VERSION" -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.otp.processor.version        -DnewVersion="$TLC_OTP_PROCESSOR_VERSION"    -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.plans.and.billings.version   -DnewVersion="$TLC_PLANS_AND_BILLINGS_VERSION" -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.payment.gateway.version      -DnewVersion="$TLC_PAYMENT_GATEWAY_VERSION"  -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.marketplace.version          -DnewVersion="$TLC_MARKETPLACE_VERSION"      -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.identity.provider            -DnewVersion="$TLC_IDENTITY_PROVIDER_VERSION" -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.cdc.core.version             -DnewVersion="$TLC_CDC_CORE_VERSION"         -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.iam.client.version           -DnewVersion="$IAM_CLIENT_VERSION"           -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.file.client.version          -DnewVersion="$FILE_CLIENT_VERSION"          -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.email.client.version         -DnewVersion="$EMAIL_CLIENT_VERSION"         -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.social.client.version        -DnewVersion="$SOCIAL_CLIENT_VERSION"        -DgenerateBackupPoms=false

    echo "CRM Server updated successfully!"
}

update_crm_server_starter() {
    print_header "UPDATING CRM SERVER STARTER"
    cd "$BASE_DIR/tlc-crm-server/server-starter"

    mvn_set_safe -DnewVersion="$CRM_SERVER_STARTER_VERSION" -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.components.starter.version          -DnewVersion="$COMPONENTS_STARTER_VERSION"    -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.common.version                  -DnewVersion="$CRM_COMMON_VERSION"             -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.core.version                    -DnewVersion="$CRM_CORE_VERSION"               -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.entity.version                  -DnewVersion="$CRM_ENTITY_VERSION"             -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.notifications.version           -DnewVersion="$CRM_NOTIFICATIONS_VERSION"      -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.workflow.version                -DnewVersion="$CRM_WORKFLOW_VERSION"           -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.webform.version                 -DnewVersion="$CRM_WEBFORM_VERSION"            -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.icon.version                    -DnewVersion="$CRM_ICON_VERSION"               -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.fields.version                  -DnewVersion="$CRM_FIELDS_VERSION"             -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.security.version                -DnewVersion="$CRM_SECURITY_VERSION"           -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.app.notification.version        -DnewVersion="$CRM_APP_NOTIFICATION_VERSION"   -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.analytics.version               -DnewVersion="$CRM_ANALYTICS_VERSION"          -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.email.notification.version      -DnewVersion="$CRM_EMAIL_NOTIFICATION_VERSION" -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.product.package.version         -DnewVersion="$CRM_PRODUCT_PACKAGE_VERSION"    -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.zapier.version                  -DnewVersion="$CRM_ZAPIER_VERSION"             -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.email.core.version              -DnewVersion="$CRM_EMAIL_CORE_VERSION"         -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.messenger.version               -DnewVersion="$CRM_MESSENGER_VERSION"          -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.plan.and.billing.version        -DnewVersion="$CRM_PLAN_BILLING_VERSION"       -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.ai.version                      -DnewVersion="$CRM_AI_VERSION"                 -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.conversation.handler.version    -DnewVersion="$CRM_CONVERSATION_HANDLER_VERSION" -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.marketplace.version             -DnewVersion="$CRM_MARKETPLACE_VERSION"        -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.crm.auto.assignment.version         -DnewVersion="$CRM_AUTO_ASSIGNMENT_VERSION"    -DgenerateBackupPoms=false

    echo "CRM Server Starter updated successfully!"
}

#########################################################
#  TLC-IAM
#  Parent has IAM_CORE_VERSION
#  Each submodule has its OWN version
#########################################################
update_iam() {
    print_header "UPDATING TLC-IAM"

    # Step 1: Update parent version
    echo "Step 1: Setting iam-core parent version to $IAM_CORE_VERSION..."
    cd "$BASE_DIR/tlc-iam"
    mvn_set_safe -DnewVersion="$IAM_CORE_VERSION" -DgenerateBackupPoms=false

    # Step 2: Update <parent><version> in each child pom
    echo "Step 2: Updating parent references in child poms..."
    update_parent_ref "$BASE_DIR/tlc-iam/client/pom.xml"   "iam-core" "$IAM_CORE_VERSION"
    update_parent_ref "$BASE_DIR/tlc-iam/server/pom.xml"   "iam-core" "$IAM_CORE_VERSION"
    update_parent_ref "$BASE_DIR/tlc-iam/common/pom.xml"   "iam-core" "$IAM_CORE_VERSION"
    update_parent_ref "$BASE_DIR/tlc-iam/features/pom.xml" "iam-core" "$IAM_CORE_VERSION"

    # Step 3: Set each submodule's OWN version
    echo "Step 3: Setting individual submodule versions..."
    cd "$BASE_DIR/tlc-iam/client"
    mvn_set_safe -DnewVersion="$IAM_CLIENT_VERSION" -DgenerateBackupPoms=false

    cd "$BASE_DIR/tlc-iam/server"
    mvn_set_safe -DnewVersion="$IAM_SERVER_VERSION" -DgenerateBackupPoms=false

    cd "$BASE_DIR/tlc-iam/common"
    mvn_set_safe -DnewVersion="$IAM_COMMON_VERSION" -DgenerateBackupPoms=false

    # Step 4: Update version properties in parent pom
    echo "Step 4: Updating version properties in IAM parent pom..."
    cd "$BASE_DIR/tlc-iam"
    mvn versions:set-property -Dproperty=iam.client.version     -DnewVersion="$IAM_CLIENT_VERSION"          -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=iam.server.version     -DnewVersion="$IAM_SERVER_VERSION"          -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=iam.common.version     -DnewVersion="$IAM_COMMON_VERSION"          -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.commons.version    -DnewVersion="$TLC_COMMONS_VERSION"         -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.validator.version  -DnewVersion="$TLC_VALIDATOR_VERSION"       -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.cache.version      -DnewVersion="$TLC_CACHE_VERSION"           -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.i18n.version       -DnewVersion="$TLC_I18N_VERSION"            -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.web.version        -DnewVersion="$TLC_WEB_VERSION"             -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.web.client.version -DnewVersion="$TLC_WEB_CLIENT_VERSION"      -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.queue.version      -DnewVersion="$TLC_QUEUE_VERSION"           -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.attachment.version -DnewVersion="$TLC_ATTACHMENT_VERSION"      -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.scheduler.version  -DnewVersion="$TLC_SCHEDULER_VERSION"       -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.cluster.version    -DnewVersion="$TLC_CLUSTER_VERSION"         -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.search.version     -DnewVersion="$TLC_SEARCH_VERSION"          -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.l10n.version       -DnewVersion="$TLC_L10N_VERSION"            -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.sql.common.version -DnewVersion="$TLC_SQL_COMMON_VERSION"      -DgenerateBackupPoms=false

    echo "IAM updated successfully!"
}

update_iam_starter() {
    print_header "UPDATING IAM STARTER"
    cd "$BASE_DIR/tlc-iam/iam-starter"

    mvn_set_safe -DnewVersion="$IAM_STARTER_VERSION" -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.components.starter.version -DnewVersion="$COMPONENTS_STARTER_VERSION" -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=iam.client.version              -DnewVersion="$IAM_CLIENT_VERSION"         -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=iam.server.version              -DnewVersion="$IAM_SERVER_VERSION"         -DgenerateBackupPoms=false

    echo "IAM Starter updated successfully!"
}

#########################################################
#  TLC-EMAIL-SERVICE
#  Parent has EMAIL_SERVICE_VERSION
#  Each submodule has its OWN version
#########################################################
update_email_service() {
    print_header "UPDATING TLC-EMAIL-SERVICE"

    # Step 1: Update parent version
    echo "Step 1: Setting email-service parent version to $EMAIL_SERVICE_VERSION..."
    cd "$BASE_DIR/tlc-email-service"
    mvn_set_safe -DnewVersion="$EMAIL_SERVICE_VERSION" -DgenerateBackupPoms=false

    # Step 2: Update <parent><version> in each child pom
    echo "Step 2: Updating parent references in child poms..."
    update_parent_ref "$BASE_DIR/tlc-email-service/email-common/pom.xml" "email-service" "$EMAIL_SERVICE_VERSION"
    update_parent_ref "$BASE_DIR/tlc-email-service/email-server/pom.xml" "email-service" "$EMAIL_SERVICE_VERSION"
    update_parent_ref "$BASE_DIR/tlc-email-service/email-client/pom.xml" "email-service" "$EMAIL_SERVICE_VERSION"

    # Step 3: Set each submodule's OWN version
    echo "Step 3: Setting individual submodule versions..."
    cd "$BASE_DIR/tlc-email-service/email-common"
    mvn_set_safe -DnewVersion="$EMAIL_COMMON_VERSION" -DgenerateBackupPoms=false

    cd "$BASE_DIR/tlc-email-service/email-server"
    mvn_set_safe -DnewVersion="$EMAIL_SERVER_VERSION" -DgenerateBackupPoms=false

    cd "$BASE_DIR/tlc-email-service/email-client"
    mvn_set_safe -DnewVersion="$EMAIL_CLIENT_VERSION" -DgenerateBackupPoms=false

    # Step 4: Update version properties in parent pom
    echo "Step 4: Updating version properties in email-service parent pom..."
    cd "$BASE_DIR/tlc-email-service"
    mvn versions:set-property -Dproperty=tlc.email.common.version         -DnewVersion="$EMAIL_COMMON_VERSION"        -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.email.server.version         -DnewVersion="$EMAIL_SERVER_VERSION"        -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.commons.version              -DnewVersion="$TLC_COMMONS_VERSION"         -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.validator.version            -DnewVersion="$TLC_VALIDATOR_VERSION"       -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.cache.version                -DnewVersion="$TLC_CACHE_VERSION"           -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.i18n.version                 -DnewVersion="$TLC_I18N_VERSION"            -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.web.version                  -DnewVersion="$TLC_WEB_VERSION"             -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.web.client.version           -DnewVersion="$TLC_WEB_CLIENT_VERSION"      -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.queue.version                -DnewVersion="$TLC_QUEUE_VERSION"           -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.notification.version         -DnewVersion="$TLC_NOTIFICATION_VERSION"    -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.attachment.version           -DnewVersion="$TLC_ATTACHMENT_VERSION"      -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.scheduler.version            -DnewVersion="$TLC_SCHEDULER_VERSION"       -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.cluster.version              -DnewVersion="$TLC_CLUSTER_VERSION"         -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.search.version               -DnewVersion="$TLC_SEARCH_VERSION"          -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.l10n.version                 -DnewVersion="$TLC_L10N_VERSION"            -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.sync.service.oauth.version   -DnewVersion="$TLC_SYNC_SERVICE_OAUTH_VERSION" -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.sql.common.version           -DnewVersion="$TLC_SQL_COMMON_VERSION"      -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.file.client.version          -DnewVersion="$FILE_CLIENT_VERSION"         -DgenerateBackupPoms=false

    echo "Email Service updated successfully!"
}

update_email_starter() {
    print_header "UPDATING EMAIL STARTER"
    cd "$BASE_DIR/tlc-email-service/email-starter"

    mvn_set_safe -DnewVersion="$EMAIL_STARTER_VERSION" -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.components.starter.version -DnewVersion="$COMPONENTS_STARTER_VERSION" -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.email.client.version        -DnewVersion="$EMAIL_CLIENT_VERSION"        -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.email.server.version        -DnewVersion="$EMAIL_SERVER_VERSION"        -DgenerateBackupPoms=false

    echo "Email Starter updated successfully!"
}

#########################################################
#  TLC-SOCIAL
#  Parent has SOCIAL_SERVICE_VERSION
#  Each submodule has its OWN version
#########################################################
update_social_service() {
    print_header "UPDATING TLC-SOCIAL"

    # Step 1: Update parent version
    echo "Step 1: Setting social-service parent version to $SOCIAL_SERVICE_VERSION..."
    cd "$BASE_DIR/tlc-social"
    mvn_set_safe -DnewVersion="$SOCIAL_SERVICE_VERSION" -DgenerateBackupPoms=false

    # Step 2: Update <parent><version> in each child pom
    echo "Step 2: Updating parent references in child poms..."
    update_parent_ref "$BASE_DIR/tlc-social/social-client/pom.xml" "social-service" "$SOCIAL_SERVICE_VERSION"
    update_parent_ref "$BASE_DIR/tlc-social/social-server/pom.xml" "social-service" "$SOCIAL_SERVICE_VERSION"
    update_parent_ref "$BASE_DIR/tlc-social/social-common/pom.xml" "social-service" "$SOCIAL_SERVICE_VERSION"

    # Step 3: Set each submodule's OWN version
    echo "Step 3: Setting individual submodule versions..."
    cd "$BASE_DIR/tlc-social/social-client"
    mvn_set_safe -DnewVersion="$SOCIAL_CLIENT_VERSION" -DgenerateBackupPoms=false

    cd "$BASE_DIR/tlc-social/social-server"
    mvn_set_safe -DnewVersion="$SOCIAL_SERVER_VERSION" -DgenerateBackupPoms=false

    cd "$BASE_DIR/tlc-social/social-common"
    mvn_set_safe -DnewVersion="$SOCIAL_COMMON_VERSION" -DgenerateBackupPoms=false

    # Step 4: Update version properties in parent pom
    echo "Step 4: Updating version properties in social parent pom..."
    cd "$BASE_DIR/tlc-social"
    mvn versions:set-property -Dproperty=tlc.social.client.version       -DnewVersion="$SOCIAL_CLIENT_VERSION"       -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.social.server.version       -DnewVersion="$SOCIAL_SERVER_VERSION"       -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.social.common.version       -DnewVersion="$SOCIAL_COMMON_VERSION"       -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.commons.version             -DnewVersion="$TLC_COMMONS_VERSION"         -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.validator.version           -DnewVersion="$TLC_VALIDATOR_VERSION"       -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.cache.version               -DnewVersion="$TLC_CACHE_VERSION"           -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.i18n.version                -DnewVersion="$TLC_I18N_VERSION"            -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.web.version                 -DnewVersion="$TLC_WEB_VERSION"             -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.web.client.version          -DnewVersion="$TLC_WEB_CLIENT_VERSION"      -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.queue.version               -DnewVersion="$TLC_QUEUE_VERSION"           -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.notification.version        -DnewVersion="$TLC_NOTIFICATION_VERSION"    -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.attachment.version          -DnewVersion="$TLC_ATTACHMENT_VERSION"      -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.scheduler.version           -DnewVersion="$TLC_SCHEDULER_VERSION"       -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.cluster.version             -DnewVersion="$TLC_CLUSTER_VERSION"         -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.search.version              -DnewVersion="$TLC_SEARCH_VERSION"          -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.exchange.rate.version       -DnewVersion="$TLC_EXCHANGE_RATE_VERSION"   -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.l10n.version                -DnewVersion="$TLC_L10N_VERSION"            -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.sync.service.oauth.version  -DnewVersion="$TLC_SYNC_SERVICE_OAUTH_VERSION" -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.sql.common.version          -DnewVersion="$TLC_SQL_COMMON_VERSION"      -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.file.common.version         -DnewVersion="$FILE_COMMON_VERSION"         -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.file.client.version         -DnewVersion="$FILE_CLIENT_VERSION"         -DgenerateBackupPoms=false

    echo "Social Service updated successfully!"
}

update_social_starter() {
    print_header "UPDATING SOCIAL STARTER"
    cd "$BASE_DIR/tlc-social/social-starter"

    mvn_set_safe -DnewVersion="$SOCIAL_STARTER_VERSION" -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.components.starter.version -DnewVersion="$COMPONENTS_STARTER_VERSION" -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.social.client.version       -DnewVersion="$SOCIAL_CLIENT_VERSION"       -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.social.server.version       -DnewVersion="$SOCIAL_SERVER_VERSION"       -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.social.common.version       -DnewVersion="$SOCIAL_COMMON_VERSION"       -DgenerateBackupPoms=false

    echo "Social Starter updated successfully!"
}

#########################################################
#  TLC-STORAGE-SERVER
#  Parent has FILE_STORAGE_VERSION
#  Each submodule has its OWN version
#########################################################
update_file_storage() {
    print_header "UPDATING TLC-STORAGE-SERVER"

    # Step 1: Update parent version
    echo "Step 1: Setting file-storage-server parent version to $FILE_STORAGE_VERSION..."
    cd "$BASE_DIR/tlc-storage-server"
    mvn_set_safe -DnewVersion="$FILE_STORAGE_VERSION" -DgenerateBackupPoms=false

    # Step 2: Update <parent><version> in each child pom
    echo "Step 2: Updating parent references in child poms..."
    update_parent_ref "$BASE_DIR/tlc-storage-server/client/pom.xml" "file-storage-server" "$FILE_STORAGE_VERSION"
    update_parent_ref "$BASE_DIR/tlc-storage-server/server/pom.xml" "file-storage-server" "$FILE_STORAGE_VERSION"
    update_parent_ref "$BASE_DIR/tlc-storage-server/common/pom.xml" "file-storage-server" "$FILE_STORAGE_VERSION"

    # Step 3: Set each submodule's OWN version
    echo "Step 3: Setting individual submodule versions..."
    cd "$BASE_DIR/tlc-storage-server/client"
    mvn_set_safe -DnewVersion="$FILE_CLIENT_VERSION" -DgenerateBackupPoms=false

    cd "$BASE_DIR/tlc-storage-server/server"
    mvn_set_safe -DnewVersion="$FILE_SERVER_VERSION" -DgenerateBackupPoms=false

    cd "$BASE_DIR/tlc-storage-server/common"
    mvn_set_safe -DnewVersion="$FILE_COMMON_VERSION" -DgenerateBackupPoms=false

    # Step 4: Update version properties in parent pom
    echo "Step 4: Updating version properties in file-storage parent pom..."
    cd "$BASE_DIR/tlc-storage-server"
    mvn versions:set-property -Dproperty=tlc.file.client.version    -DnewVersion="$FILE_CLIENT_VERSION"          -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.file.server.version    -DnewVersion="$FILE_SERVER_VERSION"          -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.file.common.version    -DnewVersion="$FILE_COMMON_VERSION"          -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.commons.version        -DnewVersion="$TLC_COMMONS_VERSION"          -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.validator.version      -DnewVersion="$TLC_VALIDATOR_VERSION"        -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.cache.version          -DnewVersion="$TLC_CACHE_VERSION"            -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.i18n.version           -DnewVersion="$TLC_I18N_VERSION"             -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.web.version            -DnewVersion="$TLC_WEB_VERSION"              -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.web.client.version     -DnewVersion="$TLC_WEB_CLIENT_VERSION"       -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.queue.version          -DnewVersion="$TLC_QUEUE_VERSION"            -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.attachment.version     -DnewVersion="$TLC_ATTACHMENT_VERSION"       -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.scheduler.version      -DnewVersion="$TLC_SCHEDULER_VERSION"        -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.cluster.version        -DnewVersion="$TLC_CLUSTER_VERSION"          -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.l10n.version           -DnewVersion="$TLC_L10N_VERSION"             -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.sql.common.version     -DnewVersion="$TLC_SQL_COMMON_VERSION"       -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.iam.client.version     -DnewVersion="$IAM_CLIENT_VERSION"           -DgenerateBackupPoms=false

    echo "File Storage Server updated successfully!"
}

update_file_starter() {
    print_header "UPDATING FILE STARTER"
    cd "$BASE_DIR/tlc-storage-server/file-starter"

    mvn_set_safe -DnewVersion="$FILE_STARTER_VERSION" -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.components.starter.version -DnewVersion="$COMPONENTS_STARTER_VERSION" -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.file.client.version         -DnewVersion="$FILE_CLIENT_VERSION"         -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.file.server.version         -DnewVersion="$FILE_SERVER_VERSION"         -DgenerateBackupPoms=false

    echo "File Starter updated successfully!"
}

#########################################################
#  TLC-COMPONENTS
#  Parent has COMPONENTS_VERSION
#  Each component has its OWN version (same pattern as IAM/Social/Email/File)
#########################################################
update_components() {
    print_header "UPDATING TLC-COMPONENTS"

    # Step 1: Update parent version
    echo "Step 1: Setting components parent version to $COMPONENTS_VERSION..."
    cd "$BASE_DIR/tlc-components"
    mvn_set_safe -DnewVersion="$COMPONENTS_VERSION" -DgenerateBackupPoms=false

    # Step 2: Update <parent><version> in all direct child poms
    echo "Step 2: Updating parent references in child poms..."
    for module in commons validator cache i18n web webclient queue notification \
                  attachment scheduler cluster search exchange-rate l10n entity \
                  sync-service-oauth otp-processor plans-and-billings payment-gateway \
                  marketplace identity-provider cdc-core features tools observability sql; do
        pom="$BASE_DIR/tlc-components/$module/pom.xml"
        [ -f "$pom" ] && update_parent_ref "$pom" "components" "$COMPONENTS_VERSION"
    done

    # Step 3: Set each component's OWN version
    echo "Step 3: Setting individual component versions..."
    cd "$BASE_DIR/tlc-components/commons"             && mvn_set_safe -DnewVersion="$TLC_COMMONS_VERSION"           -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-components/validator"           && mvn_set_safe -DnewVersion="$TLC_VALIDATOR_VERSION"         -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-components/cache"               && mvn_set_safe -DnewVersion="$TLC_CACHE_VERSION"             -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-components/i18n"                && mvn_set_safe -DnewVersion="$TLC_I18N_VERSION"              -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-components/web"                 && mvn_set_safe -DnewVersion="$TLC_WEB_VERSION"               -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-components/webclient"           && mvn_set_safe -DnewVersion="$TLC_WEB_CLIENT_VERSION"        -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-components/queue"               && mvn_set_safe -DnewVersion="$TLC_QUEUE_VERSION"             -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-components/notification"        && mvn_set_safe -DnewVersion="$TLC_NOTIFICATION_VERSION"      -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-components/attachment"          && mvn_set_safe -DnewVersion="$TLC_ATTACHMENT_VERSION"        -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-components/scheduler"           && mvn_set_safe -DnewVersion="$TLC_SCHEDULER_VERSION"         -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-components/cluster"             && mvn_set_safe -DnewVersion="$TLC_CLUSTER_VERSION"           -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-components/search"              && mvn_set_safe -DnewVersion="$TLC_SEARCH_VERSION"            -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-components/exchange-rate"       && mvn_set_safe -DnewVersion="$TLC_EXCHANGE_RATE_VERSION"     -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-components/l10n"                && mvn_set_safe -DnewVersion="$TLC_L10N_VERSION"              -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-components/entity"              && mvn_set_safe -DnewVersion="$TLC_ENTITY_VERSION"            -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-components/sync-service-oauth"  && mvn_set_safe -DnewVersion="$TLC_SYNC_SERVICE_OAUTH_VERSION" -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-components/otp-processor"       && mvn_set_safe -DnewVersion="$TLC_OTP_PROCESSOR_VERSION"     -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-components/plans-and-billings"  && mvn_set_safe -DnewVersion="$TLC_PLANS_AND_BILLINGS_VERSION" -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-components/payment-gateway"     && mvn_set_safe -DnewVersion="$TLC_PAYMENT_GATEWAY_VERSION"   -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-components/marketplace"         && mvn_set_safe -DnewVersion="$TLC_MARKETPLACE_VERSION"       -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-components/identity-provider"   && mvn_set_safe -DnewVersion="$TLC_IDENTITY_PROVIDER_VERSION" -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-components/cdc-core"            && mvn_set_safe -DnewVersion="$TLC_CDC_CORE_VERSION"          -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-components/features"            && mvn_set_safe -DnewVersion="$TLC_FEATURES_VERSION"          -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-components/tools"               && mvn_set_safe -DnewVersion="$TLC_TOOLS_VERSION"             -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-components/observability"       && mvn_set_safe -DnewVersion="$TLC_OBSERVABILITY_VERSION"     -DgenerateBackupPoms=false

    # Step 3b: Handle SQL sub-parent and its children (nested structure)
    echo "Step 3b: Updating sql parent and its children..."
    cd "$BASE_DIR/tlc-components/sql"
    mvn_set_safe -DnewVersion="$TLC_SQL_VERSION" -DgenerateBackupPoms=false

    # Update sql children parent references to point to sql parent
    update_parent_ref "$BASE_DIR/tlc-components/sql/sql-common/pom.xml" "sql" "$TLC_SQL_VERSION"
    update_parent_ref "$BASE_DIR/tlc-components/sql/oltp/pom.xml"       "sql" "$TLC_SQL_VERSION"
    update_parent_ref "$BASE_DIR/tlc-components/sql/olap/pom.xml"       "sql" "$TLC_SQL_VERSION"

    # Set sql submodule versions
    cd "$BASE_DIR/tlc-components/sql/sql-common"      && mvn_set_safe -DnewVersion="$TLC_SQL_COMMON_VERSION" -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-components/sql/oltp"            && mvn_set_safe -DnewVersion="$TLC_SQL_OLTP_VERSION"   -DgenerateBackupPoms=false
    cd "$BASE_DIR/tlc-components/sql/olap"            && mvn_set_safe -DnewVersion="$TLC_SQL_OLAP_VERSION"   -DgenerateBackupPoms=false

    # Step 4: Update version properties in parent pom
    echo "Step 4: Updating version properties in components parent pom..."
    cd "$BASE_DIR/tlc-components"
    mvn versions:set-property -Dproperty=tlc.commons.version              -DnewVersion="$TLC_COMMONS_VERSION"          -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.validator.version            -DnewVersion="$TLC_VALIDATOR_VERSION"        -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.cache.version                -DnewVersion="$TLC_CACHE_VERSION"            -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.i18n.version                 -DnewVersion="$TLC_I18N_VERSION"             -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.web.version                  -DnewVersion="$TLC_WEB_VERSION"              -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.web.client.version           -DnewVersion="$TLC_WEB_CLIENT_VERSION"       -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.sql.common.version           -DnewVersion="$TLC_SQL_COMMON_VERSION"       -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.sql.oltp.version             -DnewVersion="$TLC_SQL_OLTP_VERSION"         -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.queue.version                -DnewVersion="$TLC_QUEUE_VERSION"            -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.notification.version         -DnewVersion="$TLC_NOTIFICATION_VERSION"     -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.attachment.version           -DnewVersion="$TLC_ATTACHMENT_VERSION"       -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.scheduler.version            -DnewVersion="$TLC_SCHEDULER_VERSION"        -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.cluster.version              -DnewVersion="$TLC_CLUSTER_VERSION"          -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.search.version               -DnewVersion="$TLC_SEARCH_VERSION"           -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.exchange.rate.version        -DnewVersion="$TLC_EXCHANGE_RATE_VERSION"    -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.l10n.version                 -DnewVersion="$TLC_L10N_VERSION"             -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.entity.version               -DnewVersion="$TLC_ENTITY_VERSION"           -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.sync.service.oauth.version   -DnewVersion="$TLC_SYNC_SERVICE_OAUTH_VERSION" -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.otp.processor.version        -DnewVersion="$TLC_OTP_PROCESSOR_VERSION"    -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.payment.gateway.version      -DnewVersion="$TLC_PAYMENT_GATEWAY_VERSION"  -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.identity.provider.version    -DnewVersion="$TLC_IDENTITY_PROVIDER_VERSION" -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.cdc.core.version             -DnewVersion="$TLC_CDC_CORE_VERSION"         -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.features.version             -DnewVersion="$TLC_FEATURES_VERSION"         -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.tools.version                -DnewVersion="$TLC_TOOLS_VERSION"            -DgenerateBackupPoms=false

    echo "Components updated successfully!"
}

update_components_starter() {
    print_header "UPDATING COMPONENTS STARTER"
    cd "$BASE_DIR/tlc-components/components-starter"

    mvn_set_safe -DnewVersion="$COMPONENTS_STARTER_VERSION" -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.commons.version              -DnewVersion="$TLC_COMMONS_VERSION"          -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.validator.version            -DnewVersion="$TLC_VALIDATOR_VERSION"        -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.cache.version                -DnewVersion="$TLC_CACHE_VERSION"            -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.i18n.version                 -DnewVersion="$TLC_I18N_VERSION"             -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.web.version                  -DnewVersion="$TLC_WEB_VERSION"              -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.web.client.version           -DnewVersion="$TLC_WEB_CLIENT_VERSION"       -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.sql.common.version           -DnewVersion="$TLC_SQL_COMMON_VERSION"       -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.sql.oltp.version             -DnewVersion="$TLC_SQL_OLTP_VERSION"         -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.sql.olap.version             -DnewVersion="$TLC_SQL_OLAP_VERSION"         -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.queue.version                -DnewVersion="$TLC_QUEUE_VERSION"            -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.notification.version         -DnewVersion="$TLC_NOTIFICATION_VERSION"     -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.attachment.version           -DnewVersion="$TLC_ATTACHMENT_VERSION"       -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.scheduler.version            -DnewVersion="$TLC_SCHEDULER_VERSION"        -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.cluster.version              -DnewVersion="$TLC_CLUSTER_VERSION"          -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.search.version               -DnewVersion="$TLC_SEARCH_VERSION"           -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.exchange.rate.version        -DnewVersion="$TLC_EXCHANGE_RATE_VERSION"    -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.l10n.version                 -DnewVersion="$TLC_L10N_VERSION"             -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.entity.version               -DnewVersion="$TLC_ENTITY_VERSION"           -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.sync.service.oauth.version   -DnewVersion="$TLC_SYNC_SERVICE_OAUTH_VERSION" -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.otp.processor.version        -DnewVersion="$TLC_OTP_PROCESSOR_VERSION"    -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.plans.and.billings.version   -DnewVersion="$TLC_PLANS_AND_BILLINGS_VERSION" -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.payment.gateway.version      -DnewVersion="$TLC_PAYMENT_GATEWAY_VERSION"  -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.marketplace.version          -DnewVersion="$TLC_MARKETPLACE_VERSION"      -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.identity.provider.version    -DnewVersion="$TLC_IDENTITY_PROVIDER_VERSION" -DgenerateBackupPoms=false
    mvn versions:set-property -Dproperty=tlc.cdc.core.version             -DnewVersion="$TLC_CDC_CORE_VERSION"         -DgenerateBackupPoms=false

    echo "Components Starter updated successfully!"
}

#########################################################
#  ORCHESTRATION
#########################################################
update_all_starters() {
    print_header "UPDATING ALL STARTERS"
    update_components_starter
    update_crm_server_starter
    update_iam_starter
    update_email_starter
    update_social_starter
    update_file_starter
    print_header "ALL STARTERS UPDATED"
}

update_all() {
    print_header "UPDATING ALL TLC PROJECTS"
    update_components
    update_crm_server
    update_iam
    update_email_service
    update_social_service
    update_file_storage
    update_all_starters
    print_header "ALL PROJECTS UPDATED SUCCESSFULLY!"
}

remove_all_snapshots() {
    print_header "REMOVING -SNAPSHOT FOR PRODUCTION RELEASE"
    echo "Removing -SNAPSHOT from all pom.xml files recursively..."
    for project in tlc-crm-server tlc-iam tlc-email-service tlc-social tlc-storage-server tlc-components; do
        echo "  Processing $project..."
        find "$BASE_DIR/$project" -name "pom.xml" -exec sed -i 's/-SNAPSHOT</</' {} \;
    done
    echo "Done. Review changes and commit."
}

#########################################################
#  MAIN
#########################################################
case "${1:-help}" in
    display)    display_all_versions ;;
    all)        update_all ;;
    crm)        update_crm_server && update_crm_server_starter ;;
    iam)        update_iam && update_iam_starter ;;
    email)      update_email_service && update_email_starter ;;
    social)     update_social_service && update_social_starter ;;
    file)       update_file_storage && update_file_starter ;;
    components) update_components && update_components_starter ;;
    starters)   update_all_starters ;;
    release)    remove_all_snapshots ;;
    help|*)
        echo ""
        echo "TLC Platform - Master Version Update Script"
        echo "Usage: $0 <command>"
        echo ""
        echo "  display     - Show all current versions"
        echo "  all         - Update EVERYTHING"
        echo "  crm         - CRM Server + starter"
        echo "  iam         - IAM + starter"
        echo "  email       - Email Service + starter"
        echo "  social      - Social Service + starter"
        echo "  file        - File Storage + starter"
        echo "  components  - Components (incl sql/*) + starter"
        echo "  starters    - All starters only"
        echo "  release     - Remove -SNAPSHOT (production)"
        echo ""
        exit 0 ;;
esac

echo ""
echo "Done!"

