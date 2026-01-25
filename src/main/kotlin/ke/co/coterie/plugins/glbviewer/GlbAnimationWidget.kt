package ke.co.coterie.plugins.glbviewer

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.ui.components.JBCheckBox
import javax.swing.JComponent

class GlbAnimationWidget(project: Project) : EditorBasedWidget(project), CustomStatusBarWidget, StatusBarWidget.Multiframe {

    companion object {
        var animationEnabled = true
    }

    private val checkbox = JBCheckBox("Animation").apply {
        isSelected = animationEnabled
        toolTipText = "Toggle animation playback"
        isOpaque = false
        addActionListener {
            animationEnabled = isSelected
            GlbWireframeWidget.currentViewer?.toggleAnimation(animationEnabled)
        }
    }

    override fun ID(): String = "GlbViewerAnimationWidget"

    override fun copy(): StatusBarWidget = GlbAnimationWidget(project)

    override fun getComponent(): JComponent = checkbox
}
