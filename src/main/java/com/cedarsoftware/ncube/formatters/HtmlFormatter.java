package com.cedarsoftware.ncube.formatters;

import com.cedarsoftware.ncube.Axis;
import com.cedarsoftware.ncube.Column;
import com.cedarsoftware.ncube.CommandCell;
import com.cedarsoftware.ncube.NCube;
import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Format an NCube into an HTML document
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License");
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
public class HtmlFormatter extends NCubeFormatter
{
    public HtmlFormatter(NCube ncube)
    {
        super(ncube);
        this.ncube = ncube;
    }

    /**
     * Use this API to generate an HTML view of this NCube.
     * @param headers String list of axis names to place at top.  If more than one is listed, the first axis encountered that
     * matches one of the passed in headers, will be the axis chosen to be displayed at the top.
     * @return String containing an HTML view of this NCube.
     */
    public String format(String ... headers)
    {
        if (ncube.getAxes().size() < 1)
        {
            return "<!DOCTYPE html>\n" +
                    "<html lang=\"en\">\n" +
                    "  <head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <title>Empty NCube</title>\n" +
                    "  </head>\n" +
                    "  <body/>\n" +
                    "</html>";
        }

        String html = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                " <meta charset=\"UTF-8\">\n" +
                " <title>NCube: " + ncube.getName() + "</title>\n" +
                " <style>\n" +
                "table\n" +
                "{\n" +
                "border-collapse:collapse;\n" +
                "}\n" +
                "table, td, th\n" +
                "{\n" +
                "border:1px solid black;\n" +
                "font-family: \"arial\",\"helvetica\", sans-serif;\n" +
                "font-size: small;\n" +
                "font-weight: 500;\n" +
                "padding: 2px;\n" +
                "}\n" +
                "td\n" +
                "{\n" +
                "color: black;\n" +
                "background: white;\n" +
                "text-align: center;\n" +
                "}\n" +
                "th\n" +
                "{\n" +
                "color: white;\n" +
                "}\n" +
                "td:hover { background: #E0F0FF }\n" +
                "th:hover { background: #A2A2A2 }\n" +
                ".ncube-num\n" +
                "{\n" +
                "text-align: right;\n" +
                "}\n" +
                ".ncube-dead\n" +
                "{\n" +
                "background: #6495ED;\n" +
                "} \n" +
                ".ncube-head\n" +
                "{\n" +
                "background: #4D4D4D;\n" +
                "}  \n" +
                ".ncube-col\n" +
                "{\n" +
                "background: #929292;\n" +
                "}\n" +
                " </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<table border=\"1\">\n" +
                "<tr>\n";

        StringBuilder s = new StringBuilder();
        Object[] displayValues = getDisplayValues(headers);
        List<Axis> axes = (List<Axis>) displayValues[0];
        long height = (Long) displayValues[1];
        long width = (Long) displayValues[2];

        s.append(html);

        // Top row (special case)
        Axis topAxis = axes.get(0);
        List<Column> topColumns = topAxis.getColumns();
        final int topColumnSize = topColumns.size();
        final String topAxisName = topAxis.getName();

        if (axes.size() == 1)
        {   // Ensure that one dimension is vertically down the page
            s.append(" <th data-id=\"a").append(topAxis.getId()).append("\" class=\"ncube-head\">");
            s.append("  <div class=\"btn-group axis-menu\" data-id=\"").append(topAxisName).append("\">\n");
            s.append("   <button type=\"button\" class=\"btn-sm btn-primary dropdown-toggle axis-btn\" data-toggle=\"dropdown\">");
            s.append("    <span>").append(topAxisName).append("</span><span class=\"caret\"></span>");
            s.append("   </button></th>\n");
            s.append("  </div>\n");
            s.append(" <th class=\"ncube-dead\"></th>\n");
            s.append("</tr>\n");
            Set<Long> coord = new LinkedHashSet<Long>();

            for (int i=0; i < width; i++)
            {
                s.append("<tr>\n");
                Column column = topColumns.get(i);
                s.append(" <th data-id=\"c").append(column.getId()).append("\" class=\"ncube-col\">");
                s.append(column.isDefault() ? "Default" : column.toString());
                coord.clear();
                coord.add(topColumns.get(i).getId());
                s.append("</th>\n");
                buildCell(s, coord);
                s.append("</tr>\n");
            }
        }
        else
        {   // 2D+ shows as one column on the X axis and all other dimensions on the Y axis.
            int deadCols = axes.size() - 1;
            if (deadCols > 0)
            {
                s.append(" <th class=\"ncube-dead\" colspan=\"").append(deadCols).append("\"></th>\n");
            }
            s.append(" <th data-id=\"a").append(topAxis.getId()).append("\" class=\"ncube-head\" colspan=\"");
            s.append(topAxis.size());
            s.append("\">");
            s.append("  <div class=\"btn-group axis-menu\" data-id=\"").append(topAxisName).append("\">\n");
            s.append("  <button type=\"button\" class=\"btn-sm btn-primary dropdown-toggle axis-btn\" data-toggle=\"dropdown\">");
            s.append("   <span>").append(topAxisName).append("</span><span class=\"caret\"></span>");
            s.append("  </button>\n");
            s.append("   </div>\n");
            s.append(" </th>\n</tr>\n");

            // Second row (special case)
            s.append("<tr>\n");
            Map<String, Long> rowspanCounter = new HashMap<String, Long>();
            Map<String, Long> rowspan = new HashMap<String, Long>();
            Map<String, Long> columnCounter = new HashMap<String, Long>();
            Map<String, List<Column>> columns = new HashMap<String, List<Column>>();
            Map<String, Long> coord = new HashMap<String, Long>();

            final int axisCount = axes.size();

            for (int i=1; i < axisCount; i++)
            {
                Axis axis = axes.get(i);
                String axisName = axis.getName();
                s.append(" <th data-id=\"a").append(axis.getId()).append("\" class=\"ncube-head\">\n");
                s.append("  <div class=\"btn-group axis-menu\" data-id=\"").append(axisName).append("\">\n");
                s.append("   <button type=\"button\" class=\"btn-sm btn-primary dropdown-toggle axis-btn\" data-toggle=\"dropdown\">");
                s.append("    <span>").append(axisName).append("</span><span class=\"caret\"></span>");
                s.append("   </button>\n");
                s.append("   </div>\n");
                s.append(" </th>\n");
                long colspan = 1;

                for (int j=i + 1; j < axisCount; j++)
                {
                    colspan *= axes.get(j).size();
                }

                rowspan.put(axisName, colspan);
                rowspanCounter.put(axisName, 0L);
                columnCounter.put(axisName, 0L);
                columns.put(axisName, axis.getColumns());
            }

            for (Column column : topColumns)
            {
                s.append(" <th data-id=\"c").append(column.getId()).append("\" class=\"ncube-col\">");
                s.append(column.toString());
                s.append("</th>\n");
            }

            if (topAxis.size() != topColumnSize)
            {
                s.append(" <th class=\"ncube-col\">Default</th>");
            }

            s.append("</tr>\n");

            // The left column headers and cells
            for (long h=0; h < height; h++)
            {
                s.append("<tr>\n");
                // Column headers for the row
                for (int i=1; i < axisCount; i++)
                {
                    Axis axis = axes.get(i);
                    String axisName = axis.getName();
                    Long count = rowspanCounter.get(axisName);

                    if (count == 0)
                    {
                        Long colIdx = columnCounter.get(axisName);
                        Column column = columns.get(axisName).get(colIdx.intValue());
                        coord.put(axisName, column.getId());
                        long span = rowspan.get(axisName);

                        if (span == 1)
                        {   // drop rowspan tag since rowspan="1" is redundant and wastes space in HTML
                            // Use column's ID as TH element's ID
                            s.append(" <th data-id=\"c").append(column.getId()).append("\" class=\"ncube-col\">");
                        }
                        else
                        {   // Need to show rowspan attribute
                            // Use column's ID as TH element's ID
                            s.append(" <th data-id=\"c").append(column.getId()).append("\" class=\"ncube-col\" rowspan=\"");
                            s.append(span);
                            s.append("\">");
                        }
                        s.append(column.toString());
                        s.append("</th>\n");

                        // Increment column counter
                        colIdx++;
                        if (colIdx >= axis.size())
                        {
                            colIdx = 0L;
                        }
                        columnCounter.put(axisName, colIdx);
                    }
                    // Increment row counter (counts from 0 to rowspan of subordinate axes)
                    count++;
                    if (count >= rowspan.get(axisName))
                    {
                        count = 0L;
                    }
                    rowspanCounter.put(axisName, count);
                }

                // Cells for the row
                for (int i=0; i < width; i++)
                {
                    coord.put(topAxisName, topColumns.get(i).getId());
                    // Other coordinate values are set above this for-loop
                    buildCell(s, new LinkedHashSet<Long>(coord.values()));
                }

                s.append("</tr>\n");
            }
        }

        s.append("</table>\n");
        s.append("</body>\n");
        s.append("</html>");
        return s.toString();
    }

    private void buildCell(StringBuilder s, Set<Long> coord)
    {
        String id = "k" + setToString(coord);
        s.append(" <td data-id=\"").append(id).append("\" class=\"cell\">");
        if (ncube.containsCellById(coord))
        {
            s.append(getCellValueAsString(ncube.getCellByIdNoExecute(coord)));
        }
        s.append("</td>\n");
    }

    private static String getCellValueAsString(Object cellValue)
    {
        if (cellValue == null)
        {
            return "null";
        }
        boolean isArray = cellValue.getClass().isArray();

        if (cellValue instanceof Date || cellValue instanceof Number || cellValue instanceof String)
        {
            return Column.formatDiscreteValue((Comparable) cellValue);
        }
        else if (cellValue instanceof Boolean || cellValue instanceof Character)
        {
            return String.valueOf(cellValue);
        }
        else if (isArray && JsonReader.isPrimitive(cellValue.getClass().getComponentType()))
        {
            StringBuilder str = new StringBuilder();
            str.append('[');
            final int len = Array.getLength(cellValue);
            final int len1 = len - 1;

            for (int i=0; i < len; i++)
            {
                Object elem = Array.get(cellValue, i);
                str.append(elem.toString());
                if (i < len1)
                {
                    str.append(", ");
                }
            }
            str.append(']');
            return str.toString();
        }
        else if (isArray && Object[].class == cellValue.getClass())
        {
            StringBuilder str = new StringBuilder();
            str.append('[');
            final int len = Array.getLength(cellValue);
            final int len1 = len - 1;

            for (int i=0; i < len; i++)
            {
                Object elem = Array.get(cellValue, i);
                str.append(getCellValueAsString(elem));
                if (i < len1)
                {
                    str.append(", ");
                }
            }
            str.append(']');
            return str.toString();
        }
        else if (cellValue instanceof CommandCell)
        {
            return ((CommandCell) cellValue).getCmd();
        }
        else
        {
            try
            {
                return JsonWriter.objectToJson(cellValue);
            }
            catch (IOException e)
            {
                throw new IllegalStateException("Error with simple JSON format", e);
            }
        }
    }

    private static String setToString(Set<Long> set)
    {
        StringBuilder s = new StringBuilder();
        Iterator<Long> i = set.iterator();

        while (i.hasNext())
        {
            s.append(i.next());
            if (i.hasNext())
            {
                s.append('.');
            }
        }
        return s.toString();
    }
}
