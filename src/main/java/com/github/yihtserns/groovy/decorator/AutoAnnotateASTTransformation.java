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

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import static org.codehaus.groovy.ast.ClassHelper.make;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformationClass;

/**
 * Automatically annotate {@code @}{@link GroovyASTTransformationClass}(classes = {@link DecoratorASTTransformation}.class)
 * on annotation that is annotated with {@code @}{@link DecoratorClass}.
 *
 * @author yihtserns
 */
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class AutoAnnotateASTTransformation implements ASTTransformation {

    @Override
    public void visit(ASTNode[] astNodes, SourceUnit source) {
        AnnotationNode astTransformerClass = new AnnotationNode(ClassHelper.make(GroovyASTTransformationClass.class));
        astTransformerClass.addMember("classes", classX(DecoratorASTTransformation.class));

        ClassNode classNode = (ClassNode) astNodes[1];
        classNode.addAnnotation(astTransformerClass);
    }

    private static ClassExpression classX(Class type) {
        return new ClassExpression(make(type));
    }
}
