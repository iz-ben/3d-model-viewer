package ke.co.coterie.plugins.model3dviewer

import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.ex.StructureViewFileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.NonNls

class Model3DEditorProvider : FileEditorProvider, DumbAware, StructureViewFileEditorProvider {

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

    /**
     * Implemented so the Structure tool window resolves a builder *without* creating and
     * immediately disposing a throwaway [Model3DTextEditorWithPreview] (see
     * [StructureViewFileEditorProvider] javadoc). That throwaway path raced our composite
     * editor's deferred UI initialization against its disposal, throwing
     * IncorrectOperationException. We expose no structural view for 3D model files (the
     * Model Explorer tool window covers glTF/GLB), so return null.
     */
    override fun getStructureViewBuilder(project: Project, file: VirtualFile): StructureViewBuilder? = null

    override fun getEditorTypeId(): @NonNls String {
        return "MODEL_3D_EDITOR"
    }

    override fun getPolicy(): FileEditorPolicy {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR
    }
}
