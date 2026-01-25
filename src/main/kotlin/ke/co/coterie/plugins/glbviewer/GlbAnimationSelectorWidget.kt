package ke.co.coterie.plugins.glbviewer

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.ui.components.JBLabel
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.DefaultComboBoxModel
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
    private val comboBox = ComboBox(comboBoxModel).apply {
        toolTipText = "Select animation to play"
        isEnabled = false // Disabled by default until animations are available
        addActionListener {
            val selected = selectedItem as? String
            println("selected animation: $selected")
            if (selected?.isNotEmpty() == true) {
                println("Sending selected animation to js: $selected")
                GlbWireframeWidget.currentViewer?.selectAnimation(selected)
            }
        }
    }

    private val label = JBLabel("Pick animation:").apply {
        isEnabled = false // Disabled by default until animations are available
    }

    private val panel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply {
        isOpaque = false
        border = BorderFactory.createEmptyBorder(0, 8, 0, 0)
        add(label)
        add(comboBox)
    }

    private val animationUpdateListener: (List<String>) -> Unit = { animationList ->
        comboBoxModel.removeAllElements()
        animationList.forEach { comboBoxModel.addElement(it) }
        val hasAnimations = animationList.isNotEmpty()
        comboBox.isEnabled = hasAnimations
        label.isEnabled = hasAnimations
        if (hasAnimations) {
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
