package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.ex.EditorEx
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
    preview: FileEditor
) : TextEditorWithPreview(
    textEditor,
    preview,
    "3D Model Viewer",
    Layout.SHOW_EDITOR_AND_PREVIEW
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
            // syntax highlighting without depending on the JSON plugin module directly.
            val jsonFile = LightVirtualFile("${file.name}.json", LOADING_PLACEHOLDER)

            val editor = TextEditorProvider.getInstance().createEditor(project, jsonFile) as? TextEditor
                ?: return null
            // Read-only display without relying on file writability, so we can still
            // populate the document from the background task below.
            (editor.editor as? EditorEx)?.isViewer = true

            loadJsonAsync(project, file, editor)
            return editor
        }

        private fun loadJsonAsync(project: Project, file: VirtualFile, editor: TextEditor) {
            ApplicationManager.getApplication().executeOnPooledThread {
                val text = runCatching { GltfAssetParser.extractGltfJson(File(file.path)) }
                    .getOrNull()
                    ?: "// Unable to read glTF JSON for ${file.name}"
                val normalized = StringUtil.convertLineSeparators(text)
                ApplicationManager.getApplication().invokeLater(
                    {
                        val ed = editor.editor
                        if (!project.isDisposed && !ed.isDisposed) {
                            ApplicationManager.getApplication().runWriteAction {
                                ed.document.setText(normalized)
                            }
                        }
                    },
                    ModalityState.any()
                )
            }
        }
    }
}
