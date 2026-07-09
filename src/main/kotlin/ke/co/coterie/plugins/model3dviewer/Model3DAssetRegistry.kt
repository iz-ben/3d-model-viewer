package ke.co.coterie.plugins.model3dviewer

import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Maps opaque per-file tokens to the on-disk directory that backs a viewer.
 *
 * The bundled viewer app is served from plugin resources; the actual model and
 * its relative assets (e.g. a glTF's `Textures/foo.png`) are served straight
 * from disk. To avoid exposing the whole filesystem, each opened model is
 * registered under a random token that resolves only to files *within* the
 * model's own directory, and any path-traversal attempt is rejected.
 */
object Model3DAssetRegistry {

    private data class Entry(val baseDir: Path, val mainName: String)

    private val entries = ConcurrentHashMap<String, Entry>()

    /** Register a model file, returning a token used to build viewer URLs. */
    fun register(modelFile: Path): String {
        val normalized = modelFile.toAbsolutePath().normalize()
        val token = UUID.randomUUID().toString().replace("-", "")
        entries[token] = Entry(normalized.parent, normalized.fileName.toString())
        return token
    }

    fun unregister(token: String) {
        entries.remove(token)
    }

    /** The main model file name for a token (e.g. `RobotExpressive.glb`). */
    fun mainName(token: String): String? = entries[token]?.mainName

    /**
     * Resolve a relative path against the token's base directory. Returns null
     * if the token is unknown or the resolved path escapes the base directory
     * (path-traversal guard).
     */
    fun resolve(token: String, relativePath: String): Path? {
        val entry = entries[token] ?: return null
        val cleaned = relativePath.trimStart('/')
        if (cleaned.isEmpty()) return null
        val base = entry.baseDir.toAbsolutePath().normalize()
        val resolved = base.resolve(cleaned).normalize()
        // Must stay within the base directory.
        return if (resolved.startsWith(base)) resolved else null
    }
}
