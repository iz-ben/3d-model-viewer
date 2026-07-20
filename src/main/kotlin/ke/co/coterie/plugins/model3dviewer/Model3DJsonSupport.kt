package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.vfs.VirtualFile

/**
 * Detects three.js model JSON files (BufferGeometry / Object / Scene) by content
 * rather than by extension.
 *
 * `.json` is far too common to claim as a plugin file type, so the 3D preview is
 * offered only for files that actually contain a three.js model. A cheap sniff of
 * the file's head is enough: three.js serializers emit a `metadata` block up front
 * whose `type` names the model kind, e.g. `"metadata":{"type":"BufferGeometry"}`.
 */
object Model3DJsonSupport {

    private const val SNIFF_LIMIT = 64 * 1024
    private const val MAX_FILE_SIZE = 64L * 1024 * 1024

    // The `type` must sit inside the `metadata` object, which keeps ordinary JSON
    // (configs, package.json, schemas that merely contain a "type" key) from matching.
    private val MODEL_METADATA = Regex(
        "\"metadata\"\\s*:\\s*\\{[^}]*\"type\"\\s*:\\s*\"(BufferGeometry|Geometry|Object|Scene)\"",
        RegexOption.IGNORE_CASE,
    )

    fun isThreeJsModelJson(file: VirtualFile?): Boolean {
        if (file == null || file.isDirectory) return false
        if (file.extension?.lowercase() != "json") return false
        val length = file.length
        if (length <= 0 || length > MAX_FILE_SIZE) return false
        return try {
            val bytes = file.inputStream.use { it.readNBytes(SNIFF_LIMIT) }
            MODEL_METADATA.containsMatchIn(String(bytes, Charsets.UTF_8))
        } catch (e: Exception) {
            false
        }
    }
}
