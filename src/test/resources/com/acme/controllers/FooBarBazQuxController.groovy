package com.acme.controllers

import ncube.grv.method.NCubeGroovyController

/**
 * Example Controller-type class.  The methods in this class
 * correspond to columns on a method axis.  When getCell()
 * or getCells() is called on the n-cube, the method specified
 * in the input map [method:'foo'] will be called.
 */
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