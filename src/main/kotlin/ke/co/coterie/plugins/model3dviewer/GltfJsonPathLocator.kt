package ke.co.coterie.plugins.model3dviewer

import com.intellij.json.psi.JsonArray
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonValue
import com.intellij.psi.PsiElement

/**
 * Resolves a structural [GltfPathSeg] path (as produced by [GltfStructureModel]) to a text
 * offset inside the *displayed* glTF JSON, using JSON PSI. Because the path is structural
 * rather than an absolute offset, it stays correct against the pretty-printed JSON the editor
 * shows even though the tool window tree was built from the raw JSON.
 */
object GltfJsonPathLocator {

    /** The offset to navigate to for [path], or null when the path does not resolve. */
    fun offsetForPath(psiFile: JsonFile, path: List<GltfPathSeg>): Int? {
        var value: JsonValue = psiFile.topLevelValue ?: return null
        var target: PsiElement = value
        for (seg in path) {
            when (seg) {
                is GltfPathSeg.Key -> {
                    val prop = (value as? JsonObject)?.findProperty(seg.name) ?: return null
                    target = prop
                    value = prop.value ?: return null
                }
                is GltfPathSeg.Index -> {
                    val element = (value as? JsonArray)?.valueList?.getOrNull(seg.index) ?: return null
                    target = element
                    value = element
                }
            }
        }
        return target.textOffset
    }
}
