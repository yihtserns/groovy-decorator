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

import java.lang.annotation.Target
import java.lang.annotation.ElementType

/**
 * The class name is supposed to be Greeter3, but having that name causes it to skip AST transformation!
 *
 * @author yihtserns
 */
class Greeter31 {

    @Exclaim
    String greet(String name, int count) {
        def list = []
        count.times { list << 'Hi ' + name }

        return list.join(' ')
    }
}
