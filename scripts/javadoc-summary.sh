#!/usr/bin/env bash
set -euo pipefail

LOG_PATH="${1:-javadoc_strict.log}"
if [[ ! -f "$LOG_PATH" ]]; then
  echo "Log file not found: $LOG_PATH" >&2
  exit 1
fi

echo "Summarizing warnings in $LOG_PATH"
if ! grep -q ": warning:" "$LOG_PATH"; then
  echo "No warnings found."
  exit 0
fi

echo "Top files by warnings:"
grep -n ": warning:" "$LOG_PATH" | sed 's/: warning:.*$//' | sed -E 's/:([0-9]+):.*$//' \
  | sort | uniq -c | sort -nr | head -n 30

echo
echo "Example entries (first 100):"
grep -n ": warning:" "$LOG_PATH" | head -n 100

exit 0

