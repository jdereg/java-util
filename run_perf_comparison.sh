#!/bin/bash

# Run MultiKeyMap Performance Comparison and extract results
# This script runs the performance test and provides a clean summary

echo "🚀 Running MultiKeyMap Performance Comparison..."
echo "================================================"

# Run the test and capture output
mvn test -Dtest=MultiKeyMapPerformanceComparisonTest -q > perf_results.log 2>&1

if [ $? -eq 0 ]; then
    echo "✅ Test completed successfully"
    echo ""
    
    # Extract performance results 
    echo "📊 PERFORMANCE RESULTS SUMMARY"
    echo "=============================="
    
    # Count wins and ties
    CEDAR_WINS=$(grep -c "Cedar++" perf_results.log)
    APACHE_WINS=$(grep -c "Apache++" perf_results.log)
    TIES=$(grep -c "Tie" perf_results.log)
    TOTAL=$((CEDAR_WINS + APACHE_WINS + TIES))
    
    echo "Total Test Configurations: $TOTAL"
    echo "Cedar Wins: $CEDAR_WINS"
    echo "Apache Wins: $APACHE_WINS" 
    echo "Ties: $TIES"
    echo ""
    
    # Calculate percentages
    if [ $TOTAL -gt 0 ]; then
        CEDAR_PCT=$(( (CEDAR_WINS * 100) / TOTAL ))
        APACHE_PCT=$(( (APACHE_WINS * 100) / TOTAL ))
        TIE_PCT=$(( (TIES * 100) / TOTAL ))
        
        echo "📈 Win Percentages:"
        echo "Cedar: ${CEDAR_PCT}%"
        echo "Apache: ${APACHE_PCT}%"
        echo "Ties: ${TIE_PCT}%"
        echo ""
    fi
    
    # Show detailed breakdown by key count and data size
    echo "🔍 DETAILED BREAKDOWN"
    echo "===================="
    echo ""
    
    # Extract all results with key count and entry count
    echo "Results by Configuration (Key Count | Entry Count | Winner):"
    echo "--------------------------------------------------------"
    grep -E "(keys.*entries.*Cedar\+\+|keys.*entries.*Apache\+\+|keys.*entries.*Tie)" perf_results.log | \
    sed 's/\[INFO\] //' | \
    sed 's/keys,/keys |/' | \
    sed 's/entries/entries |/' | \
    awk '{
        if ($NF == "Cedar++") print $1, $2, $3, $4, "-> CEDAR"
        else if ($NF == "Apache++") print $1, $2, $3, $4, "-> APACHE" 
        else if ($NF == "Tie") print $1, $2, $3, $4, "-> TIE"
    }' | sort -n -k1 -k3
    
    echo ""
    echo "🎯 PERFORMANCE PATTERNS"
    echo "======================"
    
    # Analyze patterns
    echo "Cedar dominates in:"
    grep -B2 "Cedar++" perf_results.log | grep "keys.*entries" | sed 's/\[INFO\]/  -/' | head -5
    
    echo ""
    echo "Apache dominates in:"
    grep -B2 "Apache++" perf_results.log | grep "keys.*entries" | sed 's/\[INFO\]/  -/' | head -5
    
    echo ""
    echo "📄 Full results saved to: perf_results.log"
    echo ""
    echo "To re-run this comparison: ./run_perf_comparison.sh"
    
else
    echo "❌ Test failed. Check the output above for errors."
    exit 1
fi