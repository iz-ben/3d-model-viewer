package ke.co.coterie.plugins.glbviewer

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.ui.components.JBLabel
import java.awt.FlowLayout
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel

class GlbAnimationSelectorWidget(project: Project) : EditorBasedWidget(project), CustomStatusBarWidget, StatusBarWidget.Multiframe {

    companion object {
        private val animationListeners = mutableListOf<(List<String>) -> Unit>()
        private var animations: List<String> = emptyList()

        fun updateAnimations(animationList: List<String>) {
            animations = animationList
            animationListeners.forEach { it(animationList) }
        }

        fun addListener(listener: (List<String>) -> Unit) {
            animationListeners.add(listener)
            // Immediately notify with current animations if any
            if (animations.isNotEmpty()) {
                listener(animations)
            }
        }

        fun removeListener(listener: (List<String>) -> Unit) {
            animationListeners.remove(listener)
        }
    }

    private val comboBoxModel = DefaultComboBoxModel<String>()
    private val comboBox = JComboBox(comboBoxModel).apply {
        toolTipText = "Select animation to play"
        addActionListener {
            val selected = selectedItem as? String
            if (selected != null && selected.isNotEmpty()) {
                GlbWireframeWidget.currentViewer?.selectAnimation(selected)
            }
        }
    }

    private val panel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply {
        isOpaque = false
        add(JBLabel("Animation:"))
        add(comboBox)
    }

    private val animationUpdateListener: (List<String>) -> Unit = { animationList ->
        comboBoxModel.removeAllElements()
        animationList.forEach { comboBoxModel.addElement(it) }
        if (animationList.isNotEmpty()) {
            comboBox.selectedIndex = 0
        }
    }

    init {
        addListener(animationUpdateListener)
    }

    override fun ID(): String = "GlbViewerAnimationSelectorWidget"

    override fun copy(): StatusBarWidget = GlbAnimationSelectorWidget(project)

    override fun getComponent(): JComponent = panel

    override fun dispose() {
        removeListener(animationUpdateListener)
        //super.dispose()
    }
}
