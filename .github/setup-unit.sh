#!/bin/bash
set -euo pipefail

echo "Starting n8n services for testing..."
docker compose -f docker-compose-ci.yml up -d

echo "Docker services status:"
docker compose -f docker-compose-ci.yml ps

MAX_ATTEMPTS=180
ATTEMPT=0

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    if curl -sf http://localhost:5678/healthz > /dev/null 2>&1; then
        echo "n8n webserver is ready!"
        exit 0
    fi

    ATTEMPT=$((ATTEMPT + 1))
    echo "Waiting for n8n... ($ATTEMPT/$MAX_ATTEMPTS)"
    sleep 1
done

echo "n8n webserver did not become ready in time."
echo "---- n8n logs (last 200 lines) ----"
docker compose -f docker-compose-ci.yml logs --no-color n8n | tail -n 200
echo "----------------------------------"

exit 1
