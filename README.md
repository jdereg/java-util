n-cube
======
n-cube is a Rules Engine, Decision Table, Decision Tree, Templating Engine, and Enterprise Spreadsheet, built as a hyper-space.  To include in your project:

```
<dependency>
  <groupId>com.cedarsoftware</groupId>
  <artifactId>n-cube</artifactId>
  <version>2.7.5</version>
</dependency>
```
<a class="coinbase-button" data-code="1eb8ea37a2609606bb825ab2d4d3692f" data-button-style="custom_small" data-custom="NCUBE" href="https://coinbase.com/checkouts/1eb8ea37a2609606bb825ab2d4d3692f">Purchase Life-time License</a><script src="https://coinbase.com/assets/button.js" type="text/javascript"></script>

What are the components of an n-cube?
An n-cube has a set of axes (plural of axis), each of which adds a dimension.  Each axis can contain a different number of elements.  These elements are called columns.

A column can be a simple data type like a String, Number, Date, but it can also represent a Range [low, hi) as well as a Set (a combination of discrete values and ranges).  It can also contain any class that implements Java's Comparable interface.

Finally, there are cells.  In a spreadsheet, you have a row and column, like B25 to represent a cell.  In n-cube, a cell is represented by a coordinate.  A Java (or Groovy) Map is used, where the key is the name of an axis, and the value is the value that will 'bind' or match a column on the axis.  If a value does not match any column on an axis, you have the option of adding a 'default' column to the axis.  An example [age:24, state:'CA', date:'2012/12/17'].  The format given here for a Map is the declarative form used by Groovy.  In Java, that would be map.put("age", 24), map.put("state", "CA") and so on.  Because the Groovy form is much shorter, it will be used from here on out to represent coordinate maps.  Because a Map is used as the input coordinate, you can have as many dimensions (keys) as necessary to access the hyperspace (n-cube).

Assuming an n-cube is set up, and a coordinate is also set up (Map coord = [age:24, state:'CA'], the most basic API to access it is ncube.getCell(coord).  The return value will be the value at the given coordinate.  

### Rule Engine
When used as a rule engine, at least one axis within the n-cube is marked as as 'Rule' axis type.  In that case, each column is written as condition (in Groovy).  For exampple, "input.age < 18".  When ncube.getCells(coord, output) is called, the conditions along the Rule axis are executed linearly, in order.  Each condition that evaluates to not-null and not false, will be considered true, and the corresponding cell will be executed.

The input coordinate map is referenced through the variable 'input'.  The output map is referenced through the variable 'output'.  Both can be referenced in the condition as well as in the cell (for expression, method, and template cells).  Typically, when used in rule mode, as conditions fire, the corresponding cell that is executed writes something to the output map.  For example in an pricing application, 'state =='CA' || state == 'TX' as the condition, the corresponding cell may have "output.productCost *= 1.07".  The tax condition, for example.

The condition column can contain multiple statements.  Think of it like a method body.  The value of the last statement executed is evaluated as the condition. Your code has access to the input coordinate (map), output map, ncube, ncubeMgr, and execution stack (these are the variable names).  All Java code libraries and Groovy can be accessed as well.  For example, println from Groovy can be added to the conditions for debugging (as well as added to the executed cell).  The Groovy expression (or methods) in the executed cell can write multiple outputs to the output map.

As each condition on the Rule axis is executed, the n-cube rule engine writes information to a "_rule" entry into the output map.  This _rule entry is a Map which includes the condition number executed, the condition expression executed, and other useful information.  This allows you to evaluate the rule exection while developing the rules, to see rules fired.

In general, as cells execute, they write to the output map.  The input coordinate could be written to as well.  If it is modified, and a further n-cube is referenced, any modifications to the input coordinate will remain in place until that execution path returns.  When the execution path of the rules finishes and returns, the input map is restored to it's prior condition before execution. When returning then to an outer n-cube (or the code that called ncube.getCells()), that code will see no changes to the input map.  The output map will, of course, contain whatever changes were written to it.  

The return value of getCells() also returns the value of each executed cell in the return Map.  This can be useful for debugging, however, the general intention is for the rule body to make modifications to the output map, which the caller can then use.

Both condition columns and executed cells can tell the rule engine to restart execution of the conditions as well as to terminate any further conditions from being executed.  This is a linear rules execution flow, and intentionally not the RETE algorithm.

### Decision Table
When using n-cube as a decision table, each axis represents a decision variable.  For example, a state axis with all of the states of a country.  When accessed, the input coordinate would have the 'state' variable as a key in the input map, and the associated value to state would be a state, for example, 'KS' (Kansas).  If the data changes over time, it is common to add a 'date' access.  Meaning that at one point in time, say for Kansas, the value 10 was returned, but within a different time frame, perhaps 11 is returned.

Common decision variables are country, state / providence, date, business unit, business codes, user role, actions, resources, and so no.  There is no limit to the number of axes that an n-cube can have (other than memory).

Decision tables are great and work best when all variable combinations make sense (a * b * c ... * n).  If the problem space has some combinations that do not make sense, then you may want to use n-cube's Decision Tree capability.  n-cube allows you to combine decision tables, decision trees, and so on, ad infinitum.

### Decision Tree
A good example for a decision tree, is modeling the continents and countries of the world.  Not all continents have the same countries.  Therefore, it would not make sense to have an n-cube with one axis as 'continents' and another axis as 'countries.'  Instead, the initial (entry or outer n-cube) 'world' would have an axis 'continents', with columns Africa, Antarctica, Asia, Australia, Europe, North America, South America.  For each continent column, it's corresponding cell is a reference to a 'country' n-cube for that continent.  The cell reference is written like this: @NorthAmericaCountries[:] for example.  When this cell is executed, it in turn calls the 'NorthAmericaCountries' n-cube with the same input as was passed to the original ncube.  The [ : ] means that no modifications are being made to the input.  Additional inputs could be added here, as well as existing inputs could be changed before accessing the joined n-cube.

In the 'NorthAmericaCountries' n-cube, the cells would return a value (or if a subdivision of the countries is needed like 'States', the cells would join to yet further n-cubes modeling those subdivisions).  In order to 'talk to' or 'use' this n-cube decision tree, the code would look like this: Map coord = [Continent:'NA', Country:'USA', State:'OH'] for example.  This would hit the North America column in the world n-cube, that cell would call the NorthAmericaCountries n-cube, which would then join to the 'UsaStates' ncube.  To reach a Canadian province, for example, the input coordinate would look like this: Map coord = [Continent:'NA', Country:'Canada', Province:'Quebec'].  Notice that the 3rd parameter to the input is not state but province.  Both inputs work, because at each decision level, the appropriate n-cubes join to each other.

At each n-cube along the decision path, it could have additional 'scope' or dimensionality.  For example, a product axis may exist as a second axis on the cubes (or some of the cubes).  Think of a decision tree as stitching together multiple decision tables.  The cells are whatever you need them to be (Strings, numbers, Java objects, Groovy code to executed, etc.) In the case of code, think of your execution path of your program as going through a 'scope router' or 'scope filter' before the appropriate code is selected and executed.

### Template Engine
n-cube can be used to return templates (think of them as HTML pages, for example).  When a template cell is executed, variables within the template are replaced (think mail merge).  If you have used the Apache project's Velocity project, Groovy templates, or have written JSP / ASP files, then you already have an idea on how to use templates.

Snippets written like this <%  code or variable references   %> or ${code / variable references} can be added to the template.  Before the template is returned (think HTML page), these variable sections are executed.  These sections can reference n-cubes, for example, to get language specific, region specific, mobile / non-mobile, browser specific, and so on, to then fill-in a variable portion of the page.

Instead of actually storing the HTML, Groovy Code, etc. directly in an n-cube cell, the content can be referenced via a url.  This allows the HTML page to be stored on a CDN (Content Delivery Network), and then selectively retrieved (per language, state, business unit, date, etc.) and then substitutions within the page made as well (if needed, using the templating mechanism).  Image files can be referenced this way as well, allowing different images to be retrieved depending on state, date, language, product, and so on.

### Creating n-cubes
Use either the Simple JSON format to create n-cubes, or the nCubeEditor to editing the pages.  At the moment, there is no cloud-based editor for n-cube, so you need to set up the nCubeEditor as a web-app within a Java container like tomcat or jetty.  See the sample .json files in the test / resources directories for examples.

These are read in using the NCubeManager.getNCubeFromResource() API.  You can also call ncube.fromSimpleJson(String json).

#### Licensing
n-cube can be used free for personal use.

Version History
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
