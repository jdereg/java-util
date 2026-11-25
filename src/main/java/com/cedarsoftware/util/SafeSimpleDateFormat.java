package com.cedarsoftware.util;

import java.text.AttributedCharacterIterator;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A {@link SimpleDateFormat} that threads can share.
 *
 * <p>{@code new SafeSimpleDateFormat(pattern)} formats and parses as {@code new SimpleDateFormat(pattern)} does: the
 * same text, the same instants, for the default locale ({@link Locale.Category#FORMAT}) and time zone in force when it
 * is constructed, and a pattern that is not one is refused by the constructor. The setters {@link DateFormat} has
 * ({@code setTimeZone}, {@code setLenient}, {@code setCalendar}, {@code setNumberFormat}) and the two it borrows from
 * {@code SimpleDateFormat} ({@link #setDateFormatSymbols} and {@link #set2DigitYearStart}) mean what they mean
 * there.</p>
 *
 * <p>How: the configuration is one immutable value. A setter replaces it (copy-on-write), and each thread keeps a
 * {@code SimpleDateFormat} of its own for each of the last 16 configurations it used, built on first use.
 * Formatters with the same configuration share that one per thread. There are no locks on the way to a format or a
 * parse.</p>
 *
 * <p>Where it differs from a {@code SimpleDateFormat}, it is because the object is shared:</p>
 * <ul>
 *   <li>A setter takes a copy of what it is given, and a getter returns a copy. Changing a {@code Calendar},
 *       {@code NumberFormat}, {@code TimeZone} or {@code DateFormatSymbols} after handing it over, or changing
 *       what {@link #getCalendar()}, {@link #getNumberFormat()} or {@link #getTimeZone()} returned, changes nothing
 *       here. Call the setter.</li>
 *   <li>A setter reconfigures the formatter for every thread that uses it. A thread that needs its own time zone
 *       needs its own instance; constructing one is cheap.</li>
 *   <li>{@code parse} leaves the time zone alone. A {@code SimpleDateFormat} that reads a zone name ("PST")
 *       switches to that zone for whatever it formats next, and leaves the caller to set it back; here that would
 *       change one thread's results and not another's.</li>
 *   <li>A two-digit year is read against the same hundred years on every thread: those that begin 80 years before
 *       this class was first used, where a {@code SimpleDateFormat} counts from its own construction.
 *       {@link #set2DigitYearStart} chooses others.</li>
 * </ul>
 *
 * <p>What a thread keeps is made of JDK classes only (a {@code LinkedHashMap} of {@code SimpleDateFormat}s), so a
 * pool thread does not hold on to the class loader of an application that has been taken down.</p>
 *
 * <p>The static {@link #getDateFormat(String)} is the earlier way in: it returns a plain {@code SimpleDateFormat}
 * that belongs to the calling thread.</p>
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
public class SafeSimpleDateFormat extends DateFormat {
    private static final long serialVersionUID = 1L;

    // -------------------- What a thread keeps --------------------

    /**
     * A thread's formatters in order of use: a plain LinkedHashMap, with JDK classes for keys and values. A pool
     * thread outlives the applications it serves, and whatever its thread-locals reach stays in memory with it.
     * From 4.1.0 through 4.118.0 the map was a subclass declared here with this class's State for a key, so one
     * format on a pool thread kept the class loader of a discarded application, and every class it had loaded,
     * for as long as the thread lived.
     */
    private static <K> Map<K, SimpleDateFormat> formattersOfThisThread(ThreadLocal<Map<K, SimpleDateFormat>> local) {
        Map<K, SimpleDateFormat> formatters = local.get();
        if (formatters == null) {
            formatters = new LinkedHashMap<>(32, 0.75f, true);
            local.set(formatters);
        }
        return formatters;
    }

    /**
     * Adds one, and drops the one used longest ago when there are more than {@code capacity}.
     *
     * @return the entry dropped, or null
     */
    private static <K> Map.Entry<K, SimpleDateFormat> keep(Map<K, SimpleDateFormat> formatters, K key,
                                                           SimpleDateFormat sdf, int capacity) {
        formatters.put(key, sdf);
        if (formatters.size() <= capacity) {
            return null;
        }
        Iterator<Map.Entry<K, SimpleDateFormat>> entries = formatters.entrySet().iterator();
        Map.Entry<K, SimpleDateFormat> eldest = new AbstractMap.SimpleImmutableEntry<>(entries.next());
        entries.remove();
        return eldest;
    }

    /**
     * A two-digit year means the same year on every thread. A SimpleDateFormat reads one against the hundred years
     * that begin 80 years before the moment it was built, and the formatters here are built one per thread,
     * whenever a thread first needs one (and again if the thread drops it): "11/25/46" was 1946 on one thread and
     * 2046 on the next. Every one counts from the same moment instead, as the JDK counts, by its own calendar.
     */
    private static void countYearsFromOneMoment(SimpleDateFormat sdf) {
        Calendar cal = sdf.getCalendar();
        cal.setTimeInMillis(LOADED_AT);
        cal.add(Calendar.YEAR, -80);
        sdf.set2DigitYearStart(cal.getTime());
    }

    // -------------------- Static legacy accessor (pattern-only) --------------------

    // Per thread: the formatters of the last 16 patterns asked for, and beside them every formatter the caller was
    // found to have changed, which is not this class's to throw away.
    private static final int STATIC_PER_THREAD_LRU_CAPACITY = 16;
    private static final ThreadLocal<Map<String, SimpleDateFormat>> STATIC_TL = new ThreadLocal<>();
    private static final ThreadLocal<Map<String, SimpleDateFormat>> STATIC_CHANGED = new ThreadLocal<>();

    /**
     * Legacy static accessor preserved for compatibility.
     * Returns the calling thread's {@code new SimpleDateFormat(pattern)}, built the first time the thread asks for
     * the pattern. (Its two-digit years count from when this class was first used, as the instances' do, so that
     * every thread's formatter reads them alike.)
     * <p><b>Note:</b> Mutating the returned {@code SimpleDateFormat} (e.g., setTimeZone) affects later uses of
     * this pattern in the same thread, just like the original implementation: a formatter that was changed is
     * kept for as long as the thread lives, or until {@link #clearStaticThreadLocalCache()}. One that was not
     * changed is kept while its pattern is among the last 16 the thread asked for, and built again after that; it
     * has the default locale and time zone of the moment it was built.</p>
     *
     * @throws IllegalArgumentException if the pattern is not one {@code SimpleDateFormat} accepts
     */
    public static SimpleDateFormat getDateFormat(String pattern) {
        Objects.requireNonNull(pattern, "pattern");
        Map<String, SimpleDateFormat> recent = formattersOfThisThread(STATIC_TL);
        SimpleDateFormat sdf = recent.get(pattern);
        if (sdf != null) {
            return sdf;
        }
        Map<String, SimpleDateFormat> changed = STATIC_CHANGED.get();
        sdf = changed == null ? null : changed.get(pattern);
        if (sdf != null) {
            return sdf;
        }
        sdf = asBuilt(pattern);
        Map.Entry<String, SimpleDateFormat> dropped = keep(recent, pattern, sdf, STATIC_PER_THREAD_LRU_CAPACITY);
        if (dropped != null && !isAsBuilt(dropped.getKey(), dropped.getValue())) {
            if (changed == null) {
                changed = new HashMap<>();
                STATIC_CHANGED.set(changed);
            }
            changed.put(dropped.getKey(), dropped.getValue());
        }
        return sdf;
    }

    private static SimpleDateFormat asBuilt(String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        countYearsFromOneMoment(sdf);
        return sdf;
    }

    /**
     * Whether a formatter this thread was handed is still what it was handed: whether one built now is the same.
     * (So one built under a default locale or time zone that has changed since counts as changed, and is kept.)
     */
    private static boolean isAsBuilt(String pattern, SimpleDateFormat sdf) {
        SimpleDateFormat now = asBuilt(pattern);
        return now.equals(sdf)
                && now.get2DigitYearStart().equals(sdf.get2DigitYearStart())
                && State.sameRules(State.rulesOf(now.getCalendar()), State.rulesOf(sdf.getCalendar()));
    }

    /** Clears the static accessor's per-thread cache, the formatters the thread changed included. */
    public static void clearStaticThreadLocalCache() {
        STATIC_TL.remove();
        STATIC_CHANGED.remove();
    }

    // -------------------- Instance-based safe API (copy-on-write) --------------------

    // Per-thread LRU cache: a configuration's id -> SimpleDateFormat. A formatter here is about 1.3 KB, and a miss
    // costs a new SimpleDateFormat, so the bound is for a thread that uses a great many configurations, not a few.
    private static final int PER_THREAD_LRU_CAPACITY = 16;
    private static final ThreadLocal<Map<Long, SimpleDateFormat>> TL = new ThreadLocal<>();

    /**
     * Every configuration in use and the one State that stands for it. Formatters with the same configuration
     * hold the same State, so the same id, so a thread's one SimpleDateFormat serves them all: that is what makes
     * {@code new SafeSimpleDateFormat(pattern).parse(text)} on every call cost less than a new SimpleDateFormat.
     * Emptied when it reaches {@link #MAX_CONFIGURATIONS}, which bounds it for a program that makes patterns
     * without end. A State in use keeps its id; one made afterwards for the same configuration gets another,
     * which costs that configuration a second formatter on a thread and nothing else.
     */
    private static final ConcurrentMap<State, State> CONFIGURATIONS = new ConcurrentHashMap<>();
    private static final int MAX_CONFIGURATIONS = 256;
    private static final AtomicLong IDS = new AtomicLong();

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    /** "Now" for {@link #countYearsFromOneMoment}: one moment for every thread. */
    private static final long LOADED_AT = System.currentTimeMillis();

    /**
     * The class of the {@code FieldPosition} that {@code DateFormat.format(Date)} passes down when its caller
     * wants the text and not where the fields are -- on a JDK where it pays to know. Newer JDKs (25 does it, 21
     * does not) format a date into an unsynchronized buffer when a java.text formatter is asked for a String, and
     * hand a formatter outside java.text, this one, a {@code StringBuffer} that locks on every append: 156 ns
     * against 93 for {@code yyyy-MM-dd HH:mm:ss}. There, {@link #format(Date, StringBuffer, FieldPosition)}
     * asks this thread's SimpleDateFormat for the String and appends it once. Null on a JDK without that buffer,
     * where the same detour costs 9 to 17%; and null if either name is ever not found, which leaves the
     * ordinary path.
     */
    private static final Class<?> NO_POSITION = positionNobodyReads();

    private static Class<?> positionNobodyReads() {
        try {
            Class.forName("java.text.StringBufFactory");
            return Class.forName("java.text.DontCareFieldPosition");
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * One configuration, never changed once made. {@code symbols}, {@code nf} and {@code calendar} are null until
     * their setter is called: null is "what {@code new SimpleDateFormat(pattern, locale)} has of its own", which is
     * how a formatter nobody configured comes out exactly as the JDK's, and why comparing two of them looks at
     * nothing but the pattern, the locale and the zone. (Holding the locale's {@code DateFormatSymbols} here, as
     * 4.1.0 through 4.118.0 did, made a {@code SimpleDateFormat} take its era and month names from them rather
     * than from its calendar, and loaded the names of every time zone to hash them.)
     * <p>
     * Serializable because DateFormat is. A stream written by 4.118.0 or earlier reads as a State whose symbols
     * and number format were set, which is what it was. One written by this version does not read there: those
     * versions cannot do without the symbols, the number format and a summary of it, and writing them for their
     * sake would put the name of every time zone (45 KB) back into every stream.
     */
    private static final class State implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        final String pattern;
        final Locale locale;
        final TimeZone tz;                      // a copy of the caller's
        final boolean lenient;
        final DateFormatSymbols symbols;        // null, or a copy of what setDateFormatSymbols() was given
        final NumberFormat nf;                  // null, or a copy of what setNumberFormat() was given
        final Calendar calendar;                // null, or the rules of what setCalendar() was given: see rulesOf()
        final Long twoDigitYearStartEpochMs;    // null until set2DigitYearStart() is called
        final transient Long id = IDS.incrementAndGet();    // what a thread keeps its formatter under
        private transient int hash;             // 0 until asked for

        /** Takes what it is given as its own: the callers below copy what came from outside. */
        State(String pattern,
              Locale locale,
              TimeZone tz,
              boolean lenient,
              DateFormatSymbols symbols,
              NumberFormat nf,
              Calendar calendar,
              Long twoDigitYearStartEpochMs) {
            this.pattern = pattern;
            this.locale = locale;
            this.tz = tz;
            this.lenient = lenient;
            this.symbols = symbols;
            this.nf = nf;
            this.calendar = calendar;
            this.twoDigitYearStartEpochMs = twoDigitYearStartEpochMs;
        }

        /** The State that stands for this configuration: this one, if it is the first with it. */
        State shared() {
            State known = CONFIGURATIONS.get(this);
            if (known == null) {
                if (CONFIGURATIONS.size() >= MAX_CONFIGURATIONS) {
                    CONFIGURATIONS.clear();
                }
                known = CONFIGURATIONS.putIfAbsent(this, this);
            }
            return known == null ? this : known;
        }

        /** A State that was read from a stream has no id: the one in this JVM for its configuration. */
        private Object readResolve() {
            return new State(pattern, locale, tz, lenient, symbols, nf, calendar, twoDigitYearStartEpochMs).shared();
        }

        /**
         * A copy of a calendar reduced to what makes it this calendar and not another: its class and its rules
         * (first day of the week, days in a first week, where the Gregorian calendar starts). The zone and the
         * leniency are the State's own fields, and a formatter sets the time itself, so all three are made the
         * same in every copy, which lets {@code Calendar.equals()} compare the rest.
         */
        static Calendar rulesOf(Calendar cal) {
            Calendar rules = (Calendar) cal.clone();
            rules.setTimeZone(UTC);
            rules.setLenient(true);
            rules.setTimeInMillis(0);
            return rules;
        }

        SimpleDateFormat build() {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern, locale);   // as new SimpleDateFormat(pattern)
            if (symbols != null) {
                sdf.setDateFormatSymbols(symbols);                          // takes a copy
            }
            if (calendar != null) {
                sdf.setCalendar((Calendar) calendar.clone());
            }
            if (nf != null) {
                sdf.setNumberFormat((NumberFormat) nf.clone());
            }
            sdf.setTimeZone(tz);
            sdf.setLenient(lenient);
            if (twoDigitYearStartEpochMs != null) {             // last: it is read with the calendar
                sdf.set2DigitYearStart(new Date(twoDigitYearStartEpochMs));
            } else {
                countYearsFromOneMoment(sdf);
            }
            return sdf;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof State)) return false;
            State s = (State) o;
            return lenient == s.lenient
                    && pattern.equals(s.pattern)
                    && locale.equals(s.locale)
                    && sameZone(tz, s.tz)
                    && Objects.equals(twoDigitYearStartEpochMs, s.twoDigitYearStartEpochMs)
                    && Objects.equals(symbols, s.symbols)
                    && Objects.equals(nf, s.nf)
                    && sameRules(calendar, s.calendar);
        }

        @Override
        public int hashCode() {
            int h = hash;
            if (h == 0) {
                h = 31 * (31 * pattern.hashCode() + locale.hashCode()) + tz.getID().hashCode();
                h = lenient ? h : ~h;
                hash = h;
            }
            return h;
        }

        /** The same id is not the same zone: two SimpleTimeZones can share one. */
        private static boolean sameZone(TimeZone a, TimeZone b) {
            return a.getID().equals(b.getID()) && a.hasSameRules(b);
        }

        /** Calendar.equals() does not look at the class: a BuddhistCalendar is a GregorianCalendar to it. */
        private static boolean sameRules(Calendar a, Calendar b) {
            if (a == null || b == null) {
                return a == b;
            }
            return a.getClass() == b.getClass() && a.equals(b);
        }
    }

    // Instance state (copy-on-write). Non-final only so clone() can detach the copy —
    // assigned in the constructor and in clone(), never reassigned afterward.
    private AtomicReference<State> stateRef;

    /**
     * @param format a {@link SimpleDateFormat} pattern
     * @throws NullPointerException     if the pattern is null
     * @throws IllegalArgumentException if the pattern is not one {@code SimpleDateFormat} accepts
     */
    public SafeSimpleDateFormat(String format) {
        this.stateRef = new AtomicReference<>(
                new State(Objects.requireNonNull(format, "pattern"),
                        Locale.getDefault(Locale.Category.FORMAT),
                        TimeZone.getDefault(),          // a copy already
                        /* lenient */ true,
                        /* symbols */ null,
                        /* numberFormat */ null,
                        /* calendar */ null,
                        /* twoDigitYearStart */ null).shared()
        );
        // This thread's formatter, now: it is where a pattern that is not one is refused, as the JDK refuses it in
        // its constructor and not at the first format.
        SimpleDateFormat sdf = getSdf();
        // DateFormat's own two fields. DateFormat.clone() and its serialized form need them filled; nothing here
        // reads them again, the getters below answer from the configuration.
        this.calendar = (Calendar) sdf.getCalendar().clone();
        this.numberFormat = (NumberFormat) sdf.getNumberFormat().clone();
    }

    private SimpleDateFormat getSdf() {
        return formatterOf(stateRef.get());
    }

    private static SimpleDateFormat formatterOf(State st) {
        Map<Long, SimpleDateFormat> formatters = formattersOfThisThread(TL);
        SimpleDateFormat sdf = formatters.get(st.id);
        if (sdf == null) {
            sdf = st.build();
            keep(formatters, st.id, sdf, PER_THREAD_LRU_CAPACITY);
        }
        return sdf;
    }

    // ----- Public API (unchanged signatures) -----

    @Override
    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
        SimpleDateFormat sdf = getSdf();
        if (fieldPosition.getClass() == NO_POSITION) {
            return toAppendTo.append(sdf.format(date));     // see NO_POSITION
        }
        return sdf.format(date, toAppendTo, fieldPosition);
    }

    /** As {@link SimpleDateFormat#formatToCharacterIterator(Object)}: the text with its fields marked. */
    @Override
    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        return getSdf().formatToCharacterIterator(obj);
    }

    /**
     * As {@link SimpleDateFormat#parse(String, ParsePosition)}, except that the formatter's time zone is what it
     * was afterwards. A SimpleDateFormat that reads a zone name in the text ("PST", "UTC") becomes a formatter for
     * that zone; here it would have become one on the parsing thread only, for every formatter with this
     * configuration there.
     */
    @Override
    public Date parse(String source, ParsePosition pos) {
        State st = stateRef.get();
        SimpleDateFormat sdf = formatterOf(st);
        Date date = sdf.parse(source, pos);
        if (sdf.getTimeZone() != st.tz) {
            sdf.setTimeZone(st.tz);
        }
        return date;
    }

    @Override
    public void setTimeZone(TimeZone tz) {
        final TimeZone zone = (TimeZone) Objects.requireNonNull(tz, "tz").clone();
        update(s -> new State(s.pattern, s.locale, zone,
                s.lenient, s.symbols, s.nf, s.calendar, s.twoDigitYearStartEpochMs));
    }

    /** A copy of the time zone in use. */
    @Override
    public TimeZone getTimeZone() {
        return (TimeZone) stateRef.get().tz.clone();
    }

    @Override
    public void setLenient(boolean lenient) {
        update(s -> new State(s.pattern, s.locale, s.tz,
                lenient, s.symbols, s.nf, s.calendar, s.twoDigitYearStartEpochMs));
    }

    @Override
    public boolean isLenient() {
        return stateRef.get().lenient;
    }

    /**
     * Uses the given calendar's kind and rules (first day of the week, days in a first week, where the Gregorian
     * calendar starts), its time zone and its leniency, as {@link SimpleDateFormat#setCalendar} does. A copy is
     * taken: changing the calendar afterwards changes nothing here.
     */
    @Override
    public void setCalendar(Calendar cal) {
        Objects.requireNonNull(cal, "cal");
        final TimeZone zone = (TimeZone) cal.getTimeZone().clone();
        final boolean len = cal.isLenient();
        final Calendar rules = State.rulesOf(cal);
        update(s -> new State(s.pattern, s.locale, zone,
                len, s.symbols, s.nf, rules, s.twoDigitYearStartEpochMs));
    }

    /** A copy of the calendar in use: its kind, rules, time zone and leniency. Its time is no particular time. */
    @Override
    public Calendar getCalendar() {
        return (Calendar) getSdf().getCalendar().clone();
    }

    @Override
    public void setNumberFormat(NumberFormat format) {
        final NumberFormat copy = (NumberFormat) Objects.requireNonNull(format, "format").clone();
        update(s -> new State(s.pattern, s.locale, s.tz,
                s.lenient, s.symbols, copy, s.calendar, s.twoDigitYearStartEpochMs));
    }

    /** A copy of the number format in use. */
    @Override
    public NumberFormat getNumberFormat() {
        return (NumberFormat) getSdf().getNumberFormat().clone();
    }

    public void setDateFormatSymbols(DateFormatSymbols symbols) {
        final DateFormatSymbols copy = (DateFormatSymbols) Objects.requireNonNull(symbols, "symbols").clone();
        update(s -> new State(s.pattern, s.locale, s.tz,
                s.lenient, copy, s.nf, s.calendar, s.twoDigitYearStartEpochMs));
    }

    public void set2DigitYearStart(Date date) {
        Objects.requireNonNull(date, "date");
        final long epochMs = date.getTime();
        update(s -> new State(s.pattern, s.locale, s.tz,
                s.lenient, s.symbols, s.nf, s.calendar, epochMs));
    }

    /**
     * Returns a copy of this formatter whose configuration is fully detached from this
     * instance. Without this override, {@code DateFormat.clone()}'s shallow copy would share
     * the {@code AtomicReference} holding the configuration state — mutating the clone
     * (e.g. {@code setTimeZone}) would silently reconfigure the original, and vice versa.
     */
    @Override
    public Object clone() {
        SafeSimpleDateFormat copy = (SafeSimpleDateFormat) super.clone();
        copy.stateRef = new AtomicReference<>(stateRef.get());
        return copy;
    }

    @Override
    public String toString() {
        return stateRef.get().pattern;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof SafeSimpleDateFormat)) return false;
        SafeSimpleDateFormat that = (SafeSimpleDateFormat) other;
        return this.stateRef.get().equals(that.stateRef.get());
    }

    @Override
    public int hashCode() {
        return stateRef.get().hashCode();
    }

    /**
     * Copy-on-write updater. Replaces the current State with a new one; each thread builds its formatter for the
     * new State the next time it formats or parses. The formatter for the State that was replaced stays in this
     * thread's cache until newer ones push it out, so a caller that goes back and forth between two settings
     * builds each once.
     * Uses compareAndSet for thread-safety under concurrent mutations.
     */
    private void update(java.util.function.UnaryOperator<State> fn) {
        State oldSt;
        State newSt;
        do {
            oldSt = stateRef.get();
            newSt = fn.apply(oldSt).shared();
            if (oldSt.equals(newSt)) {
                return;
            }
        } while (!stateRef.compareAndSet(oldSt, newSt));
    }

    /** Clears all cached formatters for the current thread (instance-based cache). */
    public static void clearThreadLocalCache() {
        TL.remove();
    }

    /** Clears this instance’s cached formatter for the current thread. */
    public void clearThreadLocal() {
        Map<Long, SimpleDateFormat> formatters = TL.get();
        if (formatters != null) {
            formatters.remove(stateRef.get().id);
        }
    }
}
