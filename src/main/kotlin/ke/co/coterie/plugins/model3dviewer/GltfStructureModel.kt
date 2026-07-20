package ke.co.coterie.plugins.model3dviewer

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * A single segment of a structural path into the glTF JSON. Paths are structural (property
 * name / array index) rather than raw text offsets, so the tool window can resolve them
 * against the *displayed* (pretty-printed) JSON PSI regardless of the original formatting.
 */
sealed interface GltfPathSeg {
    data class Key(val name: String) : GltfPathSeg
    data class Index(val index: Int) : GltfPathSeg
}

/**
 * One node of the glTF structure tree. Category nodes (e.g. "Materials") group leaf nodes
 * (e.g. "Material 0 – Steel"). [path] locates the node in the glTF JSON for navigation;
 * [materialIndices] are the materials to highlight in the 3D preview when the node is
 * selected (empty when the node maps to no material).
 */
class GltfStructureNode(
    val label: String,
    val secondary: String? = null,
    val path: List<GltfPathSeg>? = null,
    val materialIndices: List<Int> = emptyList(),
    val children: List<GltfStructureNode> = emptyList(),
)

/**
 * Builds a [GltfStructureNode] tree from a glTF JSON document. Pure and side-effect free so
 * it can run off the EDT. See the glTF 2.0 spec for the meaning of each top-level array.
 */
object GltfStructureModel {

    private val COMPONENT_TYPES = mapOf(
        5120 to "BYTE", 5121 to "UNSIGNED_BYTE", 5122 to "SHORT",
        5123 to "UNSIGNED_SHORT", 5125 to "UNSIGNED_INT", 5126 to "FLOAT",
    )

    /** Parses [json], returning the root node (its children are the categories), or null. */
    fun build(json: String): GltfStructureNode? {
        val root = runCatching { JsonParser.parseString(json) }.getOrNull()
            ?.takeIf { it.isJsonObject }?.asJsonObject
            ?: return null

        val materialIndex = GltfMaterialIndex.fromJson(json)
        val categories = mutableListOf<GltfStructureNode>()

        root.obj("asset")?.let { categories += assetNode(it) }
        arrayCategory(root, "scenes", "Scenes") { i, o -> sceneNode(i, o) }?.let { categories += it }
        arrayCategory(root, "nodes", "Nodes") { i, o -> nodeNode(i, o, materialIndex) }?.let { categories += it }
        arrayCategory(root, "meshes", "Meshes") { i, o -> meshNode(i, o, materialIndex) }?.let { categories += it }
        arrayCategory(root, "materials", "Materials") { i, o -> materialNode(i, o) }?.let { categories += it }
        arrayCategory(root, "accessors", "Accessors") { i, o -> accessorNode(i, o) }?.let { categories += it }
        arrayCategory(root, "bufferViews", "Buffer Views") { i, o -> bufferViewNode(i, o) }?.let { categories += it }
        arrayCategory(root, "buffers", "Buffers") { i, o -> bufferNode(i, o) }?.let { categories += it }
        arrayCategory(root, "textures", "Textures") { i, o -> textureNode(i, o) }?.let { categories += it }
        arrayCategory(root, "images", "Images") { i, o -> imageNode(i, o) }?.let { categories += it }
        arrayCategory(root, "samplers", "Samplers") { i, o -> samplerNode(i, o) }?.let { categories += it }
        arrayCategory(root, "animations", "Animations") { i, o -> animationNode(i, o) }?.let { categories += it }
        arrayCategory(root, "skins", "Skins") { i, o -> skinNode(i, o) }?.let { categories += it }
        arrayCategory(root, "cameras", "Cameras") { i, o -> cameraNode(i, o) }?.let { categories += it }
        stringArrayCategory(root, "extensionsUsed", "Extensions Used")?.let { categories += it }
        stringArrayCategory(root, "extensionsRequired", "Extensions Required")?.let { categories += it }

        val version = root.obj("asset")?.str("version")
        val generator = root.obj("asset")?.str("generator")
        val secondary = listOfNotNull(version?.let { "glTF $it" }, generator).joinToString(" · ").ifEmpty { null }
        return GltfStructureNode(label = "glTF", secondary = secondary, children = categories)
    }

    private fun assetNode(asset: JsonObject): GltfStructureNode {
        val children = buildList {
            asset.str("version")?.let { add(leaf("version", it, listOf(GltfPathSeg.Key("asset"), GltfPathSeg.Key("version")))) }
            asset.str("generator")?.let { add(leaf("generator", it, listOf(GltfPathSeg.Key("asset"), GltfPathSeg.Key("generator")))) }
            asset.str("copyright")?.let { add(leaf("copyright", it, listOf(GltfPathSeg.Key("asset"), GltfPathSeg.Key("copyright")))) }
            asset.str("minVersion")?.let { add(leaf("minVersion", it, listOf(GltfPathSeg.Key("asset"), GltfPathSeg.Key("minVersion")))) }
        }
        return GltfStructureNode("Asset", asset.str("version")?.let { "glTF $it" }, listOf(GltfPathSeg.Key("asset")), children = children)
    }

    private fun sceneNode(i: Int, o: JsonObject): GltfStructureNode {
        val nodes = o.arr("nodes")?.size() ?: 0
        return leaf(o.name(i, "Scene"), "$nodes root node(s)".takeIf { nodes > 0 }, path("scenes", i))
    }

    private fun nodeNode(i: Int, o: JsonObject, mi: GltfMaterialIndex?): GltfStructureNode {
        val mesh = o.int("mesh")
        val secondary = when {
            mesh != null -> "mesh $mesh"
            o.has("camera") -> "camera ${o.int("camera")}"
            o.arr("children") != null -> "${o.arr("children")!!.size()} child node(s)"
            else -> null
        }
        return leaf(o.name(i, "Node"), secondary, path("nodes", i), mi?.materialsForNode(i)?.sorted() ?: emptyList())
    }

    private fun meshNode(i: Int, o: JsonObject, mi: GltfMaterialIndex?): GltfStructureNode {
        val prims = o.arr("primitives")?.size() ?: 0
        return leaf(o.name(i, "Mesh"), "$prims primitive(s)", path("meshes", i), mi?.materialsForMesh(i)?.sorted() ?: emptyList())
    }

    private fun materialNode(i: Int, o: JsonObject): GltfStructureNode {
        val bits = buildList {
            o.str("alphaMode")?.let { add(it) }
            if (o.get("doubleSided")?.asBoolean == true) add("double-sided")
        }
        return leaf(o.name(i, "Material"), bits.joinToString(", ").ifEmpty { null }, path("materials", i), listOf(i))
    }

    private fun accessorNode(i: Int, o: JsonObject): GltfStructureNode {
        val type = o.str("type")
        val comp = o.int("componentType")?.let { COMPONENT_TYPES[it] ?: it.toString() }
        val count = o.int("count")
        val secondary = listOfNotNull(listOfNotNull(type, comp).joinToString("/").ifEmpty { null }, count?.let { "×$it" })
            .joinToString(" ").ifEmpty { null }
        return leaf("Accessor $i", secondary, path("accessors", i))
    }

    private fun bufferViewNode(i: Int, o: JsonObject): GltfStructureNode =
        leaf("Buffer View $i", o.int("byteLength")?.let { "${it} bytes" }, path("bufferViews", i))

    private fun bufferNode(i: Int, o: JsonObject): GltfStructureNode {
        val secondary = listOfNotNull(o.int("byteLength")?.let { "$it bytes" }, o.str("uri")?.let { uriLabel(it) })
            .joinToString(" · ").ifEmpty { null }
        return leaf("Buffer $i", secondary, path("buffers", i))
    }

    private fun textureNode(i: Int, o: JsonObject): GltfStructureNode {
        val secondary = listOfNotNull(o.int("source")?.let { "source $it" }, o.int("sampler")?.let { "sampler $it" })
            .joinToString(", ").ifEmpty { null }
        return leaf("Texture $i", secondary, path("textures", i))
    }

    private fun imageNode(i: Int, o: JsonObject): GltfStructureNode {
        val label = o.str("name") ?: o.str("uri")?.let { uriLabel(it) } ?: "Image $i"
        val secondary = listOfNotNull(o.str("mimeType"), o.int("bufferView")?.let { "bufferView $it" })
            .joinToString(", ").ifEmpty { null }
        return leaf(label, secondary, path("images", i))
    }

    private fun samplerNode(i: Int, o: JsonObject): GltfStructureNode =
        leaf("Sampler $i", listOfNotNull(o.int("magFilter")?.let { "mag $it" }, o.int("minFilter")?.let { "min $it" })
            .joinToString(", ").ifEmpty { null }, path("samplers", i))

    private fun animationNode(i: Int, o: JsonObject): GltfStructureNode =
        leaf(o.name(i, "Animation"), o.arr("channels")?.let { "${it.size()} channel(s)" }, path("animations", i))

    private fun skinNode(i: Int, o: JsonObject): GltfStructureNode =
        leaf(o.name(i, "Skin"), o.arr("joints")?.let { "${it.size()} joint(s)" }, path("skins", i))

    private fun cameraNode(i: Int, o: JsonObject): GltfStructureNode =
        leaf(o.name(i, "Camera"), o.str("type"), path("cameras", i))

    // ---- helpers -------------------------------------------------------------------

    private fun arrayCategory(
        root: JsonObject,
        key: String,
        label: String,
        leafOf: (Int, JsonObject) -> GltfStructureNode,
    ): GltfStructureNode? {
        val array = root.arr(key)?.takeIf { it.size() > 0 } ?: return null
        val children = array.mapIndexedNotNull { i, el ->
            (el as? JsonObject ?: el.takeIf { it.isJsonObject }?.asJsonObject)?.let { leafOf(i, it) }
        }
        return GltfStructureNode("$label (${array.size()})", path = listOf(GltfPathSeg.Key(key)), children = children)
    }

    private fun stringArrayCategory(root: JsonObject, key: String, label: String): GltfStructureNode? {
        val array = root.arr(key)?.takeIf { it.size() > 0 } ?: return null
        val children = array.mapIndexedNotNull { i, el ->
            el.takeIf { it.isJsonPrimitive }?.asString?.let { leaf(it, null, path(key, i)) }
        }
        return GltfStructureNode("$label (${array.size()})", path = listOf(GltfPathSeg.Key(key)), children = children)
    }

    private fun leaf(
        label: String,
        secondary: String?,
        path: List<GltfPathSeg>?,
        materialIndices: List<Int> = emptyList(),
    ) = GltfStructureNode(label, secondary, path, materialIndices)

    private fun path(key: String, index: Int) = listOf(GltfPathSeg.Key(key), GltfPathSeg.Index(index))

    private fun uriLabel(uri: String): String =
        if (uri.startsWith("data:")) "embedded" else uri.substringAfterLast('/')

    private fun JsonObject.name(index: Int, kind: String): String = str("name") ?: "$kind $index"

    private fun JsonObject.obj(name: String): JsonObject? =
        get(name)?.takeIf { it.isJsonObject }?.asJsonObject

    private fun JsonObject.arr(name: String): JsonArray? =
        get(name)?.takeIf { it.isJsonArray }?.asJsonArray

    private fun JsonObject.str(name: String): String? =
        get(name)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString

    private fun JsonObject.int(name: String): Int? =
        get(name)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asInt

    private fun JsonObject.has(name: String): Boolean = get(name) != null
}
