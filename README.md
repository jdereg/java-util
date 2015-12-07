java-util
=========
Rarely available and hard-to-write Java utilities, written correctly, and thoroughly tested (> 98% code coverage via JUnit tests).

To include in your project:
```
<dependency>
  <groupId>com.cedarsoftware</groupId>
  <artifactId>java-util</artifactId>
  <version>1.19.3</version>
</dependency>
```
Like **java-util** and find it useful? **Tip** bitcoin: 1MeozsfDpUALpnu3DntHWXxoPJXvSAXmQA

Also, check out json-io at https://github.com/jdereg/json-io

Including in java-util:
* **ArrayUtilities** - Useful utilities for working with Java's arrays [ ]
* **ByteUtilities** - Useful routines for converting byte[] to HEX character [] and visa-versa.
* **CaseInsensitiveMap** - When Strings are used as keys, they are compared without case. Can be used as regular Map with any Java object as keys, just specially handles Strings.
* **CaseInsensitiveSet** - Set implementation that ignores String case for contains() calls, yet can have any object added to it (does not limit you to adding only Strings to it).
* **Converter** - Convert from once instance to another.  For example, convert("45.3", BigDecimal.class) will convert the String to a BigDecimal.  Works for all primitives, primitive wrappers, Date, java.sql.Date, String, BigDecimal, and BigInteger.  The method is very generous on what it allows to be converted.  For example, a Calendar instance can be input for a Date or Long.  Examine source to see all possibilities.
* **DateUtilities** - Robust date String parser that handles date/time, date, time, time/date, string name months or numeric months, skips comma, etc. English month names only (plus common month name abbreviations), time with/without seconds or milliseconds, y/m/d and m/d/y ordering as well.
* **DeepEquals** - Compare two object graphs and return 'true' if they are equivalent, 'false' otherwise.  This will handle cycles in the graph, and will call an equals() method on an object if it has one, otherwise it will do a field-by-field equivalency check for non-transient fields.
* **EncryptionUtilities** - Makes it easy to compute MD5 checksums for Strings, byte[], as well as making it easy to AES-128 encrypt Strings and byte[]'s.
* **IOUtilities** - Handy methods for simplifying I/O including such niceties as properly setting up the input stream for HttpUrlConnections based on their specified encoding.  Single line .close() method that handles exceptions for you.
* **MathUtilities** - Handy mathematical algorithms to make your code smaller.  For example, minimum of array of values.
* **ReflectionUtils** - Simple one-liners for many common reflection tasks.
* **SafeSimpleDateFormat** - Instances of this class can be stored as member variables and reused without any worry about thread safety.  Fixing the problems with the JDK's SimpleDateFormat and thread safety (no reentrancy support).
* **StringUtilities** - Helpful methods that make simple work of common String related tasks.
* **SystemUtilities** - A Helpful utility methods for working with external entities like the OS, environment variables, and system properties.
* **Traverser** - Pass any Java object to this Utility class, it will call your passed in anonymous method for each object it encounters while traversing the complete graph.  It handles cycles within the graph. Permits you to perform generalized actions on all objects within an object graph.
* **UniqueIdGenerator** - Generates a Java long unique id, that is unique across server in a cluster, never hands out the same value, has massive entropy, and runs very quickly.
* **UrlUtitilies** - Fetch cookies from headers, getUrlConnections(), HTTP Response error handler, and more.
* **UrlInvocationHandler** - Use to easily communicate with RESTful JSON servers, especially ones that implement a Java interface that you have access to.

### Sponsors
[![Alt text](https://www.yourkit.com/images/yklogo.png "YourKit")](https://www.yourkit.com/.net/profiler/index.jsp)

YourKit supports open source projects with its full-featured Java Profiler.
YourKit, LLC is the creator of <a href="https://www.yourkit.com/java/profiler/index.jsp">YourKit Java Profiler</a>
and <a href="https://www.yourkit.com/.net/profiler/index.jsp">YourKit .NET Profiler</a>,
innovative and intelligent tools for profiling Java and .NET applications.

[![Alt text](https://encrypted-tbn2.gstatic.com/images?q=tbn:ANd9GcS-ZOCfy4ezfTmbGat9NYuyfe-aMwbo3Czx3-kUfKreRKche2f8fg "IntellijIDEA")](https://www.jetbrains.com/idea/)

Version History
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
 * Added wildcard to regex pattern to `StringUtilities`.  This API turns a DOS-like wildcard pattern (where * matches anything and ? matches a single character) into a regex pattern useful in `String.matches()` API.
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

By: John DeRegnaucourt and Ken Partlow
