package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.GotItTooltip

/**
 * Surfaces the 3D preview the first time a three.js model JSON is opened.
 *
 * The file opens as a normal JSON editor, so the editor toolbar's preview toggle
 * is easy to miss. A one-time [GotItTooltip] (shown once per user, tracked by the
 * platform via its id) points at the editor and offers a link that reveals the 3D
 * preview.
 *
 * It only fires once the project has finished opening (see [Model3DJsonGotItState]):
 * during the bulk restore of previously open tabs, the anchor tab is not the one
 * the user is looking at and startup focus churn would dismiss the balloon while
 * still consuming its once-per-user flag. Reacting to selection as well as opening
 * means the tooltip appears the first time the user actually views such a file.
 */
class Model3DJsonGotItListener : FileEditorManagerListener {

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        maybeShowTooltip(source, file)
    }

    override fun selectionChanged(event: FileEditorManagerEvent) {
        val file = event.newFile ?: return
        maybeShowTooltip(event.manager, file)
    }

    private fun maybeShowTooltip(source: FileEditorManager, file: VirtualFile) {
        val project = source.project
        // Ignore events fired while the project is still opening (tab restore); the
        // tooltip should appear when the user genuinely opens or views such a file.
        if (!project.service<Model3DJsonGotItState>().isReady) return
        // Cheap, no-IO gate: our provider is the only one that builds a
        // Model3DTextEditorWithPreview for a .json file. Checking the already-created
        // composite avoids re-sniffing the file from disk on the EDT.
        if (file.extension?.lowercase() != "json") return
        if (source.getEditors(file).none { it is Model3DTextEditorWithPreview }) return

        // Defer until the editor composite is laid out so the anchor is showing.
        ApplicationManager.getApplication().invokeLater {
            if (project.isDisposed || !file.isValid) return@invokeLater

            val editor = source.getEditors(file)
                .firstOrNull { it is Model3DTextEditorWithPreview } as? Model3DTextEditorWithPreview
                ?: return@invokeLater

            GotItTooltip(GOT_IT_ID, MODEL3D_JSON_GOT_IT_BODY, editor)
                .withHeader(MODEL3D_JSON_GOT_IT_HEADER)
                .withLink(MODEL3D_JSON_GOT_IT_LINK) {
                    editor.setLayout(TextEditorWithPreview.Layout.SHOW_EDITOR_AND_PREVIEW)
                }
                .show(editor.component, GotItTooltip.TOP_MIDDLE)
        }
    }

    companion object {
        private const val GOT_IT_ID = "ke.co.coterie.plugins.model3dviewer.json.preview"
        private const val MODEL3D_JSON_GOT_IT_HEADER = "3D preview available"
        private const val MODEL3D_JSON_GOT_IT_BODY =
            "This file is a three.js model. Toggle the preview from the editor toolbar to view it in 3D."
        private const val MODEL3D_JSON_GOT_IT_LINK = "Show 3D preview"
    }
}
