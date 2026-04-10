package ke.co.coterie.plugins.model3dviewer

import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Parser for GLTF/GLB files that extracts metadata and external asset references.
 * 
 * GLTF is a JSON-based format that can reference external files for:
 * - buffers (binary data, typically .bin files)
 * - images (textures, typically .png, .jpg, etc.)
 *
 * GLB is a binary container format that embeds GLTF JSON and binary data in chunks.
 *
 * This parser identifies these references and resolves them to absolute file paths.
 */
object GltfAssetParser {

    private val gson = Gson()
    
    // GLB constants
    private const val GLB_MAGIC = 0x46546C67 // "glTF" in little-endian
    private const val GLB_CHUNK_TYPE_JSON = 0x4E4F534A // "JSON" in little-endian
    private const val GLB_CHUNK_TYPE_BIN = 0x004E4942 // "BIN\0" in little-endian

    /**
     * Debug flag to control verbose metadata logging.
     * Set to true to enable detailed logging of GLB/GLTF parsing.
     */
    var debugLogging = false

    /**
     * Data class representing GLTF/GLB asset metadata.
     */
    data class GltfMetadata(
        val version: String?,
        val generator: String?,
        val copyright: String?,
        val minVersion: String?,
        val extras: Map<String, Any>?
    )
    
    /**
     * Data class representing GLB file information including embedded assets.
     */
    data class GlbInfo(
        val metadata: GltfMetadata?,
        val glbVersion: Int,
        val totalFileSize: Int,
        val jsonChunkSize: Int,
        val binaryChunkSize: Int?,
        val meshCount: Int,
        val materialCount: Int,
        val textureCount: Int,
        val imageCount: Int,
        val animationCount: Int,
        val bufferCount: Int,
        val embeddedImages: List<EmbeddedImage>
    )
    
    /**
     * Combined result from parsing a GLB file in a single pass.
     * Contains both metadata and external asset references.
     */
    data class GlbParseResult(
        val info: GlbInfo?,
        val referencedAssets: List<Pair<File, String>>
    )

    /**
     * Data class representing an embedded image in a GLB file.
     */
    data class EmbeddedImage(
        val index: Int,
        val name: String?,
        val mimeType: String?,
        val bufferViewIndex: Int?,
        val uri: String?
    )
    
    /**
     * Data class representing a texture reference.
     */
    data class TextureInfo(
        val index: Int,
        val name: String?,
        val sourceImageIndex: Int?,
        val samplerIndex: Int?
    )
    
    /**
     * Data class representing a buffer.
     */
    data class BufferInfo(
        val index: Int,
        val name: String?,
        val uri: String?,
        val byteLength: Int
    )
    
    /**
     * Data class representing a material.
     */
    data class MaterialInfo(
        val index: Int,
        val name: String?,
        val baseColorTextureIndex: Int?,
        val normalTextureIndex: Int?,
        val metallicRoughnessTextureIndex: Int?,
        val emissiveTextureIndex: Int?,
        val occlusionTextureIndex: Int?
    )
    
    /**
     * Data class containing all imported assets.
     */
    data class ImportedAssets(
        val images: List<EmbeddedImage>,
        val textures: List<TextureInfo>,
        val buffers: List<BufferInfo>,
        val materials: List<MaterialInfo>
    )

    /**
     * Parses a GLTF file and logs/returns its metadata (asset section).
     *
     * @param gltfFile The GLTF file to parse
     * @return GltfMetadata containing version, generator, copyright, etc., or null if parsing fails
     */
    fun parseMetadata(gltfFile: File): GltfMetadata? {
        if (!gltfFile.exists() || !gltfFile.canRead()) {
            println("GLTF Parser: Cannot read file ${gltfFile.absolutePath}")
            return null
        }

        try {
            val jsonContent = gltfFile.readText()
            val gltfJson = gson.fromJson(jsonContent, JsonObject::class.java)

            val metadata = extractMetadata(gltfJson, gltfFile.name)
            
            // Log imported assets
            extractAndLogImportedAssets(gltfJson, gltfFile.name)
            
            return metadata
        } catch (e: Exception) {
            println("GLTF Parser: Error parsing GLTF metadata for ${gltfFile.name}: ${e.message}")
            return null
        }
    }
    
    /**
     * Parses a GLB file and logs/returns its metadata and embedded asset information.
     *
     * @param glbFile The GLB file to parse
     * @return GlbInfo containing metadata and embedded assets, or null if parsing fails
     */
    fun parseGlbMetadata(glbFile: File): GlbInfo? {
        if (!glbFile.exists() || !glbFile.canRead()) {
            println("GLB Parser: Cannot read file ${glbFile.absolutePath}")
            return null
        }

        try {
            RandomAccessFile(glbFile, "r").use { raf ->
                // Read 12-byte header
                val headerBuffer = ByteArray(12)
                raf.readFully(headerBuffer)
                val header = ByteBuffer.wrap(headerBuffer).order(ByteOrder.LITTLE_ENDIAN)
                
                val magic = header.int
                if (magic != GLB_MAGIC) {
                    println("GLB Parser: Invalid GLB magic number in ${glbFile.name}")
                    return null
                }
                
                val glbVersion = header.int
                val totalLength = header.int
                
                // Read JSON chunk header (8 bytes)
                val chunkHeaderBuffer = ByteArray(8)
                raf.readFully(chunkHeaderBuffer)
                val chunkHeader = ByteBuffer.wrap(chunkHeaderBuffer).order(ByteOrder.LITTLE_ENDIAN)
                
                val jsonChunkLength = chunkHeader.int
                val jsonChunkType = chunkHeader.int
                
                if (jsonChunkType != GLB_CHUNK_TYPE_JSON) {
                    println("GLB Parser: First chunk is not JSON in ${glbFile.name}")
                    return null
                }
                
                // Validate jsonChunkLength to prevent OOM from malformed files
                val remainingBytes = totalLength - 20 // 12-byte header + 8-byte chunk header already read
                if (jsonChunkLength !in 0..remainingBytes) {
                    println("GLB Parser: Invalid JSON chunk length ($jsonChunkLength) in ${glbFile.name}, remaining bytes: $remainingBytes")
                    return null
                }
                
                // Read JSON chunk data
                val jsonBytes = ByteArray(jsonChunkLength)
                raf.readFully(jsonBytes)
                val jsonContent = String(jsonBytes, Charsets.UTF_8).trimEnd('\u0000', ' ')
                val gltfJson = gson.fromJson(jsonContent, JsonObject::class.java)
                
                // Check for binary chunk
                var binaryChunkSize: Int? = null
                if (raf.filePointer < totalLength.toLong()) {
                    val binChunkHeaderBuffer = ByteArray(8)
                    if (raf.read(binChunkHeaderBuffer) == 8) {
                        val binChunkHeader = ByteBuffer.wrap(binChunkHeaderBuffer).order(ByteOrder.LITTLE_ENDIAN)
                        val binChunkLength = binChunkHeader.int
                        val binChunkType = binChunkHeader.int
                        if (binChunkType == GLB_CHUNK_TYPE_BIN) {
                            binaryChunkSize = binChunkLength
                        }
                    }
                }
                
                // Extract metadata
                val metadata = extractMetadata(gltfJson, glbFile.name)
                
                // Extract asset counts
                val meshCount = getArraySize(gltfJson, "meshes")
                val materialCount = getArraySize(gltfJson, "materials")
                val textureCount = getArraySize(gltfJson, "textures")
                val imageCount = getArraySize(gltfJson, "images")
                val animationCount = getArraySize(gltfJson, "animations")
                val bufferCount = getArraySize(gltfJson, "buffers")
                
                // Extract embedded images info
                val embeddedImages = extractEmbeddedImages(gltfJson)
                
                // TODO: Provide this information via collapsible pane
                // Log GLB info
                println("GLB Metadata [${glbFile.name}]:")
                println("  - GLB Version: $glbVersion")
                println("  - Total File Size: ${formatBytes(totalLength.toLong())}")
                println("  - JSON Chunk Size: ${formatBytes(jsonChunkLength.toLong())}")
                binaryChunkSize?.let { println("  - Binary Chunk Size: ${formatBytes(it.toLong())}") }
                println("  - Meshes: $meshCount")
                println("  - Materials: $materialCount")
                println("  - Textures: $textureCount")
                println("  - Images: $imageCount")
                println("  - Animations: $animationCount")
                println("  - Buffers: $bufferCount")
                
                // Log detailed imported assets
                extractAndLogImportedAssets(gltfJson, glbFile.name)
                
                return GlbInfo(
                    metadata = metadata,
                    glbVersion = glbVersion,
                    totalFileSize = totalLength,
                    jsonChunkSize = jsonChunkLength,
                    binaryChunkSize = binaryChunkSize,
                    meshCount = meshCount,
                    materialCount = materialCount,
                    textureCount = textureCount,
                    imageCount = imageCount,
                    animationCount = animationCount,
                    bufferCount = bufferCount,
                    embeddedImages = embeddedImages
                )
            }
        } catch (e: Exception) {
            println("GLB Parser: Error parsing GLB file ${glbFile.name}: ${e.message}")
            return null
        }
    }
    
    /**
     * Parses a GLB file in a single pass, returning both metadata and external asset references.
     * This is more efficient than calling parseGlbMetadata and parseGlbReferencedAssets separately,
     * as it only reads and parses the file once.
     *
     * @param glbFile The GLB file to parse
     * @return GlbParseResult containing both GlbInfo and referenced assets
     */
    fun parseGlb(glbFile: File): GlbParseResult {
        if (!glbFile.exists() || !glbFile.canRead()) {
            println("GLB Parser: Cannot read file ${glbFile.absolutePath}")
            return GlbParseResult(null, emptyList())
        }

        val parentDir = glbFile.parentFile
        val referencedFiles = mutableListOf<Pair<File, String>>()

        try {
            RandomAccessFile(glbFile, "r").use { raf ->
                // Read 12-byte header
                val headerBuffer = ByteArray(12)
                raf.readFully(headerBuffer)
                val header = ByteBuffer.wrap(headerBuffer).order(ByteOrder.LITTLE_ENDIAN)
                
                val magic = header.int
                if (magic != GLB_MAGIC) {
                    println("GLB Parser: Invalid GLB magic number in ${glbFile.name}")
                    return GlbParseResult(null, emptyList())
                }
                
                val glbVersion = header.int
                val totalLength = header.int
                
                // Read JSON chunk header (8 bytes)
                val chunkHeaderBuffer = ByteArray(8)
                raf.readFully(chunkHeaderBuffer)
                val chunkHeader = ByteBuffer.wrap(chunkHeaderBuffer).order(ByteOrder.LITTLE_ENDIAN)
                
                val jsonChunkLength = chunkHeader.int
                val jsonChunkType = chunkHeader.int
                
                if (jsonChunkType != GLB_CHUNK_TYPE_JSON) {
                    println("GLB Parser: First chunk is not JSON in ${glbFile.name}")
                    return GlbParseResult(null, emptyList())
                }
                
                // Validate jsonChunkLength to prevent OOM from malformed files
                val remainingBytes = totalLength - 20 // 12-byte header + 8-byte chunk header already read
                if (jsonChunkLength !in 0..remainingBytes) {
                    println("GLB Parser: Invalid JSON chunk length ($jsonChunkLength) in ${glbFile.name}, remaining bytes: $remainingBytes")
                    return GlbParseResult(null, emptyList())
                }
                
                // Read JSON chunk data
                val jsonBytes = ByteArray(jsonChunkLength)
                raf.readFully(jsonBytes)
                val jsonContent = String(jsonBytes, Charsets.UTF_8).trimEnd('\u0000', ' ')
                val gltfJson = gson.fromJson(jsonContent, JsonObject::class.java)
                
                // Check for binary chunk
                var binaryChunkSize: Int? = null
                if (raf.filePointer < totalLength.toLong()) {
                    val binChunkHeaderBuffer = ByteArray(8)
                    if (raf.read(binChunkHeaderBuffer) == 8) {
                        val binChunkHeader = ByteBuffer.wrap(binChunkHeaderBuffer).order(ByteOrder.LITTLE_ENDIAN)
                        val binChunkLength = binChunkHeader.int
                        val binChunkType = binChunkHeader.int
                        if (binChunkType == GLB_CHUNK_TYPE_BIN) {
                            binaryChunkSize = binChunkLength
                        }
                    }
                }
                
                // Extract metadata
                val metadata = extractMetadata(gltfJson, glbFile.name)
                
                // Extract asset counts
                val meshCount = getArraySize(gltfJson, "meshes")
                val materialCount = getArraySize(gltfJson, "materials")
                val textureCount = getArraySize(gltfJson, "textures")
                val imageCount = getArraySize(gltfJson, "images")
                val animationCount = getArraySize(gltfJson, "animations")
                val bufferCount = getArraySize(gltfJson, "buffers")
                
                // Extract embedded images info
                val embeddedImages = extractEmbeddedImages(gltfJson)
                
                // Log GLB info only when debug logging is enabled
                if (debugLogging) {
                    println("GLB Metadata [${glbFile.name}]:")
                    println("  - GLB Version: $glbVersion")
                    println("  - Total File Size: ${formatBytes(totalLength.toLong())}")
                    println("  - JSON Chunk Size: ${formatBytes(jsonChunkLength.toLong())}")
                    binaryChunkSize?.let { println("  - Binary Chunk Size: ${formatBytes(it.toLong())}") }
                    println("  - Meshes: $meshCount")
                    println("  - Materials: $materialCount")
                    println("  - Textures: $textureCount")
                    println("  - Images: $imageCount")
                    println("  - Animations: $animationCount")
                    println("  - Buffers: $bufferCount")
                    
                    // Log detailed imported assets
                    extractAndLogImportedAssets(gltfJson, glbFile.name)
                }
                
                // Extract external asset references (buffer URIs and image URIs)
                extractUrisFromArray(gltfJson, "buffers").forEach { uri ->
                    resolveAssetFile(uri, parentDir)?.let { referencedFiles.add(it) }
                }
                extractUrisFromArray(gltfJson, "images").forEach { uri ->
                    resolveAssetFile(uri, parentDir)?.let { referencedFiles.add(it) }
                }
                
                val glbInfo = GlbInfo(
                    metadata = metadata,
                    glbVersion = glbVersion,
                    totalFileSize = totalLength,
                    jsonChunkSize = jsonChunkLength,
                    binaryChunkSize = binaryChunkSize,
                    meshCount = meshCount,
                    materialCount = materialCount,
                    textureCount = textureCount,
                    imageCount = imageCount,
                    animationCount = animationCount,
                    bufferCount = bufferCount,
                    embeddedImages = embeddedImages
                )
                
                return GlbParseResult(glbInfo, referencedFiles)
            }
        } catch (e: Exception) {
            println("GLB Parser: Error parsing GLB file ${glbFile.name}: ${e.message}")
            return GlbParseResult(null, emptyList())
        }
    }
    
    /**
     * Gets the size of a JSON array by name, or 0 if not present.
     */
    private fun getArraySize(json: JsonObject, arrayName: String): Int {
        return if (json.has(arrayName) && json.get(arrayName).isJsonArray) {
            json.getAsJsonArray(arrayName).size()
        } else 0
    }
    
    /**
     * Extracts embedded image information from GLTF JSON.
     */
    private fun extractEmbeddedImages(gltfJson: JsonObject): List<EmbeddedImage> {
        val images = mutableListOf<EmbeddedImage>()
        
        if (gltfJson.has("images") && gltfJson.get("images").isJsonArray) {
            val imagesArray = gltfJson.getAsJsonArray("images")
            imagesArray.forEachIndexed { index, element ->
                if (element.isJsonObject) {
                    val imgObj = element.asJsonObject
                    val name = imgObj.get("name")?.asString
                    val mimeType = imgObj.get("mimeType")?.asString
                    val bufferView = imgObj.get("bufferView")?.asInt
                    val uri = imgObj.get("uri")?.asString
                    
                    images.add(EmbeddedImage(
                        index = index,
                        name = name,
                        mimeType = mimeType,
                        bufferViewIndex = bufferView,
                        uri = uri
                    ))
                }
            }
        }
        
        return images
    }
    
    /**
     * Extracts texture information from GLTF JSON.
     */
    private fun extractTextures(gltfJson: JsonObject): List<TextureInfo> {
        val textures = mutableListOf<TextureInfo>()
        
        if (gltfJson.has("textures") && gltfJson.get("textures").isJsonArray) {
            val texturesArray = gltfJson.getAsJsonArray("textures")
            texturesArray.forEachIndexed { index, element ->
                if (element.isJsonObject) {
                    val texObj = element.asJsonObject
                    val name = texObj.get("name")?.asString
                    val source = texObj.get("source")?.asInt
                    val sampler = texObj.get("sampler")?.asInt
                    
                    textures.add(TextureInfo(
                        index = index,
                        name = name,
                        sourceImageIndex = source,
                        samplerIndex = sampler
                    ))
                }
            }
        }
        
        return textures
    }
    
    /**
     * Extracts buffer information from GLTF JSON.
     */
    private fun extractBuffers(gltfJson: JsonObject): List<BufferInfo> {
        val buffers = mutableListOf<BufferInfo>()
        
        if (gltfJson.has("buffers") && gltfJson.get("buffers").isJsonArray) {
            val buffersArray = gltfJson.getAsJsonArray("buffers")
            buffersArray.forEachIndexed { index, element ->
                if (element.isJsonObject) {
                    val bufObj = element.asJsonObject
                    val name = bufObj.get("name")?.asString
                    val uri = bufObj.get("uri")?.asString
                    val byteLength = bufObj.get("byteLength")?.asInt ?: 0
                    
                    buffers.add(BufferInfo(
                        index = index,
                        name = name,
                        uri = uri,
                        byteLength = byteLength
                    ))
                }
            }
        }
        
        return buffers
    }
    
    /**
     * Extracts material information from GLTF JSON.
     */
    private fun extractMaterials(gltfJson: JsonObject): List<MaterialInfo> {
        val materials = mutableListOf<MaterialInfo>()
        
        if (gltfJson.has("materials") && gltfJson.get("materials").isJsonArray) {
            val materialsArray = gltfJson.getAsJsonArray("materials")
            materialsArray.forEachIndexed { index, element ->
                if (element.isJsonObject) {
                    val matObj = element.asJsonObject
                    val name = matObj.get("name")?.asString
                    
                    // PBR Metallic Roughness textures
                    var baseColorTexture: Int? = null
                    var metallicRoughnessTexture: Int? = null
                    if (matObj.has("pbrMetallicRoughness") && matObj.get("pbrMetallicRoughness").isJsonObject) {
                        val pbr = matObj.getAsJsonObject("pbrMetallicRoughness")
                        baseColorTexture = pbr.get("baseColorTexture")?.asJsonObject?.get("index")?.asInt
                        metallicRoughnessTexture = pbr.get("metallicRoughnessTexture")?.asJsonObject?.get("index")?.asInt
                    }
                    
                    val normalTexture = matObj.get("normalTexture")?.asJsonObject?.get("index")?.asInt
                    val emissiveTexture = matObj.get("emissiveTexture")?.asJsonObject?.get("index")?.asInt
                    val occlusionTexture = matObj.get("occlusionTexture")?.asJsonObject?.get("index")?.asInt
                    
                    materials.add(MaterialInfo(
                        index = index,
                        name = name,
                        baseColorTextureIndex = baseColorTexture,
                        normalTextureIndex = normalTexture,
                        metallicRoughnessTextureIndex = metallicRoughnessTexture,
                        emissiveTextureIndex = emissiveTexture,
                        occlusionTextureIndex = occlusionTexture
                    ))
                }
            }
        }
        
        return materials
    }
    
    /**
     * Extracts all imported assets and logs them.
     */
    private fun extractAndLogImportedAssets(gltfJson: JsonObject, fileName: String): ImportedAssets {
        val images = extractEmbeddedImages(gltfJson)
        val textures = extractTextures(gltfJson)
        val buffers = extractBuffers(gltfJson)
        val materials = extractMaterials(gltfJson)
        
        println("Imported Assets [$fileName]:")
        
        // Log buffers
        if (buffers.isNotEmpty()) {
            println("  Buffers (${buffers.size}):")
            buffers.forEach { buf ->
                val name = buf.name ?: "unnamed"
                val source = buf.uri?.let { "uri: $it" } ?: "embedded"
                println("    [${buf.index}] $name - ${formatBytes(buf.byteLength.toLong())} ($source)")
            }
        }
        
        // Log images
        if (images.isNotEmpty()) {
            println("  Images (${images.size}):")
            images.forEach { img ->
                val name = img.name ?: "unnamed"
                val mime = img.mimeType ?: "unknown type"
                val source = when {
                    img.uri != null && img.uri.startsWith("data:") -> "embedded base64"
                    img.uri != null -> "uri: ${img.uri}"
                    img.bufferViewIndex != null -> "bufferView: ${img.bufferViewIndex}"
                    else -> "unknown source"
                }
                println("    [${img.index}] $name ($mime) - $source")
            }
        }
        
        // Log textures
        if (textures.isNotEmpty()) {
            println("  Textures (${textures.size}):")
            textures.forEach { tex ->
                val name = tex.name ?: "unnamed"
                val imageRef = tex.sourceImageIndex?.let { "image[$it]" } ?: "no source"
                println("    [${tex.index}] $name -> $imageRef")
            }
        }
        
        // Log materials
        if (materials.isNotEmpty()) {
            println("  Materials (${materials.size}):")
            materials.forEach { mat ->
                val name = mat.name ?: "unnamed"
                val textureRefs = mutableListOf<String>()
                mat.baseColorTextureIndex?.let { textureRefs.add("baseColor[$it]") }
                mat.normalTextureIndex?.let { textureRefs.add("normal[$it]") }
                mat.metallicRoughnessTextureIndex?.let { textureRefs.add("metallicRoughness[$it]") }
                mat.emissiveTextureIndex?.let { textureRefs.add("emissive[$it]") }
                mat.occlusionTextureIndex?.let { textureRefs.add("occlusion[$it]") }
                val refs = if (textureRefs.isEmpty()) "no textures" else textureRefs.joinToString(", ")
                println("    [${mat.index}] $name - $refs")
            }
        }
        
        return ImportedAssets(images, textures, buffers, materials)
    }
    
    /**
     * Formats bytes into human-readable format.
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    /**
     * Extracts and logs metadata from the GLTF "asset" section.
     */
    private fun extractMetadata(gltfJson: JsonObject, fileName: String): GltfMetadata? {
        if (!gltfJson.has("asset") || !gltfJson.get("asset").isJsonObject) {
            println("GLTF Metadata [$fileName]: No asset section found")
            return null
        }

        val asset = gltfJson.getAsJsonObject("asset")

        val version = asset.get("version")?.asString
        val generator = asset.get("generator")?.asString
        val copyright = asset.get("copyright")?.asString
        val minVersion = asset.get("minVersion")?.asString
        val extras = if (asset.has("extras") && asset.get("extras").isJsonObject) {
            gson.fromJson(asset.get("extras"), Map::class.java) as? Map<String, Any>
        } else null

        println("GLTF Metadata [$fileName]:")
        println("  - Version: ${version ?: "N/A"}")
        println("  - Generator: ${generator ?: "N/A"}")
        println("  - Copyright: ${copyright ?: "N/A"}")
        println("  - Min Version: ${minVersion ?: "N/A"}")
        if (extras != null) {
            println("  - Extras: $extras")
        }

        return GltfMetadata(version, generator, copyright, minVersion, extras)
    }

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

            // Log metadata when parsing assets
            extractMetadata(gltfJson, gltfFile.name)

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
     * Security: Prevents path traversal attacks by ensuring resolved files remain within
     * the parent directory of the GLTF/GLB file.
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
        val canonicalParentPath = parentDir.canonicalPath

        // Prevent path traversal outside the parent directory
        if (!assetFile.canonicalPath.startsWith(canonicalParentPath)) {
            println("GLTF Parser: Security warning - Blocked path traversal attempt: $uri (resolved to ${assetFile.absolutePath}, outside of $canonicalParentPath)")
            return null
        }

        return if (assetFile.exists()) {
            println("GLTF Parser: Found referenced asset: ${assetFile.name} (relative path: $normalizedRelativePath)")
            Pair(assetFile, normalizedRelativePath)
        } else {
            println("GLTF Parser: Warning - Referenced asset not found: $uri (resolved to ${assetFile.absolutePath})")
            null
        }
    }

    /**
     * Parses a GLB file and returns a list of all referenced external asset files
     * along with their normalized relative paths.
     * 
     * Unlike self-contained GLB files, some GLB files reference external assets
     * (images with URIs instead of embedded bufferViews).
     *
     * @param glbFile The GLB file to parse
     * @return A list of Pairs containing (resolved File, normalized relative path with forward slashes)
     *         Missing files are logged and skipped.
     */
    fun parseGlbReferencedAssets(glbFile: File): List<Pair<File, String>> {
        if (!glbFile.exists() || !glbFile.canRead()) {
            println("GLB Parser: Cannot read file ${glbFile.absolutePath}")
            return emptyList()
        }

        val parentDir = glbFile.parentFile
        val referencedFiles = mutableListOf<Pair<File, String>>()

        try {
            RandomAccessFile(glbFile, "r").use { raf ->
                // Read 12-byte header
                val headerBuffer = ByteArray(12)
                raf.readFully(headerBuffer)
                val header = ByteBuffer.wrap(headerBuffer).order(ByteOrder.LITTLE_ENDIAN)
                
                val magic = header.int
                if (magic != GLB_MAGIC) {
                    println("GLB Parser: Invalid GLB magic number in ${glbFile.name}")
                    return emptyList()
                }
                
                // Skip version and totalLength
                header.int
                val totalLength = header.int
                
                // Read JSON chunk header (8 bytes)
                val chunkHeaderBuffer = ByteArray(8)
                raf.readFully(chunkHeaderBuffer)
                val chunkHeader = ByteBuffer.wrap(chunkHeaderBuffer).order(ByteOrder.LITTLE_ENDIAN)
                
                val jsonChunkLength = chunkHeader.int
                val jsonChunkType = chunkHeader.int
                
                if (jsonChunkType != GLB_CHUNK_TYPE_JSON) {
                    println("GLB Parser: First chunk is not JSON in ${glbFile.name}")
                    return emptyList()
                }
                
                // Validate jsonChunkLength to prevent OOM from malformed files
                val remainingBytes = totalLength - 20 // 12-byte header + 8-byte chunk header already read
                if (jsonChunkLength < 0 || jsonChunkLength > remainingBytes) {
                    println("GLB Parser: Invalid JSON chunk length ($jsonChunkLength) in ${glbFile.name}, remaining bytes: $remainingBytes")
                    return emptyList()
                }
                
                // Read JSON chunk data
                val jsonBytes = ByteArray(jsonChunkLength)
                raf.readFully(jsonBytes)
                val jsonContent = String(jsonBytes, Charsets.UTF_8).trimEnd('\u0000', ' ')
                val gltfJson = gson.fromJson(jsonContent, JsonObject::class.java)
                
                // Extract buffer URIs (external .bin files)
                extractUrisFromArray(gltfJson, "buffers").forEach { uri ->
                    resolveAssetFile(uri, parentDir)?.let { referencedFiles.add(it) }
                }

                // Extract image URIs (external texture files)
                extractUrisFromArray(gltfJson, "images").forEach { uri ->
                    resolveAssetFile(uri, parentDir)?.let { referencedFiles.add(it) }
                }
            }
        } catch (e: Exception) {
            println("GLB Parser: Error parsing GLB file for references ${glbFile.name}: ${e.message}")
        }

        return referencedFiles
    }
}
