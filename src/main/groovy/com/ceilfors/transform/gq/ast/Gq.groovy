package com.ceilfors.transform.gq.ast

import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * @author ceilfors
 */
@Retention (RetentionPolicy.SOURCE)
@Target ([ElementType.METHOD, ElementType.TYPE])
@GroovyASTTransformationClass ("com.ceilfors.transform.gq.ast.GqTransformation")
public @interface Gq {

}