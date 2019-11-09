package net.bjoernpetersen.musicbot.api.config

import io.mockk.mockk
import java.io.File
import java.nio.file.Path
import kotlinx.coroutines.runBlocking
import net.bjoernpetersen.musicbot.test.api.config.ConfigExtension
import net.bjoernpetersen.musicbot.test.asInstanceOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ConfigExtension::class)
class ConfigUiDslTest {

    @Nested
    inner class ActionButtonDsl {
        @Test
        fun valid() {
            var called = false
            val node: ActionButton<Impl> = actionButton {
                label = LABEL
                describe { it.name }
                action {
                    called = true
                    true
                }
            }

            val value = Impl()
            assertEquals(LABEL, node.label)
            assertEquals(value.name, node.descriptor(value))

            runBlocking {
                val entry = mockk<Config.SerializedEntry<Impl>>()
                assertTrue(node.action(entry))
            }

            assertTrue(called)
        }

        @Test
        fun application(config: Config) {
            val entry: Config.SerializedEntry<Impl> by config.serialized {
                serializer = Impl
                description = DESCRIPTION
                check { null }
                actionButton {
                    label = LABEL
                    describe { it.name }
                    action { true }
                }
            }
            assertThat(entry.uiNode)
                .asInstanceOf<ActionButton<Impl>>()
        }

        @Test
        fun missingLabel() {
            assertThrows<IllegalStateException> {
                actionButton<Impl> {
                    describe { it.name }
                    action { true }
                }
            }
        }

        @Test
        fun missingDescriptor() {
            assertThrows<IllegalStateException> {
                actionButton<Impl> {
                    label = LABEL
                    action { true }
                }
            }
        }

        @Test
        fun missingAction() {
            assertThrows<IllegalStateException> {
                actionButton<Impl> {
                    label = LABEL
                    describe { it.name }
                }
            }
        }
    }

    @Nested
    inner class PathChooserDsl {
        @Nested
        inner class OpenFile {
            @Test
            fun valid() {
                val node: PathChooser = openFile()
                assertFalse(node.isDirectory)
                assertTrue(node.isOpen)
            }

            @Test
            fun application(config: Config) {
                val entry: Config.SerializedEntry<Path> by config.serialized {
                    serializer = PathSerializer
                    description = DESCRIPTION
                    check { null }
                    openFile()
                }
                assertThat(entry.uiNode)
                    .asInstanceOf<PathChooser>()
                    .returns(false, PathChooser::isDirectory)
                    .returns(true, PathChooser::isOpen)
            }
        }

        @Nested
        inner class OpenDirectory {
            @Test
            fun valid() {
                val node: PathChooser = openDirectory()
                assertTrue(node.isDirectory)
                assertTrue(node.isOpen)
            }

            @Test
            fun application(config: Config) {
                val entry: Config.SerializedEntry<Path> by config.serialized {
                    serializer = PathSerializer
                    description = DESCRIPTION
                    check { null }
                    openDirectory()
                }
                assertThat(entry.uiNode)
                    .asInstanceOf<PathChooser>()
                    .returns(true, PathChooser::isDirectory)
                    .returns(true, PathChooser::isOpen)
            }
        }

        @Nested
        inner class SaveFile {
            @Test
            fun valid() {
                val node: PathChooser = saveFile()
                assertFalse(node.isDirectory)
                assertFalse(node.isOpen)
            }

            @Test
            fun application(config: Config) {
                val entry: Config.SerializedEntry<Path> by config.serialized {
                    serializer = PathSerializer
                    description = DESCRIPTION
                    check { null }
                    saveFile()
                }
                assertThat(entry.uiNode)
                    .asInstanceOf<PathChooser>()
                    .returns(false, PathChooser::isDirectory)
                    .returns(false, PathChooser::isOpen)
            }
        }
    }

    @Nested
    inner class FileChooserDsl {
        @Nested
        inner class OpenFile {
            @Test
            fun valid() {
                val node: FileChooser = openLegacyFile()
                assertFalse(node.isDirectory)
                assertTrue(node.isOpen)
            }

            @Test
            fun application(config: Config) {
                val entry: Config.SerializedEntry<File> by config.serialized {
                    serializer = FileSerializer
                    description = DESCRIPTION
                    check { null }
                    openLegacyFile()
                }
                assertThat(entry.uiNode)
                    .asInstanceOf<FileChooser>()
                    .returns(false, FileChooser::isDirectory)
                    .returns(true, FileChooser::isOpen)
            }
        }

        @Nested
        inner class OpenDirectory {
            @Test
            fun valid() {
                val node: FileChooser = openLegacyDirectory()
                assertTrue(node.isDirectory)
                assertTrue(node.isOpen)
            }

            @Test
            fun application(config: Config) {
                val entry: Config.SerializedEntry<File> by config.serialized {
                    serializer = FileSerializer
                    description = DESCRIPTION
                    check { null }
                    openLegacyDirectory()
                }
                assertThat(entry.uiNode)
                    .asInstanceOf<FileChooser>()
                    .returns(true, FileChooser::isDirectory)
                    .returns(true, FileChooser::isOpen)
            }
        }

        @Nested
        inner class SaveFile {
            @Test
            fun valid() {
                val node: FileChooser = saveLegacyFile()
                assertFalse(node.isDirectory)
                assertFalse(node.isOpen)
            }

            @Test
            fun application(config: Config) {
                val entry: Config.SerializedEntry<File> by config.serialized {
                    serializer = FileSerializer
                    description = DESCRIPTION
                    check { null }
                    saveLegacyFile()
                }
                assertThat(entry.uiNode)
                    .asInstanceOf<FileChooser>()
                    .returns(false, FileChooser::isDirectory)
                    .returns(false, FileChooser::isOpen)
            }
        }
    }

    @Nested
    inner class ChoiceBoxDsl {
        private val refreshImpl = Impl("refreshed")

        @Test
        fun eagerValid() {
            var isRefreshed = false
            val node: ChoiceBox<Impl> = choiceBox {
                describe { it.name }
                refresh {
                    isRefreshed = true
                    listOf(refreshImpl)
                }
            }

            runBlocking {
                assertEquals(refreshImpl.name, node.descriptor(refreshImpl))
                assertFalse(node.lazy)
                assertEquals(listOf(refreshImpl), node.refresh())
                assertTrue(isRefreshed)
            }
        }

        @Test
        fun lazyValid() {
            var isRefreshed = false
            val node: ChoiceBox<Impl> = choiceBox {
                describe { it.name }
                refresh {
                    isRefreshed = true
                    listOf(refreshImpl)
                }
                lazy()
            }

            runBlocking {
                assertEquals(refreshImpl.name, node.descriptor(refreshImpl))
                assertTrue(node.lazy)
                assertEquals(listOf(refreshImpl), node.refresh())
                assertTrue(isRefreshed)
            }
        }

        @Test
        fun application(config: Config) {
            val entry: Config.SerializedEntry<Impl> by config.serialized {
                serializer = Impl
                description = DESCRIPTION
                check { null }
                choiceBox {
                    describe { it.name }
                    refresh { emptyList() }
                }
            }
            assertThat(entry.uiNode)
                .asInstanceOf<ChoiceBox<Impl>>()
        }

        @Test
        fun `missing descriptor`() {
            assertThrows<IllegalStateException> {
                choiceBox<Impl> {
                    refresh { emptyList() }
                }
            }
        }

        @Test
        fun `missing refresh`() {
            assertThrows<IllegalStateException> {
                choiceBox<Impl> {
                    describe { it.name }
                }
            }
        }
    }

    private companion object {
        const val LABEL = "test-label"
        const val DESCRIPTION = "test-description"
    }
}
