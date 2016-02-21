package com.ceilfors.transform.gq

/**
 * @author ceilfors
 */
class GqSupport {

    static GqSupport gq = new GqSupport()

    def <T> T call(T value) {
        throw new IllegalStateException("Can't be called during runtime!")
    }
}
