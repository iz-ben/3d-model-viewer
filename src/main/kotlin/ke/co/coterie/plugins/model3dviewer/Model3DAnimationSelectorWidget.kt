package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.ui.components.JBLabel
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.JPanel

class Model3DAnimationSelectorWidget(project: Project) : EditorBasedWidget(project), CustomStatusBarWidget, StatusBarWidget.Multiframe {

    private val viewerService = Model3DViewerService.getInstance(project)
    private val animationStateService = Model3DAnimationStateService.getInstance(project)

    private val comboBoxModel = DefaultComboBoxModel<String>()
    private val comboBox = ComboBox(comboBoxModel).apply {
        toolTipText = "Select animation to play"
        isEnabled = false // Disabled by default until animations are available
        addActionListener {
            val selected = selectedItem as? String
            println("selected animation: $selected")
            if (selected?.isNotEmpty() == true) {
                println("Sending selected animation to js: $selected")
                val currentFile = viewerService.getCurrentFile()
                if (currentFile != null) {
                    animationStateService.setSelectedAnimation(currentFile, selected)
                }
                viewerService.getCurrentViewer()?.selectAnimation(selected)
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

    private val animationStateListener: (VirtualFile, Model3DAnimationStateService.FileAnimationState) -> Unit = { file, state ->
        // Only update if this is the currently active file
        if (viewerService.getCurrentFile()?.path == file.path) {
            updateUIForState(state)
        }
    }

    private val viewerChangeListener: (VirtualFile?, Model3DViewer?) -> Unit = { file, viewer ->
        if (file != null) {
            val state = animationStateService.getState(file)
            updateUIForState(state)
            // Sync selected animation with the newly selected viewer
            state.selectedAnimation?.let { animation ->
                viewer?.selectAnimation(animation)
            }
        } else {
            // No 3D model file selected, reset UI
            updateUIForState(Model3DAnimationStateService.FileAnimationState())
        }
    }

    private fun updateUIForState(state: Model3DAnimationStateService.FileAnimationState) {
        comboBoxModel.removeAllElements()
        state.availableAnimations.forEach { comboBoxModel.addElement(it) }
        val hasAnimations = state.availableAnimations.isNotEmpty()
        comboBox.isEnabled = hasAnimations
        label.isEnabled = hasAnimations
        if (hasAnimations && state.selectedAnimation != null) {
            comboBox.selectedItem = state.selectedAnimation
        } else if (hasAnimations) {
            comboBox.selectedIndex = 0
        }
    }

    init {
        animationStateService.addStateChangeListener(animationStateListener)
        viewerService.addViewerChangeListener(viewerChangeListener)

        // Initialize with current file's state if available
        viewerService.getCurrentFile()?.let { file ->
            val state = animationStateService.getState(file)
            updateUIForState(state)
        }
    }

    override fun ID(): String = "Model3DViewerAnimationSelectorWidget"

    override fun copy(): StatusBarWidget = Model3DAnimationSelectorWidget(project)

    override fun getComponent(): JComponent = panel

    override fun dispose() {
        animationStateService.removeStateChangeListener(animationStateListener)
        viewerService.removeViewerChangeListener(viewerChangeListener)
    }
}
