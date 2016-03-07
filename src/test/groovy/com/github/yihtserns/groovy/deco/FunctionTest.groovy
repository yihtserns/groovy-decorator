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

/**
 * @author yihtserns
 */
class FunctionTest {

    def cl = new GroovyClassLoader()

    @Test
    void 'can decorate method using one decorator'() {
        Class clazz = cl.parseClass("""import com.github.yihtserns.groovy.deco.Function
            class Greeter {
                {
                    def decorator = { func ->
                        { args -> func(args) + '!' }
                    }
                    def func = Function.create(this, 'greet', String, [String] as Class[]).decorateWith(decorator)
                    this.metaClass {
                        greet { String name ->
                            func([name])
                        }
                    }
                }

                String greet(name) {
                    return 'Hey ' + name
                }
            }""")

        def greeter = clazz.newInstance()
        assert greeter.greet('Noel') == 'Hey Noel!'
    }

    @Test
    void 'can decorate method using two decorators'() {
        Class clazz = cl.parseClass("""import com.github.yihtserns.groovy.deco.Function
            class Greeter {
                {
                    exclaim: {
                        def decorator = { func ->
                            { args -> func(args) + '!' }
                        }
                        def func = Function.create(this, 'greet', String, [String] as Class[]).decorateWith(decorator)
                        this.metaClass {
                            greet { String name ->
                                func([name])
                            }
                        }
                    }
                    question: {
                        def decorator = { func ->
                            { args -> func(args) + '?' }
                        }
                        def func = Function.create(this, 'greet', String, [String] as Class[]).decorateWith(decorator)
                        this.metaClass {
                            greet { String name ->
                                func([name])
                            }
                        }
                    }
                }

                String greet(name) {
                    return 'Hey ' + name
                }
            }""")

        def greeter = clazz.newInstance()
        assert greeter.greet('Noel') == 'Hey Noel!?'
    }

    @Test
    void 'can decorate method with two params'() {
        Class clazz = cl.parseClass("""import com.github.yihtserns.groovy.deco.Function
            class Greeter {
                {
                    def decorator = { func ->
                        { args -> func(args) + '!' }
                    }
                    def func = Function.create(this, 'greet', String, [String, int] as Class[]).decorateWith(decorator)
                    this.metaClass {
                        greet { String name, int id ->
                            func([name, id])
                        }
                    }
                }

                String greet(name, id) {
                    return 'Hey ' + name + ' ' + id
                }
            }""")

        def greeter = clazz.newInstance()
        assert greeter.greet('Noel', 300) == 'Hey Noel 300!'
    }

    @Test
    void 'can get method name'() {
        Class clazz = cl.parseClass("""import com.github.yihtserns.groovy.deco.Function
            class Greeter {
                {
                    def decorator = { func ->
                        { args -> func.name }
                    }
                    def func = Function.create(this, 'greet', String, [String] as Class[]).decorateWith(decorator)
                    this.metaClass {
                        greet { String name ->
                            func([name])
                        }
                    }
                }

                String greet(name) {

                }
            }""")

        def greeter = clazz.newInstance()
        assert greeter.greet('Noel') == 'greet'
    }

    @Test
    void 'decorator works when decorated method is called from a subclass'() {
        Class clazz = cl.parseClass("""import com.github.yihtserns.groovy.deco.Function
            class Greeter {
                {
                    def decorator = { func ->
                        { args -> func(args) + '!' }
                    }
                    def func = Function.create(this, 'greet', String, [String] as Class[]).decorateWith(decorator)
                    this.metaClass {
                        greet { String name ->
                            func([name])
                        }
                    }
                }

                String greet(name) {
                    return 'Hey ' + name
                }

                static class Greeter2 extends Greeter {
                }
            }""")

        def clazz2 = clazz.getClasses()[0]

        def greeter2 = clazz2.newInstance()
        assert greeter2.greet('Noel') == 'Hey Noel!'
    }

    @Test
    void "should return original method's return type"() {
        Class clazz = cl.parseClass("""import com.github.yihtserns.groovy.deco.Function
            class Greeter {
                {
                    doNothing: {
                        def decorator = { func -> { args -> } }
                        def func = Function.create(this, 'greet', void, [String] as Class[]).decorateWith(decorator)
                        this.metaClass {
                            greet { String name ->
                                func([name])
                            }
                        }
                    }
                    getReturnType: {
                        def decorator = { func -> { args -> func.returnType } }
                        def func = Function.create(this, 'greet', void, [String] as Class[]).decorateWith(decorator)
                        this.metaClass {
                            greet { String name ->
                                func([name])
                            }
                        }
                    }
                }

                void greet(name) {
                }
            }""")

        def greeter = clazz.newInstance()
        assert greeter.greet('Noel') == void
    }

    @Test
    void 'can chain decoration'() {
        def func = { name -> "Hey $name" }
        assert func('Noel') == 'Hey Noel'

        func = Function.create(func, 'call', String)
        assert func(['Noel']) == 'Hey Noel'

        func = func.decorateWith { f -> { args -> "${f(args)}!" } }
        assert func(['Noel']) == 'Hey Noel!'

        func = func.decorateWith { f -> { args -> "${f(args)}?" } }
        assert func(['Noel']) == 'Hey Noel!?'

        func = func.decorateWith { f -> { args -> "$f.name : $f.returnType.simpleName -> ${f(args)}" } }
        assert func(['Noel']) == 'call : String -> Hey Noel!?'
    }

    @Test
    void 'can pass object to decorator'() {
        def func = { name -> "Hey $name" }
        func = Function.create(func, 'call', String)
        
        def withHonorific = func.decorateWith('Mr.') { f, title ->
            { args ->
                args[0] = title + " " + args[0]

                f(args)
            }
        }
        assert withHonorific(['Noel']) == 'Hey Mr. Noel'
    }

    @Test
    void "don't pass object to decorator if no param declared for the it"() {
        def func = { name -> "Hey $name" }
        func = Function.create(func, 'call', String)

        def unadorned = func.decorateWith('Mr.') { f -> { args -> f(args) } }
        assert unadorned(['Noel']) == 'Hey Noel'
    }
}
