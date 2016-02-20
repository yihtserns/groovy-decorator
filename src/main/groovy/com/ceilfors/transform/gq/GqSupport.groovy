package com.ceilfors.transform.gq
/**
 * @author ceilfors
 */
class GqSupport {

    def call(value) {
        throw new IllegalStateException("Can't be called during runtime!")
    }

    def call(String expression, Object value) {
        GqUtils.printToFile "$expression=$value"
        return value
    }
}
