package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Central helper for determining whether a file is a supported 3D model file
 * and whether such a file is currently in focus. Used by the status bar widget
 * factories so their widgets are only shown when a matching file is active.
 */
object Model3DFileSupport {
    private val BUILT_IN_EXTENSIONS = setOf("glb", "gltf")

    fun isSupported(file: VirtualFile?): Boolean {
        val extension = file?.extension?.lowercase() ?: return false
        if (extension in BUILT_IN_EXTENSIONS) {
            return true
        }
        if (extension == "obj") {
            return Model3DSettings.getInstance().state.objSupportEnabled
        }
        return false
    }

    /**
     * Returns true when the editor currently in focus holds a supported 3D model file.
     */
    fun isSupportedFileInFocus(project: Project): Boolean {
        val file = FileEditorManager.getInstance(project).selectedEditor?.file
        return isSupported(file)
    }
}
