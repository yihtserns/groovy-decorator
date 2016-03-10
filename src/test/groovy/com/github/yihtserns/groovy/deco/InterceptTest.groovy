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

import org.codehaus.groovy.control.CompilerConfiguration
import org.junit.Test

import static org.codehaus.groovy.control.customizers.builder.CompilerCustomizationBuilder.withConfig

/**
 * @author yihtserns
 */
class InterceptTest {

    @Test
    void 'can decorate method on the fly'() {
        def cl = new GroovyClassLoader()
        Class clazz = cl.parseClass("""import com.github.yihtserns.groovy.deco.Intercept
            class Greeter {

                @Intercept({ func, args -> func(args) + '!' })
                String greet(name) {
                    return 'Hey ' + name
                }
            }""")

        def greeter = clazz.newInstance()
        assert greeter.greet('Noel') == 'Hey Noel!'
    }

    @Test
    void 'can decorate method on the fly when statically compiled'() {
        def cl = new GroovyClassLoader(
            Thread.currentThread().contextClassLoader,
            withConfig(new CompilerConfiguration()) { ast(groovy.transform.CompileStatic) })

        Class clazz = cl.parseClass("""import com.github.yihtserns.groovy.deco.Intercept
            import com.github.yihtserns.groovy.deco.Function

            class Greeter {

                @Intercept({ Function func, args -> String.valueOf(func(args)) + '!' })
                String greet(name) {
                    return 'Hey ' + name
                }
            }""")

        def greeter = clazz.newInstance()
        assert greeter.greet('Noel') == 'Hey Noel!'
    }
}
