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

import org.junit.Test

/**
 * @author yihtserns
 */
class DecoratorASTTransformationTest {

    def cl = new GroovyClassLoader()
	
    @Test
    void 'can decorate'() {
        def greeter = new Greeter1()
        assert greeter.greet() == 'Hi!'
    }

    @Test
    public void 'can decorate method with one param'() {
        def greeter = new Greeter2()
        assert greeter.greet('Noel') == 'Hi Noel!'
    }

    @Test
    public void 'can decorate method with two params'() {
        def greeter = new Greeter31()
        assert greeter.greet('Noel', 3) == 'Hi Noel Hi Noel Hi Noel!'
    }

    @Test
    public void 'can use three decorators'() {
        def greeter = new Greeter4()
        assert greeter.greet('Noel') == 'Hi Mr. Noel!?'
    }

    @Test
    public void 'can get method name'() {
        def greeter = new Greeter5()
        assert greeter.greet('Noel') == 'greet'
        assert greeter.farewell('Noel', 3) == 'farewell'
        assert greeter.bid() == 'bid'
    }
}
