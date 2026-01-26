package ke.co.coterie.plugins.model3dviewer

import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File

/**
 * Parser for GLTF files that extracts external asset references.
 * GLTF is a JSON-based format that can reference external files for:
 * - buffers (binary data, typically .bin files)
 * - images (textures, typically .png, .jpg, etc.)
 *
 * This parser identifies these references and resolves them to absolute file paths.
 */
object GltfAssetParser {

    private val gson = Gson()

    /**
     * Parses a GLTF file and returns a list of all referenced external asset files
     * along with their normalized relative paths.
     *
     * @param gltfFile The GLTF file to parse
     * @return A list of Pairs containing (resolved File, normalized relative path with forward slashes)
     *         Missing files are logged and skipped.
     */
    fun parseReferencedAssets(gltfFile: File): List<Pair<File, String>> {
        if (!gltfFile.exists() || !gltfFile.canRead()) {
            println("GLTF Parser: Cannot read file ${gltfFile.absolutePath}")
            return emptyList()
        }

        val parentDir = gltfFile.parentFile
        val referencedFiles = mutableListOf<Pair<File, String>>()

        try {
            val jsonContent = gltfFile.readText()
            val gltfJson = gson.fromJson(jsonContent, JsonObject::class.java)

            // Extract buffer URIs
            extractUrisFromArray(gltfJson, "buffers").forEach { uri ->
                resolveAssetFile(uri, parentDir)?.let { referencedFiles.add(it) }
            }

            // Extract image URIs
            extractUrisFromArray(gltfJson, "images").forEach { uri ->
                resolveAssetFile(uri, parentDir)?.let { referencedFiles.add(it) }
            }

        } catch (e: Exception) {
            println("GLTF Parser: Error parsing GLTF file ${gltfFile.name}: ${e.message}")
        }

        return referencedFiles
    }

    /**
     * Extracts URI values from a named array in the GLTF JSON.
     *
     * @param gltfJson The root GLTF JSON object
     * @param arrayName The name of the array to extract URIs from (e.g., "buffers", "images")
     * @return A list of URI strings found in the array
     */
    private fun extractUrisFromArray(gltfJson: JsonObject, arrayName: String): List<String> {
        val uris = mutableListOf<String>()

        if (gltfJson.has(arrayName) && gltfJson.get(arrayName).isJsonArray) {
            val array = gltfJson.getAsJsonArray(arrayName)
            for (element in array) {
                if (element.isJsonObject) {
                    val obj = element.asJsonObject
                    if (obj.has("uri")) {
                        val uri = obj.get("uri").asString
                        uris.add(uri)
                    }
                }
            }
        }

        return uris
    }

    /**
     * Resolves a URI to an absolute file path and returns it with the normalized relative path.
     * Skips data URIs (embedded base64 content) and returns null for missing files.
     *
     * @param uri The URI string from the GLTF file
     * @param parentDir The parent directory of the GLTF file (base for relative paths)
     * @return A Pair of (resolved File, normalized relative path with forward slashes) if the file exists, null otherwise
     */
    private fun resolveAssetFile(uri: String, parentDir: File): Pair<File, String>? {
        // Skip data URIs (embedded content)
        if (uri.startsWith("data:")) {
            println("GLTF Parser: Skipping embedded data URI")
            return null
        }

        // Normalize the relative path and ensure forward slashes
        val normalizedRelativePath = File(uri).toPath().normalize().toString().replace('\\', '/')

        // Resolve relative URI against parent directory
        val assetFile = File(parentDir, uri).canonicalFile

        return if (assetFile.exists()) {
            println("GLTF Parser: Found referenced asset: ${assetFile.name} (relative path: $normalizedRelativePath)")
            Pair(assetFile, normalizedRelativePath)
        } else {
            println("GLTF Parser: Warning - Referenced asset not found: $uri (resolved to ${assetFile.absolutePath})")
            null
        }
    }
}
