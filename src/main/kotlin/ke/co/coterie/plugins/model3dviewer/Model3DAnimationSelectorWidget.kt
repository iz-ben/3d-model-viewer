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

    // Guards against re-entrancy: programmatic combo box updates (rebuilding the
    // model / setting the selected item) must not trigger the user-selection logic.
    private var isUpdating = false

    private val comboBox = ComboBox(comboBoxModel).apply {
        toolTipText = "Select animation to play"
        isEnabled = false // Disabled by default until animations are available
        addActionListener {
            if (isUpdating) return@addActionListener
            val selected = selectedItem as? String
            println("selected animation: $selected")
            if (selected?.isNotEmpty() == true) {
                println("Sending selected animation to js: $selected")
                val currentFile = viewerService.getCurrentFile()
                if (currentFile != null) {
                    animationStateService.setSelectedAnimation(currentFile, selected)
                    // Selecting an animation starts playback in the viewer, so keep
                    // the play/pause state in sync (turns the toggle on).
                    animationStateService.setAnimationPlaying(currentFile, true)
                }
                viewerService.getCurrentViewer()?.selectAnimation(selected)
            }
        }
    }

    private val label = JBLabel("Animation:").apply {
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
            // Visibility (handled in updateUIForState) follows whether the model
            // is animated, so the selector is hidden for models without animations.
            updateUIForState(state)
            // Sync selected animation with the newly selected viewer
            state.selectedAnimation?.let { animation ->
                viewer?.selectAnimation(animation)
            }
        } else {
            // No 3D model file selected, hide the widget
            updateUIForState(Model3DAnimationStateService.FileAnimationState())
        }
    }

    private fun setWidgetVisible(visible: Boolean) {
        if (panel.isVisible != visible) {
            panel.isVisible = visible
            panel.parent?.revalidate()
            panel.parent?.repaint()
        }
    }

    private fun updateUIForState(state: Model3DAnimationStateService.FileAnimationState) {
        isUpdating = true
        try {
            comboBoxModel.removeAllElements()
            state.availableAnimations.forEach { comboBoxModel.addElement(it) }
            val hasAnimations = state.availableAnimations.isNotEmpty()
            // Only show the selector when the model actually has animations.
            setWidgetVisible(hasAnimations)
            comboBox.isEnabled = hasAnimations
            label.isEnabled = hasAnimations
            if (hasAnimations && state.selectedAnimation != null) {
                comboBox.selectedItem = state.selectedAnimation
            } else if (hasAnimations) {
                comboBox.selectedIndex = 0
            }
        } finally {
            isUpdating = false
        }
    }

    init {
        animationStateService.addStateChangeListener(animationStateListener)
        viewerService.addViewerChangeListener(viewerChangeListener)

        // The widget stays hidden unless the focused model has animations.
        val currentFile = viewerService.getCurrentFile()
        if (currentFile != null) {
            updateUIForState(animationStateService.getState(currentFile))
        } else {
            setWidgetVisible(false)
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
