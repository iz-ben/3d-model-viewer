package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * Registers the "Model Explorer" tool window, which shows the internal structure of the
 * active `.glb` / `.gltf` file. The stripe button is hidden by default (see
 * [shouldBeAvailable]) and toggled on by [GltfStructureAvailabilityListener] only while a
 * glTF model is in focus, so it never clutters non-3D projects.
 */
class GltfStructureToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun shouldBeAvailable(project: Project): Boolean = false

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = GltfStructurePanel(project, toolWindow)
        val content = ContentFactory.getInstance().createContent(panel, "", false)
        content.setDisposer(panel)
        toolWindow.contentManager.addContent(content)
    }

    companion object {
        const val ID: String = "Model Explorer"
    }
}
