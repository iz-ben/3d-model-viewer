package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Central helper for determining whether a file is a supported 3D model file
 * and whether such a file is currently in focus. Used by the status bar widget
 * factories so their widgets are only shown when a matching file is active.
 */
object Model3DFileSupport {
    private val BUILT_IN_EXTENSIONS = setOf("glb", "gltf", "stl")

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
     * Resolves the real 3D model file backing a [FileEditor]. For the JSON split editor
     * ([Model3DTextEditorWithPreview]) the editor's own file is an in-memory JSON file,
     * so we resolve the model file from the preview side instead.
     */
    fun resolveModelFile(editor: FileEditor?): VirtualFile? = when (editor) {
        is Model3DTextEditorWithPreview -> editor.modelFile
        else -> editor?.file
    }

    /**
     * Returns true when [editor] shows a supported 3D model.
     *
     * Extension-based [isSupported] can't recognise three.js model JSON (`.json` is
     * deliberately not a supported extension, so the default JSON editor keeps working
     * and only one editor provider claims the file). Our own editors, however, are only
     * ever created for files that a provider already accepted as a 3D model, so their
     * presence is itself proof of support — and checking the editor type needs no disk IO.
     */
    fun isSupportedEditor(editor: FileEditor?): Boolean {
        if (editor is Model3DTextEditorWithPreview || editor is Model3DEditor) return true
        return isSupported(resolveModelFile(editor))
    }

    /**
     * Returns true when the editor currently in focus holds a supported 3D model file.
     */
    fun isSupportedFileInFocus(project: Project): Boolean {
        val editor = FileEditorManager.getInstance(project).selectedEditor
        return isSupportedEditor(editor)
    }
}
