package ke.co.coterie.plugins.model3dviewer

import com.intellij.json.psi.JsonArray
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonValue
import com.intellij.openapi.util.TextRange

/**
 * Maps caret/selection ranges inside the glTF JSON (a [JsonFile]) to the material indices
 * they refer to, using JSON PSI so the mapping is position-accurate:
 *
 *  - inside `materials[i]`                -> material i
 *  - inside `meshes[m].primitives[p]`     -> that primitive's material
 *  - inside `meshes[m]` (but no primitive)-> every material used by mesh m
 *  - inside `nodes[n]`                    -> every material of the mesh node n references
 *
 * Ranges spanning several items contribute the union of their materials.
 */
object GltfJsonMaterialLocator {

    fun materialsForRanges(
        psiFile: JsonFile,
        index: GltfMaterialIndex,
        ranges: List<TextRange>
    ): Set<Int> {
        val root = psiFile.topLevelValue as? JsonObject ?: return emptySet()
        val out = mutableSetOf<Int>()
        for (range in ranges) {
            collect(root, index, range, out)
        }
        return out.filterTo(mutableSetOf()) { index.isValidMaterial(it) }
    }

    private fun collect(
        root: JsonObject,
        index: GltfMaterialIndex,
        range: TextRange,
        out: MutableSet<Int>
    ) {
        arrayItems(root, "materials").forEachIndexed { i, item ->
            if (item.hits(range)) out.add(i)
        }

        arrayItems(root, "meshes").forEachIndexed { m, meshItem ->
            if (!meshItem.hits(range)) return@forEachIndexed
            val primitives = (meshItem as? JsonObject)?.let { arrayItems(it, "primitives") } ?: emptyList()
            var matchedPrimitive = false
            primitives.forEachIndexed { p, primItem ->
                if (primItem.hits(range)) {
                    matchedPrimitive = true
                    index.materialForPrimitive(m, p)?.let { out.add(it) }
                }
            }
            if (!matchedPrimitive) {
                out.addAll(index.materialsForMesh(m))
            }
        }

        arrayItems(root, "nodes").forEachIndexed { n, nodeItem ->
            if (nodeItem.hits(range)) out.addAll(index.materialsForNode(n))
        }
    }

    private fun arrayItems(obj: JsonObject, name: String): List<JsonValue> =
        (obj.findProperty(name)?.value as? JsonArray)?.valueList ?: emptyList()

    private fun JsonValue.hits(range: TextRange): Boolean {
        val tr = textRange
        return if (range.isEmpty) tr.containsOffset(range.startOffset) else tr.intersects(range)
    }
}
