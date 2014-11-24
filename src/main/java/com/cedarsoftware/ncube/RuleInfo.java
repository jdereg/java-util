package com.cedarsoftware.ncube;

import com.cedarsoftware.util.CaseInsensitiveMap;
import com.cedarsoftware.util.CaseInsensitiveSet;
import groovy.util.MapEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This class contains information about the rule execution.
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
public class RuleInfo extends CaseInsensitiveMap<String, Object>
{
    public RuleInfo()
    {
        put(RuleMetaKeys.RULES_EXECUTED.name(), new ArrayList<MapEntry>());
    }

    /**
     * @return long indicating the number of conditions that fired (and therefore steps that executed).
     */
    public long getNumberOfRulesExecuted()
    {
        return getAxisBindings().size();
    }

    void ruleStopThrown()
    {
        put(RuleMetaKeys.RULE_STOP.name(), Boolean.TRUE);
    }

    /**
     * @return true if a RuleStop was thrown during rule execution
     */
    public boolean wasRuleStopThrown()
    {
        final String name = RuleMetaKeys.RULE_STOP.name();
        return containsKey(name) && (Boolean.TRUE.equals(get(name)));
    }

    public String getSystemOut()
    {
        if (containsKey(RuleMetaKeys.SYSTEM_OUT.name()))
        {
            return (String) get(RuleMetaKeys.SYSTEM_OUT.name());
        }
        return "";
    }

    public void setSystemOut(String out)
    {
        put(RuleMetaKeys.SYSTEM_OUT.name(), out);
    }

    public String getSystemErr()
    {
        if (containsKey(RuleMetaKeys.SYSTEM_ERR.name()))
        {
            return (String) get(RuleMetaKeys.SYSTEM_ERR.name());
        }
        return "";
    }

    public void setSystemErr(String err)
    {
        put(RuleMetaKeys.SYSTEM_ERR.name(), err);
    }

    public Set<String> getAssertionFailures()
    {
        if (containsKey(RuleMetaKeys.ASSERTION_FAILURES.name()))
        {
            return (Set<String>) get(RuleMetaKeys.ASSERTION_FAILURES.name());
        }
        Set<String> failures = new CaseInsensitiveSet<>();
        put(RuleMetaKeys.ASSERTION_FAILURES.name(), failures);
        return failures;

    }

    public void setAssertionFailures(Set<String> failures)
    {
        put(RuleMetaKeys.ASSERTION_FAILURES.name(), failures);
    }

    public Object getLastExecutedStatementValue()
    {
        if (containsKey(RuleMetaKeys.LAST_EXECUTED_STATEMENT.name()))
        {
            return get(RuleMetaKeys.LAST_EXECUTED_STATEMENT.name());
        }
        return null;
    }

    void setLastExecutedStatementValue(Object value)
    {
        put(RuleMetaKeys.LAST_EXECUTED_STATEMENT.name(), value);
    }

    public List<Binding> getAxisBindings()
    {
        if (containsKey(RuleMetaKeys.AXIS_BINDINGS.name()))
        {
            return (List<Binding>)get(RuleMetaKeys.AXIS_BINDINGS.name());
        }
        List<Binding> bindings = new ArrayList<>();
        put(RuleMetaKeys.AXIS_BINDINGS.name(), bindings);
        return bindings;
    }
}
