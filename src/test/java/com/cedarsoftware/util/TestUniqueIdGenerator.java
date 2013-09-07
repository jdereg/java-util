package com.cedarsoftware.util;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Ensure that the UniqueIdGenerator never hands out the
 * same ID, regardless of how fast or how many times it is
 * called.
 *
 * @author John DeRegnaucourt
 */
public class TestUniqueIdGenerator
{
    @Test
    public void testUniqueIdGeneration() throws Exception
    {
        Set ids = new HashSet();

        for (int i=0; i < 100000; i++)
        {
            ids.add(UniqueIdGenerator.getUniqueId());
        }
        assertTrue(ids.size() == 100000);
    }
}
