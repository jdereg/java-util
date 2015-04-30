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
* Cell Prototype
 * Allow specification of the class that expression "exp" cells inherit from.
 * For cube BAR in app FOO, the prototype could be specified as BAR.prototype - meaning that there is a prototype specific to the cube, -or-
   FOO.prototype meaning that this is the prototype for all expression cells in the app 'FOO' 


### n-cube editor (NCE)
// TODO: Where does cell menu go? (for cut/copy/paste)
// TODO: Implement optional keys (with minus sign support)
// TODO: test all regex's related to finding referenced cubes
// TODO: test CellInfo (in preparation for list, array, set, map)
// TODO: create visualization of n-cube in NCE

// TODO: Show subclasses of RpmClasses
