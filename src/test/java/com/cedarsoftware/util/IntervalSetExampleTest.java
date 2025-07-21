package com.cedarsoftware.util;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Production Migration Verification Algorithm using IntervalSet for efficiency.
 * <p></p>
 * ALGORITHM OVERVIEW:
 * 1. Global State: IntervalSet<ZonedDateTime> verifiedTimeRanges tracks successfully verified time bands
 * 2. Backwards Verification: Start from the latest source timestamp, step backwards by band size
 * 3. Whole-Band Approach: Band passes = mark entire band verified, band fails = retry entire band
 * 4. Efficiency: Only verify unverified time gaps (IntervalSet prevents re-verification)
 * 5. Progressive Narrowing: Use large bands (30 min) initially, narrow to small bands (10 sec) around failures
 * <p></p>
 * PRODUCTION STRATEGY:
 * - Stage 1: 30-minute bands across entire dataset → identifies problem zones
 * - Stage 2: 1-minute bands around failed zones → narrows to problem areas  
 * - Stage 3: 10-second bands on remaining failures → isolates exact problem files
 * <p></p>
 * BENEFITS:
 * - 99.9% of data verified efficiently with large bands
 * - Surgical precision on actual problem records
 * - No re-verification of good data (IntervalSet efficiency)
 * - Scales from gigabytes to terabytes
 * <p></p>
 * USE CASE: Storage API migration between Elasticsearch/MarkLogic with namespace-level cutover.
 */
class IntervalSetExampleTest {
    
    // Helper method to convert IntervalSet to List
    private static <T extends Comparable<? super T>> List<IntervalSet.Interval<T>> toList(IntervalSet<T> set) {
        List<IntervalSet.Interval<T>> list = new ArrayList<>();
        for (IntervalSet.Interval<T> interval : set) {
            list.add(interval);
        }
        return list;
    }
    
    private static final Logger LOG = Logger.getLogger(IntervalSetExampleTest.class.getName());
    static {
        LoggingConfig.init();
    }
    
    // Simulated databases - using ZonedDateTime keys for time-based migration (TreeMap for chronological order)
    private TreeMap<ZonedDateTime, String> sourceDatabase;
    private TreeMap<ZonedDateTime, String> targetDatabase;
    
    // IntervalSet tracks which timestamp ranges have been successfully verified
    private IntervalSet<ZonedDateTime> verifiedTimeRanges;
    
    // Base timestamp for test data
    private ZonedDateTime baseTime;
    
    @BeforeEach
    void setUp() {
        sourceDatabase = new TreeMap<>();  // Chronological order like the real database
        targetDatabase = new TreeMap<>();  // Chronological order like the real database
        verifiedTimeRanges = new IntervalSet<>();
        // Base time for consistent test data - start at midnight UTC
        baseTime = ZonedDateTime.parse("2024-01-01T00:00:00Z");
    }
    
    
    /**
     * Helper method to get timestamp for second offset from base time
     */
    private ZonedDateTime second(int second) {
        return baseTime.plusSeconds(second);
    }
    
    /**
     * Background migration thread: Continuously copies source records to target
     * (In real implementation, this runs in a separate thread)
     */
    void backgroundMigration() {
        // Simulate background copying - copy all source records to target
        targetDatabase.putAll(sourceDatabase);
    }
    
    /**
     * Verify API: Check that records match between source and target for the given time range.
     * Key insight: Only checks unverified ranges (uses IntervalSet to skip already verified areas).
     * This can be called while background migration is still running.
     */
    boolean verify(ZonedDateTime startTime, ZonedDateTime endTime) {
        LOG.info(String.format("🔍 /verify(%s, %s) called...", startTime, endTime));
        
        List<ZonedDateTime[]> unverifiedRanges = findUnverifiedRanges(startTime, endTime);
        
        if (unverifiedRanges.isEmpty()) {
            LOG.info(String.format("✅ Range [%s - %s] already fully verified - skipping entirely", startTime, endTime));
            return true;
        }
        
        boolean allVerified = true;
        
        for (ZonedDateTime[] range : unverifiedRanges) {
            ZonedDateTime rangeStart = range[0];
            ZonedDateTime rangeEnd = range[1];
            
            LOG.info(String.format("   📋 Verifying unverified sub-range [%s - %s]", rangeStart, rangeEnd));
            
            // Verify records match (assumes background migration has copied them)
            boolean rangeValid = verifyRange(rangeStart, rangeEnd);
            
            if (rangeValid) {
                // Permanently mark this range as verified - IntervalSet merges overlapping ranges
                int intervalCountBefore = verifiedTimeRanges.size();
                verifiedTimeRanges.add(rangeStart, rangeEnd);
                int intervalCountAfter = verifiedTimeRanges.size();
                
                // Count actual records in this range
                long recordCount = sourceDatabase.keySet().stream()
                    .filter(timestamp -> !timestamp.isBefore(rangeStart) && !timestamp.isAfter(rangeEnd))
                    .count();
                LOG.info(String.format("   ✅ Verified [%s - %s] - %d records (permanently marked)", 
                    rangeStart, rangeEnd, recordCount));
                LOG.info(String.format("      📊 IntervalSet: %d → %d intervals %s", 
                    intervalCountBefore, intervalCountAfter, 
                    intervalCountAfter < intervalCountBefore ? "(MERGED!)" : 
                    intervalCountAfter > intervalCountBefore ? "(NEW)" : "(UNCHANGED)"));
                logCurrentIntervalBands();
            } else {
                LOG.info(String.format("   ❌ Verification failed for [%s - %s] - not marking as verified", rangeStart, rangeEnd));
                allVerified = false;
                // Don't add to verifiedTimeRanges - this range will be checked again next time
            }
        }
        
        return allVerified;
    }
    
    /**
     * Find gaps in verified ranges within the requested time range
     * This should ONLY work with time ranges, NOT source data
     */
    private List<ZonedDateTime[]> findUnverifiedRanges(ZonedDateTime startTime, ZonedDateTime endTime) {
        List<ZonedDateTime[]> unverifiedRanges = new ArrayList<>();
        
        if (verifiedTimeRanges.isEmpty()) {
            // No verified ranges yet - entire range is unverified
            unverifiedRanges.add(new ZonedDateTime[]{startTime, endTime});
            return unverifiedRanges;
        }
        
        // Get all verified intervals sorted by start time
        List<IntervalSet.Interval<ZonedDateTime>> intervals = new ArrayList<>(toList(verifiedTimeRanges));
        intervals.sort(Comparator.comparing(IntervalSet.Interval::getStart));
        
        ZonedDateTime currentPos = startTime;
        
        for (IntervalSet.Interval<ZonedDateTime> interval : intervals) {
            // Skip intervals that end before our range starts
            if (interval.getEnd().isBefore(startTime)) {
                continue;
            }
            
            // Stop if this interval starts after our range ends
            if (interval.getStart().isAfter(endTime)) {
                break;
            }
            
            // If there's a gap between current position and this interval, add it as unverified
            if (currentPos.isBefore(interval.getStart())) {
                ZonedDateTime gapEnd = interval.getStart().isBefore(endTime) ? interval.getStart() : endTime;
                unverifiedRanges.add(new ZonedDateTime[]{currentPos, gapEnd});
            }
            
            // Move past this verified interval
            if (interval.getEnd().isAfter(currentPos)) {
                currentPos = interval.getEnd().isAfter(endTime) ? endTime : interval.getEnd();
            }
        }
        
        // Check if there's a gap at the end
        if (currentPos.isBefore(endTime)) {
            unverifiedRanges.add(new ZonedDateTime[]{currentPos, endTime});
        }
        
        return unverifiedRanges;
    }
    
    /**
     * Check if all records in time range match between source and target
     */
    private boolean verifyRange(ZonedDateTime startTime, ZonedDateTime endTime) {
        // Find all source records in the time range
        List<ZonedDateTime> recordsInRange = sourceDatabase.keySet().stream()
            .filter(timestamp -> !timestamp.isBefore(startTime) && !timestamp.isAfter(endTime))
            .sorted()
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            
        // Verify each record matches between source and target
        for (ZonedDateTime timestamp : recordsInRange) {
            String sourceRecord = sourceDatabase.get(timestamp);
            String targetRecord = targetDatabase.get(timestamp);
            
            if (!Objects.equals(sourceRecord, targetRecord)) {
                return false;
            }
        }
        return true;
    }
    
    @ParameterizedTest
    @ValueSource(ints = {2, 62, 97})  // First band (0-9), middle band (60-69), last band (90-99)
    void testBackwardsVerificationWithFailureAndRecovery(int failRecord) {
        String testPattern = failRecord == 2 ? "FIRST BAND FAILURE" : 
                           failRecord == 62 ? "MIDDLE BAND FAILURE" : 
                           "LAST BAND FAILURE";
        LOG.info(String.format("🚀 %s PATTERN\n", testPattern));
        
        // Setup: 100 records at second boundaries
        for (int sec = 0; sec < 100; sec++) {
            sourceDatabase.put(second(sec), "record_" + sec);
        }
        backgroundMigration();
        
        // Corrupt the specified record 
        targetDatabase.put(second(failRecord), "record_" + failRecord + "_CORRUPTED");
        
        LOG.info(String.format("📊 Setup: 100 records (0-99 seconds), record %d corrupted\n", failRecord));
        
        // Backwards verification: Start from the latest timestamp, walk backwards 10 seconds at a time
        Duration chunkSize = Duration.ofSeconds(10);
        ZonedDateTime latestTime = second(99); // Latest record timestamp
        ZonedDateTime earliestTime = second(0); // Earliest record timestamp
        int round = 0;
        
        while (true) {
            round++;
            Duration lookback = chunkSize.multipliedBy(round);
            ZonedDateTime verifyStart = latestTime.minus(lookback);
            
            // Don't go before the earliest record
            if (verifyStart.isBefore(earliestTime)) {
                verifyStart = earliestTime;
            }
            
            LOG.info(String.format("🔍 Round %d: verify(%s to %s)", round, verifyStart, latestTime));
            boolean success = verify(verifyStart, latestTime);
            LOG.info(String.format("   Result: %s, IntervalSet has %d bands", success ? "SUCCESS" : "FAILED", verifiedTimeRanges.size()));
            
            // Stop if we've reached the beginning or hit failure
            if (verifyStart.equals(earliestTime) || !success) {
                LOG.info("🏁 Verification complete\n");
                break;
            }
            
            if (round >= 20) break; // Safety
        }
        
        LOG.info("📊 After initial verification with failure:");
        logCurrentIntervalBands();
        
        // Assert: Verification should have stopped when it hit the corrupted record
        if (failRecord >= 90) {
            // Last band failure - should have 0 intervals (fails immediately)
            assertEquals(0, verifiedTimeRanges.size(), "Should have 0 intervals when first band fails");
        } else {
            // Middle or first band failure - should have some verified intervals
            assertFalse(verifiedTimeRanges.isEmpty(), "Should have at least 1 interval before failure");
        }
        
        // Assert: The failed record should NOT be in any verified range (hole exists)
        assertFalse(verifiedTimeRanges.contains(second(failRecord)), 
            "Failed record " + failRecord + " should not be in verified ranges (hole exists)");
        
        // Fix the record
        LOG.info(String.format("\n🔧 FIXING record %d...", failRecord));
        targetDatabase.put(second(failRecord), "record_" + failRecord);
        
        // Retry verification - target the entire unverified range to enable merging
        LOG.info("\n🔄 RETRY verification - targeting the entire unverified gap...");
        // Target the full range to ensure everything gets verified
        ZonedDateTime failedChunkStart = second(0);
        ZonedDateTime failedChunkEnd = second(99); // Full range
        
        LOG.info(String.format("🔍 Recovery: verify(%s to %s) to fill the gap and trigger merging", failedChunkStart, failedChunkEnd));
        boolean success = verify(failedChunkStart, failedChunkEnd);
        LOG.info(String.format("   Result: %s, IntervalSet has %d bands", success ? "SUCCESS" : "FAILED", verifiedTimeRanges.size()));
        
        if (success) {
            LOG.info("🔍 Recovery successful!");
        }
        
        // Assert: After recovery, should have 1 merged interval covering everything
        assertEquals(1, verifiedTimeRanges.size(), "Should have 1 merged interval after recovery");
        
        // The failed record may or may not be verified depending on recovery success
        // Let's just verify the recovery attempt was made
        LOG.info(String.format("Record %d verified: %s", failRecord, verifiedTimeRanges.contains(second(failRecord))));
        
        LOG.info("📊 Final result:");
        logCurrentIntervalBands();
        
        // Verify all records are covered after recovery
        long verifiedRecords = sourceDatabase.keySet().stream()
            .filter(timestamp -> verifiedTimeRanges.contains(timestamp))
            .count();
        assertEquals(sourceDatabase.size(), verifiedRecords, "All records should be verified after recovery");
        assertTrue(verifiedTimeRanges.contains(second(0)));
        assertTrue(verifiedTimeRanges.contains(second(99)));
        
        LOG.info(String.format("✅ Pattern demonstrated: %d bands cover all records!", verifiedTimeRanges.size()));
    }

    /**
     * Log current IntervalSet bands for debugging enlargement behavior
     */
    void logCurrentIntervalBands() {
        List<IntervalSet.Interval<ZonedDateTime>> intervals = toList(verifiedTimeRanges);
        if (intervals.isEmpty()) {
            LOG.info("         Bands: (none)");
            return;
        }
        
        for (int i = 0; i < intervals.size(); i++) {
            IntervalSet.Interval<ZonedDateTime> interval = intervals.get(i);
            long recordCount = sourceDatabase.subMap(interval.getStart(), true, interval.getEnd(), true).size();
            long totalTimeSpan = Duration.between(interval.getStart(), interval.getEnd()).toMillis();
            LOG.info(String.format("         Band %d: [%s - %s] (%d records, %dms span)", 
                i + 1, interval.getStart(), interval.getEnd(), recordCount, totalTimeSpan));
        }
    }
    
}