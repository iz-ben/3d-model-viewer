package ke.co.coterie.plugins.model3dviewer

import com.intellij.lang.Language

class GltfLanguage private constructor() : Language("GLTF") {
    companion object {
        @JvmField
        val INSTANCE = GltfLanguage()
    }
}
