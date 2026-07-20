package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.wm.ToolWindowManager

/**
 * Shared logic for showing/hiding the "Model Explorer" tool window stripe. Toggling
 * availability only inspects the focused file's extension (no disk IO), so it is safe on the
 * EDT and during editor restore.
 */
object GltfStructureToolWindow {

    fun updateAvailability(project: Project) {
        if (project.isDisposed) return
        val toolWindow = ToolWindowManager.getInstance(project)
            .getToolWindow(GltfStructureToolWindowFactory.ID) ?: return
        val modelFile = Model3DFileSupport.resolveModelFile(
            FileEditorManager.getInstance(project).selectedEditor,
        )
        toolWindow.isAvailable = Model3DFileSupport.isGltfStructured(modelFile)
    }
}

/**
 * Sets the initial stripe visibility once the project has opened, so a `.glb` / `.gltf`
 * restored as the active tab shows the tool window without waiting for the next selection
 * change (the availability listener may run before the tool window is registered).
 */
class GltfStructureStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        ApplicationManager.getApplication().invokeLater(
            { GltfStructureToolWindow.updateAvailability(project) },
            ModalityState.any(),
        )
    }
}

