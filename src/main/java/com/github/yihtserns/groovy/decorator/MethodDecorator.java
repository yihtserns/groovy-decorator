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
package com.github.yihtserns.groovy.decorator;

import groovy.transform.AnnotationCollector;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import static org.codehaus.groovy.ast.ClassHelper.make;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.AnnotationCollectorTransform;
import org.codehaus.groovy.transform.GroovyASTTransformationClass;

/**
 * @see #value()
 * @author yihtserns
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
@AnnotationCollector(processor = "com.github.yihtserns.groovy.decorator.MethodDecorator$AutoAnnotateTransform")
public @interface MethodDecorator {

    /**
     * @return class that contains a static method of {@code call(Closure func, Object[] args)}.
     * <ul>
     * <li>{@code func} is the original method body</li>
     * <li>{@code args} is the arguments passed in by the method caller</li>
     * </ul>
     */
    Class<?> value();

    /**
     * Automatically annotate {@code @}{@link GroovyASTTransformationClass}(classes = {@link DecoratorASTTransformation}.class)
     * on annotation that is annotated with {@code @}{@link MethodDecorator}, so users don't have to.
     */
    public static class AutoAnnotateTransform extends AnnotationCollectorTransform {

        @Override
        public List<AnnotationNode> visit(
                AnnotationNode annotationCollector,
                AnnotationNode methodDecorator,
                AnnotatedNode aliasAnnotated,
                SourceUnit source) {
            AnnotationNode astTransformClass = new AnnotationNode(make(GroovyASTTransformationClass.class));
            astTransformClass.addMember("value", new ConstantExpression(DecoratorASTTransformation.class.getName()));

            return Arrays.asList(methodDecorator, astTransformClass);
        }
    }
}
