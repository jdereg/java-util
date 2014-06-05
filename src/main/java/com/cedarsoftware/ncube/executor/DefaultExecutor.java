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

    public Object executeCommand(CommandCell command, Map<String, Object> ctx)
    {
        command.failOnErrors();

        String url = command.getUrl();
        Object cmd = command.getCmd();

        if (url != null && cmd == null)
        {
            command.expandUrl(url, ctx);
            cmd = command.fetch(ctx);
        }

        command.prepare(cmd, ctx);
        return command.execute(cmd, ctx);
    }

}
