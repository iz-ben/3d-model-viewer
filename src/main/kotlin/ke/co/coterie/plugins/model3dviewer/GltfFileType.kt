package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.NlsSafe
import icons.Model3DIcon
import org.jetbrains.annotations.NonNls
import javax.swing.Icon

class GltfFileType private constructor() : LanguageFileType(GltfLanguage.INSTANCE) {
    companion object {
        @JvmField
        val INSTANCE = GltfFileType()
    }

    override fun getName(): @NonNls String {
        return "GLTF"
    }

    override fun getDescription(): @NlsContexts.Label String {
        return "GLTF 3D Model file"
    }

    override fun getDefaultExtension(): @NlsSafe String {
        return "gltf"
    }

    override fun getIcon(): Icon {
        return Model3DIcon.gltfIcon
    }
}
