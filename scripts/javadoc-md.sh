#!/usr/bin/env bash
set -euo pipefail

mvn -s .mvn/settings.xml -DskipTests javadoc:javadoc

APIDOC="target/reports/apidocs/index.html"
OUT="docs/JAVADOC.md"
if [[ ! -f "$APIDOC" ]]; then
    echo "Javadoc not found: $APIDOC" >&2
    exit 1
fi

if ! command -v pandoc >/dev/null 2>&1; then
    echo "pandoc command is required but not found" >&2
    exit 1
fi

pandoc "$APIDOC" -f html -t gfm -o "$OUT"
echo "Markdown JavaDoc written to $OUT"
