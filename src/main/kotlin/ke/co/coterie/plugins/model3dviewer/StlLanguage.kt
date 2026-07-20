package ke.co.coterie.plugins.model3dviewer

import com.intellij.lang.Language

class StlLanguage private constructor() : Language("STL") {
    companion object {
        @JvmField
        val INSTANCE = StlLanguage()
    }
}
