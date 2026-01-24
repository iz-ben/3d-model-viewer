package ke.co.coterie.plugins.glbviewer

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.NlsSafe
import ke.co.coterie.plugins.glbviewer.GlbIcon.Companion.FILE
import org.jetbrains.annotations.NonNls
import javax.swing.Icon

class GlbFileType: LanguageFileType(GlbLanguage.INSTANCE) {
    companion object {
        val INSTANCE = GlbFileType()
    }
    override fun getName(): @NonNls String {
        return "GLB"
    }

    override fun getDescription(): @NlsContexts.Label String {
        return "Glb Language file"
    }

    override fun getDefaultExtension(): @NlsSafe String {
        return "glb"
    }

    override fun getIcon(): Icon {
        return FILE
    }

}