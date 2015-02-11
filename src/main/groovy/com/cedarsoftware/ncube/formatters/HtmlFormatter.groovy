package com.cedarsoftware.ncube.formatters

import com.cedarsoftware.ncube.Axis
import com.cedarsoftware.ncube.AxisType
import com.cedarsoftware.ncube.CellInfo
import com.cedarsoftware.ncube.Column
import com.cedarsoftware.ncube.CommandCell
import com.cedarsoftware.ncube.GroovyBase
import com.cedarsoftware.ncube.NCube
import com.cedarsoftware.ncube.proximity.LatLon
import com.cedarsoftware.ncube.proximity.Point2D
import com.cedarsoftware.ncube.proximity.Point3D
import com.cedarsoftware.util.CaseInsensitiveMap
import com.cedarsoftware.util.StringUtilities
import com.cedarsoftware.util.io.JsonReader
import com.cedarsoftware.util.io.JsonWriter
import groovy.transform.CompileStatic

import java.lang.reflect.Array

import static java.lang.Math.abs

/**
 * Format an NCube into an HTML document
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License")
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
@CompileStatic
public class HtmlFormatter implements NCubeFormatter
{
    String[] _headers

    public HtmlFormatter(String... headers)
    {
        _headers = headers;
    }

    /**
     * Calculate important values needed to display an NCube.
     *
     * @return Object[], where element 0 is a List containing the axes
     * where the first axis (element 0) is the axis to be displayed at the
     * top and the rest are the axes sorted smallest to larges.  Element 1
     * of the returned object array is the height of the cells (how many
     * rows it would take to display the entire ncube). Element 2 is the
     * width of the cell matrix (the number of columns would it take to display
     * the cell portion of the NCube).
     */
    protected Object[] getDisplayValues(NCube ncube)
    {
        if (_headers == null)
        {
            _headers = [] as String[]
        }
        Map<String, Object> headerStrings = new CaseInsensitiveMap()
        for (String header : _headers)
        {
            headerStrings[header] = null
        }
        // Step 1. Sort axes from smallest to largest.
        // Hypercubes look best when the smaller axes are on the inside, and the larger axes are on the outside.
        List<Axis> axes = new ArrayList<>(ncube.axes)
        Collections.sort(axes, new Comparator<Axis>() {
            public int compare(Axis a1, Axis a2)
            {
                a2.size() - a1.size()
            }
        })

        // Step 2.  Now find an axis that is a good candidate for the single (top) axis.  This would be an axis
        // with the number of columns closest to 12.
        int smallestDelta = Integer.MAX_VALUE
        int candidate = -1
        int count = 0

        for (Axis axis : axes)
        {
            if (headerStrings.keySet().contains(axis.name))
            {
                candidate = count
                break
            }
            int delta = abs(axis.size() - 12)
            if (delta < smallestDelta)
            {
                smallestDelta = delta
                candidate = count
            }
            count++
        }

        // Step 3. Compute cell area size
        Axis top = axes.remove(candidate)
        axes.add(0, top)   // Top element is now first.
        top = axes.remove(0)   // Grab 1st (candidate axis) one more time
        if (top.type == AxisType.RULE)
        {   // If top is a rule axis, place it last.  It is recognized that there could
            // be more than one rule axis, and there could also be a single rule axis, in
            // which this is a no-op.
            axes.add(top)
        }
        else
        {
            axes.add(0, top)
        }
        long width = axes[0].size()
        long height = 1;
        final int len = axes.size()

        for (int i = 1; i < len; i++)
        {
            height = axes[i].size() * height
        }

        [axes, height, width] as Object[]
    }

    /**
     * Use this API to generate an HTML view of this NCube.
     * matches one of the passed in headers, will be the axis chosen to be displayed at the top.
     *
     * @return String containing an HTML view of this NCube.
     */
    public String format(NCube ncube)
    {
        if (ncube.axes.size() < 1)
        {
            return getNoAxisHtml()
        }

        String html = getHtmlPreamble()

        StringBuilder s = new StringBuilder()
        Object[] displayValues = getDisplayValues(ncube)
        List<Axis> axes = (List<Axis>) displayValues[0]
        long height = (Long) displayValues[1]
        long width = (Long) displayValues[2]

        s.append(html)

        // Top row (special case)
        Axis topAxis = axes[0]
        List<Column> topColumns = topAxis.columns
        final int topColumnSize = topColumns.size()
        final String topAxisName = topAxis.name

        if (axes.size() == 1)
        {   // Ensure that one dimension is vertically down the page
            s.append(' <th data-id="a').append(topAxis.id).append('" class="th-ncube ncube-head">')
            s.append('  <div class="btn-group axis-menu" data-id="').append(topAxisName).append('">\n')
            s.append('   <button type="button" class="btn-sm btn-primary dropdown-toggle axis-btn" data-toggle="dropdown">')
            s.append('    <span>').append(topAxisName).append('</span><span class="caret"></span>')
            s.append('   </button></th>\n')
            s.append('  </div>\n')
            s.append(' <th class="th-ncube ncube-dead">')
            s.append(ncube.name)
            s.append('</th>\n')
            s.append('</tr>\n')

            for (int i = 0; i < width; i++)
            {
                s.append("<tr>\n")
                Column column = topColumns[i]
                String colId = String.valueOf(column.id)
                s.append(" <th data-id=\"").append(colId)
                s.append('" data-axis="').append(topAxisName).append('" class="th-ncube ')
                s.append(getColumnCssClass(column))
                s.append("\">")
                buildColumnGuts(s, column)
                s.append("</th>\n")
                Set<Long> colIds = new LinkedHashSet<>()
                colIds.add(column.id)
                buildCell(ncube, s, colIds, i % 2 == 0)
                s.append("</tr>\n")
            }
        }
        else
        {   // 2D+ shows as one column on the X axis and all other dimensions on the Y axis.
            int deadCols = axes.size() - 1
            if (deadCols > 0)
            {
                s.append(' <th class="th-ncube ncube-dead" colspan="').append(deadCols).append("\">")
                s.append(ncube.name)
                s.append("</th>\n")
            }
            s.append(' <th data-id="a').append(topAxis.id).append('" class="th-ncube ncube-head" colspan="')
            s.append(topAxis.size())
            s.append('">')
            s.append('  <div class="btn-group axis-menu" data-id="').append(topAxisName).append("\">\n")
            s.append('  <button type="button" class="btn-sm btn-primary dropdown-toggle axis-btn" data-toggle="dropdown">')
            s.append('   <span>').append(topAxisName).append('</span><span class="caret"></span>')
            s.append('  </button>\n')
            s.append('   </div>\n')
            s.append(' </th>\n</tr>\n')

            // Second row (special case)
            s.append("<tr>\n")
            Map<String, Long> rowspanCounter = [:]
            Map<String, Long> rowspan = [:]
            Map<String, Long> columnCounter = [:]
            Map<String, List<Column>> columns = [:]

            final int axisCount = axes.size()

            for (int i = 1; i < axisCount; i++)
            {
                Axis axis = axes[i]
                String axisName = axis.name
                s.append(' <th data-id="a').append(axis.id).append('" class="th-ncube ncube-head">\n')
                s.append('  <div class="btn-group axis-menu" data-id="').append(axisName).append('">\n')
                s.append('   <button type="button" class="btn-sm btn-primary dropdown-toggle axis-btn" data-toggle="dropdown">')
                s.append('    <span>').append(axisName).append('</span><span class="caret"></span>')
                s.append('   </button>\n')
                s.append('   </div>\n')
                s.append(' </th>\n')
                long colspan = 1;

                for (int j = i + 1; j < axisCount; j++)
                {
                    colspan *= axes[j].size()
                }

                rowspan[axisName] = colspan
                rowspanCounter[axisName] = 0L
                columnCounter[axisName] = 0L
                columns[axisName] = axis.columns
            }

            for (Column column : topColumns)
            {
                String colId = String.valueOf(column.id)
                s.append(' <th data-id="').append(colId).append('" data-axis="').append(topAxisName).append('" class="th-ncube-top ')
                s.append(getColumnCssClass(column))
                s.append('">')
                buildColumnGuts(s, column)
                s.append('</th>\n')
            }

            if (topAxis.size() != topColumnSize)
            {
                s.append(' <th class="th-ncube-top ')
                s.append(getColumnCssClass(topAxis.defaultColumn))
                s.append('">Default</th>')
            }

            s.append("</tr>\n")
            Map<String, Long> colIds = [:]

            // The left column headers and cells
            for (long h = 0; h < height; h++)
            {
                s.append("<tr>\n")
                // Column headers for the row
                for (int i = 1; i < axisCount; i++)
                {
                    Axis axis = axes[i]
                    String axisName = axis.name
                    Long count = rowspanCounter[axisName]

                    if (count == 0)
                    {
                        Long colIdx = columnCounter[axisName]
                        Column column = columns[axisName][colIdx.intValue()]
                        colIds[axisName] = column.id
                        long span = rowspan[axisName]
                        String columnId = String.valueOf(column.id)
                        String colCssClass = getColumnCssClass(column)

                        if (span == 1)
                        {   // drop rowspan tag since rowspan="1" is redundant and wastes space in HTML
                            // Use column's ID as TH element's ID
                            s.append(' <th data-id="').append(columnId).append('" data-axis="').append(axisName).append('" class="th-ncube ')
                            s.append(colCssClass)
                        }
                        else
                        {   // Need to show rowspan attribute
                            // Use column's ID as TH element's ID
                            s.append(' <th data-id="').append(columnId).append('" data-axis="').append(axisName).append('" class="th-ncube ')
                            s.append(colCssClass)
                            s.append('" rowspan="')
                            s.append(span)
                        }
                        s.append('">')
                        buildColumnGuts(s, column)
                        s.append('</th>\n')

                        // Increment column counter
                        colIdx++;
                        if (colIdx >= axis.size())
                        {
                            colIdx = 0L
                        }
                        columnCounter[axisName] = colIdx
                    }
                    // Increment row counter (counts from 0 to rowspan of subordinate axes)
                    count++;
                    if (count >= rowspan[axisName])
                    {
                        count = 0L
                    }
                    rowspanCounter[axisName] = count
                }

                // Cells for the row
                for (int i = 0; i < width; i++)
                {
                    Column column = topColumns[i]
                    colIds[topAxisName] = column.id
                    // Other coordinate values are set above this for-loop
                    buildCell(ncube, s, new LinkedHashSet<>(colIds.values()), h % 2 == 0)
                }

                s.append("</tr>\n")
            }
        }

        s.append("</table>\n")
        s.append("</body>\n")
        s.append("</html>")
        s.toString()
    }

    private static void buildColumnGuts(StringBuilder s, Column column)
    {
        final boolean isCmd = column.value instanceof CommandCell;
        final boolean isUrlCmd = isCmd && StringUtilities.hasContent(((CommandCell) column.value).url)
        final boolean isInlineCmd = isCmd && !isUrlCmd;

        if (isInlineCmd)
        {
            s.append('<pre class="ncube-pre">')
        }
        else if (isUrlCmd)
        {
            s.append('<a class="cmd-url" href="#">')
        }
        addColumnPrefixText(s, column)
        s.append(column.default ? "Default" : column.toString())
        if (isInlineCmd)
        {
            s.append("</pre>")
        }
        else if (isUrlCmd)
        {
            s.append("</a>")
        }
    }

    private static String getHtmlPreamble()
    {
        """
<!DOCTYPE html>
<html lang="en">
<head>
 <meta charset="UTF-8">
 <title>NCube: </title>
 <style>
.table-ncube
{
border-collapse:collapse;
border:1px solid lightgray;
font-family: "arial","helvetica", sans-serif;
font-size: small;
padding: 2px;
}
.td-ncube .th-ncube .th-ncube-top
{
border:1px solid lightgray;
font-family: "arial","helvetica", sans-serif;
font-size: small;
padding: 2px;
}
.td-ncube
{
color: black;
background: white;
text-align: center;
}
.th-ncube
{
color: white;
font-weight: normal;
}
.th-ncube-top
{
color: white;
text-align: center;
font-weight: normal;
}
.td-ncube:hover { background: #E0F0FF }
.th-ncube:hover { background: #A2A2A2 }
.th-ncube-top:hover { background: #A2A2A2 }
.ncube-num
{
text-align: right;
}
.ncube-dead
{
background: #6495ED;
}
.ncube-head
{
background: #4D4D4D;
}
.column
{
background: #929292;
}
.column-code
{
vertical-align: top;
}
.column-url
{
color: blue;
text-align: left;
vertical-align: top;
}
.cell
{
color: black;
background: white;
text-align: center;
vertical-align: middle
}
.cell-url
{
color: mediumblue;
background: cornsilk;
text-align: left;
vertical-align: middle
}
.cell-code
{
background: white;
text-align: left;
vertical-align: top
}
.ncube-pre
{
padding: 2px;
margin: 2px;
word-break: normal;
word-wrap: normal;
border: 0;
background: white;
}
.ncube-pre:hover { background: #E0F0FF }
.odd-row {
    background-color: #e0e0e0 !important;
}

.odd-row:hover {
    background-color: #E0F0FF !important;
}

.cell-selected {
    background-color: lightblue !important;
}

.cell-selected:hover {
    background-color: lightskyblue !important;
}

th.ncube-dead:hover {
    background: #76A7FF;
}

.th-ncube a, .th-ncube-top a {
    color: lightskyblue;
}

.th-ncube > a:hover, .th-ncube-top > a:hover {
    color: lightcyan;
}
 </style>
</head>
<body>
<table class="table-ncube" border="1">
<tr>
"""
    }

    private static String getNoAxisHtml()
    {
"""
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8">
    <title>Empty NCube</title>
  </head>
  <body/>
</html>
""";
    }

    private static void addColumnPrefixText(StringBuilder s, Column column)
    {
        if (column.value instanceof CommandCell)
        {
            String name = (String)column.getMetaProperty("name")
            if (StringUtilities.hasContent(name))
            {
                s.append("name: ")
                s.append(name)
                s.append('<hr style="margin:1px"/>')
            }
        }
    }

    private static String getColumnCssClass(Column col)
    {
        if (col.value instanceof CommandCell)
        {
            CommandCell cmd = (CommandCell) col.value
            if (StringUtilities.hasContent(cmd.url))
            {
                return "column column-url";
            }
            else if (cmd instanceof GroovyBase)
            {
                return "column column-code";
            }
        }
        return "column";
    }

    private static void buildCell(NCube ncube, StringBuilder s, Set<Long> coord, boolean odd)
    {
        String oddRow = odd ? '' : 'odd-row '
        String id = makeCellId(coord)
        s.append(' <td data-id="').append(id).append('" class="td-ncube ' + oddRow)

        if (ncube.containsCellById(coord))
        {
            Object cell = ncube.getCellByIdNoExecute(coord)
            if (cell instanceof CommandCell)
            {
                CommandCell cmd = (CommandCell) cell;
                if (StringUtilities.hasContent(cmd.url))
                {
                    s.append('cell cell-url"><a class="cmd-url" href="#">')
                    s.append(cmd.url)
                    s.append("</a>")
                }
                else if (cmd instanceof GroovyBase)
                {
                    s.append('cell cell-code"><pre class="' + oddRow + 'ncube-pre">')
                    s.append(getCellValueAsString(cell))
                    s.append("</pre>")
                }
                else
                {
                    s.append('cell">')
                    s.append(getCellValueAsString(cell))
                }
            }
            else
            {
                s.append('cell">')
                s.append(getCellValueAsString(cell))
            }
        }
        else
        {
            s.append('cell">')
        }
        s.append('</td>\n')
    }

    static String getCellValueAsString(Object cellValue)
    {
        if (cellValue == null)
        {
            return "null";
        }
        boolean isArray = cellValue.getClass().array

        if (cellValue instanceof Date || cellValue instanceof String)
        {
            return CellInfo.formatForDisplay((Comparable) cellValue)
        }
        else if (cellValue instanceof Boolean || cellValue instanceof Character)
        {
            return String.valueOf(cellValue)
        }
        else if (cellValue instanceof Point2D || cellValue instanceof Point3D || cellValue instanceof LatLon)
        {
            return cellValue.toString()
        }
        else if (cellValue instanceof Number)
        {
            return CellInfo.formatForDisplay((Comparable) cellValue)
        }
        else if (cellValue instanceof byte[])
        {
            return StringUtilities.encode((byte[]) cellValue)
        }
        else if (isArray && JsonReader.isPrimitive(cellValue.getClass().componentType))
        {
            StringBuilder str = new StringBuilder()
            str.append('[')
            final int len = Array.getLength(cellValue)
            final int len1 = len - 1;

            for (int i = 0; i < len; i++)
            {
                Object elem = Array.get(cellValue, i)
                str.append(elem.toString())
                if (i < len1)
                {
                    str.append(", ")
                }
            }
            str.append(']')
            return str.toString()
        }
        else if (isArray && ([] as Object[]).class.equals(cellValue.getClass()))
        {
            StringBuilder str = new StringBuilder()
            str.append('[')
            final int len = Array.getLength(cellValue)
            final int len1 = len - 1;

            for (int i = 0; i < len; i++)
            {
                Object elem = Array.get(cellValue, i)
                str.append(getCellValueAsString(elem))
                if (i < len1)
                {
                    str.append(", ")
                }
            }
            str.append(']')
            return str.toString()
        }
        else if (cellValue instanceof CommandCell)
        {
            return ((CommandCell) cellValue).cmd
        }
        else if (cellValue instanceof URLClassLoader)
        {   // Turn URLClassLoader back into the [] of String URL it was built from.
            URLClassLoader urlClassLoader = (URLClassLoader) cellValue;
            StringBuilder s = new StringBuilder()
            s.append('[')
            URL[] urls = urlClassLoader.URLs
            for (int i=0; i < urls.length; i++)
            {
                URL url = urls[i]
                s.append('"')
                s.append(url.toExternalForm())
                s.append('"')
                if (i < urls.length - 1)
                {
                    s.append(',')
                }
            }
            s.append(']')
            return s.toString()

        }
        else
        {
            try
            {
                return JsonWriter.objectToJson(cellValue)
            }
            catch (IOException e)
            {
                throw new IllegalStateException("Error with simple JSON format", e)
            }
        }
    }

    private static String makeCellId(Set<Long> colIds)
    {
        StringBuilder s = new StringBuilder()
        Iterator<Long> i = colIds.iterator()
        while (i.hasNext())
        {
            s.append(i.next())
            if (i.hasNext())
            {
                s.append('_')
            }
        }

        s.toString()
    }
}
