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
 * registered under a random token whose resolution is confined to a single
 * base directory, and any path-traversal attempt that escapes it is rejected.
 *
 * When the model lives inside the open project, the project root is used as the
 * base and the model is addressed by its project-relative path. This lets a
 * model reference sibling assets that sit outside its own folder (for example a
 * glTF whose textures live in a `../glTF/` directory) while still refusing any
 * path that escapes the project. When the model is outside the project (or no
 * project root is known), the base falls back to the model's own directory.
 *
 * Because the project root can be a wide base, resolution is additionally
 * confined to an allowlist of model/texture/buffer file extensions, so a
 * malicious model cannot reference arbitrary non-asset project files (source
 * code, `.env`, `.git` internals, etc.) as external buffers or images.
 */
object Model3DAssetRegistry {

    /**
     * File extensions that may be served from disk: model containers, their
     * geometry/material sidecars, and texture image formats the viewer loads.
     * Anything else (source code, configs, secrets, extension-less files) is
     * rejected even when it sits within the traversal base.
     */
    private val ALLOWED_ASSET_EXTENSIONS = setOf(
        // Model containers and sidecars.
        "gltf", "glb", "obj", "mtl", "stl", "bin",
        // Texture / image formats.
        "png", "jpg", "jpeg", "webp", "gif", "bmp", "tga",
        "ktx2", "ktx", "basis", "dds", "hdr", "exr",
    )

    private data class Entry(val baseDir: Path, val urlPath: String)

    private val entries = ConcurrentHashMap<String, Entry>()

    /**
     * Register a model file, returning a token used to build viewer URLs.
     *
     * @param modelFile the model file on disk.
     * @param projectRoot the open project's root directory, if any. When the
     *   model is inside it, the whole project becomes the traversal boundary.
     */
    fun register(modelFile: Path, projectRoot: Path? = null): String {
        val normalized = modelFile.toAbsolutePath().normalize()
        val root = projectRoot?.toAbsolutePath()?.normalize()
        val (baseDir, relPath) =
            if (root != null && normalized != root && normalized.startsWith(root)) {
                root to root.relativize(normalized)
            } else {
                normalized.parent to normalized.fileName
            }
        // Path.toString() uses the OS separator; rebuild with '/' for URLs.
        val urlPath = relPath.joinToString("/") { it.toString() }
        val token = UUID.randomUUID().toString().replace("-", "")
        entries[token] = Entry(baseDir, urlPath)
        return token
    }

    fun unregister(token: String) {
        entries.remove(token)
    }

    /**
     * The URL path (relative to the token) that addresses the main model file,
     * e.g. `RobotExpressive.glb` or `models/gltf/Helmet/glTF/Helmet.gltf`.
     */
    fun urlPath(token: String): String? = entries[token]?.urlPath

    /**
     * Resolve a relative path against the token's base directory. Returns null
     * if the token is unknown, the resolved path escapes the base directory
     * (path-traversal guard), or the file is not an allowlisted asset type.
     */
    fun resolve(token: String, relativePath: String): Path? {
        val entry = entries[token] ?: return null
        val cleaned = relativePath.trimStart('/')
        if (cleaned.isEmpty()) return null
        val base = entry.baseDir.toAbsolutePath().normalize()
        val resolved = base.resolve(cleaned).normalize()
        // Must stay within the base directory.
        if (!resolved.startsWith(base)) return null
        // Serve allowlisted asset types, plus the token's own main model file even
        // when its extension isn't a generic asset type (e.g. a three.js .json
        // model). This keeps arbitrary non-asset project files unreadable.
        val ext = resolved.fileName.toString().substringAfterLast('.', "").lowercase()
        if (ext in ALLOWED_ASSET_EXTENSIONS) return resolved
        if (resolved == base.resolve(entry.urlPath).normalize()) return resolved
        return null
    }
}
