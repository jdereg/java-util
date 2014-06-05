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
        Object res = command.getOperableCmd();

        if (url != null && res == null)
        {
            command.expandUrl(url, ctx);
            res = command.fetch(ctx);
            command.cache(res);
        }

        command.prepare(res, ctx);
        return command.execute(res, ctx);
    }

}
