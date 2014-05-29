package com.cedarsoftware.ncube.executor;

import com.cedarsoftware.ncube.CommandCell;

import java.util.Map;

/**
 * Created by jderegnaucourt on 2014/05/29.
 */
public class DefaultExecutor implements Executor
{
    public Object execute(Object c, Map<String, Object> ctx)
    {
        return c;
    }

    public Object executeCommand(CommandCell c, Map<String, Object> ctx)
    {
        return c.run(ctx);
    }
}
