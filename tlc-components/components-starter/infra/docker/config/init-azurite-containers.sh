#!/bin/sh
# init-azurite-containers.sh
# Creates required blob containers in Azurite using its built-in Node.js environment.
# This script runs inside the Azurite container after it starts.
#
# Reads from env vars (supplied via app.env):
#   TLC-CONFIG-STORAGE-AZURE-ACCOUNTNAME
#   TLC-CONFIG-STORAGE-AZURE-ACCOUNTKEY
#   TLC-CONFIG-STORAGE-AZURE-AZURITE-ENDPOINT

set -e

ACCOUNT="${TLC_CONFIG_STORAGE_AZURE_ACCOUNTNAME:-devstoreaccount1}"
ACCOUNT_KEY="${TLC_CONFIG_STORAGE_AZURE_ACCOUNTKEY:-Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==}"
AZURITE_URL="${TLC_CONFIG_STORAGE_AZURE_AZURITE_ENDPOINT:-http://azurite:10000/devstoreaccount1}"
# Strip trailing account path if present (endpoint may include /devstoreaccount1)
AZURITE_URL=$(echo "$AZURITE_URL" | sed "s|/$ACCOUNT$||")

echo "Installing @azure/storage-blob..."
cd /tmp && npm init -y --silent > /dev/null 2>&1 && npm install @azure/storage-blob --silent > /dev/null 2>&1

echo "Creating Azurite blob containers..."
node -e "
const { BlobServiceClient } = require('@azure/storage-blob');
const connStr = 'DefaultEndpointsProtocol=http;AccountName=${ACCOUNT};AccountKey=${ACCOUNT_KEY};BlobEndpoint=${AZURITE_URL}/${ACCOUNT};';
(async () => {
  const client = BlobServiceClient.fromConnectionString(connStr);
  for (const name of ['twozo-external', 'twozo-internal']) {
    try {
      await client.getContainerClient(name).create();
      console.log('Created container:', name);
    } catch(e) {
      if (e.statusCode === 409) console.log('Container already exists:', name);
      else { console.error('Error creating', name, ':', e.message); process.exit(1); }
    }
  }
  console.log('All containers ready.');
})();
"

