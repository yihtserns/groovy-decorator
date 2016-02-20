package com.ceilfors.transform.gq.ast

import com.ceilfors.transform.gq.GqSupport
import com.ceilfors.transform.gq.GqUtils
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import static org.codehaus.groovy.ast.tools.GeneralUtils.*

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class GqTransformation extends AbstractASTTransformation {

    public void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        init(astNodes, sourceUnit)

        AnnotatedNode annotatedNode = astNodes[1]
        if (annotatedNode instanceof MethodNode) {
            MethodNode method = annotatedNode as MethodNode
            BlockStatement originalCode = method.code as BlockStatement

            def closuredOriginalCode = closureX(originalCode)
            closuredOriginalCode.setVariableScope(new VariableScope())
            BlockStatement newCode = new BlockStatement([
                    printToFileStatement(constX("$method.name()" as String)),
                    declS(varX("result", method.returnType), callX(closuredOriginalCode, "call")),
                    printToFileStatement(plusX(constX("-> "), varX("result"))),
                    returnS(varX("result"))

            ], originalCode.variableScope)
            method.code = newCode
        } else if (annotatedNode instanceof ClassNode) {
            ClassNode classNode = annotatedNode as ClassNode
            classNode.addField("gq",
                    ACC_FINAL | ACC_TRANSIENT | ACC_STATIC | ACC_PRIVATE,
                    ClassHelper.make(GqSupport), ctorX(ClassHelper.make(GqSupport)))
            def transformer = new ClassCodeExpressionTransformer() {

                @Override
                protected SourceUnit getSourceUnit() {
                    sourceUnit
                }

                @Override
                Expression transform(Expression expression) {
                    if (expression instanceof MethodCallExpression && expression.methodAsString == "gq") {
                        // Traps normal method call and add expression text to the method call
                        MethodCallExpression methodCallExpression = expression as MethodCallExpression
                        ArgumentListExpression argumentListExpression = methodCallExpression.arguments as ArgumentListExpression
                        argumentListExpression.expressions.add(0, constX("${argumentListExpression.expressions.get(0).text.replace('(', '').replace(')', '')}" as String) )
                    }
                    return super.transform(expression)
                }
            }
            transformer.visitClass(classNode)
        }
    }

    private static Statement printToFileStatement(Expression expr) {
        stmt(new StaticMethodCallExpression(
                ClassHelper.make(GqUtils.class),
                'printToFile',
                new ArgumentListExpression(expr)))
    }
}