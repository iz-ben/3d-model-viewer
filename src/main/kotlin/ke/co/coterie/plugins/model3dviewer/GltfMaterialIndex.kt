package ke.co.coterie.plugins.model3dviewer

import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Pure lookup helper that resolves glTF structural references (nodes, meshes, primitives)
 * to the set of material indices they use. Built once from the model's glTF JSON so the
 * caret/selection listener can translate a JSON location into the materials to highlight.
 *
 * A glTF material index equals its position in the top-level `materials` array, so callers
 * that already know a material index can use it directly; this class handles the indirection
 * for meshes (mesh -> primitives -> material) and nodes (node -> mesh -> materials).
 */
class GltfMaterialIndex private constructor(
    val materialCount: Int,
    // meshesToMaterials[m] = ordered materials of mesh m, one entry per primitive (null when
    // a primitive has no material). Index in the inner list is the primitive index.
    private val meshPrimitiveMaterials: List<List<Int?>>,
    // nodeToMesh[n] = mesh index referenced by node n, or null when the node has no mesh.
    private val nodeToMesh: List<Int?>
) {

    /** Materials used by every primitive of the given mesh (missing materials skipped). */
    fun materialsForMesh(meshIndex: Int): Set<Int> {
        val primitives = meshPrimitiveMaterials.getOrNull(meshIndex) ?: return emptySet()
        return primitives.filterNotNull().toSet()
    }

    /** Material used by a specific primitive of a mesh, or null when it has none. */
    fun materialForPrimitive(meshIndex: Int, primitiveIndex: Int): Int? =
        meshPrimitiveMaterials.getOrNull(meshIndex)?.getOrNull(primitiveIndex)

    /** Materials used by the mesh a node references (empty when the node has no mesh). */
    fun materialsForNode(nodeIndex: Int): Set<Int> {
        val mesh = nodeToMesh.getOrNull(nodeIndex) ?: return emptySet()
        return materialsForMesh(mesh)
    }

    /** True when the given index refers to an existing material. */
    fun isValidMaterial(index: Int): Boolean = index in 0 until materialCount

    companion object {
        /** Parses the glTF JSON, returning null when it cannot be read as a glTF object. */
        fun fromJson(json: String): GltfMaterialIndex? {
            val root = runCatching { JsonParser.parseString(json) }
                .getOrNull()
                ?.takeIf { it.isJsonObject }
                ?.asJsonObject
                ?: return null

            val materialCount = root.optArray("materials")?.size() ?: 0

            val meshPrimitiveMaterials = root.optArray("meshes")?.map { meshEl ->
                val mesh = meshEl.takeIf { it.isJsonObject }?.asJsonObject
                mesh?.optArray("primitives")?.map { primEl ->
                    primEl.takeIf { it.isJsonObject }?.asJsonObject?.optInt("material")
                } ?: emptyList()
            } ?: emptyList()

            val nodeToMesh = root.optArray("nodes")?.map { nodeEl ->
                nodeEl.takeIf { it.isJsonObject }?.asJsonObject?.optInt("mesh")
            } ?: emptyList()

            return GltfMaterialIndex(materialCount, meshPrimitiveMaterials, nodeToMesh)
        }

        private fun JsonObject.optArray(name: String) =
            get(name)?.takeIf { it.isJsonArray }?.asJsonArray

        private fun JsonObject.optInt(name: String): Int? =
            get(name)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asInt
    }
}
