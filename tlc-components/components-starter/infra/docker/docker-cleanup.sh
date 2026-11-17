#!/bin/bash
docker-compose down --remove-orphans
docker volume rm docker_postgres_data docker_application_data docker_clickhouse_data
docker volume prune -f

# Enable below command to remove all dangling images
#docker rmi $(sudo docker images -f "dangling=true" -q)