package com.ceilfors.transform.gq.ast

import static com.ceilfors.transform.gq.GqSupport.gq

/**
 * @author ceilfors
 */
class GqSupportExample {

    int "3 plus 5"() {
        gq(3 + 5)
    }
}
