package net.bjoernpetersen.musicbot.api.config

fun Config.implEntry(
    key: String = "key",
    configure: SerializedConfiguration<Impl>.() -> Unit = {}
): Config.SerializedEntry<Impl> {
    return serialized(key) {
        description = "description"
        serializer = Impl
        check { null }
        actionButton {
            label = "Label"
            describe { it.name }
            action { true }
        }
        configure()
    }
}

interface Base {
    val name: String
}

data class Impl(override val name: String = "TestName") : Base {
    companion object : ConfigSerializer<Impl> {
        override fun serialize(obj: Impl): String {
            return obj.name
        }

        override fun deserialize(string: String): Impl {
            return Impl(string)
        }
    }

    object FaultySerializer : ConfigSerializer<Impl> {
        override fun serialize(obj: Impl): String {
            return obj.name
        }

        override fun deserialize(string: String): Impl {
            throw SerializationException()
        }
    }
}
