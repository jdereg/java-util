package com.cedarsoftware.ncube

import org.junit.Test

import java.util.regex.Matcher

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse

/**
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
class TestRegexes
{
    @Test
    void testLatLongRegex()
    {
        Matcher m = Regexes.valid2Doubles.matcher '25.8977899,56.899988'
        assert m.matches()
        assert '25.8977899' == m.group(1)
        assert '56.899988' == m.group(2)

        m = Regexes.valid2Doubles.matcher ' 25.8977899, 56.899988 '
        assert m.matches()
        assert '25.8977899' == m.group(1)
        assert '56.899988' == m.group(2)

        m = Regexes.valid2Doubles.matcher '-25.8977899,-56.899988 '
        assert m.matches()
        assert '-25.8977899' == m.group(1)
        assert '-56.899988' == m.group(2)

        m = Regexes.valid2Doubles.matcher 'N25.8977899, 56.899988 '
        assert !m.matches()

        m = Regexes.valid2Doubles.matcher '25.8977899, E56.899988 '
        assert !m.matches()

        m = Regexes.valid2Doubles.matcher '25., 56.899988 '
        assert !m.matches()

        m = Regexes.valid2Doubles.matcher '25.891919, 56. '
        assert !m.matches()
    }

    @Test
    void testNCubeNameParser()
    {
        String name = "['Less than \$10,000':['startIncurredAmount':'0','endIncurredAmount':'10000'],'\$10,000 - \$25,000':['startIncurredAmount':'10000','endIncurredAmount':'25000'],'\$25,000 - \$50,000':['startIncurredAmount':'25000','endIncurredAmount':'50000'],'More than \$50,000':['startIncurredAmount':'50000','endIncurredAmount':'0']]";
        Matcher m = Regexes.groovyRelRefCubeCellPatternA.matcher(name)
        assertFalse(m.find())

        m = Regexes.groovyRelRefCubeCellPattern.matcher(name)
        assertFalse(m.find())

        m = Regexes.groovyAbsRefCubeCellPattern.matcher(name)
        assertFalse(m.find())

        m = Regexes.groovyAbsRefCubeCellPatternA.matcher(name)
        assertFalse(m.find())

        name = "@Foo([:])"

        m = Regexes.groovyRelRefCubeCellPattern.matcher(name)
        m.find()
        assertEquals("Foo", m.group(2))

        name = "@Foo([:])"
        m = Regexes.groovyRelRefCubeCellPattern.matcher(name)
        m.find()
        assertEquals("Foo", m.group(2))

        name = "\$Foo([alpha:'bravo'])"
        m = Regexes.groovyAbsRefCubeCellPattern.matcher(name)
        m.find()
        assertEquals("Foo", m.group(2))

        name = "\$Foo[:]"
        m = Regexes.groovyAbsRefCubeCellPatternA.matcher(name)
        m.find()
        assertEquals("Foo", m.group(2))
    }

    @Test
    void testPositiveRelativeCubeBracketAnnotations()
    {
        def name = "@PackageScope[a:1]" // Legal because of square brackets
        def m = Regexes.groovyRelRefCubeCellPatternA.matcher(name)
        assert m.find()

        name = "@ PackageScope [a:1, b:2, c:'three', 'd':\"four\"]" // Legal because of square brackets
        m = Regexes.groovyRelRefCubeCellPatternA.matcher(name)
        assert m.find()

        name = "@PackageScope[:]" // Legal because of square brackets
        m = Regexes.groovyRelRefCubeCellPatternA.matcher(name)
        assert m.find()

        name = "@Very_Ugly.Name[:]" // Legal because of square brackets and safe name
        m = Regexes.groovyRelRefCubeCellPatternA.matcher(name)
        assert m.find()
    }

    @Test
    void testNegativeRelativeCubeBracketAnnotations()
    {
        def name = "@PackageScope[]" // Illegal because of no colon within brackets
        def m = Regexes.groovyRelRefCubeCellPatternA.matcher(name)
        assertFalse m.find()

        name = "@Foo[map]" // Illegal because of no colon within brackets
        m = Regexes.groovyRelRefCubeCellPatternA.matcher(name)
        assertFalse m.find()
    }

    @Test
    void testPositiveRelativeCubeParenAnnotations()
    {
        def name = "@PostAuthorize(jim)"    // valid, and jim better be a map
        def m = Regexes.groovyRelRefCubeCellPattern.matcher(name)
        assert m.find()

        name = "@PostAuthorize( jim )"    // valid, and jim better be a map
        m = Regexes.groovyRelRefCubeCellPattern.matcher(name)
        assert m.find()

        name = "@PreAuthorizeStart([foo:bar])"
        m = Regexes.groovyRelRefCubeCellPattern.matcher(name)
        assert m.find()

        name = "@PreAuthorizeStart( [ foo : bar ] )"
        m = Regexes.groovyRelRefCubeCellPattern.matcher(name)
        assert m.find()

        name = "@PreAuthorizeStart([foo:'bar'])"
        m = Regexes.groovyRelRefCubeCellPattern.matcher(name)
        assert m.find()

        name = "@PreAuthorizeStart([foo:\"bar\"])"
        m = Regexes.groovyRelRefCubeCellPattern.matcher(name)
        assert m.find()

        name = '@PreAuthorizeStart([\'foo\':com.foo.bar.Qux$b])'
        m = Regexes.groovyRelRefCubeCellPattern.matcher(name)
        assert m.find()

        name = '@ PreAuthorizeStart ( [  "foo"  :  com.foo.bar.Qux$b  ]  )'
        m = Regexes.groovyRelRefCubeCellPattern.matcher(name)
        assert m.find()
    }

    @Test
    void testNegativeRelativeCubeParenAnnotations()
    {
        def name = "@PreAuthorize(jim)"    // Not allowed to use PreAuthorize
        def m = Regexes.groovyRelRefCubeCellPattern.matcher(name)
        assertFalse m.find()

        name = "@PreAuthorize(resourceURI = \"/resource/submission\", actionURI = \"/action/view\")"
        m = Regexes.groovyRelRefCubeCellPattern.matcher(name)
        assertFalse m.find()

        name = "@ControllerClass"
        m = Regexes.groovyRelRefCubeCellPattern.matcher(name)
        assertFalse m.find()

        name = "@PackageScope( )"
        m = Regexes.groovyRelRefCubeCellPattern.matcher(name)
        assertFalse m.find()

        name = "@PackageScope(METHODS)"
        m = Regexes.groovyRelRefCubeCellPattern.matcher(name)
        assertFalse m.find()

        name = "@SuppressWarnings(\"foo\")"
        m = Regexes.groovyRelRefCubeCellPattern.matcher(name)
        assertFalse m.find()
    }
}
