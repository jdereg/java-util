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

# Normalize each surefire per-class summary line to "<secs>\t<name>\t<counts>".
#
# Two notes, each of which on its own previously made this script report an
# empty perf summary during a release -- the exact moment it is supposed to be
# surfacing a regression:
#
#   - Only POSIX awk is used here: 2-argument match() plus RSTART/RLENGTH. The
#     3-argument match(str, re, arr) form is a GNU awk extension, and the BSD awk
#     that ships with macOS aborts on it ("syntax error"/"illegal statement"),
#     killing the whole section.
#   - The class name trails "-- in ", i.e. it appears AFTER "Tests run:" on the
#     same line, so a pattern requiring the name to come first can never match.
#     The name may also contain spaces (when @DisplayName is used), so take the
#     entire remainder of the line rather than a single token.
#
# The line prefix is [INFO], or [WARNING] when the class had skipped tests --
# both must be stripped or the WARNING lines are missed.
normalize() {
    awk '
        /Tests run:/ && /-- in / {
            line = $0
            sub(/^\[[A-Z]+\] +/, "", line)

            idx   = index(line, "-- in ")
            name  = substr(line, idx + 6)
            stats = substr(line, 1, idx - 1)

            secs = -1
            if (match(stats, /elapsed: [0-9.]+/)) {
                t = substr(stats, RSTART, RLENGTH)
                sub(/elapsed: /, "", t)
                secs = t + 0
            }

            counts = stats
            sub(/, Time elapsed:.*$/, "", counts)
            sub(/, elapsed:.*$/, "", counts)

            printf "%.3f\t%s\t%s\n", secs, name, counts
        }
    ' "$1"
}

ALL=$(normalize "$LOG")

if [ -z "$ALL" ]; then
    echo "!! No surefire per-class summaries found in ${LOG}" >&2
    echo "   Wrong file, or the build failed before running any tests?" >&2
    exit 1
fi

echo "=== Performance test summary from ${LOG} ==="
echo

# Heuristic: perf classes are named *Perf*/*Benchmark*/*Performance*. These are
# the @EnabledIf classes that only run under -DperformRelease=true.
echo "--- Perf-test class summaries ---"
PERF=$(echo "$ALL" | awk -F'\t' '$2 ~ /Perf|Benchmark|Performance/ { printf "%s\t%s\t%s\n", $1, $2, $3 }' | sort -rn)
if [ -z "$PERF" ]; then
    echo "  (none found -- was this built with -DperformRelease=true?)"
else
    echo "$PERF" | awk -F'\t' '{ printf "  %8.2f s  %s\n            %s\n", $1, $2, $3 }'
fi
echo

echo "--- Long-running test classes (>250ms elapsed) ---"
echo "$ALL" \
  | awk -F'\t' '$1 > 0.25 { printf "%s\t%s\n", $1, $2 }' \
  | sort -rn \
  | head -20 \
  | awk -F'\t' '{ printf "  %8.2f s  %s\n", $1, $2 }'
echo

echo "--- Totals ---"
# Report the LAST match of each: earlier "Total time:" lines come from forked
# plugin executions (e.g. a stray "Total time: 18 ms") and are noise, so a
# plain `tail -3` over the combined grep can show a plugin's time as the build's.
echo "$ALL" | awk -F'\t' '
    { classes++; if ($1 > 0) total += $1 }
    END { printf "  test classes: %d, summed class time: %.1f s\n", classes, total }
'
grep -E "^\[INFO\] Tests run: .*Skipped: [0-9]+$" "$LOG" | tail -1 | sed 's/^\[INFO\] /  /'
grep -E "BUILD SUCCESS|BUILD FAILURE" "$LOG" | tail -1 | sed 's/^\[INFO\] /  /'
grep -E "^\[INFO\] Total time:" "$LOG" | tail -1 | sed 's/^\[INFO\] /  /'
