#!/bin/bash
readonly AWS_DEFAULTS="--endpoint=http://localhost:4566 --no-paginate "
bash docker-cleanup.sh
docker compose up --detach
sleep 5
echo "Creating S3 bucket..."
aws $AWS_DEFAULTS s3 mb s3://local-dev-bucket
echo "Available buckets"
aws $AWS_DEFAULTS s3 ls