package com.ceilfors.transform.gq.ast

import com.ceilfors.transform.gq.GqUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class GqTest extends Specification {

    @Rule
    TemporaryFolder temporaryFolder

    class Example {

        @Gq
        int simple() {
            5
        }
    }

    def setup() {
        System.setProperty(GqUtils.TEMP_DIR, temporaryFolder.newFolder().absolutePath)
    }

    def "Should write method name"() {
        setup:
        def example = new Example()

        when:
        example.simple()

        then:
        GqUtils.gqFile.readLines().first().contains("simple()")
    }

    def "Should write returned value"() {
        setup:
        def example = new Example()

        when:
        example.simple()

        then:
        GqUtils.gqFile.readLines().last().contains("-> 5")
    }
}
