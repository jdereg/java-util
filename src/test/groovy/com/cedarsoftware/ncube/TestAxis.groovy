package com.cedarsoftware.ncube

import com.cedarsoftware.ncube.exception.AxisOverlapException
import com.cedarsoftware.ncube.exception.CoordinateNotFoundException
import com.cedarsoftware.ncube.proximity.LatLon
import com.cedarsoftware.ncube.proximity.Point3D
import com.cedarsoftware.ncube.util.LongHashSet
import com.cedarsoftware.util.CaseInsensitiveMap
import com.cedarsoftware.util.Converter
import com.cedarsoftware.util.io.JsonWriter
import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotEquals
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail

/**
 * NCube Axis Tests
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the 'License')
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an 'AS IS' BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
class TestAxis
{
    @Before
    public void setUp() throws Exception
    {
        TestingDatabaseHelper.setupDatabase()
    }

    @After
    public void tearDown() throws Exception
    {
        TestingDatabaseHelper.tearDownDatabase()
    }

    private static boolean isValidPoint(Axis axis, Comparable value)
    {
        try
        {
            axis.addColumn value
            return true
        }
        catch (AxisOverlapException e)
        {
            return false
        }
    }

    @Test
    void testAxisNameChange()
    {
        Axis axis = new Axis('foo', AxisType.DISCRETE, AxisValueType.LONG, false)
        axis.name = 'bar'
        assert 'bar' == axis.name
    }

    @Test
    void testRemoveMetaPropertyWhenMetaPropertiesAreNull()
    {
        Axis axis = new Axis('foo', AxisType.DISCRETE, AxisValueType.LONG, false)
        assertNull axis.removeMetaProperty('foo')
    }

    @Test
    void testRemoveMetaProperty()
    {
        Axis axis = new Axis('foo', AxisType.DISCRETE, AxisValueType.LONG, false)

        def map = [foo:'bar','bar':'baz']
        axis.addMetaProperties map

        assert 'bar' == axis.getMetaProperty('foo')
        assert 'bar' == axis.removeMetaProperty('foo')
        assertNull axis.getMetaProperty('foo')
    }

    @Test
    void testClearMetaProperties()
    {
        Axis axis = new Axis('foo', AxisType.DISCRETE, AxisValueType.LONG, false)

        Map map = new HashMap()
        map.put('foo', 'bar')
        map.put('bar', 'baz')
        axis.addMetaProperties(map)

        assert 'bar' == axis.getMetaProperty('foo')
        assert 'baz' == axis.getMetaProperty('bar')

        axis.clearMetaProperties()

        assertNull axis.getMetaProperty('foo')
        assertNull axis.getMetaProperty('bar')
        assertNull axis.removeMetaProperty('foo')
    }

    @Test
    void testGetMetaPropertyWhenMetaPropertiesAreNull()
    {
        Axis axis = new Axis('foo', AxisType.DISCRETE, AxisValueType.LONG, false)
        assertNull axis.getMetaProperty('foo')
    }


    @Test(expected = IllegalArgumentException.class)
    void testConvertStringToColumnValueWithEmptyString()
    {
        Axis axis = new Axis('test axis', AxisType.DISCRETE, AxisValueType.LONG, true)
        axis.convertStringToColumnValue('')
    }

    @Test(expected = IllegalArgumentException.class)
    void testConvertStringToColumnValueWithNull()
    {
        Axis axis = new Axis('test axis', AxisType.DISCRETE, AxisValueType.LONG, true)
        axis.convertStringToColumnValue(null)
    }

    @Test(expected = IllegalArgumentException.class)
    void testConvertStringToColumnValueWihInvalidRangeDefinition()
    {
        Axis axis = new Axis('test axis', AxisType.SET, AxisValueType.LONG, true)
        axis.convertStringToColumnValue('[[5]]')
    }

    @Test(expected = IllegalArgumentException.class)
    void testConvertStringToColumnValueWihRangeException()
    {
        Axis axis = new Axis('test axis', AxisType.SET, AxisValueType.LONG, true)
        axis.convertStringToColumnValue('[null]')
    }

    @Test
    void testStandardizeColumnValueErrorHandling()
    {
        Axis states = NCubeBuilder.statesAxis
        try
        {
            states.standardizeColumnValue(null)
            fail("should not make it here")
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('null')
            assert e.message.toLowerCase().contains('cannot be used')
        }
    }

    @Test
    void testCONDITIONnoSort()
    {
        Axis axis = new Axis('sorted', AxisType.RULE, AxisValueType.EXPRESSION, true, Axis.SORTED)
        assert axis.columnOrder == Axis.DISPLAY

        axis = new Axis('sorted', AxisType.RULE, AxisValueType.BIG_DECIMAL, true, Axis.DISPLAY)
        assert axis.valueType == AxisValueType.EXPRESSION

        axis = new Axis('sorted', AxisType.RULE, AxisValueType.EXPRESSION, false, Axis.DISPLAY)
        try
        {
            axis.addColumn 10
            fail 'should not make it here'
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('rule axis')
            assert e.message.toLowerCase().contains('commandcell')
        }

        axis = new Axis('sorted', AxisType.DISCRETE, AxisValueType.LONG, false, Axis.DISPLAY)
        try
        {
            axis.findColumn null
            fail 'should not make it here'
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('null')
            assert e.message.toLowerCase().contains('sorted')
            assert e.message.toLowerCase().contains('not have')
            assert e.message.toLowerCase().contains('default column')
        }
    }

    @Test
    void testAxisValueOverlap()
    {
        Axis axis = new Axis('test axis', AxisType.DISCRETE, AxisValueType.LONG, true)
        axis.addColumn 0
        axis.addColumn 10
        axis.addColumn 100

        assert isValidPoint(axis, -1)
        assert !isValidPoint(axis, 0)
        assert !isValidPoint(axis, 10)
        assert isValidPoint(axis, 11)
        assert !isValidPoint(axis, 100)
        assert isValidPoint(axis, 101)

        try
        {
            axis.addColumn(new Range(3, 9))
            fail 'should not make it here'
        }
        catch (IllegalArgumentException expected)
        {
            expected.message.toLowerCase().contains("unsupported value type")
        }

        axis = new Axis('test axis', AxisType.DISCRETE, AxisValueType.STRING, true)
        axis.addColumn 'echo'
        axis.addColumn 'juliet'
        axis.addColumn 'tango'

        assert isValidPoint(axis, 'alpha')
        assert !isValidPoint(axis, 'echo')
        assert !isValidPoint(axis, 'juliet')
        assert isValidPoint(axis, 'kilo')
        assert !isValidPoint(axis, 'tango')
        assert isValidPoint(axis, 'uniform')

        try
        {
            axis.addColumn new Range(3, 9)
            fail 'should not make it here'
        }
        catch (IllegalArgumentException expected)
        {
            expected.message.toLowerCase().contains("unsupported value type")
        }
    }

    @Test
    void testRangeOverlap()
    {
        Axis axis = new Axis("Age", AxisType.RANGE, AxisValueType.LONG, true)
        axis.addColumn(new Range(0, 18))
        axis.addColumn(new Range(18, 30))
        axis.addColumn(new Range(65, 80))

        assertFalse(isValidRange(axis, new Range(17, 20)))
        assertFalse(isValidRange(axis, new Range(18, 20)))
        assertTrue(isValidRange(axis, new Range(30, 65)))
        assertFalse(isValidRange(axis, new Range(40, 50)))
        assertTrue(isValidRange(axis, new Range(80, 100)))
        assertFalse(isValidRange(axis, new Range(-150, 150)))
        assertTrue(axis.size() == 6)

        // Edge and Corner cases
        try
        {
            axis.addColumn(17)
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('only add range value')
        }

        try
        {
            axis.addColumn(new Range(-10, -10))
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('low and high must be different')
        }

        // Using Long's as Dates (Longs, Date, or Calendar allowed)
        axis = new Axis("Age", AxisType.RANGE, AxisValueType.DATE, true)
        axis.addColumn(new Range(0L, 18L))
        axis.addColumn(new Range(18L, 30L))
        axis.addColumn(new Range(65L, 80L))

        assertFalse(isValidRange(axis, new Range(17L, 20L)))
        assertFalse(isValidRange(axis, new Range(18L, 20L)))
        assertTrue(isValidRange(axis, new Range(30L, 65L)))
        assertFalse(isValidRange(axis, new Range(40L, 50L)))
        assertTrue(isValidRange(axis, new Range(80L, 100L)))
        assertFalse(isValidRange(axis, new Range(-150L, 150L)))
        assertTrue(axis.size() == 6)

        // Edge and Corner cases
        try
        {
            axis.addColumn(17)
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('only add range value')
        }

        try
        {
            axis.addColumn(new Range(-10L, -10L))
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('low and high must be different')
        }

        axis = new Axis("Age", AxisType.RANGE, AxisValueType.DOUBLE, true)
        axis.addColumn(new Range(0, 18))
        axis.addColumn(new Range(18, 30))
        axis.addColumn(new Range(65, 80))

        assertFalse(isValidRange(axis, new Range(17, 20)))
        assertFalse(isValidRange(axis, new Range(18, 20)))
        assertTrue(isValidRange(axis, new Range(30, 65)))
        assertFalse(isValidRange(axis, new Range(40, 50)))
        assertTrue(isValidRange(axis, new Range(80, 100)))
        assertFalse(isValidRange(axis, new Range(-150, 150)))
        assertTrue(axis.size() == 6)

        // Edge and Corner cases
        try
        {
            axis.addColumn(17)
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('only add range value')
        }

        try
        {
            axis.addColumn(new Range(-10, -10))
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('low and high must be different')
        }

        axis = new Axis("Age", AxisType.RANGE, AxisValueType.BIG_DECIMAL, true)
        axis.addColumn(new Range(0, 18))
        axis.addColumn(new Range(18, 30))
        axis.addColumn(new Range(65, 80))

        assertFalse(isValidRange(axis, new Range(17, 20)))
        assertFalse(isValidRange(axis, new Range(18, 20)))
        assertTrue(isValidRange(axis, new Range(30, 65)))
        assertFalse(isValidRange(axis, new Range(40, 50)))
        assertTrue(isValidRange(axis, new Range(80, 100)))
        assertFalse(isValidRange(axis, new Range(-150, 150)))
        assertTrue(axis.size() == 6)

        // Edge and Corner cases
        try
        {
            axis.addColumn(17)
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('only add range value')
        }

        try
        {
            axis.addColumn(new Range(-10, -10))
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('low and high must be different')
        }
    }

    @Test
    void testAxisInsertAtFront()
    {
        Axis states = new Axis('States', AxisType.SET, AxisValueType.STRING, false, Axis.SORTED)
        RangeSet set = new RangeSet('GA')
        set.add 'OH'
        set.add 'TX'
        states.addColumn set
        set = new RangeSet('AL')
        set.add 'WY'
        states.addColumn set
        assert states.size() == 2
        Column col = states.columns[0]
        assert col.value == set      // added first (because of SORTED)
    }

    @Test
    void testAxisLongType()
    {
        Axis axis = new Axis('foo', AxisType.DISCRETE, AxisValueType.LONG, false, Axis.DISPLAY)
        axis.addColumn 1
        axis.addColumn 2L
        axis.addColumn 3 as Byte
        axis.addColumn 4 as Short
        axis.addColumn '5'
        axis.addColumn new BigDecimal('6')
        axis.addColumn new BigInteger('7')
        assert AxisType.DISCRETE.equals(axis.type)
        assert AxisValueType.LONG.equals(axis.valueType)
        assert axis.size() == 7

        assert axis.columns[0].value instanceof Long
        assert axis.columns[1].value instanceof Long
        assert axis.columns[2].value instanceof Long
        assert axis.columns[3].value instanceof Long
        assert axis.columns[4].value instanceof Long
        assert axis.columns[5].value instanceof Long
        assert axis.columns[6].value instanceof Long

        assert axis.columns[0].value == 1
        assert axis.columns[1].value == 2
        assert axis.columns[2].value == 3
        assert axis.columns[3].value == 4
        assert axis.columns[4].value == 5
        assert axis.columns[5].value == 6
        assert axis.columns[6].value == 7
    }

    @Test
    void testAddingNullToAxis()
    {
        Axis axis = new Axis('foo', AxisType.DISCRETE, AxisValueType.LONG, false)
        axis.addColumn null    // Add default column
        assert axis.hasDefaultColumn()
        try
        {
            axis.addColumn null
            fail 'should throw exception'
        }
        catch (IllegalArgumentException expected)
        {
            assert expected.message.contains('not')
            assert expected.message.contains('add')
            assert expected.message.contains('default')
            assert expected.message.contains('already')
        }
        axis.deleteColumn null
        assert !axis.hasDefaultColumn()
    }

    @Test
    void testAxisGetValues()
    {
        NCube ncube = new NCube('foo')
        ncube.addAxis NCubeBuilder.longDaysOfWeekAxis
        ncube.addAxis NCubeBuilder.longMonthsOfYear
        ncube.addAxis NCubeBuilder.getOddAxis(true)
        Axis axis = (Axis) ncube.axes.get(0)
        List values = axis.columns
        assert values.size() == 7
        assert TestNCube.countMatches(ncube.toHtml(), '<tr') == 44
    }

    @Test
    void testAxisCaseInsensitivity()
    {
        NCube<String> ncube = new NCube<String>('TestAxisCase')
        Axis gender = NCubeBuilder.getGenderAxis true
        ncube.addAxis gender
        Axis gender2 = new Axis('gender', AxisType.DISCRETE, AxisValueType.STRING, true)

        try
        {
            ncube.addAxis gender2
            fail 'should throw exception'
        }
        catch (IllegalArgumentException expected)
        {
            assert expected.message.contains('axis')
            assert expected.message.contains('already')
            assert expected.message.contains('exists')
        }

        def coord = [gendeR:null]
        ncube.setCell '1', coord
        assert '1'.equals(ncube.getCell(coord))

        coord.GendeR = 'Male'
        ncube.setCell '2', coord
        assert '2'.equals(ncube.getCell(coord))

        coord.GENdeR = 'Female'
        ncube.setCell '3', coord
        assert '3'.equals(ncube.getCell(coord))

        Axis axis = ncube.getAxis 'genDER'
        assert axis.name == 'Gender'
        ncube.deleteAxis 'GeNdEr'
        assert ncube.numDimensions == 0
    }

    @Test
    void testRangeSetAxisErrors()
    {
        Axis age = new Axis('Age', AxisType.SET, AxisValueType.LONG, true)
        RangeSet set = new RangeSet(1)
        set.add 3.0
        set.add new Range(10, 20)
        set.add 25
        age.addColumn set

        set = new RangeSet(2)
        set.add 20L
        set.add 35 as Byte
        age.addColumn set

        try
        {
            set = new RangeSet(12)
            age.addColumn(set)
            fail('should throw exception')
        }
        catch (AxisOverlapException expected)
        {
            assert expected.message.contains('RangeSet')
            assert expected.message.contains('overlap')
            assert expected.message.contains('exist')
        }

        try
        {
            set = new RangeSet(15)
            age.addColumn set
            fail 'should throw exception'
        }
        catch (AxisOverlapException expected)
        {
            assert expected.message.contains('RangeSet')
            assert expected.message.contains('overlap')
            assert expected.message.contains('exist')
        }

        try
        {
            set = new RangeSet(new Character('c' as char)) // not a valid type for a LONG axis
            age.addColumn set
            fail()
        }
        catch (Exception expected)
        {
            assert expected instanceof IllegalArgumentException
            assert expected.message.toLowerCase().contains('unsupported value type')
        }

        try
        {
            Range range = new Range(0, 10)
            age.addColumn range
            fail 'should throw exception'
        }
        catch (IllegalArgumentException expected)
        {
            assert expected.message.contains('only')
            assert expected.message.contains('add')
            assert expected.message.contains('RangeSet')
        }

        RangeSet a = new RangeSet()
        RangeSet b = new RangeSet()
        assert a.compareTo(b) == 0
    }

    @Test
    void testDeleteColumnFromRangeSetAxis() throws Exception
    {
        NCube ncube = NCubeManager.getNCubeFromResource 'testCube4.json'
        ncube.deleteColumn 'code', 'b'
        Axis axis = ncube.getAxis 'code'
        assert axis.id != 0
        assert axis.columns.size() == 2
        axis.deleteColumn('o')
        assert axis.columns.size() == 1
        assert axis.idToCol.size() == 1
        assertNull axis.deleteColumnById(9)
    }

    @Test
    void testDupeIdsOnAxis() throws Exception
    {
        try
        {
            NCubeManager.getNCubeFromResource('idBasedCubeError2.json')
            fail('should not make it here')
        }
        catch (AxisOverlapException e)
        {
            assert e.message.toLowerCase().contains('range overlap')
        }
    }

    @Test
    void testAddDefaultToNearestAxis()
    {
        Axis nearest = new Axis('points', AxisType.NEAREST, AxisValueType.COMPARABLE, false)
        try
        {
            nearest.addColumn null
            fail 'should not make it here'
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains("cannot add default column")
            assert e.message.toLowerCase().contains("to nearest axis")
        }
    }

    @Test
    void testMetaProperties()
    {
        Axis c = new Axis('foo', AxisType.DISCRETE, AxisValueType.STRING, true)
        assertNull c.metaProperties.get('foo')

        c.clearMetaProperties()
        c.setMetaProperty('foo', 'bar')
        assert 'bar' == c.metaProperties.get('foo')

        c.clearMetaProperties()
        assertNull c.metaProperties.get('foo')

        c.clearMetaProperties()
        c.addMetaProperties([BaZ:'qux'])
        assert 'qux' == c.metaProperties.get('baz')
    }

    @Test
    void testToString()
    {
        Axis axis = new Axis('foo', AxisType.DISCRETE, AxisValueType.LONG, false)
        assert 'Axis: foo [DISCRETE, LONG, no-default-column, sorted]' == axis.toString()

        Axis c = new Axis('foo', AxisType.DISCRETE, AxisValueType.STRING, true)
        assertNull c.metaProperties.get('foo')
        c.setMetaProperty 'foo', 'bar'

        assert 'Axis: foo [DISCRETE, STRING, default-column, sorted]\n' +
                '  metaProps: {foo=bar}' == c.toString()
    }

    @Test
    void testConvertDiscreteColumnValue() throws Exception
    {
        // Strings
        Axis states = NCubeBuilder.statesAxis
        assert states.convertStringToColumnValue('OH') == 'OH'

        // Longs
        Axis longs = new Axis('longs', AxisType.DISCRETE, AxisValueType.LONG, false)
        assert -1L == longs.convertStringToColumnValue('-1')
        assert 0L == longs.convertStringToColumnValue('0')
        assert 1L == longs.convertStringToColumnValue('1')
        assert 12345678901234L == longs.convertStringToColumnValue('12345678901234')
        assert -12345678901234L == longs.convertStringToColumnValue('-12345678901234')
        try
        {
            longs.convertStringToColumnValue '-12345.678901234'
            fail 'should not make it here'
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('could not be converted')
        }

        // BigDecimals
        Axis bigDec = new Axis('bigDec', AxisType.DISCRETE, AxisValueType.BIG_DECIMAL, false)
        assert -1g == bigDec.convertStringToColumnValue('-1')
        assert 0g == bigDec.convertStringToColumnValue('0')
        assert 1g == bigDec.convertStringToColumnValue('1')
        assert 12345678901234g == bigDec.convertStringToColumnValue('12345678901234')
        assert -12345678901234g ==  bigDec.convertStringToColumnValue('-12345678901234')
        assert -12345.678901234g == bigDec.convertStringToColumnValue('-12345.678901234')

        // Doubles
        Axis doubles = new Axis('bigDec', AxisType.DISCRETE, AxisValueType.DOUBLE, false)
        assertEquals(-1.0, doubles.convertStringToColumnValue('-1'), 0.000001d)
        assertEquals(0.0, doubles.convertStringToColumnValue('0'), 0.000001d)
        assertEquals(1.0, doubles.convertStringToColumnValue('1'), 0.00001d)
        assertEquals(12345678901234.0, doubles.convertStringToColumnValue('12345678901234'), 0.00001d)
        assertEquals(-12345678901234.0, doubles.convertStringToColumnValue('-12345678901234'), 0.00001d)
        assertEquals(-12345.678901234d, doubles.convertStringToColumnValue('-12345.678901234'), 0.00001d)

        // Dates
        Axis dates = new Axis('Dates', AxisType.DISCRETE, AxisValueType.DATE, false)
        Calendar cal = Calendar.instance
        cal.clear()
        cal.set(2014, 0, 18, 0, 0, 0)
        assert dates.convertStringToColumnValue('1/18/2014') == cal.time
        cal.clear()
        cal.set(2014, 6, 9, 13, 10, 58)
        assert dates.convertStringToColumnValue('2014 Jul 9 13:10:58') == cal.time
        try
        {
            dates.convertStringToColumnValue('2014 Ju1y 9 13:10:58')
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('could not be converted')
        }

        // Expression
        Axis exp = new Axis('Condition', AxisType.RULE, AxisValueType.EXPRESSION, false, Axis.DISPLAY)
        assert new GroovyExpression('println \'Hello\'', null, false) == exp.convertStringToColumnValue('println \'Hello\'')

        // Comparable (this allows user to create Java Comparable object instances as Column values!
        Axis comp = new Axis('Comparable', AxisType.DISCRETE, AxisValueType.COMPARABLE, false)
        cal.clear()
        cal.set 2014, 0, 18, 16, 26, 0
        String json = JsonWriter.objectToJson cal
        assert cal == comp.convertStringToColumnValue(json)
    }

    @Test
    void testRangeParsing()
    {
        Axis axis = new Axis('ages', AxisType.RANGE, AxisValueType.LONG, true, Axis.SORTED)
        Range range = (Range) axis.convertStringToColumnValue('10,20')
        assert 10L == range.low
        assert 20L == range.high

        range = (Range) axis.convertStringToColumnValue('  10 ,\t20  \n')
        assert 10L == range.low
        assert 20L == range.high

        axis = new Axis('ages', AxisType.RANGE, AxisValueType.DATE, false)
        range = (Range) axis.convertStringToColumnValue('12/25/2014, 12/25/2016')
        Calendar calendar = Calendar.instance
        calendar.clear()
        calendar.set 2014, 11, 25
        assert calendar.time == range.low
        calendar.clear()
        calendar.set 2016, 11, 25
        assert calendar.time == range.high

        range = (Range) axis.convertStringToColumnValue('Dec 25 2014, 12/25/2016')
        calendar = calendar.instance
        calendar.clear()
        calendar.set 2014, 11, 25
        assert calendar.time == range.low
        calendar.clear()
        calendar.set 2016, 11, 25
        assert calendar.time == range.high

        range = (Range) axis.convertStringToColumnValue('Dec 25 2014, Dec 25 2016')
        calendar = calendar.instance
        calendar.clear()
        calendar.set 2014, 11, 25
        assert calendar.time == range.low
        calendar.clear()
        calendar.set 2016, 11, 25
        assert calendar.time == range.high

        range = (Range) axis.convertStringToColumnValue('12/25/2014, Dec 25 2016')
        calendar = calendar.instance
        calendar.clear()
        calendar.set 2014, 11, 25
        assert calendar.time == range.low
        calendar.clear()
        calendar.set 2016, 11, 25
        assert calendar.time == range.high
    }

    @Test
    void testRangeWithBrackets()
    {
        Axis axis = new Axis('brackets', AxisType.RANGE, AxisValueType.LONG, true, Axis.SORTED)
        Range range = (Range) axis.convertStringToColumnValue('[10,20]')
        assert 10L == range.low
        assert 20L == range.high

        range = (Range) axis.convertStringToColumnValue('  [  10 ,  20  ]  ')
        assert 10L == range.low
        assert 20L == range.high
    }

    @Test
    void testDiscreteSetParsing()
    {
        Axis axis = new Axis('ages', AxisType.SET, AxisValueType.LONG, true, Axis.SORTED)
        RangeSet set = (RangeSet) axis.convertStringToColumnValue('10,20')
        assert 10L == set.get(0)
        assert 20L == set.get(1)

        set = (RangeSet) axis.convertStringToColumnValue('  10 ,\t20  \n')
        assert 10L == set.get(0)
        assert 20L == set.get(1)

        // Support no outer brackets
        axis.convertStringToColumnValue('10, 20')
        assert 10L == set.get(0)
        assert 20L == set.get(1)

        axis = new Axis('ages', AxisType.SET, AxisValueType.DATE, false)
        set = (RangeSet) axis.convertStringToColumnValue(' "12/25/2014", "12/25/2016"')
        Calendar calendar = Calendar.instance
        calendar.clear()
        calendar.set 2014, 11, 25
        assert calendar.time == set.get(0)
        calendar.clear()
        calendar.set 2016, 11, 25
        assert calendar.time == set.get(1)

        set = (RangeSet) axis.convertStringToColumnValue(' "Dec 25th 2014", "Dec 25th 2016"')
        calendar = calendar.instance
        calendar.clear()
        calendar.set 2014, 11, 25
        assert calendar.time == set.get(0)
        calendar.clear()
        calendar.set 2016, 11, 25
        assert calendar.time == set.get(1)
    }

    @Test
    void testRangeSetParsing()
    {
        Axis axis = new Axis('ages', AxisType.SET, AxisValueType.LONG, true, Axis.SORTED)
        RangeSet set = (RangeSet) axis.convertStringToColumnValue('[10,20]')
        Range range = (Range) set.get(0)
        assert 10L == range.low
        assert 20L == range.high

        set = (RangeSet) axis.convertStringToColumnValue(' [  10 ,\t20  \n] ')
        range = (Range) set.get(0)
        assert 10L == range.low
        assert 20L == range.high

        axis = new Axis('ages', AxisType.SET, AxisValueType.DATE, false)
        set = (RangeSet) axis.convertStringToColumnValue('[ "12/25/2014", "12/25/2016"]')
        range = (Range) set.get(0)
        Calendar calendar = Calendar.instance
        calendar.clear()
        calendar.set 2014, 11, 25
        assert calendar.time == range.low
        calendar.clear()
        calendar.set 2016, 11, 25
        assert calendar.time == range.high
    }

    @Test
    void testRangeAndDiscreteSetParsing()
    {
        Axis axis = new Axis('ages', AxisType.SET, AxisValueType.LONG, true, Axis.SORTED)
        RangeSet set = (RangeSet) axis.convertStringToColumnValue('[10,20], 1979')
        Range range = (Range) set.get(0)
        assert 10L == range.low
        assert 20L == range.high
        assert 1979L == set.get(1)

        set = (RangeSet) axis.convertStringToColumnValue(' [  10 ,\t20  \n] , 1979 ')
        range = (Range) set.get(0)
        assert 10L == range.low
        assert 20L == range.high
        assert 1979L == set.get(1)

        axis = new Axis('ages', AxisType.SET, AxisValueType.DATE, false)
        set = (RangeSet) axis.convertStringToColumnValue('[ "12/25/2014", "12/25/2016"], "12/25/2020"')
        range = (Range) set.get(0)
        Calendar calendar = Calendar.instance
        calendar.clear()
        calendar.set 2014, 11, 25
        assert calendar.time == range.low
        calendar.clear()
        calendar.set 2016, 11, 25
        assert calendar.time == range.high
        calendar.clear()
        calendar.set 2020, 11, 25
        assert calendar.time == set.get(1)
    }

    @Test
    void testRangeAndDiscreteSetParsing2()
    {
        Axis axis = new Axis('ages', AxisType.SET, AxisValueType.BIG_DECIMAL, true, Axis.SORTED)
        RangeSet set = (RangeSet) axis.convertStringToColumnValue('[10,20], 1979')
        Range range = (Range) set.get(0)
        assert 10g == range.low
        assert 20g == range.high
        assert 1979g == set.get(1)

        set = (RangeSet) axis.convertStringToColumnValue(' [  10.0 ,\t20  \n] , 1979 ')
        range = (Range) set.get(0)
        assert 10g == range.low
        assert 20g == range.high
        assert 1979g == set.get(1)
    }

    @Test
    void testNearestWithDoubles()
    {
        Axis axis = new Axis('loc', AxisType.NEAREST, AxisValueType.COMPARABLE, false)
        LatLon latlon = (LatLon) axis.convertStringToColumnValue('1.0, 2.0')
        assertEquals 1.0, latlon.lat, 0.00001d
        assertEquals 2.0, latlon.lon, 0.00001d

        latlon = (LatLon) axis.convertStringToColumnValue('1,2')
        assertEquals 1.0, latlon.lat, 0.00001d
        assertEquals 2.0, latlon.lon, 0.00001d

        latlon = (LatLon) axis.convertStringToColumnValue('-1,-2')
        assertEquals(-1.0, latlon.lat, 0.00001d)
        assertEquals(-2.0, latlon.lon, 0.001d)

        axis = new Axis('loc', AxisType.NEAREST, AxisValueType.COMPARABLE, false)
        Point3D pt3d = (Point3D) axis.convertStringToColumnValue('1.0, 2.0, 3.0')
        assertEquals 1.0, pt3d.x, 0.00001d
        assertEquals 2.0, pt3d.y, 0.00001d
        assertEquals 3.0, pt3d.z, 0.00001d
    }

    @Test
    void testAddAxisSameWayAsUI()
    {
        Axis axis = new Axis('loc', AxisType.SET, AxisValueType.LONG, true)
        Axis axis2 = new Axis('loc', AxisType.SET, AxisValueType.LONG, false)
        Column colAdded = axis2.addColumn('[1, 2]')
        colAdded.id = -1
        axis.updateColumns axis2

        assert 2 == axis.columns.size()
        Column col = axis.columnsWithoutDefault.get(0)
        RangeSet rs = new RangeSet(new Range(1L, 2L))
        assert rs == col.value
    }

    @Test
    void testUpdateColumnWithMetaPropertyName()
    {
        Axis axis1 = new Axis('loc', AxisType.SET, AxisValueType.LONG, true)
        axis1.addColumn('[1, 2]')
        Axis axis2 = new Axis('loc', AxisType.SET, AxisValueType.LONG, true)
        axis2.addColumn('[1, 2]')
        List<Column> cols = axis2.columnsWithoutDefault
        cols.get(0).id = axis1.columnsWithoutDefault.get(0).id
        cols.get(0).setMetaProperty('name', 'cheese')
        cols.get(0).setMetaProperty('foo', 'bar')
        axis1.updateColumns(axis2)

        assert 2 == axis1.columns.size()
        Column col = axis1.columnsWithoutDefault.get(0)
        assert 'cheese' == col.getMetaProperty('name')
        assert 'bar' == col.getMetaProperty('foo')
    }

    @Test
    void testRemoveSetColumnWithMultipleRanges()
    {
        Axis axis = new Axis('loc', AxisType.SET, AxisValueType.LONG, false)
        RangeSet rs = new RangeSet()
        rs.add(new Range(10, 20))
        rs.add(new Range(30, 40))
        axis.addColumn(rs)
        rs = new RangeSet()
        rs.add(new Range(50, 60))
        axis.addColumn(rs)
        assert 2 == axis.columns.size()
        assert 3 == axis.rangeToCol.size()
        axis.deleteColumn(15)
        assert 1 == axis.rangeToCol.size()
        assert 1 == axis.columns.size()
    }

    @Test
    void testRemoveSetColumnWithMultipleDiscretes()
    {
        Axis axis = new Axis('loc', AxisType.SET, AxisValueType.LONG, false)
        RangeSet rs = new RangeSet()
        rs.add 20
        rs.add 30
        axis.addColumn rs
        rs = new RangeSet()
        rs.add 50
        axis.addColumn rs
        assert 2 == axis.columns.size()
        assert 3 == axis.discreteToCol.size()
        axis.deleteColumn 30
        assert 1 == axis.discreteToCol.size()
        assert 1 == axis.columns.size()
    }

    @Test
    void testAddAxisBadColumnIds()
    {
        Axis axis = new Axis('loc', AxisType.SET, AxisValueType.LONG, true)
        Axis axis2 = new Axis('loc', AxisType.SET, AxisValueType.LONG, true)
        axis2.addColumn '[1, 2]'
        try
        {
            axis.updateColumns(axis2)
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.contains('added')
            assert e.message.contains('negative')
            assert e.message.contains('values')
        }
    }

    @Test
    void testParseBadRange()
    {
        Axis axis = new Axis('foo', AxisType.RANGE, AxisValueType.LONG, false)
        try
        {
            axis.convertStringToColumnValue('this is not a range')
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.contains('not')
            assert e.message.contains('range')
        }
    }

    @Test
    void testParseBadSet()
    {
        Axis axis = new Axis('foo', AxisType.SET, AxisValueType.LONG, false)
        try
        {
            axis.convertStringToColumnValue('[null, false]')
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains("cannot be parsed as a set")
        }

        try
        {
            axis.convertStringToColumnValue('null, false')
            fail 'should not make it here'
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains("cannot be parsed as a set")
        }
    }

    @Test
    void testFindNonExistentRuleName()
    {
        Axis axis = new Axis('foo', AxisType.RULE, AxisValueType.EXPRESSION, false, Axis.DISPLAY)
        try
        {
            axis.getRuleColumnsStartingAt('foo')
            fail 'should not make it here'
        }
        catch (CoordinateNotFoundException e)
        {
            assert e.message.contains('rule')
            assert e.message.toLowerCase().contains('matches no column')
            assert e.message.contains('foo')
            assert e.message.contains('no default')
        }
    }

    @Test
    void testFindRuleNameUsingNonString()
    {
        Axis axis = new Axis('foo', AxisType.RULE, AxisValueType.EXPRESSION, false, Axis.DISPLAY)
        try
        {
            axis.findColumn 25
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.contains('rule')
            assert e.message.toLowerCase().contains('only')
            assert e.message.contains('located')
            assert e.message.contains('name')
        }
    }

    @Test
    void testGetColumnsAndCoordinateFromIds()
    {
        NCube cube = NCubeBuilder.testNCube3D_Boolean

        Axis trailor = cube.getAxis("Trailers")
        Column t = trailor.findColumn("M2A")

        Axis vehicles = cube.getAxis("Vehicles")
        Column v = vehicles.findColumn("van")

        Axis bu = cube.getAxis("BU")
        Column b = bu.findColumn("SHS")

        Set<Long> longCoord = new LongHashSet()
        longCoord.add(t.id)
        longCoord.add(v.id)
        longCoord.add(b.id)

        // Make sure all columns are bound correctly
        def coord = new CaseInsensitiveMap()
        Set<Long> boundCols = cube.ensureFullCoordinate(longCoord)
        for (Long colId : boundCols)
        {
            assertTrue(colId == t.id || colId == v.id || colId == b.id)
        }

        for (Map.Entry<String, CellInfo> entry : coord.entrySet())
        {
            CellInfo info = entry.value
            assertTrue("M2A".equals(info.value) || "van".equals(info.value) || "SHS".equals(info.value))
        }

        Column t2 = trailor.findColumn("L3A")
        longCoord.add(t2.id)
        try
        {
            cube.getColumnsAndCoordinateFromIds(longCoord, coord)
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.message.contains("more than one column"))
            assertTrue(e.message.contains("per axis"))
        }

        try
        {
            longCoord.remove(t2.id)
            longCoord.remove(t.id)
            cube.getColumnsAndCoordinateFromIds(longCoord, coord)
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.message.toLowerCase().contains("column id"))
            assertTrue(e.message.toLowerCase().contains("missing"))
        }
    }

    @Test
    void testSha1NotSensitiveToAxisNameCase()
    {
        NCube cube1 = new NCube("foo")
        NCube cube2 = new NCube("foo")
        NCube cube3 = new NCube("foo")
        Axis axis1 = new Axis("state", AxisType.DISCRETE, AxisValueType.BIG_DECIMAL, true, Axis.SORTED, cube1.maxAxisId)
        Axis axis2 = new Axis("STATE", AxisType.DISCRETE, AxisValueType.BIG_DECIMAL, true, Axis.SORTED, cube2.maxAxisId)
        Axis axis3 = new Axis("state", AxisType.DISCRETE, AxisValueType.BIG_DECIMAL, true, Axis.SORTED, cube3.maxAxisId)
        cube1.addAxis(axis1)
        cube2.addAxis(axis2)
        assertEquals(cube1.sha1(), cube2.sha1())

        cube3.addAxis(axis3)
        assertEquals(cube1.sha1(), cube3.sha1())
    }

    @Test
    void testStringAxis()
    {
        NCube<Integer> ncube = new NCube<Integer>("SingleStringAxis")
        Axis genderAxis = NCubeBuilder.getGenderAxis(false)
        ncube.addAxis(genderAxis)

        def coord = [Gender:'Male']
        ncube.setCell(0, coord)
        coord.Gender = 'Female'
        ncube.setCell(1, coord)

        try
        {
            genderAxis.addColumn(new Comparable() {
                int compareTo(Object o) {
                    return 0
                }
            })
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('unsupported value type')
        }

        coord.put("Gender", "Male")
        assertTrue(ncube.getCell(coord) == 0)
        coord.put("Gender", "Female")
        assertTrue(ncube.getCell(coord) == 1)
        assertTrue(TestNCube.countMatches(ncube.toHtml(), "<tr") == 3)

        try
        {
            coord.put("Gender", "Jones")
            ncube.getCell(coord)
            fail()
        }
        catch (CoordinateNotFoundException e)
        {
            assertTrue(e.message.contains("alue"))
            assertTrue(e.message.contains("not"))
            assertTrue(e.message.contains("found"))
            assertTrue(e.message.contains("axis"))
        }

        // 'null' value to find on String axis:
        try
        {
            coord.put("Gender", null)
            ncube.getCell(coord)
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('null')
            assert e.message.toLowerCase().contains('passed to axis')
            assert e.message.toLowerCase().contains('not have')
            assert e.message.toLowerCase().contains('default column')
        }

        // Illegal value to find on String axis:
        try
        {
            coord.put("Gender", 8)
            ncube.getCell(coord)
            fail()
        }
        catch (CoordinateNotFoundException e)
        {
            assert e.message.toLowerCase().contains('value')
            assert e.message.toLowerCase().contains('not found on axis')
        }

        // 'null' for coordinate
        try
        {
            ncube.getCell((Map)null)
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('null')
            assert e.message.toLowerCase().contains('passed in for coordinate map')
        }

        // 0-length coordinate
        try
        {
            coord.clear()
            ncube.getCell(coord)
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('input')
            assert e.message.toLowerCase().contains('not contain')
            assert e.message.toLowerCase().contains('required scope')
        }

        // coordinate / table dimension mismatch
        try
        {
            coord.clear()
            coord.State = 'OH'
            ncube.getCell(coord)
            fail()
        }
        catch (IllegalArgumentException e)
        {
            assert e.message.toLowerCase().contains('input')
            assert e.message.toLowerCase().contains('not contain')
            assert e.message.toLowerCase().contains('required scope')
        }
    }

    @Test
    void testNoDefaultColumn()
    {
        NCube<Boolean> ncube = NCubeBuilder.testNCube3D_Boolean

        def coord = [:]
        coord.put("Trailers", "S1A")
        coord.put("Vehicles", "car")
        coord.put("BU", "Agri")
        Boolean v = ncube.getCell(coord)
        assertNull(v)
        ncube.setCell(true, coord)
        v = ncube.getCell(coord)
        assertTrue(v)
        ncube.toHtml() // Use to test 3D visually

        try
        {
            coord.put("BU", "bogus")
            ncube.getCell(coord)
            fail("should throw exception")
        }
        catch (CoordinateNotFoundException e)
        {
            assertTrue(e.message.contains("alue"))
            assertTrue(e.message.contains("not"))
            assertTrue(e.message.contains("found"))
            assertTrue(e.message.contains("axis"))
        }
    }

    @Test
    void testDefaultColumn()
    {
        NCube<Boolean> ncube = new NCube<Boolean>("Test.Default.Column")
        Axis axis = NCubeBuilder.getGenderAxis(true)
        ncube.addAxis(axis)

        def male = [:]
        male.put("Gender", "Male")
        def female = [:]
        female.put("Gender", "Female")
        Map nullGender = new HashMap()
        nullGender.put("Gender", null)

        ncube.setCell(true, male)
        ncube.setCell(false, female)
        ncube.setCell(true, nullGender)

        assertTrue(ncube.getCell(male))
        assertFalse(ncube.getCell(female))
        assertTrue(ncube.getCell(nullGender))

        ncube.setCell(false, male)
        ncube.setCell(true, female)
        ncube.setCell(null, nullGender)

        assertFalse(ncube.getCell(male))
        assertTrue(ncube.getCell(female))
        assertNull(ncube.getCell(nullGender))

        def coord = [:]
        coord.put("Gender", "missed")
        ncube.setCell(true, coord)
        coord.put("Gender", "yes missed")
        assertTrue(ncube.getCell(coord))
        assertTrue(TestNCube.countMatches(ncube.toHtml(), "<tr") == 4)
    }

    @Test
    void testNearestAxisTypePoint3D()
    {
        NCube<String> ncube = new NCube<String>("Nearest3D")

        Axis points = new Axis("Point", AxisType.NEAREST, AxisValueType.COMPARABLE, false)
        points.addColumn(new Point3D(0.0, 0.0, 0.0))
        points.addColumn(new Point3D(1.0, 0.0, 0.0))
        points.addColumn(new Point3D(0.0, 1.0, 0.0))
        points.addColumn(new Point3D(-1.0, 0.0, 0.0))
        points.addColumn(new Point3D(0.0, -1.0, 0.0))
        points.addColumn(new Point3D(0.0, 0.0, 1.0))
        points.addColumn(new Point3D(0.0, 0.0, -1.0))
        ncube.addAxis(points)

        def coord = [Point:new Point3D(0.0, 0.0, 0.0)]
        ncube.setCell("0.0, 0.0, 0.0", coord)
        coord.Point = new Point3D(1.0, 0.0, 0.0)
        ncube.setCell("1.0, 0.0, 0.0", coord)
        coord.Point = new Point3D(0.0, 1.0, 0.0)
        ncube.setCell("0.0, 1.0, 0.0", coord)
        coord.Point = new Point3D(-1.0, 0.0, 0.0)
        ncube.setCell("-1.0, 0.0, 0.0", coord)
        coord.Point = new Point3D(0.0, -1.0, 0.0)
        ncube.setCell("0.0, -1.0, 0.0", coord)
        coord.Point = new Point3D(0.0, 0.0, 1.0)
        ncube.setCell("0.0, 0.0, 1.0", coord)
        coord.Point = new Point3D(0.0, 0.0, -1.0)
        ncube.setCell("0.0, 0.0, -1.0", coord)

        coord.Point = new Point3D(0.0, 0.0, 0.0)
        String s = ncube.getCell(coord)
        assertTrue("0.0, 0.0, 0.0".equals(s))

        coord.Point = new Point3D(-0.1, 0.1, 0.1)
        s = ncube.getCell(coord)
        assertTrue("0.0, 0.0, 0.0".equals(s))

        coord.Point = new Point3D(0.49, 0.49, 0.49)
        s = ncube.getCell(coord)
        assertTrue("0.0, 0.0, 0.0".equals(s))

        coord.Point = new Point3D(2.0, 100.0, 3.0)
        s = ncube.getCell(coord)
        assertTrue("0.0, 1.0, 0.0".equals(s))

        coord.Point = new Point3D(0.1, -0.2, -63.0)
        s = ncube.getCell(coord)
        assertTrue("0.0, 0.0, -1.0".equals(s))

        Point3D p1 = new Point3D(1.0, 2.0, 3.0)
        s = p1.toString()
        assertEquals("1.0, 2.0, 3.0", s)
        assertFalse(p1.equals("string"))
        Point3D p2 = new Point3D(1.0, 2.0, 3.0)
        assertTrue(p1.compareTo(p2) == 0)

        assertTrue(TestNCube.countMatches(ncube.toHtml(), "<tr") == 8)
    }

    @Test
    void testAxisProps()
    {
        Axis axis1 = new Axis('foo', AxisType.DISCRETE, AxisValueType.STRING, false, Axis.DISPLAY)
        Axis axis2 = new Axis('foo', AxisType.DISCRETE, AxisValueType.STRING, false, Axis.DISPLAY)

        assert axis1.areAxisPropsEqual(axis1)
        assert axis1.areAxisPropsEqual(axis2)
        assert !axis1.areAxisPropsEqual('fudge')

        Axis axis3 = new Axis('foo', AxisType.DISCRETE, AxisValueType.STRING, false, Axis.SORTED)
        assert !axis1.areAxisPropsEqual(axis3)

        Axis axis4 = new Axis('foo', AxisType.DISCRETE, AxisValueType.STRING, true, Axis.DISPLAY)
        assert !axis1.areAxisPropsEqual(axis4)

        Axis axis5 = new Axis('foo', AxisType.DISCRETE, AxisValueType.STRING, false, Axis.DISPLAY)
        assert axis1.areAxisPropsEqual(axis5)
        axis5.setMetaProperty 'foo', 'bar'
        assert axis1.areAxisPropsEqual(axis5) // Ensuring meta-props are not part of arePropsEquals()

        Axis axis6 = new Axis('foot', AxisType.DISCRETE, AxisValueType.STRING, false, Axis.DISPLAY)
        assert !axis1.areAxisPropsEqual(axis6)

        Axis axis7 = new Axis('foo', AxisType.RANGE, AxisValueType.STRING, false, Axis.DISPLAY)
        assert !axis1.areAxisPropsEqual(axis7)

        Axis axis8 = new Axis('foo', AxisType.DISCRETE, AxisValueType.LONG, false, Axis.DISPLAY)
        assert !axis1.areAxisPropsEqual(axis8)
    }

    @Test
    void testUpdateColumn()
    {
        Axis dow = NCubeBuilder.shortDaysOfWeekAxis
        Column wed = dow.findColumn("Wed")
        dow.updateColumn(wed.id, "aWed")
        wed = dow.columns.get(2)
        assertEquals(wed.value, "aWed")

        Column mon = dow.findColumn("Mon")
        dow.updateColumn(mon.id, "aMon")
        mon = dow.columns.get(0)
        assertEquals(mon.value, "aMon")

        Column sun = dow.findColumn("Sun")
        dow.updateColumn(sun.id, "aSun")
        sun = dow.columns.get(6)
        assertEquals(sun.value, "aSun")

        List<Column> cols = dow.columnsWithoutDefault
        assertEquals(cols.get(4).value, "aMon")
        assertEquals(cols.get(5).value, "aSun")
        assertEquals(cols.get(6).value, "aWed")

        assertEquals(-1, cols.get(4).compareTo(new Column(null, dow.nextColId)))
    }

    @Test
    void testUpdateColumnsFrontMiddleBack()
    {
        Axis axis = new Axis('Age', AxisType.RANGE, AxisValueType.LONG, false, Axis.SORTED, 1)
        axis.addColumn(new Range(5, 10))
        axis.addColumn(new Range(20, 30))
        axis.addColumn(new Range(30, 40))

        Axis axis2 = new Axis('Age', AxisType.RANGE, AxisValueType.LONG, false, Axis.SORTED, 1)
        axis2.addColumn(new Range(5, 10))
        axis2.addColumn(new Range(20, 30))
        axis2.addColumn(new Range(30, 40))

        Column newCol = axis.createColumnFromValue new Range(10, 20)
        newCol.id = -newCol.id
        axis.columnsInternal.add newCol

        axis2.updateColumns(axis)
        assert 4 == axis2.columns.size()

        newCol = axis.createColumnFromValue new Range(0, 5)
        newCol.id = -newCol.id
        axis.columnsInternal.add newCol

        axis2.updateColumns(axis)
        assert 5 == axis2.columns.size()

        newCol = axis.createColumnFromValue new Range(40, 50)
        newCol.id = -newCol.id
        axis.columnsInternal.add newCol

        axis2.updateColumns axis
        assert 6 == axis2.columns.size()

        for (Column column : axis2.columns)
        {
            assert column.id >= 0
        }

        // Test remove via updateColumns()
        axis = new Axis('Age', AxisType.RANGE, AxisValueType.LONG, false, Axis.SORTED, 1)
        axis.addColumn new Range(5, 10)
        axis.addColumn new Range(20, 30)
        axis.addColumn new Range(30, 40)

        axis2.updateColumns axis
        assert 3 == axis2.size()
    }

    @Test
    void testUpdateColumnsOverlapCheck()
    {
        Axis axis = new Axis('Age', AxisType.RANGE, AxisValueType.STRING, false, Axis.DISPLAY, 1)
        axis.addColumn new Range('2', '4')
        axis.addColumn new Range('4', '6')
        axis.addColumn new Range('6', '8')
        axis.addColumn new Range('0', '2')

        Axis axis2 = new Axis('Age', AxisType.RANGE, AxisValueType.STRING, false, Axis.DISPLAY, 1)
        axis2.addColumn new Range('2', '4')
        axis2.addColumn new Range('4', '6')
        axis2.addColumn new Range('6', '8')
        axis2.addColumn new Range('0', '2')

        Column newCol = axis.createColumnFromValue new Range('8', '10')
        newCol.id = -newCol.id
        axis.columnsInternal.add newCol

        try
        {
            axis2.updateColumns axis
            fail()
        }
        catch (AxisOverlapException e)
        {
            assert e.message.toLowerCase().contains('overlap')
            assert e.message.toLowerCase().contains('exist')
            assert e.message.toLowerCase().contains('axis')
        }
    }

    @Test
    void testUpColumnsMaintainsOrder()
    {
        Axis axis = new Axis('days', AxisType.DISCRETE, AxisValueType.STRING, false, Axis.DISPLAY, 1)
        axis.addColumn 'Mon'
        axis.addColumn 'Tue'
        Column wed = axis.addColumn 'Wed'
        wed.id = -wed.id
        axis.addColumn 'Thu'
        axis.addColumn 'Fri'
        axis.addColumn 'Sat'
        axis.addColumn 'Sun'

        // Mon/Sat backwards
        // Wed missing
        // Bogus column added (named 'Whoops')
        // Fix these problems with updateColumns (simulate user moving columns in NCE)
        Axis axis2 = new Axis('days', AxisType.DISCRETE, AxisValueType.STRING, false, Axis.DISPLAY, 1)
        axis2.addColumn 'Sat'
        axis2.addColumn 'Tue'
        axis2.addColumn 'Wed'
        axis2.addColumn 'Thu'
        axis2.addColumn 'Fri'
        axis2.addColumn 'Mon'
        axis2.addColumn 'Sun'
        axis2.addColumn 'Whoops'
        axis2.deleteColumn 'Wed'

        axis2.updateColumns axis
        assert 7 == axis2.size()
        assert 'Mon' == axis2.columns[0].value
        assert 'Tue' == axis2.columns[1].value
        assert 'Wed' == axis2.columns[2].value
        assert 'Thu' == axis2.columns[3].value
        assert 'Fri' == axis2.columns[4].value
        assert 'Sat' == axis2.columns[5].value
        assert 'Sun' == axis2.columns[6].value
    }

    @Test
    void testUpColumnsMaintainsOrderWithDefault()
    {
        Axis axis = new Axis('days', AxisType.DISCRETE, AxisValueType.STRING, true, Axis.DISPLAY, 1)
        axis.addColumn 'Mon'
        axis.addColumn 'Tue'
        Column wed = axis.addColumn 'Wed'
        wed.id = -wed.id
        axis.addColumn 'Thu'
        axis.addColumn 'Fri'
        axis.addColumn 'Sat'
        axis.addColumn 'Sun'

        // Mon/Sat backwards
        // Wed missing
        // Bogus column added (named 'Whoops')
        // Fix these problems with updateColumns (simulate user moving columns in NCE)
        Axis axis2 = new Axis('days', AxisType.DISCRETE, AxisValueType.STRING, true, Axis.DISPLAY, 1)
        axis2.addColumn 'Sat'
        axis2.addColumn 'Tue'
        axis2.addColumn 'Wed'
        axis2.addColumn 'Thu'
        axis2.addColumn 'Fri'
        axis2.addColumn 'Mon'
        axis2.addColumn 'Sun'
        axis2.addColumn 'Whoops'
        axis2.deleteColumn 'Wed'

        axis2.updateColumns axis
        assert 8 == axis2.size()
        assert 'Mon' == axis2.columns[0].value
        assert 'Tue' == axis2.columns[1].value
        assert 'Wed' == axis2.columns[2].value
        assert 'Thu' == axis2.columns[3].value
        assert 'Fri' == axis2.columns[4].value
        assert 'Sat' == axis2.columns[5].value
        assert 'Sun' == axis2.columns[6].value
        assertNull axis2.columns[7].value
        assert Integer.MAX_VALUE == axis2.columns.get(7).displayOrder
    }

    @Test
    void testUpColumnsMaintainsIgnoresDefault()
    {
        Axis axis = new Axis('days', AxisType.DISCRETE, AxisValueType.STRING, true, Axis.DISPLAY, 1)
        axis.addColumn 'Mon'
        axis.addColumn 'Tue'
        Column wed = axis.addColumn 'Wed'
        wed.id = -wed.id
        axis.addColumn 'Thu'
        axis.addColumn 'Fri'
        axis.addColumn 'Sat'
        axis.addColumn 'Sun'

        // Mon/Sat backwards
        // Wed missing
        // Bogus column added (named 'Whoops')
        // Fix these problems with updateColumns (simulate user moving columns in NCE)
        Axis axis2 = new Axis('days', AxisType.DISCRETE, AxisValueType.STRING, false, Axis.DISPLAY, 1)
        axis2.addColumn 'Sat'
        axis2.addColumn 'Tue'
        axis2.addColumn 'Wed'
        axis2.addColumn 'Thu'
        axis2.addColumn 'Fri'
        axis2.addColumn 'Mon'
        axis2.addColumn 'Sun'
        axis2.addColumn 'Whoops'
        axis2.deleteColumn 'Wed'

        axis2.updateColumns axis
        assert 7 == axis2.size()
        assert 'Mon' == axis2.columns[0].value
        assert 'Tue' == axis2.columns[1].value
        assert 'Wed' == axis2.columns[2].value
        assert 'Thu' == axis2.columns[3].value
        assert 'Fri' == axis2.columns[4].value
        assert 'Sat' == axis2.columns[5].value
        assert 'Sun' == axis2.columns[6].value
    }

    @Test
    void testProveDefaultLast()
    {
        Axis axis = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, true, Axis.SORTED)
        axis.addColumn("alpha")
        axis.addColumn("charlie")
        axis.addColumn("bravo")
        List<Column> cols = axis.columns
        assertEquals(cols.get(0).value, "alpha")
        assertEquals(cols.get(1).value, "bravo")
        assertEquals(cols.get(2).value, "charlie")
        assertEquals(cols.get(3).value, null)

        axis = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, false, Axis.SORTED)
        axis.addColumn("alpha")
        axis.addColumn("charlie")
        axis.addColumn("bravo")
        cols = axis.columns
        assertEquals(3, cols.size())
        assertEquals(cols.get(0).value, "alpha")
        assertEquals(cols.get(1).value, "bravo")
        assertEquals(cols.get(2).value, "charlie")

        axis = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, true, Axis.DISPLAY)
        axis.addColumn("alpha")
        axis.addColumn("charlie")
        axis.addColumn("bravo")
        cols = axis.columns
        assertEquals(cols.get(0).value, "alpha")
        assertEquals(cols.get(1).value, "charlie")
        assertEquals(cols.get(2).value, "bravo")
        assertEquals(cols.get(3).value, null)

        axis = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, false, Axis.DISPLAY)
        axis.addColumn("alpha")
        axis.addColumn("charlie")
        axis.addColumn("bravo")
        cols = axis.columns
        assertEquals(3, cols.size())
        assertEquals(cols.get(0).value, "alpha")
        assertEquals(cols.get(1).value, "charlie")
        assertEquals(cols.get(2).value, "bravo")
    }

    @Test
    void testMaxAxisId()  throws Exception
    {
        NCube cube = new NCube("fourD")
        assertEquals(0, cube.maxAxisId)

        Axis axis1 = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, true, Axis.SORTED, cube.maxAxisId + 1)
        cube.addAxis(axis1)
        assertEquals(1, cube.maxAxisId)

        Axis axis2 = new Axis("bar", AxisType.DISCRETE, AxisValueType.STRING, true, Axis.SORTED, cube.maxAxisId + 1)
        cube.addAxis(axis2)
        assertEquals(2, cube.maxAxisId)

        Axis axis3 = new Axis("baz", AxisType.DISCRETE, AxisValueType.STRING, true, Axis.SORTED, cube.maxAxisId + 1)
        cube.addAxis(axis3)
        assertEquals(3, cube.maxAxisId)

        Axis axis4 = new Axis("qux", AxisType.DISCRETE, AxisValueType.STRING, true, Axis.SORTED, cube.maxAxisId + 1)
        cube.addAxis(axis4)
        assertEquals(4, cube.maxAxisId)
    }

    @Test
    void testEqualsAxisNameMismatch()  throws Exception
    {
        Axis axis1 = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, true)
        Axis axis2 = new Axis("foot", AxisType.DISCRETE, AxisValueType.STRING, true)
        NCube cube1 = new NCube("bar")
        NCube cube2 = new NCube("bar")

        cube1.addAxis(axis1)
        cube2.addAxis(axis2)

        assertNotEquals(cube1, cube2)
    }

    @Test
    void testEqualsAxisMetaMismatch()  throws Exception
    {
        Axis axis1 = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, true)
        axis1.setMetaProperty("fingers", 4)
        axis1.setMetaProperty("thumb", 1)
        Axis axis2 = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, true)
        NCube cube1 = new NCube("bar")
        NCube cube2 = new NCube("bar")

        cube1.addAxis(axis1)
        cube2.addAxis(axis2)
        assertNotEquals(cube1, cube2)
    }

    @Test
    void testEqualsAxisTypeMismatch()  throws Exception
    {
        Axis axis1 = new Axis("foo", AxisType.RANGE, AxisValueType.STRING, true)
        Axis axis2 = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, true)
        NCube cube1 = new NCube("bar")
        NCube cube2 = new NCube("bar")

        cube1.addAxis(axis1)
        cube2.addAxis(axis2)
        assertNotEquals(cube1, cube2)
    }

    @Test
    void testEqualsAxisValueTypeMismatch()  throws Exception
    {
        Axis axis1 = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, true)
        Axis axis2 = new Axis("foo", AxisType.DISCRETE, AxisValueType.LONG, true)
        NCube cube1 = new NCube("bar")
        NCube cube2 = new NCube("bar")

        cube1.addAxis(axis1)
        cube2.addAxis(axis2)
        assertNotEquals(cube1, cube2)
    }

    @Test
    void testEqualsAxisDefaultMismatch()  throws Exception
    {
        Axis axis1 = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, true)
        Axis axis2 = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, false)
        NCube cube1 = new NCube("bar")
        NCube cube2 = new NCube("bar")

        cube1.addAxis(axis1)
        cube2.addAxis(axis2)
        assertNotEquals(cube1, cube2)
    }

    @Test
    void testEqualsColumnCountMismatch()  throws Exception
    {
        Axis axis1 = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, false)
        Axis axis2 = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, false)
        NCube cube1 = new NCube("bar")
        NCube cube2 = new NCube("bar")

        cube1.addAxis(axis1)
        cube1.addColumn("foo", "qux")

        cube2.addAxis(axis2)
        assertNotEquals(cube1, cube2)
    }

    @Test
    void testEqualsColumnTypeMismatch()  throws Exception
    {
        Axis axis1 = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, false)
        Axis axis2 = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, true)
        NCube cube1 = new NCube("bar")
        NCube cube2 = new NCube("bar")

        cube1.addAxis(axis1)
        cube1.addColumn("foo", "qux")

        cube2.addAxis(axis2)
        assertNotEquals(cube1, cube2)
    }

    @Test
    void testEqualsColumnValueMismatch()  throws Exception
    {
        Axis axis1 = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, true)
        Axis axis2 = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, true)
        NCube cube1 = new NCube("bar")
        NCube cube2 = new NCube("bar")

        cube1.addAxis(axis1)
        cube1.addColumn("foo", "baz")

        cube2.addAxis(axis2)
        cube2.addColumn("foo", "qux")
        assertNotEquals(cube1, cube2)
    }

    @Test
    void testEqualsColumnMetaPropertiesMismatch()  throws Exception
    {
        Axis axis1 = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, true)
        Axis axis2 = new Axis("foo", AxisType.DISCRETE, AxisValueType.STRING, true)
        NCube cube1 = new NCube("bar")
        NCube cube2 = new NCube("bar")

        cube1.addAxis(axis1)
        cube1.addColumn("foo", "baz")
        Column col = axis1.findColumn("baz")
        col.setMetaProperty("Glock", "23")

        cube2.addAxis(axis2)
        cube2.addColumn("foo", "baz")
        assertNotEquals(cube1, cube2)
    }

    @Test
    void testLargeNumberOfColumns() throws Exception
    {
        NCube ncube = new NCube("BigDaddy")
        Axis axis = new Axis("numbers", AxisType.SET, AxisValueType.LONG, true, Axis.DISPLAY)
        ncube.addAxis(axis)
        def coord = [:]

        long start = System.nanoTime()
        for (int i = 0; i < 10000; i += 10)
        {
            RangeSet set = new RangeSet(i)
            Range range = new Range(i + 1, i + 4)
            set.add(range)
            axis.addColumn(set)
            coord.put("numbers", i)
            ncube.setCell(i * 2, coord)
        }

        long stop = System.nanoTime()

        double diff = (stop - start) / 1000.0  // usec
//        println("build 10,000 columns = " + (diff / 1000.0) + " ms")

        start = System.nanoTime()
        for (int i = 0; i < 10000; i += 10)
        {
            coord.numbers = i
            Integer ans = (Integer) ncube.getCell(coord)
            assertEquals(i * 2, ans.intValue())
        }
        stop = System.nanoTime()

        diff = (stop - start) / 1000.0  // usec
//        println("lookup 10,000 times large number of columns = " + (diff / 1000.0) + " ms")
    }

    @Test
    void testIntSetParsing()
    {
        Axis axis = new Axis('ages', AxisType.SET, AxisValueType.LONG, true, Axis.SORTED)
        RangeSet set = (RangeSet) axis.convertStringToColumnValue('10, 20, [50, 90], 100')
        assert set.size() == 4
        assert set.get(0) == 10
        assert set.get(1) == 20
        assert set.get(2) == new Range(50L, 90L)
        assert set.get(3) == 100

        set = (RangeSet) axis.convertStringToColumnValue('10, 20, [50, 90]')
        assert set.size() == 3
        assert set.get(0) == 10
        assert set.get(1) == 20
        assert set.get(2) == new Range(50L, 90L)

        set = (RangeSet) axis.convertStringToColumnValue('[50, 90], 94')
        assert set.size() == 2
        assert set.get(0) == new Range(50L, 90L)
        assert set.get(1) == 94

        set = (RangeSet) axis.convertStringToColumnValue('[50, 90]')
        assert set.size() == 1
        assert set.get(0) == new Range(50L, 90L)

        set = (RangeSet) axis.convertStringToColumnValue('[50, 90], [20, 60]')
        assert set.size() == 2
        assert set.get(0) == new Range(50L, 90L)
        assert set.get(1) == new Range(20L, 60L)

        set = (RangeSet) axis.convertStringToColumnValue('[50, 90], 789, [20, 60]')
        assert set.size() == 3
        assert set.get(0) == new Range(50L, 90L)
        assert set.get(1) == 789
        assert set.get(2) == new Range(20L, 60L)
    }

    @Test
    void testFloatSetParsing()
    {
        Axis axis = new Axis('ages', AxisType.SET, AxisValueType.BIG_DECIMAL, true, Axis.SORTED)
        RangeSet set = (RangeSet) axis.convertStringToColumnValue('10.1, 20, [50, 90.5], 100.1')
        assert set.size() == 4
        assert set.get(0) == 10.1
        assert set.get(1) == 20
        assert set.get(2) == new Range(50.0, 90.5)
        assert set.get(3) == 100.1
    }

    @Test
    void testDateSetParsing()
    {
        Axis axis = new Axis('dates', AxisType.SET, AxisValueType.DATE, true, Axis.SORTED)
        RangeSet set = (RangeSet) axis.convertStringToColumnValue('10 Dec 1995, 1995/12/25, [1996 dec 17, 2001-01-31], Jun 10th 2010')
        assert set.size() == 4
        assert set.get(0) == Converter.convert("10 Dec 1995", Date.class)
        assert set.get(1) == Converter.convert("25 Dec 1995", Date.class)
        assert set.get(2) == new Range(Converter.convert("1996 dec 17", Date.class), Converter.convert('2001-01-31', Date.class))
        assert set.get(3) == Converter.convert("Jun 10th 2010", Date.class)
    }

    @Test
    void testStringSetParsing()
    {
        Axis axis = new Axis('strings', AxisType.SET, AxisValueType.STRING, true, Axis.SORTED)
        RangeSet set = (RangeSet) axis.convertStringToColumnValue('10 Dec 1995, 1995/12/25, [1996 dec 17, 2001-01-31], Jun 10th 2010')
        assert set.size() == 4
        assert set.get(0) == "10 Dec 1995"
        assert set.get(1) == "1995/12/25"
        assert set.get(2) == new Range("1996 dec 17", '2001-01-31')
        assert set.get(3) == "Jun 10th 2010"

        set = (RangeSet) axis.convertStringToColumnValue('  The quick, "brown fox", [ "jumps over", the lazy dog], I\'m dead serious, ""this is quoted""')
        assert set.size() == 5
        assert set.get(0) == "The quick"
        assert set.get(1) == "brown fox"
        assert set.get(2) == new Range('jumps over', 'the lazy dog')
        assert set.get(3) == "I'm dead serious"
        assert set.get(4) == '"this is quoted"'
    }

    private static boolean isValidRange(Axis axis, Range range)
    {
        try
        {
            axis.addColumn(range);
            return true;
        }
        catch (AxisOverlapException e)
        {
            return false;
        }
    }
}
