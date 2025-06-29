package com.cedarsoftware.util;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class implements a Thread-Safe (re-entrant) SimpleDateFormat
 * class.  It does this by using a ThreadLocal that holds a Map, instead
 * of the traditional approach to hold the SimpleDateFormat in a ThreadLocal.
 *
 * Each ThreadLocal holds a single HashMap containing SimpleDateFormats, keyed
 * by a String format (e.g. "yyyy/M/d", etc.), for each new SimpleDateFormat
 * instance that was created within the threads execution context.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class SafeSimpleDateFormat extends DateFormat
{
    private final String _format;
    private static final ThreadLocal<Map<String, SimpleDateFormat>> _dateFormats = ThreadLocal.withInitial(ConcurrentHashMap::new);

    public static SimpleDateFormat getDateFormat(String format)
    {
        Map<String, SimpleDateFormat> formatters = _dateFormats.get();
        return formatters.computeIfAbsent(format, SimpleDateFormat::new);
    }

    public SafeSimpleDateFormat(String format)
    {
        _format = format;
        DateFormat dateFormat = getDateFormat(_format);
        // Reset for new instance
        Calendar cal = Calendar.getInstance();
        cal.clear();
        dateFormat.setCalendar(cal);
        dateFormat.setLenient(cal.isLenient());
        dateFormat.setTimeZone(cal.getTimeZone());
        NumberFormat numberFormat = NumberFormat.getNumberInstance();
        numberFormat.setGroupingUsed(false);
        dateFormat.setNumberFormat(numberFormat);
    }

    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition)
    {
        return getDateFormat(_format).format(date, toAppendTo, fieldPosition);
    }
    
    public Date parse(String source, ParsePosition pos)
    {
        return getDateFormat(_format).parse(source, pos);
    }

    @Override
    public void setTimeZone(TimeZone tz)
    {
        getDateFormat(_format).setTimeZone(tz);
    }

    @Override
    public void setCalendar(Calendar cal)
    {
        getDateFormat(_format).setCalendar(cal);
    }

    @Override
    public void setNumberFormat(NumberFormat format)
    {
        getDateFormat(_format).setNumberFormat(format);
    }

    @Override
    public void setLenient(boolean lenient)
    {
        getDateFormat(_format).setLenient(lenient);
    }

    public void setDateFormatSymbols(DateFormatSymbols symbols)
    {
        getDateFormat(_format).setDateFormatSymbols(symbols);
    }

    public void set2DigitYearStart(Date date)
    {
        getDateFormat(_format).set2DigitYearStart(date);
    }

    public String toString() {
        return _format;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof SafeSimpleDateFormat) {
            SafeSimpleDateFormat that = (SafeSimpleDateFormat) other;
            return getDateFormat(_format).equals(getDateFormat(that._format));
        }
        if (other instanceof SimpleDateFormat) {
            return getDateFormat(_format).equals(other);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getDateFormat(_format).hashCode();
    }
}
