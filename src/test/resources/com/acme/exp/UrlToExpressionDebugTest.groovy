package com.acme.exp

import ncube.grv.exp.NCubeGroovyExpression

class UrlToExpressionDebugTest extends NCubeGroovyExpression
{
    def run()
    {
        if (input.get('age') == null)
        {
            return -1
        }
        LibCode libCode = new LibCode();
        libCode.pow(input.age, 2);
    }
}
