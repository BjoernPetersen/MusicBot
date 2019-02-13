# Declaration

## Bases

A plugin may implement several base interfaces, which are annotated by `@Base`, `@ActiveBase`
or `@IdBase`. Superclasses which are not annotated with either of those are ignored.

### `@Base`

Use this to annotate a base class or interface which other plugins might depend on.
It is recommended to create a base interface or class for your plugin, even if you don't plan on
creating multiple implementations yourself.

### `@ActiveBase`

This annotation marks a base which is either actively used by the bot 
(like `Provider` and `Suggester`), or actively provides a feature and should be enabled even if no
other active plugin depends on it.

Plugins implementing an active base **must** also have an ID base.

### `@IdBase`

If your plugin implements an active base (such as `Provider` or `Suggester`),
it needs a base that identifies it. There will always be only one enabled plugin per ID base.

For example, a provider of Spotify songs should implement a base interface 
`SpotifyProvider` annotated with `@IdBase`.
Songs that implementation provides would not have a reference to the implementation itself, but 
to its ID, `SpotifyProvider`. If someone else comes along now and creates an alternative implementation 
of that ID base, songs created by the first implementation could still be used even though the
implementation changed. 

If, for some reason, you absolutely don't want to create a separate base interface, 
you may also annotate your implementation with `@IdBase` directly.

A plugin must not have more than one ID base.

### Examples

#### Provider

A `Provider` for Spotify songs may have a base interface `SpotifyProvider`, which is declared
as an `@IdBase`.

All implementations of the `SpotifyProvider` base must use the same song ID format to be compatible.

```kotlin
@IdBase("Spotify")
interface SpotifyProvider : Provider
class SpotifyProviderImpl : SpotifyProvider
```

#### PlaybackFactory

The `PlaybackFactory` interface does not have value in itself, so instead, 
just specializations of the `PlaybackFactory` interface are annotated with `@Base`,
for example `Mp3PlaybackFactory` or `YouTubePlaybackFactory`.

---

An implementation using VLC to play a variety of file formats may look like this:

```kotlin
interface VlcPlaybackFactory : Mp3PlaybackFactory, AacPlaybackFactory, FlacPlaybackFactory
class VlcPlaybackFactoryImpl : VlcPlaybackFactory
```

Note that the base interfaces used here are predefined in the core library.

---

An implementation for Spotify songs may look like this:

```kotlin
@Base
interface SpotifyPlaybackFactory : PlaybackFactory {
    fun getPlayback(spotifySongId: String): Playback
}
class SpotifyPlaybackFactoryImpl : SpotifyPlaybackFactory
```

Note that in this case, the base interface declares the crucial `getPlayback(...)` method itself.
Without it, a `PlaybackFactory` would not be not useful at all.

## Service

In order for the bot to find your plugin, you have to create a file with the fully qualified name
of the implemented plugin interface (e.g. `net.bjoernpetersen.musicbot.spi.plugin.Provider`)
in the `META-INF/services` directory. The file should contain all your implementations of the
base interface, each on its own line.
