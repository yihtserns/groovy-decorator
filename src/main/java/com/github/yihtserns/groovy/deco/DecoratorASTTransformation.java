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
package com.github.yihtserns.groovy.deco;

import java.util.ArrayList;
import java.util.List;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import static org.codehaus.groovy.ast.ClassHelper.make;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SimpleMessage;
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
        MethodNode method = (MethodNode) astNodes[1];

        ClosureExpression closuredOriginalCode = closureX(method.getParameters(), method.getCode());
        closuredOriginalCode.setVariableScope(new VariableScope());

        List<Expression> arguments = new ArrayList<Expression>();
        arguments.add(ctorX(
                make(Function.class), args(closuredOriginalCode, constX(method.getName()))));
        arguments.add(toVars(method.getParameters()));

        List<AnnotationNode> methodDecorators = annotation.getClassNode().getAnnotations(make(MethodDecorator.class));
        if (methodDecorators.isEmpty()) {
            String msg = String.format("Annotation to decorate method must be annotated with %s. %s lacks this annotation.",
                    MethodDecorator.class.getName(),
                    annotation.getClassNode().getName());
            sourceUnit.getErrorCollector().addError(new SimpleMessage(msg, sourceUnit));
            return;
        }

        AnnotationNode methodDecorator = methodDecorators.get(0);
        Expression closure = methodDecorator.getMember("value");
        closure = callX(closure, "newInstance", args(classX(ClassNode.THIS), classX(ClassNode.THIS)));

        Expression newMethodBody = callX(closure, "call", args(arguments));
        if (method.getReturnType() != ClassHelper.VOID_TYPE) {
            method.setCode(returnS(newMethodBody));
        } else {
            method.setCode(new ExpressionStatement(newMethodBody));
        }
    }

    private static ListExpression toVars(Parameter[] parameters) {
        ListExpression name2Vars = new ListExpression();
        for (Parameter parameter : parameters) {
            String paramName = parameter.getName();
            name2Vars.addExpression(varX(paramName));
        }

        return name2Vars;
    }

    private static ClosureExpression closureX(Parameter[] parameters, Statement body) {
        return new ClosureExpression(parameters, body);
    }

    private static ConstantExpression constX(Object value) {
        return new ConstantExpression(value);
    }

    private static ArgumentListExpression args(Expression... expressions) {
        return new ArgumentListExpression(expressions);
    }

    private static ArgumentListExpression args(List<Expression> expressions) {
        return new ArgumentListExpression(expressions);
    }

    private static ConstructorCallExpression ctorX(ClassNode type, ArgumentListExpression args) {
        return new ConstructorCallExpression(type, args);
    }

    private static ClassExpression classX(Class type) {
        return new ClassExpression(make(type));
    }

    private static ClassExpression classX(ClassNode type) {
        return new ClassExpression(type);
    }

    private static MethodCallExpression callX(Expression type, String methodName, ArgumentListExpression args) {
        return new MethodCallExpression(type, methodName, args);
    }

    private static ReturnStatement returnS(Expression expression) {
        return new ReturnStatement(expression);
    }

    private static VariableExpression varX(String variableName) {
        return new VariableExpression(variableName);
    }
}
