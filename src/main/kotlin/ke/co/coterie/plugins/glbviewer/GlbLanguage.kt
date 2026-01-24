package ke.co.coterie.plugins.glbviewer

import com.intellij.lang.Language

class GlbLanguage: Language("GLB") {
    companion object {
        val INSTANCE = GlbLanguage()
    }
}