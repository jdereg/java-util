#!/bin/bash

# Complete Performance Comparison Runner and Analyzer
# This script runs the test and provides comprehensive results

echo "🚀 MultiKeyMap Performance Comparison"
echo "====================================="
echo ""

# Run the test
echo "Running performance test (this may take ~15-20 seconds)..."
mvn test -Dtest=MultiKeyMapPerformanceComparisonTest -q > perf_results.log 2>&1

if [ $? -ne 0 ]; then
    echo "❌ Test failed. Check perf_results.log for details."
    exit 1
fi

# Parse results
CEDAR_WINS=$(grep -c "Cedar++" perf_results.log)
APACHE_WINS=$(grep -c "Apache++" perf_results.log)
TIES=$(grep -c "Tie" perf_results.log)
TOTAL=$((CEDAR_WINS + APACHE_WINS + TIES))

echo "✅ Test completed successfully!"
echo ""
echo "🏆 FINAL RESULTS SUMMARY"
echo "========================"
echo "Total Test Configurations: $TOTAL"
echo "Cedar MultiKeyMap Wins: $CEDAR_WINS ($(( (CEDAR_WINS * 100) / TOTAL ))%)"
echo "Apache MultiKeyMap Wins: $APACHE_WINS ($(( (APACHE_WINS * 100) / TOTAL ))%)"
echo "Ties: $TIES ($(( (TIES * 100) / TOTAL ))%)"
echo ""

# Performance by key count
echo "📊 WINS BY KEY COUNT:"
echo "===================="
for keys in 1 2 3 4 5 6; do
    cedar=$(grep -B2 "Cedar++" perf_results.log | grep -c "$keys keys")
    apache=$(grep -B2 "Apache++" perf_results.log | grep -c "$keys keys")
    ties=$(grep -B2 "Tie" perf_results.log | grep -c "$keys keys")
    total=$((cedar + apache + ties))
    
    if [ $total -gt 0 ]; then
        printf "%d keys: Cedar=%d, Apache=%d, Ties=%d\n" $keys $cedar $apache $ties
    fi
done
echo ""

# Show some specific high-performance examples
echo "⚡ CEDAR'S BEST PERFORMANCES:"
echo "============================"
grep -B1 "Cedar++" perf_results.log | grep "keys.*entries" | head -3 | while read line; do
    config=$(echo "$line" | sed 's/\[INFO\] //' | awk -F'|' '{print $1}')
    echo "  $config"
done
echo ""

echo "⚡ APACHE'S BEST PERFORMANCES:"
echo "============================="
grep -B1 "Apache++" perf_results.log | grep "keys.*entries" | head -3 | while read line; do
    config=$(echo "$line" | sed 's/\[INFO\] //' | awk -F'|' '{print $1}')
    echo "  $config"
done
echo ""
