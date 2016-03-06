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

import java.util.List;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotationNode;
import static org.codehaus.groovy.ast.ClassHelper.make;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.Variable;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import static org.codehaus.groovy.ast.expr.VariableExpression.THIS_EXPRESSION;
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

    /**
     * <pre>
     * class MyClass {
     *   {
     *      def _func = new Decorator1._closure(this, this)(Function.create(this, 'method', boolean, [String, int]))
     *      this.invokeMethod('metaClass', ({
     *          delegate.method { String x, int y -&gt;
     *              _func([x, y])
     *          }
     *      }) // Supposed to be `this.metaClass { ... }`, but `this.metaClass` got interpreted as `this.getMetaClass()`

     *      def _func = new Decorator2._closure(this, this)(Function.create(this, 'method', boolean, [String, int]))
     *      this.invokeMethod('metaClass', {
     *          delegate.method { String x, int y -&gt;
     *              _func([x, y])
     *          }
     *      })
     *   }
     *
     *  {@code @}Decorator1
     *  {@code @}Decorator2
     *   boolean method(String x, int y) {
     *     ...
     *   }
     * }
     * </pre>
     */
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
        ClassExpression decoratorClass = (ClassExpression) methodDecorator.getMember("value");

        VariableExpression funcVar = varX("_func");
        funcVar.setClosureSharedVariable(true);
        MethodCallExpression createFunction = callX(classX(Function.class), "create", args(
                THIS_EXPRESSION,
                constX(method.getName()),
                classX(method.getReturnType()),
                toTypeList(method.getParameters())));

        ConstructorCallExpression newDecorator = ctorX(decoratorClass.getType(), args(THIS_EXPRESSION, THIS_EXPRESSION));
        createFunction = callX(newDecorator, "call", args(createFunction));

        MethodCallExpression callFunction = callX(funcVar, "call", args(toVarList(method.getParameters())));
        VariableScope localVarScope = localVarScopeOf(funcVar);

        ClosureExpression methodInterceptor = closureX(method.getParameters(), stmtX(callFunction), localVarScope);
        MethodCallExpression declareMethodInterceptor = callX(varX("delegate"), method.getName(), args(methodInterceptor));

        Statement initializeMethodInterception = stmtX(
                declareX(funcVar, createFunction),
                callX(THIS_EXPRESSION, "invokeMethod", args(
                                constX("metaClass"),
                                closureX(Parameter.EMPTY_ARRAY, stmtX(declareMethodInterceptor), localVarScope))));
        method.getDeclaringClass().addObjectInitializerStatements(initializeMethodInterception);
    }

    private static VariableScope localVarScopeOf(Variable... variables) {
        VariableScope varScope = new VariableScope();
        for (Variable variable : variables) {
            varScope.putReferencedLocalVariable(variable);
        }

        return varScope;
    }

    private static Statement stmtX(Expression... expressions) {
        BlockStatement statement = new BlockStatement();
        for (Expression expression : expressions) {
            statement.addStatement(new ExpressionStatement(expression));
        }

        return statement;
    }

    private static ListExpression toVarList(Parameter[] parameters) {
        ListExpression vars = new ListExpression();
        for (Parameter parameter : parameters) {
            vars.addExpression(varX(parameter.getName()));
        }

        return vars;
    }

    private static ListExpression toTypeList(Parameter[] parameters) {
        ListExpression types = new ListExpression();
        for (Parameter parameter : parameters) {
            types.addExpression(classX(parameter.getType()));
        }

        return types;
    }

    private static DeclarationExpression declareX(VariableExpression var, Expression initialValue) {
        return new DeclarationExpression(var, EQUAL_TOKEN, initialValue);
    }

    private static ClosureExpression closureX(Parameter[] parameters, Statement body, VariableScope varScope) {
        ClosureExpression expression = new ClosureExpression(parameters, body);
        expression.setVariableScope(varScope);

        return expression;
    }

    private static ConstantExpression constX(Object value) {
        return new ConstantExpression(value);
    }

    private static ArgumentListExpression args(Expression... expressions) {
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

    private static ConstructorCallExpression ctorX(ClassNode type, Expression args) {
        return new ConstructorCallExpression(type, args);
    }

    private static VariableExpression varX(String variableName) {
        return new VariableExpression(variableName);
    }
}
