package com.github.bjoernpetersen.musicbot.api.config

interface ConfigSerializer<T> {
    fun serialize(obj: T): String
    @Throws(SerializationException::class)
    fun deserialize(string: String): T
}

class SerializationException : Exception()

object IntSerializer : ConfigSerializer<Int> {
    override fun serialize(obj: Int): String = obj.toString()
    @Throws(SerializationException::class)
    override fun deserialize(string: String): Int = string.toIntOrNull()
        ?: throw SerializationException()
}
