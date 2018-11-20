package com.github.bjoernpetersen.musicbot.internal.player

import com.google.inject.AbstractModule
import com.google.inject.Guice
import org.junit.jupiter.api.Test
import javax.annotation.Nullable
import javax.inject.Inject

class DefaultPlayerTest {

    @Test
    fun test() {
        Guice.createInjector(Mod()).getInstance(IThing::class.java).print()

    }
}

interface IThing {
    fun print()
}

class Mod :AbstractModule(){
    override fun configure() {
        bind(IThing::class.java).to(Thing::class.java)
    }
}

class Thing @Inject constructor(@Nullable val string: String?) : IThing {
    override fun print() {
        println("string: ${string == null}")
    }

}
