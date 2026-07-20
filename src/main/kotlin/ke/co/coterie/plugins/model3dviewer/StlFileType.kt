package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.NlsSafe
import icons.Model3DIcon
import org.jetbrains.annotations.NonNls
import javax.swing.Icon

class StlFileType private constructor() : LanguageFileType(StlLanguage.INSTANCE) {
    companion object {
        @JvmField
        val INSTANCE = StlFileType()
    }

    override fun getName(): @NonNls String {
        return "STL"
    }

    override fun getDescription(): @NlsContexts.Label String {
        return "STL 3D Model file"
    }

    override fun getDefaultExtension(): @NlsSafe String {
        return "stl"
    }

    override fun getIcon(): Icon {
        return Model3DIcon.stlIcon
    }
}
