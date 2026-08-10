#!/bin/sh
# Runs via nginx image /docker-entrypoint.d before nginx starts.
set -eu

: "${WATCHNEST_SERVER_NAME:?WATCHNEST_SERVER_NAME is required}"

envsubst '${WATCHNEST_SERVER_NAME}' \
  < /etc/nginx/templates-src/http.conf.template \
  > /etc/nginx/http.conf

envsubst '${WATCHNEST_SERVER_NAME}' \
  < /etc/nginx/templates-src/https.conf.template \
  > /etc/nginx/https.conf

CERT="/etc/letsencrypt/live/${WATCHNEST_SERVER_NAME}/fullchain.pem"
if [ -f "$CERT" ]; then
  cp /etc/nginx/https.conf /etc/nginx/conf.d/default.conf
else
  cp /etc/nginx/http.conf /etc/nginx/conf.d/default.conf
fi
