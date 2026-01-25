package ke.co.coterie.plugins.model3dviewer

import com.intellij.lang.Language

class ObjLanguage private constructor() : Language("OBJ") {
    companion object {
        @JvmField
        val INSTANCE = ObjLanguage()
    }
}
