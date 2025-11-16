# RegexUtilities Adoption Guide for json-io

## Overview

This document provides guidance for adopting `RegexUtilities` in the json-io project to improve performance and security.

## Current Pattern Usage in json-io

Analysis of json-io revealed **4 dynamic Pattern.compile() calls** with **no caching**, located in:

- `WriteOptionsBuilder.java` (lines 293, 733)
- `ReadOptionsBuilder.java` (lines 379, 1331)

### Example of Current Implementation

```java
// WriteOptionsBuilder.java:733
public WriteOptionsBuilder removeAliasTypeNamesMatching(String typeNamePattern) {
    String regex = StringUtilities.wildcardToRegexString(typeNamePattern);
    Pattern pattern = Pattern.compile(regex);  // ⚠️ NO CACHING
    options.aliasTypeNames.keySet().removeIf(key -> pattern.matcher(key).matches());
    return this;
}
```

### Performance Impact

Each call to `removeAliasTypeNamesMatching()` or similar methods recompiles the same pattern, which:
- Wastes CPU cycles on redundant pattern compilation
- Increases latency for frequently-used patterns
- Lacks ReDoS (Regular Expression Denial of Service) protection

## Recommended Changes

### Option 1: Use RegexUtilities for Caching and Security (Recommended)

```java
// WriteOptionsBuilder.java - Refactored with RegexUtilities
public WriteOptionsBuilder removeAliasTypeNamesMatching(String typeNamePattern) {
    String regex = StringUtilities.wildcardToRegexString(typeNamePattern);
    Pattern pattern = RegexUtilities.getCachedPattern(regex);  // ✅ CACHED + ReDoS PROTECTION
    if (pattern != null) {
        options.aliasTypeNames.keySet().removeIf(key ->
            RegexUtilities.safeMatches(pattern, key));
    }
    return this;
}
```

**Benefits:**
- **Performance**: Pattern is cached and reused across calls
- **Security**: Built-in ReDoS protection with configurable timeouts
- **Thread Safety**: ConcurrentHashMap-based caching is thread-safe
- **Invalid Pattern Handling**: Returns null for invalid patterns instead of throwing exceptions

### Option 2: Manual Caching (If RegexUtilities Not Available)

```java
private static final Map<String, Pattern> WILDCARD_PATTERN_CACHE = new ConcurrentHashMap<>();

public WriteOptionsBuilder removeAliasTypeNamesMatching(String typeNamePattern) {
    String regex = StringUtilities.wildcardToRegexString(typeNamePattern);
    Pattern pattern = WILDCARD_PATTERN_CACHE.computeIfAbsent(regex, r -> {
        try {
            return Pattern.compile(r);
        } catch (PatternSyntaxException e) {
            return null;  // Cache invalid patterns
        }
    });

    if (pattern != null) {
        options.aliasTypeNames.keySet().removeIf(key -> pattern.matcher(key).matches());
    }
    return this;
}
```

## All Affected Methods

The following methods should be updated in json-io:

### WriteOptionsBuilder.java

1. **removePermanentAliasTypeNamesMatching** (line 293)
2. **removeAliasTypeNamesMatching** (line 733)

### ReadOptionsBuilder.java

3. **removePermanentAliasTypeNamesMatching** (line 379)
4. **removeAliasTypeNamesMatching** (line 1331)

## Implementation Steps

1. **Add java-util dependency** (if not already present) - json-io already depends on java-util
2. **Import RegexUtilities**:
   ```java
   import com.cedarsoftware.util.RegexUtilities;
   ```
3. **Replace Pattern.compile() calls** with `RegexUtilities.getCachedPattern()`
4. **Replace pattern.matcher(x).matches()** calls with `RegexUtilities.safeMatches(pattern, x)`
5. **Add null checks** for invalid patterns
6. **Run full test suite** to ensure no regressions

## Expected Performance Improvements

- **First call**: Slight overhead due to cache insertion (~0.1ms)
- **Subsequent calls with same pattern**: ~100-1000x faster (no regex compilation)
- **Memory**: Minimal - patterns are cached with soft references
- **Security**: Protection against ReDoS attacks with configurable timeouts

## Testing Recommendations

After adopting RegexUtilities, add tests to verify:

1. **Pattern caching works**:
   ```java
   @Test
   void testPatternCaching() {
       WriteOptionsBuilder builder = new WriteOptionsBuilder();
       // Call twice with same pattern
       builder.removeAliasTypeNamesMatching("com.foo.*");
       builder.removeAliasTypeNamesMatching("com.foo.*");

       // Verify pattern was cached (check stats if available)
       Map<String, Object> stats = RegexUtilities.getPatternCacheStats();
       assertTrue((Integer) stats.get("totalCachedPatterns") > 0);
   }
   ```

2. **Invalid patterns handled gracefully**:
   ```java
   @Test
   void testInvalidPattern() {
       WriteOptionsBuilder builder = new WriteOptionsBuilder();
       // Should not throw exception, just skip the removal
       assertDoesNotThrow(() ->
           builder.removeAliasTypeNamesMatching("(unclosed"));
   }
   ```

3. **All existing tests still pass** (1800+ tests in json-io)

## Configuration (Optional)

Users can configure RegexUtilities behavior via system properties:

```properties
# Enable/disable all security features (default: true)
cedarsoftware.security.enabled=true

# Enable/disable regex timeout (default: true)
cedarsoftware.regex.timeout.enabled=true

# Timeout in milliseconds (default: 5000)
cedarsoftware.regex.timeout.milliseconds=5000
```

## Migration Checklist

- [ ] Review all 4 Pattern.compile() call sites in json-io
- [ ] Replace with RegexUtilities.getCachedPattern()
- [ ] Replace matcher().matches() with RegexUtilities.safeMatches()
- [ ] Add null pattern checks
- [ ] Run full test suite (mvn test)
- [ ] Verify performance improvements with benchmarks
- [ ] Update changelog.md with performance/security improvements
- [ ] Consider documenting in user guide if public-facing behavior changes

## Questions?

For questions or issues with RegexUtilities adoption, refer to:
- RegexUtilities JavaDoc
- RegexUtilitiesTest.java for usage examples
- DateUtilities.java for real-world usage patterns
