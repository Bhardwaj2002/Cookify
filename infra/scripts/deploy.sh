#!/usr/bin/env bash
# Builds Cookify and deploys it to the EC2 instance. Run from the project root on your machine:
#   EC2_HOST=ec2-1-2-3-4.ap-south-1.compute.amazonaws.com SSH_KEY=~/keys/cookify.pem ./infra/scripts/deploy.sh
# Options:
#   RUN_TESTS=1   run the full test suite before deploying (needs local Postgres)
#   SETUP=1       also copy infra files and run the one-time server setup
set -euo pipefail

: "${EC2_HOST:?Set EC2_HOST to the public DNS name of the instance}"
: "${SSH_KEY:?Set SSH_KEY to the path of your .pem key}"
EC2_USER="${EC2_USER:-ec2-user}"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SSH=(ssh -i "$SSH_KEY" -o StrictHostKeyChecking=accept-new "$EC2_USER@$EC2_HOST")

cd "$ROOT/backend"
if [ "${RUN_TESTS:-0}" = "1" ]; then
  echo "==> Running tests"
  ./mvnw -q test
fi
echo "==> Building backend jar"
./mvnw -q -DskipTests package
JAR=$(ls target/cookify-backend-*.jar | grep -v plain | head -1)

if [ "${SETUP:-0}" = "1" ]; then
  echo "==> Copying infra files and running one-time setup"
  rsync -az -e "ssh -i $SSH_KEY" "$ROOT/infra/" "$EC2_USER@$EC2_HOST:/tmp/cookify-infra/"
  "${SSH[@]}" "sudo bash /tmp/cookify-infra/scripts/setup-server.sh"
fi

echo "==> Uploading backend and frontend"
scp -i "$SSH_KEY" "$JAR" "$EC2_USER@$EC2_HOST:/tmp/cookify.jar"
rsync -az --delete -e "ssh -i $SSH_KEY" "$ROOT/frontend/" "$EC2_USER@$EC2_HOST:/tmp/cookify-frontend/"

echo "==> Installing and restarting"
"${SSH[@]}" "sudo install -o cookify -g cookify -m 640 /tmp/cookify.jar /opt/cookify/cookify.jar \
  && sudo rsync -a --delete /tmp/cookify-frontend/ /var/www/cookify/ \
  && sudo systemctl restart cookify"

echo "==> Waiting for the API to come up"
for i in $(seq 1 30); do
  if "${SSH[@]}" "curl -sf http://127.0.0.1:8080/api/health >/dev/null"; then
    echo "==> Deployed. Health: $("${SSH[@]}" "curl -s http://127.0.0.1:8080/api/health")"
    exit 0
  fi
  sleep 3
done
echo "!! The API did not become healthy. Check logs: ssh … 'sudo journalctl -u cookify -n 100'"
exit 1
