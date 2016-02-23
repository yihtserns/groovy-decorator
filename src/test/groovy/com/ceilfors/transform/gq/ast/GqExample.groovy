package com.ceilfors.transform.gq.ast

/**
 * @author ceilfors
 */
class GqExample {

    @Gq
    int "return 5"() {
        5
    }

    @Gq
    int add(int x, int y) {
        return x + y
    }
}
