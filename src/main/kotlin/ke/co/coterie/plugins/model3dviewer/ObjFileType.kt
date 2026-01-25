package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.NlsSafe
import icons.Model3DIcon
import org.jetbrains.annotations.NonNls
import javax.swing.Icon

class ObjFileType private constructor() : LanguageFileType(ObjLanguage.INSTANCE) {
    companion object {
        @JvmField
        val INSTANCE = ObjFileType()
    }

    override fun getName(): @NonNls String {
        return "OBJ"
    }

    override fun getDescription(): @NlsContexts.Label String {
        return "OBJ 3D Model file"
    }

    override fun getDefaultExtension(): @NlsSafe String {
        return "obj"
    }

    override fun getIcon(): Icon {
        return Model3DIcon.objIcon
    }
}
