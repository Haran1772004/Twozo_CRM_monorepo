#!/bin/sh

# Generate timestamp
TIMESTAMP=$(date +%s)

# Original JVM dump
ORIG_FILE="/tmp/heapdumps/heapdump.hprof"

# Check if file exists
if [ ! -f "$ORIG_FILE" ]; then
  echo "Heapdump not found!"
  exit 1
fi

# Rename with timestamp
DUMP_FILE="/tmp/heapdump_${TIMESTAMP}.hprof"
mv "$ORIG_FILE" "$DUMP_FILE"

echo "Heapdump renamed to $DUMP_FILE. Compressing..."

# Compress
gzip -c "$DUMP_FILE" > "${DUMP_FILE}.gz"
rm -f "$DUMP_FILE"

echo "Compressed heapdump created: ${DUMP_FILE}.gz"

# Optional: Upload to Azure Blob
# az storage blob upload --file "${DUMP_FILE}.gz" --container-name ... etc
