#!/usr/bin/env bash
set -euo pipefail

echo "Generating VAPID keys..."
echo "Copy the output into your .env file:"
echo ""
npx web-push generate-vapid-keys
echo ""
echo "Set VAPID_SUBJECT=mailto:your@email.com in .env as well."
