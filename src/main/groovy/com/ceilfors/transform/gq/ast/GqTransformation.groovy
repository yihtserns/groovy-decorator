package com.ceilfors.transform.gq.ast

import com.ceilfors.transform.gq.GqUtils
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.VariableScope
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.Expression
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

        MethodNode method = astNodes[1] as MethodNode
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
    }

    private static Statement printToFileStatement(Expression expr) {
        stmt(new StaticMethodCallExpression(
                ClassHelper.make(GqUtils.class),
                'printToFile',
                new ArgumentListExpression(expr)))
    }
}