### Redirecting `java.util.logging` (JUL) from this Library

This library uses `java.util.logging.Logger` (JUL) for its internal logging. This is a common practice for libraries to avoid imposing a specific logging framework dependency on their users.

However, most applications use more sophisticated logging frameworks like SLF4J, Logback, or Log4j2. To integrate this library's logs into your application's existing logging setup, you'll need to install a "bridge" that redirects JUL messages to your chosen framework.

**All the configurations below are application-scoped.** This means you make these changes once in your application's setup code or configuration. You are essentially telling your entire application how to handle JUL logs.

---

**Optional: Using `java.util.logging` Directly with Consistent Formatting**

If you are *not* bridging JUL to another framework but want to use JUL directly within your application (or for simple standalone cases), this library provides `LoggingConfig` to apply a consistent console format for JUL messages.

*   **What it does:** It configures JUL's `ConsoleHandler` to use a specific formatter.
*   **How to use it:** Call `LoggingConfig.init()` early in your application's startup (e.g., at the beginning of your `main` method) to use the default pattern.
    ```java
    // In your application's main class or an initialization block
    public static void main(String[] args) {
        LoggingConfig.init(); // Uses default format "yyyy-MM-dd HH:mm:ss.SSS"
        // ... rest of your application startup
    }
    ```
*   To pass a custom date/time pattern:
    ```java
    LoggingConfig.init("yyyy/MM/dd HH:mm:ss");
    ```
*   The pattern can also be supplied globally via the system property `ju.log.dateFormat`:
    ```bash
    java -Dju.log.dateFormat="HH:mm:ss.SSS" -jar your-app.jar
    ```
*   **Where does this code go?** Typically, in the `main` method of your application, or in a static initializer block of your main class, or an early initialization routine.
*   **What file?** Your application's main Java file or an initialization-specific Java file.
*   **Important:** This `LoggingConfig` is only relevant if you intend to use JUL directly. If you are bridging JUL to another framework (as described below), that framework will control the formatting.

---

### Bridging JUL to Other Logging Frameworks

The following sections describe how to redirect JUL logs to popular logging frameworks. You'll generally perform two steps:
1.  Add a "bridge" or "adapter" dependency to your project's build file (e.g., `pom.xml` for Maven, `build.gradle` for Gradle).
2.  Perform a one-time initialization, either programmatically or via a system property.

#### 1. SLF4J (and by extension, Logback, Log4j 1.x, etc.)

SLF4J is a popular logging facade. If your application uses SLF4J (often with Logback or Log4j 1.x as the backend), you can redirect JUL logs to SLF4J.

*   **Step 1: Add the `jul-to-slf4j` Bridge Dependency**

    Ensure the `jul-to-slf4j.jar` is on your application's classpath.
    *   **Maven (`pom.xml`):**
        ```xml
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>jul-to-slf4j</artifactId>
            <version>2.0.7</version> <!-- Use the version compatible with your SLF4J API version -->
        </dependency>
        ```
    *   **Gradle (`build.gradle`):**
        ```gradle
        dependencies {
            implementation 'org.slf4j:jul-to-slf4j:2.0.7' // Use the version compatible with your SLF4J API version
        }
        ```

*   **Step 2: Install the Bridge Programmatically**

    Add the following Java code to run once, very early in your application's startup sequence.
    *   **Java Code:**
        ```java
        import org.slf4j.bridge.SLF4JBridgeHandler;

        public class MainApplication {
            public static void main(String[] args) {
                // Remove existing JUL handlers (optional but recommended to avoid duplicate logging)
                SLF4JBridgeHandler.removeHandlersForRootLogger();

                // Add SLF4JBridgeHandler to JUL's root logger
                SLF4JBridgeHandler.install();

                // ... rest of your application initialization and startup
            }
        }
        ```
    *   **Where does this code go?**
        *   In your application's `main` method (as shown above).
        *   In a static initializer block of your main application class.
        *   If using a framework like Spring Boot, in a method annotated with `@PostConstruct` in a configuration class, or an `ApplicationListener<ApplicationReadyEvent>`.
            The key is that it must run *before* any JUL logs from this library (or others) are emitted that you want to capture.
    *   **Why `removeHandlersForRootLogger()`?** JUL might have default handlers (like a `ConsoleHandler`) already configured. If you don't remove them, logs might be processed by both JUL's original handlers *and* then again by SLF4J, leading to duplicate output.

*   **If you use Logback:** Logback is an SLF4J-native implementation. Follow the exact same steps above (add `jul-to-slf4j` and install `SLF4JBridgeHandler`). JUL logs will then flow through SLF4J to Logback, and Logback's configuration (`logback.xml`) will control their final output and formatting.

#### 2. Log4j 2

Log4j 2 provides its own adapter to bridge JUL calls.

*   **Step 1: Add the `log4j-jul` Adapter Dependency**

    Ensure the `log4j-jul.jar` is on your application's classpath. It's part of the Log4j 2 distribution.
    *   **Maven (`pom.xml`):**
        ```xml
        <dependency>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-jul</artifactId>
            <version>2.20.0</version> <!-- Use your Log4j 2 version -->
        </dependency>
        ```
    *   **Gradle (`build.gradle`):**
        ```gradle
        dependencies {
            implementation 'org.apache.logging.log4j:log4j-jul:2.20.0' // Use your Log4j 2 version
        }
        ```

*   **Step 2: Set the JUL LogManager System Property**

    Configure the JVM to use Log4j 2's `LogManager` for JUL by setting a system property. This property must be set *before* any `java.util.logging.LogManager` class is loaded, so it's best set when the JVM starts.
    *   **System Property:**
        `-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager`
    *   **How to set this system property?**
        *   **Command Line:** When launching your Java application:
            ```bash
            java -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager -jar your-app.jar
            ```
        *   **IDE Run/Debug Configuration:** Most IDEs (IntelliJ IDEA, Eclipse, VS Code) have a section in the "Run/Debug Configurations" panel to specify "VM options" or "JVM arguments". Add the `-D` property there.
        *   **Build Tools (for execution or test phases):**
            *   Maven Surefire/Failsafe (for tests):
                ```xml
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <configuration>
                        <systemPropertyVariables>
                            <java.util.logging.manager>org.apache.logging.log4j.jul.LogManager</java.util.logging.manager>
                        </systemPropertyVariables>
                    </configuration>
                </plugin>
                ```
            *   Gradle (for `JavaExec` tasks or `test` task):
                ```gradle
                tasks.withType(JavaExec) {
                    systemProperty 'java.util.logging.manager', 'org.apache.logging.log4j.jul.LogManager'
                }
                test {
                    systemProperty 'java.util.logging.manager', 'org.apache.logging.log4j.jul.LogManager'
                }
                ```
        *   **Environment Variables:** You can set `JAVA_OPTS` or `MAVEN_OPTS` (though these are broad and affect all Java/Maven processes started with them):
            ```bash
            export JAVA_OPTS="$JAVA_OPTS -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager"
            ```
    *   Once this system property is set, all `java.util.logging` output will be routed to Log4j 2, and its configuration (e.g., `log4j2.xml`) will control the output.

---

Most application developers are comfortable bridging JUL output to their preferred logging framework when needed. By relying on `java.util.logging` by default, this library remains lightweight and avoids imposing specific logging dependencies.