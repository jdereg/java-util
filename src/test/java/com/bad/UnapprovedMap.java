package com.bad;

import java.util.HashMap;

/**
 * Test-only class used to verify CompactMap properly rejects map types from disallowed packages.
 * This class exists solely to test the package validation logic in CompactMap.
 */
public class UnapprovedMap<K, V> extends HashMap<K, V> {
    // Empty implementation - only used for testing package validation
}