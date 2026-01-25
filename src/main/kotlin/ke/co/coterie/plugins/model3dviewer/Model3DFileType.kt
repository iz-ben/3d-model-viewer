package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.NlsSafe
import icons.Model3DIcon
import org.jetbrains.annotations.NonNls
import javax.swing.Icon

class Model3DFileType private constructor() : LanguageFileType(Model3DLanguage.INSTANCE) {
    companion object {
        @JvmField
        val INSTANCE = Model3DFileType()
    }

    override fun getName(): @NonNls String {
        return "GLB"
    }

    override fun getDescription(): @NlsContexts.Label String {
        return "GLB 3D Model file"
    }

    override fun getDefaultExtension(): @NlsSafe String {
        return "glb"
    }

    override fun getIcon(): Icon {
        return Model3DIcon.glbIcon
    }
}
