#!/usr/bin/env bash
# One-time setup of a fresh Amazon Linux 2023 EC2 instance for Cookify.
# Run ON the server (deploy.sh copies this folder to /tmp/cookify-infra):
#   sudo bash /tmp/cookify-infra/scripts/setup-server.sh
set -euo pipefail

INFRA_DIR="$(cd "$(dirname "$0")/.." && pwd)"

echo "==> Installing Java 21, Nginx, rsync"
dnf install -y java-21-amazon-corretto-headless nginx rsync

echo "==> Creating service user and folders"
id cookify &>/dev/null || useradd --system --home /opt/cookify --shell /sbin/nologin cookify
mkdir -p /opt/cookify /var/www/cookify
chown cookify:cookify /opt/cookify

echo "==> Adding 1 GB swap (t3.micro has 1 GB RAM)"
if [ ! -f /swapfile ]; then
  dd if=/dev/zero of=/swapfile bs=1M count=1024 status=none
  chmod 600 /swapfile
  mkswap /swapfile >/dev/null
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

echo "==> Nginx site"
cp "$INFRA_DIR/nginx/cookify.conf" /etc/nginx/conf.d/cookify.conf
nginx -t
systemctl enable --now nginx
systemctl reload nginx

echo "==> systemd service"
cp "$INFRA_DIR/systemd/cookify.service" /etc/systemd/system/cookify.service
systemctl daemon-reload
systemctl enable cookify

if [ ! -f /etc/cookify.env ]; then
  cp "$INFRA_DIR/aws/cookify.env.example" /etc/cookify.env
  chmod 600 /etc/cookify.env
  echo
  echo "!! Edit /etc/cookify.env (DB, JWT_SECRET, APP_URL, S3 bucket, SES sender) before the first deploy:"
  echo "   sudo nano /etc/cookify.env"
fi

echo "==> Done. Next: run infra/scripts/deploy.sh from your machine."
