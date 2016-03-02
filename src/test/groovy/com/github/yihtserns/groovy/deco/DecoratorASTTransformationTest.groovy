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
    public void 'can get method name'() {
        def instance = toInstance("""package com.github.yihtserns.groovy.deco

            class Greeter {

                @ReturnMethodName
                String greet(String name) {
                }

                @ReturnMethodName
                String farewell(String name, int count) {
                }

                @ReturnMethodName
                String bid() {
                }
            }
        """)

        assert instance.greet('Noel') == 'greet'
        assert instance.farewell('Noel', 3) == 'farewell'
        assert instance.bid() == 'bid'
    }

    def toInstance(String classScript) {
        def clazz = cl.parseClass(classScript)

        return clazz.newInstance()
    }
}
