package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.NonNls

class Model3DEditorProvider : FileEditorProvider, DumbAware {

    override fun accept(
        project: Project,
        file: VirtualFile
    ): Boolean {
        return Model3DFileSupport.isSupported(file)
    }

    override fun createEditor(
        project: Project,
        file: VirtualFile
    ): FileEditor {
        val preview = Model3DEditor(project, file)
        // Show the glTF JSON next to the 3D preview (editor/split/preview toggle).
        // Fall back to a preview-only editor if the JSON cannot be extracted.
        val jsonEditor = Model3DTextEditorWithPreview.createJsonTextEditor(project, file)
        return if (jsonEditor != null) {
            Model3DTextEditorWithPreview(jsonEditor, preview)
        } else {
            preview
        }
    }

    override fun getEditorTypeId(): @NonNls String {
        return "MODEL_3D_EDITOR"
    }

    override fun getPolicy(): FileEditorPolicy {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR
    }
}
