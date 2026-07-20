package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project

/**
 * Shows the "Model Explorer" tool window's stripe button only while a `.glb` / `.gltf` file
 * is in focus. Toggling availability (rather than reading file content) is cheap and safe on
 * the EDT, mirroring how the status bar widgets react to selection changes.
 */
class GltfStructureAvailabilityListener(private val project: Project) : FileEditorManagerListener {

    override fun selectionChanged(event: FileEditorManagerEvent) = updateAvailability()

    override fun fileOpened(
        source: FileEditorManager,
        file: com.intellij.openapi.vfs.VirtualFile,
    ) = updateAvailability()

    private fun updateAvailability() {
        GltfStructureToolWindow.updateAvailability(project)
    }
}
