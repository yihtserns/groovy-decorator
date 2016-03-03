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
import java.util.Arrays;
import java.util.List;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotationNode;
import static org.codehaus.groovy.ast.ClassHelper.make;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SimpleMessage;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

/**
 *
 * @author yihtserns
 */
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class DecoratorASTTransformation implements ASTTransformation {

    private static final Token EQUAL_TOKEN = Token.newSymbol(Types.EQUAL, -1, -1);

    @Override
    public void visit(ASTNode[] astNodes, final SourceUnit sourceUnit) {
        AnnotationNode annotation = (AnnotationNode) astNodes[0];
        MethodNode method = (MethodNode) astNodes[1];

        List<AnnotationNode> methodDecorators = annotation.getClassNode().getAnnotations(make(MethodDecorator.class));
        if (methodDecorators.isEmpty()) {
            String msg = String.format("Annotation to decorate method must be annotated with %s. %s lacks this annotation.",
                    MethodDecorator.class.getName(),
                    annotation.getClassNode().getName());
            sourceUnit.getErrorCollector().addError(new SimpleMessage(msg, sourceUnit));
            return;
        }

        AnnotationNode methodDecorator = methodDecorators.get(0);
        Expression decorate = methodDecorator.getMember("value");
        decorate = callX(decorate, "newInstance", args(classX(ClassNode.THIS), classX(ClassNode.THIS)));
        VariableExpression decorateVar = varX("decorate");
        decorateVar.setClosureSharedVariable(true);
        DeclarationExpression decl0 = declareX(
                decorateVar,
                decorate);

        VariableExpression funcVar = varX("func");
        funcVar.setClosureSharedVariable(true);
        DeclarationExpression decl = declareX(
                funcVar,
                callX(classX(Function.class), "create", args(
                                classX(method.getDeclaringClass()),
                                constX(method.getName()),
                                toTypes(method.getParameters()))));

        List<Expression> decorateArgs = new ArrayList<Expression>();
        decorateArgs.add(callX(funcVar, "curry", args(varX("delegate"))));
        decorateArgs.add(toVars(method.getParameters()));

        VariableScope varScope = new VariableScope();
        varScope.putReferencedLocalVariable(decorateVar);
        varScope.putReferencedLocalVariable(funcVar);
        Expression metaClass = propX(classX(method.getDeclaringClass()), "metaClass");
        ClosureExpression methodInterceptor = new ClosureExpression(
                method.getParameters(),
                new ExpressionStatement(callX(decorateVar, "call", args(decorateArgs))));
        methodInterceptor.setVariableScope(varScope);
        BinaryExpression decl2 = assignX(propX(metaClass, method.getName()), methodInterceptor);

        List<Statement> statements = Arrays.<Statement>asList(
                new ExpressionStatement(decl0),
                new ExpressionStatement(decl),
                new ExpressionStatement(decl2));
        BlockStatement blockStatement = new BlockStatement(statements, new VariableScope());
        method.getDeclaringClass().addStaticInitializerStatements(Arrays.<Statement>asList(blockStatement), false);
    }

    private static ListExpression toVars(Parameter[] parameters) {
        ListExpression name2Vars = new ListExpression();
        for (Parameter parameter : parameters) {
            String paramName = parameter.getName();
            name2Vars.addExpression(varX(paramName));
        }

        return name2Vars;
    }

    private static ListExpression toTypes(Parameter[] parameters) {
        ListExpression types = new ListExpression();
        for (Parameter parameter : parameters) {
            types.addExpression(classX(parameter.getType()));
        }

        return types;
    }

    private static PropertyExpression propX(Expression obj, String propertyName) {
        return new PropertyExpression(obj, propertyName);
    }

    private static DeclarationExpression declareX(VariableExpression var, Expression value) {
        return new DeclarationExpression(var, EQUAL_TOKEN, value);
    }

    private static BinaryExpression assignX(Expression assignee, Expression value) {
        return new BinaryExpression(assignee, EQUAL_TOKEN, value);
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

    private static ClassExpression classX(Class type) {
        return new ClassExpression(make(type));
    }

    private static ClassExpression classX(ClassNode type) {
        return new ClassExpression(type);
    }

    private static MethodCallExpression callX(Expression type, String methodName, ArgumentListExpression args) {
        return new MethodCallExpression(type, methodName, args);
    }

    private static VariableExpression varX(String variableName) {
        return new VariableExpression(variableName);
    }
}
