package com.cedarsoftware.ncube.executor;

import com.cedarsoftware.ncube.CommandCell;

import java.util.Map;

/**
 * Created by jderegnaucourt on 2014/05/29.
 */
public interface Executor
{
    Object execute(Object c, Map<String, Object> ctx);
    Object executeCommand(CommandCell c, Map<String, Object> ctx);
}
