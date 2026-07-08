package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.ui.components.JBCheckBox
import javax.swing.BorderFactory
import javax.swing.JComponent

class Model3DWireframeWidget(project: Project) : EditorBasedWidget(project), CustomStatusBarWidget, StatusBarWidget.Multiframe {

    companion object {
        var wireframeEnabled = false
    }

    private val viewerService = Model3DViewerService.getInstance(project)

    private val checkbox = JBCheckBox("Wireframe").apply {
        isSelected = wireframeEnabled
        toolTipText = "Toggle wireframe mode"
        isOpaque = false
        border = BorderFactory.createEmptyBorder(0, 8, 0, 0)
        addActionListener {
            wireframeEnabled = isSelected
            println("Wireframe mode: $wireframeEnabled")
            viewerService.getCurrentViewer()?.toggleWireframe(wireframeEnabled)
        }
    }

    private val viewerChangeListener: (VirtualFile?, Model3DViewer?) -> Unit = { file, viewer ->
        // Only show this widget while a supported 3D model file is in focus
        setWidgetVisible(file != null)
        // Sync wireframe state with the newly selected viewer
        viewer?.toggleWireframe(wireframeEnabled)
    }

    private fun setWidgetVisible(visible: Boolean) {
        if (checkbox.isVisible != visible) {
            checkbox.isVisible = visible
            checkbox.parent?.revalidate()
            checkbox.parent?.repaint()
        }
    }

    init {
        setWidgetVisible(Model3DFileSupport.isSupportedFileInFocus(project))
        viewerService.addViewerChangeListener(viewerChangeListener)
    }

    override fun ID(): String = "Model3DViewerWireframeWidget"

    override fun copy(): StatusBarWidget = Model3DWireframeWidget(project)

    override fun getComponent(): JComponent = checkbox

    override fun dispose() {
        viewerService.removeViewerChangeListener(viewerChangeListener)
    }
}
