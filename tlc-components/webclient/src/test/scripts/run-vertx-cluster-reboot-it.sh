#!/usr/bin/env bash
# Manual runner for VertxClientClusterRebootIT (no JUnit/Surefire).
#
# Prerequisite:
#   docker compose -f components-starter/infra/docker/docker-compose.yml up -d zookeeper
#
# Usage (from repo root):
#   ./webclient/src/test/scripts/run-vertx-cluster-reboot-it.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../../.." && pwd)"
cd "$ROOT"

OUT="$ROOT/webclient/target/it-classes"
mkdir -p "$OUT"

python3 - <<'PY'
from pathlib import Path
base = Path.home() / ".m2/repository"
jars = []

def add(p):
    p = Path(p)
    if p.exists():
        jars.append(str(p))
    else:
        raise SystemExit(f"Missing dependency jar: {p}")

add(base / "com/tlc/commons/5.34.2/commons-5.34.2.jar")
add(base / "com/tlc/scheduler/5.3.3/scheduler-5.3.3.jar")
add(base / "com/tlc/observability/1.0.4/observability-1.0.4.jar")
add(base / "org/osgi/osgi.core/8.0.0/osgi.core-8.0.0.jar")
for a in ["vertx-core", "vertx-web-client", "vertx-zookeeper", "vertx-hazelcast", "vertx-uri-template", "vertx-web-common"]:
    add(base / f"io/vertx/{a}/4.5.8/{a}-4.5.8.jar")
add(base / "org/slf4j/slf4j-api/2.0.16/slf4j-api-2.0.16.jar")
v = "2.17.2"
add(base / f"com/fasterxml/jackson/core/jackson-databind/{v}/jackson-databind-{v}.jar")
add(base / f"com/fasterxml/jackson/core/jackson-core/{v}/jackson-core-{v}.jar")
add(base / f"com/fasterxml/jackson/core/jackson-annotations/{v}/jackson-annotations-{v}.jar")
add(base / "com/hazelcast/hazelcast/5.3.7/hazelcast-5.3.7.jar")
for name in ["curator-framework", "curator-client", "curator-recipes"]:
    cands = sorted((base / "org/apache/curator" / name).glob("*/*.jar"))
    cands = [c for c in cands if "sources" not in c.name and "javadoc" not in c.name]
    add(cands[-1])
add(base / "org/apache/zookeeper/zookeeper/3.8.5/zookeeper-3.8.5.jar")
add(base / "org/apache/zookeeper/zookeeper-jute/3.8.5/zookeeper-jute-3.8.5.jar")
for art in ["netty-common", "netty-buffer", "netty-transport", "netty-handler", "netty-codec",
            "netty-codec-http", "netty-codec-http2", "netty-resolver", "netty-resolver-dns",
            "netty-transport-native-unix-common"]:
    cands = sorted((base / "io/netty" / art).glob("*/*.jar"))
    cands = [c for c in cands if "sources" not in c.name and "javadoc" not in c.name
             and "linux" not in c.name and "osx" not in c.name]
    if cands:
        jars.append(str(cands[-1]))
cands = sorted((base / "com/google/guava/guava").glob("*/*.jar"))
cands = [c for c in cands if "sources" not in c.name]
jars.append(str(cands[-1]))
for p in [
    "org/apache/yetus/audience-annotations/0.12.0/audience-annotations-0.12.0.jar",
    "commons-io/commons-io/2.11.0/commons-io-2.11.0.jar",
    "org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar",
]:
    add(base / p)

Path("/tmp/vertx-reboot-it.cp").write_text(":".join(dict.fromkeys(jars)))
print("classpath jars:", len(dict.fromkeys(jars)))
PY

CP="$(cat /tmp/vertx-reboot-it.cp)"
find webclient/src/main/java -name '*.java' ! -name 'WebClientAccess.java' ! -name 'BundleListener.java' > /tmp/wc-main.sources
javac --release 17 -cp "$CP" -d "$OUT" @/tmp/wc-main.sources
javac --release 17 -cp "$OUT:$CP" -d "$OUT" \
  webclient/src/test/java/com/tlc/web/client/internal/VertxClientClusterRebootIT.java

echo "Running VertxClientClusterRebootIT against localhost:2181 ..."
exec java -cp "$OUT:$CP" com.tlc.web.client.internal.VertxClientClusterRebootIT
