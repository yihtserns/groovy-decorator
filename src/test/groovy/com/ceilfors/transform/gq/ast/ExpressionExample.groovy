package com.ceilfors.transform.gq.ast

import static com.ceilfors.transform.gq.GqSupport.gq

/**
 * @author ceilfors
 */
class ExpressionExample {

    def method() {
        gq(3 + 5)
    }
}
