package ke.co.coterie.plugins.glbviewer

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.ui.components.JBCheckBox
import javax.swing.BorderFactory
import javax.swing.JComponent

class GlbAnimationWidget(project: Project) : EditorBasedWidget(project), CustomStatusBarWidget, StatusBarWidget.Multiframe {

    companion object {
        var animationEnabled = true
    }

    private val checkbox = JBCheckBox("Toggle animation").apply {
        isSelected = animationEnabled
        toolTipText = "Toggle animation playback"
        isOpaque = false
        border = BorderFactory.createEmptyBorder(0, 8, 0, 0)
        isEnabled = false // Disabled by default until animations are available
        addActionListener {
            animationEnabled = isSelected
            GlbWireframeWidget.currentViewer?.toggleAnimation(animationEnabled)
        }
    }

    private val animationUpdateListener: (List<String>) -> Unit = { animationList ->
        checkbox.isEnabled = animationList.isNotEmpty()
        if (animationList.isEmpty()) {
            checkbox.isSelected = false
            animationEnabled = false
        }
    }

    init {
        GlbAnimationSelectorWidget.addListener(animationUpdateListener)
    }

    override fun ID(): String = "GlbViewerAnimationWidget"

    override fun copy(): StatusBarWidget = GlbAnimationWidget(project)

    override fun getComponent(): JComponent = checkbox

    override fun dispose() {
        GlbAnimationSelectorWidget.removeListener(animationUpdateListener)
    }
}
