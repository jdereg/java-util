n-cube
======
n-cube is a Rules Engine, Decision Table, Decision Tree, Templating Engine, CDN Proxy Router, and Enterprise Spreadsheet, built as a hyper-space.  The Domain Specific Language (**DSL**) for the rules is **Groovy**. To include in your project:

```
<dependency>
  <groupId>com.cedarsoftware</groupId>
  <artifactId>n-cube</artifactId>
  <version>3.1.3</version>
</dependency>
```
<a class="coinbase-button" data-code="1eb8ea37a2609606bb825ab2d4d3692f" data-button-style="custom_small" data-custom="NCUBE" href="https://coinbase.com/checkouts/1eb8ea37a2609606bb825ab2d4d3692f">Purchase Life-time License</a><script src="https://coinbase.com/assets/button.js" type="text/javascript"></script>

The image below is a Visual Summary of the main capabilities of n-cube.
![Alt text](https://raw.githubusercontent.com/jdereg/n-cube/master/n-cubeImage.png "n-cube Capabilities")

What are the components of an n-cube?
An n-cube has a set of axes (plural of axis), each of which adds a dimension. For example, in Excel, there are two axes, one axis numbered [rows] and one lettered [columns]. Within n-cube, each axis has a name, like 'State', 'Date', 'year', 'Gender', 'Month', etc.

Each axis contains columns.  In excel, the columns are just numbers and letters.  In n-cube, the columns can be a set of values like the months of a year, years, age ranges, price ranges, dates, states, coordinates (2D, 3D, lat/lon), expressions, and so on.

A column can be a simple data type like a String, Number, Date, but it can also represent a Range [low, hi) as well as a Set (a combination of discrete values and ranges).  A column can also contain expressions or any class that implements Java's Comparable interface.  It is these columns that input coordinates match (bind to).

Finally, there are cells.  In a spreadsheet, you have a row and column, like B25 to represent a cell.  In n-cube, a cell is similarly represented by a coordinate.  A Java (or Groovy) `Map` is used, where the key is the name of an axis, and the value is the value that will 'bind' or match a column on the axis.  If a value does not match any column on an axis, you have the option of adding a 'default' column to the axis.  Here's an example `[age:24, state:'CA', date:'2012/12/17']`.  The format given here for a `Map` is the declarative form used by Groovy.  In Java, that would be `map.put("age", 24)`, `map.put("state", "CA")` and so on.  Because the Groovy form is much shorter, it will be used from here on out to represent coordinate maps.  Because a `Map` is used as the input coordinate, you can have as many dimensions (keys) as desired.

Once an n-cube is set up, and a coordinate is also set up (e.g. Map coord = [age:24, state:'CA']), the most basic API to access it is `ncube.getCell(coord)`.  The return value will be the value of the cell at the given coordinate.  If the cell contains a simple value (`String`, `integer`, `Date`, `floating point number`, `boolean value`), it is returned.  If the cell contains an expression (written in Groovy), the expression is executed.  The return value for the cell in this case is the return value of the expression.

Expressions can be a simple as: `input.age > 17`, which would return `true` if the 'age' key on the input coordinate (map) was greater than 17, or `false` if not.  Expressions can be as complex as an entire `Class` with multiple methods (that can use other classes).  Expressions are written in Groovy.  See http://groovy.codehaus.org/.  Groovy was chosen because it is essentially Java (has Java syntax, compiles and runs at Java speed), but has many syntactic short-cuts that result in shorter code as compared to Java.

A cell in an n-cube can reference another cell within the same n-cube, like you might do in Excel.  For example, you may have a formula in Excel like this: `=b25 + b32 * 2`, stored say in `A1`.  The value for `A1` would be computed using the formula stored in `A1`.  N-cube allows these same capabilities, plus more (code / business logic).  A cell could have an `if` statement in it, a `for-loop`, `switch statement`, reference other cells within the same cube, or it can reference cells within different n-cubes.  The referenced cell can then be another formula, reference other cells, and so on.

### Rule Engine
When used as a rule engine, at least one axis within the n-cube is marked as as 'Rule' axis type.  In that case, each column is written as a condition (in Groovy).  For example, `input.age < 18`.  When a Rules n-cube is executed, each condition on the Rule axis is evaluated.  If the value is `true` (as how Groovy considers truth: http://groovy.codehaus.org/Groovy+Truth), then the associated cell is executed.  If no conditions are executed, and there is a default column on the rule axis, then the statement associated to the default column is executed.

To kick off the Rule execution, `ncube.getCell(coord, output)` is called. The conditions along the Rule axis are executed linearly, in order. Condition columns can reference values passed in on the input map (using `input.age`, `input.state`, etc.) as well as cells within other cubes.

The input coordinate map is referenced through the variable `input`.  The output map is referenced through the variable `output`.  Both can be referenced in the condition as well as in the cell (for expression, method, and template cells).  Typically, when used in rule mode, as conditions fire, the corresponding cell that is executed writes something to the output map.  For example in an pricing application, `state =='CA' || state == 'TX'` as the condition, the corresponding cell may have `output.productCost *= 1.07`.  The tax condition, for example.

The condition column can contain multiple statements.  Think of it like a method body.  The value of the last statement executed is evaluated as the condition. Your code has access to the input coordinate (map), output map, and the n-cube in which the code resides.  All Java code libraries and Groovy can be accessed as well.  For example, `println` from Groovy can be added to the conditions for debugging (as well as added to the executed cell).  The Groovy expression (or methods) in the executed cell can write multiple outputs to the output map.

As each condition on the Rule axis is executed, the n-cube rule engine writes information to a "_rule" entry into the output map.  This _rule entry is a `Map` which includes the condition name executed, the condition expression executed, and other useful information.  This allows you to evaluate the rule exection while developing the rules, to see rules fired.  This Map can be cast to `RuleInfo`, which has explicit APIs on it to retreive values from it, eliminating the need to know the keys.

In general, as cells execute, they write to the `output` map.  The `input` coordinate could be written to as well.  If it is modified, and a further n-cube is referenced, any modifications to the `input` coordinate will remain in place until that execution path returns.  When the execution path of the rules finishes and returns, the `input` map is restored to it's prior condition before execution. When returning then to an outer n-cube (or the code that called `ncube.getCell()`), that code will see no changes to the `input` map.  The `output` map will, of course, contain whatever changes were written to it.

Both condition columns and executed cells can tell the rule engine to restart execution of the conditions as well as to terminate any further conditions from being executed.  This is a linear rules execution flow, and intentionally not the RETE algorithm.

### Decision Table
When using n-cube as a decision table, each axis represents a decision variable.  For example, a state axis with all of the states of a country.  When accessed, the `input` coordinate would have the 'state' variable as a key in the input map, and the associated value to state would be a state, for example, 'KS' (Kansas).  If the data changes over time, it is common to add a 'date' axis.  Meaning that at one point in time, say for Kansas, the value 10 was returned, but within a different time frame, perhaps 11 is returned.

Common decision variables are country, state / providence, date, business unit, business codes, user role, actions, resources, and so no.  There is no limit to the number of axes that an n-cube can have (other than memory).

Decision tables are great and work best when all variable combinations make sense (a * b * c ... * n).  If the problem space has some combinations that do not make sense, then you may want to use n-cube's Decision Tree capability.  n-cube allows you to combine decision tables, decision trees, rules, and so on, ad infinitum.

### Decision Tree
A good example for a decision tree, is modeling the continents and countries of the world.  Not all continents have the same countries.  Therefore, it would not make sense to have an n-cube with one axis as 'continents' and another axis as 'countries.'  Instead, the initial (entry or outer n-cube) 'world' would have an axis 'continents', with columns Africa, Antarctica, Asia, Australia, Europe, North America, South America.  For each continent column, it's corresponding cell is a reference to a 'country' n-cube for that continent.  The cell reference is written like this: `@NorthAmericaCountries[:]` for example.  When this cell is executed, it in turn calls the 'NorthAmericaCountries' n-cube with the same input as was passed to the original ncube.  The `[ : ]` means that no modifications are being made to the input.  Additional inputs could be added here, as well as existing inputs could be changed before accessing the joined n-cube.

In the 'NorthAmericaCountries' n-cube, the cells would return a value (or if a subdivision of the countries is needed like 'States', the cells would join to yet further n-cubes modeling those subdivisions).  In order to 'talk to' or 'use' this n-cube decision tree, the code would look like this: `Map coord = [Continent:'NA', Country:'USA', State:'OH']` for example.  This would hit the North America column in the world n-cube, that cell would call the NorthAmericaCountries n-cube, which would then join to the 'UsaStates' n-cube.  To reach a Canadian province, for example, the input coordinate would look like this: `Map coord = [Continent:'NA', Country:'Canada', Province:'Quebec']`.  Notice that the 3rd parameter to the input is not state but province.  Both inputs work, because at each decision level, the appropriate n-cubes join to each other.

At each n-cube along the decision path, it could have additional 'scope' or dimensionality.  For example, a product axis may exist as a second axis on the cubes (or some of the cubes).  Think of a decision tree as stitching together multiple decision tables.  The cells are whatever you need them to be (Strings, numbers, Java objects, Groovy code to executed, etc.) In the case of code, think of your execution path of your program as going through a 'scope router' or 'scope filter' before the appropriate code is selected and executed.

### Template Engine
n-cube can be used to return templates (think of a template as an HTML page, for example, with replaceable parts - like mail merge.  Not limited to HTML, it could be any text file.)  When a template cell is executed, variables within the template are replaced (like mail merge).  If you have used the Apache project's Velocity project, Groovy templates, or have written JSP / ASP files, then you already have an idea on how to use templates.

Snippets written like this `<%  code or variable references   %>` or `${code / variable references}` can be added to the template.  Before the template is returned (think HTML page), these variable sections are executed.  The replaceable sections can reference n-cubes, for example, to get language specific content, region specific content, mobile / non-mobile content, browser specific content, and so on, to then fill-in a variable portion of the page.

Instead of actually storing the HTML, Groovy Code, etc. directly in an n-cube cell, the content can be referenced via a URL.  This allows the HTML page to be stored on a CDN (Content Delivery Network), and then selectively retrieved (per language, state, business unit, date, etc.) and then substitutions within the page made as well (if needed, using the templating mechanism).  Image files can be referenced this way as well, allowing different images to be retrieved depending on state, date, language, product, and so on.

### CDN Proxy Router
N-cube cells can be specified by URLs.  In the case of a Content Delivery Network, HTML files, Images, Javascript files, etc, can be also listed as URLs.  Used this way, the content is transferred back to the requesting (calling app).  Typically this is accomplished by using the UrlRewriteFilter (http://tuckey.org/urlrewrite/) inside Tomcat.  This filter is similar to the Apache webserver's mod_rewrite module.  By routing dyn/* to n-cube's CdnRouter class, the HTTP request will be proxied (resent) to the intended destination.  The HTTP response will then be returned to the original caller.

Used in this fashion, HTTP requests target a CDN n-cube, the n-cube may have axes on it for state, device type, date, etc., and depending on those may serve up different content depending on the logical name being requested.  For example, an HTML page uses a logical request like this: "dyn/html/account".  Notice that this is a logical URL.  No file extension is listed.  This request is received on Tomcat and redirected (using UrlRewriteFilter) to the n-cube CdnRouter.  The router makes a request to a 'decision tree' n-cube that first routes based on type (html, css, js, images, etc.).  This outer n-cube is a Decision tree that has a branch for each content type.

The next cube maps the logical name to the desired actual name.  In the example above, the HTML ncube has the logical HTML file names on one axis, and the cells have URLs to the real content.  This indirection allows the content to be moved without the page having to be changed.  Furthermore, if the page (or style sheet or Javascript code) returned needed to be different because of the user-agent device, the date, etc, then the routing cube can have an axis for each of these additional decision criteria.

HTTP Request ===> dyn/html/account ===> tomcat ===> UrlRewrite.xml ===> CdnRouter ===> content-n-cubes ===> physical file.  The content-n-cubes have the logical file names on the content.name axis, and the associated cell has the physical name.  If it is not found, the default cell will add the appropriate extension to the file type, and then make an attempt at fetching the content.  This way, these mime-type routing cubes only require entries on their axis when the logical to phsyical file name mapping is non-standard (changing based on device type, date, business unit, etc.)

### Creating n-cubes
Use either the Simple JSON format to create n-cubes, or the nCubeEditor to editing the pages.  At the moment, there is no cloud-based editor for n-cube, so you need to set up the nCubeEditor as a web-app within a Java container like tomcat or jetty.  See the sample .json files in the test / resources directories for examples.

These are read in using the NCubeManager.getNCubeFromResource() API.  You can also call ncube.fromSimpleJson(String json).

#### Licensing
n-cube can be used free for personal use.

Version History
* 3.1.3
 * Bug fix: Fixed bug in rule engine where Boolean.equals() was being called instead of isTrue() - which uses proper Groovy Truth.  This bug was introduced in 3.1.1.
* 3.1.2
 * Bug fix: Tightened up regex pattern match that is used to expand relative references into getRelativeCubeCell() calls.  This prevents it from matching popular Java/Groovy annotations in the source code wtihin an expression cell.
 * Started work on GitPersister
* 3.1.1
 * Bindings to rule axis with a name is O(1) - directly starts evaluating the named condition.
 * Rule axis now has `fireAll` (versus fire once).  Fire all conditions is the default and what previously existed.  If the `fireAll` property of the `Axis` is set false on a Rule `Axis`, then the first condition that fires, will be the only condition that fires on that axis.
 * `NCubeInfoDto` now includes the SHA1 of the persisted n-cube.
 * bug fix: HTML and JSON formatters handle when the cell contents are a classLoader, as in the case of sys.classpath after it has been loaded.
 * bug fix: rule's with condition of null are now converted to false, allowing the JSON cube that contained such a condition to be loaded.
 * Rule Engine execution performance improvement - evaluation of the current axis to column binding set stops immediately if the rule axis is not bound (for the current condition being evaluated).
 * Many new tests added, including more concurrency tests
 * Moved to Log4J2
* 3.1.0
 * All JUnit test cases converted from Java to Groovy.
 * Improvement in classloader management.  Initially, a classloader per App (tenant, app, version) was maintained.  This has been further refined to support any additional scope that may have been added to the `sys.classpath` cube.  This allows a different URL set per AppID per additional scope like a business unit, for example.
* 3.0.10
 * Attempting to re-use GroovyClassLoader after clearCache(appId). Discovered that the URLs do not clear.
* 3.0.9
 * Internal work on classpath management.  Fixing an issue where clearing the cache needed to reset the URLs within the GroovyClassLoader.
* 3.0.8
 * Bug fix: Threading issue in NCubeManager during initialization.  GroovyClassLoaders could be accessed before the resource URLs were added to the GroovyClassLoader.
 * Bug fix: CdnClassLoader was allowing .class files to be loaded remotely, which 1) is too slow to allow (.class files are attempted to be loaded with HTTP GET which fails very slowly with a 404, and 2) is insecure.  Instead, a future version will allow a 'white-less' of acceptable classes that can be remotely loaded.
* 3.0.6 / 3.0.7
 * Changed `getDeltaDescription()` to return a list of `Delta` objects, which contain the textual difference as well as the location (NCube, Axis, Column, Cell) of the difference and the type of difference (ADD, DELETE, UPDATE).
* 3.0.5
 * Added `getDeltaDescription()` to `NCube` which returns a `List` of differences between the two cubes, each entry is a unique difference.  An empty list means there are no differences.
* 3.0.4
 * Test results now confine all output to the RuleInfo (no more output in the output keys besides '_rule').
 * Formatting of test output now includes `System.out` and `System.err`.  `System.err` output shows in dark red (typical of modern IDEs).
* 3.0.3
 * Added `NCubeInfoDto` to list of classes that are available to Groovy Expression cells, without the author having to import it (inherited imports).
 * Added checks to NCubeManager to prevent any mutable operation on a release cube. Added here in addition to the perister implementations.
* 3.0.2
 * Improved support for reading cubes that were stored in json-io serialized format.
* 3.0.1
 * `NCubeManager` has new API, `resolveRelativeUrl()`.  This API will take a relative URL (com/foo/bar.groovy) and return an absolute URL using the sys.classpath for the given ApplicationID.
 * Bug fix: test data was being cleared when an update cube happened.  The test data was not being copied to the new revision.
* 3.0.0
 * `NCubeManager` no longer has `Connection` in any of it's APIs. Instead a `NCubePersister` is set into the `NCubeManager` at start up, and it uses the persister for interacting with the database.  Set a `ConnectionProvider` inside the `Persister` so that it can obtain connections.  This is in preparation of MongoDB persister support.
 * Cubes can now be written to, in addition to being read from while executing.  This means that n-cube can now be used for transactional data.
 * Caching has been greatly improved and simplified from the user's perspective.  In the past, `NCubeManager` had to be told when to load cubes, and when it could be asked to fetch from the cache.  With the new caching strategy, cubes are loaded with a simple `NCubeManager.getCube()` call.  The Manager will fetch it from the persister if it is not already in it's internal cache.
 * Revision history support added. When a cube is deleted, updated, or restored, an new record is created with a higher revision number.  Cubes are never deleted.  This enabled the new restore capability as well as version history.
 * `NCubeManager` manages the classpath for each Application (tenant, app, version, status).  The classpath is maintained in the `sys.classpath` cube as `List` of `String` paths (relative [resource] entries as well as jar entries supported).
 * `NCubeManager` manages the Application version.  `NCubeManager` will look to the 0.0.0 SNAPSHOT version of the `sys.bootstrap` cube for the App's version and SNAPSHOT. This makes it simple to manage version and status within this single cube.
 * When loading a `GroovyExpression` cell that is specified by a relative URL that ends with .groovy, and attempt will be made to locate this class already in the JVM, as might be there when running with a code coverage tool like Clover.
 * `ApplicationID` class is available to `GroovyExpression` cells.
 * Classpath, Method caches, and so forth are all scoped to tenant id.  When one is cleared, it does not clear cache for other tenants (`ApplicationID`s).
 * Removed unnecessary synchronization by using `ConcurrentHashMap`s.
 * JVM now handles proxied connections.
 * Many more tests have been added, getting code coverage to 95%.
* 2.9.18
 * Carrying ApplicationID throughout n-cube in preparation for n-cube 3.0.0.  This version is technically a pre-release candidate for 3.0.0.  It changes API and removes deprecated APIs.
* 2.9.17
 * Updated CSS tags in Html version of n-cube
 * bug fix: Removed StackOverflow that would occur if an n-cube cell referenced a non-existent n-cube.
 * n-cube names are now treated as case-retentive (case is ignored when locating them, however, original case is retained).
 * Improved unit test coverage.
* 2.9.16
 * Rule name is now displayed (if the 'name' meta-property on column is added) in the HTML.
 * Updated to use Groovy 2.3.7 up from 2.3.4
 * Added more exclusions to the CdnClassLoader to ensure that it does not make wasteful requests.
 * getCubeNames() is now available to Groovy cells to obtain the list of all n-cubes within the app (version and status).
* 2.9.15
 * Added getCube(), getAxis(), getColumn() APIs to NCubeGroovyExpression so that executing cells have easy access to these elements.
 * Added many n-cube classes and all of Java-util's classes as imports within NCubeGroovyExpression so that executing cells have direct access to these classes without requiring them to perform imports.
* 2.9.14
 * Required Scope and Optional Scope supported added.  Required scope is the minimal amount of keys (Set<String>) that must be present on the input coordinate in order to call ncube.getCell().  The n-cube meta property requiredScopeKeys can be set to a Groovy Expression (type="exp") in order to return a List of declared required scope keys.  Optional scope is computed by scanning all the rule conditions, and cells (and joining to other cubes) and including all of the scope keys that are found after 'input.'  The required scope keys are subtracted from this, and that is the full optional scope. Calls to ncube.setCell() need only the scope keys that the n-cube demands (values for Axes that do not have a Default column and are not Rule axes).  The declared required scope keys are not required for setCell(), removeCell(), or containsCell().
 * The 'RUN' feature (Tests) updated to use custom JSON written format, insulating the code from changes.
 * The 'RUN' feature obtains the Required Scope using the new Required Scope API.
* 2.9.13
 * The sys.classpath n-cube (one per app) now allows processing for loading .class files
 * Bug fix: not enough contrast between text URLs and background color on expression cells using URL to point to code.
* 2.9.12
 * Groovy classes from expression and method cells are compiled in parallel.
* 2.9.11
 * output.return entry added to output Map when getCell() / getCells() called.  The value associated to the key "return" is the value of the last step executed.  If it is a table of values, it is the value that was accessed.
* 2.9.8-2.9.10
 * Improvements in HTML display when a cell (or Column) has code in it.
* 2.9.7
 * Bug fix: Axis.updateColumns() should not have been processed (it is 'turned' on / off at Axis level).  This caused cells pointing to it to be dropped when the columns were edited.
* 2.9.6
 * The n-cube API that supports batch column editing (updateColumns()) has been updated to support all the proper parsing and range checking.
 * The HTML n-cube has been updated to include data-axis tags on the columns to support double-click column editing in NCE.
 * The top column row supports hover highlight.
* 2.9.5
 * SHA1 calculation of an n-cube is faster using a SHA1 MessageDigest instance directly.
 * Consolidated JsonFormatter / GroovyJsonFormatter into JsonFormatter.
 * Code moved from JsonFormatter to CellType Enum.
 * RuleInfo now a first-rate class. Dig into it (found on output map of getCell()) for rule execution tracing.
* 2.9.4
 * Rule execution tracing is complete, including calls to sub-rule cubes, sub-sub-rule cubes, etc.  It includes both 'begin>cubeName' and 'end>cubeName' markers as well as an entry for all rules that executed (condition true) in between.  If other rule cubes were called during rule execution, there execution traces are added, maintaining order.  The number of steps execution for a given rule set is kept, as well as all column bindings for each rule (indicates which columns pointed to the rule executed).
 * n-cube sha1() is now computed when formatted into JSON.  It is added as a meta-property on n-cube.  The SHA1 will be used along with NCE to determine if an n-cube has changed (basic for optimistic locking).
 * APIs added to generate test case input coordinates for all populated cells.
 * Improved HTML formatting for display in NCE (eventually NCE will do this in Javascript, and the JSON for the n-cube only will be sent to the client).
 * Code clean up related to formatting values and parsing values.
 * NCubeManager support for ApplicationID started, but not yet complete.
 * NCubeManager support for MongoDB started, but not yet complete.
 * MultiMatch flag removed from n-cube.  If you need multi-match on an axis, use a Rule axis.
* 2.9.3
 * SET and NEAREST axis values are now supported within Axis.convertStringToColumnValue().  This allows in-line editing of these values in the n-cube editor.
 * Many more tests added getting line coverage up to 96%.
 * NCube.setCellUsingObject() and NCube.getCellUsingObject() APIs removed.  Instead use NCube.objectToMap, and then call getCell() or getCells() with that Map.
* 2.9.2
 * jump() API added to expression cells.  Call jump() restarts the rule execution for the currently executing cube.  Calling jump([condition:ruleName, condition2:ruleName2, ...]) permits restarting the rule execution on a particular rule for each rule axis specified.
 * rule execution: If a rule axis is specified in the input coordinate (it is optional), then the associated value is expected to be a rule name (the 'name' field on a Column).  Execution for the rule axis will start at the specified rule.
 * Added new API to n-cube to fetch a List containing all required coordinates for each cell.  The coordinates are in terms of variable key names, not column ids.  This is useful for the n-cube Editor (GUI), allowing it to generate the Test Input coordinates for any cube.
 * N-cube hashcode() API was dramatically simplified.
 * Updated to use json-io 2.7.0
* 2.9.1
 * Added header 'content-type' for when CDN files are loaded locally from a developer's machine.  Providing the mime-type will quiet down browser warnings when loading Javascript, CSS, and HTML files.
 * Added new loadCubes() API to NCubeManager. This permits the caller to load all cubes for a given app, version, and status at start up, so that calls to other n-cubes from Groovy code will not have to worry about the other cubes being loaded.
 * Deprecated [renamed] NCubeManager.setBaseResourceUrls() to NCubeManager.addBaseResourceUrls().  It is additive, not replacing.
 * NCubeManager, Advice, Rules, and Axis tests have been separated into their own test classes, further reducing TestNCube class.
* 2.9.0
 * Bug fix: HTTP response headers are now copied case-insensitively to CdnRouter proxied HTTP response
 * New CdnDefaultHandler available for CDN content routers which dynamically adds logical file names to the CDN type specific routing cache.
* 2.8.2
 * Bug fix: CdnRouter now calls back to the CdnRoutingProvider on new API, doneWithConnection(Connection c) so that the provider knows that it can close or release the connection.
* 2.8.1
 * Bug fix: Exception handler in CdnRouter was chopping off the stack trace (only error message was getting reported).
 * Test case added for the case where an n-cube modifies itself from within execution of getCell().  The example has an axis where the cell associated to the default Column on an axis adds the sought after column.  It's akin to a Map get() call that does a put() of the item if it is not there.
* 2.8.0
 * Rule Execution: After execution of cube that had one or more rule axes (call to getCells()), the output Map has a new entry added under the "_rule" section, named "RULES_EXECUTED".  This entry contains the names (or ID if no name given) of the condition(s) and the return value of the associated statement. More than one condition could be associated to a statement in the case of a cube with 2 or more rule axes.  Therefore the keys of the Map are List, and the value is the associated statement return value.
 * Rule Execution: A condition's truth (true or false) value follows the same as the Groovy language.  For details, see http://groovy.codehaus.org/Groovy+Truth
 * Java 1.7: Template declarations updated to Java 1.7 syntax (no need to repeat collection template parameters a 2nd time on the RHS).
* 2.7.5
 * Added ability to turn Set<Long> into Map<String, Object> coordinate that will retrieve cell described by Set<Long>.  Useful for n-cube editor.
* 2.7.4
 * Bug fix: reloading n-cubes now clears all of its internal caches, thereby allowing reloading Groovy code without server restarts.
 * Bug fix: NCubeManager was not rethrowing the exception when a bad URL was passed to setBaseResourceUrls().
 * Bug fix: If a URL failed to resolve (valid URL, but nothing valid at the other end of the URL), an NPE occurred.  Now an exception is thrown indicating the URL that failed to resolve, the n-cube it resided within, and the version of the n-cube.
* 2.7.3
 * Added GroovyClassLoader 'file' version so that the .json loaded files do not need to call NCubeManager.setUrlClassLoader()
* 2.7.2
 * New API added to NCubeManager, doesCubeExist(), which returns true if the given n-cube is stored within the persistent storage.
 * HTML-syntax highlighting further improved
* 2.7.1
 * Dynamically loaded Groovy classes (loaded from URL), load much faster.
 * The HTML representation of n-cube updated to differentiate URL specified cells and expression cells, from all other cells.  Very basic syntax highlighting if you can call it that.
* 2.7.0
 * New capability: key-value pairs can be added to n-cube, any axis, and any column.  These are picked up from the JSON format or set via setMetaProperty() API.  This allows you to add additional information to an ncube, it's axis, or a column, and it will be stored and retrieved with the n-cube and can be queried later.
 * New capability: NCubeManager has a new API, setUrlClassLoader() which allows you to set a List of String URLs to be added to the Groovy class path that is used when a class references another class by import, extends, or implements.  The URL should point to the fully qualified location up to but just before the code resource (don't include the /com/yourcompany/... portion).
 * New capability: n-cube can be used as a CDN router.  Used in this fashion, fetches to get content will be routed based on the scope of the cube used to route CDN requests.  In order to use this feature, it is expected you have a servlet filter like urlRewrite from Tuckey, which will forward requests to the CdnRouter class.  Furthermore, you need to set up a CdnRouterProvider.  See the code / comments in the com.cedarsoftware.ncube.util package.
* 2.6.3
 * CdnUrlExecutor updated to handle Classpath for resolving URL content (in addition to the existing HTTP support).
* 2.6.2
 * GroovyShell made static in GroovyBase.  GroovyShell is re-entrant.  The GroovyShell is only used when parsing CommandCell URLs to allow for @refCube[:] type expansions.
* 2.6.1
 * Refactor CommandCell interface to have fewer APIs.
 * Bug fix: null check added for when 'Command Text' is empty and attempting search for referenced cube names within it.
* 2.6.0
 * An Executor can be added to a call to getCell(), getCells() where the Executor will be called instead of fetching the cell.  The defaultCellExecutor will execute the cell as before.  It can be overridden so external code can be executed before the cell is returned or after it is executed.
 * runRuleCube() API added to the NCubeGroovyExpression so that a rule can run other rules in other cubes.
 * Potential concurrency bug fixed if the URL: feature of an n-cube was used, and the cell content was not cached.
* 2.5.0
 * Advice can be specified to cube and method name, using wildcards.  For example, `*Controller.save*()` would add the advice to all n-cube Controller classes methods that start with `save`.
 * `containsCellValue()` API added to NCube.  This will return `true` if, and only if, the cell specified by the coordinate has an actual value located in it (defaultCellValue does not count).
 * `containsCell()` API changed.  It will return `true` if the cell has a value, including the defaultCellValue, if it is not null.
 * Both `containsCell()` and `containsCellValue()` throw `CoordinateNotFoundException` if the specified coordinate falls outside the n-cube's defined hyper-space.
 * New public API added to Axis, `promoteValue(AxisValueType type, Comparable value)`.  If the value passed in (e.g. an int) is of the same kind as AxisValueType (e.g. long) then the returned value will be the the larger AxisValueType (long in this example).  If the `valueType` is not of the same basic nature as `value`, an intelligent conversion will be done.  For example, String to Date, Calendar to Date, Date to String, Long to String, Integer to String, String to BigDecimal, and so on.
 * When a Rule cube executes, the output map always contains an `_rule` entry.  There is a constant now defined on NCube (`RULE_EXEC_INFO`) which is used to fetch this meta-information Map.  A new Enum has been added, `RuleMetaKeys` which contains an enum for each rule-meta information entry in this Map.
* 2.4.0
 * Advice interface added.  Allows before() and after() methods to be called before Controller methods or expressions are called.  Only expressions specified by 'url' can have advice placed around them.
 * NCube now writes 'simple JSON' format.  This is the same JSON format that is used in the test resources .json files.  NCubes stored in the database are now written in simple JSON format.  This insulates the format from member variable changes to n-cube and its related classes.
 * Expression (Rule) axis - now supports default column.  This column is fired if no prior expressions fired.  It is essentially the logical NOT of all of the expressions on a rule axis.  Makes it trivial to write the catch-if-nothing-fired-condition.
 * Expression (Rule) axis - conditions on Rule axis can now be specified with URL to the Groovy condition code.  This allows single-step debugging of lengthy conditions.
 * URL specified commands can use res:// URLs for indicating that the source is located within the Java classpath, or http(s).
 * URL specified commands can use @cube[:] references within the URL to allow the hostname / protocol / port to be indicated elsewhere, insulating URLs from changing when the host / protocol / port needs to be changed.
 * NCube methods and expressions specified by URL can now be single-stepped.
 * In the simple JSON format, type 'array' is no longer supported.  To make a cell an Object[], List, Map, etc., make the cell type 'exp' and then specify the content in List, Map, Object[], etc.  For an Object[] use "[1, 2, 3] as Object[]". For a List, use "['This', 'is', 'a list']".  For a Map, use "[key1:'Alpha', key2:'Beta']"
 * Expression cells or controller methods that have identical source code, will use the same Groovy class internally.  The source code is SHA1 hashed and keyed by the hash internally.
* 2.3.3
 * RenameNCube() API added to NCubeManager.
 * The regex that locates the relative n-cube references e.g. @otherCube[x:val], improved to no longer incorrectly find annotations as n-cube references.
 * Levenshtein algorithm moved to CedarSoftware's java-util library. N-cube already had a dependence on java-util.
* 2.3.2
 * HTML formatting improved to handle all cell data types
 * Parse routine that fetches n-cube names was matching too broad a string for n-cube name.
* 2.3.1
 * Date axis be created from, or matched with, a String that passed DateUtilities.parseDate().
 * String axis can be created from, or matched with, a Number instance.
 * Axis.promoteValue() has been made public.
* 2.3.0
 * Groovy expression, method, and template cells can be loaded from 'url' instead of having content directly in 'value' field.  In addition, the 'cacheable' attribute can be added.  When 'true', the template, expression, or method, is loaded and compiled once, and then stored in memory. If 'cacheable' attribute is 'false', then the content is retrieved on each access.
 * Within the url, other n-cubes can be referenced.  For example, @settings[:]/html/index.html.  In this example, the current input coordinate that directed access to the cell containing the URL reference, is passed as input to the referenced n-cube(s).  This allows a 'settings-type' n-cube to be used to keep track of actual domains, ports, contexts, etc., leaving the URLs in all the other cubes not needed to be changed when the domain, port, etc. is changed.
* 2.2.0
 * Axis update column value(s) support added
 * NCubeInfoDto ncube id changed from long to String
 * NCubeManager now caches n-cube by name and version, allowing two or more versions of the same named n-cube to be loaded at the same time.  Useful in multi-tenant environment.
 * NCube has the version it was loaded from 'stamped' into it (whether file or disk loaded). Use n-ube's getVersion() API to retrieve it.
* 2.1.0
 * Rule conditions and statements can stop rule execution.  ruleStop() can be called from the condition column or from a Groovy expression or method cell.
 * Output Map is written to in the '_rule' key of the output map, which is a Map, with an entry to indicate whether or not rules where stopped prematurely.  In the future, other useful rule execution will be added to this map.
 * id's specified in simple JSON format can be long, string, double, or boolean. Allows aliasing columns to be referenced by cells' id field.
 * HTML formatting code moved into separate internal formatters package, where other n-cube formatters would be placed.
* 2.0.1
 * 'binary' type added to simple JSON format.  Marks a cell to be returned as byte[].  'value' should be set to hex digits 'CAFEBABE10', or the 'url' should be set to point to location returning binary content.
 * 'cacheable' flag added to 'string' and 'binary' cells (when specified as 'url').  If not specified, the default is cacheable=true, meaning that n-cube will fetch the contents from the URL, and then hold onto it.  Set to "cacheable":false and n-cube will retrieve the content each time the cell is referenced.
* 2.0.0
 * Initial version

By: John DeRegnaucourt
