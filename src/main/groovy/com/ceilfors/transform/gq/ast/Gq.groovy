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
@DecoratorClass(Gq.Decorator)
public @interface Gq {

    public static final class Decorator {

        static def call(func, args) {
            def result = func(*args)

            try {
                GqUtils.printToFile(func.name + "(${args.join(', ')})")
                GqUtils.printToFile("-> " + result)
            } finally {
                return result
            }
        }
    }
}
