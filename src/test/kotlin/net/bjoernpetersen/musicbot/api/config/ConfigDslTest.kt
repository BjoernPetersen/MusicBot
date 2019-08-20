package net.bjoernpetersen.musicbot.api.config

import net.bjoernpetersen.musicbot.test.api.config.ConfigExtension
import net.bjoernpetersen.musicbot.test.asInstanceOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@UseExperimental(ExperimentalConfigDsl::class)
@ExtendWith(ConfigExtension::class)
class ConfigDslTest {
    @Test
    fun serializerContravariant(config: Config) {
        val serializeAny = { any: Any -> any.toString() }
        config.implEntry {
            serialization {
                serialize(serializeAny)
                deserialize { Impl.deserialize(it) }
            }
        }
    }

    @Test
    fun serializationDsl() {
        // TODO
    }

    @Nested
    inner class StringEntry {
        private val default = "defaultValue"
        private val value = "value"
        @Test
        fun `with default and ui`(config: Config) {
            val entry = config.string(KEY) {
                description = DESCRIPTION
                check(NonnullConfigChecker)
                default(default)
                uiNode = PasswordBox
            }
            assertEquals(KEY, entry.key)
            assertEquals(DESCRIPTION, entry.description)
            assertEquals(PasswordBox, entry.uiNode)

            assertEquals(default, entry.default)
            assertEquals(default, entry.get())
            assertNull(entry.getWithoutDefault())
            assertNull(entry.checkError())

            entry.set(value)
            assertThat(value)
                .isEqualTo(entry.get())
                .isEqualTo(entry.getWithoutDefault())
            assertNull(entry.checkError())
        }

        @Test
        fun `without default and ui`(config: Config) {
            val entry = config.string(KEY) {
                description = DESCRIPTION
                check(NonnullConfigChecker)
            }
            assertEquals(KEY, entry.key)
            assertEquals(DESCRIPTION, entry.description)
            assertNull(entry.uiNode)

            assertNull(entry.default)
            assertNull(entry.get())
            assertNull(entry.getWithoutDefault())
            assertNotNull(entry.checkError())

            entry.set(value)
            assertThat(value)
                .isEqualTo(entry.get())
                .isEqualTo(entry.getWithoutDefault())
            assertNull(entry.checkError())
        }

        @TestFactory
        fun `blank key`(config: Config) = listOf("", " ")
            .map { key ->
                dynamicTest("Key: \"$key\"") {
                    assertThrows<IllegalArgumentException> {
                        config.string(key) {
                            description = DESCRIPTION
                            check(NonnullConfigChecker)
                        }
                    }
                }
            }

        @Test
        fun `missing description`(config: Config) {
            assertThrows<IllegalStateException> {
                config.string(KEY) {
                    check(NonnullConfigChecker)
                }
            }
        }

        @Test
        fun `missing checker`(config: Config) {
            assertThrows<IllegalStateException> {
                config.string(KEY) {
                    description = DESCRIPTION
                }
            }
        }
    }

    @Nested
    inner class BooleanEntry {
        private val testDefault = false
        private val testValue = true
        @Test
        fun get(config: Config) {
            val entry = config.boolean(KEY) {
                description = DESCRIPTION
                default = testDefault
            }
            assertEquals(KEY, entry.key)
            assertEquals(DESCRIPTION, entry.description)
            assertEquals(CheckBox, entry.uiNode)

            assertThat(testDefault)
                .isEqualTo(entry.default)
                .isEqualTo(entry.get())
            assertNull(entry.getWithoutDefault())
            assertNull(entry.checkError())

            entry.set(testValue)
            assertThat(testValue)
                .isEqualTo(entry.get())
                .isEqualTo(entry.getWithoutDefault())
            assertNull(entry.checkError())
        }

        @TestFactory
        fun `blank key`(config: Config) = listOf("", " ")
            .map { key ->
                dynamicTest("Key: \"$key\"") {
                    assertThrows<IllegalArgumentException> {
                        config.boolean(key) {
                            description = DESCRIPTION
                            default = testDefault
                        }
                    }
                }
            }

        @Test
        fun `missing description`(config: Config) {
            assertThrows<IllegalStateException> {
                config.boolean(KEY) {
                    default = testDefault
                }
            }
        }

        @Test
        fun `missing default`(config: Config) {
            assertThrows<IllegalStateException> {
                config.boolean(KEY) {
                    description = DESCRIPTION
                }
            }
        }
    }

    @Nested
    inner class SerializedEntry {
        private val default = Impl("default")
        private val value = Impl()
        @Test
        fun `with default and ui`(config: Config) {
            val entry = config.serialized<Impl>(KEY) {
                description = DESCRIPTION
                serializer = Impl
                check(NonnullConfigChecker)
                default(default)
                choiceBox {
                    describe { it.name }
                    refresh { listOf(value) }
                }
            }
            assertEquals(KEY, entry.key)
            assertEquals(DESCRIPTION, entry.description)
            assertThat(entry.uiNode)
                .asInstanceOf<ChoiceBox<*>>()

            assertEquals(default, entry.default)
            assertEquals(default, entry.get())
            assertNull(entry.getWithoutDefault())
            assertNull(entry.checkError())

            entry.set(value)
            assertThat(value)
                .isEqualTo(entry.get())
                .isEqualTo(entry.getWithoutDefault())
            assertNull(entry.checkError())
        }

        @Test
        fun `without default and ui`(config: Config) {
            val entry = config.serialized<Impl>(KEY) {
                description = DESCRIPTION
                serializer = Impl
                check(NonnullConfigChecker)
            }
            assertEquals(KEY, entry.key)
            assertEquals(DESCRIPTION, entry.description)
            assertNull(entry.uiNode)

            assertNull(entry.default)
            assertNull(entry.get())
            assertNull(entry.getWithoutDefault())
            assertNotNull(entry.checkError())

            entry.set(value)
            assertThat(value)
                .isEqualTo(entry.get())
                .isEqualTo(entry.getWithoutDefault())
            assertNull(entry.checkError())
        }

        @TestFactory
        fun `blank key`(config: Config) = listOf("", " ")
            .map { key ->
                dynamicTest("Key: \"$key\"") {
                    assertThrows<IllegalArgumentException> {
                        config.serialized<Impl>(key) {
                            description = DESCRIPTION
                            serializer = Impl
                            check(NonnullConfigChecker)
                        }
                    }
                }
            }

        @Test
        fun `missing description`(config: Config) {
            assertThrows<IllegalStateException> {
                config.serialized<Impl>(KEY) {
                    serializer = Impl
                    check(NonnullConfigChecker)
                }
            }
        }

        @Test
        fun `missing serializer`(config: Config) {
            assertThrows<IllegalStateException> {
                config.serialized<Impl>(KEY) {
                    description = DESCRIPTION
                    check(NonnullConfigChecker)
                }
            }
        }

        @Test
        fun `missing checker`(config: Config) {
            assertThrows<IllegalStateException> {
                config.serialized<Impl>(KEY) {
                    description = DESCRIPTION
                    serializer = Impl
                }
            }
        }
    }

    private companion object {
        const val KEY = "key"
        const val DESCRIPTION = "description"
    }
}
