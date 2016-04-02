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

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.ast.tools.GeneralUtils;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SimpleMessage;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

import java.util.ArrayList;
import java.util.List;

import static org.codehaus.groovy.ast.ClassHelper.*;
import static org.codehaus.groovy.ast.expr.VariableExpression.*;
import static org.codehaus.groovy.ast.tools.GeneralUtils.*;

/**
 * @author yihtserns
 * @see #visit(ASTNode[], SourceUnit)
 * @see MethodDecorator
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class DecoratorASTTransformation implements ASTTransformation {

    private static final String METHOD_NODE_METADATA_KEY = "decoratedMethodNode";

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
     *     decorating$methodInputInput([input1, input2])
     *   }
     *
     *   private String decorated$method(x.Input input1, y.Input input2) {
     *     ...
     *   }
     *
     *  {@code @}Decorator1
     *   String method(y.Input input1, x.Input input2) {
     *     _decorating$methodInputInput([input1, input2])
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
        FieldNode funcField = getDecoratingField(method, clazz);
        if (funcField == null) {
            funcField = createDecoratingField(method, clazz);
        }

        MethodCallExpression getDecoratingAnnotation = callX(methodX(method), "getAnnotation", args(classX(annotation.getClassNode())));
        Expression decoratorInstance = methodDecorators.get(0).getMember("value");
        if (decoratorInstance instanceof ClassExpression) {
            decoratorInstance = ctorX(decoratorInstance.getType(), args(THIS_EXPRESSION, THIS_EXPRESSION));
        }

        Expression existingFunction = funcField.getInitialValueExpression();
        Expression decorateExistingFunction = callX(existingFunction, "decorateWith", args(
                getDecoratingAnnotation,
                decoratorInstance));
        funcField.setInitialValueExpression(decorateExistingFunction);
    }

    /**
     * @return returns <tt>decoratingField</tt> if it has been created before by other decorator, otherwise null
     */
    private FieldNode getDecoratingField(MethodNode method, ClassNode clazz) {
        return clazz.getField(buildDecoratingFieldName(method, clazz));
    }

    private FieldNode createDecoratingField(MethodNode method, ClassNode clazz) {
        String decoratingFieldName = buildDecoratingFieldName(method, clazz);
        MethodNode decoratedMethod = copyMethodTo("decorated$" + method.getName(), method);

        MethodCallExpression createFunction = callX(classX(Function.class), "create", args(
                closureThatCalls(decoratedMethod),
                constX(method.getName()),
                classX(method.getReturnType())));

        FieldNode funcField = clazz.addField(decoratingFieldName, FieldNode.ACC_PRIVATE, make(Function.class), createFunction);
        markFuncFieldAsBelongingTo(method, funcField);

        // Replace original method's body
        MethodCallExpression callFunctionField = callX(fieldX(funcField), "call", args(toVarList(method.getParameters())));
        method.setCode(stmt(callFunctionField));

        return funcField;
    }

    private static boolean funcFieldBelongsTo(MethodNode method, FieldNode funcField) {
        return method.equals(funcField.getNodeMetaData(METHOD_NODE_METADATA_KEY));
    }

    private static void markFuncFieldAsBelongingTo(MethodNode method, FieldNode funcField) {
        funcField.setNodeMetaData(METHOD_NODE_METADATA_KEY, method);
    }

    private MethodNode copyMethodTo(String newPrivateMethodName, MethodNode method) {
        return method.getDeclaringClass().addMethod(
                newPrivateMethodName,
                MethodNode.ACC_PRIVATE,
                method.getReturnType(),
                method.getParameters(),
                method.getExceptions(),
                method.getCode());
    }

    private static ClosureExpression closureThatCalls(MethodNode method) {
        return closureX(
                method.getParameters(),
                stmt(callX(THIS_EXPRESSION, method.getName(), toArgs(method.getParameters()))),
                new VariableScope());
    }

    private static String buildDecoratingFieldName(MethodNode method, ClassNode clazz) {
        StringBuilder decoratingFieldNameBuilder = new StringBuilder("decorating$" + method.getName().replaceAll("\\W", "\\$"));

        for (Parameter parameter : method.getParameters()) {
            decoratingFieldNameBuilder.append(buildTypeName(parameter.getType()));
        }

        String decoratingFieldName = decoratingFieldNameBuilder.toString();

        FieldNode funcField;
        while ((funcField = clazz.getField(decoratingFieldName)) != null && !funcFieldBelongsTo(method, funcField)) {
            decoratingFieldName = "_" + decoratingFieldName;
        }
        return decoratingFieldName;
    }

    private static String buildTypeName(ClassNode type) {
        if (type.isArray()) {
            return buildTypeName(type.getComponentType()) + "Array";
        }

        return type.getNameWithoutPackage();
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
        ClosureExpression expression = GeneralUtils.closureX(parameters, body);
        expression.setVariableScope(varScope);

        return expression;
    }

    private static Expression methodX(MethodNode method) {
        return new MethodCallExpression(classX(method.getDeclaringClass()), "getDeclaredMethod", args(
                constX(method.getName()),
                toTypes(CLASS_Type, method.getParameters())));
    }

    private static ArgumentListExpression toArgs(Parameter[] parameters) {
        ArgumentListExpression args = new ArgumentListExpression();
        for (Parameter parameter : parameters) {
            args.addExpression(varX(parameter.getName()));
        }

        return args;
    }
}
