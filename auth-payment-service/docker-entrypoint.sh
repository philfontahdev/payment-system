#!/bin/sh
set -e

# Load from Docker Swarm secret file only if it exists — preserves env vars set by docker-compose
load_secret() {
    local var="$1"
    local file="/run/secrets/$2"
    [ -f "$file" ] && export "$var"="$(cat "$file")"
}

load_secret DB_USERNAME       db_username
load_secret DB_PASSWORD       db_password
load_secret RABBITMQ_HOST     rabbitmq_host
load_secret RABBITMQ_USERNAME rabbitmq_username
load_secret RABBITMQ_PASSWORD rabbitmq_password
load_secret JWT_SECRET        jwt_secret
load_secret STRIPE_SECRET_KEY stripe_secret_key
load_secret STRIPE_WEBHOOK_SECRET stripe_webhook_secret

exec java -jar app.jar
