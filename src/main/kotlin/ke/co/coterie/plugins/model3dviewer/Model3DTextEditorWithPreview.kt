package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import java.io.File

/**
 * Editor that shows the glTF JSON alongside the 3D model preview, mirroring the
 * editor / split / preview toggle of the Markdown editor.
 *
 * The JSON side is a read-only view: for .gltf it is the file contents, for .glb
 * it is the JSON chunk extracted from the binary container.
 */
class Model3DTextEditorWithPreview(
    textEditor: TextEditor,
    preview: FileEditor,
    defaultLayout: Layout = Layout.SHOW_EDITOR_AND_PREVIEW
) : TextEditorWithPreview(
    textEditor,
    preview,
    "3D Model Viewer",
    defaultLayout
) {

    /**
     * The real 3D model file backing this editor (from the preview side). The text
     * editor's own file is an in-memory JSON [LightVirtualFile], so status bar widgets
     * and [Model3DViewerService] use this to know which 3D model is in focus. We do
     * NOT override [getFile] for this, because the platform ties the text editor's
     * highlighting session to [getFile]; returning the model file there breaks JSON
     * highlighting.
     */
    val modelFile: VirtualFile?
        get() = previewEditor.file

    /** The glTF JSON side of this editor, exposed for structure-tree navigation. */
    val jsonTextEditor: TextEditor = textEditor

    /**
     * Reveals [offset] in the glTF JSON side: makes the editor visible (if only the preview
     * was showing) and moves the caret there. Used by the glTF Structure tool window to jump
     * to the JSON of a selected structure node.
     */
    fun revealJsonLocation(offset: Int) {
        if (getLayout() == Layout.SHOW_PREVIEW) {
            setLayout(Layout.SHOW_EDITOR_AND_PREVIEW)
        }
        val ed = jsonTextEditor.editor
        if (offset in 0..ed.document.textLength) {
            ed.caretModel.moveToOffset(offset)
            ed.scrollingModel.scrollToCaret(ScrollType.CENTER)
        }
    }

    companion object {
        private val JSON_EXTENSIONS = setOf("gltf", "glb")
        private const val LOADING_PLACEHOLDER = "// Loading glTF JSON…"

        /**
         * Builds a read-only [TextEditor] showing the model's glTF JSON, or null when the
         * file cannot contain extractable glTF JSON (e.g. .obj), in which case the caller
         * should fall back to a preview-only editor.
         *
         * The editor is returned immediately with a placeholder; the actual JSON is read
         * and pretty-printed off the EDT and set on the document once ready, so
         * constructing the editor never blocks the UI on file IO for large models.
         */
        fun createJsonTextEditor(project: Project, file: VirtualFile): TextEditor? {
            if (file.extension?.lowercase() !in JSON_EXTENSIONS) return null

            // Present the JSON in a light in-memory file named *.json so it gets JSON
            // syntax highlighting and JSON PSI (used to map the caret to materials).
            val jsonFile = LightVirtualFile("${file.name}.json", LOADING_PLACEHOLDER)

            val editor = TextEditorProvider.getInstance().createEditor(project, jsonFile) as? TextEditor
                ?: return null
            // Read-only display without relying on file writability, so we can still
            // populate the document from the background task below.
            (editor.editor as? EditorEx)?.isViewer = true

            // Highlight the corresponding materials in the 3D preview as the caret moves
            // or the selection changes. Listeners are disposed together with the editor.
            val highlightListener = GltfMaterialHighlightListener(project, file, editor.editor)
            editor.editor.caretModel.addCaretListener(highlightListener, editor)
            editor.editor.selectionModel.addSelectionListener(highlightListener, editor)

            loadJsonAsync(project, file, editor, highlightListener)
            return editor
        }

        private fun loadJsonAsync(
            project: Project,
            file: VirtualFile,
            editor: TextEditor,
            highlightListener: GltfMaterialHighlightListener
        ) {
            ApplicationManager.getApplication().executeOnPooledThread {
                val json = runCatching { GltfAssetParser.extractGltfJson(File(file.path)) }.getOrNull()
                val text = json ?: "// Unable to read glTF JSON for ${file.name}"
                val normalized = StringUtil.convertLineSeparators(text)
                // Build the material index off-EDT from the same JSON we just read.
                val materialIndex = json?.let { GltfMaterialIndex.fromJson(it) }
                ApplicationManager.getApplication().invokeLater(
                    {
                        val ed = editor.editor
                        if (!project.isDisposed && !ed.isDisposed) {
                            ApplicationManager.getApplication().runWriteAction {
                                ed.document.setText(normalized)
                            }
                            highlightListener.materialIndex = materialIndex
                        }
                    },
                    ModalityState.any()
                )
            }
        }
    }
}
