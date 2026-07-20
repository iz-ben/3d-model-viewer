package ke.co.coterie.plugins.model3dviewer

import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.fileEditor.ex.StructureViewFileEditorProvider
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.NonNls

/**
 * Adds a 3D preview for three.js model JSON files without disturbing the IDE's
 * default JSON editing.
 *
 * `.json` stays a normal JSON file: this provider accepts a file only when its
 * content is a three.js model (see [Model3DJsonSupport]). It replaces the default
 * editor with a single [TextEditorWithPreview] whose text side is the ordinary,
 * fully editable JSON editor for the real file and whose preview side is the 3D
 * viewer. Using one combined editor (rather than a second
 * [FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR] tab) keeps the familiar
 * editor/preview toggle and avoids the platform's multi-provider selection edge
 * cases.
 */
class Model3DJsonEditorProvider : FileEditorProvider, DumbAware, StructureViewFileEditorProvider {

    override fun accept(project: Project, file: VirtualFile): Boolean =
        Model3DJsonSupport.isThreeJsModelJson(file)

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val preview = Model3DEditor(project, file)
        // The text side is the standard JSON editor for the real file, so editing,
        // saving, schema validation and highlighting all keep working. Open showing
        // the editor (not a split) so it behaves like a normal JSON file until the
        // user toggles the preview.
        val textEditor = TextEditorProvider.getInstance().createEditor(project, file) as? TextEditor
        return if (textEditor != null) {
            Model3DTextEditorWithPreview(textEditor, preview, TextEditorWithPreview.Layout.SHOW_EDITOR)
        } else {
            preview
        }
    }

    /**
     * Implemented so the Structure tool window never spins up and immediately disposes a
     * throwaway [Model3DTextEditorWithPreview] just to read its structure builder (see
     * [StructureViewFileEditorProvider] javadoc); that race threw
     * IncorrectOperationException. When this model JSON is the active editor the live
     * composite still exposes the normal JSON structure, so returning null here is safe.
     */
    override fun getStructureViewBuilder(project: Project, file: VirtualFile): StructureViewBuilder? = null

    override fun getEditorTypeId(): @NonNls String = EDITOR_TYPE_ID

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR

    companion object {
        const val EDITOR_TYPE_ID: String = "MODEL_3D_JSON_PREVIEW"
    }
}
