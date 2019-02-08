package net.bjoernpetersen.musicbot.api.loader

import net.bjoernpetersen.musicbot.spi.loader.Resource
import java.io.File
import java.io.IOException

/**
 * A file resource. The file will be deleted when [free] is called.
 *
 * @param file any file
 */
class FileResource(val file: File) : Resource {

    private var deleted = false
    override fun free() {
        deleted = file.delete()
        if (!deleted) throw IOException("Could not delete ${file.path}")
    }

    override fun isValid() = !deleted && file.isFile
}
