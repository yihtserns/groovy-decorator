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

import java.lang.annotation.Documented
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target
import java.lang.annotation.ElementType
import org.codehaus.groovy.transform.GroovyASTTransformationClass

/**
 * Unlike {@link MethodDecorator}, which is used to describe how an annotation decorates a method:
 * <pre>
 *{@code @}MethodDecorator({ &lt;how to decorate method&gt; })
 * AnAnnotation
 * ...
 * then
 * ...
 *{@code @}AnAnnotation
 * &lt;the method&gt;(...)
 * </pre>
 *
 * {@code Intercept} allows ad-hoc decoration to be described on the method itself:
 * <pre>
 *{@code @}Intercept({ &lt;how to decorate method&gt; })
 * &lt;the method&gt;(...)
 * </pre>
 *
 * @see #value()
 * @author yihtserns
 */
@MethodDecorator({ func, Intercept intercept ->
    def handle = intercept.value().newInstance(this, this)

    return { args ->
        handle(func, args)
    }
})
@GroovyASTTransformationClass("com.github.yihtserns.groovy.decorator.DecoratorASTTransformation")
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface Intercept {

   /**
    * Returns a closure in this form:
    * <pre>
    * // Representing the decorated method, and the list of arguments the caller used to call the method
    * { com.github.yihtserns.groovy.decorator.Function func, args -&gt;
    *
    *   // return func(args) // Call the decorated method and return the result
    *   // Or do whatever
    * }
    * </pre>
    * The closure can also reference any property that the method's class has.
    *
    * @return a closure that intercepts a method call
    */
    Class<? extends Closure> value()
}
