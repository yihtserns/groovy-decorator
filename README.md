Groovy Decorator
================
Python-inspired method decorator for Groovy.

Tested on
---------
- Groovy 2.4.6
- Java 1.7.0_75

Example
-------
```groovy
// Guard.groovy in its own project
import com.github.yihtserns.groovy.deco.MethodDecorator
import org.codehaus.groovy.transform.GroovyASTTransformationClass

@MethodDecorator({ func -> {
    { args ->
        String username = args[0]

        if (username == 'hacker') {
            throw new UnsupportedOperationException("Hacker not allowed")
        } else {
            func(args) // Call original method
        }
    }
})
@GroovyASTTransformationClass("com.github.yihtserns.groovy.deco.DecoratorASTTransformation")
@interface Guard {
}
```

```groovy
// SomeScript.groovy
class SomeOperation {

    @Guard
    public String doStuff(String username, int secretCode) {
        println "${username}: ${secretCode}"
    }
}

def op = new SomeOperation()
op.doStuff('good guy', 3) // prints 'good guy: 3'
op.doStuff('hacker', 1) // throws UnsupportedOperationException
```
