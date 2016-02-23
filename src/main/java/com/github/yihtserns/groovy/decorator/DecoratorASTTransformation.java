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

import java.util.ArrayList;
import java.util.List;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ListExpression;
import static org.codehaus.groovy.ast.tools.GeneralUtils.args;
import static org.codehaus.groovy.ast.tools.GeneralUtils.callX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.classX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.closureX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.constX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.returnS;
import static org.codehaus.groovy.ast.tools.GeneralUtils.varX;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

/**
 *
 * @author yihtserns
 */
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class DecoratorASTTransformation implements ASTTransformation {

    @Override
    public void visit(ASTNode[] astNodes, final SourceUnit sourceUnit) {
        AnnotationNode annotation = (AnnotationNode) astNodes[0];

        DecoratorClass decoratorClass = (DecoratorClass) annotation.getClassNode().getTypeClass().getAnnotation(DecoratorClass.class);

        MethodNode method = (MethodNode) astNodes[1];
        ClosureExpression closuredOriginalCode = closureX(method.getParameters(), method.getCode());
        closuredOriginalCode.setVariableScope(new VariableScope());

        List<Expression> arguments = new ArrayList<Expression>();
        arguments.add(constX(method.getName()));
        arguments.add(closuredOriginalCode);
        arguments.add(new ListExpression(toVars(method.getParameters())));

        method.setCode(returnS(callX(classX(decoratorClass.value()), "call", args(arguments))));
    }

    private static List<Expression> toVars(Parameter[] parameters) {
        List<Expression> variables = new ArrayList<Expression>();
        for (Parameter parameter : parameters) {
            variables.add(varX(parameter.getName()));
        }

        return variables;
    }
}