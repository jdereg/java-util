#!/bin/bash
# Pull the @EnabledIf perf-test numbers out of a deploy log so they can be
# eyeballed at a glance, especially during a release where you want to spot
# any unusual numbers.
#
# Usage: scripts/extract-perf-results.sh path/to/deploy.log
set -e

LOG="${1:-}"
if [ -z "$LOG" ] || [ ! -f "$LOG" ]; then
    echo "Usage: $0 path/to/deploy.log" >&2
    exit 1
fi

echo "=== Performance test summary from ${LOG} ==="
echo

# 1. Find lines that look like perf test classes (heuristic: name contains
#    "Perf" or "Benchmark" or "Performance"), pull their Tests-run summaries
#    plus any "elapsed" time near them.
echo "--- Perf-test class summaries ---"
grep -E "(Perf|Benchmark|Performance).*Tests run:" "$LOG" | \
    sed -E 's/^\[INFO\] //' | \
    head -30
echo

# 2. Pull any line that prints elapsed milliseconds with a clear context (heuristic).
echo "--- Long-running test classes (>250ms elapsed) ---"
awk '
    /Tests run:.*elapsed:/ {
        # extract elapsed time
        if (match($0, /elapsed: ([0-9.]+) s/, arr)) {
            secs = arr[1] + 0
            if (secs > 0.25) {
                printf "  %6.2f s  %s\n", secs, $0
            }
        }
    }
' "$LOG" | sort -n | tail -20
echo

# 3. Total wall time
echo "--- Build totals ---"
grep -E "Total time:|BUILD SUCCESS|BUILD FAILURE" "$LOG" | tail -3
