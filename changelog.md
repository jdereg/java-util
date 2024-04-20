### Revision History
* 2.7.0
  * Added `ConcurrentHashList,` which implements a thread-safe `List.` Provides all API support except for `listIterator(),` however, it implements `iterator()` which returns an iterator to a snapshot copy of the `List.` 
  * Added `ConcurrentHashSet,` a true `Set` which is a bit easier to use than `ConcurrentSkipListSet,` which as a `NaviableSet` and `SortedSet,` requires each element to be `Comparable.`
  * Performance improvement: On `LRUCache,` removed unnecessary `Collections.SynchronizedMap` surrounding the internal `LinkedHashMap` as the concurrent protection offered by `ReentrantReadWriteLock` is all that is needed.
* 2.6.0
  * Performance improvement: `Converter` instance creation is faster due to the code no longer copying the static default table.  Overrides are kept in separate variable.
  * New capability added: `MathUtilities.parseToMinimalNumericType()` which will parse a String number into a Long, BigInteger, Double, or BigDecimal, choosing the "smallest" datatype to represent the number without loss of precision.
  * New conversions added to convert from `Map` to `StringBuilder` and `StringBuffer.`
* 2.5.0
  * pom.xml file updated to support both OSGi Bundle and JPMS Modules. 
  * module-info.class resides in the root of the .jar but it is not referenced.
* 2.4.9
  * Updated to allow the project to be compiled by versions of JDK > 1.8 yet still generate class file format 52 .class files so that they can be executed on JDK 1.8+ and up.
  * Incorporated @AxataDarji GraphComparator changes that reduce cyclomatic code complexity (refactored to smaller methods)
* 2.4.8
  * Performance improvement: `DeepEquals.deepHashCode()` - now using `IdentityHashMap()` for cycle (visited) detection.
  * Modernization: `UniqueIdGenerator` - updated to use `Lock.lock()` and `Lock.unlock()` instead of `synchronized` keyword.
  * Using json-io 4.14.1 for cloning object in "test" scope, eliminates cycle depedencies when building both json-io and java-util.
* 2.4.7
  * All 687 conversions supported are now 100% cross-product tested.  Converter test suite is complete.
* 2.4.6
  * All 686 conversions supported are now 100% cross-product tested.  There will be more exception tests coming.
* 2.4.5
  * Added `ReflectionUtils.getDeclaredFields()` which gets fields from a `Class`, including an `Enum`, and special handles enum so that system fields are not returned.
* 2.4.4
  * `Converter` - Enum test added.  683 combinations.
* 2.4.3
  * `DateUtilities` - now supports timezone offset with seconds component (rarer than seeing a bald eagle in your backyard).  
  * `Converter` - many more tests added...682 combinations.  
* 2.4.2
  * Fixed compatibility issues with `StringUtilities.` Method parameters changed from String to CharSequence broke backward compatibility.  Linked jars are bound to method signature at compile time, not at runtime. Added both methods where needed.  Removed methods with "Not" in the name.
  * Fixed compatibility issue with `FastByteArrayOutputStream.` The `.getBuffer()` API was removed in favor of toByteArray(). Now both methods exist, leaving `getBuffer()` for backward compatibility.
  * The Converter "Everything" test updated to track which pairs are tested (fowarded or reverse) and then outputs in order what tests combinations are left to write.
* 2.4.1
  * `Converter` has had significant expansion in the types that it can convert between, about 670 combinations.  In addition, you can add your own conversions to it as well. Call the `Converter.getSupportedConversions()` to see all the combinations supported.  Also, you can use `Converter` instance-based now, allowing it to have different conversion tables if needed.
  * `DateUtilities` has had performance improvements (> 35%), and adds a new `.parseDate()` API that allows it to return a `ZonedDateTime.` See the updated Javadoc on the class for a complete description of all the formats it supports.  Normally, you do not need to use this class directly, as you can use `Converter` to convert between `Dates`, `Calendars`, and the new Temporal classes like `ZonedDateTime,` `Duration,` `Instance,` as well as Strings. 
  * `FastByteArrayOutputStream` updated to match `ByteArrayOutputStream` API. This means that `.getBuffer()` is `.toByteArray()` and `.clear()` is now `.reset().`
  * `FastByteArrayInputStream` added.  Matches `ByteArrayInputStream` API.
  * Bug fix: `SafeSimpleDateFormat` to properly format dates having years with fewer than four digits.
  * Bug fix: SafeSimpleDateFormat .toString(), .hashCode(), and .equals() now delegate to the contain SimpleDataFormat instance.  We recommend using the newer DateTimeFormatter, however, this class works well for Java 1.8+ if needed.
* 2.4.0
  * Added ClassUtilities.  This class has a method to get the distance between a source and destination class.  It includes support for Classes, multiple inheritance of interfaces, primitives, and class-to-interface, interface-interface, and class to class.
  * Added LRUCache.  This class provides a simple cache API that will evict the least recently used items, once a threshold is met.
* 2.3.0
  * Added `FastReader` and `FastWriter.` 
    * `FastReader` can be used instead of the JDK `PushbackReader(BufferedReader)).` It is much faster with no synchronization and combines both.  It also tracks line `[getLine()]`and column `[getCol()]` position monitoring for `0x0a` which it can be queried for.  It also can be queried for the last snippet read: `getLastSnippet().`  Great for showing parsing error messages that accurately point out where a syntax error occurred.  Make sure you use a new instance per each thread.
    * `FastWriter` can be used instead of the JDK `BufferedWriter` as it has no synchronization.  Make sure you use a new Instance per each thread.
* 2.2.0
  * Built with JDK 1.8 and runs with JDK 1.8 through JDK 21.
  * The 2.2.x will continue to maintain JDK 1.8.  The 3.0 branch [not yet created] will be JDK11+
  * Added tests to verify that `GraphComparator` and `DeepEquals` do not count sorted order of Sets for equivalency.  It does however, require `Collections` that are not `Sets` to be in order.
* 2.1.1
  * ReflectionUtils skips static fields, speeding it up and remove runtime warning (field SerialVersionUID).  Supports JDK's up through 21.
* 2.1.0
  * `DeepEquals.deepEquals(a, b)` compares Sets and Maps without regards to order per the equality spec.
  * Updated all dependent libraries to latest versions as of 16 Sept 2023.
* 2.0.0
  * Upgraded from Java 8 to Java 11.
  * Updated `ReflectionUtils.getClassNameFromByteCode()` to handle up to Java 17 `class` file format.
* 1.68.0
  * Fixed: `UniqueIdGenerator` now correctly gets last two digits of ID using 3 attempts - JAVA_UTIL_CLUSTERID (optional), CF_INSTANCE_INDEX, and finally using SecuritRandom for the last two digits.  
  * Removed `log4j` in favor of `slf4j` and `logback`.
* 1.67.0
  * Updated log4j dependencies to version `2.17.1`.
* 1.66.0
  * Updated log4j dependencies to version `2.17.0`.
* 1.65.0
  * Bug fix: Options (IGNORE_CUSTOM_EQUALS and ALLOW_STRINGS_TO_MATCH_NUMBERS) were not propagated inside containers\
  * Bug fix: When progagating options the Set of visited ItemsToCompare (or a copy if it) should be passed on to prevent StackOverFlow from occurring.
* 1.64.0
  * Performance Improvement: `DateUtilities` now using non-greedy matching for regex's within date sub-parts.
  * Performance Improvement: `CompactMap` updated to use non-copying iterator for all non-Sorted Maps.  
  * Performance Improvement: `StringUtilities.hashCodeIgnoreCase()` slightly faster - calls JDK method that makes one less call internally.
* 1.63.0
  * Performance Improvement: Anytime `CompactMap` / `CompactSet` is copied internally, the destination map is pre-sized to correct size, eliminating growing underlying Map more than once.
  * `ReflectionUtils.getConstructor()` added.  Fetches Constructor, caches reflection operation - 2nd+ calls pull from cache.
* 1.62.0
  * Updated `DateUtilities` to handle sub-seconds precision more robustly.
  * Updated `GraphComparator` to add missing srcValue when MAP_PUT replaces existing value. @marcobjorge
* 1.61.0
  * `Converter` now supports `LocalDate`, `LocalDateTime`, `ZonedDateTime` to/from `Calendar`, `Date`, `java.sql.Date`, `Timestamp`, `Long`, `BigInteger`, `BigDecimal`, `AtomicLong`, `LocalDate`, `LocalDateTime`, and `ZonedDateTime`.
* 1.60.0  [Java 1.8+]
  * Updated to require Java 1.8 or newer.
  * `UniqueIdGenerator` will recognize Cloud Foundry `CF_INSTANCE_INDEX`, in addition to `JAVA_UTIL_CLUSTERID` as an environment variable or Java system property.  This will be the last two digits of the generated unique id (making it cluster safe).  Alternatively, the value can be the name of another environment variable (detected by not being parseable as an int), in which case the value of the specified environment variable will be parsed as server id within cluster (value parsed as int, mod 100).
  * Removed a bunch of Javadoc warnings from build.
* 1.53.0  [Java 1.7+]
  * Updated to consume `log4j 2.13.3` - more secure.
* 1.52.0
  * `ReflectionUtils` now caches the methods it finds by `ClassLoader` and `Class`.  Earlier, found methods were cached per `Class`. This did not handle the case when multiple `ClassLoaders` were used to load the same class with the same method.  Using `ReflectionUtils` to locate the `foo()` method will find it in `ClassLoaderX.ClassA.foo()` (and cache it as such), and if asked to find it in `ClassLoaderY.ClassA.foo()`, `ReflectionUtils` will not find it in the cache with `ClassLoaderX.ClassA.foo()`, but it will fetch it from `ClassLoaderY.ClassA.foo()` and then cache the method with that `ClassLoader/Class` pairing.
  * `DeepEquals.equals()` was not comparing `BigDecimals` correctly.  If they had different scales but represented the same value, it would return `false`.  Now they are properly compared using `bd1.compareTo(bd2) == 0`.
  * `DeepEquals.equals(x, y, options)` has a new option.  If you add `ALLOW_STRINGS_TO_MATCH_NUMBERS` to the options map, then if a `String` is being compared to a `Number` (or vice-versa), it will convert the `String` to a `BigDecimal` and then attempt to see if the values still match.  If so, then it will continue.  If it could not convert the `String` to a `Number`, or the converted `String` as a `Number` did not match, `false` is returned.
  * `convertToBigDecimal()` now handles very large `longs` and `AtomicLongs` correctly (before it returned `false` if the `longs` were greater than a `double's` max integer representation.)
  * `CompactCIHashSet` and `CompactCILinkedHashSet` now return a new `Map` that is sized to `compactSize() + 1` when switching from internal storage to `HashSet` / `LinkedHashSet` for storage.  This is purely a performance enhancement.
* 1.51.0
  * New Sets:
    * `CompactCIHashSet` added. This `CompactSet` expands to a case-insensitive `HashSet` when `size() > compactSize()`.
    * `CompactCILinkedSet` added. This `CompactSet` expands to a case-insensitive `LinkedHashSet` when `size() > compactSize()`.
    * `CompactLinkedSet` added.  This `CompactSet` expands to a `LinkedHashSet` when `size() > compactSize()`.
    * `CompactSet` exists. This `CompactSet` expands to a `HashSet` when `size() > compactSize()`.
  * New Maps
    * `CompactCILinkedMap` exists. This `CompactMap` expands to a case-insensitive `LinkedHashMap` when `size() > compactSize()` entries.
    * `CompactCIHashMap` exists.  This `CompactMap` expands to a case-insensitive `HashMap` when `size() > compactSize()` entries.      
    * `CompactLinkedMap` added.  This `CompactMap` expands to a `LinkedHashMap` when `size() > compactSize()` entries.
    * `CompactMap` exists.  This `CompactMap` expands to a `HashMap` when `size() > compactSize()` entries.
* 1.50.0
  * `CompactCIHashMap` added.  This is a `CompactMap` that is case insensitive.  When more than `compactSize()` entries are stored in it (default 80), it uses a `CaseInsenstiveMap` `HashMap` to hold its entries. 
  * `CompactCILinkedMap` added.  This is a `CompactMap` that is case insensitive.  When more than `compactSize()` entries are stored in it (default 80), it uses a `CaseInsenstiveMap` `LinkedHashMap` to hold its entries.
  * Bug fix: `CompactMap` `entrySet()` and `keySet()` were not handling the `retainAll()`, `containsAll()`, and `removeAll()` methods case-insensitively when case-insensitivity was activated.
  * `Converter` methods that convert to byte, short, int, and long now accepted String decimal numbers.  The decimal portion is truncated.
* 1.49.0
  * Added `CompactSet`.  Works similarly to `CompactMap` with single `Object[]` holding elements until it crosses `compactSize()` threshold.  
  This `Object[]` is adjusted dynamically as objects are added and removed.  
* 1.48.0
  * Added `char` and `Character` support to `Convert.convert*()`
  * Added full Javadoc to `Converter`.
  * Performance improvement in `Iterator.remove()` for all of `CompactMap's` iterators: `keySet().iterator()`, `entrySet().iterator`, and `values().iterator`.
  * In order to get to 100% code coverage with Jacoco, added more tests for `Converter`, `CaseInsenstiveMap`, and `CompactMap`.
* 1.47.0
  * `Converter.convert2*()` methods added: If `null` passed in, primitive 'logical zero' is returned. Example: `Converter.convert(null, boolean.class)` returns `false`.
  * `Converter.convertTo*()` methods: if `null` passed in, `null` is returned.  Allows "tri-state" Boolean. Example: `Converter.convert(null, Boolean.class)` returns `null`.
  * `Converter.convert()` converts using `convertTo*()` methods for primitive wrappers, and `convert2*()` methods for primitive classes.
  * `Converter.setNullMode()` removed.
* 1.46.0
  * `CompactMap` now supports 4 stages of "growth", making it much smaller in memory than nearly any `Map`.  After `0` and `1` entries,
  and between `2` and `compactSize()` entries, the entries in the `Map` are stored in an `Object[]` (using same single member variable).  The
  even elements the 'keys' and the odd elements are the associated 'values'.  This array is dynamically resized to exactly match the number of stored entries.
  When more than `compactSize()` entries are used, the `Map` then uses the `Map` returned from the overrideable `getNewMap()` api to store the entries.
  In all cases, it maintains the underlying behavior of the `Map`. 
  * Updated to consume `log4j 2.13.1`
* 1.45.0
  * `CompactMap` now supports case-insensitivity when using String keys.  By default, it is case sensitive, but you can override the 
  `isCaseSensitive()` method and return `false`.  This allows you to return `TreeMap(String.CASE_INSENSITIVE_ORDER)` or `CaseInsensitiveMap`
  from the `getNewMap()` method.  With these overrides, CompactMap is now case insensitive, yet still 'compact.'
  * `Converter.setNullMode(Converter.NULL_PROPER | Converter.NULL_NULL)` added to allow control over how `null` values are converted.
  By default, passing a `null` value into primitive `convert*()` methods returns the primitive form of `0` or `false`.  
  If the static method `Converter.setNullMode(Converter.NULL_NULL)` is called it will change the behavior of the primitive
  `convert*()` methods return `null`.    
* 1.44.0
  * `CompactMap` introduced.  
  `CompactMap` is a `Map` that strives to reduce memory at all costs while retaining speed that is close to `HashMap's` speed.
  It does this by using only one (1) member variable (of type `Object`) and changing it as the `Map` grows.  It goes from
  single value, to a single `Map Entry`, to an `Object[]`, and finally it uses a `Map` (user defined).  `CompactMap` is
  especially small when `0` or `1` entries are stored in it.  When `size()` is from `2` to `compactSize()`, then entries
  are stored internally in single `Object[]`.  If the `size() > compactSize()` then the entries are stored in a
  regular `Map`.
  ``` 
     // If this key is used and only 1 element then only the value is stored
     protected K getSingleValueKey() { return "someKey"; }
  
     // Map you would like it to use when size() > compactSize().  HashMap is default
     protected abstract Map<K, V> getNewMap();
  
     // If you want case insensitivity, return true and return new CaseInsensitiveMap or TreeMap(String.CASE_INSENSITIVE_PRDER) from getNewMap()
     protected boolean isCaseInsensitive() { return false; }        // 1.45.0
  
     // When size() > than this amount, the Map returned from getNewMap() is used to store elements.
     protected int compactSize() { return 100; }                    // 1.46.0
  ```     
    ##### **Empty**
    This class only has one (1) member variable of type `Object`.  If there are no entries in it, then the value of that 
    member variable takes on a pointer (points to sentinel value.)
    ##### **One entry**
    If the entry has a key that matches the value returned from `getSingleValueKey()` then there is no key stored
    and the internal single member points to the value (still retried with 100% proper Map semantics).
    
    If the single entry's key does not match the value returned from `getSingleValueKey()` then the internal field points
    to an internal `Class` `CompactMapEntry` which contains the key and the value (nothing else).  Again, all APIs still operate
    the same.
   ##### **2 thru compactSize() entries**
   In this case, the single member variable points to a single Object[] that contains all the keys and values.  The
   keys are in the even positions, the values are in the odd positions (1 up from the key).  [0] = key, [1] = value,
   [2] = next key, [3] = next value, and so on.  The Object[] is dynamically expanded until size() > compactSize(). In
   addition, it is dynamically shrunk until the size becomes 1, and then it switches to a single Map Entry or a single
   value.
 
   ##### **size() > compactSize()**
   In this case, the single member variable points to a `Map` instance (supplied by `getNewMap()` API that user supplied.)
   This allows `CompactMap` to work with nearly all `Map` types.
   This Map supports null for the key and values, as long as the Map returned by getNewMap() supports null keys-values.       
* 1.43.0
  * `CaseInsensitiveMap(Map orig, Map backing)` added for allowing precise control of what `Map` instance is used to back the `CaseInsensitiveMap`.  For example,
  ```
    Map originalMap = someMap  // has content already in it
    Map ciMap1 = new CaseInsensitiveMap(someMap, new TreeMap())  // Control Map type, but not initial capacity
    Map ciMap2 = new CaseInsensitiveMap(someMap, new HashMap(someMap.size()))    // Control both Map type and initial capacity
    Map ciMap3 = new CaseInsensitiveMap(someMap, new Object2ObjectOpenHashMap(someMap.size()))   // Control initial capacity and use specialized Map from fast-util.  
  ```
  * `CaseInsensitiveMap.CaseInsensitiveString()` constructor made `public`. 
* 1.42.0
  * `CaseInsensitiveMap.putObject(Object key, Object value)` added for placing objects into typed Maps.
* 1.41.0
  * `CaseInsensitiveMap.plus()` and `.minus()` added to support `+` and `-` operators in languages like Groovy.
  * `CaseInsenstiveMap.CaseInsensitiveString` (`static` inner Class) is now `public`.  
* 1.40.0
  * Added `ReflectionUtils.getNonOverloadedMethod()` to support reflectively fetching methods with only Class and Method name available.  This implies there is no method overloading.
* 1.39.0
  * Added `ReflectionUtils.call(bean, methodName, args...)` to allow one-step reflective calls.  See Javadoc for any limitations.
  * Added `ReflectionUtils.call(bean, method, args...)` to allow easy reflective calls.  This version requires obtaining the `Method` instance first.  This approach allows methods with the same name and number of arguments (overloaded) to be called.
  * All `ReflectionUtils.getMethod()` APIs cache reflectively located methods to significantly improve performance when using reflection.
  * The `call()` methods throw the target of the checked `InvocationTargetException`.  The checked `IllegalAccessException` is rethrown wrapped in a RuntimeException.  This allows making reflective calls without having to handle these two checked exceptions directly at the call point. Instead, these exceptions are usually better handled at a high-level in the code.   
* 1.38.0
  * Enhancement: `UniqueIdGenerator` now generates the long ids in monotonically increasing order.  @HonorKnight
  * Enhancement: New API [`getDate(uniqueId)`] added to `UniqueIdGenerator` that when passed an ID that it generated, will return the time down to the millisecond when it was generated.
* 1.37.0
  * `TestUtil.assertContainsIgnoreCase()` and `TestUtil.checkContainsIgnoreCase()` APIs added.  These are generally used in unit tests to check error messages for key words, in order (as opposed to doing `.contains()` on a string which allows the terms to appear in any order.)
  * Build targets classes in Java 1.7 format, for maximum usability.  The version supported will slowly move up, but only based on necessity allowing for widest use of java-util in as many projects as possible.
* 1.36.0
  * `Converter.convert()` now bi-directionally supports `Calendar.class`, e.g. Calendar to Date, SqlDate, Timestamp, String, long, BigDecimal, BigInteger, AtomicLong, and vice-versa.  
  * `UniqueIdGenerator.getUniqueId19()` is a new API for getting 19 digit unique IDs (a full `long` value)  These are generated at a faster rate (10,000 per millisecond vs. 1,000 per millisecond) than the original (18-digit) API.
  * Hardcore test added for ensuring concurrency correctness with `UniqueIdGenerator`.
  * Javadoc beefed up for `UniqueIdGenerator`.
  * Updated public APIs to have proper support for generic arguments.  For example Class&lt;T&gt;, Map&lt;?, ?&gt;, and so on.  This eliminates type casting on the caller's side.
  * `ExceptionUtilities.getDeepestException()` added.  This API locates the source (deepest) exception.  
* 1.35.0
  * `DeepEquals.deepEquals()`, when comparing `Maps`, the `Map.Entry` type holding the `Map's` entries is no longer considered in equality testing. In the past, a custom Map.Entry instance holding the key and value could cause inquality, which should be ignored.  @AndreyNudko
  * `Converter.convert()` now uses parameterized types so that the return type matches the passed in `Class` parameter.  This eliminates the need to cast the return value of `Converter.convert()`.
  * `MapUtilities.getOrThrow()` added which throws the passed in `Throwable` when the passed in key is not within the `Map`. @ptjuanramos
* 1.34.2
  * Performance Improvement: `CaseInsensitiveMap`, when created from another `CaseInsensitiveMap`, re-uses the internal `CaseInsensitiveString` keys, which are immutable.
  * Bug fix: `Converter.convertToDate(), Converter.convertToSqlDate(), and Converter.convertToTimestamp()` all threw a `NullPointerException` if the passed in content was an empty String (of 0 or more spaces). When passed in NULL to these APIs, you get back null.  If you passed in empty strings or bad date formats, an IllegalArgumentException is thrown with a message clearly indicating what input failed and why.
* 1.34.0
  * Enhancement: `DeepEquals.deepEquals(a, b options)` added.  The new options map supports a key `DeepEquals.IGNORE_CUSTOM_EQUALS` which can be set to a Set of String class names.  If any of the encountered classes in the comparison are listed in the Set, and the class has a custom `.equals()` method, it will not be called and instead a `deepEquals()` will be performed.  If the value associated to the `IGNORE_CUSTOM_EQUALS` key is an empty Set, then no custom `.equals()` methods will be called, except those on primitives, primitive wrappers, `Date`, `Class`, and `String`. 
* 1.33.0
  * Bug fix: `DeepEquals.deepEquals(a, b)` could report equivalent unordered `Collections` / `Maps` as not equal if the items in the `Collection` / `Map` had the same hash code.
* 1.32.0
  * `Converter` updated to expose `convertTo*()` APIs that allow converting to a known type.
* 1.31.1
  * Renamed `AdjustableFastGZIPOutputStream` to `AdjustableGZIPOutputStream`.
* 1.31.0
  * Add `AdjustableFastGZIPOutputStream` so that compression level can be adjusted.
* 1.30.0
  * `ByteArrayOutputStreams` converted to `FastByteArrayOutputStreams` internally.
* 1.29.0
  * Removed test dependencies on Guava
  * Rounded out APIs on `FastByteArrayOutputStream`
  * Added APIs to `IOUtilities`.
* 1.28.2
  * Enhancement: `IOUtilities.compressBytes(FastByteArrayOutputStream, FastByteArrayOutputStream)` added.
* 1.28.1
  * Enhancement: `FastByteArrayOutputStream.getBuffer()` API made public.
* 1.28.0
  * Enhancement: `FastByteArrayOutputStream` added.  Similar to JDK class, but without `synchronized` and access to inner `byte[]` allowed without duplicating the `byte[]`.
* 1.27.0
  * Enhancement: `Converter.convert()` now supports `enum` to `String`
* 1.26.1
  * Bug fix: The internal class `CaseInsensitiveString` did not implement `Comparable` interface correctly.
* 1.26.0
  * Enhancement: added `getClassNameFromByteCode()` API to `ReflectionUtils`.
* 1.25.1
  * Enhancement: The Delta object returned by `GraphComparator` implements `Serializable` for those using `ObjectInputStream` / `ObjectOutputStream`.  Provided by @metlaivan (Ivan Metla) 
* 1.25.0
  * Performance improvement: `CaseInsensitiveMap/Set` internally adds `Strings` to `Map` without using `.toLowerCase()` which eliminates creating a temporary copy on the heap of the `String` being added, just to get its lowerCaseValue.
  * Performance improvement: `CaseInsensitiveMap/Set` uses less memory internally by caching the hash code as an `int`, instead of an `Integer`.
  * `StringUtilities.caseInsensitiveHashCode()` API added.  This allows computing a case-insensitive hashcode from a `String` without any object creation (heap usage).
* 1.24.0
  * `Converter.convert()` - performance improved using class instance comparison versus class `String` name comparison.
  * `CaseInsensitiveMap/Set` - performance improved.  `CaseInsensitiveString` (internal) short-circuits on equality check if hashCode() [cheap runtime cost] is not the same.  Also, all method returning true/false to detect if `Set` or `Map` changed rely on size() instead of contains.
* 1.23.0
  * `Converter.convert()` API update: When a mutable type (`Date`, `AtomicInteger`, `AtomicLong`, `AtomicBoolean`) is passed in, and the destination type is the same, rather than return the instance passed in, a copy of the instance is returned.
* 1.22.0
  * Added `GraphComparator` which is used to compute the difference (delta) between two object graphs.  The generated `List` of Delta objects can be 'played' against the source to bring it up to match the target.  Very useful in transaction processing systems.
* 1.21.0
  * Added `Executor` which is used to execute Operating System commands.  For example, `Executor exector = new Executor(); executor.exec("echo This is handy");  assertEquals("This is handy", executor.getOut().trim());`
  * bug fix: `CaseInsensitiveMap`, when passed a `LinkedHashMap`, was inadvertently using a HashMap instead.
* 1.20.5
  * `CaseInsensitiveMap` intentionally does not retain 'not modifiability'.
  * `CaseInsensitiveSet` intentionally does not retain 'not modifiability'. 
* 1.20.4
  * Failed release.  Do not use.
* 1.20.3
  * `TrackingMap` changed so that `get(anyKey)` always marks it as keyRead.  Same for `containsKey(anyKey)`.
  * `CaseInsensitiveMap` has a constructor that takes a `Map`, which allows it to take on the nature of the `Map`, allowing for case-insensitive `ConcurrentHashMap`, sorted `CaseInsensitiveMap`, etc.  The 'Unmodifiable' `Map` nature is intentionally not taken on.  The passed in `Map` is not mutated.
  * `CaseInsensitiveSet` has a constructor that takes a `Collection`, nwhich allows it to take on the nature of the `Collection`, allowing for sorted `CaseInsensitiveSets`.  The 'unmodifiable' `Collection` nature is intentionally not taken on.  The passed in `Set` is not mutated.  
* 1.20.2
  * `TrackingMap` changed so that an existing key associated to null counts as accessed. It is valid for many `Map` types to allow null values to be associated to the key.
  * `TrackingMap.getWrappedMap()` added so that you can fetch the wrapped `Map`.
* 1.20.1
  * `TrackingMap` changed so that `.put()` does not mark the key as accessed.
* 1.20.0
  * `TrackingMap` added.  Create this map around any type of Map, and it will track which keys are accessed via .get(), .containsKey(), or .put() (when put overwrites a value already associated to the key).  Provided by @seankellner.
* 1.19.3
  * Bug fix: `CaseInsensitiveMap.entrySet()` - calling `entry.setValue(k, v)` while iterating the entry set, was not updating the underlying value.  This has been fixed and test case added.
* 1.19.2
  * The order in which system properties are read versus environment variables via the `SystemUtilities.getExternalVariable()` method has changed.  System properties are checked first, then environment variables.
* 1.19.1
  * Fixed issue in `DeepEquals.deepEquals()` where a Container type (`Map` or `Collection`) was being compared to a non-container - the result of this comparison was inconsistent.   It is always false if a Container is compared to a non-container type (anywhere within the object graph), regardless of the comparison order A, B versus comparing B, A.
* 1.19.0
  * `StringUtilities.createUtf8String(byte[])` API added which is used to easily create UTF-8 strings without exception handling code.
  * `StringUtilities.getUtf8Bytes(String s)` API added which returns a byte[] of UTF-8 bytes from the passed in Java String without any exception handling code required.
  * `ByteUtilities.isGzipped(bytes[])` API added which returns true if the `byte[]` represents gzipped data.
  * `IOUtilities.compressBytes(byte[])` API added which returns the gzipped version of the passed in `byte[]` as a `byte[]`
  * `IOUtilities.uncompressBytes(byte[])` API added which returns the original byte[] from the passed in gzipped `byte[]`.
  * JavaDoc issues correct to support Java 1.8 stricter JavaDoc compilation.
* 1.18.1
  * `UrlUtilities` now allows for per-thread `userAgent` and `referrer` as well as maintains backward compatibility for setting these values globally.
  * `StringUtilities` `getBytes()` and `createString()` now allow null as input, and return null for output for null input.
  * Javadoc updated to remove errors flagged by more stringent Javadoc 1.8 generator.
* 1.18.0
  * Support added for `Timestamp` in `Converter.convert()`
  * `null` can be passed into `Converter.convert()` for primitive types, and it will return their logical 0 value (0.0f, 0.0d, etc.).  For primitive wrappers, atomics, etc, null will be returned.
  * "" can be passed into `Converter.convert()` and it will set primitives to 0, and the object types (primitive wrappers, dates, atomics) to null.  `String` will be set to "".
* 1.17.1
  * Added full support for `AtomicBoolean`, `AtomicInteger`, and `AtomicLong` to `Converter.convert(value, AtomicXXX)`.  Any reasonable value can be converted to/from these, including Strings, Dates (`AtomicLong`), all `Number` types.
  * `IOUtilities.flush()` now supports `XMLStreamWriter`
* 1.17.0
  * `UIUtilities.close()` now supports `XMLStreamReader` and `XMLStreamWriter` in addition to `Closeable`.
  * `Converter.convert(value, type)` - a value of null is supported for the numeric types, boolean, and the atomics - in which case it returns their "zero" value and false for boolean.  For date and String return values, a null input will return null.  The `type` parameter must not be null.
* 1.16.1
  * In `Converter.convert(value, type)`, the value is trimmed of leading / trailing white-space if it is a String and the type is a `Number`.
* 1.16.0
  * Added `Converter.convert()` API.  Allows converting instances of one type to another.  Handles all primitives, primitive wrappers, `Date`, `java.sql.Date`, `String`, `BigDecimal`,  `BigInteger`, `AtomicInteger`, `AtomicLong`, and `AtomicBoolean`.  Additionally, input (from) argument accepts `Calendar`.
  * Added static `getDateFormat()` to `SafeSimpleDateFormat` for quick access to thread local formatter (per format `String`).
* 1.15.0
  * Switched to use Log4J2 () for logging.
* 1.14.1
  * bug fix: `CaseInsensitiveMap.keySet()` was only initializing the iterator once.  If `keySet()` was called a 2nd time, it would no longer work.
* 1.14.0
  * bug fix: `CaseInsensitiveSet()`, the return value for `addAll()`, `returnAll()`, and `retainAll()` was wrong in some cases.
* 1.13.3
  * `EncryptionUtilities` - Added byte[] APIs.  Makes it easy to encrypt/decrypt `byte[]` data.
  * `pom.xml` had extraneous characters inadvertently added to the file - these are removed.
  * 1.13.1 & 13.12 - issues with sonatype
* 1.13.0
  * `DateUtilities` - Day of week allowed (properly ignored).
  * `DateUtilities` - First (st), second (nd), third (rd), and fourth (th) ... supported.
  * `DateUtilities` - The default toString() standard date / time displayed by the JVM is now supported as a parseable format.
  * `DateUtilities` - Extra whitespace can exist within the date string.
  * `DateUtilities` - Full time zone support added.
  * `DateUtilities` - The date (or date time) is expected to be in isolation. Whitespace on either end is fine, however, once the date time is parsed from the string, no other content can be left (prevents accidently parsing dates from dates embedded in text).
  * `UrlUtilities` - Removed proxy from calls to `URLUtilities`.  These are now done through the JVM.
* 1.12.0
  * `UniqueIdGenerator` uses 99 as the cluster id when the JAVA_UTIL_CLUSTERID environment variable or System property is not available.  This speeds up execution on developer's environments when they do not specify `JAVA_UTIL_CLUSTERID`.
  * All the 1.11.x features rolled up.
* 1.11.3
  * `UrlUtilities` - separated out call that resolves `res://` to a public API to allow for wider use.
* 1.11.2
  * Updated so headers can be set individually by the strategy (`UrlInvocationHandler`)
  * `InvocationHandler` set to always uses `POST` method to allow additional `HTTP` headers.
* 1.11.1
  * Better IPv6 support (`UniqueIdGenerator`)
  * Fixed `UrlUtilities.getContentFromUrl()` (`byte[]`) no longer setting up `SSLFactory` when `HTTP` protocol used.
* 1.11.0
  * `UrlInvocationHandler`, `UrlInvocationStrategy` - Updated to allow more generalized usage. Pass in your implementation of `UrlInvocationStrategy` which allows you to set the number of retry attempts, fill out the URL pattern, set up the POST data, and optionally set/get cookies.
  * Removed dependency on json-io.  Only remaining dependency is Apache commons-logging.
* 1.10.0
  * Issue #3 fixed: `DeepEquals.deepEquals()` allows similar `Map` (or `Collection`) types to be compared without returning 'not equals' (false).  Example, `HashMap` and `LinkedHashMap` are compared on contents only.  However, compare a `SortedSet` (like `TreeMap`) to `HashMap` would fail unless the Map keys are in the same iterative order.
  * Tests added for `UrlUtilities`
  * Tests added for `Traverser`
* 1.9.2
  * Added wildcard to regex pattern to `StringUtilities`.  This API turns a DOS-like wildcard pattern (where  * matches anything and ? matches a single character) into a regex pattern useful in `String.matches()` API.
* 1.9.1
  * Floating-point allow difference by epsilon value (currently hard-coded on `DeepEquals`.  Will likely be optional parameter in future version).
* 1.9.0
  * `MathUtilities` added.  Currently, variable length `minimum(arg0, arg1, ... argn)` and `maximum()` functions added.  Available for `long`, `double`, `BigInteger`, and `BigDecimal`.   These cover the smaller types.
  * `CaseInsensitiveMap` and `CaseInsensitiveSet` `keySet()` and `entrySet()` are faster as they do not make a copy of the entries.  Internally, `CaseInsensitiveString` caches it's hash, speeding up repeated access.
  * `StringUtilities levenshtein()` and `damerauLevenshtein()` added to compute edit length.  See Wikipedia to understand of the difference between these two algorithms.  Currently recommend using `levenshtein()` as it uses less memory.
  * The Set returned from the `CaseInsensitiveMap.entrySet()` now contains mutable entry's (value-side). It had been using an immutable entry, which disallowed modification of the value-side during entry walk.
* 1.8.4
  * `UrlUtilities`, fixed issue where the default settings for the connection were changed, not the settings on the actual connection.
* 1.8.3
  * `ReflectionUtilities` has new `getClassAnnotation(classToCheck, annotation)` API which will return the annotation if it exists within the classes super class hierarchy or interface hierarchy.  Similarly, the `getMethodAnnotation()` API does the same thing for method annotations (allow inheritance - class or interface).
* 1.8.2
  * `CaseInsensitiveMap` methods `keySet()` and `entrySet()` return Sets that are identical to how the JDK returns 'view' Sets on the underlying storage.  This means that all operations, besides `add()` and `addAll()`, are supported.
  * `CaseInsensitiveMap.keySet()` returns a `Set` that is case insensitive (not a `CaseInsensitiveSet`, just a `Set` that ignores case).  Iterating this `Set` properly returns each originally stored item.
* 1.8.1
  * Fixed `CaseInsensitiveMap() removeAll()` was not removing when accessed via `keySet()`
* 1.8.0
  * Added `DateUtilities`.  See description above.
* 1.7.4
  * Added "res" protocol (resource) to `UrlUtilities` to allow files from classpath to easily be loaded.  Useful for testing.
* 1.7.2
  * `UrlUtilities.getContentFromUrl() / getContentFromUrlAsString()` - removed hard-coded proxy server name
* 1.7.1
  * `UrlUtilities.getContentFromUrl() / getContentFromUrlAsString()` - allow content to be fetched as `String` or binary (`byte[]`).
* 1.7.0
  * `SystemUtilities` added.  New API to fetch value from environment or System property
  * `UniqueIdGenerator` - checks for environment variable (or System property) JAVA_UTIL_CLUSTERID (0-99).  Will use this if set, otherwise last IP octet mod 100.
* 1.6.1
  * Added: `UrlUtilities.getContentFromUrl()`
* 1.6.0
  * Added `CaseInsensitiveSet`.
* 1.5.0
  * Fixed: `CaseInsensitiveMap's iterator.remove()` method, it did not remove items.
  * Fixed: `CaseInsensitiveMap's equals()` method, it required case to match on keys.
* 1.4.0
  * Initial version
