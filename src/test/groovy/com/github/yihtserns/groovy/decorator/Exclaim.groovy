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

import groovy.transform.AnnotationCollector
import java.lang.annotation.Target
import java.lang.annotation.ElementType
import org.codehaus.groovy.transform.GroovyASTTransformationClass
import com.github.yihtserns.groovy.decorator.Exclaim.Decorator

/**
 *
 * @author yihtserns
 */
@Target(ElementType.METHOD)
@DecoratorClass(Decorator)
@GroovyASTTransformationClass(DecoratorClass.TRANSFORMER_CLASS)
@interface Exclaim {

    static class Decorator {

        static def call(funcName, func, name2Args) {
            return func(*name2Args.values().collect()) + '!'
        }
    }
}
