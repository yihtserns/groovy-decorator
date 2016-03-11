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
class FunctionTest {

    @Test
    void 'can decorate using one decorator'() {
        def func = { name -> 'Hey ' + name }
        func = Function.create(func, 'greet', String)
        func = func.decorateWith { f ->
            return { args -> f(args) + '!' }
        }

        assert func(['Noel']) == 'Hey Noel!'
    }

    @Test
    void 'can get method name'() {
        final String methodName = 'greet'

        def func = { }
        func = Function.create(func, methodName, String)
        func = func.decorateWith { f ->
            { args -> f.name }
        }

        assert func(['Noel']) == methodName
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
    void 'can decorate for two params'() {
        def func = { name, id -> 'Hey ' + name + ' ' + id }
        func = Function.create(func, 'greet', String)
        func = func.decorateWith { f ->
            { args -> f(args) + '!' }
        }

        assert func(['Noel', 300]) == 'Hey Noel 300!'
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
