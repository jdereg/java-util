java-util
=========
Rarely available and hard-to-write Java utilities, written correctly, and thoroughly tested

To include in your project:
```
<dependency>
  <groupId>com.cedarsoftware</groupId>
  <artifactId>java-util</artifactId>
  <version>1.7.2</version>
</dependency>

<dependency>
  <groupId>com.cedarsoftware</groupId>
  <artifactId>json-io</artifactId>
  <version>2.4.1</version>
</dependency>
```

Also, check out json-io at https://github.com/jdereg/json-io

Including in java-util:
* **ArrayUtilities** - Useful utilities for working with Java's arrays [ ]
* **CaseInsensitiveMap** - When Strings are used as keys, they are compared without case. Can be used as regular Map with any Java object as keys, just specially handles Strings.
* **CaseInsensitiveSet** - Set implementation that ignores String case for contains() calls, yet can have any object added to it (does not limit you to adding only Strings to it).
* **DeepEquals** - Compare two object graphs and return 'true' if they are equivalent, 'false' otherwise.  This will handle cycles in the graph, and will call an equals() method on an object if it has one, otherwise it will do a field-by-field equivalency check for non-transient fields.
* **EncryptionUtilities** - Makes it easy to compute MD5 checksums for Strings, byte[], as well as making it easy to AES-128 encrypt Strings and byte[]'s.
* **IOUtilities** - Handy methods for simplifying I/O including such niceties as properly setting up the input stream for HttpUrlConnections based on their specified encoding.  Single line .close() method that handles exceptions for you.
* **ReflectionUtils** - Simple one-liners for many common reflection tasks.
* **SafeSimpleDateFormat** - Instances of this class can be stored as member variables and resued, without any worry about thread safety.  Fixing the problems with the JDK's SimpleDateFormat and thread safety (no reentrancy support).
* **StringUtilities** - Helpful methods that make simple work of common String related tasks.
* **SystemUtilities** - A Helpful utility methods for working with external entities like the OS, environment variables, and system properties.
* **Traverser** - Pass any Java object to this Utility class, it will call your passed in anonymous method for each object it encounters while traversing the complete graph.  It handles cycles within the graph. Permits you to perform generalized actions on all objects within an object graph.
* **UniqueIdGenerator** - Generates a Java long unique id, that is unique across server in a cluster, never hands out the same value, has massive entropy, and runs very quickly.
* **UrlUtitilies** - Fetch cookies from headers, getUrlConnections(), HTTP Response error handler, and more.
* **UrlInvocationHandler**, **SessionAwareInvocationHandler**, **CookieAwareInvocationHandler** - Use to easily communicate with RESTful JSON servers, especially ones that implement a Java interface that you have access to.

Version History
* 1.7.2
 * UrlUtilities.getContentFromUrl / getContentFromUrlAsString - removed hard-coded proxy server name
* 1.7.1
 * UrlUtilities.getContentFromUrl / getContentFromUrlAsString - allow content to be fetched as String or binary (byte[]).
* 1.7.0
 * SystemUtilities added.  New API to fetch value from environment or System property
 * UniqueIdGenerator - checks for environment variable (or System property) JAVA_UTIL_CLUSTERID (0-99).  Will use this if set, otherwise last IP octet mod 100.
* 1.6.1
 * Added: UrlUtilities.getContentFromUrl();
* 1.6.0
 * Added CaseInsensitiveSet.
* 1.5.0
 * Fixed: CaseInsensitiveMap's iterator.remove() method, it did not remove items.  
 * Fixed: CaseInsensitiveMap's equals() method, it required case to match on keys.
* 1.4.0
 * Initial version

By: John DeRegnaucourt and Ken Partlow
