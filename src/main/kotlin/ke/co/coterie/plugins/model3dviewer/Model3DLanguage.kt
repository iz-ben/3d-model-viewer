package ke.co.coterie.plugins.model3dviewer

import com.intellij.lang.Language

class Model3DLanguage private constructor() : Language("3DModel") {
    companion object {
        @JvmField
        val INSTANCE = Model3DLanguage()
    }
}
