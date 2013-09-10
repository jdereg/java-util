java-util
=========
Rarely available and hard-to-write Java utilities, written correctly, and thoroughly tested

To include in your project:
```
<dependency>
  <groupId>com.cedarsoftware</groupId>
  <artifactId>java-util</artifactId>
  <version>1.2.0</version>
</dependency>
```

Also, check out json-io at https://github.com/jdereg/json-io

Including in java-util:
* ArrayUtilities - Useful utilities for working with Java's arrays [ ]
* CaseInsensitiveMap - When Strings are used as keys, they are compared without case. Can be used as regular Map with any JAva object as keys, just specially handles Strings.
* DeepEquals - Compare two object graphs and return 'true' if they are equals, 'false' otherwise.  This will handle cycles in the graph, and will call an equals() method on an object if it has one, otherwise it will do a field-by-field equivalency check for non-transient fields.
* EncryptionUtilities - Makes it easy to compute MD5 checksums for Strings, byte[], as well as making it easy to AES-128 encrypt Strings and byte[]'s.
* IOUtilities - Handy methods for simplifying I/O including such niceties as properly setting up the input stream for HttpUrlConnections based on their specified encoding.  Single line .close() method that handles exceptions for you.
* ReflectionUtils - Simple one-liners for many common reflection tasks.
* SafeSimpleDateFormat - Instances of this class can be stored as member variables and resued, without any worry about thread safety.  Fixing the problems with the JDK's SimpleDateFormat and thread safety (no reentrancy support).
* StringUtilities - Helpful methods that make simple work of common String related tasks.
* UniqueIdGenerator - Generates a Java long unique id, that is unique across server in a cluster, never hands out the same value, has massive entropy, and runs very quickly.
* UrlUtitilies - Fetch cookies from headers, getUrlConnections(), HTTP Response error handler, and more.
* UrlInvocationHandler, SessionAwareInvocationHandler, CookieAwareInvocationHandler - Use to easily communicate with RESTful JSON servers, especially ones that implement a Java interface that you have access to.
