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
import org.junit.experimental.theories.DataPoint
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith

import org.codehaus.groovy.control.CompilerConfiguration

import static org.codehaus.groovy.control.customizers.builder.CompilerCustomizationBuilder.withConfig
import static org.junit.Assert.fail

/**
 * @author yihtserns
 */
@RunWith(Theories)
class DecoratorASTTransformationTest {

    @DataPoint
    public static def normalInstantiator = toInstantiator(new GroovyClassLoader())

    @DataPoint
    public static def compileStaticInstantiator = toInstantiator(
        new GroovyClassLoader(
            Thread.currentThread().contextClassLoader,
            withConfig(new CompilerConfiguration()) { ast(groovy.transform.CompileStatic) }))

    @Theory
    void 'can decorate'(toInstance) {
        def instance = toInstance("""package com.github.yihtserns.groovy.deco

            class Greeter {
            
                @Exclaim
                String greet() {
                    return 'Hi'
                }
            }
        """)

        assert instance.greet() == 'Hi!'
    }

    @Theory
    void 'can decorate method with one param'(toInstance) {
        def instance = toInstance("""package com.github.yihtserns.groovy.deco

            class Greeter {

                @Exclaim
                String greet(String name) {
                    return 'Hi ' + name
                }
            }
        """)

        assert instance.greet('Noel') == 'Hi Noel!'
    }

    @Theory
    public void 'can decorate method with three params'(toInstance) {
        def instance = toInstance("""package com.github.yihtserns.groovy.deco

            class Greeter {

                @Exclaim
                String greet(String name, int count) {
                    def list = []
                    count.times { list << 'Hi ' + name }

                    return list.join(' ')
                }
            }
        """)

        assert instance.greet('Noel', 3) == 'Hi Noel Hi Noel Hi Noel!'
    }

    @Theory
    void 'can use three decorators'(toInstance) {
        def instance = toInstance("""package com.github.yihtserns.groovy.deco

            class Greeter {

                @Honorific
                @Exclaim
                @Question
                String greet(String name) {
                    return 'Hi ' + name
                }
            }
        """)

        assert instance.greet('Noel') == 'Hi Mr. Noel!?'
    }

    @Theory
    void 'can get func metadata'(toInstance) {
        def instance = toInstance("""package com.github.yihtserns.groovy.deco

            class Greeter {

                @ReturnMetadata
                String greet(String name) {
                }

                @ReturnMetadata
                String farewell(String name, int count) {
                }

                @ReturnMetadata // returns number of characters in method name
                int bidding() {
                }
            }
        """)

        assert instance.greet('Noel') == 'greet: String'
        assert instance.farewell('Noel', 3) == 'farewell: String'
        assert instance.bidding() == 'bidding'.length()
    }

    @Theory
    void 'should throw when decorating annotation not annotated with @MethodDecorator'(toInstance) {
        try {
            toInstance("""package com.github.yihtserns.groovy.deco

                class Greeter {

                    @WithoutMethodDecorator
                    String greet(String name) {
                        return 'Hi ' + name
                    }
                }
            """)
            fail("Should throw exception")
        } catch (e) {
            assert e.message.contains("Annotation to decorate method must be annotated with com.github.yihtserns.groovy.deco.MethodDecorator."
                + " com.github.yihtserns.groovy.deco.WithoutMethodDecorator lacks this annotation.")
        }
    }

    @Theory
    void 'stack trace should show original location of code'(toInstance) {
        def instance = toInstance("""package com.github.yihtserns.groovy.deco

            class Greeter {

                @Exclaim
                String greet(String name) {
                    String msg = "Raising error for name: " + name
                    throw new RuntimeException(msg)
                }
            }
        """)

        try {
            instance.greet("Noel")
            fail("Should throw exception")
        } catch (ex) {
            def sanitizedEx = org.codehaus.groovy.runtime.StackTraceUtils.deepSanitize(ex)
            sanitizedEx.stackTrace[0].with {
                assert it.className == 'com.github.yihtserns.groovy.deco.Greeter'
                assert it.lineNumber == 8
            }
        }
    }

    /**
     * Just to add to the possible scenarios that may need to be supported.
     */
    @Theory
    void 'can mimic groovy.transform.Memoized'(toInstance) {
        def greeter = toInstance("""import com.github.yihtserns.groovy.deco.Exclaim
            import com.github.yihtserns.groovy.deco.Memoized

            class Greeter {
                int count = 1

                @Exclaim
                @Memoized
                String greet(String name) {
                    if (count >= 5) {
                        throw new RuntimeException("Cannot call method more than 5 times")
                    }
                    count++

                    return 'Hey ' + name
                }
            }""")

        10.times {
            assert greeter.greet('Noel') == 'Hey Noel!'
        }
    }

    @Theory
    void 'can reference elements in decorator annotation'(toInstance) {
        withoutMaxCacheSize: {
            def greeter = toInstance("""import com.github.yihtserns.groovy.deco.Exclaim
            import com.github.yihtserns.groovy.deco.Memoized

            class Greeter {
                int called = 0

                @Memoized
                int greet(int diff) {
                    called++
                    return diff
                }
            }""")

            greeter.greet(1)
            greeter.greet(2)
            greeter.greet(3)
            greeter.greet(1)
            greeter.greet(2)
            greeter.greet(3)
            assert greeter.called == 3

            greeter.greet(100) // Won't kick anyone out since cache has unlimited size
            assert greeter.called == 4

            greeter.greet(3)
            assert greeter.called == 4 // Verify value '3' still in cache

            greeter.greet(1)
            assert greeter.called == 4 // Verify value '1' still in cache
        }

        withMaxCacheSize: {
            def greeter = toInstance("""import com.github.yihtserns.groovy.deco.Exclaim
                import com.github.yihtserns.groovy.deco.Memoized

                class Greeter {
                    int called = 0

                    @Memoized(maxCacheSize = 3)
                    int greet(int diff) {
                        called++
                        return diff
                    }
                }""")

            greeter.greet(1)
            greeter.greet(2)
            greeter.greet(3)
            greeter.greet(1)
            greeter.greet(2)
            greeter.greet(3)
            assert greeter.called == 3

            greeter.greet(100) // Kicks out value '1' from cache
            assert greeter.called == 4

            greeter.greet(3)
            assert greeter.called == 4 // Verify value '3' still in cache

            greeter.greet(1)
            assert greeter.called == 5 // Verify value '1' not in cache anymore
        }
    }

    @Theory
    void 'can work with private method'(toInstance) {
        def instance = toInstance("""package com.github.yihtserns.groovy.deco

            class Greeter {

                @Exclaim
                private String greet() {
                    return 'Hi'
                }
            }
        """)

        assert instance.greet() == 'Hi!'
    }

    @Theory
    void 'can work with untyped parameter'(toInstance) {
        def instance = toInstance("""package com.github.yihtserns.groovy.deco

            class Greeter {

                @Exclaim
                String greet(name) {
                    return 'Hi ' + name
                }
            }
        """)

        assert instance.greet('Noel') == 'Hi Noel!'
    }

    @Theory
    void 'can handle array-type parameter'(toInstance) {
        def instance = toInstance("""package com.github.yihtserns.groovy.deco

            class Greeter {
                @Exclaim
                String greet(String[] names) {
                    return 'Hi '+ names.join(', ')
                }

                @Question
                String greet(String[][] namesGroups) {
                    return namesGroups.collect { String[] group ->
                        return 'Hi ' + group.join(', ') + '.'
                    }.join(' ')
                }
            }
        """)

        assert instance.greet(['Noel', 'Patrick', 'Stella'] as String[]) == 'Hi Noel, Patrick, Stella!'
        assert instance.greet([['Noel', 'Patrick'], ['Stella']] as String[][]) == 'Hi Noel, Patrick. Hi Stella.?'
    }

    @Theory
    void 'can handle multiple methods with parameter with same class name'(toInstance) {
        def instance = toInstance("""package com.github.yihtserns.groovy.deco

            class Greeter {
                @Exclaim
                String run(x.Input input) {
                    return input.class.name
                }

                @Question
                String run(y.Input input) {
                    return input.class.name
                }

                @Intercept({ Function func, args -> func(args).toString() + '.' })
                String run(z.Input input) {
                    return input.class.name
                }
            }
        """)

        assert instance.run(new x.Input()) == 'x.Input!'
        assert instance.run(new y.Input()) == 'y.Input?'
        assert instance.run(new z.Input()) == 'z.Input.'
    }

    private static Closure toInstantiator(GroovyClassLoader cl) {
        return { classScript ->
            def clazz = cl.parseClass(classScript)

            return clazz.newInstance()
        }
    }
}
