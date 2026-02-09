### Revision History

#### 4.93.0 (Unreleased)
* **BUG FIX**: `ArrayConversions.enumSetToArray()` - `ArrayStoreException` when converting `EnumSet` to `Long[]`. Ordinal values (autoboxed `Integer`) were stored directly into `Long[]` arrays. Split `Integer`/`Long` branches and added explicit `(long)` cast.
* **BUG FIX**: `CollectionHandling.sizeOrDefault()` - `ArrayBlockingQueue` overflow when converting arrays with >16 elements. `sizeOrDefault()` only handled `Collection` sources, returning hardcoded 16 for arrays. Added `Array.getLength()` path for array sources.
* **BUG FIX**: `CollectionHandling` - `SynchronousQueue` and `DelayQueue` now throw descriptive `IllegalArgumentException` when used as conversion targets, instead of silently failing at runtime (`Queue full` / `ClassCastException`).
* **BUG FIX**: `MapConversions.toThrowable()` - `ClassCastException` when `causeMessage` map entry was a non-String value (e.g., `Integer`). Replaced unchecked `(String)` cast with `.toString()`.
* **BUG FIX**: `MapConversions.analyzeTarget()` - Failed to detect `UnmodifiableSortedMap`, `UnmodifiableNavigableMap`, and their synchronized/checked variants. `endsWith("$UnmodifiableMap")` missed inner class names like `$UnmodifiableSortedMap`. Changed to `contains("$Unmodifiable") && endsWith("Map")` (and similarly for Synchronized, Checked, Empty, Singleton).
* **BUG FIX**: `MapConversions.toColor()` - Packed `rgb` integer ignored explicit `alpha` map entry. When both `rgb` and `alpha` keys were present, alpha bits from the packed int (often 0) overwrote the explicit alpha value. Now decomposes RGB channels and applies explicit alpha separately.
* **BUG FIX**: `MapConversions.copyEntries()` - Caught all `Exception` types, silently swallowing errors during map entry copying. Narrowed to `ClassCastException | NullPointerException` (the only exceptions `Map.put()` throws for incompatible entries).
* **BUG FIX**: `NumberConversions.toYear()` - Used `shortValue()` instead of `intValue()`, silently truncating year values outside -32768..32767 (e.g., year 40000 became -25536).
* **BUG FIX**: `NumberConversions.floatingPointToBigInteger()` - `NaN` and `Infinity` caused `NumberFormatException` from `new BigInteger("NaN")`. Added explicit guard with descriptive `IllegalArgumentException`.
* **BUG FIX**: `NumberConversions.floatToString()` / `doubleToString()` - Negative zero (`-0.0`) collapsed to `"0"` because `== 0` is `true` for both `-0.0` and `+0.0`. Used `floatToRawIntBits` / `doubleToRawLongBits` to distinguish them, preserving IEEE 754 round-tripping.
* **BUG FIX**: `BigIntegerConversions.toUUID()` - Silently truncated values exceeding 128 bits instead of throwing. A 129-bit value would have its high bits dropped, producing a completely wrong UUID. Added overflow check.
* **BUG FIX**: `DateConversions` - `toString()`, `toYear()`, `toYearMonth()`, `toMonthDay()`, `toSqlDate()` called `date.toInstant()` directly, which throws `UnsupportedOperationException` on `java.sql.Date`. Replaced with the safe `toInstant(from, converter)` helper that wraps sql.Date via `new Date(date.getTime())`.
* **JAVA 25+ PREP**: `Unsafe` - Added `ReflectionFactory.newConstructorForSerialization()` as preferred strategy for constructor-bypassing instantiation, falling back to `sun.misc.Unsafe` only when ReflectionFactory is unavailable. Caches serialization constructors per class.
* **BUG FIX**: `RegexUtilities` - Pattern caches used `ConcurrentHashMap` which rejects null from `computeIfAbsent`, causing `NullPointerException` on every invalid regex pattern. Switched to `ConcurrentHashMapNullSafe`.
* **BUG FIX**: `GraphComparator.processPrimitiveArray()` - `NullPointerException` when both source and target array elements were null (missing null guard before `.equals()` call).
* **BUG FIX**: `CompactLinkedSet.isCaseInsensitive()` - Returned `true` instead of `false` (copy-paste error from case-insensitive variant).
* **BUG FIX**: `IOUtilities` - Native memory leak when handling deflate-encoded streams. Custom `Inflater` passed to `InflaterInputStream` was never `end()`ed. Switched to default constructor so `close()` manages the Inflater lifecycle.
* **BUG FIX**: `MultiKeyMap` - ThreadLocal array reuse in `getMultiKey()`/`containsMultiKey()` was unsafe under reentrant calls (e.g., custom `equals()`/`hashCode()` calling back into the map). Added reentrance detection that falls back to fresh allocation.
* **BUG FIX**: `IdentitySet.resize()` - Integer overflow when at `MAX_CAPACITY` (1<<30). `oldCapacity << 1` wrapped to `Integer.MIN_VALUE`, causing `NegativeArraySizeException`. Added capacity guard.
* **BUG FIX**: `SystemUtilities.getNetworkInterfaces()` - `NullPointerException` when `NetworkInterface.getNetworkInterfaces()` returns null (valid per JDK docs when no interfaces exist).
* **BUG FIX**: `AbstractConcurrentNullSafeMap.computeIfAbsent()` - Mapping function called twice when key was mapped to null, violating the Map contract (at most once). Replaced with single `compute()` call.
* **BUG FIX**: `CompactMap.equals()` - Null keys skipped during equality comparison in compact array state. Removed erroneous `entries[i] != null` guard; `areKeysEqual()` already handles nulls via `Objects.equals()`.
* **BUG FIX**: `TTLCache.get()` - Race condition where background purge thread could null `node.value` via `unlink()` between the expiry check and value read. Fixed by capturing value before expiry check.
* **BUG FIX**: `ReflectionUtils` - TOCTOU race in `getDangerousClassPatterns()`/`getSensitiveFieldPatterns()` where two separate volatile fields could be read in a mismatched state. Combined into single `SimpleImmutableEntry` volatile reference.
* **BUG FIX**: `UrlUtilities.validateContentLength()` - Used raw `maxContentLength` field instead of `getConfiguredMaxContentLength()`, ignoring system property override.
* **BUG FIX**: `FastByteArrayOutputStream.write()` - Integer overflow when `count + len` exceeded `Integer.MAX_VALUE` silently skipped buffer growth, leading to `ArrayIndexOutOfBoundsException`. Added overflow detection.
* **BUG FIX**: `TestUtil.assertContainsIgnoreCase()` - Used Java `assert` keyword which is a no-op without `-ea` JVM flag. Replaced with explicit `throw new AssertionError(...)`.
* **DOC FIX**: `ArrayUtilities.createArray()` - Javadoc incorrectly declared `@throws NullPointerException`; method actually returns null for null input.
* **TEST FIX**: `LRUCacheOverflowTest` - Stabilized flaky microbenchmark: runs 5 rounds and compares best times, raised threshold from 3x to 5x.
* **TEST FIX**: `ConverterArrayCollectionTest.testCachingPerformance` - Stabilized flaky performance test: added JIT warmup, best-of-3 rounds, raised threshold from 500ms to 750ms.
* **PERFORMANCE**: `FastReader.readUntil()` - Optimized hot inner loop (~37% faster): localized `position` field to stack variable to enable register allocation, precomputed loop bound to a single condition, and deferred `totalRead` counter update to loop exit.
* **PERFORMANCE**: `FastWriter.write(String)` - Optimized fast path: localized `nextChar` field to stack variable, removed redundant `len == 0` early return (handled naturally by fast path), cached `str.length()` to local, simplified bounds check.

#### 4.92.0 - 2026-02-08
* **PERFORMANCE**: `CaseInsensitiveMap` - Significant speedup by removing the global LRU cache and constructing `CaseInsensitiveString` wrappers directly on the heap. The cache's `ConcurrentHashMap` lookup + LRU bookkeeping cost more per call than simply creating the lightweight wrapper. CaseInsensitiveMap(HashMap) is now ~4x faster than TreeMap(CASE_INSENSITIVE_ORDER), up from ~9x slower.
* **BUG FIX**: `CaseInsensitiveString.equals()` - Fixed incorrect equality when hash code was 0 (skipped `equalsIgnoreCase()` check).
* **CLEANUP**: Deprecated `replaceCache()`, `resetCacheToDefault()`, and `setMaxCacheLengthString()` as no-ops. System properties `caseinsensitive.cache.size` and `caseinsensitive.max.string.length` no longer have any effect.

#### 4.91.0 - 2026-02-08
* **BUG FIX**: `DeepEquals` - Enum constants with class bodies misclassified as `TYPE_MISMATCH`
  * `Class.isEnum()` returns `false` for anonymous subclasses created by enum constants with bodies (e.g., `FOO { @Override ... }`)
  * Two different enum constants from the same enum with bodies bypassed the enum reference-equality check and fell through to the class-equality check, producing `TYPE_MISMATCH` instead of `VALUE_MISMATCH`
  * Fixed by using `instanceof Enum` instead of `Class.isEnum()`, which correctly handles anonymous enum subclasses
* **BUG FIX**: `DeepEquals` - Asymmetric simple-type check produced order-dependent error messages
  * Only `key1`'s type was checked for the simple-type fast path; when `key1` was a simple type (e.g., `String`) but `key2` was a complex type (e.g., `List`), the comparison entered the simple-type branch based on `key1` alone
  * `deepEquals("hello", list)` produced `VALUE_MISMATCH` while `deepEquals(list, "hello")` produced `COLLECTION_TYPE_MISMATCH` for the same comparison
  * Fixed by requiring both sides to be simple types before entering the fast path, falling through to container/class-equality checks for correct symmetric `TYPE_MISMATCH` reporting
* **PERFORMANCE**: `DeepEquals.decomposeOrderedCollection()` - Eliminated unnecessary `ArrayList` copy for Deque comparisons
  * When comparing two Deques (not Lists), both collections were copied to `ArrayList` (O(n) allocation + copy) just for index-based access
  * Fixed by using iterator-based forward traversal with an array buffer for Deques, while preserving direct indexed access for Lists
* **CLEANUP**: `DeepEquals` - Removed unused `SCALE_DOUBLE` and `SCALE_FLOAT` constants (dead code from prior refactoring)
* **CLEANUP**: `DeepEquals` - Fixed misleading comments in `nearlyEqual()` methods that incorrectly stated bitwise equality handles `+0.0 == -0.0` (it does not; the tolerance check handles it)
* **BUG FIX**: `MultiKeyMap` - Concurrent resize lost entries due to stripe lock / bucket mismatch
  * `getStripeIndex()` computed the stripe from `(spread(hash) & bucketMask) & STRIPE_MASK`, where `bucketMask` depends on the current table size. Between computing the stripe and acquiring the lock, a concurrent resize could replace `buckets` with a larger table. The `*NoLock` methods then re-read the new table and computed a bucket index that mapped to a **different stripe**, allowing two threads to modify the same bucket concurrently — a lost-update race that silently dropped entries
  * Fixed by making `getStripeIndex()` table-size-independent (`spread(hash) & STRIPE_MASK`), enforcing minimum capacity >= `STRIPE_COUNT` so same-bucket always means same-stripe, and using the locally captured table reference consistently in `putNoLock()` / `removeNoLock()`
* **BUG FIX**: `UrlUtilities.setCookies()` - Error message incorrectly said "AFTER" instead of "BEFORE" calling `connect()`
  * The `IllegalStateException` handler told users to call `setCookies()` *after* connecting, but cookies must be set *before* `connect()` — the opposite of what the message said
  * Fixed by changing "AFTER" to "BEFORE" in the error message
* **BUG FIX**: `UrlUtilities` - `NumberFormatException` on invalid system properties for max download size / max content length
  * Four methods (`getConfiguredMaxDownloadSize()`, `getConfiguredMaxContentLength()`, `getMaxDownloadSize()`, `getMaxContentLength()`) called `Long.parseLong()` / `Integer.parseInt()` without catching `NumberFormatException`
  * A non-numeric system property value (e.g., `urlutilities.max.download.size=abc`) crashed with an unhandled exception instead of falling back to the default
  * Fixed by catching `NumberFormatException` and falling through to the default value
* **BUG FIX**: `TTLCache.put()` - Always returned `null` instead of the previous value
  * `unlink(oldEntry.node)` sets `node.value = null` before the return value was read, so `put()` always returned `null` even when replacing an existing entry
  * Fixed by saving the old value before calling `unlink()`
* **BUG FIX**: `TTLCache.equals()` - Returned `false` incorrectly when expired-but-not-yet-purged entries existed
  * `equals()` delegated to `AbstractSet.equals()` which short-circuits on `entrySet().size()` mismatch; `size()` included expired entries but the iterator skipped them
  * Fixed by rewriting `equals()` as a single-pass comparison over non-expired entries, avoiding the size/iterator inconsistency entirely
* **BUG FIX**: `TrackingMap.remove(key, value)` / `replace(key, oldValue, newValue)` - Non-concurrent fallbacks returned `true` for absent keys when value was `null`
  * Both fallback paths used `Objects.equals(curValue, value)` without checking `containsKey()`, so `Objects.equals(null, null)` matched absent keys
  * `remove("absent", null)` incorrectly returned `true`; `replace("absent", null, "x")` returned `true` and **inserted a spurious entry**
  * Fixed by adding the `containsKey` guard matching the JDK's default `Map.remove(key, value)` and `Map.replace(key, old, new)` contracts
* **BUG FIX**: `StringUtilities.regionEqualsIgnoreCase()` - ASCII case folding matched non-letter characters
  * The condition `(c1 - 'A') <= 25` used signed arithmetic, so it was true for all chars from NUL (0) through 'Z' (90), not just 'A'-'Z'
  * This caused false positives for 6 non-letter character pairs (e.g., ';' matched '[', '@' matched '`') when comparing non-String CharSequences case-insensitively
  * Fixed by using explicit range check: `c1 >= 'A' && c1 <= 'Z'`
* **BUG FIX**: `StringUtilities.wildcardToRegexString()` - `+` not escaped in generated regex
  * The regex quantifier `+` was not in the escape list, so a wildcard like `"a+b"` produced regex `^a+b$` matching `"ab"`, `"aab"`, etc. instead of only the literal `"a+b"`
  * Fixed by adding `+` to the set of escaped regex metacharacters
* **BUG FIX**: `CollectionUtilities.listOf()` / `setOf()` - Null elements silently accepted despite documented NPE
  * Both methods documented `@throws NullPointerException` for null elements (matching `List.of()`/`Set.of()` contract), but `ArrayList.add(null)` and `LinkedHashSet.add(null)` do not throw
  * Fixed by adding explicit `Objects.requireNonNull()` checks in both methods
* **BUG FIX**: `ClassUtilities` - `CLASS_NOT_FOUND_SENTINEL` using `Void.class` masked legitimate `java.lang.Void` lookups
  * The "class not found" cache sentinel was `Void.class`, so the first `forName("java.lang.Void", cl)` call succeeded, but subsequent calls found `Void.class` in the cache, matched the sentinel, and incorrectly threw `ClassNotFoundException`
  * Fixed by replacing the sentinel with a private inner class (`ClassNotFoundSentinel`) that can never collide with any real class
* **BUG FIX**: `RegexUtilities.getRegexTimeoutMilliseconds()` - `NumberFormatException` on invalid system property
  * Unlike the boolean config methods which gracefully handle invalid input, this method threw an unhandled `NumberFormatException` if `cedarsoftware.regex.timeout.milliseconds` was set to a non-numeric value
  * Fixed by catching `NumberFormatException` and falling back to the default timeout (5000ms)
* **BUG FIX**: `ReflectionUtils.isTrustedCaller()` - Dangerous class security check was always bypassed
  * `isTrustedCaller()` found `ReflectionUtils` itself on the call stack, which always matched the `com.cedarsoftware.util.` trusted prefix
  * This made `isDangerousClass()` always return `false`, completely disabling the dangerous class security feature even when enabled
  * Fixed by skipping `ReflectionUtils` frames when walking the stack, so only actual external callers are evaluated
* **BUG FIX**: `MathUtilities` - `minimum()`/`maximum()` for `BigInteger` and `BigDecimal` did not null-check `values[0]` when array had 2+ elements
  * Calling e.g. `minimum(null, BigInteger.ONE)` produced a confusing `NullPointerException` from `BigInteger.compareTo()` instead of the friendly `IllegalArgumentException`
  * Fixed by moving the null check before the loop, which also eliminates the redundant `len == 1` special case
* **PERFORMANCE**: `ThreadedLRUCacheStrategy.computeIfAbsent()` - Eliminated unnecessary eviction checks on cache hits
  * `computeIfAbsent()` incremented `insertsSinceEviction` on every call, including cache hits
  * This triggered unnecessary eviction scans even when no new entry was added
  * Fixed by tracking whether the mapping function was invoked, only running eviction logic on actual inserts
* **CLEANUP**: `ThreadedLRUCacheStrategy` - Removed unused `softCap` field and `SOFT_CAP_RATIO` constant
  * The "Zone C probabilistic inline eviction" described in the Javadoc was never implemented
  * Updated Javadoc to accurately describe the actual eviction zones
* **BUG FIX**: `LockingLRUCacheStrategy.clear()` - Concurrent `get()` could corrupt the linked list after `clear()`
  * `clear()` reset the head/tail sentinels but did not null out removed nodes' `prev`/`next` links
  * A concurrent `get()` using `tryLock` could call `moveToHead()` on a node with stale links, splicing ghost nodes into the live list
  * This caused `entrySet()`, `containsValue()`, `hashCode()`, and `equals()` to return stale data
  * Fixed by walking the list and nulling each node's links before resetting the sentinels
* **BUG FIX**: `LoggingConfig.init()` - Missing `initialized` guard allowed repeated reconfiguration
  * The no-arg `init()` did not check the `initialized` flag, unlike `init(String)` which did
  * In test environments, every class with `static { LoggingConfig.init(); }` re-captured the full stack trace and overwrote formatters
  * User-configured formats set via `init(String)` could be silently overwritten by subsequent `init()` calls
* **PERFORMANCE**: `IOUtilities.compressBytes(FastByteArrayOutputStream, FastByteArrayOutputStream)` - Eliminated unnecessary buffer copy
  * Used `writeTo(gzipStream)` (zero-copy) instead of `toByteArray()` (allocates full copy), matching the `ByteArrayOutputStream` overload
* **PERFORMANCE**: `IOUtilities.compressBytes(byte[], int, int)` - Removed redundant double copy
  * `toByteArray()` already returns a correctly-sized copy; the outer `Arrays.copyOf()` was redundant
* **BUG FIX**: `IdentitySet.addInternal()` - Element duplication after removal via tombstone slots
  * When a DELETED tombstone appeared before an existing element in the probe chain, `addInternal()` inserted a duplicate without checking further, causing `add()` to return `true` for elements already present, inflated `size`, and ghost entries that survived `remove()`
  * Fixed by remembering the first DELETED slot but continuing to probe until `null` or finding the element
* **BUG FIX**: `IdentitySet` constructor - Infinite loop for `initialCapacity > 2^30`
  * The power-of-2 rounding loop overflowed `int`, causing `capacity` to go negative then zero, looping forever
  * Fixed by clamping the target to `[1, 2^30]` before the loop
* **BUG FIX**: `Executor` - `InterruptedException` swallowed without restoring thread interrupt flag
  * Both `execute()` methods now call `Thread.currentThread().interrupt()` before returning
  * Bounded gobbler thread `join()` calls with timeout to prevent indefinite hangs after `destroyForcibly()`
* **BUG FIX**: `ExceptionUtilities.getDeepestException()` - Infinite loop on circular exception cause chains
  * Added cycle detection using `IdentitySet` to safely handle circular chains (e.g. A→B→A)
  * Fixed Javadoc on `safelyIgnoreException(Throwable)` that incorrectly claimed `ThreadDeath` was rethrown
* **BUG FIX**: `EncryptionUtilities` - All 7 `fast*` file-hashing methods (`fastMD5`, `fastSHA1`, `fastSHA256`, `fastSHA384`, `fastSHA512`, `fastSHA3_256`, `fastSHA3_512`) never used the optimized `FileChannel` path
  * `Files.newInputStream()` returns `ChannelInputStream` on Java 9+, not `FileInputStream`, so the `instanceof FileInputStream` check always failed
  * Every call fell through to the slower `InputStream.read(byte[])` path instead of the intended `FileChannel`/`ByteBuffer` path
  * Fixed by using `new FileInputStream(file)` directly, which guarantees the `FileChannel` optimization is used
* **PERFORMANCE**: `ByteUtilities` - Cached security property lookups for better performance
  * Security-related `System.getProperty()` calls are now cached with property change detection
  * Eliminates repeated property parsing on every `encode()`/`decode()` call
  * Cache automatically refreshes when property values change
* **BUG FIX**: `ByteUtilities.encode()` - Added integer overflow protection
  * Arrays larger than `Integer.MAX_VALUE / 2` now throw `IllegalArgumentException`
  * Previously would cause `NegativeArraySizeException` due to `bytes.length * 2` overflow
* **PERFORMANCE**: `ByteUtilities.indexOf()` - Added fast path for single-byte patterns
  * Single-byte pattern searches now use optimized loop without nested iteration
* **FEATURE**: `ByteUtilities` - Added new search methods
  * `lastIndexOf(byte[] data, byte[] pattern, int start)` - Find last occurrence searching backwards
  * `lastIndexOf(byte[] data, byte[] pattern)` - Find last occurrence from end
  * `contains(byte[] data, byte[] pattern)` - Check if pattern exists in data
* **CLEANUP**: `ByteUtilities.HEX_ARRAY` is now private
  * Use `ByteUtilities.toHexChar(int)` public API instead
  * `StringUtilities` updated to use `toHexChar()` method
* **MAINTENANCE**: Fixed flaky `IOUtilitiesProtocolValidationTest.testProtocolValidationPerformance` test
  * Added warmup iterations to allow JIT compilation before timing
  * Increased threshold from 100ms to 500ms for CI environments with variable performance
* **MAINTENANCE**: Fixed flaky `UniqueIdGeneratorTest` timing tests
  * Increased threshold from 2ms to 50ms for CI environments with thread scheduling delays
* **MAINTENANCE**: Fixed flaky `MultiKeyMapLockStripingTest.testPerformanceWithStriping` test
  * Increased slowdown tolerance from 5x to 10x for CI environments with shared resources
* **BUG FIX**: `MultiKeyMap` - `cachedHashCode` not invalidated by ConcurrentMap methods
  * `putIfAbsent`, `computeIfAbsent`, `computeIfPresent`, `compute`, `merge`, `remove(K,V)`, `replace(K,V)`, `replace(K,V,V)` all bypassed `cachedHashCode = null` invalidation
  * After any of these methods mutated the map, `hashCode()` returned a stale cached value
* **BUG FIX**: `MultiKeyMap` - `compareCollections` skips trailing elements after Set sections
  * Unconditional `i++` at end of while loop overcounted after Set branch which already advances `i`
  * Elements following a Set in an expanded key were never compared, causing false key matches
* **BUG FIX**: `MultiKeyMap` - Small Set comparison (<=6 elements) doesn't track consumed matches
  * Under `valueBasedEquality`, two distinct elements in set1 could match the same element in set2
  * For example, `Integer(1)` and `Long(1L)` both matching `Integer(1)`, producing false equality
  * Fixed with `boolean[]` consumed tracking in all three comparison methods
* **BUG FIX**: `MultiKeyMap` - Stripe contention diagnostics track wrong stripe
  * `putInternal` and `removeInternal` used `hash & STRIPE_MASK` but `getStripeLock` uses `(hash & tableMask) & STRIPE_MASK`
  * When table size < stripe count, per-stripe metrics were attributed to incorrect stripes
  * Extracted shared `getStripeIndex()` helper for consistent stripe computation
* **BUG FIX**: `MultiKeyMap` - ThreadLocal lookup arrays leak references
  * `getMultiKey()` and `containsMultiKey()` methods never nulled out array entries after use
  * In thread-pool environments, this pinned references to user objects for the lifetime of the thread
  * Added `try/finally` cleanup in all 8 methods
* **PERFORMANCE**: `MultiKeyMap` - Hash spreading for better bucket distribution
  * Added `spread(h) = h ^ (h >>> 16)` at all bucket selection points (same technique as `ConcurrentHashMap`)
  * When the table is small, only low-order bits select the bucket; spreading mixes in higher bits to reduce collisions
  * Applied at all 7 bucket index computation sites; stored hashes are unchanged
* **PERFORMANCE**: `MultiKeyMap` - `keySet()` and `values()` no longer rebuild full `entrySet()`
  * `values()` now iterates buckets directly, skipping all key reconstruction (`reconstructKey()`) and `SimpleEntry` allocation
  * `keySet()` now iterates buckets directly, skipping `SimpleEntry` wrapper allocation
* **PERFORMANCE**: `MultiKeyMap` - Lock contention tracking is now opt-in via `trackContentionMetrics(true)`
  * Eliminates 2+ `AtomicInteger` CAS operations per `put`/`remove` call when tracking is disabled (default)
  * Also skips the `tryLock()`-then-`lock()` contention detection pattern, using a single `lock()` call instead
  * Enable via `MultiKeyMap.builder().trackContentionMetrics(true)` when diagnostics are needed
* **PERFORMANCE**: `CaseInsensitiveMap` - Eliminate double lookup in `equals()`
  * Replaced `containsKey()` + `get()` (two `convertKey()` calls and two hash probes per entry) with single `get()`, falling back to `containsKey()` only for null values
* **PERFORMANCE**: `CaseInsensitiveMap` - Cache `keySet()` and `entrySet()` view objects
  * Previously created new `AbstractSet` instances on every call; now cached in `transient` fields matching the JDK `AbstractMap`/`HashMap` pattern
* **BUG FIX**: `AbstractConcurrentNullSafeMap` - `merge()` incorrectly invokes remapping function when existing value is null
  * When a key was mapped to `null` (stored internally as `NullSentinel.NULL_VALUE`), the sentinel made null appear non-null to the backing map's `merge()`
  * The remapping function was incorrectly called with `(null, newValue)` instead of just inserting the new value per the `merge()` contract
* **PERFORMANCE**: `AbstractConcurrentNullSafeMap` - `equals()` and `hashCode()` iterate internal map directly
  * Previously iterated `entrySet()` wrappers where each `Entry.getValue()` did a full hash probe on the backing map
  * Also eliminated `containsKey()` + `get()` double lookup on the other map in `equals()`
* **PERFORMANCE**: `AbstractConcurrentNullSafeMap` - Cache `keySet()`, `entrySet()`, and `values()` view objects
  * Previously created new anonymous instances on every call; now cached in `transient` fields
* **BUG FIX**: `CaseInsensitiveMap` - `entrySet().remove()` and `removeAll()` ignore entry value
  * Both methods only checked the key, removing entries even when the value didn't match
  * Violates the `Set<Map.Entry>` contract which requires both key AND value to match
  * Fixed: `remove()` now checks `Objects.equals()` on the value before removing; `removeAll()` delegates to the fixed `remove()`
* **BUG FIX**: `MultiKeyMap` - `hashCode()` uses case-sensitive hashing for case-insensitive maps
  * `hashCode()` iterated `entrySet()` which reconstructs original-case String keys, then used `String.hashCode()` (case-sensitive)
  * Two equal case-insensitive MultiKeyMaps with different-case keys produced different hashCodes, violating the hashCode contract
  * Fixed: case-insensitive mode now iterates internal buckets using pre-computed case-insensitive key hashes
* **BUG FIX**: `ClassValueSet` - `clear()` race condition allows permanently stale cache entries
  * Cache was invalidated BEFORE clearing the backing set, allowing a concurrent `contains()` to re-cache stale `true` values that would never be invalidated
  * Fixed: snapshot keys, clear backing set first, then invalidate cache (matching `ClassValueMap` pattern)
* **PERFORMANCE**: `ClassValueSet` - Remove unnecessary O(n) copy in `iterator()`
  * Previously copied entire backing set into an `IdentitySet` snapshot on every `iterator()` call
  * `ConcurrentHashMap.newKeySet()` already provides weakly-consistent iterators that never throw `ConcurrentModificationException`
* **PERFORMANCE**: `ClassValueSet` - Simplify `equals()` and `hashCode()`
  * Removed redundant second iteration in `equals()` — `size()` check + one-direction subset check is sufficient
  * Removed dead `h += 0` null branch and unnecessary null guard in `hashCode()`
* **PERFORMANCE**: `CompactSet` - Add `equals()` self-check to avoid O(n) comparison when comparing to self
* **BUG FIX**: `ClassValueMap` - `remove(null, value)` and `replace(null, oldValue, newValue)` use identity comparison instead of `equals()`
  * Both methods used `AtomicReference.compareAndSet()` which compares with `==`, not `equals()`
  * For non-interned objects, the operations silently failed even when the value was logically equal
  * Fixed: CAS loops with `Objects.equals()` for proper value-based comparison per the `ConcurrentMap` contract
* **PERFORMANCE**: `ClassValueMap` - Cache `entrySet()` and `values()` view objects
  * Previously created new anonymous instances on every call; now cached in `transient` fields
* **PERFORMANCE**: `ClassValueMap` - Override `containsValue()` for direct check
  * Previously inherited `AbstractMap.containsValue()` which iterates `entrySet()` creating wrapper objects
  * Now checks `nullKeyStore` + `backingMap.containsValue()` directly without allocation
* **PERFORMANCE**: `ClassValueMap` - Override `isEmpty()` for short-circuit
  * `backingMap.isEmpty()` is O(1) vs inherited `size() == 0` which computes full ConcurrentHashMap size
* **BUG FIX**: `ConcurrentNavigableMapNullSafe` - `wrapEntry().getValue()` performs live lookup instead of returning snapshot value
  * Navigation entry methods (`firstEntry`, `lastEntry`, `lowerEntry`, `floorEntry`, `ceilingEntry`, `higherEntry`) returned entries whose `getValue()` did `internalMap.get(key)` — a live lookup
  * Violated the `NavigableMap` contract that these methods return snapshot entries
  * If the map was modified after getting the entry, `getValue()` returned the new value or `null` (if the key was removed)
  * Fixed: `wrapEntry()` now returns `SimpleImmutableEntry` with snapshot value, matching `ConcurrentSkipListMap` behavior
* **PERFORMANCE**: `ConcurrentNavigableMapNullSafe` - Cache `keySet()` view object
  * Previously created a new `KeyNavigableSet` on every `keySet()` call, bypassing the base class cache
  * Now cached in a `transient` field; the view is already backed by the live internal map
* **BUG FIX**: `ConcurrentList` - `remove(int)` TOCTOU race can remove wrong element
  * `index == size() - 1` check was before the write lock; concurrent modifications between the check and `removeLast()` acquiring the lock could cause a different element to be removed
  * Fixed: moved size check inside the write lock
* **BUG FIX**: `ConcurrentList` - `add(int, E)` TOCTOU race can insert at wrong position
  * `index == size()` check was before the write lock; concurrent `addLast()` between the check and lock acquisition could place the element at the wrong index
  * Fixed: moved size check inside the write lock
* **BUG FIX**: `ConcurrentList` - `addAll(Collection)` not atomic — elements can be interleaved
  * Each `addLast()` acquired/released the write lock individually, allowing concurrent operations to interleave elements within the batch
  * Inconsistent with `addAll(int, Collection)` which was already atomic
  * Fixed: write lock is now held for the entire operation
* **PERFORMANCE**: `ConcurrentList` - `hashCode()`, `equals()`, `forEach()`, and `toString()` avoid O(n) snapshot allocation
  * Previously created snapshot arrays via `iterator()` → `toArray()`; now iterate directly under read lock
* **BUG FIX**: `Converter` - `isConversionSupportedFor()` caches `UNSUPPORTED` for user-added conversions, poisoning `convert()`
  * `getConversionFromDBs()` used hardcoded instanceId `0L` for `USER_DB` lookups, but `addConversion()` stores entries with `this.instanceId`
  * `isConversionSupportedFor()` cached `UNSUPPORTED` when it couldn't find the user conversion
  * If `isConversionSupportedFor(source, target)` was called before `convert(source, target)`, the `UNSUPPORTED` entry poisoned the cache, causing `convert()` to silently return `null`
  * Fixed: `getConversionFromDBs()` now checks `USER_DB` with `this.instanceId` first
* **BUG FIX**: `Converter` - `hasConverterOverrideFor()` misses dynamically added conversions
  * Extracted instanceId from `options.getConverterOverrides()` iterator instead of using `this.instanceId`
  * Conversions added via `addConversion()` after construction were invisible to `isSimpleTypeConversionSupported()`
  * Fixed: uses `this.instanceId` directly for a simple O(1) hash lookup
* **BUG FIX**: `Converter` - Constructor stores option overrides with wrong instanceId
  * `ConverterOptions` overrides were stored in `USER_DB` with the pair's instanceId (typically `0L`) instead of `this.instanceId`
  * This was inconsistent with `addConversion()` which uses `this.instanceId`, causing lookup mismatches
  * Fixed: constructor now stores overrides with `this.instanceId`
* **BUG FIX**: `Converter` - `isConversionSupportedFor(Class)` static cache ignores dynamic overrides
  * `SELF_CONVERSION_CACHE` is static but caches results from instance methods that depend on instance-specific state
  * If an instance without overrides cached `false`, instances with user overrides also got `false`
  * Fixed: checks `hasConverterOverrideFor()` before consulting the static cache
* **PERFORMANCE**: `Converter` - Skip `USER_DB` lookup in `convert()` when no user conversions exist
  * Added `hasUserConversions` flag set in constructor and `addConversion()`
  * When `false`, the `USER_DB.get()` call is skipped, saving one `ConversionPair` allocation + `ConcurrentHashMap.get()` per `convert()` call
* **PERFORMANCE**: `Converter` - Remove redundant instance-level caching for built-in conversions
  * Built-in conversions were cached at both `0L` (shared) and `instanceId` (instance-specific)
  * The instance-level cache was redundant since `getCachedConverter()` falls back to the shared `0L` entry
  * Removed the redundant `cacheConverter()` call, reducing `FULL_CONVERSION_CACHE` entries
* **BUG FIX**: `Converter` - `isConversionSupportedFor()` and `isSimpleTypeConversionSupported()` miss user conversions via inheritance
  * Both methods called `getInheritedConverter()` with hardcoded instanceId `0L` instead of `this.instanceId`
  * If a user registered a conversion for a parent class (e.g., `Number→MyType`), queries for child classes (e.g., `Integer→MyType`) could not find it via the inheritance walk
  * Worse, both methods cached `UNSUPPORTED`, poisoning the `convert()` cache — the same class of cache poisoning bug as the direct-lookup fix above, but in the inheritance path
  * Fixed: both methods now use `this.instanceId` for the inheritance walk, consistent with `convert()`
* **CLEANUP**: `Converter` - Removed dead code in `getConversionFromDBs()`
  * After the constructor fix (storing overrides at `this.instanceId`), the `USER_DB` check at instanceId `0L` was unreachable dead code
* **BUG FIX**: `ConcurrentSet` - `toString()` used `{}` braces instead of standard `[]` brackets
  * All JDK `Set` implementations and `AbstractCollection.toString()` use `[]`; `ConcurrentSet` was the only outlier
* **PERFORMANCE**: `ConcurrentSet` - `retainAll()` uses `HashSet` instead of `ConcurrentHashMap.newKeySet()` for temporary lookup
  * The temporary wrapped-collection set is only accessed by the current thread; concurrent overhead was unnecessary
* **MAINTENANCE**: `CaseInsensitiveSet` - Added regression tests for case-insensitive `hashCode()` and `retainAll()`
  * Verified `hashCode()` is case-insensitive through `SetFromMap` → `CaseInsensitiveMap.keySet().hashCode()` delegation
  * Verified `retainAll()` is case-insensitive through `SetFromMap` → `CaseInsensitiveMap.keySet().retainAll()` delegation
* **BUG FIX**: `CompactMap` - EMPTY_MAP sentinel collision causing silent data loss
  * The `EMPTY_MAP` sentinel was a static `String`; if a user stored that exact string as a value, Java string interning caused the map to appear empty
  * Fixed: changed `EMPTY_MAP` to a unique `Object` instance that cannot collide with user values
* **BUG FIX**: `CompactMap` - Iterator `remove()` during Map-to-array transition
  * When `iterator.remove()` caused size to drop to `compactSize()`, the code bypassed `mapIterator.remove()` and called `CompactMap.this.remove()` directly, creating fragile coupling to backing map iteration order
  * Fixed: always use `mapIterator.remove()` first, then manually build the compact array from remaining entries
* **BUG FIX**: `CompactMap` - `equals()` missing `return true` for compact array path
  * The loop verifying all entries match fell through without returning `true`, reaching a fallthrough path that redundantly checked equality via `entrySet().equals()`
  * Fixed: added `return true` after the compact array verification loop
* **BUG FIX**: `CompactMap` - `removeFromMap()` not sorting array after Map-to-array transition
  * Entries were copied in backing map's iteration order without sorting; for sorted/reverse CompactMaps, binary search failed to find present keys
  * Fixed: call `sortCompactArray()` after building the array in both `removeFromMap()` and the iterator's transition path
* **BUG FIX**: `CompactMap` - `keySet().retainAll()` case-sensitivity mismatch
  * For legacy subclasses where `isCaseInsensitive()` returns `true` but `getNewMap()` returns a plain `HashMap`, the retain lookup was case-sensitive
  * Keys differing only in case from the retain collection were incorrectly removed
  * Fixed: use `CaseInsensitiveMap` for the lookup when `isCaseInsensitive()` is true
* **BUG FIX**: `CompactMap` - `ConcurrentModificationException` detection uses size, not modCount
  * Iterator used `expectedSize != size()` to detect concurrent modification, missing structural changes where size stayed constant (e.g., add one key + remove another)
  * Fixed: added `modCount` field that increments on every structural modification; iterator checks `expectedModCount` matching standard Java collection behavior
* **PERFORMANCE**: `CompactMap` - Eliminate double lookup in `removeFromMap()`
  * Replaced `containsKey()` + `remove()` with single `remove()` call, falling back to `containsKey()` only when `remove()` returns null
* **PERFORMANCE**: `CompactMap` - Optimize `handleTransitionToSingleEntry()`
  * Directly assigns `val` to remaining entry, bypassing `clear()` + `put()` which dispatches through the full state chain
* **PERFORMANCE**: `CompactMap` - Single scan in `equals()` for compact array path
  * Replaced `containsKey()` + `get()` (two linear scans per entry) with single inline scan that finds key and retrieves value in one pass
* **PERFORMANCE**: `CompactMap` - Cache size in iterator
  * Replaced `size()` calls in `hasNext()` and `advance()` with the already-tracked `expectedSize` field, avoiding `instanceof` dispatch on every iteration step

#### 4.90.0 - 2026-02-02
* **BUG FIX**: `DeepEquals` - URL comparison now uses string representation instead of `URL.equals()`
  * Java's `URL.equals()` performs DNS resolution which causes flaky CI failures
  * Now compares URLs using `toExternalForm()` for reliable, deterministic comparison
* **MAINTENANCE**: Migrated test files from deprecated `JsonIo.toObjects()` to `JsonIo.toJava().asClass()` API
  * Updated 8 calls in `CompactMapTest` and `ConverterEverythingTest`
* **PERFORMANCE**: Added `FastReader.readUntil()` for bulk character reading until delimiter
  * Reads characters into destination buffer until one of two delimiters is found
  * Delimiter character is left unconsumed for subsequent read
  * Enables bulk string parsing optimization in json-io's JsonParser

#### 4.89.0 - 2026-01-31
* **PERFORMANCE**: `FastReader.getLastSnippet()` now returns bounded 200-char context
  * Previously could return 0 to 8192 characters depending on buffer position
  * Now consistently returns up to the last 200 characters read for useful error context
* **PERFORMANCE**: `ParameterizedTypeImpl.getActualTypeArguments()` removed defensive clone
  * Returns direct reference for performance - callers should not modify the array
  * Eliminates allocation overhead in hot paths during type resolution
* **PERFORMANCE**: `Converter.isConversionSupportedFor(source, target)` now caches negative results
  * Previously only cached positive hits, causing repeated inheritance traversal for unsupported pairs
  * Now caches `UNSUPPORTED` sentinel for O(1) lookup on subsequent calls
  * `addConversion()` already clears caches via `clearCachesForType()`, so invalidation is handled
* **PERFORMANCE**: `ReflectionUtils.getMethod()` and `getNonOverloadedMethod()` now cache negative results
  * Previously threw exceptions inside `computeIfAbsent`, bypassing the cache for "not found" cases
  * Now caches sentinel `Method` objects for failed lookups, achieving O(1) on subsequent calls
  * Avoids repeated expensive class hierarchy traversals for non-existent methods
* **PERFORMANCE**: `ConcurrentSet.spliterator()` now returns an optimized spliterator for parallel streams
  * Delegates to underlying `ConcurrentHashMap` spliterator for efficient parallel decomposition
  * Properly reports `CONCURRENT` and `DISTINCT` characteristics
  * Correctly handles null sentinel unwrapping for null element support
  * Enables efficient `parallelStream()` operations on `ConcurrentSet`
* **BUG FIX**: `TrackingMap.putIfAbsent()` now correctly handles null values
  * Previously used `get() == null` which conflates "key absent" with "key present with null value"
  * Now uses `containsKey()` to properly distinguish between the two cases
* **BUG FIX**: `TrackingMap.computeIfPresent()` now only tracks keys that were actually present
  * Previously tracked the key unconditionally, even when the key didn't exist
* **PERFORMANCE**: `TrackingMap` now caches interface cast results at construction time
  * Avoids repeated `instanceof` checks and casts for `asConcurrent()`, `asNavigable()`, `asSorted()` methods
* **PERFORMANCE**: `TrackingMap` - Pre-sized `HashSet` in constructor for `readKeys`
  * Uses initial capacity of 16 to reduce early rehashing
* **BUG FIX**: `TrackingMap` sub-maps now share the parent's `readKeys` set
  * `subMap()`, `headMap()`, `tailMap()`, `descendingMap()` now correctly track reads across all views
  * Previously each sub-map had an isolated `readKeys` set that didn't reflect in parent
* **BUG FIX**: `TTLCache.containsValue()` now filters expired entries
  * Previously returned `true` for expired entries still in the cache
* **BUG FIX**: `TTLCache.keySet()` now filters expired entries
  * Iterator skips expired entries and correctly removes underlying cache entries
* **BUG FIX**: `TTLCache.values()` now filters expired entries
  * Iterator skips expired entries and correctly removes underlying cache entries
* **BUG FIX**: `TTLCache.entrySet()` now filters expired entries
  * Iterator skips expired entries with proper look-ahead logic
  * `remove()` correctly removes the last-returned entry, not the prefetched next entry
* **BUG FIX**: `TTLCache.hashCode()` now filters expired entries
  * Previously included expired entries in hash computation
* **BUG FIX**: `TTLCache.close()` now thread-safe
  * Added `closed` volatile flag to prevent double-close of scheduled executor
* **BUG FIX**: `TestUtil.assertContainsIgnoreCase()` and `checkContainsIgnoreCase()` now correctly advance past found tokens
  * Previously could match the same token multiple times when searching for sequential occurrences
  * Now uses `indexOf(needle, fromIndex)` to ensure each token is found after the previous one
* **BUG FIX**: `TestUtil.fetchResource()` now uses UTF-8 charset as documented
  * Previously used platform default charset despite Javadoc stating UTF-8
* **BUG FIX**: `TestUtil.fetchResource()` now throws descriptive error for missing resources
  * Previously threw NPE with no context when resource not found
  * Now throws `IllegalArgumentException` with resource name
* **PERFORMANCE**: `TestUtil.assertContainsIgnoreCase()` and `checkContainsIgnoreCase()` avoid substring allocation
  * Uses offset tracking with `indexOf(needle, fromIndex)` instead of creating substring on each iteration
* **PERFORMANCE**: `Traverser` optimizations for object graph traversal
  * Added `ClassValueMap` cache for `shouldSkipClass()` to avoid repeated `isAssignableFrom()` checks
  * Hoisted security limit lookups outside traversal loop (limits don't change during traversal)
  * `NodeVisit` constructor now wraps maps directly instead of creating unnecessary `HashMap` copies
  * Inlined validation methods to reduce method call overhead
* **PERFORMANCE**: `SafeSimpleDateFormat` thread-safety and allocation improvements
  * `State` constructor now takes `Long` instead of `Date` to avoid temporary object allocation
  * `getSdf()` now uses `computeIfAbsent()` instead of manual get/put pattern
  * `update()` method now uses `compareAndSet` loop for proper thread-safety under concurrent mutations
  * Eliminated unnecessary `Date` object creation in setter methods
* **CLEANUP**: Import organization and unused import removal across multiple classes

#### 4.88.0 - 2026-01-26
* **BUG FIX**: `FastReader` - Added bounds validation in `read(char[], int, int)` method
  * Now throws `IndexOutOfBoundsException` for invalid offset, length, or buffer overflow
  * Matches standard `Reader` contract and `FastWriter` behavior
* **BUG FIX**: `FastWriter` - Fixed NPE in `flush()` after `close()`
  * `flush()` now returns safely when called after `close()` instead of throwing `NullPointerException`
  * Matches `close()` behavior which already handles this case
* **BUG FIX**: `FastWriter` - Added bounds validation in `write(String, int, int)` method
  * Now throws `IndexOutOfBoundsException` for invalid offset/length parameters
  * Matches validation in `write(char[], int, int)` method
* **PERFORMANCE**: `FastWriter` - Improved buffer utilization in `write(int c)` method
  * Now uses full buffer capacity before flushing (was wasting one slot)
  * Buffer is flushed immediately when full to maintain invariants for other write methods
* **PERFORMANCE**: `FastWriter` - Made class `final` for JVM optimizations
  * Enables JIT compiler to inline method calls, matching `FastReader` which is already final
* **BUG FIX**: `FastByteArrayOutputStream` - Fixed critical integer overflow in `grow()` method
* **NEW**: `FastByteArrayInputStream` - Added JDK 9+ compatible methods
  * `readAllBytes()` - Efficient single-copy implementation (auto-overrides on JDK 9+)
  * `readNBytes(int len)` - Efficient partial read (auto-overrides on JDK 11+)
  * `transferTo(OutputStream)` - Single write operation to output (auto-overrides on JDK 9+)
  * All methods work on JDK 8 as regular methods and automatically become overrides on newer JDKs
* **NEW**: `FastByteArrayOutputStream` - Added zero-copy buffer access methods
  * `getInternalBuffer()` - Direct access to internal buffer without copying
  * `getCount()` - Returns valid byte count for use with `getInternalBuffer()`
  * `toInputStream()` - Creates `FastByteArrayInputStream` from current data
* **NEW**: `FastByteArrayOutputStream` - Added `toString(Charset)` method
  * Allows explicit charset specification instead of platform default encoding
* **CLEANUP**: `FastByteArrayInputStream` - Added missing `@Override` on `read()` method
* **CLEANUP**: `FastByteArrayInputStream` - Added explicit `(int)` cast in `skip()` method
  * Makes the safe long-to-int conversion explicit with explanatory comment
* **CLEANUP**: `FastByteArrayOutputStream` - Fixed Javadoc typo ("theoerical" → "theoretical")
* **PERFORMANCE**: `FastByteArrayOutputStream` - Added early return optimization
* `write(byte[], int, int)` now returns immediately when `len == 0`
* Skips unnecessary bounds checks and capacity operations for zero-length writes
* Previous 2x growth (`oldCapacity << 1`) overflowed for buffers > 1GB causing `NegativeArraySizeException`
* Changed to 1.5x growth strategy (`oldCapacity + (oldCapacity >> 1)`) to reduce overflow risk
* Added `MAX_ARRAY_SIZE` constant (`Integer.MAX_VALUE - 8`) following JDK best practices
* Added `hugeCapacity()` method for safe handling of very large allocations
* **BUG FIX**: `FastByteArrayOutputStream` - Fixed inconsistent null exception type
  * `write(byte[], int, int)` now throws `NullPointerException` for null array (was `IndexOutOfBoundsException`)
  * Matches JDK convention and `FastByteArrayInputStream` behavior
* **BUG FIX**: `FastByteArrayInputStream` - Added null validation in constructor
  * Constructor now throws `NullPointerException` with descriptive message for null input
  * Previously threw `NullPointerException` on `buf.length` access with no message
* **BUG FIX**: `StringUtilities` - Fixed `commaSeparatedStringToSet()` return type inconsistency
  * Changed from `Collectors.toSet()` to `Collectors.toCollection(LinkedHashSet::new)`
  * Now consistently returns `LinkedHashSet` as documented, maintaining insertion order
* **BUG FIX**: `StringUtilities` - Fixed integer overflow in `repeat()` method
  * Moved overflow check outside security block so it always runs
  * Prevents `StringBuilder` from being created with negative capacity when `s.length() * count` overflows
* **BUG FIX**: `StringUtilities` - Fixed integer overflow in `encode()` method
  * Added overflow check for `bytes.length << 1` to handle very large byte arrays
  * Added null check returning `null` for consistency with `decode()` method
* **PERFORMANCE**: `FastReader` - Optimized `getLastSnippet()` implementation
  * Replaced character-by-character `StringBuilder.append()` loop with `new String(buf, 0, position)`
* **PERFORMANCE**: `StringUtilities` - Optimized `snakeToCamel()` to avoid array allocation
  * Replaced `toCharArray()` iteration with `charAt()` loop
  * Added `StringBuilder` initial capacity hint
* **PERFORMANCE**: `StringUtilities` - Optimized `trimEmptyToDefault()` to avoid allocation
  * Replaced `Optional.ofNullable().map().orElse()` with simple null check
  * Removed unused `Optional` import
* **PERFORMANCE**: `StringUtilities` - Optimized `repeat()` with O(log n) doubling algorithm
  * Replaced O(n) loop with doubling algorithm for large repeat counts
  * Added early return for empty string input
* **PERFORMANCE**: `StringUtilities` - Optimized `padLeft()` and `padRight()` methods
  * Replaced character-by-character loop with `Arrays.fill()` on char array
* **PERFORMANCE**: `StringUtilities` - Optimized `encode()` hex encoding
  * Replaced `StringBuilder` with direct char array manipulation
  * Accesses `HEX_ARRAY` directly instead of calling `convertDigit()` method
* **CLEANUP**: `FastReader` - Fixed misleading constructor error message
  * Changed "Buffer sizes must be positive" to accurately state that `bufferSize` must be positive and `pushbackBufferSize` must be non-negative
* **BUG FIX**: `UniqueIdGenerator` - Added validation to `getDate()` and `getDate19()` methods
  * Both methods now throw `IllegalArgumentException` for negative IDs
  * Matches existing validation in `getInstant()` and `getInstant19()`
* **PERFORMANCE**: `UniqueIdGenerator` - Replaced reflection with MethodHandle for `onSpinWait()`
  * Changed from `Method.invoke()` to `MethodHandle.invokeExact()` in spin-wait loop
  * `MethodHandle.invokeExact()` can be inlined by JIT compiler; `Method.invoke()` cannot
  * Estimated 10-50x faster spin-wait hint delivery after JIT warmup
* **PERFORMANCE**: `UniqueIdGenerator` - Optimized `waitForNextMillis()` to reduce syscall overhead
  * Reduced `currentTimeMillis()` calls by batching 8 spin-waits between time checks
  * Previously called `currentTimeMillis()` after every single `onSpinWait()`
* **PERFORMANCE**: `UniqueIdGenerator` - Optimized static initialization
  * Cached hostname lookup to avoid duplicate `getExternalVariable("HOSTNAME")` calls
  * Replaced SHA256 hash with simpler `String.hashCode()` for hostname-based server ID
  * Both provide sufficient distribution for 0-99 range; hashCode is much faster
* **CLEANUP**: `UniqueIdGenerator` - Removed unused `StandardCharsets` import
  * No longer needed after switching from SHA256 to hashCode for hostname hashing
* **PERFORMANCE**: `ReflectionUtils` - Cache parsed dangerous class and sensitive field patterns
  * Added volatile caching with property change detection for `getDangerousClassPatterns()` and `getSensitiveFieldPatterns()`
  * Patterns are now parsed once and cached until the system property value changes
  * Pre-processes patterns (trim/toLowerCase) during cache population for efficient matching
  * Reduces repeated string parsing overhead in security checks
* **PERFORMANCE**: `ReflectionUtils` - Cache Method lookups for Record component operations
  * Extended `RecordSupport` class to cache `RecordComponent.getName()` and `RecordComponent.getAccessor()` methods
  * Avoids repeated reflection lookups when processing Record types
* **SECURITY**: `ReflectionUtils` - Add bounds checking in `getClassNameFromByteCode()`
  * Added validation for `this_class` index and string pool index before array access
  * Prevents `ArrayIndexOutOfBoundsException` on malformed class files
  * Returns clear `IllegalStateException` with descriptive message for invalid bytecode
* **CLEANUP**: `ReflectionUtils` - Removed dead code `makeParamKey()` method
  * Method was unused and has been removed

#### 4.87.0  - 2026-01-26
* **SECURITY**: `DateUtilities` - Fixed ReDoS vulnerability in malformed input validation
  * Replaced vulnerable regex pattern `(.{10,})\1{4,}` with algorithmic `hasExcessiveRepetition()` method
  * New method detects excessive repetition without catastrophic backtracking risk
* **PERFORMANCE**: `DateUtilities` - Multiple optimizations reducing memory allocation
  * Replaced `ConcurrentHashMap` with `HashMap` for months and timezone maps (immutable data)
  * Replaced `BigDecimal` arithmetic in `convertFractionToNanos()` with string/long operations
  * Removed unused `BigDecimal` import
* **IMPROVED**: `DateUtilities` - Updated timezone name `Europe/Kiev` to `Europe/Kyiv`
  * Reflects modern IANA timezone database naming (changed in 2022)
* **CLEANUP**: `ClassUtilities` - Removed dead code and unnecessary synchronization
  * Removed unused `ARG_PATTERN` field (pre-compiled regex that was never used)
  * Removed unnecessary inner `synchronized(holder)` blocks in alias methods
  * Outer synchronization on `ALIASES_TO_CLASS` already provides thread safety
* **BUG FIX**: `ConcurrentList` - Fixed null element handling in Deque methods
  * `getFirst()`, `getLast()`, `removeFirst()`, `removeLast()` now correctly handle null elements
  * Previously threw `NoSuchElementException` when list contained null as a valid element
* **BUG FIX**: `ConcurrentList` - Fixed visibility race condition in addFirst/addLast
  * Changed from `lazySet()` to `set()` for immediate visibility
  * Changed from read lock to write lock to prevent readers seeing updated counters before values written
* **BUG FIX**: `ConcurrentList` - Fixed hashCode to comply with List contract
  * Removed `EncryptionUtilities.finalizeHash()` which violated the standard List.hashCode() formula
  * ConcurrentList.hashCode() now matches ArrayList.hashCode() for equal lists
* **BUG FIX**: `ConcurrentList` - Added bucket cleanup to prevent memory leak
  * Empty buckets outside head/tail range are now removed after poll operations
  * Prevents unbounded memory growth during heavy addFirst/pollFirst or addLast/pollLast usage
* **PERFORMANCE**: `ConcurrentList` - Multiple optimizations reducing allocations
  * `iterator()` now uses direct array-backed iterator instead of ArrayList intermediary
  * `listIterator()` now uses direct array-backed ListIterator
  * `toArray()` builds result array directly instead of via ArrayList
  * `contains()`, `indexOf()`, `lastIndexOf()` use direct index access instead of iterator
  * Simplified `getBucket()` to direct lookup (buckets always exist for valid indices)
* **BUG FIX**: `EncryptionUtilities` - Fixed NumberFormatException on malformed salt/IV size properties
  * Created `getMinSaltSize()`, `getMaxSaltSize()`, `getMinIvSize()`, `getMaxIvSize()` helper methods
  * These methods handle NumberFormatException gracefully like existing property getters
  * Previously, invalid property values caused uncaught exceptions in encrypt/decrypt operations
* **SECURITY**: `EncryptionUtilities` - Clear PBEKeySpec password from memory after key derivation
  * Added `finally` block to call `spec.clearPassword()` after `deriveKey()` operations
  * Follows security best practice to minimize password exposure in memory
* **IMPROVED**: `EncryptionUtilities` - Replaced magic numbers in decrypt methods with constants
  * Decrypt methods now use `STANDARD_SALT_SIZE` and `STANDARD_IV_SIZE` to compute offsets
  * Makes code self-documenting and prevents silent breakage if constants change
* **PERFORMANCE**: `EncryptionUtilities` - Multiple optimizations reducing allocations
  * Cached `SecureRandom` instance as static final field (thread-safe, avoids repeated instantiation)
  * Cached `SecretKeyFactory` instance for PBKDF2 (thread-safe, avoids `getInstance()` per call)
  * Use `ByteBuffer` for cleaner output assembly in encrypt methods
* **CLEANUP**: `EncryptionUtilities` - Removed system property pollution in static initializer
  * Previously set default values into global System.properties namespace
  * Getter methods already handle missing properties by returning defaults
* **BUILD**: Version alignment with json-io 4.87.0
  * Keeping java-util and json-io version numbers synchronized simplifies dependency management and ensures compatibility

#### 4.86.0  - 2025-01-26
* **PERFORMANCE**: `CompactMap` - Multiple optimizations reducing memory allocation and improving iteration performance
  * Fixed redundant `get()` call in `entrySet().contains()` - now reuses the result instead of calling both `get()` and `containsKey()`
  * Use `getCachedLegacyConstructed()` consistently in `switchBackingFromMapToArray()` instead of calling `getClass().getName().endsWith()`
  * Cache `CompactMapComparator` instances statically - 4 pre-created comparators for all combinations of case-insensitive and reverse ordering
  * Added `isArraySorted()` check in `quickSort()` to skip unnecessary sorting during iteration (common case since binary insertion in `put()` maintains order)

#### 4.85.0  - 2025-01-24
* **PERFORMANCE**: `ClassUtilities` - Lock-free cache operations using CAS-based removal
  * Replaced synchronized blocks in `fromCache()`, `toCache()`, and `drainQueue()` with `ConcurrentHashMap.remove(key, value)`
  * CAS-based removal only removes if the exact WeakReference is still present, preventing race conditions
  * Eliminates lock contention in high-throughput class loading scenarios
* **PERFORMANCE**: `ClassUtilities` - Added cache for assignable type lookups in `getArgForType()`
  * New `ASSIGNABLE_TYPE_CACHE` using `ClassValueMap<Optional<Supplier<Object>>>` avoids repeated O(n) scans of `ASSIGNABLE_CLASS_MAPPING`
  * Uses `Optional` to distinguish "no match found" (`Optional.empty()`) from "not yet cached" (`null`)
  * Improves constructor parameter resolution performance for repeated type lookups
* **PERFORMANCE**: `ClassUtilities` - Optimized synthetic parameter name detection
  * Added `isSyntheticArgName()` method using efficient string operations instead of regex
  * Avoids `Pattern.matcher().matches()` overhead for checking "arg0", "arg1", etc. patterns
  * Used in constructor parameter resolution to detect compiler-generated names
* **PERFORMANCE**: `DeepEquals` - Replace O(n²) visited set copying with O(1) ScopedSet for unordered collections
  * Previously, comparing unordered collections copied the entire visited set for each probe element (O(n × k))
  * New `ScopedSet` wrapper creates a lightweight view with O(1) creation time
  * Tracks local additions separately and discards them when probe completes
  * Significant improvement for comparing large unordered collections/maps with cycles
* **PERFORMANCE**: `DeepEquals` - Multiple optimizations reducing per-comparison overhead
  * Replaced `ConcurrentSet` with `HashSet` for visited tracking - no concurrent access occurs
  * Cache system properties at class load time instead of parsing on every call
  * Simplified `hashCode()` by removing unnecessary `finalizeHash()` call
* **IMPROVED**: `ConcurrentNavigableSetNullSafe` - Changed sentinel from String to Object instance
  * Previous `"null_" + UUID.randomUUID()` String could theoretically collide with user data
  * New `new Object()` sentinel is guaranteed unique and cannot collide
  * Also changed from `.equals()` to identity comparison (`==`) for sentinel detection
* **BUG FIX**: `ClassValueMap` - Fixed `clear()` race condition that could leave permanently stale cache entries
  * Previous order: invalidate cache → clear backingMap (allowed concurrent `get()` to repopulate cache from still-populated backingMap)
  * Fixed order: snapshot keys → clear backingMap → invalidate cache (ensures any `computeValue` after clear sees empty map)
  * After `clear()` completes, all subsequent `get()` calls now see correct state
* **BUG FIX**: `ConcurrentNavigableMapNullSafe` - Fixed `ClassCastException` on sub-map views
  * Navigational methods (`lowerKey`, `floorKey`, `ceilingKey`, `higherKey`, etc.) incorrectly cast to `ConcurrentSkipListMap`
  * Sub-map, head-map, tail-map, and descending-map views are not `ConcurrentSkipListMap` instances
  * Fixed by casting to `ConcurrentNavigableMap` instead (the interface that defines these methods)
* **BUG FIX**: `ConcurrentNavigableSetNullSafe` - Fixed user comparators that don't handle nulls throwing NPE
  * Comparators like `String.CASE_INSENSITIVE_ORDER` would throw NPE when comparing null elements
  * Wrapper now gracefully falls back to default null ordering (nulls > non-nulls) if comparator throws NPE
  * User comparators that handle nulls (via `Comparator.nullsFirst/nullsLast` or custom handling) still control null ordering
* **BUILD**: Updated json-io test dependency from 4.83.0 to 4.84.0

#### 4.84.0  - 2025-01-19
* **BUG FIX**: `ClassValueMap` - Fixed race condition in `putIfAbsent(null, value)` for null key handling
  * Previously used separate `volatile boolean hasNullKeyMapping` + `AtomicReference<V> nullKeyValue` fields
  * Race: thread A could see `hasNullKeyMapping=true` but read stale `nullKeyValue` before thread B's write completed
  * Fix: Combined into single `AtomicReference<Object> nullKeyStore` with sentinel values (`NO_NULL_KEY_MAPPING`, `NULL_FOR_NULL_KEY`)
  * Eliminates volatile read on common `get()` path - now pure AtomicReference.get()
* **BUG FIX**: `TTLCache` - Fixed thread-safety issue in `put()` that could corrupt LRU chain
  * Previously used `tryLock()` and skipped LRU updates if lock unavailable
  * Under contention, cache map and LRU chain could become inconsistent
  * Fix: Now acquires lock for entire put operation; moves cache map update inside lock
  * Added safety check in `moveToTail()` to skip already-evicted nodes
* **BUG FIX**: `UniversalConversions` - Fixed NPE in bridge methods when source value is null
  * `integerToAtomicInteger()`, `longToAtomicLong()`, `booleanToAtomicBoolean()` now return null for null input
  * Previously threw NullPointerException on `new AtomicXxx(null)`
* **PERFORMANCE**: `LRUCache` (THREADED strategy) - Removed unnecessary volatile from `Node.value` and `Node.timestamp`
  * `value` is never modified after construction
  * `timestamp` is used for approximate LRU only - stale reads acceptable given existing approximations
  * ConcurrentHashMap publish provides necessary memory barriers for initial visibility
* **PERFORMANCE**: `LRUCache` (LOCKING strategy) - GC improvements
  * Made `Node.key` final
  * Null out `node.prev`/`node.next` links on removal to avoid GC nepotism
  * Null out `node.value` on eviction to release value reference promptly
* **PERFORMANCE**: `StringUtilities.hashCodeIgnoreCase()` - Single-pass algorithm optimization
  * Previously made two passes: first to check ASCII, then to compute hash
  * Now computes hash while checking for non-ASCII, falling back only when non-ASCII detected
* **PERFORMANCE**: `Converter` - Removed ~50 redundant conversion entries now handled by surrogate system
  * Entries like `Byte → AtomicBoolean`, `String → AtomicInteger`, etc. are now automatic via surrogate bridges
  * Example: `String → AtomicInteger` now goes `String → Integer → AtomicInteger` via surrogate system
  * Reduces CONVERSION_DB size and maintenance burden without losing any functionality
* **NEW**: `Converter` - Added `Boolean ↔ BitSet` conversions
  * `BitSet → Boolean`: returns `true` if any bit set, `false` if empty
  * `Boolean → BitSet`: `true` creates BitSet with bit 0 set, `false` creates empty BitSet
  * Primitive `boolean ↔ BitSet` works automatically via surrogate system
* **IMPROVED**: `LRUCache` (THREADED strategy) - Fixed scheduler shutdown to properly clear reference
  * `shutdownScheduler()` now sets `scheduler = null` after shutdown to allow recreation
  * Previously, shutdown scheduler reference remained, preventing new cache instances from starting cleanup
* **BUILD**: Updated json-io test dependency from 4.81.0 to 4.83.0
* **BUILD**: Re-enabled `ConverterEverythingTest` (was temporarily excluded)
* **DOCS**: Updated README to reflect 1600+ conversions (was 1000+)

#### 4.83.0  - 2025-01-18
* **PERFORMANCE**: `LRUCache` (THREADED strategy) - Major performance improvements reducing overhead from ~5x to ~1.4x vs ConcurrentHashMap
  * Replaced `System.nanoTime()` with logical clock (AtomicLong counter) for LRU ordering - ~5ns vs ~25ns per operation
  * Simplified probabilistic timestamp updates using timestamp low bits - eliminates atomic/ThreadLocal operations on most accesses
  * Removed extra `get()` call in `put()` since Node creation is now cheap with logical clock
  * Added amortized eviction (batch every 16 inserts) to spread eviction cost across operations
* **BUG FIX**: `LRUCache` - Fixed hard cap enforcement to guarantee memory bounds under high-throughput scenarios
  * Split eviction into `tryEvict()` (skippable) and `forceEvict()` (blocks until complete)
  * Hard cap (2x capacity) now uses `LockSupport.parkNanos(1000)` for efficient spinning with low CPU usage
  * Fixes issue where rapid inserts could exceed memory bounds when eviction couldn't keep up
* **CHANGE**: `CaseInsensitiveMap` - Default cache changed from `ConcurrentHashMap` to `LRUCache` (THREADED strategy)
  * Provides true LRU eviction (hot entries preserved) vs random eviction with ConcurrentHashMap
  * Guarantees bounded memory (max 2x capacity) vs loose bounds with ConcurrentHashMap
  * Performance is equivalent or better in benchmarks
  * Users can still use `replaceCache()` to configure ConcurrentHashMap if desired

#### 4.82.0  - 2025-01-17
* **BUG FIX**: `TypeUtilities` - Fixed WildcardType bounds array mutation bug
  * External `WildcardType` implementations return internal arrays from `getUpperBounds()`/`getLowerBounds()`
  * These arrays were being modified in-place during resolution, corrupting the original type
  * Fix: Clone arrays from external implementations before modification; skip cloning for internal `WildcardTypeImpl`
* **BUG FIX**: `TypeUtilities` - Fixed unsafe cast for method-level TypeVariables
  * `TypeVariable` can be declared on `Method` or `Constructor`, not just `Class`
  * Casting `getGenericDeclaration()` to `Class` would throw `ClassCastException` for method-level type variables
  * Fix: Check declaration type and return first bound for non-class type variables
* **PERFORMANCE**: `TypeUtilities` - Added array class cache to avoid repeated `Array.newInstance()` allocations in `getRawClass()`
* **PERFORMANCE**: `TypeUtilities` - Use `IdentitySet` instead of `HashSet` for cycle detection (reference equality is sufficient and faster)
* **IMPROVED**: `TypeUtilities` - Simplified `hashCode()` implementations using standard `31 * result` pattern instead of `EncryptionUtilities.finalizeHash()`
* **IMPROVED**: `TypeUtilities.WildcardTypeImpl` - Constructor now takes ownership of arrays without cloning (internal class only)
* **IMPROVED**: `DeepEquals` - Fixed Javadoc formatting using `{@code}` blocks for proper HTML rendering of generic types
* **DEPRECATED**: `LRUCache(int capacity, int cleanupDelayMillis)` - The `cleanupDelayMillis` parameter is no longer used; use `LRUCache(int)` instead
* **BUILD**: Added `@SuppressWarnings` annotations to eliminate compile warnings across multiple classes:
  * `CaseInsensitiveMap`, `ClassUtilities`, `ConcurrentNavigableSetNullSafe`, `ConcurrentSet`
  * `MultiKeyMap`, `ReflectionUtils`, `TTLCache`, `UrlUtilities`, `MapConversions`
* **BUILD**: Removed unused regex timeout methods from `DateUtilities`
* **BUILD**: Cleaned up unused code in `ObjectConversions`
* **PERFORMANCE**: `MultiKeyMap` - Significant hot path optimizations using Class identity checks
  * `valueHashCode()` - Reordered type checks to handle Integer/Long/Double first; uses Class identity (`==`) instead of `instanceof` for common types (55% CPU reduction in JFR profiling)
  * `computeElementHash()` - Added Class identity fast path for String/Integer/Long/Double, skipping 4+ instanceof checks for 75% of typical key elements
  * `flattenKey()` - Added fast path at method entry for String/Integer/Long/Double keys
  * `flattenObjectArrayN()` - Skip array/collection checks for known simple types
  * `isArrayOrCollection()` - Added Class identity short-circuit for common simple types
  * **Result**: Cedar MultiKeyMap win rate vs Apache Commons Collections improved from ~60% to ~71% in performance benchmarks, while providing thread-safety, null key/value support, unlimited key count, and value-based numeric equality that Apache lacks
* **PERFORMANCE**: `ClassValueSet` - Optimized with Class identity checks
  * `contains()` and `remove()` now use `o.getClass() == Class.class` instead of `instanceof`
  * `clear()` no longer creates unnecessary HashSet copy before invalidating cache
* **NEW**: `IdentitySet<T>` - High-performance generic Set using object identity (`==`) instead of `equals()`
  * Lightweight replacement for `Collections.newSetFromMap(new IdentityHashMap<>())`
  * Extends `AbstractSet<T>` and implements full `Set<T>` interface including `iterator()`
  * Uses open addressing with linear probing for excellent cache locality
  * Single `Object[]` array - no Entry objects, no Boolean values
  * ~8 bytes per element vs ~40-48 bytes for IdentityHashMap-backed Set
  * Generic type safety: `IdentitySet<Object>`, `IdentitySet<Class<?>>`, `IdentitySet<Map<?,?>>`, etc.
  * Ideal for cycle detection, visited tracking, and identity-based membership tests
* **PERFORMANCE**: Replaced `Collections.newSetFromMap(new IdentityHashMap<>())` with `IdentitySet<T>` in:
  * `Traverser` - object graph traversal visited tracking (`IdentitySet<Object>`)
  * `MapUtilities` - map structure cycle detection (`IdentitySet<Map<?,?>>`)
  * `ObjectConversions` - object-to-map conversion visited tracking (`IdentitySet<Object>`)
  * `DeepEquals` - deep hash code and format value cycle detection (`IdentitySet<Object>`)
  * `ClassUtilities` - inheritance chain traversal visited tracking (`IdentitySet<Class<?>>`)
* **PERFORMANCE**: Replaced `HashSet<Class<?>>` with `IdentitySet<Class<?>>` for faster identity-based lookups:
  * `ReflectionUtils` - Class hierarchy traversal in `findClassAnnotation()`, `getMethodAnnotation()`, interface graph BFS, and method lookup (4 locations)
  * `DeepEquals` - Custom equals class ignore set
  * `CaseInsensitiveMap` - Registry duplicate Class check
  * `ClassValueSet` - `retainAll()` removal tracking and iterator snapshot
  * `Converter` - Type variations tracking

#### 4.81.0 - 2025-01-10
* **PERFORMANCE**: `StringUtilities.equals(CharSequence, CharSequence)` - Optimized for CharSequence-to-String comparisons
  * Now uses `String.contentEquals(CharSequence)` when either argument is a String
  * `String.contentEquals()` is JVM-intrinsic optimized and handles StringBuilder efficiently (direct char array comparison)
  * Previous implementation only optimized String-to-String comparisons, missing the common case of StringBuilder-to-String
* **ARCHITECTURE**: `CompactMap` - Replaced runtime Java compilation with pre-compiled bytecode template
  * **No longer requires JDK**: The builder API (`CompactMap.builder().build()`) now works on JRE, not just JDK
  * **How it works**: A pre-compiled bytecode template is patched at runtime with the configuration hash, then static fields are injected with configuration values (case sensitivity, compact size, ordering, etc.)
  * **Performance**: Eliminates JavaCompiler overhead - template classes are created via `MethodHandles.Lookup.defineClass()` with simple byte array patching
  * **Same functionality**: All existing builder options work exactly as before (case sensitivity, ordering, compact size, custom map types, etc.)
  * **Backward compatible**: Subclassing `CompactMap` continues to work unchanged
  * Disabled obsolete `CompactMapMethodsTest.testGetJavaFileForOutputAndOpenOutputStream` (tested old JavaFileManager approach)
* **BUG FIX**: `Converter` - Fixed char[]/byte[] cross-conversion returning null after `isConversionSupportedFor()` was called
  * Root cause: `VoidConversions::toNull` was used as a placeholder in `CONVERSION_DB` to "advertise" that char[] ↔ byte[] conversions are supported
  * When `isConversionSupportedFor(char[].class, byte[].class)` was called, this placeholder got cached
  * Subsequent `convert(char[], byte[].class)` found the cached placeholder and used it, returning null instead of performing actual conversion
  * Fix: Replaced placeholder entries with actual converters that call `ArrayConversions.arrayToArray()`
  * Added tests: `testIsConversionSupportedForDoesNotBreakConvert`, `testArrayCrossConversionWithAndWithoutSupportCheck`
* **BUG FIX**: `AbstractConcurrentNullSafeMap.computeIfAbsent()` - Fixed contention causing hangs under concurrent load
  * Affects: `ConcurrentHashMapNullSafe` and `ConcurrentNavigableMapNullSafe` (both extend AbstractConcurrentNullSafeMap)
  * **Root cause**: Used `ConcurrentHashMap.compute()` which holds a lock on the hash bin for the entire duration of the mapping function execution. Under high concurrency, threads pile up waiting for bin locks, causing hangs.
  * **Key insight**: The common case (no null values stored) doesn't need special sentinel handling during computation
  * **Fix**: Fast path now delegates directly to `ConcurrentHashMap.computeIfAbsent()` - this is lock-free for cache hits and only briefly locks for insertions. The slow path (key mapped to null, which is rare) uses optimistic locking with `putIfAbsent()`/`replace()`.
  * **Result**: Performance is now virtually identical to plain `ConcurrentHashMap` for typical use cases (caching, memoization, etc.)
* **TEST FIX**: `LRUCacheTest` - Added proper cleanup to prevent OutOfMemoryError during deployment tests
  * The `testSpeed` and `testCacheBlast` tests create 10M-entry caches (~800MB each)
  * Added `@AfterEach` cleanup that clears and nullifies caches, then suggests GC
  * Added explicit `cache.clear()` at the end of large tests to free memory before next test
* **IMPROVED**: Scheduler lifecycle management for `ThreadedLRUCacheStrategy` and `TTLCache`
  * Both classes now have proper shutdown methods that await termination (up to 5 seconds graceful, then 1 second forced)
  * Schedulers are now lazily recreatable - if shut down, creating new cache instances automatically restarts them
  * `ThreadedLRUCacheStrategy.shutdownScheduler()` - new static method for explicit cleanup
  * `TTLCache.shutdown()` - now returns boolean indicating clean termination, properly awaits termination
  * All schedulers use daemon threads, so they won't prevent JVM shutdown even without explicit cleanup
* **IMPROVED**: `ThreadedLRUCacheStrategy` - Complete algorithm redesign with zone-based eviction and sample-15 approximate LRU
  * **Memory guarantee**: Cache never exceeds 2x capacity, allowing predictable worst-case memory sizing
  * **Four-zone eviction strategy**:
    * Zone A (0 to 1x capacity): Normal operation, no eviction
    * Zone B (1x to 1.5x capacity): Background cleanup only (every 500ms)
    * Zone C (1.5x to 2x capacity): Probabilistic inline eviction (0% at 1.5x → 100% at 2x)
    * Zone D (2x+ capacity): Hard cap with evict-before-insert
  * **Sample-15 eviction**: Instead of sorting all entries O(n log n), samples 15 entries and evicts the oldest. Based on Redis research, this provides ~99% LRU accuracy with O(1) cost per eviction.
  * **No more sorting**: Eliminated all O(n log n) sort operations - entire algorithm is now O(1) for inline operations
  * **Concurrent race fix**: Zone D eviction now uses a while-loop to guarantee hard cap enforcement. Previously, multiple threads could all check `size < hardCap`, then all insert, causing unbounded growth (check-then-act race condition). Now inserts first, then loops eviction until under hard cap.
  * **Fixes deployment hang**: Heavy write load (500k unique keys into 5k cache) no longer causes hangs due to expensive sorting or unbounded cache growth

#### 4.80.0 - 2025-01-05
* **PERFORMANCE**: Cache repeated `getClass()` calls in hot paths
  * `ConcurrentNavigableMapNullSafe` - Cache `o1.getClass()` and `o2.getClass()` in comparator (6 calls → 2)
  * `CollectionUtilities` - Cache class lookups in deep copy operations for source, pair.source, and element objects
  * `GraphComparator` - Cache `srcValue.getClass()` and `targetValue.getClass()` in compare loop (7 calls → 2)
* **PERFORMANCE**: `ClassUtilities` and `GraphComparator` - Replace `isAssignableFrom(obj.getClass())` with `isInstance(obj)`
  * `ClassUtilities.java:2278` - Constructor parameter type matching now uses `isInstance()`
  * `GraphComparator.java:375` - Field type validation now uses `isInstance()`
  * `isInstance()` avoids the `getClass()` call, providing better performance on hot paths
* **FIX**: `ClassUtilities` - Fixed unsafe mode reentrancy issue with counter-based ThreadLocal approach
  * Previously, nested `setUseUnsafe(true)`/`setUseUnsafe(false)` calls would incorrectly disable unsafe mode for outer callers
  * Changed from `ThreadLocal<Boolean>` to `ThreadLocal<Integer>` counter (`unsafeDepth`)
  * `setUseUnsafe(true)` increments the counter, `setUseUnsafe(false)` decrements (but not below 0)
  * Unsafe mode is active when counter > 0, supporting proper nesting
  * Added comprehensive tests for nested calls, triple nesting, extra disables, and thread isolation
* **FIX**: `ThreadedLRUCacheStrategy` - Major concurrency fixes and architectural improvements:
  * **Architecture**: Replaced per-instance ScheduledFutures with single shared cleanup thread for all cache instances. Uses WeakReference registry allowing unused caches to be garbage collected. Dead references pruned automatically during iteration.
  * **Fix silent task death**: Wrapped cleanup in try-catch to prevent ScheduledExecutorService from silently cancelling the task on exceptions.
  * **Fix sort instability**: Capture timestamps to array before sorting to prevent `IllegalArgumentException: Comparison method violates its general contract` caused by concurrent timestamp updates during sort.
  * **Aggressive cleanup**: When cache exceeds 10x capacity, removes all excess items immediately instead of batching.
* **PERFORMANCE**: `AbstractConcurrentNullSafeMap.computeIfAbsent()` - Added fast-path check before locking
  * Original implementation always called `compute()` which locks the bucket even for cache hits
  * New implementation checks `get()` first - cache hits now bypass locking entirely
  * Significant reduction in lock contention under concurrent load
* **ADDED**: `computeIfAbsent()` and `putIfAbsent()` support to LRU cache strategies
  * `ThreadedLRUCacheStrategy` - atomic operations with timestamp updates
  * `LockingLRUCacheStrategy` - atomic operations with LRU reordering
  * `LRUCache` - delegation to underlying strategy

#### 4.72.0 - 2025-12-31
* **BUG FIX**: Fixed Jackson dependencies incorrectly declared without `<scope>test</scope>`. Jackson (jackson-databind, jackson-dataformat-xml) is only used for testing and should not be a transitive dependency. This restores java-util's zero external runtime dependencies.
* **BUG FIX**: Fixed `ThreadedLRUCacheStrategy` scheduled task accumulation. When `CaseInsensitiveMap.replaceCache()` was called multiple times, each new cache scheduled a purge task that was never cancelled. These orphaned tasks accumulated and could overwhelm the scheduler thread. Now:
  * `ThreadedLRUCacheStrategy.shutdown()` properly cancels the scheduled purge task
  * `LRUCache.shutdown()` delegates to the strategy's shutdown
  * `CaseInsensitiveMap.replaceCache()` calls shutdown on the old cache before replacing
* **UPDATED**: Test dependencies updated to latest versions:
  * jackson-databind: 2.17.2 → 2.20.1
  * jackson-dataformat-xml: 2.17.2 → 2.20.1

#### 4.71.0 - 2025-12-31
* **PERFORMANCE**: `MultiKeyMap.expandAndHash()` - Multiple optimizations for faster key processing:
  * **Fast-path for common leaf types**: Added early-exit check for common final types (String, Integer, Long, Double, Boolean, Short, Byte, Character, Float) using class identity comparison (`==`) instead of falling through the `instanceof` chain. Since these are the most frequent key types, checking them first skips the more expensive `instanceof` checks (Map, Set, Collection) for 80%+ of calls.
  * **Array detection optimization**: Replaced `isArray()` with `getComponentType() != null` for slight additional speedup.
  * **Set allocation elimination**: Eliminated per-element ArrayList allocation when processing Sets. Previously created N temporary ArrayLists and N `addAll()` calls for a Set with N elements. Now adds elements directly to result list.
  * **Benchmark impact**: 3 additional wins vs Apache Commons MultiKeyMap (29 wins vs 26 before), converting ties to wins with no new losses.
* **PERFORMANCE**: `MultiKeyMap.keysMatch()` - Removed redundant conditional check where both branches performed identical operations.
* **PERFORMANCE**: `Converter.getInheritedConverter()` - Added caching for inheritance pairs:
  * **cacheCompleteHierarchy**: Caches full type hierarchy including level 0
  * **cacheInheritancePairs**: Uses MultiKeyMap to cache sorted pairs per (source, target)
  * **InheritancePair class**: Holds cached pair data without instanceId
  * First call for each (source, target) pair builds and caches the sorted pairs. Subsequent calls use O(1) cache lookup + iteration over pre-sorted list.
* **IMPROVED**: `Converter.getSupportedConversions()` and `allSupportedConversions()` - Now correctly report all dynamic container conversions:
  * Array/Collection conversions: Object[] ↔ Collection, array to array
  * Enum conversions: Array/Collection/Map to Enum (creates EnumSet), EnumSet to Collection/Object[]
  * `isConversionSupportedFor()`: Added optimistic handling for Object[] source component type - returns true when source component is Object.class (can't know actual element types at compile time)
* **CLEANUP**: `FastReader` - Removed unused extended API methods that were never utilized by json-io:
  * Deprecated: `getLine()` and `getCol()` now return 0 (line/column tracking removed for performance)
  * Removed line/column tracking overhead from hot path
  * `getLastSnippet()` provides error context without per-character tracking cost

#### 4.70.0 - 2025-11-18

> * **ADDED**: `RegexUtilities` - New utility class providing thread-safe pattern caching and ReDoS (Regular Expression Denial of Service) protection for Java regex operations. This class addresses two critical concerns in regex-heavy applications:
>   * **Pattern Caching**: Thread-safe ConcurrentHashMap-based caching of compiled Pattern objects to eliminate redundant Pattern.compile() overhead. Supports three caching strategies:
>     * **Standard patterns**: `getCachedPattern(String)` - Most common case
>     * **Case-insensitive patterns**: `getCachedPatternCaseInsensitive(String)` - Dedicated cache for CASE_INSENSITIVE flag
>     * **Custom flags**: `getCachedPattern(String, int)` - Supports any combination of Pattern flags (MULTILINE, DOTALL, etc.)
>   * **ReDoS Protection**: Timeout-based regex execution prevents catastrophic backtracking attacks that cause CPU exhaustion. All safe* methods use configurable timeouts (default 5000ms) with shared ExecutorService for high-performance operation:
>     * `safeMatches(Pattern, String)` - Timeout-protected pattern matching
>     * `safeFind(Pattern, String)` - Returns SafeMatchResult with captured groups
>     * `safeReplaceFirst(Pattern, String, String)` - Safe replacement operations
>     * `safeReplaceAll(Pattern, String, String)` - Safe global replacement
>     * `safeSplit(Pattern, String)` - Safe string splitting
>   * **Invalid Pattern Handling**: Returns null for malformed patterns instead of throwing exceptions, with patterns cached to avoid repeated compilation attempts
>   * **Performance**: Shared CachedThreadPool ExecutorService eliminates per-operation thread creation overhead (previously caused 74% performance degradation, now zero overhead)
>   * **Configuration**: System property controls via cedarsoftware.security.enabled, cedarsoftware.regex.timeout.enabled, cedarsoftware.regex.timeout.milliseconds
>   * **Observability**: `getPatternCacheStats()` provides cache metrics (total patterns, invalid patterns, cache sizes per strategy)
>   * **Thread Safety**: All operations are thread-safe with daemon threads to prevent JVM shutdown blocking
>   * **Test Coverage**: Comprehensive test suite with 17,807 tests passing, including pattern caching verification, timeout protection, and invalid pattern handling
>   * **Use Cases**: Prevents regex-based DoS attacks, improves performance for frequently-used patterns, provides unified regex API across Cedar Software projects (java-util, json-io, n-cube)
> * **PERFORMANCE**: `FastReader` - Multiple performance and reliability optimizations:
>   * **Made class `final`**: Prevents subclassing and enables JVM optimizations (method inlining, devirtualization)
>   * **Inlined `movePosition()` in hot path**: Eliminated ~1.5M method calls per MB of JSON by inlining line/column tracking directly in `read()` and `read(char[], int, int)` methods
>   * **Improved EOF handling**: Added early-exit check (`if (limit == -1) return;`) in `fill()` to avoid redundant read attempts after EOF
>   * **Optimized `read(char[], int, int)`**: Reduced local variable allocations and improved loop efficiency by hoisting position/offset updates outside inner loops
>   * **Fixed pushback tracking**: Corrected line/column position reversal when characters are pushed back (changed `0x0a` to `'\n'` for clarity)
>   * **Pre-sized StringBuilder**: `getLastSnippet()` now pre-allocates capacity to avoid internal array resizing
>   * **Increased default buffer sizes**: Changed from 10 to 16 pushback buffers (60% more capacity) for better handling of complex tokenization scenarios
>   * **Impact**: These micro-optimizations compound in json-io's parsing hot path where `FastReader` methods are called millions of times per large JSON file
> * **IMPROVED**: `StringConversions.toPattern()` - Updated to use `RegexUtilities.getCachedPattern()` for pattern caching and ReDoS protection. The Converter framework's String → Pattern conversion now benefits from:
>   * **Pattern Caching**: Eliminates redundant Pattern.compile() calls when same pattern string is converted multiple times
>   * **ReDoS Protection**: Timeout-protected compilation prevents malicious regex patterns from causing CPU exhaustion
>   * **Invalid Pattern Handling**: Returns clear IllegalArgumentException for invalid patterns instead of propagating PatternSyntaxException
>   * **Backward Compatibility**: All existing Pattern conversion tests pass (5 tests in PatternConversionsTest)
>   * **Performance**: Same pattern string returns cached instance on subsequent conversions
> * **PERFORMANCE**: `DataGeneratorInputStream.withRandomBytes()` - **~7x faster** via two synergistic optimizations (profiler-verified):
>   * **Optimization 1 - Unbounded nextInt()**: Use `random.nextInt() & 0xFF` instead of `random.nextInt(256)`. Profiler measurements: 4,410ms → 2,517ms (**1.75x speedup, 43% faster**). Unbounded nextInt() avoids internal bound-checking overhead in Random class.
>   * **Optimization 2 - Byte extraction batching**: Extract 4 bytes from each Random call using bit shifting instead of 1 byte per call. Reduces Random method calls by 75% (4x reduction). Expected additional 4x speedup: 2,517ms → ~630ms.
>   * **Combined impact**: **~7x total speedup** (4,410ms → ~630ms) by multiplying both optimizations (1.75x × 4x ≈ 7x)
>   * **Implementation**: Uses bit masking (`buffer & 0xFF`) and unsigned right shift (`buffer >>>= 8`) to extract 4 bytes sequentially from 32-bit int. Maintains internal buffer with `bytesRemaining` counter.
>   * **Branch elimination**: Creates two separate IntSupplier implementations (one for includeZero=true, one for false) instead of checking flag on every `getAsInt()` call.
>   * **Random instance lifecycle**: Created once per stream (not in factory method), following same pattern as withRepeatingPattern() and withSequentialBytes()
>   * **Deterministic**: Maintains exact same random sequences for same seeds - all DataGeneratorInputStream tests pass with identical output
> * **PERFORMANCE**: `MultiKeyMap` stripe lock count increased for better concurrency under high load:
>   * **Changed formula**: `cores * 2` instead of `cores / 2` (4x more stripes on typical systems)
>   * **Increased max**: 128 stripes (was 32) to support high-core-count servers (64+ cores)
>   * **12-core example**: 8 stripes → 32 stripes (4x more parallelism)
>   * **Impact**: Profiler showed severe lock contention in `putInternal()` - 6,588ms blocked waiting for locks. With 4x more stripes, contention drops dramatically as threads distribute across more locks.
>   * **Rationale**: Matches ConcurrentHashMap's DEFAULT_CONCURRENCY_LEVEL approach (concurrency = cores or higher) for optimal write parallelism
>   * **Backward compatible**: Auto-tuned based on CPU cores, no API changes
> * **PERFORMANCE**: `ConcurrentList.size()` and `isEmpty()` made O(1) lock-free using dedicated size counter:
>   * **Problem**: Profiler flame graph showed `size()` as prominent hot spot. Previous implementation used `readLock` around `tail.get() - head.get()`, causing lock acquisition overhead on frequently-called methods.
>   * **Solution**: Added dedicated `AtomicLong sizeCounter` maintained by all add/remove operations. `size()` and `isEmpty()` now just read the counter - no locks, no calculations.
>   * **Previous**: `readLock.lock()` → `tail - head` → `readLock.unlock()` (lock overhead + two volatile reads)
>   * **New**: `sizeCounter.get()` (single volatile read, no locks)
>   * **Correctness**: Initially attempted to remove readLock from `tail - head` calculation, but Claude Sonnet 4.5 correctly identified thread-safety issue (inconsistent snapshots could cause negative sizes). Dedicated counter solves this properly.
>   * **Overhead**: Adds one `incrementAndGet()`/`decrementAndGet()` to add/remove operations, but these already have locks or atomic operations, so impact is minimal compared to massive speedup of lock-free `size()`.
>   * **Impact**: Eliminates readLock acquisition for size queries (called 100+ times across codebase), reduces lock contention, improves scalability
>   * All ConcurrentList tests pass (including concurrency stress tests)
> * **TEST PERFORMANCE**: `ConcurrentList2Test` - Multiple optimizations eliminating 88+ seconds and reducing random number overhead:
>   * **Problem 1**: Found two lines computing `int start = random.nextInt(random.nextInt(list.size()))` - nested calls taking 44+ seconds each, but `start` variable was unused (gray in IDE).
>   * **Fix 1**: Removed unused random number generation from iterator/listIterator test threads.
>   * **Problem 2**: Using `SecureRandom` instead of `Random` (SecureRandom is much slower, unnecessary for tests).
>   * **Fix 2**: Changed to regular `Random` - tests don't need cryptographic randomness.
>   * **Problem 3**: Using `random.nextInt(bound)` which profiler showed is 1.75x slower than unbounded `nextInt()`.
>   * **Fix 3**: Changed to unbounded `nextInt()` with bit masking/modulo: `random.nextInt() % 3`, `(random.nextInt() & 0x3FF) + 1000`, etc.
>   * **Problem 4**: Unused `subListRunnable` defined but never executed (dead code).
>   * **Fix 4**: Removed unused runnable and associated imports.
>   * **Impact**: Eliminates 88+ seconds of wasted CPU time (44.27s + 44.22s), plus significant reduction in modifier thread overhead from faster random generation. Same test coverage, much faster execution.
> * **IMPROVED**: `IOUtilities` transfer methods now return byte counts - All `transfer*()` methods now return the number of bytes transferred instead of void, enabling callers to verify transfer completion and track progress:
>   * **Methods returning `long`**: `transfer(InputStream, OutputStream)`, `transfer(InputStream, OutputStream, callback)`, `transfer(File, OutputStream)`, `transfer(InputStream, File, callback)`, `transfer(URLConnection, File, callback)`, `transfer(File, URLConnection, callback)` - Use `long` to support files and streams larger than 2GB (Integer.MAX_VALUE)
>   * **Methods returning `int`**: `transfer(InputStream, byte[])`, `transfer(URLConnection, byte[])` - Use `int` since Java arrays are bounded by Integer.MAX_VALUE
>   * **Backward compatible**: Existing code that ignores return values continues to work unchanged
>   * **Test coverage**: Added 6 comprehensive tests using `DataGeneratorInputStream` to verify byte counts for transfers ranging from 0 bytes to 5MB, including callback verification and pattern validation
>   * **Use cases**: Enables verification of complete transfers, progress tracking for large files, detecting truncated transfers, and audit logging of transfer operations
> * **FIXED**: `DeepEquals` - Fixed 7 critical and high-severity thread safety and reliability issues found via comprehensive adversarial code review:
>   * **Deep recursion StackOverflowError** - Recursive deepEquals calls now inherit remaining depth budget instead of resetting to full budget, preventing unbounded recursion when custom equals() methods trigger nested comparisons. Added budget inheritance logic that passes (maxDepth - currentDepth) to recursive calls.
>   * **Hash/equals contract violation** - Fixed hashDouble() and hashFloat() to maintain contract that equal values have equal hashes. Changed quantization from 1e12 to 1e10 (100× coarser) to match epsilon-based equality tolerance (1e-12), preventing HashMap/HashSet storage failures. Trade-off: ~40% hash collisions for values 2-10× epsilon apart (acceptable - narrow band).
>   * **ThreadLocal memory leak** - Added try-finally blocks with ThreadLocal.remove() in deepHashCode() entry point to prevent memory leaks in long-running applications, especially those using thread pools where threads are reused.
>   * **Unbounded memory allocation** - Moved depth budget tracking from options Map to separate ThreadLocal stack, making options Map stable and reusable. Reduced HashMap allocations from 500,000 to ~2 for 1M-node graphs (500,000× improvement), preventing OutOfMemoryError with large object graphs.
>   * **SimpleDateFormat race condition** - Replaced `ThreadLocal<SimpleDateFormat>` with SafeSimpleDateFormat for date formatting in diff output. SafeSimpleDateFormat provides copy-on-write semantics and per-thread LRU cache, preventing corrupted date formatting from re-entrant callbacks.
>   * **formattingStack re-entrancy** - Changed formattingStack from `ThreadLocal<Set<Object>>` to `ThreadLocal<Deque<Set<Object>>>` (stack of Sets), where each top-level formatValue() call gets its own Set for circular reference detection. Prevents false `<circular Object>` detection when re-entrant deepEquals calls format the same object in different contexts.
>   * **Unsafe visited set publication** - Replaced HashSet with ConcurrentSet for visited set tracking. ConcurrentSet uses weakly consistent iterators (backed by ConcurrentHashMap) that never throw ConcurrentModificationException, providing fail-safe behavior instead of fail-fast when inputs are modified concurrently.
>   * **Test coverage**: Added 37 comprehensive tests across 6 new test classes verifying all fixes. All 17,726 existing tests pass with zero regressions.
>   * **Performance**: Minimal overhead for normal usage, with massive improvements for edge cases (500,000× fewer allocations for large graphs, 100 MB → 400 bytes memory usage)
>   * **Backward compatibility**: 100% backward compatible - all public APIs unchanged, behavior identical for normal usage patterns

#### 4.3.0 - 2025-11-07

> * **ADDED**: `DataGeneratorInputStream` - A flexible, memory-efficient `InputStream` that generates data on-the-fly using various strategies without consuming memory to store the data. This class is ideal for testing code that handles large streams, generating synthetic test data, or creating pattern-based input. Supports multiple generation modes:
>   * **Random bytes**: Generates random bytes (0-255) with optional seed for reproducible tests. Can exclude zero bytes if needed for specific testing scenarios
>   * **Repeating patterns**: Repeats a string or byte array pattern cyclically (e.g., "ABC" → ABCABCABC...)
>   * **Constant byte**: Outputs the same byte value repeatedly for simple fill patterns
>   * **Sequential bytes**: Counts sequentially between two byte values with automatic wrap-around. Supports both ascending (10→20) and descending (20→10) sequences
>   * **Random strings**: Generates random proper-case alphabetic strings (like "Xkqmz Pqwer Fgthn") using `StringUtilities.getRandomString()`, separated by configurable delimiters
>   * **Custom generator**: Accepts any `IntSupplier` lambda for complete flexibility
>   * All methods use static factory pattern (`withRandomBytes()`, `withRepeatingPattern()`, etc.) for clear, readable code
>   * Zero memory overhead - data is generated on-demand and immediately discarded, enabling efficient testing with TB+ scale streams
>   * Thread-safe read operations with proper `InputStream` contract compliance
>   * Full JavaDoc with comprehensive examples for each generation mode

#### 4.2.0 - 2025-11-02

> * **FIXED**: `MultiKeyMap` nested Set lookup bug in COLLECTIONS_EXPANDED mode - Fixed size mismatch false negatives when looking up keys containing expanded Collections. In COLLECTIONS_EXPANDED mode, stored keys have expanded size (includes SET_OPEN/SET_CLOSE markers) while lookup keys have un-expanded Collection size. Added skipSizeCheck logic to bypass size comparison for Collection-to-Collection matches in expanded mode, allowing compareCollections() to handle the structural comparison correctly. This fixes lookups failing incorrectly when using nested Sets or Collections as multi-keys.
>
> * **IMPROVED**: Code quality improvements from comprehensive IntelliJ IDEA inspection analysis (17 fixes across 5 classes):
>   * **MultiKeyMap**: Improved comment precision (arity → size), enhanced Javadoc clarity, optimized variable declarations for better readability
>   * **StringUtilities**: Enhanced null safety with explicit checks, improved loop variable scoping, added type casting safety guards, optimized string concatenation patterns
>   * **ConcurrentList**: Improved synchronization block granularity, enhanced iterator safety, optimized size calculations with better caching
>   * **ClassUtilities**: Reduced cognitive complexity in findClosest(), improved exception handling clarity, enhanced method parameter validation
>   * **CaseInsensitiveMap**: Optimized keySet() and values() operations, improved type safety in internal operations, enhanced edge case handling
>   * All changes maintain 100% backward compatibility while improving code maintainability and reducing potential edge case issues
>
> * **FIXED**: Map and Set hashCode() contract compliance - Removed incorrect `EncryptionUtilities.finalizeHash()` calls from 6 classes that violated the Map and Set interface contracts. The Map contract requires `hashCode() = sum of entry hashCodes`, and the Set contract requires `hashCode() = sum of element hashCodes`. Using finalizeHash() broke the Object.hashCode() contract (equal objects must have equal hashCodes) and caused HashSet/HashMap storage failures. Fixed classes: `AbstractConcurrentNullSafeMap`, `TTLCache`, `LockingLRUCacheStrategy`, `ThreadedLRUCacheStrategy`, `ConcurrentSet`, `ClassValueSet`.
>
> * **CHANGED**: `IOUtilities` close/flush methods now throw exceptions as unchecked - **Breaking behavioral change**: All `close()` and `flush()` methods in `IOUtilities` (for `Closeable`, `Flushable`, `XMLStreamReader`, `XMLStreamWriter`) now throw exceptions as unchecked via `ExceptionUtilities.uncheckedThrow()` instead of silently swallowing them. This change provides:
>   * **Better diagnostics**: Close/flush failures are now visible rather than silently hidden
>   * **Cleaner code**: No try-catch required at call sites - works seamlessly in finally blocks
>   * **Early problem detection**: Infrastructure issues (disk full, network failures, resource exhaustion) surface immediately
>   * **Caller flexibility**: Exceptions can still be caught higher in the call stack if desired
>   * **Important**: While close/flush exceptions are rare, when they occur they often indicate serious issues that should be diagnosed rather than hidden. This change makes java-util consistent with its existing philosophy of throwing checked exceptions as unchecked (see `transfer()`, `compressBytes()`, etc. which already use this pattern).
>
> * **ADDED**: Geometric primitives in dedicated `geom` package - The 5 AWT-replacement classes (`Point`, `Rectangle`, `Dimension`, `Insets`, `Color`) are organized in `com.cedarsoftware.util.geom` following Java's package organization pattern (`java.awt.geom`). This provides:
>   * **Clear organization**: Geometric/graphical primitives grouped separately from general utilities
>   * **Enhanced documentation**: All classes prominently state "Zero-dependency - No java.desktop/java.awt required" with emphasis on headless server/microservices use
>   * **Full module support**: Package exported via both JPMS module descriptor and OSGi MANIFEST
>
> * **FIXED**: Added missing `cache` package to JPMS and OSGi exports - The `com.cedarsoftware.util.cache` package (containing `LockingLRUCacheStrategy` and `ThreadedLRUCacheStrategy`) was not exported in module descriptors. Added `exports com.cedarsoftware.util.cache;` to moditect configuration and OSGi Export-Package directive. This ensures the cache package is properly accessible to both JPMS modules and OSGi bundles.
>
> * **IMPROVED**: Added comprehensive cloud-native and containerization documentation to README - Added prominent "Cloud Native & Container Ready" section highlighting java-util's advantages for modern cloud deployments:
>   * **Platform badges**: AWS, Azure, GCP, Kubernetes, Docker compatibility
>   * **Container optimization**: Minimal footprint (~1.1MB total, 85% smaller than Guava), zero dependencies, fast startup optimized for serverless/FaaS
>   * **Deployment guide**: Platform-specific advantages for AWS Lambda/ECS/EKS, Azure Functions/AKS, GCP Cloud Run/GKE, Kubernetes, Docker
>   * **Performance examples**: Dockerfile showing 50% image size reduction, Kubernetes YAML demonstrating lower resource requests
>   * **Serverless ready**: Explicit callouts for Lambda, Cloud Functions, Cloudflare Workers, edge computing
>   * **Enterprise security**: Minimal attack surface, no Log4Shell exposure, SOC 2/FedRAMP/PCI-DSS compliance benefits
>
> * **REMOVED**: java.awt/java.desktop dependency eliminated - Created 5 Cedar DTO classes (`Color`, `Dimension`, `Point`, `Rectangle`, `Insets`) to replace java.awt equivalents, completely removing the java.desktop module dependency. This enables:
>   * **Headless deployment**: No display system required - ideal for servers, containers, and cloud platforms
>   * **Smaller footprint**: Eliminates 100MB+ java.desktop module from runtime
>   * **Cloud-ready**: Compatible with AWS Lambda, GraalVM native-image, Docker distroless images
>   * **Faster startup**: 2-3x improvement without loading AWT/Swing infrastructure
>   * **Reduced attack surface**: Removes entire GUI subsystem from security considerations
>   * **AWT-compatible API**: Cedar DTOs use identical method signatures (getRed(), getWidth(), etc.) for seamless migration
>   * **Backward-compatible parsing**: StringConversions accepts both "Dimension[...]" and "java.awt.Dimension[...]" formats for existing serialized data
>   * **Java 8 compatible**: Uses final classes with private fields (not Records), maintaining Java 8 baseline while enabling future Record migration (Java 17+)
>
> * **PERFORMANCE**: Zero-allocation multi-key lookups with ThreadLocal arrays - Added explicit overloads for `getMultiKey(k1, k2)` through `getMultiKey(k1..k5)` and `containsMultiKey(k1, k2)` through `containsMultiKey(k1..k5)` that use ThreadLocal<Object[]> arrays (one per size: LOOKUP_KEY_2 through LOOKUP_KEY_5). Eliminates varargs array allocation on every multi-key lookup call. For lookup-only operations, the ThreadLocal arrays are reused per thread and only used for comparison (never stored), providing zero-allocation lookups for the most common 2-5 key cases. Expected to improve MultiKeyMap performance vs Apache Commons MultiKeyMap in benchmark scenarios.
>
> * **PERFORMANCE**: Simplified `Converter` cache lookups using MultiKeyMap's ThreadLocal optimization - Refactored `getCachedConverter()` to use `FULL_CONVERSION_CACHE.getMultiKey(source, target, instanceId)` directly, eliminating Converter's own ThreadLocal<Object[3]>. Leverages MultiKeyMap's internal LOOKUP_KEY_3 ThreadLocal for zero-allocation lookups. Cleaner code (removed redundant ThreadLocal, simplified getCachedConverter from 8 lines to 4) with identical performance - MultiKeyMap's getMultiKey() provides the same ~26% speedup over varargs.
>
> * **FIXED**: `MultiKeyMap` collection key handling in COLLECTIONS_NOT_EXPANDED mode - Fixed two critical issues: (1) keysMatch() now uses collection.equals() instead of element-by-element comparison for proper equality semantics across different Collection implementations (e.g., Arrays.ArrayList vs Collections.UnmodifiableRandomAccessList), (2) entrySet() now preserves original collection types instead of reconstructing them, preventing hash code mismatches after deserialization. These fixes ensure collection keys can be looked up correctly after serialization/deserialization cycles.
>
> * **REMOVED**: `MultiKeyMap.entries()`, `MultiKeyEntry`, and `EntryIterator` - Removed deprecated `entries()` method that exposed internal flattened key structure with markers. All code now uses standard `entrySet()` which returns keys as native List/Set/single structures suitable for serialization. This cleanup:
>   * Eliminates 3 unused public methods: `externalizeNulls()`, `externalizeMarkers()`, `internalizeMarkers()`
>   * Removes 2 classes: public `MultiKeyEntry` and private `EntryIterator`
>   * Refactors internal methods (`keySet()`, `values()`, `hashCode()`, `toString()`) to use `entrySet()` or direct bucket iteration
>   * Migrates all tests and `Converter` to use `entrySet()` instead of deprecated `entries()`
>
> * **REMOVED**: `MultiKeyMap` reconstruct method consolidation - Consolidated multiple reconstruct methods into a single clean `reconstructKey()` method:
>   * Removed `reconstructKeyForSerialization()` redundant wrapper method
>   * Removed deprecated `reconstructKey()` that returned Object[] arrays
>   * Removed `collectElements()`, `keyView()`, `externalizeAndWrapKey()`, and `externalizeKey()` helper methods
>   * Renamed `reconstructKeyToNative()` → `reconstructKey()` as the single public key reconstruction method
>   * Updated json-io's `MultiKeyMapFactory` to remove old format handling with `internalizeMarkers()`
>
> * **FIXED**: `MultiKeyMap` critical NULL_SENTINEL equality bug - Fixed `elementEquals()` to properly check equality after normalizing NULL_SENTINEL to null. Previously, NULL_SENTINEL and null were incorrectly treated as unequal, breaking null key handling.
>
> * **FIXED**: `ConcurrentList` pollFirst()/pollLast() lock usage - Changed pollFirst() and pollLast() to use write locks instead of read locks. Previously they used read locks while modifying data (setting bucket elements to null), creating a race condition with toArray() which also uses read locks. This caused toArray() to see "phantom nulls" from concurrent removals. With write locks, poll operations are properly synchronized with read operations, allowing toArray() to safely preserve legitimate null values.
>
> * **IMPROVED**: `MultiKeyMap` performance optimizations:
>   * **Removed duplicate getClass() calls**: Eliminated redundant `getClass()` and `isArray()` calls in `flattenKey()` by declaring variables once and reusing across code paths
>   * **Optimized instanceof checks**: Deferred rare atomic array type checks until after confirming key is an array, eliminating 9 redundant instanceof checks across 3 hot-path methods (normalizeForLookup, findSimpleOrComplexKey, createMultiKey)
>   * **Increased Set comparison threshold**: Changed from 3 to 6 elements for nested O(n²) comparison vs HashMap allocation. Benchmarking shows 36 comparisons (6²) is faster than HashMap overhead, improving Set/List performance ratio from 6.15x to 4.8x (22% improvement)
>
> * **IMPROVED**: `MultiKeyMap` capacity and size handling:
>   * **Switched to AtomicLong for size tracking**: Migrated from AtomicInteger to AtomicLong to support maps with billions of entries (beyond 2³¹-1 limitation) with zero performance impact
>   * **Added longSize() method**: Returns true size as `long` without Integer.MAX_VALUE cap, enabling accurate size reporting for very large maps
>   * **Enhanced size() documentation**: Documents Integer.MAX_VALUE capping behavior and directs users to longSize() for maps exceeding 2³¹-1 entries
>
> * **IMPROVED**: `MultiKeyMap` documentation enhancements:
>   * **Fixed misleading volatile read comments**: Corrected 5 instances where comments incorrectly stated `table.length()` was a volatile read (array lengths are immutable). Comments now accurately describe that only the `buckets` reference read is volatile
>   * **Documented Map contract violations**: Added explicit documentation that `entrySet()`, `keySet()`, and `values()` return snapshots (not live views), explaining the rationale and performance trade-offs of snapshot semantics in concurrent contexts
>   * **Added Big-O complexity documentation**: New class-level section documenting performance characteristics (O(k) for get/put/remove, O(1) for size, O(n) for snapshots) with clear explanations of complexity variables
>   * **Added capacity/size limits documentation**: Comprehensive section explaining AtomicLong usage, Integer.MAX_VALUE capping in size(), memory requirements (~200-300GB for 2³¹ entries), and feasibility on modern servers
>   * **Enhanced Set key examples**: Added detailed examples showing Sets combined with Object[] and List keys, demonstrating order-agnostic Set matching vs order-dependent List matching in multi-dimensional keys
>   * **Removed commented-out code**: Eliminated 6 commented AWT/Swing array type references to reduce code clutter and avoid JPMS/OSGi headless deployment confusion
>
> * **IMPROVED**: `MultiKeyMap` performance optimizations for `equals()` and `hashCode()`:
>   * **Optimized equals() implementation**: Refactored to walk the OTHER map and query THIS map using `get()`, eliminating unnecessary key reconstruction on our side. Reduces work by 50% and eliminates all extra memory allocations during equality checks
>   * **Added hashCode() caching**: Implemented cached hashCode with invalidation on mutations (put, remove, clear). First call computes O(n*k), subsequent calls are O(1). Provides massive speedup for maps used in HashSets or as keys in other maps
>
> * **IMPROVED**: `MultiKeyMap` toString() now uses distinct notation for Lists vs Sets:
>   * Lists use square brackets `[1, 2, 3]` (order-sensitive)
>   * Sets use curly braces `{4, 5, 6}` (order-agnostic)
>   * Mixed keys clearly show both: `🆔 [1, 2, 3], {4, 5, 6} → 🟣 value`
>   * Nested structures properly display with appropriate delimiters
>
> * **ADDED**: `MultiKeyMap` json-io serialization support - `MultiKeyMap` instances can now be serialized and deserialized using json-io, preserving complex multi-dimensional keys including Lists, Sets, Arrays, and null values. Keys are serialized in their native structures (not flattened arrays), making the JSON output clean and human-readable. This enables `MultiKeyMap` use in distributed caching, persistent storage, REST APIs, and microservices communication
>
> * **ADDED**: Comprehensive test coverage for MultiKeyMap equals(), hashCode(), and toString() functionality:
>   * `MultiKeyMapEqualsHashCodeTest`: 25 tests verifying equals/hashCode contracts with mixed List/Set keys
>   * `MultiKeyMapToStringTest`: 21 tests verifying correct List/Set notation in output
>   * `MultiKeyMapMixedListSetTest`: 16 tests verifying order-sensitive List and order-agnostic Set matching

#### 4.1.0

> * **FIXED**: `ClassUtilities.setUseUnsafe()` is now thread-local instead of global, preventing race conditions in multi-threaded environments where concurrent threads need different unsafe mode settings
> 
> * **IMPROVED**: `ClassUtilities` comprehensive improvements from GPT-5 review:
>
>   **🔒 SECURITY FIXES:**
>   * **Enhanced class loading security with additional blocked prefixes**: Added blocking for `jdk.nashorn.` package to prevent Nashorn JavaScript engine exploitation; added blocking for `java.lang.invoke.MethodHandles$Lookup` class which can open modules reflectively and bypass security boundaries
>   * **Added percent-encoded path traversal blocking**: Enhanced resource path validation to block percent-encoded traversal sequences (%2e%2e, %2E%2E, etc.) before normalization; prevents bypass attempts using URL encoding
>   * **Enhanced resource path security**: Added blocking of absolute Windows drive paths (e.g., "C:/...", "D:/...") in resource loading to prevent potential security issues
>   * **Enhanced security blocking**: Added package-level blocking for `javax.script.*` to prevent loading of any class in that package
>   * **Added belt-and-suspenders alias security**: addPermanentClassAlias() now validates classes through SecurityChecker.verifyClass() to prevent aliasing to blocked classes
>   * **Fixed security bypass in cache hits**: Alias and cache hits now properly go through SecurityChecker.verifyClass() to prevent bypassing security checks
>   * **Updated Unsafe permission check**: Replaced outdated "accessClassInPackage.sun.misc" permission with custom "com.cedarsoftware.util.enableUnsafe" permission appropriate for modern JDKs
>   * **Simplified resource path validation**: Removed over-eager validation that blocked legitimate resources, focusing on actual security risks (.., null bytes, backslashes)
>   * **Improved validateResourcePath() precision**: Made validation more precise - now only blocks null bytes, backslashes, and ".." path segments (not substrings), allowing legitimate filenames like "my..proto"
>
>   **⚡ PERFORMANCE OPTIMIZATIONS:**
>   * **Optimized constructor matching performance**: Eliminated redundant toArray() calls per constructor attempt by converting collection to array once
>   * **Optimized resource path validation**: Replaced regex pattern matching with simple character checks, eliminating regex engine overhead
>   * **Optimized findClosest() performance**: Pull distance map once from ClassHierarchyInfo to avoid repeated computeInheritanceDistance() calls
>   * **Optimized findLowestCommonSupertypesExcluding performance**: Now iterates the smaller set when finding intersection
>   * **Optimized findInheritanceMatches hot path**: Pre-cache ClassHierarchyInfo lookups for unique value classes
>   * **Optimized loadClass() string operations**: Refactored JVM descriptor parsing to count brackets once upfront, reducing string churn
>   * **Optimized hot-path logging performance**: Added isLoggable() guards to all varargs logging calls to prevent unnecessary array allocations
>   * **Optimized getParameters() calls**: Cached constructor.getParameters() results to avoid repeated allocations
>   * **Optimized buffer creation**: Cached zero-length ByteBuffer and CharBuffer instances to avoid repeated allocations
>   * **Optimized trySetAccessible caching**: Fixed to actually use its accessibility cache, preventing repeated failed setAccessible() attempts
>   * **Added accessibility caching**: Implemented caching for trySetAccessible using synchronized WeakHashMap for memory-safe caching
>   * **Prevented zombie cache entries**: Implemented NamedWeakRef with ReferenceQueue to automatically clean up dead WeakReference entries
>
>   **🐛 BUG FIXES:**
>   * **Fixed interface depth calculation**: Changed ClassHierarchyInfo to use max BFS distance instead of superclass chain walking
>   * **Fixed tie-breaking for common supertypes**: Changed findLowestCommonSupertypesExcluding to sort by sum of distances from both classes
>   * **Fixed JPMS SecurityException handling**: Added proper exception handling for trySetAccessible calls under JPMS
>   * **Fixed nameToClass initialization inconsistency**: Added "void" type to static initializer and included common aliases in clearCaches()
>   * **Fixed tie-breaker logic**: Corrected shouldPreferNewCandidate() to properly prefer more specific types
>   * **Fixed areAllConstructorsPrivate() for implicit constructors**: Method now correctly returns false for classes with no declared constructors
>   * **Fixed mutable buffer sharing**: ByteBuffer, CharBuffer, and array default instances are now created fresh on each call
>   * **Fixed inner class construction**: Inner class constructors with additional parameters beyond enclosing instance are now properly matched
>   * **Fixed varargs ArrayStoreException vulnerability**: Added proper guards when packing values into varargs arrays
>   * **Fixed named-parameter gating**: Constructor parameter name detection now checks ALL parameters have real names
>   * **Fixed Currency default creation**: Currency.getInstance(Locale.getDefault()) now gracefully falls back to USD
>   * **Fixed generated-key Map ordering**: Fixed bug where Maps with generated keys could inject nulls when keys had gaps
>   * **Fixed loadResourceAsBytes() leading slash handling**: Added fallback to strip leading slash when ClassLoader.getResourceAsStream() fails
>   * **Fixed OSGi class loading consistency**: OSGi framework classes now loaded using consistent classloader
>   * **Fixed ClassLoader key mismatch**: Consistently resolve null ClassLoader to same instance
>   * **Fixed computeIfAbsent synchronization**: Replaced non-synchronized computeIfAbsent with properly synchronized getLoaderCache()
>   * **Fixed off-by-one in class load depth**: Now validates nextDepth instead of currentDepth
>   * **Fixed OSGi/JPMS classloader resolution**: Simplified loadClass() to consistently use getClassLoader() method
>   * **Fixed permanent alias preservation**: Split aliases into built-in and user maps so clearCaches() preserves user-added permanent aliases
>   * **Fixed removePermanentClassAlias loader cache invalidation**: Both add and remove methods now properly clear per-loader cache entries
>   * **Fixed findLowestCommonSupertypesExcluding NPE**: Added null-check for excluded parameter
>   * **Fixed ArrayStoreException in matchArgumentsWithVarargs**: Added final try-catch guard for exotic conversion edge cases
>   * **Fixed OSGi loader cache cleanup**: clearCaches() now properly clears the osgiClassLoaders cache
>   * **Fixed OSGi cache NPE**: Fixed potential NullPointerException in getOSGiClassLoader() when using computeIfAbsent()
>   * **Fixed incorrect comment**: Updated accessibilityCache comment to correctly state it uses Collections.synchronizedMap
>
>   **🎯 API IMPROVEMENTS:**
>   * **Added boxing support in computeInheritanceDistance()**: Primitive types can now reach reference types through boxing
>   * **Added primitive widening support**: Implemented JLS 5.1.2 primitive widening conversions (byte→short→int→long→float→double)
>   * **Added Java-style array support**: loadClass() now supports Java-style array names like "int[][]" and "java.lang.String[]"
>   * **Added varargs constructor support**: Implemented proper handling for varargs constructors
>   * **Enhanced varargs support with named parameters**: newInstanceWithNamedParameters() now properly handles varargs parameters
>   * **Improved API clarity for wrapper types**: Changed getArgForType to only provide default values for actual primitives
>   * **Improved API clarity**: Renamed defaultClass parameter to defaultValue in findClosest() method
>   * **Fixed API/docs consistency for null handling**: All primitive/wrapper conversion methods now consistently throw IllegalArgumentException
>   * **Added null safety**: Made doesOneWrapTheOther() null-safe, returning false for null inputs
>   * **Added cache management**: Added clearCaches() method for testing and hot-reload scenarios
>   * **Added deterministic Map fallback ordering**: When constructor parameter matching falls back to Map.values() and Map is HashMap, values are sorted alphabetically
>   * **Implemented ClassLoader-scoped caching**: Added WeakHashMap-based caching with ClassLoader keys and WeakReference values
>
>   **📚 DOCUMENTATION & CLEANUP:**
>   * **Updated documentation**: Enhanced class-level Javadoc and userguide.md to accurately reflect all public methods
>   * **Documented Map ordering requirement**: Added documentation to newInstance() methods clarifying LinkedHashMap usage
>   * **Improved documentation clarity**: Updated computeInheritanceDistance() documentation to clarify caching
>   * **Added comprehensive edge case test coverage**: Created ClassUtilitiesEdgeCaseTest with tests for deep interface hierarchies
>   * **Added tests for public utility methods**: Added tests for logMethodAccessIssue(), logConstructorAccessIssue(), and clearCaches()
>   * **Removed deprecated method**: Removed deprecated indexOfSmallestValue() method
>   * **Removed unused private method**: Removed getMaxReflectionOperations() and associated constant
>   * **Removed unnecessary flush() call**: Eliminated no-op ByteArrayOutputStream.flush() in readInputStreamFully()
>   * **Clarified Converter usage**: Added comment explaining why ClassUtilities uses legacy Converter.getInstance()
>
>   **🔧 CONFIGURATION & DEFAULTS:**
>   * **Fixed surprising default values**: Changed default instance creation to use predictable, stable values:
>     * Date/time types now default to epoch (1970-01-01) instead of current time
>     * UUID defaults to nil UUID (all zeros) instead of random UUID
>     * Pattern defaults to empty pattern instead of match-all ".*"
>     * URL/URI mappings commented out to return null instead of potentially connectable localhost URLs
>   * **Removed problematic defaults**: 
>     * Removed EnumMap default mapping to TimeUnit.class
>     * Removed EnumSet.class null supplier from ASSIGNABLE_CLASS_MAPPING
>     * Removed Class.class → String.class mapping
>     * Removed Comparable→empty string mapping
>   * **Preserved mapping order**: Changed ASSIGNABLE_CLASS_MAPPING to LinkedHashMap for deterministic iteration
>   * **Improved immutability**: Made PRIMITIVE_WIDENING_DISTANCES and all inner maps unmodifiable
>   * **Reduced logging noise**: Changed various warnings from WARNING to FINE level for expected JPMS violations
>   * **Improved OSGi loader discovery order**: Changed getClassLoader() to try context loader first, then anchor, then OSGi
>   * **Improved resource path handling for Windows developers**: Backslashes in resource paths are now normalized to forward slashes
>   * **Simplified primitive checks**: Removed redundant isPrimitive() OR checks since methods handle both primitives and wrappers
>   * **Simplified SecurityManager checks**: Removed redundant ReflectPermission check in trySetAccessible()
>   * **Made record support fields volatile**: Proper thread-safe lazy initialization for JDK 14+ features
> 
> * **IMPROVED**: `CaseInsensitiveSet` refactored to use `Collections.newSetFromMap()` for cleaner implementation:
>   * Simplified implementation using Collections.newSetFromMap(CaseInsensitiveMap) internally
>   * Added Java 8+ support: spliterator(), removeIf(Predicate), and enhanced forEach() methods
>   * Fixed removeAll behavior for proper case-insensitive removal with non-CaseInsensitive collections
>   * Maintained full API compatibility
> 
> * **FIXED**: `DeepEquals` collection comparison was too strict when comparing different Collection implementations:
>   * Fixed UnmodifiableCollection comparison with Lists/ArrayLists based on content
>   * Relaxed plain Collection vs List comparison as unordered collections
>   * Preserved Set vs List distinction due to incompatible equality semantics
> 
> * **FIXED**: `SafeSimpleDateFormat` thread-safety and lenient mode issues:
>   * Fixed NPE in setters by initializing parent DateFormat fields
>   * Fixed lenient propagation to both Calendar and SimpleDateFormat
>   * Keep parent fields in sync when setters are called
> 
> * **IMPROVED**: `SafeSimpleDateFormat` completely redesigned with copy-on-write semantics:
>   * Copy-on-write mutations create new immutable state snapshots
>   * Thread-local LRU caching for SimpleDateFormat instances
>   * No locks on hot path - format/parse use thread-local cached instances
>   * Immutable state tracking for all configuration
>   * Smart cache invalidation on configuration changes
>   * Backward compatibility maintained
> 
> * **FIXED**: `UniqueIdGenerator` Java 8 compatibility:
>   * Fixed Thread.onSpinWait() using reflection for Java 9+, no-op fallback for Java 8
> 
> * **PERFORMANCE**: Optimized `DeepEquals` based on GPT-5 code review:
>   * **Algorithm & Data Structure Improvements:**
>     * Migrated from LinkedList to ArrayDeque for stack operations
>     * Pop-immediately optimization eliminating double iterations
>     * Depth tracking optimization avoiding costly parent chain traversal
>     * Early termination optimization using LIFO comparison order
>     * Primitive array optimization comparing directly without stack allocations
>     * Pre-size hash buckets to avoid rehashing on large inputs
>     * Fixed O(n²) path building using forward build and single reverse
>     * Optimized probe comparisons to bypass diff generation completely
>     * Added Arrays.equals fast-path for primitive arrays
>     * Optimized decomposeMap to compute hash once per iteration
>     * Added fast path for integral number comparison avoiding BigDecimal
>   
>   * **Correctness Fixes:**
>     * Changed epsilon value from 1e-15 to 1e-12 for practical floating-point comparisons
>     * Adjusted hash scales to maintain hash-equals contract with new epsilon
>     * Fixed List comparison semantics - Lists only compare equal to other Lists
>     * Fixed floating-point comparison using absolute tolerance for near-zero
>     * Made NaN comparison consistent via bitwise equality
>     * Fixed hash-equals contract for floating-point with proper NaN/infinity handling
>     * Fixed infinity comparison preventing infinities from comparing equal to finite numbers
>     * Fixed ConcurrentModificationException using iterator.remove()
>     * Fixed formatDifference crash using detailNode approach
>     * Fixed deepHashCode bucket misalignment with slow-path fallback
>     * Fixed leftover detection for unmatched elements
>     * Fixed visited set leakage in candidate matching
>     * Fixed non-monotonic depth budget clamping
>     * Fixed deepHashCode Map collisions using XOR for key-value pairs
>   
>   * **Features & Improvements:**
>     * Added Java Record support using record components instead of fields
>     * Added Deque support with List compatibility
>     * Improved sensitive data detection with refined patterns
>     * Improved MAP_MISSING_KEY error messages with clearer formatting
>     * Added security check in formatComplexObject for sensitive fields
>     * Added string sanitization for secure errors
>     * Type-safe visited set using `Set<ItemsToCompare>`
>     * Skip static/transient fields in formatting
>     * Implemented global depth budget across recursive paths
>     * Added Locale.ROOT for consistent formatting
>     * Gated diff_item storage behind option to prevent retention
>     * Added DIFF_ITEM constant for type-safe usage
>   
>   * **Code Quality:**
>     * Removed static initializer mutating global system properties
>     * Removed unreachable AtomicInteger/AtomicLong branches
>     * Fixed Javadoc typos and added regex pattern commentary
>     * Fixed documentation to match default security settings
>     * Performance micro-optimizations hoisting repeated lookups
> 
> * **SECURITY & CORRECTNESS**: `ReflectionUtils` comprehensive fixes based on GPT-5 security audit:
>   * Fixed over-eager setAccessible() only for non-public members
>   * Fixed getNonOverloadedMethod enforcement for ANY parameter count
>   * Added interface hierarchy search using breadth-first traversal
>   * Fixed method annotation search traversing super-interfaces
>   * Fixed trusted-caller bypass - ReflectionUtils no longer excludes itself
>   * Removed static System.setProperty calls during initialization
>     * **Fixed Javadoc typo**: Corrected "instants hashCode()" to "instance's hashCode()" in deepHashCode documentation
>     * **Added regex pattern commentary**: Clarified that HEX_32_PLUS and UUID_PATTERN use lowercase patterns since strings are lowercased before matching
>     * **Type-safe visited set**: Changed visited set type from `Set<Object>` to `Set<ItemsToCompare>` for compile-time type safety and to prevent accidental misuse
>     * **Added Arrays.equals fast-path**: Use native Arrays.equals for primitive arrays as optimization before element-by-element comparison with diff tracking
>     * **Skip static/transient fields in formatting**: Aligned formatComplexObject and formatValueConcise with equality semantics by skipping static and transient fields
>     * **Implemented global depth budget**: Pass remaining depth budget through child calls to ensure security limits are truly global across all recursive paths, preventing excessive recursion
>   * **Additional nuanced fixes from GPT-5 review**:
>     * **Fixed non-monotonic depth budget**: Clamp child budget to tighter of inherited budget and remaining configured budget to prevent depth limit bypass
>     * **Added string sanitization for secure errors**: Sanitize map keys and string values when secure errors are enabled to prevent sensitive data leakage
>     * **Optimized decomposeMap**: Avoid rehashing keys multiple times by computing hash once per iteration
>     * **Fixed deepHashCode Map collisions**: Hash key-value pairs together using XOR for order-independent hashing that reduces collisions
>     * **Added Locale.ROOT for numeric formatting**: Ensure consistent decimal formatting across all locales
>     * **Added Deque support with List compatibility**: List and Deque now compare as equal when containing the same ordered elements, treating both as ordered sequences that allow duplicates (berries over branches philosophy)
>     * **Fixed visited set leakage in candidate matching**: Use copies of visited set for exploratory candidate comparisons in unordered collections and maps to prevent pollution with failed comparison state
>     * **Fixed documentation to match default security settings**: Updated Javadoc to correctly state that default safeguards are enabled (100k limits for collections/arrays/maps, 1k for object fields, 1M for recursion depth)
>     * **Added fast path for integral number comparison**: Avoid expensive BigDecimal conversion for Byte, Short, Integer, Long, AtomicInteger, and AtomicLong comparisons
>     * **Added special case handling for AtomicInteger and AtomicLong**: Use get() methods directly like AtomicBoolean, avoiding reflective field access for better performance and consistency
>     * **Precompiled sensitive data regex patterns**: Avoid regex compilation overhead on every call to looksLikeSensitiveData() by using precompiled Pattern objects
>     * **Added Enum handling as simple type**: Use reference equality (==) for enum comparisons and format as EnumType.NAME, avoiding unnecessary reflective field walking
> * **IMPROVED**: `ReflectionUtils` enhancements based on GPT-5 review:
>   * **Fixed getMethod interface search**: Now properly searches entire interface hierarchy using BFS traversal to find default methods
>   * **Removed pre-emptive SecurityManager checks**: Removed unnecessary SecurityManager checks from call() methods since setAccessible is already wrapped
>   * **Documented null-caching requirement**: Added clear documentation to all cache setter methods that custom Map implementations must support null values
>   * **Fixed getClassAnnotation javadoc**: Corrected @throws documentation to accurately reflect that only annoClass=null throws, classToCheck=null returns null

#### 4.0.0

> * **FEATURE**: Added `deepCopyContainers()` method to `CollectionUtilities` and `ArrayUtilities`:
>   * **Deep Container Copy**: Iteratively copies all arrays and collections to any depth while preserving references to non-container objects ("berries")
>   * **Iterative Implementation**: Uses heap-based traversal with work queue to avoid stack overflow on deeply nested structures
>   * **Circular Reference Support**: Properly handles circular references, maintaining the circular structure in the copy
>   * **Enhanced Type Preservation**: 
>     * EnumSet → EnumSet (preserves enum type)
>     * Deque → LinkedList (preserves deque operations, supports nulls)
>     * PriorityQueue → PriorityQueue (preserves comparator and heap semantics)
>     * SortedSet → TreeSet (preserves comparator and sorting)
>     * Set → LinkedHashSet (preserves insertion order)
>     * List → ArrayList (optimized for random access)
>     * Other Queue types → LinkedList (preserves queue operations)
>   * **Performance Optimizations**: 
>     * Primitive arrays use `System.arraycopy` for direct copying without boxing/unboxing overhead
>     * Primitive arrays at root level are not queued (already fully copied)
>     * Collections are pre-sized to avoid resize/rehash operations during population
>     * Only containers are queued for processing, eliminating per-element allocations
>     * Direct array access for object arrays instead of reflection in tight loops
>     * Pre-sized IdentityHashMap (64) to avoid rehash thrashing
>     * EnumSet uses efficient `clone().clear()` for empty sets
>   * **Maps as Berries**: Maps are treated as non-containers and not deep copied
>   * **Thread Safety Note**: Method is not thread-safe under concurrent source mutation
> * **FEATURE**: Added `caseSensitive` configuration option to `MultiKeyMap`:
>   * **Case-Sensitive Mode**: New constructor `MultiKeyMap(boolean caseSensitive)` allows case-sensitive String key comparisons (default remains case-insensitive)
>   * **Performance Optimization**: Eliminated per-key branching by storing caseSensitive as final field, improving JIT optimization
>   * **Full API Support**: Case sensitivity applies to all MultiKeyMap operations including standard Map interface and multi-key methods
>   * **Documentation**: Updated README.md with examples showing case-sensitive vs case-insensitive behavior
> * **DOCUMENTATION**: Updated README.md to document MultiKeyMap's advanced configuration options:
>   * Added examples for case-sensitive mode configuration
>   * Added examples for value-based equality mode for cross-type numeric comparisons
>   * Updated comparison table showing MultiKeyMap's unique features vs competitors
> * **MAJOR PERFORMANCE OPTIMIZATION**: Enhanced `MultiKeyMap` with comprehensive performance improvements based on GPT5 code review:
>   * **Fixed KIND_COLLECTION Fast Path**: Added `!valueBasedEquality` check to gate fast path, ensuring collections with numerically equivalent but type-different elements match correctly (e.g., [1,2,3] matches [1L,2L,3L] in value-based mode)
>   * **Optimized compareNumericValues**: Replaced with highly optimized version using same-class fast paths, avoiding BigDecimal conversion for common cases. Added helper methods: `isIntegralLike`, `isBig`, `extractLongFast`, `toBigDecimal`
>   * **Defensive RandomAccess Checking**: Added `instanceof List` checks before `instanceof RandomAccess` in 6 locations to prevent ClassCastException
>   * **Branch-Free Loop Optimization**: Split loops by `valueBasedEquality` mode in 7 comparison methods, eliminating per-element branching for better JIT optimization and CPU branch prediction
>   * **Avoided Primitive Boxing**: Refactored `comparePrimitiveArrayToObjectArray` to avoid boxing in type-strict mode with direct type checking
>   * **Collapsed Duplicate Type Ladders**: Created `primVsList` and `primVsIter` helper methods, eliminating redundant 8-type switch statements and reducing bytecode size
>   * **Consolidated Symmetric Methods**: Made symmetric comparison methods delegate to their counterparts, reducing code duplication
>   * **NaN Handling for Primitive Arrays**: Added special NaN handling for double[] and float[] arrays respecting valueBasedEquality mode
>   * **Ref-Equality Guards**: Added `if (a == b) continue;` guards in all comparison loops, leveraging JVM caching for common values
> * **PERFORMANCE ENHANCEMENT**: Enhanced `MultiKeyMap` with significant hash computation optimizations:
>   * **Hash Computation Limit**: Added MAX_HASH_ELEMENTS (4) limit to bound hash computation for large arrays/collections, significantly improving performance
>   * **Early Exit Optimization**: Hash computation now stops early for large containers while maintaining excellent hash distribution
>   * **Dimensionality Check Optimization**: Separated hash computation from dimensionality detection for better performance on large containers
>   * **ArrayList Optimization**: Added specialized fast path for ArrayList iteration avoiding iterator overhead
>   * **Primitive Array Optimizations**: Enhanced hash computation for String[], int[], long[], double[], and boolean[] arrays with bounded processing
>   * **Generic Array Processing**: Improved reflection-based array processing with hash computation limits
>   * **Collection Processing**: Optimized both ArrayList and generic Collection processing with early termination
>   * **Performance Testing**: Added comprehensive test coverage including hash distribution analysis, collision analysis, and performance comparisons

#### 3.9.0

> * **MAJOR FEATURE**: Enhanced `MultiKeyMap` with comprehensive performance and robustness improvements:
>   * **Security Enhancement**: Replaced String sentinels with custom objects to prevent key collisions in internal operations
>   * **Performance Optimization**: Added comprehensive collection and typed array optimizations with NULL_SENTINEL uniformity
>   * **Performance**: Enhanced MultiKeyMap visual formatting and optimized ArrayList iteration patterns
>   * **Hash Algorithm**: Added MurmurHash3 finalization for improved hash distribution
>   * **Bug Fix**: Fixed instanceof Object[] hierarchy issues ensuring proper type handling across all array types
>   * **Enhancement**: Improved null key handling and enhanced toString() formatting with proper emoji symbols
>   * **Simplification**: Streamlined MultiKeyMap implementation for better maintainability and performance
>   * **Test Coverage**: Added comprehensive test coverage including:
>     * Generic array processing test coverage ensuring robust type handling
>     * MultiKeyMap.formatSimpleKey method testing for output consistency
>     * NULL_SENTINEL and cycle detection test coverage for edge case robustness
>     * Fixed MultiKeyMapMapInterfaceTest emoji format expectations
> * **ENHANCEMENT**: `IntervalSet` improvements:
>   * **Simplified Architecture**: Uses half-open intervals [start, end) eliminating need for custom boundary functions
>   * **API Enhancement**: Mirrors ConcurrentSkipListSet's behavior more accurately
>   * **New Feature**: Added snapshot() method for obtaining point-in-time snapshots with better return types than toArray()
>   * **JSON Round-Trip Support**: Added constructor that accepts snapshot() output, enabling easy JSON serialization/deserialization round-trips
>   * **Bug Fix**: Fixed JSON serialization constructors for proper deserialization support
>   * **Documentation**: Added comprehensive quanta calculation examples using Math.nextUp() and temporal precision APIs
> * **DOCUMENTATION**: Updated changelog.md and improved table formatting throughout documentation

#### 3.8.0

> * **MAJOR FEATURE**: Added `IntervalSet` - thread-safe set of half-open intervals [start, end). Optimized (collapsed) by default, or all intervals retained if `autoMerge=false` (audit mode):
>   * **Half-Open Semantics**: Uses [start, end) intervals where start is inclusive, end is exclusive - eliminates boundary ambiguity
>   * **High Performance**: O(log n) operations using `ConcurrentSkipListMap` for all queries, insertions, and range operations
>   * **Dual Storage Modes**: Auto-merge mode (default) merges overlapping intervals; discrete mode preserves all intervals for audit trails
>   * **Rich Query API**: Navigation methods (`nextInterval`, `previousInterval`, `higherInterval`, `lowerInterval`), containment checking, and range queries
>   * **Simplified Boundaries**: Half-open intervals eliminate need for complex boundary calculations while supporting all Comparable types
>   * **Thread Safety**: Lock-free reads with minimal write locking; weakly consistent iteration reflects live changes; use `snapshot().iterator()` for point-in-time iteration
>   * **Quanta Support**: Comprehensive documentation for creating minimal intervals using Math.nextUp(), temporal precision, and integer arithmetic
>   * **Type Support**: Full support for Integer, Long, Date, Timestamp, LocalDate, ZonedDateTime, Duration, and all Comparable types
>   * **Comprehensive Testing**: 116+ test cases covering all data types, concurrent operations, edge cases, and both storage modes
> * **TEST FIX**: Stabilized `ConcurrentListIteratorTest.testReadFailsGracefullyWhenConcurrentRemoveShrinksList` by using a latch to reliably detect the expected exception under heavy load
> * **BUG FIX**: Prevented null elements from appearing in iterator snapshots of `ConcurrentList` under extreme concurrency
> * **BUG FIX**: Corrected `IntervalSet` range removal operations, enforced unique start keys in discrete mode, and improved type support documentation.
> * **REFACTOR**: Simplified `MultiKeyMap` by removing the redundant volatile `size` field and relying on the existing `AtomicInteger` for size tracking.
> * **REFACTOR**: Consolidated hash computation logic in `MultiKeyMap` to reduce duplication and improve readability.

#### 3.7.0

> * **MAJOR FEATURE**: Enhanced `MultiKeyMap` with N-dimensional array expansion support:
>   * **N-Dimensional Array Expansion**: Nested arrays of any depth are automatically flattened recursively into multi-keys with sentinel preservation
>   * **Visual Notation**: `{{"a", "b"}, {"c", "d"}} → [SENTINELS, DN, "a", "b", UP, DN, "c", "d", UP]` - powerful structural preservation
>   * **Iterative Processing**: Uses stack-based approach to avoid recursion limits with deeply nested arrays
>   * **Universal Support**: Works with jagged arrays, mixed types, null elements, and empty sub-arrays
>   * **API Consistency**: Full support across all MultiKeyMap APIs (put/get/containsKey/remove and putMultiKey/getMultiKey/removeMultiKey/containsMultiKey)
>   * **Comprehensive Testing**: 13 test cases covering 2D/3D arrays, mixed types, jagged arrays, deep nesting, and edge cases
> * **SECURITY ENHANCEMENT**: Enhanced `TrackingMap` with SHA-1 based key tracking to eliminate array component ambiguity:
>   * **Ambiguity Resolution**: Different array structures `[[a,b],[c,d]]` vs `[a,b,c,d]` now track distinctly using SHA-1 hashes
>   * **Structural Sentinels**: Added `LEVEL_DOWN`/`LEVEL_UP`/`HAS_SENTINELS` objects to preserve array nesting information
>   * **Hash-Based Tracking**: Multi-dimensional arrays tracked via SHA-1 hash of expanded sentinel structure
>   * **Performance Optimized**: O(1) sentinel detection using `HAS_SENTINELS` flag, shorter string representations for speed
>   * **Clean APIs**: `MultiKeyMap.get1DKey()` and `computeSHA1Hash()` provide focused functionality for TrackingMap
> * **API ENHANCEMENT**: Updated `MultiKeyMap` varargs method names for disambiguation:
>   * **Renamed Methods**: `put()` → `putMultiKey()`, `get()` → `getMultiKey()`, `remove()` → `removeMultiKey()`, `containsKey()` → `containsMultiKey()`
>   * **Backward Compatibility**: Standard Map interface methods (single key) remain unchanged
>   * **Documentation Updated**: README.md, userguide.md, and Javadoc all reflect correct API usage
> * **ENUM SIMPLIFICATION**: Streamlined `MultiKeyMap.CollectionKeyMode` from 3 to 2 values:
>   * **Simplified Options**: `COLLECTIONS_EXPANDED` (default) and `COLLECTIONS_NOT_EXPANDED`
>   * **Clear Behavior**: Arrays are ALWAYS expanded regardless of setting; enum only affects Collections
>   * **Constructor Support**: Enhanced constructors to accept `CollectionKeyMode` parameter for configuration
>   * **Documentation Clarity**: Updated all documentation to reflect simplified enum behavior
> * **BUG FIX**: Fixed `ConcurrentListConcurrencyTest.testConcurrentQueueOperations` timing issue:
>   * **Flaky Test Resolution**: Updated test expectations to accommodate realistic concurrent producer/consumer timing variations
>   * **Race Condition**: Test was expecting perfect 100% consumption rate in concurrent scenario, but timing variations meant some `pollFirst()` calls returned null
>   * **Improved Validation**: Now validates ≥90% consumption rate and empty queue state, which properly tests ConcurrentList functionality
>   * **No Functional Changes**: This was a test-only fix; ConcurrentList behavior remains unchanged and correct
> * **PROCESS IMPROVEMENT**: Enhanced deployment pipeline with updated Maven Sonatype publishing process
> * **PERFORMANCE**: Optimized test execution by disabling compilation for faster test cycles during development
> * **TEST FIX**: Stabilized `ConcurrentListIteratorTest.testReadFailsGracefullyWhenConcurrentRemoveShrinksList`
>   * Used a latch to reliably detect the expected exception under heavy load

#### 3.6.0

> * **MAJOR FEATURE**: Added many additional types to `Converter`, expanding conversion capability (1,700+ total conversion pairs):
>   * **Atomic Arrays**: Added full bidirectional conversion support for `AtomicIntegerArray`, `AtomicLongArray`, and `AtomicReferenceArray`
>   * **NIO Buffers**: Added complete bridge system for all NIO buffer types (`IntBuffer`, `LongBuffer`, `FloatBuffer`, `DoubleBuffer`, `ShortBuffer`) with existing `ByteBuffer` and `CharBuffer`
>   * **BitSet Integration**: Added intelligent `BitSet` conversion support with bridges to `boolean[]` (bit values), `int[]` (set bit indices), and `byte[]` (raw representation)
>   * **Stream API**: Added bidirectional conversion support for `IntStream`, `LongStream`, and `DoubleStream` primitive streams
>   * **Universal Array Access**: Each array-like type now has access to the entire universal array conversion ecosystem - for example, `AtomicIntegerArray` → `int[]` → `Color` works seamlessly
>   * **Performance Optimized**: All bridges use efficient extraction/creation patterns with minimal overhead
>   * Removed redundant array surrogate pairs that were duplicating universal array system functionality
>   * **MutliKeyMap** - Yes, a MultiKeyMap that supports n-keys, creates no heap pressure for get() { no allocations (new) within get() execution path}, full thread-safety for all operations.
> * **ARCHITECTURE IMPROVEMENT**: Enhanced `addConversion()` method with comprehensive primitive/wrapper support:
>   * When adding a conversion involving primitive or wrapper types, the system now automatically creates ALL relevant combinations
>   * Example: `addConversion(UUID.class, Boolean.class, converter)` now creates entries for both `(UUID, Boolean)` and `(UUID, boolean)`
>   * Eliminates runtime double-lookup overhead in favor of storage-time enumeration for better performance
>   * Ensures seamless primitive/wrapper interoperability in user-defined conversions
>   * **Code Simplification**: Refactored implementation to leverage existing `ClassUtilities` methods, reducing complexity while maintaining identical functionality
> * **API ENHANCEMENT**: Added `ClassUtilities.toPrimitiveClass()` method as complement to existing `toPrimitiveWrapperClass()`:
>   * Converts wrapper classes to their corresponding primitive classes (e.g., `Integer.class` → `int.class`)
> * `ConcurrentList` now uses chunked atomic buckets for lock-free deque operations. See userguide for architecture diagram and capabilities table
>   * Returns the same class if not a wrapper type, ensuring safe usage for any class
>   * Leverages optimized `ClassValueMap` caching for high-performance lookups
>   * Centralizes primitive/wrapper conversion logic in `ClassUtilities` for consistency across java-util
> * **BUG FIX**: Fixed time conversion precision inconsistencies in `Converter` for consistent long conversion behavior:
>   * **Consistency Fix**: All time classes now consistently convert to/from `long` using **millisecond precision** (eliminates mixed millisecond/nanosecond behavior)
>   * **Universal Rule**: `Duration` → long, `Instant` → long, `LocalTime` → long now all return milliseconds for predictable behavior
>   * **Round-trip Compatibility**: Long ↔ time class conversions are now fully round-trip compatible with consistent precision
>   * **BigInteger Unchanged**: BigInteger conversions continue to use precision-based rules (legacy classes = millis, modern classes = nanos)
>   * **Feature Options**: Added configurable precision control for advanced use cases requiring nanosecond precision:
>     * System properties: `cedarsoftware.converter.modern.time.long.precision`, `cedarsoftware.converter.duration.long.precision`, `cedarsoftware.converter.localtime.long.precision`
>     * Per-instance options via `ConverterOptions.getCustomOption()` - see [Feature Options for Precision Control](userguide.md#feature-options-for-precision-control) for details
>   * **Impact**: Minimal - fixes inconsistent behavior and provides migration path through feature options
>   * **Rationale**: Eliminates confusion from mixed precision behavior and provides simple, memorable conversion rules
> * Added `computeIfAbsent` support to `MultiKeyMap` for lazy value population
> * Added `putIfAbsent` support to `MultiKeyMap` for atomic insert when key is missing or mapped to null
> * Expanded `MultiKeyMap` to fully implement `ConcurrentMap`: added `computeIfPresent`, `compute`, `replace`, and `remove(key,value)`
> * Fixed stripe locking in `MultiKeyMap` to consistently use `ReentrantLock`
> * **Feature Enhancements**:
>   * Supports conversion from String formats: hex colors (`#FF0000`, `FF0000`), named colors (`red`, `blue`, etc.), `rgb(r,g,b)`, and `rgba(r,g,b,a)` formats
>   * Supports conversion from Map format using keys: `red`, `green`, `blue`, `alpha`, `rgb`, `color`, and `value`
>   * Supports conversion from Map format using short keys: `r`, `g`, `b`, and `a` for compact representation
>   * Supports conversion from int arrays: `[r,g,b]` and `[r,g,b,a]` formats with validation
>   * Supports conversion from numeric types: Integer/Long packed RGB/ARGB values
>   * Supports conversion to all above formats with proper round-trip compatibility
>   * Values are converted through `converter.convert()` allowing String, AtomicInteger, Double, etc. as color component values
>   * Added comprehensive test coverage with 38 test methods covering all conversion scenarios
>   * Eliminates need for custom Color factories in json-io and other serialization libraries
>   * The static `Converter.getInstance()` method remains available for accessing the default shared instance
> * **Security Enhancement**: Fixed critical security vulnerabilities in `CompactMap` dynamic code generation:
>   * Added strict input sanitization to prevent code injection attacks in class name generation
>   * Fixed memory leak by using `WeakReference` for generated class caching to allow garbage collection
>   * Fixed race condition in class generation by ensuring consistent OSGi/JPMS-aware ClassLoader usage
>   * Enhanced input validation in `Builder` methods with comprehensive null checks and range validation
>   * Improved resource management during compilation with proper exception handling
> * **Security Enhancement**: Fixed critical security issues in `ClassUtilities`:
>   * Added strict security checks for unsafe instantiation with `RuntimePermission` validation
>   * Enhanced reflection security in `trySetAccessible()` to not suppress `SecurityExceptions`
>   * Updated deprecated `SecurityManager` usage for Java 17+ compatibility with graceful fallback
> * **Security Enhancement**: Fixed critical security vulnerabilities in `ReflectionUtils`:
>   * Added `ReflectPermission` security checks to prevent unrestricted method invocation in `call()` methods
>   * Created `secureSetAccessible()` wrapper to prevent access control bypass attacks
>   * Fixed cache poisoning vulnerabilities by using object identity (`System.identityHashCode`) instead of string-based cache keys
>   * Updated all cache key classes to use tamper-proof object identity comparison for security
>   * Enhanced security boundary enforcement across all reflection operations
> * **Security Enhancement**: Fixed critical security vulnerabilities in `DateUtilities`:
>   * Fixed Regular Expression Denial of Service (ReDoS) vulnerability by simplifying complex regex patterns
>   * Eliminated nested quantifiers and complex alternations that could cause catastrophic backtracking
>   * Fixed thread safety issue by making month names map immutable using `Collections.unmodifiableMap()`
>   * Added comprehensive input validation with bounds checking for all numeric parsing operations
>   * Enhanced error messages with specific field names and valid ranges for better debugging
> * **Security Enhancement**: Fixed critical SSL certificate bypass vulnerability in `UrlUtilities`:
>   * Added comprehensive security warnings to `NAIVE_TRUST_MANAGER` and `NAIVE_VERIFIER` highlighting the security risks
>   * Deprecated dangerous SSL bypass methods with clear documentation of vulnerabilities and safer alternatives
>   * Fixed `getAcceptedIssuers()` to return empty array instead of null for improved security
>   * Added runtime logging when SSL certificate validation is disabled to warn of security risks
>   * Enhanced JUnit test coverage to verify security fixes and validate proper warning behavior
> * **Security Enhancement**: Fixed ReDoS vulnerability in `DateUtilities` regex patterns:
>   * Limited timezone pattern repetition to prevent catastrophic backtracking (max 50 characters)
>   * Limited nanosecond precision to 1-9 digits to prevent infinite repetition attacks
>   * Added comprehensive ReDoS protection tests to verify malicious inputs complete quickly
>   * Preserved all existing DateUtilities functionality (187/187 tests pass)
>   * Conservative fix maintains exact capture group structure for API compatibility
> * **Security Enhancement**: Fixed thread safety vulnerability in `DateUtilities` timezone mappings:
>   * Made `ABBREVIATION_TO_TIMEZONE` map immutable using `Collections.unmodifiableMap()`
>   * Used `ConcurrentHashMap` during initialization for thread-safe construction
>   * Prevents external modification that could corrupt timezone resolution
>   * Eliminates potential race conditions in multi-threaded timezone lookups
>   * Added comprehensive thread safety tests to verify concurrent access protection
> * **Performance Optimization**: Optimized `CollectionUtilities` APIs:
>   * Pre-size collections in `listOf()`/`setOf()` to avoid resizing overhead
>   * Replace `Collections.addAll()` with direct loops for better performance
>   * Use `Collections.emptySet`/`emptyList` instead of creating new instances
>   * Updated codebase to use consistent collection APIs (`CollectionUtilities.setOf()` vs `Arrays.asList()`)
> * **Performance Optimization**: Enhanced `CaseInsensitiveMap` efficiency:
>   * Fixed thread safety issues in cache management with `AtomicReference`
>   * Optimized `retainAll()` to avoid size() anti-pattern (O(1) vs potentially O(n))
>   * Added `StringUtilities.containsIgnoreCase()` method with optimized `regionMatches` performance
>   * Updated `CaseInsensitiveMap` to use new `containsIgnoreCase` instead of double `toLowerCase()`
> * **Performance Optimization**: Enhanced `DateUtilities` efficiency:
>   * Optimized timezone resolution to avoid unnecessary string object creation in hot path
>   * Only create uppercase strings for timezone lookups when needed, reducing memory allocation overhead
>   * Improved timezone abbreviation lookup performance by checking exact match first
> * **Security Enhancement**: Fixed timezone handling security boundary issues in `DateUtilities`:
>   * Added control character validation to prevent null bytes and control characters in timezone strings
>   * Enhanced exception information sanitization to prevent information disclosure
>   * Improved error handling with truncated error messages for security
>   * Preserved API compatibility by maintaining `ZoneRulesException` and `DateTimeException` for existing test expectations
>   * Added case-insensitive GMT handling and additional validation of system-returned timezone IDs
> * **Code Quality**: Enhanced `ArrayUtilities` and `ByteUtilities`:
>   * Fixed generic type safety in `EMPTY_CLASS_ARRAY` using `Class<?>[0]`
>   * Added bounds validation to `ByteUtilities.isGzipped(offset)` to prevent `ArrayIndexOutOfBoundsException`
>   * Added time complexity documentation to `ArrayUtilities.removeItem()` method (O(n))
>   * Improved documentation for null handling and method contracts
> * **Performance Optimization**: Replaced inefficient `String.matches()` with pre-compiled regex patterns in `ClassUtilities`
> * Updated a few more spots where internal reflection updated `ReflectionUtils` caching for better performance.
> * **Performance Enhancement**: Added concurrent performance optimizations to `CaseInsensitiveMap`:
>   * Added `mappingCount()` method for efficient concurrent map size queries
>   * Added bulk parallel operations: `forEach(long, BiConsumer)`, `forEachKey(long, Consumer)`, `forEachValue(long, Consumer)`
>   * Added parallel search operations: `searchKeys(long, Function)`, `searchValues(long, Function)`, `searchEntries(long, Function)`
>   * Added parallel reduce operations: `reduceKeys(long, Function, BinaryOperator)`, `reduceValues(long, Function, BinaryOperator)`, `reduceEntries(long, Function, BinaryOperator)`
>   * Enhanced iterator implementations with concurrent-aware behavior for ConcurrentHashMap backing maps
>   * Optimized for ~95% native ConcurrentHashMap performance while maintaining case-insensitive functionality
>   * Added centralized thread-safe key unwrapping with comprehensive documentation
> * **Enhancement**: Brought `CompactSet` to parity with `CompactMap` for concurrent functionality:
>   * Added `mapType()` method to `CompactSet.Builder` for specifying concurrent backing map types
>   * Added support for `ConcurrentHashMap` and `ConcurrentSkipListSet` backing collections
>   * Enhanced builder pattern to support all concurrent collection types available in `CompactMap`
>   * Maintains automatic size-based transitions while respecting concurrent backing map selection
> * **Enhancement**: Brought `CaseInsensitiveSet` to parity with `CaseInsensitiveMap` concurrent capabilities:
>   * Added `elementCount()` method for efficient concurrent set size queries (delegates to backing map's `mappingCount()`)
>   * Added bulk parallel operations: `forEach(long, Consumer)`, `searchElements(long, Function)`, `reduceElements(long, Function, BinaryOperator)`
>   * Enhanced iterator implementation with concurrent-aware behavior inheriting from backing `CaseInsensitiveMap`
>   * Added `getBackingMap()` method for direct access to underlying `CaseInsensitiveMap` instance
>   * Full feature parity ensures consistent concurrent performance characteristics across case-insensitive collections
> * **Code Quality**: Eliminated all unchecked cast warnings in concurrent null-safe map classes:
>   * Updated `AbstractConcurrentNullSafeMap` method signatures to accept `Object` parameters instead of generic types
>   * Updated `ConcurrentNavigableMapNullSafe` method signatures for type safety compliance
>   * Improved overall type safety without breaking existing API compatibility
>   * Reduced compiler warnings from 15 to 0 across concurrent collection classes
> * **Documentation**: Comprehensive README.md enhancements for professional project presentation:
>   * Added comprehensive badge section with Maven Central, Javadoc, license, and compatibility information
>   * Enhanced Quick Start section with practical code examples for common use cases
>   * Added Performance Benchmarks section showcasing speed improvements and memory efficiency
>   * Created comprehensive Feature Matrix table comparing java-util collections with JDK alternatives
>   * Added Security Features showcase highlighting 70+ security controls and defensive programming practices
>   * Enhanced Integration examples for Spring, Jakarta EE, Spring Boot, and microservices architectures
>   * Extracted Framework Integration Examples to separate `frameworks.md` file with corrected cache constructor examples
> * **Testing**: Added comprehensive test coverage for all new concurrent functionality:
>   * 27 new JUnit tests for `CaseInsensitiveMap` concurrent operations covering thread safety and performance
>   * 15 new JUnit tests for `CompactSet` concurrent functionality and builder pattern enhancements  
>   * 23 new JUnit tests for `CaseInsensitiveSet` concurrent operations and feature parity validation
>   * Added multi-dimensional array conversion test matching README.md example for better documentation accuracy

#### 3.5.0

> * `Converter.getInstance()` exposes the default instance used by the static API
> * `ClassUtilities.newInstance()` accepts `Map` arguments using parameter names and falls back to the no‑arg constructor
> * `Converter.convert()` returns the source when assignment compatible (when no other conversion path is selected)
> * Throwable creation from a `Map` handles aliases and nested causes
> * Jar file is built with `-parameters` flag going forward (increased the jar size by about 10K)

#### 3.4.0

> * `MapUtilities.getUnderlyingMap()` now uses identity comparison to avoid false cycle detection with wrapper maps
> * `ConcurrentNavigableMapNullSafe.pollFirstEntry()` and `pollLastEntry()` now return correct values after removal
> * `UrlInvocationHandler` (deprecated) was finally removed.
> * `ProxyFactory` (deprecated) was finally removed.
> * `withReadLockVoid()` now suppresses exceptions thrown by the provided `Runnable`
> * `SystemUtilities.createTempDirectory()` now returns a canonical path so that
    temporary directories resolve symlinks on macOS and other platforms.
> * Updated inner-class JSON test to match removal of synthetic `this$` fields.
> * Fixed `ExecutorAdditionalTest` to compare canonical paths for cross-platform consistency
> * Fixed `Map.Entry.setValue()` for entries from `ConcurrentNavigableMapNullSafe` and `AbstractConcurrentNullSafeMap` to update the backing map
> * Map.Entry views now fetch values from the backing map so `toString()` and `equals()` reflect updates
> * Fixed test expectation for wrapComparator to place null keys last
> * `Converter` now offers single-argument overloads of `isSimpleTypeConversionSupported`
    and `isConversionSupportedFor` that cache self-type lookups
> * Fixed `TTLCache.purgeExpiredEntries()` NPE when removing expired entries
> * `UrlUtilities` no longer deprecated; certificate validation defaults to on, provides streaming API and configurable timeouts
> * Logging instructions merged into `userguide.md`; README section condensed
> * `ExceptionUtilities` adds private `uncheckedThrow` for rethrowing any `Throwable` unchecked
> * `IOUtilities` and related APIs now throw `IOException` unchecked

#### 3.3.3 LLM inspired updates against the life-long "todo" list.

> * `TTLCache` now recreates its background scheduler if used after `TTLCache.shutdown()`.
> * `SafeSimpleDateFormat.equals()` now correctly handles other `SafeSimpleDateFormat` instances.
> * Manifest cleaned up by removing `Import-Package` entries for `java.sql` and `java.xml`
> * All `System.out` and `System.err` prints replaced with `java.util.logging.Logger` usage.
> * Documentation explains how to route `java.util.logging` output to SLF4J, Logback, or Log4j 2 in the user guide
> * `ArrayUtilities` - new APIs `isNotEmpty`, `nullToEmpty`, and `lastIndexOf`; improved `createArray`, `removeItem`, `addItem`, `indexOf`, `contains`, and `toArray`
> * `ClassUtilities` - safer class loading fallback, improved inner class instantiation and updated Javadocs
> * `CollectionConversions.arrayToCollection` now returns a type-safe collection
> * `CompactMap.getConfig()` returns the library default compact size for legacy subclasses.
> * `ConcurrentHashMapNullSafe` - fixed race condition in `computeIfAbsent` and added constructor to specify concurrency level.
> * `StringConversions.toSqlDate` now preserves the time zone from ISO date strings instead of using the JVM default.
> * `ConcurrentList` is now `final`, implements `Serializable` and `RandomAccess`, and uses a fair `ReentrantReadWriteLock` for balanced thread scheduling.
> * `ConcurrentList.containsAll()` no longer allocates an intermediate `HashSet`.
> * `listIterator(int)` now returns a snapshot-based iterator instead of throwing `UnsupportedOperationException`.
> * `Converter` - factory conversions map made immutable and legacy caching code removed
> * `DateUtilities` uses `BigDecimal` for fractional second conversion, preventing rounding errors with high precision input
> * `EncryptionUtilities` now uses AES-GCM with random IV and PBKDF2-derived keys. Legacy cipher APIs are deprecated. Added SHA-384, SHA3-256, and SHA3-512 hashing support with improved input validation.
> * Documentation for `EncryptionUtilities` updated to list all supported SHA algorithms and note heap buffer usage.
> * `Executor` now uses `ProcessBuilder` with a 60-second timeout and provides an `ExecutionResult` API
> * `IOUtilities` improved: configurable timeouts, `inputStreamToBytes` throws `IOException` with size limit, offset bug fixed in `uncompressBytes`
> * `MathUtilities` now validates inputs for empty arrays and null lists, fixes documentation, and improves numeric parsing performance
> * `ReflectionUtils` cache size is configurable via the `reflection.utils.cache.size` system property, uses
> * `StringUtilities.decode()` now returns `null` when invalid hexadecimal digits are encountered.
> * `StringUtilities.getRandomString()` validates parameters and throws descriptive exceptions.
> * `StringUtilities.count()` uses a reliable substring search algorithm.
> * `StringUtilities.hashCodeIgnoreCase()` updates locale compatibility when the default locale changes.
> * `StringUtilities.commaSeparatedStringToSet()` returns a mutable empty set using `LinkedHashSet`.
> * `StringUtilities` adds `snakeToCamel`, `camelToSnake`, `isNumeric`, `repeat`, `reverse`, `padLeft`, and `padRight` helpers.
> * Constants `FOLDER_SEPARATOR` and `EMPTY` are now immutable (`final`).
> * Deprecated `StringUtilities.createUtf8String(byte[])` removed; use `createUTF8String(byte[])` instead.
> * `SystemUtilities` logs shutdown hook failures, handles missing network interfaces and returns immutable address lists
  `TestUtil.fetchResource`, `MapUtilities.cloneMapOfSets`, and core cache methods.
> * `TrackingMap` - `replaceContents()` replaces the misleading `setWrappedMap()` API. `keysUsed()` now returns an unmodifiable `Set<Object>` and `expungeUnused()` prunes stale keys.
> * Fixed tests for `TrackingMap.replaceContents` and `setWrappedMap` to avoid tracking keys during verification
> * `Unsafe` now obtains the sun.misc.Unsafe instance from the `theUnsafe` field instead of invoking its constructor, preventing JVM crashes during tests
> * `Traverser` supports lazy field collection, improved null-safe class skipping, and better error logging
> * `Traverser` now ignores synthetic fields, preventing traversal into outer class references
> * `Traverser` logs inaccessible fields at `Level.FINEST` instead of printing to STDERR
> * `TypeUtilities.setTypeResolveCache()` validates that the supplied cache is not null and inner `Type` implementations now implement `equals` and `hashCode`
> * `UniqueIdGenerator` uses `java.util.logging` and reduces CPU usage while waiting for the next millisecond
> * Explicitly set versions for `maven-resources-plugin`, `maven-install-plugin`, and `maven-deploy-plugin` to avoid Maven 4 compatibility warnings
> * Added Javadoc for several public APIs where it was missing.  Should be 100% now.
> * JUnits added for all public APIs that did not have them (no longer relying on json-io to "cover" them). Should be 100% now.
> * Custom map types under `com.cedarsoftware.io` allowed for `CompactMap`
#### 3.3.2 JDK 24+ Support
> * `LRUCache` - `getCapacity()` API added so you can query/determine capacity of an `LRUCache` instance after it has been created.
> * `SystemUtilities.currentJdkMajorVersion()` added to provide JDK8 thru JDK24 compatible way to get the JDK/JRE major version.
> * `CompactMap` - When using the builder pattern with the .build() API, it requires being run with a JDK - you will get a clear error if executed on a JRE. Using CompactMap (or static subclass of it like CompactCIHashMap or one of your own) does not have this requirement. The withConfig() and newMap() APIs also expect to execute on a JDK (dynamica compilation).
> * `CompactSet` - Has the same requirements regarding JDK/JRE as CompactMap.
> * Updated tests to support JDK 24+
>   * EST, MST, HST mapped to fixed offsets (‑05:00, ‑07:00, ‑10:00) when the property sun.timezone.ids.oldmapping=true was set
>   * The old‑mapping switch was removed, and the short IDs are now links to region IDs: EST → America/Panama, MST → America/Phoenix, HST → Pacific/Honolulu
#### 3.3.1 New Features and Improvements
> * `CaseInsensitiveMap/Set` compute hashCodes slightly faster because of update to `StringUtilities.hashCodeIgnoreCase().`  It takes advantage of ASCII for Locale's that use Latin characters.
> * `CaseInsensitiveString` inside `CaseInsensitiveMap` implements `CharSequence` and can be used outside `CaseInsensitiveMap` as a case-insensitive but case-retentiative String and passed to methods that take `CharSequence.`
> * `FastReader/FastWriter` - tests added to bring it to 100% Class, Method, Line, and Branch coverage.
> * `FastByteArrayInputStream/FastByteArrayOutputStream` - tests added to bring it to 100% Class, Method, Line, and Branch coverage.
> * `TrackingMap.setWrappedMap()` - added to allow the user to set the wrapped map to a different map.  This is useful for testing purposes.
> * Added tests for CompactCIHashSet, CompactCILinkedSet and CompactLinkedSet constructors.
#### 3.3.0 New Features and Improvements
> * `CompactCIHashSet, CompactCILinkedSet, CompactLinkedSet, CompactCIHashMap, CompactCILinkedMap, CompactLinkedMap` are no longer deprecated. Subclassing `CompactMap` or `CompactSet` is a viable option if you need to serialize the derived class with libraries other than `json-io,` like Jackson, Gson, etc.
> * Added `CharBuffer to Map,` `ByteBuffer to Map,` and vice-versa conversions.
> * `DEFAULT_FIELD_FILTER` in `ReflectionUtils` made public.
> * Bug fix: `FastWriter` missing characters on buffer limit #115 by @ozhelezniak-talend.
#### 3.2.0 New Features and Improvements
> * **Added `getConfig()` and `withConfig()` methods to `CompactMap` and `CompactSet`**
>   - These methods allow easy inspection of `CompactMap/CompactSet` configurations
>   - Provides alternative API for creating a duplicate of a `CompactMap/CompactSet` with the same configuration
>   -  If you decide to use a non-JDK `Map` for the `Map` instance used by `CompactMap`, you are no longer required to have both a default constructor and a constructor that takes an initialize size.**
> * **Deprecated** `shutdown` API on `LRUCache` as it now uses a Daemon thread for the scheduler.  This means that the thread will not prevent the JVM from exiting.
#### 3.1.1
> * [ClassValueMap](userguide.md#classvaluemap) added. High-performance `Map` optimized for ultra-fast `Class` key lookups using JVM-optimized `ClassValue`
> * [ClassValueSet](userguide.md#classvalueset) added. High-performance `Set` optimized for ultra-fast `Class` membership testing using JVM-optimized `ClassValue`
> * Performance improvements: Converter's `convert(),` `isConversionSupported(),` `isSimpleTypeConversion()` are faster via improved caching.
#### 3.1.0
> * [TypeUtilities](userguide.md#typeutilities) added. Advanced Java type introspection and generic resolution utilities.
> * Currency and Pattern support added to Converter.
> * Performance improvements: ClassUtilities caches the results of distance between classes and fetching all supertypes.
> * Bug fix: On certain windows machines, applications would not exit because of non-daenmon thread used for scheduler in LRUCache/TTLCache. Fixed by @kpartlow.
#### 3.0.3
> * `java.sql.Date` conversion - considered a timeless "date", like a birthday, and not shifted due to time zones. Example, `2025-02-07T23:59:59[America/New_York]` coverage effective date, will remain `2025-02-07` when converted to any time zone.
> * `Currency` conversions added (toString, toMap and vice-versa)
> * `Pattern` conversions added (toString, toMap and vice-versa)
> * `YearMonth` conversions added (all date-time types to `YearMonth`)
> * `Year` conversions added (all date-time types to `Year`)
> * `MonthDay` conversions added (all date-time types to `MonthDay`)
> * All Temporal classes, when converted to a Map, will typically use a single String to represent the Temporal object. Uses the ISO 8601 formats for dates, other ISO formats for Currency, etc.
#### 3.0.2
>
> * Conversion test added that ensures all conversions go from instance, to JSON, and JSON, back to instance, through all conversion types supported. `java-util` uses `json-io` as a test dependency only.
> * `Timestamp` conversion improvements (better honoring of nanos) and Timezone is always specified now, so no risk of system default Timezone being used.  Would only use system default timezone if tz not specified, which could only happen if older version sending older format JSON.
#### 3.0.1
> * [ClassUtilities](userguide.md#classutilities) adds
>   * `Set<Class<?>> findLowestCommonSupertypes(Class<?> a, Class<?> b)`
>     * which returns the lowest common anscestor(s) of two classes, excluding `Object.class.`  This is useful for finding the common ancestor of two classes that are not related by inheritance.  Generally, executes in O(n log n) - uses sort internally.  If more than one exists, you can filter the returned Set as you please, favoring classes, interfaces, etc.
>   * `Class<?> findLowestCommonSupertype(Class<?> a, Class<?> b)`
>     * which is a convenience method that calls the above method and then returns the first one in the Set or null.
>   * `boolean haveCommonAncestor(Class<?> a, Class<?> b)`
>     * which returns true if the two classes have a common ancestor (excluding `Object.class`).
>   * `Set<Class<?>> getAllSupertypes(Class<?> clazz)`
>     * which returns all superclasses and interfaces of a class, including itself.  This is useful for finding all the classes and interfaces that a class implements or extends.
> * Moved `Sealable*` test cases to json-io project.
> * Removed remaining usages of deprecated `CompactLinkedMap.`
#### 3.0.0
> * [DeepEquals](userguide.md#deepequals) now outputs the first encountered graph "diff" in the passed in input/output options Map if provided. See userguide for example output.
> * [CompactMap](userguide.md#compactmap) and [CompactSet](userguide.md#compactset) no longer do you need to sublcass for variations.  Use the new builder api.
> * [ClassUtilities](userguide.md#classutilities) added `newInstance()`. Also, `getClassLoader()` works in OSGi, JPMS, and non-modular environments.
> * [Converter](userguide.md#converter) added support for arrays to collections, arrays to arrays (for type difference that can be converted), for n-dimensional arrays.  Collections to arrays and Collections to Collections, also supported nested collections. Arrays and Collections to EnumSet.
> * [ReflectionUtils](userguide.md#reflectionutils) robust caching in all cases, optional `Field` filtering via `Predicate.`
> * [SystemUtilities](userguide.md#systemutilities) added many new APIs.
> * [Traverser](userguide.md#traverser) updated to support passing all fields to visitor, uses lambda for visitor.
> * Should be API compatible with 2.x.x versions.
> * Complete Javadoc upgrade throughout the project.
> * New [User Guide](userguide.md#compactset) added.
#### 2.18.0
> * Fix issue with field access `ClassUtilities.getClassLoader()` when in OSGi environment.  Thank you @ozhelezniak-talend.
> * Added `ClassUtilities.getClassLoader(Class<?> c)` so that class loading was not confined to java-util classloader bundle. Thank you @ozhelezniak-talend.
#### 2.17.0
> * `ClassUtilities.getClassLoader()` added. This will safely return the correct class loader when running in OSGi, JPMS, or neither.
> * `ArrayUtilities.createArray()` added. This method accepts a variable number of arguments and returns them as an array of type `T[].`
> * Fixed bug when converting `Map` containing "time" key (and no `date` nor `zone` keys) with value to `java.sql.Date.`  The millisecond portion was set to 0.
#### 2.16.0
> * `SealableMap, LRUCache,` and `TTLCache` updated to use `ConcurrentHashMapNullSafe` internally, to simplify their implementation, as they no longer have to implement the null-safe work, `ConcurrentHashMapNullSafe` does that for them.
> * Added `ConcurrentNavigableMapNullSafe` and `ConcurrentNavigableSetNullSafe`
> * Allow for `SealableNavigableMap` and `SealableNavigableSet` to handle null
> * Added support for more old timezone names (EDT, PDT, ...)
> * Reverted back to agrona 1.22.0 (testing scope only) because it uses class file format 52, which still works with JDK 1.8
> * Missing comma in OSGI support added in pom.xml file. Thank you @ozhelezniak.
> * `TestGraphComparator.testNewArrayElement` updated to reliable compare results (not depdendent on a Map that could return items in differing order).  Thank you @wtrazs
#### 2.15.0
> * Introducing `TTLCache`: a cache with a configurable minimum Time-To-Live (TTL). Entries expire and are automatically removed after the specified TTL. Optionally, set a `maxSize` to enable Least Recently Used (LRU) eviction. Each `TTLCache` instance can have its own TTL setting, leveraging a shared `ScheduledExecutorService` for efficient resource management. To ensure proper cleanup, call `TTLCache.shutdown()` when your application or service terminates.
> * Introducing `ConcurrentHashMapNullSafe`: a drop-in replacement for `ConcurrentHashMap` that supports `null` keys and values. It uses internal sentinel values to manage `nulls,` providing a seamless experience. This frees users from `null` handling concerns, allowing unrestricted key-value insertion and retrieval.
> * `LRUCache` updated to use a single `ScheduledExecutorService` across all instances, regardless of the individual time settings. Call the static `shutdown()` method on `LRUCache` when your application or service is ending.
#### 2.14.0
> * `ClassUtilities.addPermanentClassAlias()` - add an alias that `.forName()` can use to instantiate class (e.g. "date" for `java.util.Date`)
> * `ClassUtilities.removePermanentClassAlias()` - remove an alias that `.forName()` can no longer use.
> * Updated build plug-in dependencies.
#### 2.13.0
> * `LRUCache` improved garbage collection handling to avoid [gc Nepotism](https://psy-lob-saw.blogspot.com/2016/03/gc-nepotism-and-linked-queues.html?lr=1719181314858) issues by nulling out node references upon eviction. Pointed out by [Ben Manes](https://github.com/ben-manes).
> * Combined `ForkedJoinPool` and `ScheduledExecutorService` into use of only `ScheduledExecutorServive,` which is easier for user.  The user can supply `null` or their own scheduler. In the case of `null`, one will be created and the `shutdown()` method will terminate it.  If the user supplies a `ScheduledExecutorService` it will be *used*, but not shutdown when the `shutdown()` method is called. This allows `LRUCache` to work well in containerized environments.
#### 2.12.0
> * `LRUCache` updated to support both "locking" and "threaded" implementation strategies.
#### 2.11.0
> * `LRUCache` re-written so that it operates in O(1) for `get(),` `put(),` and `remove()` methods without thread contention. When items are placed into (or removed from) the cache, it schedules a cleanup task to trim the cache to its capacity.  This means that it will operate as fast as a `ConcurrentHashMap,` yet shrink to capacity quickly after modifications.
#### 2.10.0
> * Fixed potential memory leak in `LRUCache.`
> * Added `nextPermutation` to `MathUtilities.`
> * Added `size(),`, `isEmpty(),` and `hasContent` to `CollectionUtilities.`
#### 2.9.0
> * Added `SealableList` which provides a `List` (or `List` wrapper) that will make it read-only (sealed) or read-write (unsealed), controllable via a `Supplier<Boolean>.`  This moves the immutability control outside the list and ensures that all views on the `List` respect the sealed-ness.  One master supplier can control the immutability of many collections.
> * Added `SealableSet` similar to SealableList but with `Set` nature.
> * Added `SealableMap` similar to SealableList but with `Map` nature.
> * Added `SealableNavigableSet` similar to SealableList but with `NavigableSet` nature.
> * Added `SealableNavigableMap` similar to SealableList but with `NavigableMap` nature.
> * Updated `ConcurrentList` to support wrapping any `List` and making it thread-safe, including all view APIs: `iterator(),` `listIterator(),` `listIterator(index).` The no-arg constructor creates a `ConcurrentList`  ready-to-go. The constructor that takes a `List` parameter constructor wraps the passed in list and makes it thread-safe.
> * Renamed `ConcurrentHashSet` to `ConcurrentSet.`
#### 2.8.0
> * Added `ClassUtilities.doesOneWrapTheOther()` API so that it is easy to test if one class is wrapping the other.
> * Added `StringBuilder` and `StringBuffer`  to `Strings` to the `Converter.` Eliminates special cases for `.toString()` calls where generalized `convert(src, type)` is being used.
#### 2.7.0
> * Added `ConcurrentList,` which implements a thread-safe `List.` Provides all API support except for `listIterator(),` however, it implements `iterator()` which returns an iterator to a snapshot copy of the `List.`
> * Added `ConcurrentHashSet,` a true `Set` which is a bit easier to use than `ConcurrentSkipListSet,` which as a `NavigableSet` and `SortedSet,` requires each element to be `Comparable.`
> * Performance improvement: On `LRUCache,` removed unnecessary `Collections.SynchronizedMap` surrounding the internal `LinkedHashMap` as the concurrent protection offered by `ReentrantReadWriteLock` is all that is needed.
#### 2.6.0
> * Performance improvement: `Converter` instance creation is faster due to the code no longer copying the static default table.  Overrides are kept in separate variable.
> * New capability added: `MathUtilities.parseToMinimalNumericType()` which will parse a String number into a Long, BigInteger, Double, or BigDecimal, choosing the "smallest" datatype to represent the number without loss of precision.
> * New conversions added to convert from `Map` to `StringBuilder` and `StringBuffer.`
#### 2.5.0
> * pom.xml file updated to support both OSGi Bundle and JPMS Modules.
> * module-info.class resides in the root of the .jar but it is not referenced.
#### 2.4.9
> * Updated to allow the project to be compiled by versions of JDK > 1.8 yet still generate class file format 52 .class files so that they can be executed on JDK 1.8+ and up.
> * Incorporated @AxataDarji GraphComparator changes that reduce cyclomatic code complexity (refactored to smaller methods)
#### 2.4.8
> * Performance improvement: `DeepEquals.deepHashCode()` - now using `IdentityHashMap()` for cycle (visited) detection.
> * Modernization: `UniqueIdGenerator` - updated to use `Lock.lock()` and `Lock.unlock()` instead of `synchronized` keyword.
> * Using json-io 4.14.1 for cloning object in "test" scope, eliminates cycle depedencies when building both json-io and java-util.
#### 2.4.7
> * All 687 conversions supported are now 100% cross-product tested.  Converter test suite is complete.
#### 2.4.6
> * All 686 conversions supported are now 100% cross-product tested.  There will be more exception tests coming.
#### 2.4.5
> * Added `ReflectionUtils.getDeclaredFields()` which gets fields from a `Class`, including an `Enum`, and special handles enum so that system fields are not returned.
#### 2.4.4
> * `Converter` - Enum test added.  683 combinations.
#### 2.4.3
> * `DateUtilities` - now supports timezone offset with seconds component (rarer than seeing a bald eagle in your backyard).
> * `Converter` - many more tests added...682 combinations.
#### 2.4.2
> * Fixed compatibility issues with `StringUtilities.` Method parameters changed from String to CharSequence broke backward compatibility.  Linked jars are bound to method signature at compile time, not at runtime. Added both methods where needed.  Removed methods with "Not" in the name.
> * Fixed compatibility issue with `FastByteArrayOutputStream.` The `.getBuffer()` API was removed in favor of toByteArray(). Now both methods exist, leaving `getBuffer()` for backward compatibility.
> * The Converter "Everything" test updated to track which pairs are tested (fowarded or reverse) and then outputs in order what tests combinations are left to write.
#### 2.4.1
> * `Converter` has had significant expansion in the types that it can convert between, about 670 combinations.  In addition, you can add your own conversions to it as well. Call the `Converter.getSupportedConversions()` to see all the combinations supported.  Also, you can use `Converter` instance-based now, allowing it to have different conversion tables if needed.
> * `DateUtilities` has had performance improvements (> 35%), and adds a new `.parseDate()` API that allows it to return a `ZonedDateTime.` See the updated Javadoc on the class for a complete description of all the formats it supports.  Normally, you do not need to use this class directly, as you can use `Converter` to convert between `Dates`, `Calendars`, and the new Temporal classes like `ZonedDateTime,` `Duration,` `Instance,` as well as Strings.
> * `FastByteArrayOutputStream` updated to match `ByteArrayOutputStream` API. This means that `.getBuffer()` is `.toByteArray()` and `.clear()` is now `.reset().`
> * `FastByteArrayInputStream` added.  Matches `ByteArrayInputStream` API.
> * Bug fix: `SafeSimpleDateFormat` to properly format dates having years with fewer than four digits.
> * Bug fix: SafeSimpleDateFormat .toString(), .hashCode(), and .equals() now delegate to the contain SimpleDataFormat instance.  We recommend using the newer DateTimeFormatter, however, this class works well for Java 1.8+ if needed.
#### 2.4.0
> * Added ClassUtilities.  This class has a method to get the distance between a source and destination class.  It includes support for Classes, multiple inheritance of interfaces, primitives, and class-to-interface, interface-interface, and class to class.
> * Added LRUCache.  This class provides a simple cache API that will evict the least recently used items, once a threshold is met.
#### 2.3.0
> Added
> `FastReader` and `FastWriter.`
>    * `FastReader` can be used instead of the JDK `PushbackReader(BufferedReader)).` It is much faster with no synchronization and combines both.  It also tracks line `[getLine()]`and column `[getCol()]` position monitoring for `0x0a` which it can be queried for.  It also can be queried for the last snippet read: `getLastSnippet().`  Great for showing parsing error messages that accurately point out where a syntax error occurred.  Make sure you use a new instance per each thread.
>    * `FastWriter` can be used instead of the JDK `BufferedWriter` as it has no synchronization.  Make sure you use a new Instance per each thread.
#### 2.2.0
> * Built with JDK 1.8 and runs with JDK 1.8 through JDK 21.
> * The 2.2.x will continue to maintain JDK 1.8.  The 3.0 branch [not yet created] will be JDK11+
> * Added tests to verify that `GraphComparator` and `DeepEquals` do not count sorted order of Sets for equivalency.  It does however, require `Collections` that are not `Sets` to be in order.
#### 2.1.1
> * ReflectionUtils skips static fields, speeding it up and remove runtime warning (field SerialVersionUID).  Supports JDK's up through 21.
#### 2.1.0
> * `DeepEquals.deepEquals(a, b)` compares Sets and Maps without regards to order per the equality spec.
> * Updated all dependent libraries to latest versions as of 16 Sept 2023.
#### 2.0.0
> * Upgraded from Java 8 to Java 11.
> * Updated `ReflectionUtils.getClassNameFromByteCode()` to handle up to Java 17 `class` file format.
#### 1.68.0
> * Fixed: `UniqueIdGenerator` now correctly gets last two digits of ID using 3 attempts - JAVA_UTIL_CLUSTERID (optional), CF_INSTANCE_INDEX, and finally using SecuritRandom for the last two digits.
> * Removed `log4j` in favor of `slf4j` and `logback`.
#### 1.67.0
> * Updated log4j dependencies to version `2.17.1`.
#### 1.66.0
> * Updated log4j dependencies to version `2.17.0`.
#### 1.65.0
> * Bug fix: Options (IGNORE_CUSTOM_EQUALS and ALLOW_STRINGS_TO_MATCH_NUMBERS) were not propagated inside containers\
> * Bug fix: When progagating options the Set of visited ItemsToCompare (or a copy if it) should be passed on to prevent StackOverFlow from occurring.
#### 1.64.0
> * Performance Improvement: `DateUtilities` now using non-greedy matching for regex's within date sub-parts.
> * Performance Improvement: `CompactMap` updated to use non-copying iterator for all non-Sorted Maps.
> * Performance Improvement: `StringUtilities.hashCodeIgnoreCase()` slightly faster - calls JDK method that makes one less call internally.
#### 1.63.0
> * Performance Improvement: Anytime `CompactMap` / `CompactSet` is copied internally, the destination map is pre-sized to correct size, eliminating growing underlying Map more than once.
> * `ReflectionUtils.getConstructor()` added.  Fetches Constructor, caches reflection operation - 2nd+ calls pull from cache.
#### 1.62.0
> * Updated `DateUtilities` to handle sub-seconds precision more robustly.
> * Updated `GraphComparator` to add missing srcValue when MAP_PUT replaces existing value. @marcobjorge
#### 1.61.0
> * `Converter` now supports `LocalDate`, `LocalDateTime`, `ZonedDateTime` to/from `Calendar`, `Date`, `java.sql.Date`, `Timestamp`, `Long`, `BigInteger`, `BigDecimal`, `AtomicLong`, `LocalDate`, `LocalDateTime`, and `ZonedDateTime`.
#### 1.60.0  [Java 1.8+]
> * Updated to require Java 1.8 or newer.
> * `UniqueIdGenerator` will recognize Cloud Foundry `CF_INSTANCE_INDEX`, in addition to `JAVA_UTIL_CLUSTERID` as an environment variable or Java system property.  This will be the last two digits of the generated unique id (making it cluster safe).  Alternatively, the value can be the name of another environment variable (detected by not being parseable as an int), in which case the value of the specified environment variable will be parsed as server id within cluster (value parsed as int, mod 100).
> * Removed a bunch of Javadoc warnings from build.
#### 1.53.0  [Java 1.7+]
> * Updated to consume `log4j 2.13.3` - more secure.
#### 1.52.0
> * `ReflectionUtils` now caches the methods it finds by `ClassLoader` and `Class`.  Earlier, found methods were cached per `Class`. This did not handle the case when multiple `ClassLoaders` were used to load the same class with the same method.  Using `ReflectionUtils` to locate the `foo()` method will find it in `ClassLoaderX.ClassA.foo()` (and cache it as such), and if asked to find it in `ClassLoaderY.ClassA.foo()`, `ReflectionUtils` will not find it in the cache with `ClassLoaderX.ClassA.foo()`, but it will fetch it from `ClassLoaderY.ClassA.foo()` and then cache the method with that `ClassLoader/Class` pairing.
> * `DeepEquals.equals()` was not comparing `BigDecimals` correctly.  If they had different scales but represented the same value, it would return `false`.  Now they are properly compared using `bd1.compareTo(bd2) == 0`.
> * `DeepEquals.equals(x, y, options)` has a new option.  If you add `ALLOW_STRINGS_TO_MATCH_NUMBERS` to the options map, then if a `String` is being compared to a `Number` (or vice-versa), it will convert the `String` to a `BigDecimal` and then attempt to see if the values still match.  If so, then it will continue.  If it could not convert the `String` to a `Number`, or the converted `String` as a `Number` did not match, `false` is returned.
> * `convertToBigDecimal()` now handles very large `longs` and `AtomicLongs` correctly (before it returned `false` if the `longs` were greater than a `double's` max integer representation.)
> * `CompactCIHashSet` and `CompactCILinkedHashSet` now return a new `Map` that is sized to `compactSize() + 1` when switching from internal storage to `HashSet` / `LinkedHashSet` for storage.  This is purely a performance enhancement.
#### 1.51.0
> New Sets:
>    * `CompactCIHashSet` added. This `CompactSet` expands to a case-insensitive `HashSet` when `size() > compactSize()`.
>    * `CompactCILinkedSet` added. This `CompactSet` expands to a case-insensitive `LinkedHashSet` when `size() > compactSize()`.
>    * `CompactLinkedSet` added.  This `CompactSet` expands to a `LinkedHashSet` when `size() > compactSize()`.
>    * `CompactSet` exists. This `CompactSet` expands to a `HashSet` when `size() > compactSize()`.
>
> New Maps:
>    * `CompactCILinkedMap` exists. This `CompactMap` expands to a case-insensitive `LinkedHashMap` when `size() > compactSize()` entries.
>    * `CompactCIHashMap` exists.  This `CompactMap` expands to a case-insensitive `HashMap` when `size() > compactSize()` entries.
>    * `CompactLinkedMap` added.  This `CompactMap` expands to a `LinkedHashMap` when `size() > compactSize()` entries.
>    * `CompactMap` exists.  This `CompactMap` expands to a `HashMap` when `size() > compactSize()` entries.
#### 1.50.0
> * `CompactCIHashMap` added.  This is a `CompactMap` that is case insensitive.  When more than `compactSize()` entries are stored in it (default 50), it uses a `CaseInsenstiveMap` `HashMap` to hold its entries.
> * `CompactCILinkedMap` added.  This is a `CompactMap` that is case insensitive.  When more than `compactSize()` entries are stored in it (default 50), it uses a `CaseInsenstiveMap` `LinkedHashMap` to hold its entries.
> * Bug fix: `CompactMap` `entrySet()` and `keySet()` were not handling the `retainAll()`, `containsAll()`, and `removeAll()` methods case-insensitively when case-insensitivity was activated.
> * `Converter` methods that convert to byte, short, int, and long now accepted String decimal numbers.  The decimal portion is truncated.
#### 1.49.0
> * Added `CompactSet`.  Works similarly to `CompactMap` with single `Object[]` holding elements until it crosses `compactSize()` threshold.
  This `Object[]` is adjusted dynamically as objects are added and removed.
#### 1.48.0
> * Added `char` and `Character` support to `Convert.convert*()`
> * Added full Javadoc to `Converter`.
> * Performance improvement in `Iterator.remove()` for all of `CompactMap's` iterators: `keySet().iterator()`, `entrySet().iterator`, and `values().iterator`.
> * In order to get to 100% code coverage with Jacoco, added more tests for `Converter`, `CaseInsenstiveMap`, and `CompactMap`.
#### 1.47.0
> * `Converter.convert2*()` methods added: If `null` passed in, primitive 'logical zero' is returned. Example: `Converter.convert(null, boolean.class)` returns `false`.
> * `Converter.convertTo*()` methods: if `null` passed in, `null` is returned.  Allows "tri-state" Boolean. Example: `Converter.convert(null, Boolean.class)` returns `null`.
> * `Converter.convert()` converts using `convertTo*()` methods for primitive wrappers, and `convert2*()` methods for primitive classes.
> * `Converter.setNullMode()` removed.
#### 1.46.0
> * `CompactMap` now supports 4 stages of "growth", making it much smaller in memory than nearly any `Map`.  After `0` and `1` entries,
  and between `2` and `compactSize()` entries, the entries in the `Map` are stored in an `Object[]` (using same single member variable).  The
  even elements the 'keys' and the odd elements are the associated 'values'.  This array is dynamically resized to exactly match the number of stored entries.
  When more than `compactSize()` entries are used, the `Map` then uses the `Map` returned from the overrideable `getNewMap()` api to store the entries.
  In all cases, it maintains the underlying behavior of the `Map`.
> * Updated to consume `log4j 2.13.1`
#### 1.45.0
> * `CompactMap` now supports case-insensitivity when using String keys.  By default, it is case sensitive, but you can override the
  `isCaseSensitive()` method and return `false`.  This allows you to return `TreeMap(String.CASE_INSENSITIVE_ORDER)` or `CaseInsensitiveMap`
  from the `getNewMap()` method.  With these overrides, CompactMap is now case insensitive, yet still 'compact.'
> * `Converter.setNullMode(Converter.NULL_PROPER | Converter.NULL_NULL)` added to allow control over how `null` values are converted.
  By default, passing a `null` value into primitive `convert*()` methods returns the primitive form of `0` or `false`.
  If the static method `Converter.setNullMode(Converter.NULL_NULL)` is called it will change the behavior of the primitive
  `convert*()` methods return `null`.
#### 1.44.0
> * `CompactMap` introduced.
  `CompactMap` is a `Map` that strives to reduce memory at all costs while retaining speed that is close to `HashMap's` speed.
  It does this by using only one (1) member variable (of type `Object`) and changing it as the `Map` grows.  It goes from
  single value, to a single `Map Entry`, to an `Object[]`, and finally it uses a `Map` (user defined).  `CompactMap` is
  especially small when `0` or `1` entries are stored in it.  When `size()` is from `2` to `compactSize()`, then entries
  are stored internally in single `Object[]`.  If the `size() > compactSize()` then the entries are stored in a
  regular `Map`.
>  ```
>     // If this key is used and only 1 element then only the value is stored
>     protected K getSingleValueKey() { return "someKey"; }
>
>     // Map you would like it to use when size() > compactSize().  HashMap is default
>     protected abstract Map<K, V> getNewMap();
>
>     // If you want case insensitivity, return true and return new CaseInsensitiveMap or TreeMap(String.CASE_INSENSITIVE_PRDER) from getNewMap()
>     protected boolean isCaseInsensitive() { return false; }        // 1.45.0
>
>     // When size() > than this amount, the Map returned from getNewMap() is used to store elements.
>     protected int compactSize() { return 100; }                    // 1.46.0
>  ```
>    ##### **Empty**
>    This class only has one (1) member variable of type `Object`.  If there are no entries in it, then the value of that
>    member variable takes on a pointer (points to sentinel value.)
>    ##### **One entry**
>    If the entry has a key that matches the value returned from `getSingleValueKey()` then there is no key stored
>    and the internal single member points to the value (still retried with 100% proper Map semantics).
>
>    If the single entry's key does not match the value returned from `getSingleValueKey()` then the internal field points
>    to an internal `Class` `CompactMapEntry` which contains the key and the value (nothing else).  Again, all APIs still operate
>    the same.
>   ##### **2 thru compactSize() entries**
>   In this case, the single member variable points to a single Object[] that contains all the keys and values.  The
>   keys are in the even positions, the values are in the odd positions (1 up from the key).  [0] = key, [1] = value,
>   [2] = next key, [3] = next value, and so on.  The Object[] is dynamically expanded until size() > compactSize(). In
>   addition, it is dynamically shrunk until the size becomes 1, and then it switches to a single Map Entry or a single
>   value.
>
>   ##### **size() > compactSize()**
>   In this case, the single member variable points to a `Map` instance (supplied by `getNewMap()` API that user supplied.)
>   This allows `CompactMap` to work with nearly all `Map` types.
>   This Map supports null for the key and values, as long as the Map returned by getNewMap() supports null keys-values.
#### 1.43.0
> * `CaseInsensitiveMap(Map orig, Map backing)` added for allowing precise control of what `Map` instance is used to back the `CaseInsensitiveMap`.  For example,
>  ```
>    Map originalMap = someMap  // has content already in it
>    Map ciMap1 = new CaseInsensitiveMap(someMap, new TreeMap())  // Control Map type, but not initial capacity
>    Map ciMap2 = new CaseInsensitiveMap(someMap, new HashMap(someMap.size()))    // Control both Map type and initial capacity
>    Map ciMap3 = new CaseInsensitiveMap(someMap, new Object2ObjectOpenHashMap(someMap.size()))   // Control initial capacity and use specialized Map from fast-util.
>  ```
> * `CaseInsensitiveMap.CaseInsensitiveString()` constructor made `public`.
#### 1.42.0
> * `CaseInsensitiveMap.putObject(Object key, Object value)` added for placing objects into typed Maps.
#### 1.41.0
> * `CaseInsensitiveMap.plus()` and `.minus()` added to support `+` and `-` operators in languages like Groovy.
> * `CaseInsenstiveMap.CaseInsensitiveString` (`static` inner Class) is now `public`.
#### 1.40.0
> * Added `ReflectionUtils.getNonOverloadedMethod()` to support reflectively fetching methods with only Class and Method name available.  This implies there is no method overloading.
#### 1.39.0
> * Added `ReflectionUtils.call(bean, methodName, args...)` to allow one-step reflective calls.  See Javadoc for any limitations.
> * Added `ReflectionUtils.call(bean, method, args...)` to allow easy reflective calls.  This version requires obtaining the `Method` instance first.  This approach allows methods with the same name and number of arguments (overloaded) to be called.
> * All `ReflectionUtils.getMethod()` APIs cache reflectively located methods to significantly improve performance when using reflection.
> * The `call()` methods throw the target of the checked `InvocationTargetException`.  The checked `IllegalAccessException` is rethrown wrapped in a RuntimeException.  This allows making reflective calls without having to handle these two checked exceptions directly at the call point. Instead, these exceptions are usually better handled at a high-level in the code.
#### 1.38.0
> * Enhancement: `UniqueIdGenerator` now generates the long ids in monotonically increasing order.  @HonorKnight
> * Enhancement: New API [`getDate(uniqueId)`] added to `UniqueIdGenerator` that when passed an ID that it generated, will return the time down to the millisecond when it was generated.
#### 1.37.0
> * `TestUtil.assertContainsIgnoreCase()` and `TestUtil.checkContainsIgnoreCase()` APIs added.  These are generally used in unit tests to check error messages for key words, in order (as opposed to doing `.contains()` on a string which allows the terms to appear in any order.)
> * Build targets classes in Java 1.7 format, for maximum usability.  The version supported will slowly move up, but only based on necessity allowing for widest use of java-util in as many projects as possible.
#### 1.36.0
> * `Converter.convert()` now bi-directionally supports `Calendar.class`, e.g. Calendar to Date, SqlDate, Timestamp, String, long, BigDecimal, BigInteger, AtomicLong, and vice-versa.
> * `UniqueIdGenerator.getUniqueId19()` is a new API for getting 19 digit unique IDs (a full `long` value)  These are generated at a faster rate (10,000 per millisecond vs. 1,000 per millisecond) than the original (18-digit) API.
> * Hardcore test added for ensuring concurrency correctness with `UniqueIdGenerator`.
> * Javadoc beefed up for `UniqueIdGenerator`.
> * Updated public APIs to have proper support for generic arguments.  For example Class&lt;T&gt;, Map&lt;?, ?&gt;, and so on.  This eliminates type casting on the caller's side.
> * `ExceptionUtilities.getDeepestException()` added.  This API locates the source (deepest) exception.
#### 1.35.0
> * `DeepEquals.deepEquals()`, when comparing `Maps`, the `Map.Entry` type holding the `Map's` entries is no longer considered in equality testing. In the past, a custom Map.Entry instance holding the key and value could cause inquality, which should be ignored.  @AndreyNudko
> * `Converter.convert()` now uses parameterized types so that the return type matches the passed in `Class` parameter.  This eliminates the need to cast the return value of `Converter.convert()`.
> * `MapUtilities.getOrThrow()` added which throws the passed in `Throwable` when the passed in key is not within the `Map`. @ptjuanramos
#### 1.34.2
> * Performance Improvement: `CaseInsensitiveMap`, when created from another `CaseInsensitiveMap`, re-uses the internal `CaseInsensitiveString` keys, which are immutable.
> * Bug fix: `Converter.convertToDate(), Converter.convertToSqlDate(), and Converter.convertToTimestamp()` all threw a `NullPointerException` if the passed in content was an empty String (of 0 or more spaces). When passed in NULL to these APIs, you get back null.  If you passed in empty strings or bad date formats, an IllegalArgumentException is thrown with a message clearly indicating what input failed and why.
#### 1.34.0
> * Enhancement: `DeepEquals.deepEquals(a, b options)` added.  The new options map supports a key `DeepEquals.IGNORE_CUSTOM_EQUALS` which can be set to a Set of String class names.  If any of the encountered classes in the comparison are listed in the Set, and the class has a custom `.equals()` method, it will not be called and instead a `deepEquals()` will be performed.  If the value associated to the `IGNORE_CUSTOM_EQUALS` key is an empty Set, then no custom `.equals()` methods will be called, except those on primitives, primitive wrappers, `Date`, `Class`, and `String`.
#### 1.33.0
> * Bug fix: `DeepEquals.deepEquals(a, b)` could report equivalent unordered `Collections` / `Maps` as not equal if the items in the `Collection` / `Map` had the same hash code.
#### 1.32.0
> * `Converter` updated to expose `convertTo*()` APIs that allow converting to a known type.
#### 1.31.1
> * Renamed `AdjustableFastGZIPOutputStream` to `AdjustableGZIPOutputStream`.
#### 1.31.0
> * Add `AdjustableFastGZIPOutputStream` so that compression level can be adjusted.
#### 1.30.0
> * `ByteArrayOutputStreams` converted to `FastByteArrayOutputStreams` internally.
#### 1.29.0
> * Removed test dependencies on Guava
> * Rounded out APIs on `FastByteArrayOutputStream`
> * Added APIs to `IOUtilities`.
#### 1.28.2
> * Enhancement: `IOUtilities.compressBytes(FastByteArrayOutputStream, FastByteArrayOutputStream)` added.
#### 1.28.1
> * Enhancement: `FastByteArrayOutputStream.getBuffer()` API made public.
#### 1.28.0
> * Enhancement: `FastByteArrayOutputStream` added.  Similar to JDK class, but without `synchronized` and access to inner `byte[]` allowed without duplicating the `byte[]`.
#### 1.27.0
> * Enhancement: `Converter.convert()` now supports `enum` to `String`
#### 1.26.1
> * Bug fix: The internal class `CaseInsensitiveString` did not implement `Comparable` interface correctly.
#### 1.26.0
> * Enhancement: added `getClassNameFromByteCode()` API to `ReflectionUtils`.
#### 1.25.1
> * Enhancement: The Delta object returned by `GraphComparator` implements `Serializable` for those using `ObjectInputStream` / `ObjectOutputStream`.  Provided by @metlaivan (Ivan Metla)
#### 1.25.0
> * Performance improvement: `CaseInsensitiveMap/Set` internally adds `Strings` to `Map` without using `.toLowerCase()` which eliminates creating a temporary copy on the heap of the `String` being added, just to get its lowerCaseValue.
> * Performance improvement: `CaseInsensitiveMap/Set` uses less memory internally by caching the hash code as an `int`, instead of an `Integer`.
> * `StringUtilities.caseInsensitiveHashCode()` API added.  This allows computing a case-insensitive hashcode from a `String` without any object creation (heap usage).
#### 1.24.0
> * `Converter.convert()` - performance improved using class instance comparison versus class `String` name comparison.
> * `CaseInsensitiveMap/Set` - performance improved.  `CaseInsensitiveString` (internal) short-circuits on equality check if hashCode() [cheap runtime cost] is not the same.  Also, all method returning true/false to detect if `Set` or `Map` changed rely on size() instead of contains.
#### 1.23.0
> * `Converter.convert()` API update: When a mutable type (`Date`, `AtomicInteger`, `AtomicLong`, `AtomicBoolean`) is passed in, and the destination type is the same, rather than return the instance passed in, a copy of the instance is returned.
#### 1.22.0
> * Added `GraphComparator` which is used to compute the difference (delta) between two object graphs.  The generated `List` of Delta objects can be 'played' against the source to bring it up to match the target.  Very useful in transaction processing systems.
#### 1.21.0
> * Added `Executor` which is used to execute Operating System commands.  For example, `Executor exector = new Executor(); executor.exec("echo This is handy");  assertEquals("This is handy", executor.getOut().trim());`
> * bug fix: `CaseInsensitiveMap`, when passed a `LinkedHashMap`, was inadvertently using a HashMap instead.
#### 1.20.5
> * `CaseInsensitiveMap` intentionally does not retain 'not modifiability'.
> * `CaseInsensitiveSet` intentionally does not retain 'not modifiability'.
#### 1.20.4
> * Failed release.  Do not use.
#### 1.20.3
> * `TrackingMap` changed so that `get(anyKey)` always marks it as keyRead.  Same for `containsKey(anyKey)`.
> * `CaseInsensitiveMap` has a constructor that takes a `Map`, which allows it to take on the nature of the `Map`, allowing for case-insensitive `ConcurrentHashMap`, sorted `CaseInsensitiveMap`, etc.  The 'Unmodifiable' `Map` nature is intentionally not taken on.  The passed in `Map` is not mutated.
> * `CaseInsensitiveSet` has a constructor that takes a `Collection`, which allows it to take on the nature of the `Collection`, allowing for sorted `CaseInsensitiveSets`.  The 'unmodifiable' `Collection` nature is intentionally not taken on.  The passed in `Set` is not mutated.
#### 1.20.2
> * `TrackingMap` changed so that an existing key associated to null counts as accessed. It is valid for many `Map` types to allow null values to be associated to the key.
> * `TrackingMap.getWrappedMap()` added so that you can fetch the wrapped `Map`.
#### 1.20.1
> * `TrackingMap` changed so that `.put()` does not mark the key as accessed.
#### 1.20.0
> * `TrackingMap` added.  Create this map around any type of Map, and it will track which keys are accessed via .get(), .containsKey(), or .put() (when put overwrites a value already associated to the key).  Provided by @seankellner.
#### 1.19.3
> * Bug fix: `CaseInsensitiveMap.entrySet()` - calling `entry.setValue(k, v)` while iterating the entry set, was not updating the underlying value.  This has been fixed and test case added.
#### 1.19.2
> * The order in which system properties are read versus environment variables via the `SystemUtilities.getExternalVariable()` method has changed.  System properties are checked first, then environment variables.
#### 1.19.1
> * Fixed issue in `DeepEquals.deepEquals()` where a Container type (`Map` or `Collection`) was being compared to a non-container - the result of this comparison was inconsistent.   It is always false if a Container is compared to a non-container type (anywhere within the object graph), regardless of the comparison order A, B versus comparing B, A.
#### 1.19.0
> * `StringUtilities.createUtf8String(byte[])` API added which is used to easily create UTF-8 strings without exception handling code.
> * `StringUtilities.getUtf8Bytes(String s)` API added which returns a byte[] of UTF-8 bytes from the passed in Java String without any exception handling code required.
> * `ByteUtilities.isGzipped(bytes[])` API added which returns true if the `byte[]` represents gzipped data.
> * `IOUtilities.compressBytes(byte[])` API added which returns the gzipped version of the passed in `byte[]` as a `byte[]`
> * `IOUtilities.uncompressBytes(byte[])` API added which returns the original byte[] from the passed in gzipped `byte[]`.
> * JavaDoc issues correct to support Java 1.8 stricter JavaDoc compilation.
#### 1.18.1
> * `UrlUtilities` now allows for per-thread `userAgent` and `referrer` as well as maintains backward compatibility for setting these values globally.
> * `StringUtilities` `getBytes()` and `createString()` now allow null as input, and return null for output for null input.
> * Javadoc updated to remove errors flagged by more stringent Javadoc 1.8 generator.
#### 1.18.0
> * Support added for `Timestamp` in `Converter.convert()`
> * `null` can be passed into `Converter.convert()` for primitive types, and it will return their logical 0 value (0.0f, 0.0d, etc.).  For primitive wrappers, atomics, etc, null will be returned.
> * "" can be passed into `Converter.convert()` and it will set primitives to 0, and the object types (primitive wrappers, dates, atomics) to null.  `String` will be set to "".
#### 1.17.1
> * Added full support for `AtomicBoolean`, `AtomicInteger`, and `AtomicLong` to `Converter.convert(value, AtomicXXX)`.  Any reasonable value can be converted to/from these, including Strings, Dates (`AtomicLong`), all `Number` types.
> * `IOUtilities.flush()` now supports `XMLStreamWriter`
#### 1.17.0
> * `UIUtilities.close()` now supports `XMLStreamReader` and `XMLStreamWriter` in addition to `Closeable`.
> * `Converter.convert(value, type)` - a value of null is supported for the numeric types, boolean, and the atomics - in which case it returns their "zero" value and false for boolean.  For date and String return values, a null input will return null.  The `type` parameter must not be null.
#### 1.16.1
> * In `Converter.convert(value, type)`, the value is trimmed of leading / trailing white-space if it is a String and the type is a `Number`.
#### 1.16.0
> * Added `Converter.convert()` API.  Allows converting instances of one type to another.  Handles all primitives, primitive wrappers, `Date`, `java.sql.Date`, `String`, `BigDecimal`,  `BigInteger`, `AtomicInteger`, `AtomicLong`, and `AtomicBoolean`.  Additionally, input (from) argument accepts `Calendar`.
> * Added static `getDateFormat()` to `SafeSimpleDateFormat` for quick access to thread local formatter (per format `String`).
#### 1.15.0
> * Switched to use Log4J2 () for logging.
#### 1.14.1
> * bug fix: `CaseInsensitiveMap.keySet()` was only initializing the iterator once.  If `keySet()` was called a 2nd time, it would no longer work.
#### 1.14.0
> * bug fix: `CaseInsensitiveSet()`, the return value for `addAll()`, `returnAll()`, and `retainAll()` was wrong in some cases.
#### 1.13.3
> * `EncryptionUtilities` - Added byte[] APIs.  Makes it easy to encrypt/decrypt `byte[]` data.
> * `pom.xml` had extraneous characters inadvertently added to the file - these are removed.
> * 1.13.1 & 13.12 - issues with sonatype
#### 1.13.0
> * `DateUtilities` - Day of week allowed (properly ignored).
> * `DateUtilities` - First (st), second (nd), third (rd), and fourth (th) ... supported.
> * `DateUtilities` - The default toString() standard date / time displayed by the JVM is now supported as a parseable format.
> * `DateUtilities` - Extra whitespace can exist within the date string.
> * `DateUtilities` - Full time zone support added.
> * `DateUtilities` - The date (or date time) is expected to be in isolation. Whitespace on either end is fine, however, once the date time is parsed from the string, no other content can be left (prevents accidently parsing dates from dates embedded in text).
> * `UrlUtilities` - Removed proxy from calls to `URLUtilities`.  These are now done through the JVM.
#### 1.12.0
> * `UniqueIdGenerator` uses 99 as the cluster id when the JAVA_UTIL_CLUSTERID environment variable or System property is not available.  This speeds up execution on developer's environments when they do not specify `JAVA_UTIL_CLUSTERID`.
> * All the 1.11.x features rolled up.
#### 1.11.3
> * `UrlUtilities` - separated out call that resolves `res://` to a public API to allow for wider use.
#### 1.11.2
> * Updated so headers can be set individually by the strategy (`UrlInvocationHandler`)
> * `InvocationHandler` set to always uses `POST` method to allow additional `HTTP` headers.
#### 1.11.1
> * Better IPv6 support (`UniqueIdGenerator`)
> * Fixed `UrlUtilities.getContentFromUrl()` (`byte[]`) no longer setting up `SSLFactory` when `HTTP` protocol used.
#### 1.11.0
> * `UrlInvocationHandler`, `UrlInvocationStrategy` - Updated to allow more generalized usage. Pass in your implementation of `UrlInvocationStrategy` which allows you to set the number of retry attempts, fill out the URL pattern, set up the POST data, and optionally set/get cookies.
> * Removed dependency on json-io.  Only remaining dependency is Apache commons-logging.
#### 1.10.0
> * Issue #3 fixed: `DeepEquals.deepEquals()` allows similar `Map` (or `Collection`) types to be compared without returning 'not equals' (false).  Example, `HashMap` and `LinkedHashMap` are compared on contents only.  However, compare a `SortedSet` (like `TreeMap`) to `HashMap` would fail unless the Map keys are in the same iterative order.
> * Tests added for `UrlUtilities`
> * Tests added for `Traverser`
#### 1.9.2
> * Added wildcard to regex pattern to `StringUtilities`.  This API turns a DOS-like wildcard pattern (where  * matches anything and ? matches a single character) into a regex pattern useful in `String.matches()` API.
#### 1.9.1
> * Floating-point allow difference by epsilon value (currently hard-coded on `DeepEquals`.  Will likely be optional parameter in future version).
#### 1.9.0
> * `MathUtilities` added.  Currently, variable length `minimum(arg0, arg1, ... argn)` and `maximum()` functions added.  Available for `long`, `double`, `BigInteger`, and `BigDecimal`.   These cover the smaller types.
> * `CaseInsensitiveMap` and `CaseInsensitiveSet` `keySet()` and `entrySet()` are faster as they do not make a copy of the entries.  Internally, `CaseInsensitiveString` caches it's hash, speeding up repeated access.
> * `StringUtilities levenshtein()` and `damerauLevenshtein()` added to compute edit length.  See Wikipedia to understand of the difference between these two algorithms.  Currently recommend using `levenshtein()` as it uses less memory.
> * The Set returned from the `CaseInsensitiveMap.entrySet()` now contains mutable entry's (value-side). It had been using an immutable entry, which disallowed modification of the value-side during entry walk.
#### 1.8.4
> * `UrlUtilities`, fixed issue where the default settings for the connection were changed, not the settings on the actual connection.
#### 1.8.3
> * `ReflectionUtilities` has new `getClassAnnotation(classToCheck, annotation)` API which will return the annotation if it exists within the classes super class hierarchy or interface hierarchy.  Similarly, the `getMethodAnnotation()` API does the same thing for method annotations (allow inheritance - class or interface).
#### 1.8.2
> * `CaseInsensitiveMap` methods `keySet()` and `entrySet()` return Sets that are identical to how the JDK returns 'view' Sets on the underlying storage.  This means that all operations, besides `add()` and `addAll()`, are supported.
> * `CaseInsensitiveMap.keySet()` returns a `Set` that is case insensitive (not a `CaseInsensitiveSet`, just a `Set` that ignores case).  Iterating this `Set` properly returns each originally stored item.
#### 1.8.1
> * Fixed `CaseInsensitiveMap() removeAll()` was not removing when accessed via `keySet()`
#### 1.8.0
> * Added `DateUtilities`.  See description above.
#### 1.7.4
> * Added "res" protocol (resource) to `UrlUtilities` to allow files from classpath to easily be loaded.  Useful for testing.
#### 1.7.2
> * `UrlUtilities.getContentFromUrl() / getContentFromUrlAsString()` - removed hard-coded proxy server name
#### 1.7.1
> * `UrlUtilities.getContentFromUrl() / getContentFromUrlAsString()` - allow content to be fetched as `String` or binary (`byte[]`).
#### 1.7.0
> * `SystemUtilities` added.  New API to fetch value from environment or System property
> * `UniqueIdGenerator` - checks for environment variable (or System property) JAVA_UTIL_CLUSTERID (0-99).  Will use this if set, otherwise last IP octet mod 100.
#### 1.6.1
> * Added: `UrlUtilities.getContentFromUrl()`
#### 1.6.0
> * Added `CaseInsensitiveSet`.
#### 1.5.0
> * Fixed: `CaseInsensitiveMap's iterator.remove()` method, it did not remove items.
> * Fixed: `CaseInsensitiveMap's equals()` method, it required case to match on keys.
#### 1.4.0
> * Initial version
