#!/usr/bin/env bash
set -euo pipefail
for f in docs/diagrams/*.svg; do
  if ! grep -q 'rect width="100%" height="100%" fill="#FFFFFF"' "$f"; then
    if grep -q '<defs/>' "$f"; then
      perl -0 -i -pe 's@<defs/>@<defs/><rect width="100%" height="100%" fill="#FFFFFF"/>@' "$f"
    else
      perl -0 -i -pe 's@</defs>@</defs><rect width="100%" height="100%" fill="#FFFFFF"/>@' "$f"
    fi
  fi
done
