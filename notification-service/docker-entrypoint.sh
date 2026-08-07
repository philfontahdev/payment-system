#!/bin/sh
set -e

# Load from Docker Swarm secret file only if it exists — preserves env vars set by docker-compose
load_secret() {
    local var="$1"
    local file="/run/secrets/$2"
    [ -f "$file" ] && export "$var"="$(cat "$file")"
    return 0
}

load_secret RABBITMQ_HOST     rabbitmq_host
load_secret RABBITMQ_USERNAME rabbitmq_username
load_secret RABBITMQ_PASSWORD rabbitmq_password
load_secret MAIL_HOST         mail_host
load_secret MAIL_USERNAME     mail_username
load_secret MAIL_PASSWORD     mail_password
load_secret MAIL_FROM         mail_from

exec java -jar app.jar
