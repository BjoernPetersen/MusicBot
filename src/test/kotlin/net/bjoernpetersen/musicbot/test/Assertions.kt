package net.bjoernpetersen.musicbot.test

import org.assertj.core.api.InstanceOfAssertFactories
import org.assertj.core.api.ObjectAssert

inline fun <reified U> ObjectAssert<*>.asInstanceOf(): ObjectAssert<U> {
    return asInstanceOf(InstanceOfAssertFactories.type(U::class.java))
}
