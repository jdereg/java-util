n-cube 'ToDo' list
======

### n-cube engine

* Datatypes
 * Additional datatypes: list, map, set, array containing only JSON primitive types
 * DefaultCellValue needs to be declared and processed like the cell type and requiredScopeKeys
* OptionalScope
 * Support additional (and subtractive) optional scope keys (minus sign (-) in front will remove an optional scope key)
 * Test all regular expression patterns to ensure they find cube names
 * Docs on optional scope: mention that optional scope will not be found in URL GroovyExpression cells
* Testing
 * CellInfo

### n-cube editor (NCE)
* Hyperlink all cube names so that they can be single-click and become the active n-cube being edited
* Search / Filter support
  * filter app names (with drop-down type matching)
  * filter cube names (with drop-down type matching)
  * search cubes (including axis names, column, and cell values) for string
  