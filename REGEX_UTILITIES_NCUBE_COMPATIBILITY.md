# RegexUtilities Compatibility with n-cube

## Overview

This document analyzes n-cube's existing pattern caching infrastructure and provides guidance on RegexUtilities adoption.

## Executive Summary

**n-cube already has excellent pattern caching!** The `DecisionTable.java` class implements a sophisticated, thread-safe pattern caching system that is production-ready.

**RegexUtilities provides complementary value** through ReDoS protection and unified API across Cedar Software projects.

## Current Pattern Usage in n-cube

### Pattern Distribution

- **40+ static final Patterns** in `Regexes.java` interface - excellent centralized pattern repository
- **12 dynamic Pattern.compile() calls** throughout various classes
- **Existing caching infrastructure** in DecisionTable.java (lines 393-394, 2290-2306)

### n-cube's Excellent Existing Implementation

```java
// DecisionTable.java:393-394
private static final Map<String, Pattern> COMPILED_PATTERNS = new ConcurrentHashMap<>();
private static final Map<String, Pattern> COMPILED_PATTERNS_CI = new ConcurrentHashMap<>();
private static final Set<String> INVALID_PATTERNS = ConcurrentHashMap.newKeySet();

// DecisionTable.java:2290-2306
private static Pattern getCompiledPattern(String pattern, boolean caseInsensitive) {
    if (StringUtilities.isEmpty(pattern)) {
        return null;
    }

    if (INVALID_PATTERNS.contains(pattern)) {
        return null;  // Already known to be invalid
    }

    Map<String, Pattern> cache = caseInsensitive ? COMPILED_PATTERNS_CI : COMPILED_PATTERNS;

    return cache.computeIfAbsent(pattern, p -> {
        try {
            return caseInsensitive ?
                Pattern.compile(p, Pattern.CASE_INSENSITIVE) :
                Pattern.compile(p);
        } catch (PatternSyntaxException e) {
            // Cache the invalid pattern to avoid repeated compilation attempts
            INVALID_PATTERNS.add(p);
            LOG.warn("Invalid regex pattern '{}' - caching as invalid. Error: {}", p, e.getMessage());
            return null;
        }
    });
}
```

**Strengths of n-cube's implementation:**
- ✅ Thread-safe with ConcurrentHashMap
- ✅ Separate caches for case-sensitive and case-insensitive patterns
- ✅ Invalid pattern tracking to avoid repeated compilation errors
- ✅ Proper logging of invalid patterns
- ✅ Null-safe with empty string checks

## When to Use RegexUtilities in n-cube

### Scenario 1: ReDoS Protection (Primary Benefit)

**Use Case**: User-provided regex patterns in decision tables

```java
// Before (current n-cube code)
Pattern pattern = getCompiledPattern(userPattern, false);
boolean matches = pattern.matcher(input).matches();

// After (with ReDoS protection)
Pattern pattern = RegexUtilities.getCachedPattern(userPattern);
boolean matches = RegexUtilities.safeMatches(pattern, input);  // Timeout protected
```

**Why?** User-provided patterns could contain catastrophic backtracking that causes CPU exhaustion. RegexUtilities provides timeout protection.

### Scenario 2: New Code (Code Standardization)

**Use Case**: Writing new features that need regex operations

```java
// Recommended for new n-cube code
import com.cedarsoftware.util.RegexUtilities;

public class NewFeature {
    public boolean matchesPattern(String input, String userPattern) {
        Pattern pattern = RegexUtilities.getCachedPattern(userPattern);
        return RegexUtilities.safeMatches(pattern, input);
    }
}
```

**Why?** Consistency across Cedar Software projects and built-in security by default.

### Scenario 3: Keep Existing DecisionTable Caching

**Recommendation**: **DO NOT replace** DecisionTable's existing `getCompiledPattern()` method

**Why?**
1. n-cube's implementation is already excellent
2. It's battle-tested in production
3. It has domain-specific logging and error handling
4. Migration would add risk with minimal benefit

**Optional Enhancement**: Add ReDoS protection layer

```java
// Optional: Enhance existing getCompiledPattern with timeout protection
private static boolean safeMatches(Pattern pattern, String input) {
    // Delegate to RegexUtilities for timeout protection only
    return RegexUtilities.safeMatches(pattern, input);
}
```

## Dynamic Pattern.compile() Calls in n-cube

### Files with Dynamic Pattern Compilation

1. **FastDecisionTable.java:134** - Pattern compiled from user regex
2. **SqlPersister.java:172, 1833** - Search pattern compilation
3. **NCubeManager.java:1790** - Wildcard to regex conversion
4. **NCubeRuntime.java:665, 687, 907, 1068, 2832, 2883** - Various search and parsing patterns

### Recommended Approach

**High Priority** (user-facing patterns):
- FastDecisionTable.java - Could benefit from RegexUtilities for ReDoS protection
- NCubeRuntime search methods - User-provided search patterns should have timeout protection

**Low Priority** (internal patterns):
- SqlPersister.java - Internal patterns, low risk
- NCubeManager.java - Wildcard conversions, consider caching but low frequency

**Example Refactoring** (FastDecisionTable.java:134):

```java
// Before
this.pattern = Pattern.compile(regex);

// After (with ReDoS protection)
this.pattern = RegexUtilities.getCachedPattern(regex);

// Usage
boolean matches = RegexUtilities.safeMatches(this.pattern, input);
```

## Comparison: n-cube vs RegexUtilities

| Feature | n-cube DecisionTable | RegexUtilities |
|---------|---------------------|----------------|
| Pattern Caching | ✅ Excellent | ✅ Excellent |
| Thread Safety | ✅ ConcurrentHashMap | ✅ ConcurrentHashMap |
| Invalid Pattern Tracking | ✅ Yes | ✅ Yes |
| Case-Insensitive Cache | ✅ Separate cache | ✅ Separate cache |
| Flag-based Caching | ❌ No | ✅ Yes (any flags) |
| ReDoS Protection | ❌ No | ✅ Timeout-based |
| Cross-Project Standard | ❌ n-cube only | ✅ java-util, json-io, n-cube |
| Cache Statistics | ❌ No | ✅ Yes |
| Domain Logging | ✅ Custom logger | ❌ SecurityException only |

## Migration Strategy

### Phase 1: New Code Only (Recommended)

- Use RegexUtilities for all **new** n-cube features
- Keep existing DecisionTable implementation
- Benefits: Zero risk, gradual adoption, immediate value

### Phase 2: High-Risk Patterns (Optional)

- Identify user-facing regex operations
- Add RegexUtilities timeout protection layer
- Keep n-cube's caching infrastructure
- Benefits: ReDoS protection for user inputs

### Phase 3: Full Migration (Not Recommended)

- Replace DecisionTable caching with RegexUtilities
- **NOT recommended** due to high risk, low benefit
- Only consider if consolidating cache implementations across entire Cedar Software ecosystem

## Configuration for n-cube

If adopting RegexUtilities timeout protection:

```properties
# Enable ReDoS protection for user-provided patterns
cedarsoftware.security.enabled=true
cedarsoftware.regex.timeout.enabled=true
cedarsoftware.regex.timeout.milliseconds=5000  # 5 seconds for complex decision tables
```

## Testing Recommendations

If adopting RegexUtilities in n-cube:

1. **Test pattern caching works**:
   ```groovy
   void testPatternCachingWithRegexUtilities() {
       Pattern p1 = RegexUtilities.getCachedPattern("test.*")
       Pattern p2 = RegexUtilities.getCachedPattern("test.*")
       assert p1.is(p2)  // Same instance
   }
   ```

2. **Test ReDoS protection** (if enabled):
   ```groovy
   void testReDoSProtection() {
       System.setProperty("cedarsoftware.regex.timeout.milliseconds", "100")
       // This would timeout with catastrophic backtracking pattern
       // (test depends on JVM regex engine behavior)
   }
   ```

3. **Verify all existing n-cube tests still pass**

## Recommendations Summary

### ✅ DO:
- Use RegexUtilities for **new** n-cube features
- Add ReDoS protection for **user-facing** regex operations
- Keep existing DecisionTable caching infrastructure
- Use RegexUtilities in FastDecisionTable for user patterns

### ❌ DON'T:
- Replace DecisionTable's `getCompiledPattern()` method
- Migrate low-risk internal pattern compilations
- Add unnecessary complexity to proven production code

### 🤔 CONSIDER:
- Optional ReDoS protection layer for high-value operations
- Gradual adoption in new code only
- Cross-project standardization benefits vs migration risks

## Questions?

For questions about RegexUtilities and n-cube integration:
- Review RegexUtilities JavaDoc and tests
- Compare with DecisionTable.getCompiledPattern() implementation
- Consider ReDoS risk vs migration complexity for your specific use case
- Consult with n-cube maintainers before major refactoring

## Conclusion

**n-cube's existing pattern caching is production-ready and excellent.** RegexUtilities provides complementary value through:
1. ReDoS protection for user-facing patterns
2. Cross-project API standardization
3. Additional features (cache stats, multi-flag caching)

**Recommended approach**: Use RegexUtilities for new code and user-facing patterns, keep existing DecisionTable infrastructure.
