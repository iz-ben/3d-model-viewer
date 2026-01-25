package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.NonNls

class Model3DEditorProvider : FileEditorProvider, DumbAware {

    companion object {
        private val SUPPORTED_EXTENSIONS = setOf("glb", "gltf")
    }

    override fun accept(
        project: Project,
        file: VirtualFile
    ): Boolean {
        val extension = file.extension?.lowercase() ?: return false

        // Always accept GLB and GLTF
        if (extension in SUPPORTED_EXTENSIONS) {
            return true
        }

        // Accept OBJ only if enabled in settings
        if (extension == "obj") {
            return Model3DSettings.getInstance().state.objSupportEnabled
        }

        return false
    }

    override fun createEditor(
        project: Project,
        file: VirtualFile
    ): FileEditor {
        return Model3DEditor(project, file)
    }

    override fun getEditorTypeId(): @NonNls String {
        return "MODEL_3D_EDITOR"
    }

    override fun getPolicy(): FileEditorPolicy {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR
    }
}
