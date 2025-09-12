#!/usr/bin/env bash
set -euo pipefail

# Run strict Javadoc and save full log
mvn -s .mvn/settings.xml -DskipTests -Ddoclint=all -DfailOnWarnings=true javadoc:javadoc > javadoc_strict.log 2>&1 || true

# Print first 200 warnings
if [[ -f javadoc_strict.log ]]; then
  echo "First warnings (up to 200):"
  grep -n ": warning:" -m 200 javadoc_strict.log || true
  echo
  echo "Files with highest warning counts:"
  grep -n ": warning:" javadoc_strict.log | sed 's/: warning:.*$//' | sed -E 's/:([0-9]+):.*$//' \
    | sort | uniq -c | sort -nr | head -n 20 || true
fi

# Propagate javadoc exit code by re-running quietly
mvn -q -s .mvn/settings.xml -DskipTests -Ddoclint=all -DfailOnWarnings=true javadoc:javadoc >/dev/null 2>&1
exit $?
