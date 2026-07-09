package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.ui.components.JBCheckBox
import javax.swing.BorderFactory
import javax.swing.JComponent

class Model3DAutoRotateWidget(project: Project) : EditorBasedWidget(project), CustomStatusBarWidget, StatusBarWidget.Multiframe {

    companion object {
        // Auto-rotate is on by default to preserve the viewer's original behaviour.
        var autoRotateEnabled = true
    }

    private val viewerService = Model3DViewerService.getInstance(project)

    private val checkbox = JBCheckBox("Auto-rotate").apply {
        isSelected = autoRotateEnabled
        toolTipText = "Toggle automatic camera rotation"
        isOpaque = false
        border = BorderFactory.createEmptyBorder(0, 8, 0, 0)
        addActionListener {
            autoRotateEnabled = isSelected
            println("Auto-rotate mode: $autoRotateEnabled")
            viewerService.getCurrentViewer()?.toggleAutoRotate(autoRotateEnabled)
        }
    }

    private val viewerChangeListener: (VirtualFile?, Model3DViewer?) -> Unit = { file, viewer ->
        // Only show this widget while a supported 3D model file is in focus
        setWidgetVisible(file != null)
        // Sync auto-rotate state with the newly selected viewer
        viewer?.toggleAutoRotate(autoRotateEnabled)
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

    override fun ID(): String = "Model3DViewerAutoRotateWidget"

    override fun copy(): StatusBarWidget = Model3DAutoRotateWidget(project)

    override fun getComponent(): JComponent = checkbox

    override fun dispose() {
        viewerService.removeViewerChangeListener(viewerChangeListener)
    }
}
