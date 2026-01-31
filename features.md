# Feature Options

Modern enterprise applications demand libraries that adapt to diverse security requirements, performance constraints, and operational environments. Following the architectural principles embraced by industry leaders like Google (with their extensive use of feature flags), Netflix (with their chaos engineering configurations), Amazon (with their service-specific tuning), and Meta (with their A/B testing infrastructure), java-util embraces a **flexible feature options approach** that puts control directly in the hands of developers and operations teams.

This approach aligns with current best practices in cloud-native development, including GitOps configurations, service mesh policies, and progressive delivery patterns that define the cutting edge of modern software architecture.

Rather than forcing a one-size-fits-all configuration, java-util provides granular control over every aspect of its behavior through system properties. This approach enables:

- **Zero-downtime security hardening** - Enable security features without code changes
- **Environment-specific tuning** - Different limits for development vs. production
- **Gradual rollout strategies** - Test new security features with feature flags
- **Compliance flexibility** - Meet varying regulatory requirements across deployments
- **Performance optimization** - Fine-tune resource limits based on actual usage patterns

All security features are **disabled by default** to ensure seamless upgrades, with the flexibility to enable and configure them per environment. This design philosophy allows java-util to serve both lightweight applications and enterprise-grade systems from the same codebase.

---

## Table of Contents

- [ArrayUtilities](#arrayutilities)
- [ByteUtilities](#byteutilities)
- [DateUtilities](#dateutilities)
- [DeepEquals](#deepequals)
- [EncryptionUtilities](#encryptionutilities)
- [IOUtilities](#ioutilities)
- [MathUtilities](#mathutilities)
- [ReflectionUtils](#reflectionutils)
- [StringUtilities](#stringutilities)
- [SystemUtilities](#systemutilities)
- [Traverser](#traverser)
- [UrlUtilities](#urlutilities)
- [Converter](#converter)
- [Other](#other)

---

## Complete Property Reference

<table>
<tr style="background-color: #334155;">
<th style="color: #ffffff; font-weight: bold; padding: 8px;">Fully Qualified Property Name</th>
<th style="color: #ffffff; font-weight: bold; padding: 8px;">Allowed Values</th>
<th style="color: #ffffff; font-weight: bold; padding: 8px;">Default Value</th>
<th style="color: #ffffff; font-weight: bold; padding: 8px;">Description</th>
</tr>
<tr style="background-color: #f0f4f8;">
<td colspan="4" style="color: #334155; font-weight: bold; padding: 8px;" id="arrayutilities"><strong>ArrayUtilities</strong></td>
</tr>
<tr>
<td><code>arrayutilities.security.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Master switch for all ArrayUtilities security features</td>
</tr>
<tr>
<td><code>arrayutilities.component.type.validation.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Block dangerous system classes in array operations</td>
</tr>
<tr>
<td><code>arrayutilities.max.array.size</code></td>
<td>Integer</td>
<td><span style="color: #007acc">2147483639</span></td>
<td>Maximum array size (Integer.MAX_VALUE-8)</td>
</tr>
<tr>
<td><code>arrayutilities.dangerous.class.patterns</code></td>
<td>Comma-separated patterns</td>
<td><span style="color: #007acc; font-size: 7pt">java.lang.Runtime,<br>java.lang.ProcessBuilder,<br>java.lang.System,<br>java.security.,javax.script.,<br>sun.,com.sun.,java.lang.Class</span></td>
<td>Dangerous class patterns to block</td>
</tr>
<tr style="background-color: #f0f4f8;">
<td colspan="4" style="color: #334155; font-weight: bold; padding: 8px;" id="byteutilities"><strong>ByteUtilities</strong></td>
</tr>
<tr>
<td><code>byteutilities.security.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Master switch for all ByteUtilities security features</td>
</tr>
<tr>
<td><code>byteutilities.max.hex.string.length</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Hex string length limit for decode operations</td>
</tr>
<tr>
<td><code>byteutilities.max.array.size</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Byte array size limit for encode operations</td>
</tr>
<tr style="background-color: #f0f4f8;">
<td colspan="4" style="color: #334155; font-weight: bold; padding: 8px;" id="dateutilities"><strong>DateUtilities</strong></td>
</tr>
<tr>
<td><code>dateutilities.security.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Master switch for all DateUtilities security features</td>
</tr>
<tr>
<td><code>dateutilities.input.validation.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Enable input length and content validation</td>
</tr>
<tr>
<td><code>dateutilities.regex.timeout.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Enable regex timeout protection</td>
</tr>
<tr>
<td><code>dateutilities.malformed.string.protection.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Enable malformed input protection</td>
</tr>
<tr>
<td><code>dateutilities.max.input.length</code></td>
<td>Integer</td>
<td><span style="color: #007acc">1000</span></td>
<td>Maximum input string length</td>
</tr>
<tr>
<td><code>dateutilities.max.epoch.digits</code></td>
<td>Integer</td>
<td><span style="color: #007acc">19</span></td>
<td>Maximum digits for epoch milliseconds</td>
</tr>
<tr>
<td><code>dateutilities.regex.timeout.milliseconds</code></td>
<td>Long</td>
<td><span style="color: #007acc">1000</span></td>
<td>Timeout for regex operations in milliseconds</td>
</tr>
<tr style="background-color: #f0f4f8;">
<td colspan="4" style="color: #334155; font-weight: bold; padding: 8px;" id="deepequals"><strong>DeepEquals</strong></td>
</tr>
<tr>
<td><code>deepequals.secure.errors</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Enable error message sanitization</td>
</tr>
<tr>
<td><code>deepequals.max.collection.size</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Collection size limit</td>
</tr>
<tr>
<td><code>deepequals.max.array.size</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Array size limit</td>
</tr>
<tr>
<td><code>deepequals.max.map.size</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Map size limit</td>
</tr>
<tr>
<td><code>deepequals.max.object.fields</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Object field count limit</td>
</tr>
<tr>
<td><code>deepequals.max.recursion.depth</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Recursion depth limit</td>
</tr>
<tr>
<td colspan="4" style="padding: 8px; font-style: italic;">
<strong>Programmatic Options (via options Map):</strong><br>
• <code>ignoreCustomEquals</code> (Boolean): Ignore custom equals() methods<br>
• <code>stringsCanMatchNumbers</code> (Boolean): Allow "10" to match numeric 10<br>
• <code>deepequals.include.diff_item</code> (Boolean): Include ItemsToCompare object (for memory efficiency, default false)<br>
<strong>Output Keys:</strong><br>
• <code>diff</code> (String): Human-readable difference path<br>
• <code>diff_item</code> (ItemsToCompare): Detailed difference object (when include.diff_item=true)
</td>
</tr>
<tr style="background-color: #f0f4f8;">
<td colspan="4" style="color: #334155; font-weight: bold; padding: 8px;" id="encryptionutilities"><strong>EncryptionUtilities</strong></td>
</tr>
<tr>
<td><code>encryptionutilities.security.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Master switch for all EncryptionUtilities security features</td>
</tr>
<tr>
<td><code>encryptionutilities.file.size.validation.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Enable file size limits for hashing operations</td>
</tr>
<tr>
<td><code>encryptionutilities.buffer.size.validation.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Enable buffer size validation</td>
</tr>
<tr>
<td><code>encryptionutilities.crypto.parameters.validation.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Enable cryptographic parameter validation</td>
</tr>
<tr>
<td><code>encryptionutilities.max.file.size</code></td>
<td>Long</td>
<td><span style="color: #007acc">2147483647</span></td>
<td>Maximum file size for hashing operations (2GB)</td>
</tr>
<tr>
<td><code>encryptionutilities.max.buffer.size</code></td>
<td>Integer</td>
<td><span style="color: #007acc">1048576</span></td>
<td>Maximum buffer size (1MB)</td>
</tr>
<tr>
<td><code>encryptionutilities.min.pbkdf2.iterations</code></td>
<td>Integer</td>
<td><span style="color: #007acc">10000</span></td>
<td>Minimum PBKDF2 iterations</td>
</tr>
<tr>
<td><code>encryptionutilities.max.pbkdf2.iterations</code></td>
<td>Integer</td>
<td><span style="color: #007acc">1000000</span></td>
<td>Maximum PBKDF2 iterations</td>
</tr>
<tr>
<td><code>encryptionutilities.min.salt.size</code></td>
<td>Integer</td>
<td><span style="color: #007acc">8</span></td>
<td>Minimum salt size in bytes</td>
</tr>
<tr>
<td><code>encryptionutilities.max.salt.size</code></td>
<td>Integer</td>
<td><span style="color: #007acc">64</span></td>
<td>Maximum salt size in bytes</td>
</tr>
<tr>
<td><code>encryptionutilities.min.iv.size</code></td>
<td>Integer</td>
<td><span style="color: #007acc">8</span></td>
<td>Minimum IV size in bytes</td>
</tr>
<tr>
<td><code>encryptionutilities.max.iv.size</code></td>
<td>Integer</td>
<td><span style="color: #007acc">32</span></td>
<td>Maximum IV size in bytes</td>
</tr>
<tr style="background-color: #f0f4f8;">
<td colspan="4" style="color: #334155; font-weight: bold; padding: 8px;" id="ioutilities"><strong>IOUtilities</strong></td>
</tr>
<tr>
<td><code>io.debug</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Enable debug logging</td>
</tr>
<tr>
<td><code>io.connect.timeout</code></td>
<td>Integer (1000-300000)</td>
<td><span style="color: #007acc">5000</span></td>
<td>Connection timeout (1s-5min)</td>
</tr>
<tr>
<td><code>io.read.timeout</code></td>
<td>Integer (1000-300000)</td>
<td><span style="color: #007acc">30000</span></td>
<td>Read timeout (1s-5min)</td>
</tr>
<tr>
<td><code>io.max.stream.size</code></td>
<td>Long</td>
<td><span style="color: #007acc">2147483647</span></td>
<td>Stream size limit (2GB)</td>
</tr>
<tr>
<td><code>io.max.decompression.size</code></td>
<td>Long</td>
<td><span style="color: #007acc">2147483647</span></td>
<td>Decompression size limit (2GB)</td>
</tr>
<tr>
<td><code>io.path.validation.disabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Path security validation enabled</td>
</tr>
<tr>
<td><code>io.url.protocol.validation.disabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>URL protocol validation enabled</td>
</tr>
<tr>
<td><code>io.allowed.protocols</code></td>
<td>Comma-separated</td>
<td><span style="color: #007acc">http,https,file,jar</span></td>
<td>Allowed URL protocols</td>
</tr>
<tr>
<td><code>io.file.protocol.validation.disabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>File protocol validation enabled</td>
</tr>
<tr>
<td><code>io.debug.detailed.urls</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Detailed URL logging disabled</td>
</tr>
<tr>
<td><code>io.debug.detailed.paths</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Detailed path logging disabled</td>
</tr>
<tr style="background-color: #f0f4f8;">
<td colspan="4" style="color: #334155; font-weight: bold; padding: 8px;" id="mathutilities"><strong>MathUtilities</strong></td>
</tr>
<tr>
<td><code>mathutilities.security.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Master switch for all MathUtilities security features</td>
</tr>
<tr>
<td><code>mathutilities.max.array.size</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Array size limit for min/max operations</td>
</tr>
<tr>
<td><code>mathutilities.max.string.length</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>String length limit for parsing</td>
</tr>
<tr>
<td><code>mathutilities.max.permutation.size</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>List size limit for permutations</td>
</tr>
<tr style="background-color: #f0f4f8;">
<td colspan="4" style="color: #334155; font-weight: bold; padding: 8px;" id="reflectionutils"><strong>ReflectionUtils</strong></td>
</tr>
<tr>
<td><code>reflectionutils.security.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Master switch for all ReflectionUtils security features</td>
</tr>
<tr>
<td><code>reflectionutils.dangerous.class.validation.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Block dangerous class access</td>
</tr>
<tr>
<td><code>reflectionutils.sensitive.field.validation.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Block sensitive field access</td>
</tr>
<tr>
<td><code>reflectionutils.max.cache.size</code></td>
<td>Integer</td>
<td><span style="color: #007acc">50000</span></td>
<td>Maximum cache size per cache type</td>
</tr>
<tr>
<td><code>reflectionutils.dangerous.class.patterns</code></td>
<td>Comma-separated patterns</td>
<td><span style="color: #007acc; font-size: 7pt">java.lang.Runtime,java.lang.Process,<br>java.lang.ProcessBuilder,sun.misc.Unsafe,<br>jdk.internal.misc.Unsafe,<br>javax.script.ScriptEngine,<br>javax.script.ScriptEngineManager</span></td>
<td>Dangerous class patterns</td>
</tr>
<tr>
<td><code>reflectionutils.sensitive.field.patterns</code></td>
<td>Comma-separated patterns</td>
<td><span style="color: #007acc; font-size: 7pt">password,passwd,secret,secretkey,<br>apikey,api_key,authtoken,accesstoken,<br>credential,confidential,adminkey,private</span></td>
<td>Sensitive field patterns</td>
</tr>
<tr>
<td><code>reflection.utils.cache.size</code></td>
<td>Integer</td>
<td><span style="color: #007acc">1500</span></td>
<td>Reflection cache size</td>
</tr>
<tr style="background-color: #f0f4f8;">
<td colspan="4" style="color: #334155; font-weight: bold; padding: 8px;" id="stringutilities"><strong>StringUtilities</strong></td>
</tr>
<tr>
<td><code>stringutilities.security.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Master switch for all StringUtilities security features</td>
</tr>
<tr>
<td><code>stringutilities.max.hex.decode.size</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Max hex string size for decode()</td>
</tr>
<tr>
<td><code>stringutilities.max.wildcard.length</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Max wildcard pattern length</td>
</tr>
<tr>
<td><code>stringutilities.max.wildcard.count</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Max wildcard characters in pattern</td>
</tr>
<tr>
<td><code>stringutilities.max.levenshtein.string.length</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Max string length for Levenshtein distance</td>
</tr>
<tr>
<td><code>stringutilities.max.damerau.levenshtein.string.length</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Max string length for Damerau-Levenshtein</td>
</tr>
<tr>
<td><code>stringutilities.max.repeat.count</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Max repeat count for repeat() method</td>
</tr>
<tr>
<td><code>stringutilities.max.repeat.total.size</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Max total size for repeat() result</td>
</tr>
<tr style="background-color: #f0f4f8;">
<td colspan="4" style="color: #334155; font-weight: bold; padding: 8px;" id="systemutilities"><strong>SystemUtilities</strong></td>
</tr>
<tr>
<td><code>systemutilities.security.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Master switch for all SystemUtilities security features</td>
</tr>
<tr>
<td><code>systemutilities.environment.variable.validation.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Block sensitive environment variable access</td>
</tr>
<tr>
<td><code>systemutilities.file.system.validation.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Validate file system operations</td>
</tr>
<tr>
<td><code>systemutilities.resource.limits.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Enforce resource usage limits</td>
</tr>
<tr>
<td><code>systemutilities.max.shutdown.hooks</code></td>
<td>Integer</td>
<td><span style="color: #007acc">100</span></td>
<td>Maximum number of shutdown hooks</td>
</tr>
<tr>
<td><code>systemutilities.max.temp.prefix.length</code></td>
<td>Integer</td>
<td><span style="color: #007acc">100</span></td>
<td>Maximum temporary directory prefix length</td>
</tr>
<tr>
<td><code>systemutilities.sensitive.variable.patterns</code></td>
<td>Comma-separated patterns</td>
<td><span style="color: #007acc; font-size: 7pt">PASSWORD,PASSWD,PASS,SECRET,KEY,<br>TOKEN,CREDENTIAL,AUTH,APIKEY,API_KEY,<br>PRIVATE,CERT,CERTIFICATE,DATABASE_URL,<br>DB_URL,CONNECTION_STRING,DSN,<br>AWS_SECRET,AZURE_CLIENT_SECRET,<br>GCP_SERVICE_ACCOUNT</span></td>
<td>Sensitive variable patterns</td>
</tr>
<tr style="background-color: #f0f4f8;">
<td colspan="4" style="color: #334155; font-weight: bold; padding: 8px;" id="traverser"><strong>Traverser</strong></td>
</tr>
<tr>
<td><code>traverser.security.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Master switch for all Traverser security features</td>
</tr>
<tr>
<td><code>traverser.max.stack.depth</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Maximum stack depth</td>
</tr>
<tr>
<td><code>traverser.max.objects.visited</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Maximum objects visited</td>
</tr>
<tr>
<td><code>traverser.max.collection.size</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Maximum collection size to process</td>
</tr>
<tr>
<td><code>traverser.max.array.length</code></td>
<td>Integer</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Maximum array length to process</td>
</tr>
<tr style="background-color: #f0f4f8;">
<td colspan="4" style="color: #334155; font-weight: bold; padding: 8px;" id="urlutilities"><strong>UrlUtilities</strong></td>
</tr>
<tr>
<td><code>urlutilities.security.enabled</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Master switch for all UrlUtilities security features</td>
</tr>
<tr>
<td><code>urlutilities.max.download.size</code></td>
<td>Long</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Max download size in bytes</td>
</tr>
<tr>
<td><code>urlutilities.max.content.length</code></td>
<td>Long</td>
<td><span style="color: #007acc">0</span> (disabled)</td>
<td>Max Content-Length header value</td>
</tr>
<tr>
<td><code>urlutilities.allow.internal.hosts</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">true</span></td>
<td>Allow access to internal/local hosts</td>
</tr>
<tr>
<td><code>urlutilities.allowed.protocols</code></td>
<td>Comma-separated</td>
<td><span style="color: #007acc">http,https,ftp</span></td>
<td>Allowed protocols</td>
</tr>
<tr>
<td><code>urlutilities.strict.cookie.domain</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Enable strict cookie domain validation</td>
</tr>
<tr style="background-color: #f0f4f8;">
<td colspan="4" style="color: #334155; font-weight: bold; padding: 8px;" id="converter"><strong>Converter</strong></td>
</tr>
<tr>
<td><code>converter.modern.time.long.precision</code></td>
<td><code>millis</code>, <code>nanos</code></td>
<td><span style="color: #007acc">millis</span></td>
<td>Precision for Instant, ZonedDateTime, OffsetDateTime conversions</td>
</tr>
<tr>
<td><code>converter.duration.long.precision</code></td>
<td><code>millis</code>, <code>nanos</code></td>
<td><span style="color: #007acc">millis</span></td>
<td>Precision for Duration conversions</td>
</tr>
<tr>
<td><code>converter.localtime.long.precision</code></td>
<td><code>millis</code>, <code>nanos</code></td>
<td><span style="color: #007acc">millis</span></td>
<td>Precision for LocalTime conversions</td>
</tr>
<tr style="background-color: #f0f4f8;">
<td colspan="4" style="color: #334155; font-weight: bold; padding: 8px;" id="other"><strong>Other</strong></td>
</tr>
<tr>
<td><code>java.util.force.jre</code></td>
<td><code>true</code>, <code>false</code></td>
<td><span style="color: #007acc">false</span></td>
<td>Force JRE simulation (testing only)</td>
</tr>
</table>

> **Note:** All security features are disabled by default for backward compatibility. Most properties accepting `0` disable the feature entirely. Properties can be set via system properties (`-D` flags) or environment variables.

---

## Quick Examples

### Enable Security Features

```bash
# Development (permissive)
-Dreflectionutils.security.enabled=false

# Staging (warning mode)
-Dreflectionutils.security.enabled=true
-Dreflectionutils.dangerous.class.validation.enabled=false

# Production (full security)
-Dreflectionutils.security.enabled=true
-Dreflectionutils.dangerous.class.validation.enabled=true
-Dreflectionutils.sensitive.field.validation.enabled=true
```

### Resource Limits

```bash
# Prevent memory exhaustion attacks
-Ddeepequals.max.collection.size=1000000
-Dstringutilities.max.repeat.total.size=10485760
-Dmathutilities.max.array.size=1000000
```

### Network Security

```bash
# Restrict allowed protocols
-Dio.allowed.protocols=https
-Durlutilities.allowed.protocols=https

# Prevent SSRF
-Durlutilities.allow.internal.hosts=false
-Durlutilities.max.download.size=104857600
```

---

[Back to README](README.md)
