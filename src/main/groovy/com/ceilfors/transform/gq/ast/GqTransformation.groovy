package com.ceilfors.transform.gq.ast

import com.ceilfors.transform.gq.GqUtils
import org.codehaus.groovy.ast.*
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

        final def closureVariableScope = new VariableScope(originalCode.variableScope)
        for (Parameter parameter in methodNode.parameters) {
            // Allow closure to access the original code's variable
            closureVariableScope.putReferencedLocalVariable(parameter)
            parameter.setClosureSharedVariable(true) // KLUDGE: Should we use: new VariableScopeVisitor(sourceUnit, true).visitMethod(methodNode)
        }

        List<ASTNode> nodes = astBuilder.buildFromSpec {
            block {
                expression {
                    staticMethodCall(GqUtils, "printToFile") {
                        argumentList {
                            gString "${methodNode.name}(parameters)", {
                                strings {
                                    constant "${methodNode.name}(" as String
                                    for (int i = 0; i < methodNode.parameters.size(); i++) {
                                        if (i != 0) {
                                            constant ', '
                                        }
                                    }
                                    constant ')'
                                }
                                values {
                                    for (Parameter parameter in methodNode.parameters) {
                                        variable parameter.name
                                    }
                                }
                            }
                        }
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

                            (expression.last() as ClosureExpression).setVariableScope(closureVariableScope)
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