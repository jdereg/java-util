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
        if (c.hasErrors()) {
            // If the cell failed to compile earlier, do not keep trying to recompile or run it.
            throw new IllegalStateException(c.getErrorMessage());
        }

        String url = c.getUrl();
        Object cmd = c.getCmd();

        if (url != null && !c.hasBeenFetched())
        {
            if (!c.isExpanded())
            {
                c.expandUrl(url, ctx);
            }

            cmd = c.fetch(ctx);
            c.cache(cmd);
        }

        c.prepare(cmd, ctx);
        return c.execute(cmd, ctx);
    }

}
