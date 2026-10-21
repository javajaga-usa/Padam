#!/bin/bash
# Double-click on a Mac to start Padam. On Linux, run ./Start\ Padam.command
cd "$(dirname "$0")"
export PATH="/usr/local/bin:/opt/homebrew/bin:$PATH"
if ! command -v node >/dev/null 2>&1; then
  echo "Padam needs Node.js. Opening the download page..."
  echo "Install the LTS version, then double-click Start Padam again."
  (open https://nodejs.org/en/download || xdg-open https://nodejs.org/en/download) >/dev/null 2>&1
  read -r -p "Press Enter to close."
  exit 1
fi
node launcher.js "$@" || read -r -p "Press Enter to close."
