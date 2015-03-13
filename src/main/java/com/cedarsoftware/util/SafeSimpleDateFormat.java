package com.cedarsoftware.util;

import java.text.DateFormatSymbols;
import java.text.FieldPosition;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
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
 * @author John DeRegnaucourt (john@cedarsoftware.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class SafeSimpleDateFormat extends Format
{
    private final String _format;
    private static final ThreadLocal<Map<String, SimpleDateFormat>> _dateFormats = new ThreadLocal<Map<String, SimpleDateFormat>>()
    {
        public Map<String, SimpleDateFormat> initialValue()
        {
            return new ConcurrentHashMap<>();
        }
    };

    public static SimpleDateFormat getDateFormat(String format)
    {
        Map<String, SimpleDateFormat> formatters = _dateFormats.get();
        SimpleDateFormat formatter = formatters.get(format);
        if (formatter == null)
        {
            formatter = new SimpleDateFormat(format);
            formatters.put(format, formatter);
        }
        return formatter;
    }

    public SafeSimpleDateFormat(String format)
    {
        _format = format;
    }

    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos)
    {
        return getDateFormat(_format).format(obj, toAppendTo, pos);
    }

    public Object parseObject(String source, ParsePosition pos)
    {
        return getDateFormat(_format).parse(source, pos);
    }

    public Date parse(String day) throws ParseException
    {
        return getDateFormat(_format).parse(day);
    }

    public void setTimeZone(TimeZone tz)
    {
        getDateFormat(_format).setTimeZone(tz);
    }

    public void setCalendar(Calendar cal)
    {
        getDateFormat(_format).setCalendar(cal);
    }

    public void setNumberFormat(NumberFormat format)
    {
        getDateFormat(_format).setNumberFormat(format);
    }

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
}