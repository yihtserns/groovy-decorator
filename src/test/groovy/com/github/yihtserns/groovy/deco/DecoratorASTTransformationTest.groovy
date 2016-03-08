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
class DecoratorASTTransformationTest {

    def cl = new GroovyClassLoader()

    @Test
    void 'can decorate'() {
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

    @Test
    public void 'can decorate method with one param'() {
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

    @Test
    public void 'can decorate method with three params'() {
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

    @Test
    public void 'can use three decorators'() {
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

    @Test
    public void 'can get func metadata'() {
        def instance = toInstance("""package com.github.yihtserns.groovy.deco

            class Greeter {

                @ReturnMetadata
                String greet(String name) {
                }

                @ReturnMetadata
                String farewell(String name, int count) {
                }

                @ReturnMetadata
                int bid() {
                }
            }
        """)

        assert instance.greet('Noel') == ['greet', String]
        assert instance.farewell('Noel', 3) == ['farewell', String]
        assert instance.bid() == ['bid', int]
    }

    @Test
    public void 'should throw when decorating annotation not annotated with @MethodDecorator'() {
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

    @Test
    public void 'stack trace should show original location of code'() {
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
                assert it.methodName == 'greet'
                assert it.lineNumber == 8
            }
        }
    }

    /**
     * Just to add to the possible scenarios that may need to be supported.
     */
    @Test
    public void 'can mimic groovy.transform.Memoized'() {
        Class clazz = cl.parseClass("""import com.github.yihtserns.groovy.deco.Exclaim
            import com.github.yihtserns.groovy.deco.Memoized

            class Greeter {
                int count = 1

                @Exclaim
                @Memoized
                String greet(name) {
                    if (count >= 5) {
                        throw new RuntimeException("Cannot call method more than 5 times")
                    }
                    count++

                    return 'Hey ' + name
                }
            }""")

        def greeter = clazz.newInstance()
        10.times {
            assert greeter.greet('Noel') == 'Hey Noel!'
        }
    }

    @Test
    public void 'can reference elements in decorator annotation'() {
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

    @Test
    public void 'can work with private method'() {
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

    @Test
    public void 'can work with method level static compilation'() {
        def instance = toInstance("""package com.github.yihtserns.groovy.deco

            class Greeter {

                @groovy.transform.CompileStatic
                @Exclaim
                String greet() {
                    return 'Hi'
                }
            }
        """)

        assert instance.greet() == 'Hi!'
    }

    @Test
    public void 'can work with class level static compilation'() {
        notImplementedYet {
            def instance = toInstance("""package com.github.yihtserns.groovy.deco

                @groovy.transform.CompileStatic
                class Greeter {

                    @Exclaim
                    String greet() {
                        return 'Hi'
                    }
                }
            """)

            assert instance.greet() == 'Hi!'
        }
    }

    private static void notImplementedYet(run) {
        try {
            run()
        } catch (Throwable e) {
            org.junit.Assume.assumeNoException(e)
        }
        throw new junit.framework.AssertionFailedError("Method is marked as 'not yet implemented' but passes unexpectedly")
    }

    def toInstance(String classScript) {
        def clazz = cl.parseClass(classScript)

        return clazz.newInstance()
    }
}
