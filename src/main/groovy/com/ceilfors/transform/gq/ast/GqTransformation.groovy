package com.ceilfors.transform.gq.ast

import com.ceilfors.transform.gq.GqSupport
import com.ceilfors.transform.gq.GqUtils
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import static org.codehaus.groovy.ast.tools.GeneralUtils.constX
import static org.codehaus.groovy.ast.tools.GeneralUtils.ctorX

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class GqTransformation extends AbstractASTTransformation {

    public void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        init(astNodes, sourceUnit)

        AnnotatedNode annotatedNode = astNodes[1] as AnnotatedNode
        if (annotatedNode instanceof MethodNode) {
            transformMethodNode(annotatedNode)
        } else if (annotatedNode instanceof ClassNode) {
            transformClassNode(annotatedNode)
        }
    }

    private void transformClassNode(ClassNode classNode) {
        classNode.addField("gq", ACC_FINAL | ACC_TRANSIENT | ACC_STATIC | ACC_PRIVATE,
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
                    argumentListExpression.expressions.add(0, constX("${argumentListExpression.expressions.get(0).text.replace('(', '').replace(')', '')}" as String))
                }
                return super.transform(expression)
            }
        }
        transformer.visitClass(classNode)
    }

    private void transformMethodNode(MethodNode methodNode) {
        def astBuilder = new AstBuilder()
        BlockStatement originalCode = methodNode.code as BlockStatement
        List<ASTNode> nodes = astBuilder.buildFromSpec {
            block {
                expression {
                    staticMethodCall(GqUtils, "printToFile") {
                        argumentList { constant "$methodNode.name()" as String }
                    }
                }
                expression {
                    declaration {
                        variable "result"
                        token "="
                        methodCall {
                            closure {
                                parameters {}
                                block {
                                    expression.addAll(originalCode.statements)
                                }

                            }
                            (expression.last() as ClosureExpression).setVariableScope(new VariableScope())
                            constant "call"
                            argumentList {}
                        }

                    }
                }
                expression {
                    staticMethodCall(GqUtils, "printToFile") {
                        argumentList {
                            binary {
                                constant "-> "
                                token "+"
                                variable "result"
                            }

                        }
                    }
                }
                returnStatement { variable "result" }
            }
        }
        methodNode.code = nodes[0] as Statement
    }
}