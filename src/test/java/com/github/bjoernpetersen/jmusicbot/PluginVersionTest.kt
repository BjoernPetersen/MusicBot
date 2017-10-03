package com.github.bjoernpetersen.jmusicbot

import com.github.bjoernpetersen.jmusicbot.MusicBot.Builder
import com.github.bjoernpetersen.jmusicbot.config.Config
import com.github.bjoernpetersen.jmusicbot.config.Config.Entry
import com.github.bjoernpetersen.jmusicbot.config.DummyConfigStorageAdapter
import com.github.bjoernpetersen.jmusicbot.platform.Platform
import com.github.bjoernpetersen.jmusicbot.platform.Support
import com.github.zafarkhaja.semver.Version
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.IOException

internal class PluginVersionTest {

    private lateinit var botVersion: Version
    private lateinit var builder: Builder

    @BeforeEach
    fun initBuilder() {
        builder = Builder(Config(DummyConfigStorageAdapter()))
    }

    @BeforeEach
    fun initBotVersion() {
        botVersion = MusicBot.getVersion()
    }

    @Disabled("Does not work for MusicBot 0.x.x")
    @Test
    fun majorTooOld() {
        val version = Version.forIntegers(
                botVersion.majorVersion - 1,
                botVersion.minorVersion,
                botVersion.patchVersion
        )
        assertTrue(builder.isUnsupported(VersionPlugin(version)))
    }

    @Test
    fun majorTooNew() {
        assertTrue(builder.isUnsupported(VersionPlugin(botVersion.incrementMajorVersion())))
    }

    @Test
    fun minorTooOld() {
        val version = Version.forIntegers(
                botVersion.majorVersion,
                botVersion.minorVersion - 1,
                botVersion.patchVersion
        )
        assertTrue(builder.isUnsupported(VersionPlugin(version)))
    }

    @Test
    fun minorTooNew() {
        assertTrue(builder.isUnsupported(VersionPlugin(botVersion.incrementMinorVersion())))
    }

    @Test
    fun differentPatch() {
        val olderVersion = Version.forIntegers(
                botVersion.majorVersion,
                botVersion.minorVersion,
                Math.max(0, botVersion.patchVersion - 1)
        )
        assertFalse(builder.isUnsupported(VersionPlugin(olderVersion)))
        assertFalse(builder.isUnsupported(VersionPlugin(botVersion.incrementPatchVersion())))
    }

    @Test
    fun nullUnsupportedMessage() {
        assertNull(builder.getUnsupportedMessage("TESTS", emptyList()))
        assertNull(builder.getUnsupportedMessage("TESTS", setOf(VersionPlugin(botVersion))))
        val maxUnsupported = VersionPlugin(botVersion, botVersion.incrementMajorVersion())
        assertNull(builder.getUnsupportedMessage("TESTS", setOf(maxUnsupported)))
    }

    @Test
    fun unsupportedMessageContainsPlugins() {
        val plugins = setOf(
                VersionPlugin(botVersion),
                VersionPlugin(botVersion.incrementMajorVersion()),
                VersionPlugin(botVersion.incrementMinorVersion()),
                VersionPlugin(botVersion.incrementPatchVersion())
        )
        val msg = builder.getUnsupportedMessage("TESTKINDS", plugins)
        assertNotNull(msg)
        msg!!
        assertTrue(msg.contains(botVersion.incrementMajorVersion().toString()))
        assertTrue(msg.contains(botVersion.incrementMinorVersion().toString()))
        assertFalse(msg.contains(botVersion.incrementPatchVersion().toString()))
        assertFalse(msg.contains(botVersion.toString()))
    }

    @Test
    fun unsupportedMessageContainsKind() {
        val kind = "TESTKINDS"
        val msg = builder.getUnsupportedMessage(kind, setOf(VersionPlugin(botVersion.incrementMajorVersion())))
        assertNotNull(msg)
        assertTrue(msg!!.contains(kind))
    }

    @Test
    fun sameVersion() {
        val version = Version.forIntegers(1, 1, 1);
        assertFalse(builder.isUnsupported(version, version, version))
    }

    @Test
    fun newerMajorOlderMinor() {
        val version = Version.forIntegers(6, 4, 0)
        val minVersion = Version.forIntegers(5, 5, 0)
        val maxVersion = Version.forIntegers(7, 0, 0)
        assertFalse(builder.isUnsupported(version, minVersion, maxVersion))
    }

    @Test
    fun exactMinVersion() {
        val version = Version.forIntegers(5, 5, 5)
        val maxVersion = version.incrementMajorVersion()
        assertFalse(builder.isUnsupported(version, version, maxVersion))
    }

    @Test
    fun exactMaxVersion() {
        val minVersion = Version.forIntegers(5, 5, 5)
        val version = minVersion.incrementMajorVersion()
        assertFalse(builder.isUnsupported(version, minVersion, version))
    }

    @Test
    fun olderPatch() {
        val minVersion = Version.forIntegers(5, 5, 5)
        val maxVersion = Version.forIntegers(6, 5, 5)
        val version = Version.forIntegers(5, 5, 0)
        assertFalse(builder.isUnsupported(version, minVersion, maxVersion))
    }

    @Test
    fun newerPatch() {
        val minVersion = Version.forIntegers(5, 5, 5)
        val maxVersion = Version.forIntegers(6, 5, 5)
        val version = Version.forIntegers(6, 5, 11)
        assertFalse(builder.isUnsupported(version, minVersion, maxVersion))
    }

    @Test
    fun olderMinor() {
        val minVersion = Version.forIntegers(5, 5, 5)
        val maxVersion = Version.forIntegers(6, 5, 5)
        val version = Version.forIntegers(5, 4, 5)
        assertTrue(builder.isUnsupported(version, minVersion, maxVersion))
    }

    @Test
    fun newerMinor() {
        val minVersion = Version.forIntegers(5, 5, 5)
        val maxVersion = Version.forIntegers(6, 5, 5)
        val version = Version.forIntegers(6, 6, 5)
        assertTrue(builder.isUnsupported(version, minVersion, maxVersion))
    }

    @Test
    fun olderMajor() {
        val minVersion = Version.forIntegers(5, 5, 5)
        val maxVersion = Version.forIntegers(6, 5, 5)
        val version = Version.forIntegers(4, 5, 5)
        assertTrue(builder.isUnsupported(version, minVersion, maxVersion))
    }

    @Test
    fun newerMajor() {
        val minVersion = Version.forIntegers(5, 5, 5)
        val maxVersion = Version.forIntegers(6, 5, 5)
        val version = Version.forIntegers(7, 5, 5)
        assertTrue(builder.isUnsupported(version, minVersion, maxVersion))
    }

    private class VersionPlugin(val minVersion: Version, val maxVersion: Version) : Plugin {

        constructor(singleVersion: Version) : this(singleVersion, singleVersion)

        override fun initializeConfigEntries(config: Config): List<Entry> {
            return emptyList()
        }

        override fun dereferenceConfigEntries() {}

        override fun getReadableName(): String {
            return "Version [$minVersion, $maxVersion]";
        }

        override fun getMinSupportedVersion(): Version {
            return minVersion
        }

        override fun getMaxSupportedVersion(): Version {
            return maxVersion
        }

        override fun getSupport(platform: Platform): Support {
            return Support.YES
        }

        @Throws(IOException::class)
        override fun close() {
        }
    }
}
