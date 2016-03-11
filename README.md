Groovy Decorator
================
Python-inspired method decorator for Groovy.

[![Groovy 2.4.6](https://img.shields.io/badge/groovy-2.4.6-blue.svg)](http://www.groovy-lang.org/) [![Java 1.7.0_75](https://img.shields.io/badge/java-1.7.0__75-red.svg)](https://java.com)

[![Build Status](https://travis-ci.org/yihtserns/groovy-decorator.svg?branch=master)](https://travis-ci.org/yihtserns/groovy-decorator)

Example
-------
```groovy
// Guard.groovy in its own project
import com.github.yihtserns.groovy.deco.MethodDecorator
import com.github.yihtserns.groovy.deco.Function
import org.codehaus.groovy.transform.GroovyASTTransformationClass
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target
import java.lang.annotation.ElementType

@MethodDecorator({ Function func ->
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
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
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
import com.github.yihtserns.groovy.deco.Function
import org.codehaus.groovy.transform.GroovyASTTransformationClass
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target
import java.lang.annotation.ElementType

@MethodDecorator({ Function func, Guard guard ->
    String[] prohibited = guard.against()

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
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
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

### Ad-hoc method decoration
```groovy
// SomeScript.groovy
import com.github.yihtserns.groovy.deco.Intercept
import com.github.yihtserns.groovy.deco.Function

class SomeOperation {

    @Intercept({ Function func, args ->
        String username = args[0]

        if (username == 'hacker') {
            throw new UnsupportedOperationException("hacker not allowed")
        } else {
            func(args) // Call original method
        }
    })
    public String doStuff(String username, int secretCode) {
        println "${username}: ${secretCode}"
    }
}

def op = new SomeOperation()
op.doStuff('good guy', 3) // prints 'good guy: 3'
op.doStuff('hacker', 1) // throws UnsupportedOperationException
```

Limitations
-----------
### Cannot work with @CompileStatic for Groovy version < 2.3.9
An exception will be thrown:
```groovy
java.lang.ArrayIndexOutOfBoundsException: Internal compiler error while compiling script1457454321240940813275.groovy
Method: MethodNode@1527752119[java.lang.Object doCall(java.lang.Object)]
Line -1, expecting casting to java.lang.Object but operand stack is empty
	at org.codehaus.groovy.classgen.asm.OperandStack.doConvertAndCast(OperandStack.java:323)
	at org.codehaus.groovy.classgen.asm.OperandStack.doGroovyCast(OperandStack.java:290)
...
```
