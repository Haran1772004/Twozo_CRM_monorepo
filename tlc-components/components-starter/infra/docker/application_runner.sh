#!/usr/bin/env bash
set -euo pipefail

# ──────────────────────────────────────────────────────────────────────────────
# application_runner.sh
#
# Interactive script to Maven-build, Docker-image-build and deploy the
# TWOZO CRM platform via docker compose.
#
# Runtime inputs:
#   --server        Target deployment server:
#                   "local"       → Deploy to local machine (default)
#   --build-mode    "full"         → Maven build + Docker image build
#                   "docker-only"  → Docker image build only (reuse existing jars)
#                   "skip"         → No build at all, just (re)deploy containers
#   --modules, -m   Which service modules to build (all | comma-separated list)
#   --clean         "all"  → Tear down containers AND remove all volumes
#                            (postgres, opensearch, application data, etc.)
#                   "none" → Only recreate containers; DB and app data survive
#   --tunnel        Enable ngrok tunnel for remote access via static URL
#   --crm-sub       Comma-separated submodules inside tlc-crm-server to build
#                   (e.g. core,workflow). "all" or omit to build everything.
#                   Discovered dynamically from tlc-crm-server/pom.xml at runtime.
#   --skip-components  Skip Maven build for tlc-components & components-starter.
#                      Useful when only service code has changed and shared
#                      component libraries are already up to date.
#                      Accepts "yes" or "no". Prompted interactively if omitted.
#   --ai-fix        Enable AI-assisted build error resolution using Claude.
#                   "yes" → Auto-fix build errors with Claude AI (default if ANTHROPIC_API_KEY is set)
#                   "no"  → Disable AI assistance
#   --ai-max-retries  Maximum number of AI fix attempts per build step (default: 3)
#   --help, -h      Show usage
#
# If any flag is omitted the script will prompt for it interactively.
#
# Environment variables:
#   DOCKER_BIN          – docker binary / command (default: "docker")
#   MVN_BIN             – mvn binary / command    (default: "mvn")
#   MVN_OPTS            – extra Maven CLI flags   (default: "-DskipTests")
#   ANTHROPIC_API_KEY   – API key for Claude AI (required for --ai-fix)
#   AI_MODEL            – Claude model to use (default: "claude-sonnet-4-20250514")
# ──────────────────────────────────────────────────────────────────────────────

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.yml"

DOCKER_BIN="${DOCKER_BIN:-docker}"
MVN_BIN="${MVN_BIN:-mvn}"
# Default Maven CLI flags: skip tests, javadoc, and source-jar generation.
# All three are unnecessary during a deployment build and add ~10-20% overhead.
MVN_OPTS="${MVN_OPTS:--DskipTests -Dmaven.javadoc.skip=true -Dmaven.source.skip=true}"
MVN_THREADS="${MVN_THREADS:-1C}"          # parallel Maven threads (1 per CPU core)
BUILDKIT_ENABLED="${BUILDKIT_ENABLED:-1}" # Docker BuildKit (1 = enabled)

# Pre-allocate JVM heap for Maven to avoid GC pressure during large builds.
# TieredCompilation level-1 gives fast JIT with minimal warm-up overhead.
# Override by setting MAVEN_OPTS before calling the script.
export MAVEN_OPTS="${MAVEN_OPTS:--Xmx2g -Xms512m -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Djava.awt.headless=true}"

# Activate Docker BuildKit globally – inherited by all docker build sub-processes
export DOCKER_BUILDKIT="${BUILDKIT_ENABLED}"
export COMPOSE_DOCKER_CLI_BUILD="${BUILDKIT_ENABLED}"

# ── AI Configuration ─────────────────────────────────────────────────────────
ANTHROPIC_API_KEY="${ANTHROPIC_API_KEY:-}"
AI_MODEL="${AI_MODEL:-claude-sonnet-4-20250514}"
AI_MAX_RETRIES=3
AI_FIX_ENABLED=""
AI_LOG_DIR="${SCRIPT_DIR}/.ai-fix-logs"

read -r -a DOCKER_CMD <<< "${DOCKER_BIN}"
read -r -a MVN_CMD <<< "${MVN_BIN}"

# ══════════════════════════════════════════════════════════════════════════════
# AI-ASSISTED BUILD ERROR RESOLUTION (Claude API)
# ══════════════════════════════════════════════════════════════════════════════

# Calls the Claude API with a prompt and returns the response text.
# Usage: ai_response="$(call_claude "system prompt" "user prompt")"
call_claude() {
  local system_prompt="$1"
  local user_prompt="$2"
  local request_body response

  # Build JSON request body safely via jq
  request_body="$(jq -n \
    --arg model "${AI_MODEL}" \
    --arg system "${system_prompt}" \
    --arg user "${user_prompt}" \
    '{
      model: $model,
      max_tokens: 8192,
      system: $system,
      messages: [{role: "user", content: $user}]
    }' 2>/dev/null || true)"

  if [[ -z "${request_body}" ]]; then
    echo ""
    return 1
  fi

  # Show a spinner while waiting for the AI response
  local response_file="${AI_LOG_DIR}/.claude_response_tmp"
  set +e
  curl -sf --max-time 30 \
    -H "x-api-key: ${ANTHROPIC_API_KEY}" \
    -H "anthropic-version: 2023-06-01" \
    -H "content-type: application/json" \
    -d "${request_body}" \
    "https://api.anthropic.com/v1/messages" \
    -o "${response_file}" 2>/dev/null &
  local curl_pid=$!

  # Spinner while waiting
  local spin_chars='⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏'
  local i=0
  while kill -0 "${curl_pid}" 2>/dev/null; do
    printf "\r  ${spin_chars:i++%${#spin_chars}:1} Waiting for Claude AI response..." >&2
    sleep 0.3
  done
  printf "\r                                                    \r" >&2

  wait "${curl_pid}"
  local curl_rc=$?
  set -e

  if [[ ${curl_rc} -ne 0 || ! -f "${response_file}" ]]; then
    rm -f "${response_file}"
    echo ""
    return 1
  fi

  response="$(cat "${response_file}" 2>/dev/null || true)"
  rm -f "${response_file}"

  if [[ -z "${response}" ]]; then
    echo ""
    return 1
  fi

  # Check for API errors
  local error_type
  error_type="$(echo "${response}" | jq -r '.error.type // empty' 2>/dev/null || true)"
  if [[ -n "${error_type}" ]]; then
    local error_msg
    error_msg="$(echo "${response}" | jq -r '.error.message // empty' 2>/dev/null || true)"
    printf "  API Error: %s - %s\n" "${error_type}" "${error_msg}" >&2
    echo ""
    return 1
  fi

  # Extract the text content from the response
  local text
  text="$(echo "${response}" | jq -r '.content[0].text // empty' 2>/dev/null || true)"
  echo "${text}"
}

# ── Build error classification ────────────────────────────────────────────────
# Returns "CATEGORY|user-facing suggestion" for a given error output.
# Used both in the AI path (as a hint to Claude) and the non-AI path (display only).
classify_build_error() {
  local error_output="$1"
  if echo "${error_output}" | grep -qiE "OutOfMemoryError|GC overhead|heap space|Cannot allocate memory"; then
    echo "OOM|Increase JVM/Docker memory. Try --stop-for-build yes or set MVN_OPTS='-DskipTests -Xmx2g'"
  elif echo "${error_output}" | grep -qiE "cannot find symbol|does not exist|package.*not found|ClassNotFoundException"; then
    echo "MAVEN_COMPILE|Compilation error — check imports. Ensure tlc-components is built first (remove --skip-components)."
  elif echo "${error_output}" | grep -qiE "Could not resolve|Artifact.*not found|Failed to.*download|Connection refused.*maven"; then
    echo "MAVEN_DEPENDENCY|Dependency resolution failed. Check network or Maven Central availability."
  elif echo "${error_output}" | grep -qiE "COPY failed|no such file or directory|failed to solve"; then
    echo "DOCKER_BUILD|Docker build failed — ensure JARs exist from the Maven build step. Run with --build-mode full."
  elif echo "${error_output}" | grep -qiE "authentication required|401 Unauthorized|403 Forbidden|invalid credentials"; then
    echo "AUTH|Authentication error — verify Docker registry or SSH credentials."
  elif echo "${error_output}" | grep -qiE "connection refused|network unreachable|timeout|no route to host|name resolution"; then
    echo "NETWORK|Network error — check connectivity to Maven Central, Docker Hub, or the remote server."
  elif echo "${error_output}" | grep -qiE "no space left|disk quota|ENOSPC"; then
    echo "DISK|Disk full — run: docker system prune -af --volumes"
  elif echo "${error_output}" | grep -qiE "permission denied|Operation not permitted|EPERM"; then
    echo "PERMISSION|Permission denied — run: sudo usermod -aG docker \$USER  then log out/in."
  elif echo "${error_output}" | grep -qiE "ssh: connect|Host key verification failed|Connection timed out.*ssh"; then
    echo "SSH|SSH connection failed — verify SSH_HOST, SSH_USER, and SSH_KEY_PATH in the server env file."
  elif echo "${error_output}" | grep -qiE "BUILD FAILURE|COMPILATION ERROR"; then
    echo "MAVEN_BUILD|Maven build failure — check the output above for the root cause."
  else
    echo "UNKNOWN|"
  fi
}

# Analyzes a build error and returns a JSON object with fix instructions.
# The AI returns structured output: { "diagnosis": "...", "fixes": [ { "file": "...", "action": "replace|insert|create", "search": "...", "replace": "..." } ], "commands": ["..."] }
ai_analyze_build_error() {
  local build_step="$1"
  local build_dir="$2"
  local error_output="$3"
  local context_files="$4"

  # Pre-classify and display for immediate user feedback
  local _cls _cat _sug
  _cls="$(classify_build_error "${error_output}")"
  _cat="${_cls%%|*}"; _sug="${_cls##*|}"
  if [[ "${_cat}" != "UNKNOWN" ]]; then
    printf "\n${YELLOW}  ┌─ Error Classification ──────────────────────────────────────────┐${NC}\n"
    printf   "${YELLOW}  │${NC}  Category  : ${BOLD}%s${NC}\n" "${_cat}"
    [[ -n "${_sug}" ]] && printf "${YELLOW}  │${NC}  Fix hint  : %s\n" "${_sug}"
    printf   "${YELLOW}  └────────────────────────────────────────────────────────────────┘${NC}\n\n"
  fi

  local system_prompt
  system_prompt="You are an expert Java/Maven/Docker build engineer for the TWOZO CRM platform.
Your job is to diagnose build failures and provide EXACT fixes.

The project structure:
- tlc-parent: Parent POM
- tlc-components: Shared component libraries (notification, entity, commons, etc.)
- tlc-components/components-starter: Spring Boot starter for components
- tlc-crm-server: CRM application server with submodules (core, notifications, workflow, etc.)
- tlc-iam: Identity and access management server
- tlc-storage-server: File storage server
- tlc-email-service: Email service
- tlc-social: Social media integration

Error category already detected: ${_cat}
${_sug:+Fix hint: ${_sug}}

IMPORTANT RULES:
1. Return ONLY valid JSON. No markdown, no explanation outside JSON.
2. For file fixes, provide the EXACT text to search and replace.
3. Include enough context in 'search' to make it unique (3-5 lines).
4. If the fix requires running a command (e.g., installing a dependency), include it in 'commands'.
5. If you cannot determine the fix, set 'fixable' to false and explain in 'diagnosis'.
6. Include 'user_message' with a developer-friendly explanation.
7. Include 'manual_steps' with actionable steps if the fix cannot be applied automatically.

Response format:
{
  \"fixable\": true|false,
  \"diagnosis\": \"Brief explanation of the root cause\",
  \"user_message\": \"Developer-friendly explanation of what went wrong\",
  \"fixes\": [
    {
      \"file\": \"/absolute/path/to/file.java\",
      \"action\": \"replace\",
      \"search\": \"exact text to find (3-5 lines with context)\",
      \"replace\": \"exact replacement text\"
    }
  ],
  \"commands\": [\"optional shell commands to run after file fixes\"],
  \"manual_steps\": [\"step 1\", \"step 2\"]
}"

  local user_prompt
  user_prompt="BUILD STEP: ${build_step}
BUILD DIRECTORY: ${build_dir}
PROJECT ROOT: ${ROOT_DIR}

ERROR OUTPUT (last 150 lines):
${error_output}

CONTEXT FILES:
${context_files}

Analyze this build error and provide the fix as JSON."

  call_claude "${system_prompt}" "${user_prompt}"
}

# Applies fixes returned by the AI analysis.
# Returns 0 if fixes were applied, 1 if not fixable.
ai_apply_fixes() {
  local ai_response="$1"
  local log_file="$2"

  # Validate JSON
  if ! echo "${ai_response}" | jq empty 2>/dev/null; then
    warn "AI returned invalid JSON response"
    echo "${ai_response}" >> "${log_file}"
    return 1
  fi

  local fixable diagnosis
  fixable="$(echo "${ai_response}" | jq -r '.fixable // false' 2>/dev/null || echo "false")"
  diagnosis="$(echo "${ai_response}" | jq -r '.diagnosis // "Unknown"' 2>/dev/null || echo "Unknown")"
  local user_message
  user_message="$(echo "${ai_response}" | jq -r '.user_message // ""' 2>/dev/null || true)"

  printf "\n${CYAN}  ┌─ AI Diagnosis ──────────────────────────────────────────────────┐${NC}\n"
  printf   "${CYAN}  │${NC}  ${BOLD}%s${NC}\n" "${diagnosis}"
  [[ -n "${user_message}" ]] && printf "${CYAN}  │${NC}  ${DIM}%s${NC}\n" "${user_message}"
  printf   "${CYAN}  └────────────────────────────────────────────────────────────────┘${NC}\n\n"
  echo "Diagnosis: ${diagnosis}" >> "${log_file}"

  if [[ "${fixable}" != "true" ]]; then
    # Show manual steps when provided
    local manual_count
    manual_count="$(echo "${ai_response}" | jq '.manual_steps | length' 2>/dev/null || echo "0")"
    if [[ "${manual_count}" -gt 0 ]]; then
      warn "Manual steps required:"
      for (( _mi = 0; _mi < manual_count; _mi++ )); do
        local _step
        _step="$(echo "${ai_response}" | jq -r ".manual_steps[${_mi}]" 2>/dev/null || true)"
        [[ -n "${_step}" ]] && warn "  $(( _mi + 1 )). ${_step}"
      done
    fi
    warn "AI determined this error is not auto-fixable"
    return 1
  fi

  # Apply file fixes
  local fix_count
  fix_count="$(echo "${ai_response}" | jq '.fixes | length' 2>/dev/null || echo "0")"

  for ((i = 0; i < fix_count; i++)); do
    local file action search replace
    file="$(echo "${ai_response}" | jq -r ".fixes[${i}].file" 2>/dev/null || true)"
    action="$(echo "${ai_response}" | jq -r ".fixes[${i}].action" 2>/dev/null || true)"
    search="$(echo "${ai_response}" | jq -r ".fixes[${i}].search" 2>/dev/null || true)"
    replace="$(echo "${ai_response}" | jq -r ".fixes[${i}].replace" 2>/dev/null || true)"

    if [[ ! -f "${file}" && "${action}" != "create" ]]; then
      warn "  AI referenced non-existent file: ${file}"
      continue
    fi

    case "${action}" in
      replace)
        info "  Fixing: ${file}"
        if grep -qF "${search}" "${file}" 2>/dev/null; then
          # Use python for reliable multi-line replacement via temp files
          local search_file="${AI_LOG_DIR}/.search_tmp"
          local replace_file="${AI_LOG_DIR}/.replace_tmp"
          printf '%s' "${search}" > "${search_file}"
          printf '%s' "${replace}" > "${replace_file}"

          python3 - "${file}" "${search_file}" "${replace_file}" <<'PYEOF' && info "    ✓ Applied replacement" || warn "    ✗ Replacement failed"
import sys
target_file, search_file, replace_file = sys.argv[1], sys.argv[2], sys.argv[3]
with open(target_file, 'r') as f:
    content = f.read()
with open(search_file, 'r') as f:
    search = f.read()
with open(replace_file, 'r') as f:
    replace = f.read()
if search in content:
    content = content.replace(search, replace, 1)
    with open(target_file, 'w') as f:
        f.write(content)
    sys.exit(0)
else:
    sys.exit(1)
PYEOF
          rm -f "${search_file}" "${replace_file}"
        else
          warn "    ✗ Search text not found in file"
        fi
        echo "  [${action}] ${file}" >> "${log_file}"
        ;;
      insert)
        info "  Inserting into: ${file}"
        local position
        position="$(echo "${ai_response}" | jq -r ".fixes[${i}].position // \"end\"")"
        if [[ "${position}" == "beginning" ]]; then
          local tmp_file="${file}.ai_tmp"
          printf '%s\n' "${replace}" | cat - "${file}" > "${tmp_file}" && mv "${tmp_file}" "${file}"
        else
          printf '\n%s\n' "${replace}" >> "${file}"
        fi
        info "    ✓ Inserted content"
        echo "  [${action}] ${file}" >> "${log_file}"
        ;;
      create)
        info "  Creating: ${file}"
        mkdir -p "$(dirname "${file}")"
        printf '%s' "${replace}" > "${file}"
        info "    ✓ File created"
        echo "  [${action}] ${file}" >> "${log_file}"
        ;;
      *)
        warn "  Unknown action: ${action}"
        ;;
    esac
  done

  # Run any additional commands
  local cmd_count
  cmd_count="$(echo "${ai_response}" | jq '.commands | length' 2>/dev/null || echo "0")"

  for ((i = 0; i < cmd_count; i++)); do
    local cmd
    cmd="$(echo "${ai_response}" | jq -r ".commands[${i}]" 2>/dev/null || true)"
    [[ -z "${cmd}" || "${cmd}" == "null" ]] && continue
    info "  Running: ${cmd}"
    eval "${cmd}" >> "${log_file}" 2>&1 || warn "    Command failed: ${cmd}"
  done

  return 0
}

# Gathers context files relevant to a build error (pom.xml, referenced source files).
gather_error_context() {
  local build_dir="$1"
  local error_output="$2"
  local context=""

  # Always include the pom.xml of the build directory
  if [[ -f "${build_dir}/pom.xml" ]]; then
    context+="--- ${build_dir}/pom.xml ---"$'\n'
    context+="$(head -100 "${build_dir}/pom.xml")"$'\n\n'
  fi

  # Extract referenced Java files from error output and include their content
  local referenced_files
  referenced_files="$(echo "${error_output}" | grep -oP '\[ERROR\].*?(/[^\s:]+\.java)' | grep -oP '/[^\s:]+\.java' | sort -u | head -5 || true)"

  for ref_file in ${referenced_files}; do
    if [[ -f "${ref_file}" ]]; then
      context+="--- ${ref_file} ---"$'\n'
      context+="$(cat "${ref_file}")"$'\n\n'
    fi
  done

  # Also try to find files referenced by simple class name
  local error_classes
  error_classes="$(echo "${error_output}" | grep -oP 'cannot find symbol.*class \K\w+' | sort -u | head -3 || true)"

  for cls in ${error_classes}; do
    local found_file
    found_file="$(find "${ROOT_DIR}" -name "${cls}.java" -not -path "*/target/*" 2>/dev/null | head -1 || true)"
    if [[ -n "${found_file}" && -f "${found_file}" ]]; then
      context+="--- ${found_file} ---"$'\n'
      context+="$(cat "${found_file}")"$'\n\n'
    fi
  done

  echo "${context}"
}

# Main function: runs a build command with AI-assisted error recovery.
# Usage: ai_build "step name" "working_dir" command [args...]
# Returns the exit code of the final build attempt.
ai_build() {
  local step_name="$1"
  local work_dir="$2"
  shift 2
  local -a build_cmd=("$@")

  local attempt=0
  local build_log="${AI_LOG_DIR}/${step_name//[^a-zA-Z0-9_-]/_}.log"
  local build_output_file="${AI_LOG_DIR}/.build_output_tmp"
  local build_rc

  mkdir -p "${AI_LOG_DIR}"

  while true; do
    attempt=$((attempt + 1))

    # Run the build command, capture output to a FILE (not command substitution)
    # This avoids set -e issues inside subshells
    info "Build attempt ${attempt}/${AI_MAX_RETRIES} for: ${step_name}"
    build_rc=0

    # Use a temp file to capture output — disable errexit to catch failures
    set +e
    (cd "${work_dir}" && "${build_cmd[@]}") > "${build_output_file}" 2>&1
    build_rc=$?
    set -e

    if [[ ${build_rc} -eq 0 ]]; then
      # Build succeeded
      if [[ ${attempt} -gt 1 ]]; then
        info "✓ Build succeeded after AI fix (attempt ${attempt})"
      fi
      rm -f "${build_output_file}"
      return 0
    fi

    # Build failed — read the captured output
    local error_output
    error_output="$(cat "${build_output_file}" 2>/dev/null || true)"
    local last_lines
    last_lines="$(tail -150 "${build_output_file}" 2>/dev/null || true)"

    if [[ "${AI_FIX_ENABLED}" != "yes" ]]; then
      # Even without AI, show the classified error for actionable guidance
      local _fc _fcat _fsug
      _fc="$(classify_build_error "${last_lines}")"
      _fcat="${_fc%%|*}"; _fsug="${_fc##*|}"
      echo ""
      warn "══ Build Failed: ${step_name} ══"
      if [[ "${_fcat}" != "UNKNOWN" ]]; then
        warn "  Category : ${_fcat}"
        [[ -n "${_fsug}" ]] && warn "  Fix hint : ${_fsug}"
        echo ""
      fi
      tail -80 "${build_output_file}" 2>/dev/null || true
      rm -f "${build_output_file}"
      return 1
    fi

    if [[ ${attempt} -gt ${AI_MAX_RETRIES} ]]; then
      warn "═══════════════════════════════════════════════════════════════"
      warn "  AI auto-fix exhausted all ${AI_MAX_RETRIES} retries for: ${step_name}"
      warn "═══════════════════════════════════════════════════════════════"
      echo ""
      warn "Last error output:"
      tail -50 "${build_output_file}" 2>/dev/null || true
      echo ""
      warn "AI fix logs saved to: ${build_log}"

      # Show the AI's last diagnosis as a suggestion
      if [[ -f "${build_log}" ]]; then
        echo ""
        info "═══ AI Suggestions ═══"
        grep "^Diagnosis:" "${build_log}" | tail -1 || true
      fi
      rm -f "${build_output_file}"
      return 1
    fi

    # AI-assisted fix attempt
    echo ""
    banner "🤖 AI Fix – Attempt ${attempt}/${AI_MAX_RETRIES} for: ${step_name}"
    info "Analyzing build error with Claude AI (${AI_MODEL})..."

    local context_files
    context_files="$(gather_error_context "${work_dir}" "${last_lines}")"

    local ai_response
    ai_response="$(ai_analyze_build_error "${step_name}" "${work_dir}" "${last_lines}" "${context_files}")"

    if [[ -z "${ai_response}" ]]; then
      warn "AI did not return a response (API error or timeout)"
      echo "Attempt ${attempt}: No AI response" >> "${build_log}"
      echo ""
      warn "══════════════════════════════════════════════════════════════"
      warn "  BUILD FAILED – AI could not auto-fix (no API response)"
      warn "  Showing error output below for manual resolution:"
      warn "══════════════════════════════════════════════════════════════"
      echo ""
      tail -80 "${build_output_file}" 2>/dev/null || true
      echo ""
      rm -f "${build_output_file}"
      return 1
    fi

    echo "--- AI Response (attempt ${attempt}) ---" >> "${build_log}"
    echo "${ai_response}" >> "${build_log}"

    if ai_apply_fixes "${ai_response}" "${build_log}"; then
      info "Fixes applied. Retrying build..."
      echo ""
    else
      warn "AI could not provide a fix for this error"
      tail -80 "${build_output_file}" 2>/dev/null || true
      rm -f "${build_output_file}"
      return 1
    fi
  done
}

# ── Colours (no-op when stdout is not a terminal) ────────────────────────────
if [[ -t 1 ]]; then
  BOLD='\033[1m' GREEN='\033[0;32m' YELLOW='\033[0;33m' RED='\033[0;31m'
  CYAN='\033[0;36m' BLUE='\033[0;34m' DIM='\033[2m' NC='\033[0m'
else
  BOLD='' GREEN='' YELLOW='' RED='' CYAN='' BLUE='' DIM='' NC=''
fi

banner()    { printf "\n${BOLD}${GREEN}══════════════════════════════════════════════════════════════${NC}\n"; printf "${BOLD}${GREEN}  %s${NC}\n" "$*"; printf "${BOLD}${GREEN}══════════════════════════════════════════════════════════════${NC}\n\n"; }
info()      { printf "${GREEN}[INFO]${NC}  %s\n" "$*"; }
warn()      { printf "${YELLOW}[WARN]${NC}  %s\n" "$*" >&2; }
success()   { printf "${GREEN}  ✓${NC}  %s\n" "$*"; }
detail()    { printf "${DIM}      %s${NC}\n" "$*"; }

usage() {
  cat <<'USAGE'
Usage: application_runner.sh [--server local] [--build-mode <full|docker-only|skip>] [--modules <list|all>] [--clean <none|all>]

Options:
  --server, -s       Target deployment server.
                     "local"       → Deploy to local machine (default)
  --build-mode, -b   "full"        → Maven build + Docker image build
                     "docker-only" → Docker image build only (use existing jars)
                     "skip"        → No build, just (re)deploy containers
  --modules, -m      Comma-separated modules to build, or "all".
                     Available: crm-client, iam-server, file-server,
                                email-server, social, crm-server
  --clean            "all"  → Remove containers AND all docker volumes (clean slate)
                     "none" → Keep database & application data intact
   --tunnel           Enable ngrok tunnel for remote access via static URL.
                     Requires NGROK_DOMAIN set in config/env/app.env.
  --crm-sub          Comma-separated submodules inside tlc-crm-server, or "all".
                     Discovered dynamically from tlc-crm-server/pom.xml at runtime.
  --skip-components  Skip Maven build for tlc-components & components-starter.
                     "yes" → Skip (reuse previously built jars)
                     "no"  → Build them (default)
  --stop-for-build   Stop selected service containers before Maven build.
                     "yes" → Stop selected services (frees memory during build)
                     "no"  → Keep all services running during build (default)
  --parallel         Enable parallel Maven builds via -T flag.
                     "yes" → Parallel builds with -T MVN_THREADS (default: yes)
                     "no"  → Serial Maven builds
  --incremental      Skip the 'clean' phase in all Maven commands.
                     "yes" → Reuse previous target/ dirs (~40-60% faster, 2nd+ run)
                     "no"  → Full clean build every time (default; always safe)
                     Safe for Java-only changes. Use "no" after POM/branch changes.
  --maven-offline    Run Maven with --offline (skip remote repo checks).
                     "yes" → ~10-20% faster when all deps are in local .m2 cache
                     "no"  → Normal online mode (default)
  --yes, -y          Skip the confirmation prompt (for CI / non-interactive use).
  --ai-fix           Enable AI-assisted build error resolution using Claude.
                     "yes" → Auto-fix errors with Claude AI
                     "no"  → Disable (default if no API key)
                     Requires ANTHROPIC_API_KEY environment variable.
  --ai-max-retries   Max AI fix attempts per build step (default: 3).
  --help, -h         Show this help.

Environment:
  DOCKER_BIN          Docker binary        (default: docker)
  MVN_BIN             Maven binary         (default: mvn)
  MVN_OPTS            Extra Maven CLI flags (default: -DskipTests -Dmaven.javadoc.skip=true -Dmaven.source.skip=true)
  MAVEN_OPTS          JVM options for Maven (default: -Xmx2g -Xms512m -XX:+TieredCompilation -XX:TieredStopAtLevel=1)
  MVN_THREADS         Maven parallel threads (default: 1C = 1 per CPU core)
  BUILDKIT_ENABLED    Docker BuildKit on/off (default: 1 = enabled)
  ANTHROPIC_API_KEY   Claude API key       (required for --ai-fix yes)
   AI_MODEL            Claude model         (default: claude-sonnet-4-20250514)


Examples:
   ./application_runner.sh --server local --modules all --build-mode full --clean none
   ./application_runner.sh --server local -m crm-server -b docker-only --clean none
   ./application_runner.sh --server local --modules all -b full --clean none --crm-sub core,workflow
   ./application_runner.sh --build-mode skip --clean none --tunnel
   ./application_runner.sh -m crm-server -b full --clean none --skip-components yes
   ./application_runner.sh -m crm-server -b full --clean none --ai-fix yes
   ./application_runner.sh --modules all -b full --clean none --ai-fix yes --ai-max-retries 5
   ./application_runner.sh --modules all -b full --clean none --parallel yes --yes
   ./application_runner.sh -m crm-server -b full --clean none --incremental yes --maven-offline yes --yes
   ./application_runner.sh -m crm-server -b full --clean none --incremental yes --skip-components yes --yes
USAGE
}

die() { printf "${RED}[ERROR]${NC} %s\n" "$*" >&2; exit 1; }

trim() {
  local s="$1"
  s="${s#"${s%%[![:space:]]*}"}"
  s="${s%"${s##*[![:space:]]}"}"
  printf '%s' "$s"
}

normalize_module() {
  case "$1" in
    crm-client|client)     echo "crm-client"  ;;
    iam|iam-server)        echo "iam-server"   ;;
    file|file-server)      echo "file-server"  ;;
    email|email-server)    echo "email-server" ;;
    social)                echo "social"       ;;
    crm|crm-server)       echo "crm-server"   ;;
    *) return 1 ;;
  esac
}

# ── Maven project directories (for `mvn install`) ───────────────────────────
# Build order matters: parent → components → components-starter → service libs → starters

declare -A MAVEN_PROJECT_DIRS
MAVEN_PROJECT_DIRS["iam-server"]="${ROOT_DIR}/tlc-iam"
MAVEN_PROJECT_DIRS["file-server"]="${ROOT_DIR}/tlc-storage-server"
MAVEN_PROJECT_DIRS["email-server"]="${ROOT_DIR}/tlc-email-service"
MAVEN_PROJECT_DIRS["social"]="${ROOT_DIR}/tlc-social"
MAVEN_PROJECT_DIRS["crm-server"]="${ROOT_DIR}/tlc-crm-server"

# ── Maven starter directories (for `mvn install` – the fat-jar producers) ───
declare -A MAVEN_STARTER_DIRS
MAVEN_STARTER_DIRS["iam-server"]="${ROOT_DIR}/tlc-iam/iam-starter"
MAVEN_STARTER_DIRS["file-server"]="${ROOT_DIR}/tlc-storage-server/file-starter"
MAVEN_STARTER_DIRS["email-server"]="${ROOT_DIR}/tlc-email-service/email-starter"
MAVEN_STARTER_DIRS["social"]="${ROOT_DIR}/tlc-social/social-starter"
MAVEN_STARTER_DIRS["crm-server"]="${ROOT_DIR}/tlc-crm-server/server-starter"

# ── Docker image directories (where the Dockerfile lives) ───────────────────
declare -A DOCKER_DIRS
DOCKER_DIRS["crm-client"]="${ROOT_DIR}/crm-client"
DOCKER_DIRS["iam-server"]="${ROOT_DIR}/tlc-iam/iam-starter"
DOCKER_DIRS["file-server"]="${ROOT_DIR}/tlc-storage-server/file-starter"
DOCKER_DIRS["email-server"]="${ROOT_DIR}/tlc-email-service/email-starter"
DOCKER_DIRS["social"]="${ROOT_DIR}/tlc-social/social-starter"
DOCKER_DIRS["crm-server"]="${ROOT_DIR}/tlc-crm-server/server-starter"

declare -A DOCKER_IMAGES
DOCKER_IMAGES["crm-client"]="accountId/crm-client:latest"
DOCKER_IMAGES["iam-server"]="accountId/iam-server:latest"
DOCKER_IMAGES["file-server"]="accountId/file-server:latest"
DOCKER_IMAGES["email-server"]="accountId/email-server:latest"
DOCKER_IMAGES["social"]="accountId/social:latest"
DOCKER_IMAGES["crm-server"]="accountId/crm-server:latest"

# ── Docker compose service names (module → compose service) ─────────────────
declare -A COMPOSE_SERVICES
COMPOSE_SERVICES["crm-client"]="crm-client"
COMPOSE_SERVICES["iam-server"]="iam-server"
COMPOSE_SERVICES["file-server"]="file-server"
COMPOSE_SERVICES["email-server"]="email-server"
COMPOSE_SERVICES["social"]="social"
COMPOSE_SERVICES["crm-server"]="crm-server"

# ── Azure Container Registry (ACR) configuration ────────────────────────────
# Removed - local deployment only

# ── Parse CLI arguments ─────────────────────────────────────────────────────
SERVER_TARGET=""
MODULES_INPUT=""
BUILD_MODE=""
CLEAN_MODE=""
TUNNEL_ENABLED=false
TUNNEL_SET=false
CRM_SUB_INPUT=""
SKIP_COMPONENTS=""
STOP_FOR_BUILD=""
PARALLEL_BUILD=""
INCREMENTAL_BUILD=""   # skip 'clean' in Maven commands (reuse previous target/ dirs)
MAVEN_OFFLINE=""       # add --offline to Maven commands (skip remote repo checks)
SKIP_CONFIRM=false
CLI_MODE=false

while [[ $# -gt 0 ]]; do
  CLI_MODE=true
  case "$1" in
    --server|-s)
      SERVER_TARGET="${2:-}"
      shift 2 ;;
    --modules|-m)
      MODULES_INPUT="${2:-}"
      shift 2 ;;
    --build-mode|-b)
      BUILD_MODE="${2:-}"
      shift 2 ;;
    --clean)
      CLEAN_MODE="${2:-}"
      shift 2 ;;
    --tunnel)
      TUNNEL_ENABLED=true
      TUNNEL_SET=true
      shift ;;
    --crm-sub)
      CRM_SUB_INPUT="${2:-}"
      shift 2 ;;
    --skip-components)
      SKIP_COMPONENTS="${2:-}"
      shift 2 ;;
    --stop-for-build)
      STOP_FOR_BUILD="${2:-}"
      shift 2 ;;
    --parallel)
      PARALLEL_BUILD="${2:-}"
      shift 2 ;;
    --incremental)
      INCREMENTAL_BUILD="${2:-}"
      shift 2 ;;
    --maven-offline)
      MAVEN_OFFLINE="${2:-}"
      shift 2 ;;
    --yes|-y)
      SKIP_CONFIRM=true
      shift ;;
    --ai-fix)
      AI_FIX_ENABLED="${2:-}"
      shift 2 ;;
    --ai-max-retries)
      AI_MAX_RETRIES="${2:-3}"
      shift 2 ;;
    --help|-h)
      usage; exit 0 ;;
    *)
      die "Unknown argument: $1" ;;
  esac
done

# ── Available modules list ────────────────────────────────────────────────────
ALL_MODULES=("crm-client" "iam-server" "file-server" "email-server" "social" "crm-server")

# ── Discover CRM submodules dynamically from tlc-crm-server/pom.xml ──────────
# This ensures newly added modules are automatically available without script changes.
CRM_POM="${ROOT_DIR}/tlc-crm-server/pom.xml"
ALL_CRM_SUBMODULES=()
if [[ -f "${CRM_POM}" ]]; then
  while IFS= read -r line; do
    mod="$(echo "${line}" | sed -n 's/.*<module>\(.*\)<\/module>.*/\1/p' | xargs)"
    [[ -n "${mod}" && "${mod}" != "server-starter" ]] && ALL_CRM_SUBMODULES+=("${mod}")
  done < "${CRM_POM}"
fi
if [[ ${#ALL_CRM_SUBMODULES[@]} -eq 0 ]]; then
  warn "Could not discover CRM submodules from ${CRM_POM} – will build all by default"
fi

# ── Interactive prompts for missing inputs ───────────────────────────────────
# Prompt order: Server → Build Mode → Modules → Clean → Tunnel → Skip Components → AI Fix → CRM Submodules

# ── 1. Server selection ──────────────────────────────────────────────────────
# Default to local deployment (only option available)
SERVER_TARGET="${SERVER_TARGET:-local}"
SERVER_TARGETS=("local")

# ── 2. Build mode ───────────────────────────────────────────────────────────
if [[ -z "${BUILD_MODE}" ]]; then
  echo ""
  echo "Build mode?"
  echo "  1) full         → Maven build + Docker image build"
  echo "  2) docker-only  → Docker image build only (reuse existing jars)"
  echo "  3) skip         → No build, just (re)deploy containers"
  echo ""
  echo "Enter number [1]:"
  read -r BUILD_MODE_INPUT
  BUILD_MODE_INPUT="$(trim "${BUILD_MODE_INPUT}")"
  case "${BUILD_MODE_INPUT}" in
    1|"") BUILD_MODE="full" ;;
    2)    BUILD_MODE="docker-only" ;;
    3)    BUILD_MODE="skip" ;;
    *)    die "Invalid build mode selection: '${BUILD_MODE_INPUT}' – enter 1, 2 or 3" ;;
  esac
fi

# ── 3. Module selection ──────────────────────────────────────────────────────
if [[ -z "${MODULES_INPUT}" ]]; then
  echo ""
  echo "Select modules to build:"
  echo "  0) all"
  for i in "${!ALL_MODULES[@]}"; do
    printf "  %d) %s\n" "$((i + 1))" "${ALL_MODULES[$i]}"
  done
  echo ""
  echo "Enter numbers separated by commas (e.g. 1,3,5) [0 = all]:"
  read -r MODULES_INPUT
  MODULES_INPUT="$(trim "${MODULES_INPUT}")"
  [[ -z "${MODULES_INPUT}" || "${MODULES_INPUT}" == "0" ]] && MODULES_INPUT="all"

  # Convert number selections to module names
  if [[ "${MODULES_INPUT}" != "all" ]]; then
    IFS=',' read -r -a num_selections <<< "${MODULES_INPUT}"
    resolved_names=()
    for num in "${num_selections[@]}"; do
      num="$(trim "${num}")"
      [[ -z "${num}" ]] && continue
      # Validate it's a number
      if ! [[ "${num}" =~ ^[0-9]+$ ]]; then
        die "Invalid selection: '${num}' – please enter numbers only"
      fi
      if [[ "${num}" == "0" ]]; then
        MODULES_INPUT="all"
        resolved_names=()
        break
      fi
      local_idx=$((num - 1))
      if [[ ${local_idx} -lt 0 || ${local_idx} -ge ${#ALL_MODULES[@]} ]]; then
        die "Invalid module number: ${num} (valid range: 0-${#ALL_MODULES[@]})"
      fi
      resolved_names+=("${ALL_MODULES[$local_idx]}")
    done
    if [[ "${MODULES_INPUT}" != "all" ]]; then
      MODULES_INPUT="$(IFS=','; echo "${resolved_names[*]}")"
    fi
  fi
fi

# ── 4. Clean mode ───────────────────────────────────────────────────────────
if [[ -z "${CLEAN_MODE}" ]]; then
  echo ""
  echo "Clean docker data?"
  echo "  1) none → Keep database & application data (only redeploy code)"
  echo "  2) all  → Remove all volumes (clean slate – DB, opensearch, app data wiped)"
  echo ""
  echo "Enter number [1]:"
  read -r CLEAN_MODE_INPUT
  CLEAN_MODE_INPUT="$(trim "${CLEAN_MODE_INPUT}")"
  case "${CLEAN_MODE_INPUT}" in
    1|"") CLEAN_MODE="none" ;;
    2)    CLEAN_MODE="all" ;;
    *)    die "Invalid clean mode selection: '${CLEAN_MODE_INPUT}' – enter 1 or 2" ;;
  esac
fi

# ── Interactive prompt for tunnel (if not set via CLI) ───────────────────────
# Only prompt if the user did not supply --modules, --build-mode, --clean via CLI
# (i.e. they are in full interactive mode). Also skip for QA and integration
# deployments – ngrok tunnel is a local-only feature.
if [[ "${TUNNEL_SET}" == false && "${CLI_MODE}" == false && "${SERVER_TARGET}" == "local" ]]; then
  echo "Enable ngrok tunnel for remote access?"
  echo "  1) no  → Local access only (default)"
  echo "  2) yes → Expose the application via a public ngrok URL"
  echo ""
  echo "Enter number [1]:"
  read -r TUNNEL_INPUT
  TUNNEL_INPUT="$(trim "${TUNNEL_INPUT}")"
  case "${TUNNEL_INPUT}" in
    2)    TUNNEL_ENABLED=true ;;
    1|"") TUNNEL_ENABLED=false ;;
    *)    die "Invalid tunnel selection: '${TUNNEL_INPUT}' – enter 1 or 2" ;;
  esac
elif [[ "${TUNNEL_SET}" == false && "${SERVER_TARGET}" != "local" ]]; then
  # Tunnel is not applicable for remote environments – silently disable it.
  TUNNEL_ENABLED=false
fi

# ── Validate remaining inputs ───────────────────────────────────────────────
[[ "${BUILD_MODE}" =~ ^(full|docker-only|skip)$ ]]   || die "--build-mode must be 'full', 'docker-only' or 'skip'"
[[ "${CLEAN_MODE}" =~ ^(none|all)$ ]]                || die "--clean must be 'none' or 'all'"

# ── 6. Skip components (only relevant for full build) ────────────────────────
if [[ "${BUILD_MODE}" == "full" && -z "${SKIP_COMPONENTS}" ]]; then
  echo ""
  echo "Skip Maven build for tlc-components & components-starter?"
  echo "  1) no  → Build them (default)"
  echo "  2) yes → Skip (reuse previously built jars)"
  echo ""
  echo "Enter number [1]:"
  read -r SKIP_COMPONENTS_INPUT
  SKIP_COMPONENTS_INPUT="$(trim "${SKIP_COMPONENTS_INPUT}")"
  case "${SKIP_COMPONENTS_INPUT}" in
    2)    SKIP_COMPONENTS="yes" ;;
    1|"") SKIP_COMPONENTS="no"  ;;
    *)    die "Invalid skip-components selection: '${SKIP_COMPONENTS_INPUT}' – enter 1 or 2" ;;
  esac
elif [[ -z "${SKIP_COMPONENTS}" ]]; then
  SKIP_COMPONENTS="no"
fi
# Normalize the value
case "${SKIP_COMPONENTS}" in
  yes|y|Y|Yes|YES) SKIP_COMPONENTS="yes" ;;
  *)               SKIP_COMPONENTS="no"  ;;
esac

# ── 6b. Stop for build ────────────────────────────────────────────────────────
if [[ "${BUILD_MODE}" == "full" && -z "${STOP_FOR_BUILD}" ]]; then
  echo ""
  echo "Stop selected service containers before Maven build to free memory?"
  echo "  1) no  → Keep services running during build (default; recommended)"
  echo "  2) yes → Stop selected services before Maven build (frees more memory)"
  echo ""
  echo "Enter number [1]:"
  read -r SFB_INPUT
  SFB_INPUT="$(trim "${SFB_INPUT}")"
  case "${SFB_INPUT}" in
    2)    STOP_FOR_BUILD="yes" ;;
    1|"") STOP_FOR_BUILD="no"  ;;
    *)    die "Invalid stop-for-build selection: '${SFB_INPUT}' – enter 1 or 2" ;;
  esac
elif [[ -z "${STOP_FOR_BUILD}" ]]; then
  STOP_FOR_BUILD="no"
fi
case "${STOP_FOR_BUILD}" in
  yes|y|Y|Yes|YES) STOP_FOR_BUILD="yes" ;;
  *)               STOP_FOR_BUILD="no"  ;;
esac

# ── 6c. Parallel Maven builds ─────────────────────────────────────────────────
# Default to enabled; only prompt in full interactive mode if user hasn't set it
[[ -z "${PARALLEL_BUILD}" ]] && PARALLEL_BUILD="yes"
case "${PARALLEL_BUILD}" in
  yes|y|Y|Yes|YES) PARALLEL_BUILD="yes" ;;
  *)               PARALLEL_BUILD="no"  ;;
esac

# ── 6d. Incremental Maven builds (skip 'clean') ───────────────────────────────
if [[ "${BUILD_MODE}" == "full" && -z "${INCREMENTAL_BUILD}" ]]; then
  echo ""
  echo "Use incremental Maven build (skip 'clean', reuse previous target/ dirs)?"
  echo "  1) no  → Full clean build every time (default; safe, always correct)"
  echo "  2) yes → Skip 'clean' for ~40-60% faster builds on second+ runs"
  echo "           (safe when only Java source changed; use 'no' after POM changes)"
  echo ""
  echo "Enter number [1]:"
  read -r INC_INPUT
  INC_INPUT="$(trim "${INC_INPUT}")"
  case "${INC_INPUT}" in
    2)    INCREMENTAL_BUILD="yes" ;;
    1|"") INCREMENTAL_BUILD="no"  ;;
    *)    die "Invalid incremental selection: '${INC_INPUT}' – enter 1 or 2" ;;
  esac
elif [[ -z "${INCREMENTAL_BUILD}" ]]; then
  INCREMENTAL_BUILD="no"
fi
case "${INCREMENTAL_BUILD}" in
  yes|y|Y|Yes|YES) INCREMENTAL_BUILD="yes" ;;
  *)               INCREMENTAL_BUILD="no"  ;;
esac

# ── 6e. Maven offline mode ────────────────────────────────────────────────────
if [[ "${BUILD_MODE}" == "full" && -z "${MAVEN_OFFLINE}" ]]; then
  echo ""
  echo "Run Maven in offline mode (--offline)?"
  echo "  1) no  → Check remote repositories for updates (default)"
  echo "  2) yes → Skip all remote checks; use only local .m2 cache (~10-20% faster)"
  echo "           (use only when all dependencies are already cached locally)"
  echo ""
  echo "Enter number [1]:"
  read -r OFL_INPUT
  OFL_INPUT="$(trim "${OFL_INPUT}")"
  case "${OFL_INPUT}" in
    2)    MAVEN_OFFLINE="yes" ;;
    1|"") MAVEN_OFFLINE="no"  ;;
    *)    die "Invalid offline selection: '${OFL_INPUT}' – enter 1 or 2" ;;
  esac
elif [[ -z "${MAVEN_OFFLINE}" ]]; then
  MAVEN_OFFLINE="no"
fi
case "${MAVEN_OFFLINE}" in
  yes|y|Y|Yes|YES) MAVEN_OFFLINE="yes" ;;
  *)               MAVEN_OFFLINE="no"  ;;
esac

# ── 7. AI-assisted build fixing ───────────────────────────────────────────────
if [[ -z "${AI_FIX_ENABLED}" && "${BUILD_MODE}" == "full" ]]; then
  if [[ -n "${ANTHROPIC_API_KEY}" ]]; then
    echo "Enable AI-assisted build error resolution (Claude ${AI_MODEL})?"
    echo "  1) yes → Auto-fix build errors using AI (API key detected)"
    echo "  2) no  → Manual troubleshooting (default)"
    echo ""
    echo "Enter number [1]:"
    read -r AI_FIX_INPUT
    AI_FIX_INPUT="$(trim "${AI_FIX_INPUT}")"
    case "${AI_FIX_INPUT}" in
      1|"") AI_FIX_ENABLED="yes" ;;
      2)    AI_FIX_ENABLED="no"  ;;
      *)    AI_FIX_ENABLED="no"  ;;
    esac
  else
    AI_FIX_ENABLED="no"
  fi
elif [[ -z "${AI_FIX_ENABLED}" ]]; then
  AI_FIX_ENABLED="no"
fi
# Normalize
case "${AI_FIX_ENABLED}" in
  yes|y|Y|Yes|YES) AI_FIX_ENABLED="yes" ;;
  *)               AI_FIX_ENABLED="no"  ;;
esac
# Validate AI prerequisites
if [[ "${AI_FIX_ENABLED}" == "yes" ]]; then
  [[ -z "${ANTHROPIC_API_KEY}" ]] && die "ANTHROPIC_API_KEY environment variable is required for --ai-fix yes"
  command -v jq >/dev/null 2>&1 || die "'jq' is required for AI fix mode. Install with: sudo apt install jq"
  command -v python3 >/dev/null 2>&1 || die "'python3' is required for AI fix mode"

  # Quick API key validation (lightweight request)
  info "Validating Anthropic API key..."
  set +e
  _validate_resp="$(curl -sf --max-time 10 \
    -H "x-api-key: ${ANTHROPIC_API_KEY}" \
    -H "anthropic-version: 2023-06-01" \
    -H "content-type: application/json" \
    -d '{"model":"'"${AI_MODEL}"'","max_tokens":1,"messages":[{"role":"user","content":"hi"}]}' \
    "https://api.anthropic.com/v1/messages" 2>/dev/null)"
  _validate_rc=$?
  set -e

  if [[ ${_validate_rc} -ne 0 ]]; then
    warn "Cannot reach Anthropic API. AI fix will attempt calls during build failures anyway."
    warn "If AI fails to respond, build errors will be displayed for manual resolution."
  else
    _error_type="$(echo "${_validate_resp}" | jq -r '.error.type // empty' 2>/dev/null || true)"
    if [[ "${_error_type}" == "authentication_error" ]]; then
      warn "Invalid ANTHROPIC_API_KEY. AI fix will likely fail — please update your key."
      warn "Get a key from: https://console.anthropic.com/settings/keys"
    elif [[ "${_error_type}" == "invalid_request_error" && "$(echo "${_validate_resp}" | jq -r '.error.message // empty' 2>/dev/null || true)" == *"model"* ]]; then
      warn "Model '${AI_MODEL}' may not be available. Will attempt fallback."
    else
      info "API key validated ✓"
    fi
  fi
  info "AI fix mode enabled (model: ${AI_MODEL}, max retries: ${AI_MAX_RETRIES})"
fi

# ── Resolve module list (early, needed for CRM submodule prompt) ─────────────
modules=()
if [[ "${MODULES_INPUT}" == "all" ]]; then
  modules=("crm-client" "iam-server" "file-server" "email-server" "social" "crm-server")
else
  IFS=',' read -r -a raw_modules <<< "${MODULES_INPUT}"
  for raw in "${raw_modules[@]}"; do
    raw="$(trim "${raw}")"
    [[ -z "${raw}" ]] && continue
    normalized="$(normalize_module "${raw}")" || die "Unknown module: ${raw}"
    modules+=("${normalized}")
  done
fi
[[ ${#modules[@]} -eq 0 ]] && die "No modules selected"

# ── 8. CRM submodules (if crm-server selected & full build) ─────────────────
crm_submodules=()
crm_server_selected=false
for m in "${modules[@]}"; do
  [[ "${m}" == "crm-server" ]] && crm_server_selected=true && break
done

if [[ "${crm_server_selected}" == true && "${BUILD_MODE}" == "full" ]]; then
  if [[ -z "${CRM_SUB_INPUT}" ]]; then
    echo ""
    echo "Select CRM submodules to build (inside tlc-crm-server):"
    echo "  0) all"
    for i in "${!ALL_CRM_SUBMODULES[@]}"; do
      printf "  %d) %s\n" "$((i + 1))" "${ALL_CRM_SUBMODULES[$i]}"
    done
    echo ""
    echo "Enter numbers separated by commas (e.g. 2,4,12) [0 = all]:"
    read -r CRM_SUB_INPUT
    CRM_SUB_INPUT="$(trim "${CRM_SUB_INPUT}")"
    [[ -z "${CRM_SUB_INPUT}" || "${CRM_SUB_INPUT}" == "0" ]] && CRM_SUB_INPUT="all"

    # Convert number selections to submodule names
    if [[ "${CRM_SUB_INPUT}" != "all" ]]; then
      IFS=',' read -r -a num_selections <<< "${CRM_SUB_INPUT}"
      resolved_sub_names=()
      for num in "${num_selections[@]}"; do
        num="$(trim "${num}")"
        [[ -z "${num}" ]] && continue
        if ! [[ "${num}" =~ ^[0-9]+$ ]]; then
          die "Invalid selection: '${num}' – please enter numbers only"
        fi
        if [[ "${num}" == "0" ]]; then
          CRM_SUB_INPUT="all"
          resolved_sub_names=()
          break
        fi
        local_idx=$((num - 1))
        if [[ ${local_idx} -lt 0 || ${local_idx} -ge ${#ALL_CRM_SUBMODULES[@]} ]]; then
          die "Invalid submodule number: ${num} (valid range: 0-${#ALL_CRM_SUBMODULES[@]})"
        fi
        resolved_sub_names+=("${ALL_CRM_SUBMODULES[$local_idx]}")
      done
      if [[ "${CRM_SUB_INPUT}" != "all" ]]; then
        CRM_SUB_INPUT="$(IFS=','; echo "${resolved_sub_names[*]}")"
      fi
    fi
  fi

  # Resolve CRM submodule list
  if [[ "${CRM_SUB_INPUT}" == "all" ]]; then
    crm_submodules=()   # empty means build all (default mvn behaviour)
  else
    IFS=',' read -r -a raw_subs <<< "${CRM_SUB_INPUT}"
    for raw in "${raw_subs[@]}"; do
      raw="$(trim "${raw}")"
      [[ -z "${raw}" ]] && continue
      # Validate submodule name
      valid=false
      for valid_sub in "${ALL_CRM_SUBMODULES[@]}"; do
        [[ "${raw}" == "${valid_sub}" ]] && valid=true && break
      done
      [[ "${valid}" == false ]] && die "Unknown CRM submodule: ${raw}"
      crm_submodules+=("${raw}")
    done
  fi
fi

# ── Validate tunnel configuration ───────────────────────────────────────────
# NGROK_DOMAIN in app.env:
#   - If set   → ngrok uses that as a static/fixed URL  (e.g. twozo-dev.ngrok-free.app)
#   - If empty → ngrok assigns a random URL each time
ENV_FILE="${SCRIPT_DIR}/config/env/app.env"
NGROK_DOMAIN=""
if [[ "${TUNNEL_ENABLED}" == true ]]; then
  NGROK_DOMAIN="$(grep -E '^NGROK_DOMAIN=' "${ENV_FILE}" 2>/dev/null | cut -d'=' -f2- | xargs)"
  if [[ -n "${NGROK_DOMAIN}" ]]; then
    info "Tunnel mode : static domain → https://${NGROK_DOMAIN}"
  else
    warn "NGROK_DOMAIN is not set in ${ENV_FILE}"
    warn "Tunnel will use a random ngrok URL (changes every restart)"
    warn "To get a free static domain:"
    warn "  1. Go to https://dashboard.ngrok.com/domains"
    warn "  2. Click 'Create Domain' (free plan gives 1 static domain)"
    warn "  3. Set NGROK_DOMAIN=<your-domain> in ${ENV_FILE}"
    info "Tunnel mode : random URL"
  fi
fi

"${DOCKER_CMD[@]}" compose version >/dev/null 2>&1 || die "docker compose is not available. Set DOCKER_BIN if needed."

# Collect unique modules that require a Maven build (everything except crm-client)
maven_modules=()
for m in "${modules[@]}"; do
  [[ "${m}" != "crm-client" ]] && maven_modules+=("${m}")
done

# ── Step 0: Prune dangling Docker images ─────────────────────────────────────
# Dangling images are safe to remove at any time — they are untagged intermediate
# build layers.  We do NOT run "image prune -a" which would wipe infrastructure
# images (postgres, redis, kafka, …).  Volume removal is handled by --clean all.
info "Removing dangling images..."
"${DOCKER_CMD[@]}" image prune -f --filter "dangling=true" 2>/dev/null || true
if [[ "${CLEAN_MODE}" == "all" ]]; then
  info "Removing unused volumes (clean mode: all)..."
  "${DOCKER_CMD[@]}" volume prune -f 2>/dev/null || true
fi
info "Docker prune completed"
echo ""

# ── Summary and confirmation ──────────────────────────────────────────────────
banner "TWOZO CRM – Build & Deploy"
info "Server(s)       : $(IFS=', '; echo "${SERVER_TARGETS[*]}")"
info "Modules         : ${modules[*]}"
info "Build mode      : ${BUILD_MODE}"
info "Clean mode      : ${CLEAN_MODE}"
info "Tunnel          : ${TUNNEL_ENABLED}"
info "Skip components : ${SKIP_COMPONENTS}"
info "Stop for build  : ${STOP_FOR_BUILD}"
info "Parallel Maven  : ${PARALLEL_BUILD} (-T ${MVN_THREADS})"
info "Incremental     : ${INCREMENTAL_BUILD}"
info "Maven offline   : ${MAVEN_OFFLINE}"
info "Docker BuildKit : ${BUILDKIT_ENABLED}"
info "AI fix          : ${AI_FIX_ENABLED}"
if [[ ${#crm_submodules[@]} -gt 0 ]]; then
  info "CRM subs        : ${crm_submodules[*]}"
elif [[ "${crm_server_selected}" == true && "${BUILD_MODE}" == "full" ]]; then
  info "CRM subs        : all"
fi
if [[ "${CLEAN_MODE}" == "all" ]]; then
  echo ""
  warn "⚠  --clean all will WIPE ALL Docker volumes (DB and app data will be lost!)"
fi
echo ""

if [[ "${SKIP_CONFIRM}" != true ]]; then
  printf "  ${BOLD}Proceed with deployment? [Y/n]: ${NC}"
  read -r _confirm_input
  _confirm_input="$(trim "${_confirm_input}")"
  case "${_confirm_input}" in
    y|Y|yes|Yes|YES|"") ;;
    *) info "Deployment cancelled."; exit 0 ;;
  esac
  echo ""
fi

# ══════════════════════════════════════════════════════════════════════════════
# BUILD PHASE  (runs once locally)
# ══════════════════════════════════════════════════════════════════════════════
# ══════════════════════════════════════════════════════════════════════════════
# BUILD PHASE  (runs once locally; images are shared across all target servers)
# ══════════════════════════════════════════════════════════════════════════════

# ── Step 1: Selective stop — ONLY the services being rebuilt ─────────────────
# KEY OPTIMIZATION: Infrastructure containers (postgres, redis, kafka,
# opensearch, hazelcast, …) stay alive, saving 2-5 minutes per deployment.
# Only stop selected app services when --stop-for-build yes is explicitly set.
if [[ "${BUILD_MODE}" == "full" && "${STOP_FOR_BUILD}" == "yes" ]]; then
  banner "Stopping selected services to free memory for Maven build"
  for _sfb_module in "${modules[@]}"; do
    _sfb_svc="${COMPOSE_SERVICES[${_sfb_module}]}"
    "${DOCKER_CMD[@]}" compose -f "${COMPOSE_FILE}" stop "${_sfb_svc}" 2>/dev/null || true
    detail "Stopped: ${_sfb_svc}"
  done
  info "Selected services stopped"
  echo ""
fi

# ── Step 2: Maven builds ────────────────────────────────────────────────────
if [[ "${BUILD_MODE}" == "full" && ${#maven_modules[@]} -gt 0 ]]; then

  # ── Maven speed flags ───────────────────────────────────────────────────────
  # Parallel threads (~30-50% faster on multi-core)
  MVN_THREAD_FLAG=""
  [[ "${PARALLEL_BUILD}" == "yes" ]] && MVN_THREAD_FLAG="-T ${MVN_THREADS}"

  # Incremental: omit 'clean' to reuse previous target/ dirs (~40-60% faster
  # on repeated builds when only Java source changed; not safe after POM changes)
  CLEAN_FLAG="clean"
  [[ "${INCREMENTAL_BUILD}" == "yes" ]] && CLEAN_FLAG=""

  # Offline: skip remote repository checks (~10-20% faster when deps are cached)
  MVN_OFFLINE_FLAG=""
  [[ "${MAVEN_OFFLINE}" == "yes" ]] && MVN_OFFLINE_FLAG="--offline"

  if [[ "${INCREMENTAL_BUILD}" == "yes" ]]; then
    info "Incremental build: 'clean' phase skipped — reusing previous target/ directories"
  fi
  if [[ "${MAVEN_OFFLINE}" == "yes" ]]; then
    info "Offline mode: remote repository checks disabled"
  fi
  echo ""

  # Pre-build Docker cleanup: free disk space before the Maven build.
  # We intentionally omit "-a" so tagged infra images are NOT deleted.
  banner "Pre-build Docker Cleanup"
  if [[ "${CLEAN_MODE}" == "all" ]]; then
    info "Running docker system prune --volumes (clean mode: all, preserving tagged images)..."
    "${DOCKER_CMD[@]}" system prune --volumes -f 2>/dev/null || true
  else
    info "Running docker system prune (preserving tagged images and volumes)..."
    "${DOCKER_CMD[@]}" system prune -f 2>/dev/null || true
  fi
  info "Docker system prune completed"
  echo ""

  # 1a. Install parent POM
  # 'clean' is never useful here: -N means no submodule recursion and there
  # is no source to compile — just POM processing. Always skip clean.
  banner "Maven Install – tlc-parent"
  if [[ "${AI_FIX_ENABLED}" == "yes" ]]; then
    ai_build "tlc-parent" "${ROOT_DIR}/tlc-parent" "${MVN_CMD[@]}" install -N ${MVN_OFFLINE_FLAG} ${MVN_OPTS} || die "Build failed: tlc-parent"
  else
    ( cd "${ROOT_DIR}/tlc-parent" && "${MVN_CMD[@]}" install -N ${MVN_OFFLINE_FLAG} ${MVN_OPTS} )
  fi

  # 1b. Install component libraries (skipped if --skip-components yes)
  if [[ "${SKIP_COMPONENTS}" == "yes" ]]; then
    info "Skipping tlc-components Maven build (--skip-components yes)"
  else
    banner "Maven Install – tlc-components"
    if [[ "${AI_FIX_ENABLED}" == "yes" ]]; then
      ai_build "tlc-components" "${ROOT_DIR}/tlc-components" "${MVN_CMD[@]}" ${CLEAN_FLAG} install ${MVN_THREAD_FLAG} ${MVN_OFFLINE_FLAG} ${MVN_OPTS} || die "Build failed: tlc-components"
    else
      ( cd "${ROOT_DIR}/tlc-components" && "${MVN_CMD[@]}" ${CLEAN_FLAG} install ${MVN_THREAD_FLAG} ${MVN_OFFLINE_FLAG} ${MVN_OPTS} )
    fi
  fi

  # 1c. Install components-starter (skipped if --skip-components yes)
  if [[ "${SKIP_COMPONENTS}" == "yes" ]]; then
    info "Skipping components-starter Maven build (--skip-components yes)"
  else
    banner "Maven Install – components-starter"
    if [[ "${AI_FIX_ENABLED}" == "yes" ]]; then
      ai_build "components-starter" "${ROOT_DIR}/tlc-components/components-starter" "${MVN_CMD[@]}" ${CLEAN_FLAG} install ${MVN_THREAD_FLAG} ${MVN_OFFLINE_FLAG} ${MVN_OPTS} || die "Build failed: components-starter"
    else
      ( cd "${ROOT_DIR}/tlc-components/components-starter" && "${MVN_CMD[@]}" ${CLEAN_FLAG} install ${MVN_THREAD_FLAG} ${MVN_OFFLINE_FLAG} ${MVN_OPTS} )
    fi
  fi

  # 1d. Install each service's library modules
  for module in "${maven_modules[@]}"; do
    project_dir="${MAVEN_PROJECT_DIRS[${module}]}"
    [[ ! -d "${project_dir}" ]] && die "Maven project directory not found: ${project_dir}"

    if [[ "${module}" == "crm-server" && ${#crm_submodules[@]} -gt 0 ]]; then
      pl_list="$(IFS=','; echo "${crm_submodules[*]}")"
      banner "Maven Install – ${module} (submodules: ${pl_list})"
      if [[ "${AI_FIX_ENABLED}" == "yes" ]]; then
        ai_build "${module}-libs" "${project_dir}" "${MVN_CMD[@]}" ${CLEAN_FLAG} install -pl "${pl_list}" -am ${MVN_THREAD_FLAG} ${MVN_OFFLINE_FLAG} ${MVN_OPTS} || die "Build failed: ${module}-libs"
      else
        ( cd "${project_dir}" && "${MVN_CMD[@]}" ${CLEAN_FLAG} install -pl "${pl_list}" -am ${MVN_THREAD_FLAG} ${MVN_OFFLINE_FLAG} ${MVN_OPTS} )
      fi
    else
      banner "Maven Install – ${module} (libraries)"
      if [[ "${AI_FIX_ENABLED}" == "yes" ]]; then
        ai_build "${module}-libs" "${project_dir}" "${MVN_CMD[@]}" ${CLEAN_FLAG} install ${MVN_THREAD_FLAG} ${MVN_OFFLINE_FLAG} ${MVN_OPTS} || die "Build failed: ${module}-libs"
      else
        ( cd "${project_dir}" && "${MVN_CMD[@]}" ${CLEAN_FLAG} install ${MVN_THREAD_FLAG} ${MVN_OFFLINE_FLAG} ${MVN_OPTS} )
      fi
    fi
  done

  # 1e. Build each starter fat jar
  for module in "${maven_modules[@]}"; do
    starter_dir="${MAVEN_STARTER_DIRS[${module}]}"
    [[ ! -d "${starter_dir}" ]] && die "Maven starter directory not found: ${starter_dir}"

    banner "Maven Package – ${module} (starter fat-jar)"
    if [[ "${AI_FIX_ENABLED}" == "yes" ]]; then
      ai_build "${module}-starter" "${starter_dir}" "${MVN_CMD[@]}" ${CLEAN_FLAG} package ${MVN_THREAD_FLAG} ${MVN_OFFLINE_FLAG} ${MVN_OPTS} || die "Build failed: ${module}-starter"
    else
      ( cd "${starter_dir}" && "${MVN_CMD[@]}" ${CLEAN_FLAG} package ${MVN_THREAD_FLAG} ${MVN_OFFLINE_FLAG} ${MVN_OPTS} )
    fi
  done

  info "Maven builds completed successfully"
  echo ""
fi

# ── Step 3: Docker image builds ─────────────────────────────────────────────
if [[ "${BUILD_MODE}" != "skip" ]]; then
  for module in "${modules[@]}"; do
    docker_dir="${DOCKER_DIRS[${module}]}"
    docker_image="${DOCKER_IMAGES[${module}]}"
    [[ ! -d "${docker_dir}" ]] && die "Docker build directory not found: ${docker_dir}"

    banner "Docker Build – ${module} → ${docker_image}"
    if [[ "${AI_FIX_ENABLED}" == "yes" ]]; then
      ai_build "docker-${module}" "${docker_dir}" "${DOCKER_CMD[@]}" build --build-arg BUILDKIT_INLINE_CACHE=1 -t "${docker_image}" . || die "Build failed: docker-${module}"
    else
      ( cd "${docker_dir}" && "${DOCKER_CMD[@]}" build --build-arg BUILDKIT_INLINE_CACHE=1 -t "${docker_image}" . )
    fi
    sleep 1
  done
  info "Docker image builds completed successfully"
  echo ""
fi

# ══════════════════════════════════════════════════════════════════════════════
# DEPLOYMENT PHASE  (local deployment)
# ══════════════════════════════════════════════════════════════════════════════

# ── Local deployment ────────────────────────────────────────────────────────

# Step 4: Clean or prepare
  CLEAN_COMPOSE_ARGS=(-f "${COMPOSE_FILE}")
  [[ "${TUNNEL_ENABLED}" == true ]] && CLEAN_COMPOSE_ARGS+=(--profile tunnel)

  if [[ "${CLEAN_MODE}" == "all" ]]; then
    banner "Cleaning – removing containers and ALL volumes"
    "${DOCKER_CMD[@]}" compose "${CLEAN_COMPOSE_ARGS[@]}" down --remove-orphans --volumes
  else
    # Gracefully stop Kafka/Zookeeper to prevent ZooKeeper NodeExists errors
    banner "Preparing – graceful shutdown of Kafka/Zookeeper"
    info "Stopping Kafka and Zookeeper to clear ephemeral ZooKeeper nodes..."
    "${DOCKER_CMD[@]}" compose -f "${COMPOSE_FILE}" stop kafka zookeeper 2>/dev/null || true
    "${DOCKER_CMD[@]}" compose -f "${COMPOSE_FILE}" rm -f kafka zookeeper 2>/dev/null || true

    # KEY OPTIMIZATION: stop only the services being redeployed (not all infra)
    if [[ "${BUILD_MODE}" != "skip" ]]; then
      info "Stopping selected services for recreation (infrastructure stays alive)..."
      for _sel_mod in "${modules[@]}"; do
        _sel_svc="${COMPOSE_SERVICES[${_sel_mod}]}"
        "${DOCKER_CMD[@]}" compose -f "${COMPOSE_FILE}" stop "${_sel_svc}" 2>/dev/null || true
        detail "  Stopped: ${_sel_svc}"
      done
    fi
    info "Preparation complete"
  fi

  # Step 5: Deploy
  banner "Deploying – docker compose up"

  COMPOSE_ARGS=(-f "${COMPOSE_FILE}")
  [[ "${TUNNEL_ENABLED}" == true ]] && COMPOSE_ARGS+=(--profile tunnel)

  # Resolve which compose services were selected
  selected_services=()
  for m in "${modules[@]}"; do
    selected_services+=("${COMPOSE_SERVICES[${m}]}")
  done

  # Check if ALL modules were selected
  all_selected=true
  for m in "${ALL_MODULES[@]}"; do
    found=false
    for s in "${modules[@]}"; do
      [[ "${m}" == "${s}" ]] && found=true && break
    done
    if [[ "${found}" == false ]]; then
      all_selected=false
      break
    fi
  done

   if [[ "${all_selected}" == true ]]; then
     info "All modules selected → recreating all containers"
     "${DOCKER_CMD[@]}" compose "${COMPOSE_ARGS[@]}" up --detach --remove-orphans --force-recreate
   else
     # Partial selection → first ensure infrastructure & unselected services are running
     info "Ensuring infrastructure and unchanged services are running..."
     "${DOCKER_CMD[@]}" compose "${COMPOSE_ARGS[@]}" up --detach --remove-orphans

     # Then force-recreate ONLY the selected services
     info "Recreating only selected modules: ${selected_services[*]}"
     "${DOCKER_CMD[@]}" compose "${COMPOSE_ARGS[@]}" up --detach --force-recreate --no-deps "${selected_services[@]}"
   fi

   success "Deployment complete"

   # ── Ngrok tunnel URL (local only) ──────────────────────────────────────────
   if [[ "${TUNNEL_ENABLED}" == true ]]; then
     echo ""
     TUNNEL_URL=""
     if [[ -n "${NGROK_DOMAIN}" ]]; then
       TUNNEL_URL="https://${NGROK_DOMAIN}"
     else
       info "Waiting for ngrok tunnel to start..."
       for i in {1..30}; do
         CONTAINER_STATUS="$("${DOCKER_CMD[@]}" inspect -f '{{.State.Status}}' ngrok-tunnel 2>/dev/null || true)"
         if [[ "${CONTAINER_STATUS}" != "running" ]]; then
           info "  Attempt ${i}/30 – ngrok container status: ${CONTAINER_STATUS:-not found}"
           sleep 3; continue
         fi
         API_RESPONSE="$(curl -sf http://localhost:4040/api/tunnels 2>/dev/null || true)"
         if [[ -z "${API_RESPONSE}" ]]; then
           info "  Attempt ${i}/30 – ngrok API not ready yet..."
           sleep 3; continue
         fi
         TUNNEL_URL="$(echo "${API_RESPONSE}" \
           | python3 -c "import sys,json;d=json.load(sys.stdin);tunnels=d.get('tunnels',[]);print(tunnels[0]['public_url'] if tunnels else '')" 2>/dev/null || true)"
         if [[ -z "${TUNNEL_URL}" ]]; then
           TUNNEL_URL="$(echo "${API_RESPONSE}" \
             | grep -oP '"public_url"\s*:\s*"https://[^"]+' | head -1 \
             | sed 's/"public_url"\s*:\s*"//' || true)"
         fi
         if [[ -n "${TUNNEL_URL}" ]]; then break; fi
         info "  Attempt ${i}/30 – tunnel not established yet..."
         sleep 3
       done
     fi
     if [[ -n "${TUNNEL_URL}" ]]; then
       info "╔══════════════════════════════════════════════════════════════════╗"
       info "║  🌐 Ngrok Tunnel Active                                          ║"
       info "║  Public URL : ${TUNNEL_URL}"
       info "║  Dashboard  : http://localhost:4040                              ║"
       info "╚══════════════════════════════════════════════════════════════════╝"
     else
       warn "Ngrok tunnel may still be starting. Check: http://localhost:4040"
     fi
   fi


banner "Done"
info "Server(s)  : $(IFS=', '; echo "${SERVER_TARGETS[*]}")"
info "Build mode : ${BUILD_MODE}"
info "Clean mode : ${CLEAN_MODE}"
info "Modules    : ${modules[*]}"
info "Tunnel     : ${TUNNEL_ENABLED}"

