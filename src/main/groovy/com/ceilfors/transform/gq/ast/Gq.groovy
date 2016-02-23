package com.ceilfors.transform.gq.ast

import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target
import com.ceilfors.transform.gq.GqUtils
import com.github.yihtserns.groovy.decorator.DecoratorClass

/**
 * @author ceilfors
 */
@Retention (RetentionPolicy.SOURCE)
@Target (ElementType.METHOD)
@DecoratorClass(Gq.Decorator)
@GroovyASTTransformationClass (DecoratorClass.TRANSFORMER_CLASS)
public @interface Gq {

    public static final class Decorator {

        static def call(funcName, func, name2Args) {
            def args = name2Args.values().collect()
            def result = func(*args)

            try {
                GqUtils.printToFile(funcName + "(${args.join(', ')})")
                GqUtils.printToFile("-> " + result)
            } finally {
                return result
            }
        }
    }
}
