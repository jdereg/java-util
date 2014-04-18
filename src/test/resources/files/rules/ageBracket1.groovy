import ncube.grv.exp.NCubeGroovyExpression

class Condition extends NCubeGroovyExpression
{
    def run()
    {
        input.age < 18
    }
}