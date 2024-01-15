package com.cedarsoftware.util.convert;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateConversionTests {
    public void testDateToCalendarTimeZone() {
        Date date = new Date();

        System.out.println(date);

        TimeZone timeZone = TimeZone.getTimeZone("America/New_York");
        Calendar cal = Calendar.getInstance(timeZone);
        cal.setTime(date);

        System.out.println(date);
    }
}
