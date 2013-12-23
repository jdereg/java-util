n-cube
======
n-cube is a Rules Engine, Decision Table, Decision Tree, Templating Engine, and Enterprise Spreadsheet, built as a hyper-space.  

What are the components of an n-cube?
An ncube has a set of axes (plural of axis), each of which adds a dimension.  Each axis can contain a different number of elements.  These elements are called columns.

A column contain be a simple data type like a String, Number, Date, but it can also represent a Range [low, hi) as well as a Set (a combination of discrete values and ranges).  

Finally, there are cells.  In a spreadsheet, you have a row and column, like B25 to represent a cell.  In n-cube, a cell is represented by a coordinate.  In Java and Groovy a Map is used, where the key is the name of an axis, and the value is the value that will 'bind' or match a column on the axis.  If a value does not match any column on an axis, you have the option of adding a 'default' column to the axis.  An example [age:24, state:'CA', date:'2012/12/17'].  The format given here for a Map is the declarative form used by Groovy.  In Java, that would be map.put("age", 24), map.put("state", "CA") and so on.  Because the Groovy form is much shorter, it will be used from here on out to represent coordinate maps.  Because a Map is used as the input coordinate, you can have as many dimensions (keys) as necessary to access the hyperspace (n-cube).

Assuming an n-cube is set up, and a coordinate is also set up (Map coord = [age:24, state:'CA'], the most basic API to access it is ncube.getCell(coord).  The return value will be the value at the given coordinate.  


Version History
* 2.0.1
 * 'binary' type added to simple JSON format.  Marks a cell to be returned as byte[].  'value' should be set to hex digits 'CAFEBABE10', or the 'uri' / 'url' should be set to point to location returning binary content.
 * 'cache' flag added to 'string' and 'binary' cells (when specified as 'uri' or 'url').  If not specified, the default is cache=true, meaning that n-cube will fetch the contents from the URL, and then hold onto it.  Set to "cache":false and n-cube will retrieve the content each time the cell is referenced.
* 2.0.0
 * Initial version

By: John DeRegnaucourt