package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.ui.components.JBCheckBox
import javax.swing.BorderFactory
import javax.swing.JComponent

class Model3DAnimationWidget(project: Project) : EditorBasedWidget(project), CustomStatusBarWidget, StatusBarWidget.Multiframe {

    private val viewerService = Model3DViewerService.getInstance(project)
    private val animationStateService = Model3DAnimationStateService.getInstance(project)

    private val checkbox = JBCheckBox("Toggle animation").apply {
        isSelected = true // Default to playing
        toolTipText = "Toggle animation playback"
        isOpaque = false
        border = BorderFactory.createEmptyBorder(0, 8, 0, 0)
        isEnabled = false // Disabled by default until animations are available
        addActionListener {
            val currentFile = viewerService.getCurrentFile()
            if (currentFile != null) {
                animationStateService.setAnimationPlaying(currentFile, isSelected)
            }
            viewerService.getCurrentViewer()?.toggleAnimation(isSelected)
        }
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
            // Sync animation playing state with the newly selected viewer
            viewer?.toggleAnimation(state.isPlaying)
        } else {
            // No 3D model file selected, reset UI
            updateUIForState(Model3DAnimationStateService.FileAnimationState())
        }
    }

    private fun updateUIForState(state: Model3DAnimationStateService.FileAnimationState) {
        val hasAnimations = state.availableAnimations.isNotEmpty()
        checkbox.isEnabled = hasAnimations
        checkbox.isSelected = if (hasAnimations) state.isPlaying else false
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

    override fun ID(): String = "Model3DViewerAnimationWidget"

    override fun copy(): StatusBarWidget = Model3DAnimationWidget(project)

    override fun getComponent(): JComponent = checkbox

    override fun dispose() {
        animationStateService.removeStateChangeListener(animationStateListener)
        viewerService.removeViewerChangeListener(viewerChangeListener)
    }
}
