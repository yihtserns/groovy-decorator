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

@MethodDecorator({ func ->
    return { args ->
        String username = args[0]

        if (username == 'hacker') {
            throw new UnsupportedOperationException("hacker not allowed")
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

### Using annotation elements
```groovy
// Guard.groovy in its own project
import com.github.yihtserns.groovy.deco.MethodDecorator
import org.codehaus.groovy.transform.GroovyASTTransformationClass

@MethodDecorator({ func, guard ->
    String[] prohibited = guard.against ?: ['hacker']

    return { args ->
        String username = args[0]

        if (prohibited.contains(username)) {
            throw new UnsupportedOperationException("$username not allowed")
        } else {
            func(args) // Call original method
        }
    }
})
@GroovyASTTransformationClass("com.github.yihtserns.groovy.deco.DecoratorASTTransformation")
@interface Guard {

    String[] against() default ['hacker']
}
```

```groovy
// SomeScript.groovy
class SomeOperation {

    @Guard
    public String doStuff(String username, int secretCode) {
        println "${username}: ${secretCode}"
    }

    @Guard(against = ['good guy', 'hacker'])
    public String doSuperSensitiveStuff(String username, int secretCode) {
        println "${username}: ${secretCode}"
    }
}

def op = new SomeOperation()

op.doStuff('admin', 10) // prints 'admin: 10'
op.doStuff('good guy', 3) // prints 'good guy: 3'
op.doStuff('hacker', 1) // throws UnsupportedOperationException

op.doSuperSensitiveStuff('admin', 10) // prints 'admin: 10'
op.doSuperSensitiveStuff('good guy', 3) // throws UnsupportedOperationException
op.doSuperSensitiveStuff('hacker', 1) // throws UnsupportedOperationException
```

Limitations
-----------
### Cannot work with @CompileStatic
An exception will be thrown:
```groovy
General error during class generation: size==0

java.lang.ArrayIndexOutOfBoundsException: size==0
	at org.codehaus.groovy.classgen.asm.OperandStack.getTopOperand(OperandStack.java:729)
	at org.codehaus.groovy.classgen.asm.BinaryExpressionHelper.evaluateEqual(BinaryExpressionHelper.java:306)
...
```

Gotcha
------
### Does not work on private method for Groovy version >= 2.4.0
Due to [GROOVY-7368](https://issues.apache.org/jira/browse/GROOVY-7368).
