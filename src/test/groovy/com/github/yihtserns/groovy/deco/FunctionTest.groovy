/*
 * Copyright 2016 yihtserns.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.yihtserns.groovy.deco

import org.junit.Test
import static org.junit.Assert.fail

/**
 * @author yihtserns
 */
class FunctionTest {

    def cl = new GroovyClassLoader()

    @Test
    void 'can decorate method using one decorator'() {
        Class clazz = cl.parseClass("class Greeter { String greet(name) { return 'Hey ' + name } }")

        def decorate = { func, args -> func(*args) + '!' }
        def func = Function.create(clazz, 'greet', [String])
        clazz.metaClass.greet = { String name ->
            decorate(func.curry(delegate), [name])
        }

        def greeter = clazz.newInstance()
        assert greeter.greet('Noel') == 'Hey Noel!'
    }

    @Test
    void 'can decorate method using two decorators'() {
        Class clazz = cl.parseClass("class Greeter { String greet(name) { return 'Hey ' + name } }")

        exclaim: {
            def decorate = { func, args -> func(*args) + '!' }
            def func = Function.create(clazz, 'greet', [String])
            clazz.metaClass.greet = { String name ->
                decorate(func.curry(delegate), [name])
            }
        }
        question: {
            def decorate = { func, args -> func(*args) + '?' }
            def func = Function.create(clazz, 'greet', [String])
            clazz.metaClass.greet = { String name ->
                decorate(func.curry(delegate), [name])
            }
        }

        def greeter = clazz.newInstance()
        assert greeter.greet('Noel') == 'Hey Noel!?'
    }

    @Test
    void 'can decorate method with two params'() {
        Class clazz = cl.parseClass("class Greeter { String greet(name, id) { return 'Hey ' + name + ' ' + id } }")

        def decorate = { func, args -> func(*args) + '!' }
        def func = Function.create(clazz, 'greet', [String, int])
        clazz.metaClass.greet = { String name, int id ->
            decorate(func.curry(delegate), [name, id])
        }

        def greeter = clazz.newInstance()
        assert greeter.greet('Noel', 300) == 'Hey Noel 300!'
    }

    @Test
    void 'can get method name'() {
        Class clazz = cl.parseClass("class Greeter { String greet(name) { } }")

        def decorate = { func, args -> func.name }
        def func = Function.create(clazz, 'greet', [String])
        clazz.metaClass.greet = { String name ->
            decorate(func.curry(delegate), [name])
        }

        def greeter = clazz.newInstance()
        assert greeter.greet('Noel') == 'greet'
    }

    @Test
    void 'decorator works when decorated method is called from a subclass'() {
        Class clazz = cl.parseClass("""class Greeter {
            String greet(name) {
                return 'Hey ' + name
            }

            static class Greeter2 extends Greeter {
            }
        }""")

        def decorate = { func, args -> func(*args) + '!' }
        def func = Function.create(clazz, 'greet', [String])
        clazz.metaClass.greet = { String name ->
            decorate(func.curry(delegate), [name])
        }

        def clazz2 = clazz.getClasses()[0]

        def greeter2 = clazz2.newInstance()
        assert greeter2.greet('Noel') == 'Hey Noel!'
    }
}
