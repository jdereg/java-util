package groovy;

import ncube.grv.exp.NCubeGroovyController;

class FooBarBazQuxController extends NCubeGroovyController
{
    def foo()
    {
        return 2;
    }

    def bar()
    {
        return foo() * 2;
    }

    def baz()
    {
        return bar() * 2;
    }

    def qux()
    {
        return baz() * 2;
    }
}