package ke.co.coterie.plugins.glbviewer

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.ui.components.JBCheckBox
import javax.swing.BorderFactory
import javax.swing.JComponent

class GlbWireframeWidget(project: Project) : EditorBasedWidget(project), CustomStatusBarWidget, StatusBarWidget.Multiframe {

    companion object {
        var wireframeEnabled = false
        var currentViewer: GlbViewer? = null
    }

    private val checkbox = JBCheckBox("Wireframe").apply {
        isSelected = wireframeEnabled
        toolTipText = "Toggle wireframe mode"
        isOpaque = false
        border = BorderFactory.createEmptyBorder(0, 8, 0, 0)
        addActionListener {
            wireframeEnabled = isSelected
            currentViewer?.toggleWireframe(wireframeEnabled)
        }
    }

    override fun ID(): String = "GlbViewerWireframeWidget"

    override fun copy(): StatusBarWidget = GlbWireframeWidget(project)

    override fun getComponent(): JComponent = checkbox
}
