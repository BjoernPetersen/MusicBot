# Resources

Provider plugins may allocate resources when a song is loaded via the `Provider.loadSong()` method.
Such a resource could be reusable (e.g. a downloaded file) or single-use (e.g. a data stream).

Whenever the Core determines that the resource for a song might be needed soon,
it will check whether a valid resource is cached for the song, and if not, call your `loadSong()`
implementation.

The resource might be discarded without ever being used, but before a resource is discarded, it is
guaranteed that the `Resource.free()` method will be called.

## Examples

### No resources

If you don't need to allocate any resources, you can simply return the `NoResource` singleton.

```kotlin
override fun loadSong(song: Song): Resource = NoResource
```

### File resources

A commonly used type of resource is a file resource. Let's say your provider needs to download an
MP3-file before playing it. That MP3-file of course needs to be deleted at some point in the future,
because storage space is typically finite.
For that use case, the `FileResource` class is already included in the Core library.

In this case your `loadSong()` and `supplyPlayback()` methods might look the following way:

```kotlin

@Base
interface MyPlaybackFactory : FilePlaybackFactory

@IdBase
class MyProvider : Provider {
    @Inject
    private lateinit var myPlaybackFactory: MyPlaybackFactory

    // ...

    override fun loadSong(song: Song): Resource {
        val file: File = downloadSong(song)
        return FileResource(file)
    }

    override fun supplyPlayback(song: Song, resource: Resource): Playback {
        val fileResource = resource as FileResource
        return myPlaybackFactory.createPlayback(fileResource.file)
    }
}
```
