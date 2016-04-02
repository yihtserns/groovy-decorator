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

package com.github.yihtserns.groovy.decorator

import com.squareup.burst.BurstJUnit4
import com.squareup.burst.annotation.Burst
import org.codehaus.groovy.control.CompilerConfiguration
import org.junit.Test
import org.junit.runner.RunWith

import static org.codehaus.groovy.control.customizers.builder.CompilerCustomizationBuilder.withConfig

/**
 * @author yihtserns
 */
@RunWith(BurstJUnit4)
class InterceptTest {

    private enum Compilation {

        NORMAL(cl: new GroovyClassLoader()),
        STATIC(cl: new GroovyClassLoader(
                Thread.currentThread().contextClassLoader,
                withConfig(new CompilerConfiguration()) { ast(groovy.transform.CompileStatic) }))

        ClassLoader cl

        Class parseClass(String classScript) {
            return cl.parseClass(classScript)
        }
    }

    @Burst Compilation compilation

    @Test
    void 'can decorate method on the fly'() {
        Class clazz = compilation.parseClass("""import com.github.yihtserns.groovy.decorator.Intercept
            import com.github.yihtserns.groovy.decorator.Function

            class Greeter {

                @Intercept({ Function func, args -> "\${func(args)}!" })
                String greet(name) {
                    return 'Hey ' + name
                }
            }""")

        def greeter = clazz.newInstance()
        assert greeter.greet('Noel') == 'Hey Noel!'
    }

    @Test
    void "closure can access class' field"() {
        Class clazz = compilation.parseClass("""import com.github.yihtserns.groovy.decorator.Intercept
            class Greeter {
                int count = 0

                @Intercept({ func, args -> count++ })
                void doNothing() {
                }
            }""")

        def greeter = clazz.newInstance()

        greeter.doNothing()
        assert greeter.count == 1
        
        greeter.doNothing()
        assert greeter.count == 2
    }

    @Test
    public void 'can share method decoration'() {
        Class clazz = compilation.parseClass("""import com.github.yihtserns.groovy.decorator.Intercept
            import com.github.yihtserns.groovy.decorator.Function

            class Greeter {

                @Intercept(PrependNonsense)
                String greet(name) {
                    return 'Hey ' + name
                }

                @Intercept(PrependNonsense)
                String greet() {
                    return 'Hi'
                }

                private static class PrependNonsense extends Closure {
                    PrependNonsense(owner, thisObject) {
                        super(owner, thisObject)
                    }

                    String doCall(Function func, args) {
                        "Nonsense: \${func(args)}"
                    }
                }
            }""")

        def greeter = clazz.newInstance()

        assert greeter.greet('Noel') == 'Nonsense: Hey Noel'
        assert greeter.greet() == 'Nonsense: Hi'
    }

    @Test
    void "closure class can access class' field"() {
        Class clazz = compilation.parseClass("""import com.github.yihtserns.groovy.decorator.Intercept
            class Greeter {
                int count = 0

                @Intercept(ReturnIncrementedCount)
                void doNothing() {
                }

                private static class ReturnIncrementedCount extends Closure {
                    ReturnIncrementedCount(owner, thisObject) {
                        super(owner, thisObject)
                    }

                    def doCall(func, args) {
                        count++
                    }
                }
            }""")

        def greeter = clazz.newInstance()

        greeter.doNothing()
        assert greeter.count == 1

        greeter.doNothing()
        assert greeter.count == 2
    }
}
