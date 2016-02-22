package com.ceilfors.transform.gq.ast

import com.ceilfors.transform.gq.GqUtils
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.VariableScope
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class GqTransformation extends AbstractASTTransformation {

    public void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        init(astNodes, sourceUnit)

        AnnotatedNode annotatedNode = astNodes[1] as AnnotatedNode
        if (annotatedNode instanceof MethodNode) {
            transformMethodNode(annotatedNode)
        } else {
            throw new IllegalStateException("Gq annotation is only usable in methods.")
        }
    }

    private void transformMethodNode(MethodNode methodNode) {
        def astBuilder = new AstBuilder()
        BlockStatement originalCode = methodNode.code as BlockStatement
        List<ASTNode> nodes = astBuilder.buildFromSpec {
            block {
                expression {
                    staticMethodCall(GqUtils, "printToFile") {
                        argumentList { constant methodNode.name + "()" }
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