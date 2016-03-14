Groovy Decorator
================
Python-inspired method decorator for Groovy.

[![Groovy 2.4.6](https://img.shields.io/badge/groovy-2.4.6-blue.svg)](http://www.groovy-lang.org/) [![Java 1.7.0_75](https://img.shields.io/badge/java-1.7.0__75-red.svg)](https://java.com)

[![Build Status](https://travis-ci.org/yihtserns/groovy-decorator.svg?branch=master)](https://travis-ci.org/yihtserns/groovy-decorator)
[![Maven Central Releases](https://img.shields.io/badge/maven--central-releases-blue.svg)](http://search.maven.org/#search|ga|1|g:"com.github.yihtserns" AND a:"groovy-decorator")

Example
-------
```groovy
// Guard.groovy in its own project
import com.github.yihtserns.groovy.decorator.MethodDecorator
import com.github.yihtserns.groovy.decorator.Function
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
@GroovyASTTransformationClass("com.github.yihtserns.groovy.decorator.DecoratorASTTransformation")
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
import com.github.yihtserns.groovy.decorator.MethodDecorator
import com.github.yihtserns.groovy.decorator.Function
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
@GroovyASTTransformationClass("com.github.yihtserns.groovy.decorator.DecoratorASTTransformation")
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
import com.github.yihtserns.groovy.decorator.Intercept
import com.github.yihtserns.groovy.decorator.Function

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

### Sharing ad-hoc method decoration
```groovy
// SomeScript.groovy
import com.github.yihtserns.groovy.decorator.Intercept
import com.github.yihtserns.groovy.decorator.Function

class SomeOperation {

    @Intercept(BlockHacker)
    public String doStuff(String username, int secretCode) {
        println "${username}: ${secretCode}"
    }

    @Intercept(BlockHacker)
    public String doAnotherStuff(String username, int secretCode) {
        println "${username}: ${secretCode}"
    }

    private static class BlockHacker extends Closure {

        BlockHacker(owner, thisObject) {
            super(owner, thisObject)
        }

        def doCall(Function func, args) {
            String username = args[0]

            if (username == 'hacker') {
                throw new UnsupportedOperationException("hacker not allowed")
            } else {
                func(args) // Call original method
            }
        }
    }
}

def op = new SomeOperation()

op.doStuff('admin', 10) // prints 'admin: 10'
op.doStuff('good guy', 3) // prints 'good guy: 3'
op.doStuff('hacker', 1) // throws UnsupportedOperationException

op.doAnotherStuff('admin', 10) // prints 'admin: 10'
op.doAnotherStuff('good guy', 3) // prints 'good guy: 3'
op.doAnotherStuff('hacker', 1) // throws UnsupportedOperationException
```

API
---
&nbsp; | Description
------ | -----------
`Function.name` : `String` | Name of the decorated method.
`Function.returnType` : `Class<?>` | Return type of the decorated method.

The way it works is similar to `groovy.transform.Memoized`, in that it turns:
```groovy
class MyClass {

    @Decorator1(el1 = val1, el2 = val2,... elN = valN)
    @Decorator2
    boolean method(String x, int y) {
        ...
    }

    @Decorator1
    String method(x.Input input1, y.Input input2) {
        ...
    }

    @Decorator1
    String method(y.Input input1, x.Input input2) {
        ...
    }
}
```
into:
```groovy
class MyClass {

    private Function decorating$methodStringint = Function.create({ String x, int y -> decorated$method(x, y) }, boolean)
                                                          .decorateWith(
                                                              /** Decorator1 annotated on method(String, int) **/,
                                                              /** Decorator1's decorator closure **/)
                                                          .decorateWith(
                                                              /** Decorator2 annotated on method(String, int) **/,
                                                              /** Decorator2's decorator closure **/)
    private Function decorating$methodInputInput = Function.create({ x.Input input1, y.Input input2 -> decorated$method(input1, input2) }, String)
                                                           .decorateWith(
                                                               /** Decorator1 annotated on method(x.Input, y.Input) **/,
                                                               /** Decorator1's decorator closure **/)
    private Function _decorating$methodInputInput = Function.create({ y.Input input1, x.Input input2 -> decorated$method(input1, input2) }, String)
                                                            .decorateWith(
                                                                /** Decorator1 annotated on method(y.Input, x.Input) **/,
                                                                /** Decorator1's decorator closure **/)

    @Decorator1(el1 = val1, el2 = val2,... elN = valN)
    @Decorator2
    boolean method(String x, int y) {
        decorating$methodStringint([x, y])
    }

    private boolean decorated$method(String x, int y) {
        ...
    }

    @Decorator1
    String method(x.Input input1, y.Input input2) {
        decorating$methodInputInput([input1, input2])
    }

    private String decorated$method(x.Input input1, y.Input input2) {
        ...
    }

    @Decorator1
    String method(y.Input input1, x.Input input2) {
        _decorating$methodInputInput([input1, input2])
    }

    private String decorated$method(y.Input input1, x.Input input2) {
        ...
    }
}
```

### Multiple decoration ordering
```groovy
@Decorator1 // Decorate with this first, then
@Decorator2 // Decorate with this, then
@Decorator3 // Decorate with this
String getSecret(user, secretId) {
  ...
}
```

Example:
```groovy
@Allow(Role.FIELD_AGENT)
@CacheResult
@Log(entry=true, exit=true)
String getSecret(user, secretId) {
  ...
}

...
getSecret('707', 707) // -> Log -> CacheResult -> Allow -> greet(user, secretId)
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
