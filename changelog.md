### Revision History
* 1.34.1
  * Enhancement: Added support for `null` arguments in `Converter` shortcut methods (e.g.`Converter.convertToLong()`).
  * Enhancement: Updated `Converter.convertToLong()` to return `0L` for any `String` that `.trims()` to an empty `String`.
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
  * `TrackingMap` changed so that an existing key associated to null counts as accessed. It is valid for many Map types to allow null values to be associated to the key.
  * `TrackingMap.getWrappedMap()` added so that you can fetch the wrapped Map.
* 1.20.1
  * TrackingMap changed so that .put() does not mark the key as accessed.
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
  * `Converter.convert(value, type)` - a value of null is supported, and returns null.  A null type, however, throws an `IllegalArgumentException`.
* 1.16.1
  * In `Converter.convert(value, type)`, the value is trimmed of leading / trailing white-space if it is a String and the type is a `Number`.
* 1.16.0
  * Added `Converter.convert()` API.  Allows converting instances of one type to another.  Handles all primitives, primitive wrappers, `Date`, `java.sql.Date`, `String`, `BigDecimal`, and `BigInteger`.  Additionally, input (from) argument accepts `Calendar`.
  * Added static `getDateFormat()` to `SafeSimpleDateFormat` for quick access to thread local formatter (per format `String`).
* 1.15.0
  * Switched to use Log4J2 () for logging.
* 1.14.1
  * bug fix: `CaseInsensitiveMa.keySet()` was only initializing the iterator once.  If `keySet()` was called a 2nd time, it would no longer work.
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
  * `StringUtilities levenshtein()` and `damerauLevenshtein()` added to compute edit length.  See Wikipedia for understand of the difference.  Currently recommend using `levenshtein()` as it uses less memory.
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
