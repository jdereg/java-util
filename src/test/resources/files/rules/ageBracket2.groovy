import ncube.grv.exp.NCubeGroovyExpression

class Condition2 extends NCubeGroovyExpression
{
    def run()
    {
        input.age >= 18 && input.age < 40
    }
}