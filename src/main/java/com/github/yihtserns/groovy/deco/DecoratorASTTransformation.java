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
import static org.codehaus.groovy.ast.ClassHelper.CLASS_Type;
import static org.codehaus.groovy.ast.ClassHelper.make;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ArrayExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.FieldExpression;
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
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

/**
 *
 * @author yihtserns
 */
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class DecoratorASTTransformation implements ASTTransformation {

    /**
     * Turns:
     * <pre>
     * class MyClass {
     *
     *  {@code @}Decorator1(el1 = val1, el2 = val2,... elN = valN)
     *  {@code @}Decorator2
     *   boolean method(String x, int y) {
     *     ...
     *   }
     *
     *  {@code @}Decorator1
     *   String method(x.Input input1, y.Input input2) {
     *     ...
     *   }
     *
     *  {@code @}Decorator1
     *   String method(y.Input input1, x.Input input2) {
     *     ...
     *   }
     * }
     * </pre>
     * into:
     * <pre>
     * class MyClass {
     *
     *   private Function decorating$methodStringint = Function.create({ String x, int y -> decorated$method(x, y) }, boolean)
     *                                                        .decorateWith(
     *                                                              MyClass.getDeclaredMethod('method', [String, int] as Class[]).getAnnotation(Decorator1.class),
     *                                                              new Decorator1$_closure1(this, this))
     *                                                        .decorateWith(
     *                                                              MyClass.getDeclaredMethod('method', [String, int] as Class[]).getAnnotation(Decorator2.class),
     *                                                              new Decorator2$_closure1(this, this))
     *   private Function decorating$methodInputInput = Function.create({ x.Input input1, y.Input input2 -> decorated$method(input1, input2) }, String)
     *                                                        .decorateWith(
     *                                                              MyClass.getDeclaredMethod('method', [x.Input, y.Input] as Class[]).getAnnotation(Decorator1.class),
     *                                                              new Decorator1$_closure1(this, this))
     *   private Function _decorating$methodInputInput = Function.create({ y.Input input1, x.Input input2 -> decorated$method(input1, input2) }, String)
     *                                                        .decorateWith(
     *                                                              MyClass.getDeclaredMethod('method', [y.Input, x.Input] as Class[]).getAnnotation(Decorator1.class),
     *                                                              new Decorator1$_closure1(this, this))
     *
     *  {@code @}Decorator1(el1 = val1, el2 = val2,... elN = valN)
     *  {@code @}Decorator2
     *   boolean method(String x, int y) {
     *     decorating$methodStringint([x, y])
     *   }
     *
     *   private boolean decorated$method(String x, int y) {
     *     ...
     *   }
     *
     *  {@code @}Decorator1
     *   String method(x.Input input1, y.Input input2) {
     *     decorating$methodInputInput(input1, input2)
     *   }
     *
     *   private String decorated$method(x.Input input1, y.Input input2) {
     *     ...
     *   }
     *
     *  {@code @}Decorator1
     *   String method(y.Input input1, x.Input input2) {
     *     _decorating$methodInputInput(input1, input2)
     *   }
     *
     *   private String decorated$method(y.Input input1, x.Input input2) {
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

        ClassNode clazz = method.getDeclaringClass();

        // Move original method's body into a new method
        MethodNode decoratedMethod = clazz.addMethod(
                "decorated$" + method.getName(),
                MethodNode.ACC_PRIVATE,
                method.getReturnType(),
                method.getParameters(),
                method.getExceptions(),
                method.getCode());

        ClassExpression decoratorClass = (ClassExpression) methodDecorators.get(0).getMember("value");

        MethodCallExpression getDecoratingAnnotation = callX(methodX(method), "getAnnotation", argsX(classX(annotation.getClassNode())));
        ConstructorCallExpression newDecorator = ctorX(decoratorClass.getType(), argsX(THIS_EXPRESSION, THIS_EXPRESSION));

        StringBuilder decoratingFieldNameBuilder = new StringBuilder("decorating$");
        decoratingFieldNameBuilder.append(method.getName().replaceAll("\\W", "\\$"));
        for (Parameter parameter : method.getParameters()) {
            decoratingFieldNameBuilder.append(parameter.getType().getNameWithoutPackage());
        }
        String decoratingFieldName = decoratingFieldNameBuilder.toString();

        FieldNode funcField = clazz.getField(decoratingFieldName);
        if (funcField != null) {
            Expression funcValue = funcField.getInitialValueExpression();
            funcValue = callX(funcValue, "decorateWith", argsX(
                    getDecoratingAnnotation,
                    newDecorator));

            funcField.setInitialValueExpression(funcValue);
        } else {
            // Create closure that calls the new method
            ClosureExpression callDecoratedMethod = closureX(
                    method.getParameters(),
                    stmtS(callX(THIS_EXPRESSION, decoratedMethod.getName(), toArgs(method.getParameters()))),
                    new VariableScope());
            MethodCallExpression createFunction = callX(classX(Function.class), "create", argsX(
                    callDecoratedMethod,
                    constX(method.getName()),
                    classX(method.getReturnType())));
            createFunction = callX(createFunction, "decorateWith", argsX(
                    getDecoratingAnnotation,
                    newDecorator));
            funcField = clazz.addField(decoratingFieldName, FieldNode.ACC_PRIVATE, make(Function.class), createFunction);
        }

        // Replace original method's body with one that calls the closure
        MethodCallExpression callFunction = callX(fieldX(funcField), "call", argsX(toVarList(method.getParameters())));
        method.setCode(stmtS(callFunction));
    }

    private static Statement stmtS(Expression... expressions) {
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

    private static ArrayExpression toTypes(ClassNode arrayType, Parameter[] parameters) {
        List<Expression> paramExpressions = new ArrayList<Expression>();
        for (Parameter parameter : parameters) {
            paramExpressions.add(classX(parameter.getType()));
        }

        return new ArrayExpression(arrayType, paramExpressions);
    }

    private static ClosureExpression closureX(Parameter[] parameters, Statement body, VariableScope varScope) {
        ClosureExpression expression = new ClosureExpression(parameters, body);
        expression.setVariableScope(varScope);

        return expression;
    }

    private static ConstantExpression constX(Object value) {
        return new ConstantExpression(value);
    }

    private static ArgumentListExpression argsX(Expression... expressions) {
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

    private static Expression methodX(MethodNode method) {
        return new MethodCallExpression(classX(method.getDeclaringClass()), "getDeclaredMethod", argsX(
                constX(method.getName()),
                toTypes(CLASS_Type, method.getParameters())));
    }

    private static FieldExpression fieldX(FieldNode field) {
        return new FieldExpression(field);
    }

    private static ArgumentListExpression toArgs(Parameter[] parameters) {
        ArgumentListExpression args = new ArgumentListExpression();
        for (Parameter parameter : parameters) {
            args.addExpression(varX(parameter.getName()));
        }

        return args;
    }
}
