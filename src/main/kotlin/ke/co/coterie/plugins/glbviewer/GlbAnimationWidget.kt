package ke.co.coterie.plugins.glbviewer

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.ui.components.JBCheckBox
import javax.swing.BorderFactory
import javax.swing.JComponent

class GlbAnimationWidget(project: Project) : EditorBasedWidget(project), CustomStatusBarWidget, StatusBarWidget.Multiframe {

    private val viewerService = GlbViewerService.getInstance(project)
    private val animationStateService = GlbAnimationStateService.getInstance(project)

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

    private val animationStateListener: (VirtualFile, GlbAnimationStateService.FileAnimationState) -> Unit = { file, state ->
        // Only update if this is the currently active file
        if (viewerService.getCurrentFile()?.path == file.path) {
            updateUIForState(state)
        }
    }

    private val viewerChangeListener: (VirtualFile?, GlbViewer?) -> Unit = { file, _ ->
        if (file != null) {
            val state = animationStateService.getState(file)
            updateUIForState(state)
        } else {
            // No GLB file selected, reset UI
            updateUIForState(GlbAnimationStateService.FileAnimationState())
        }
    }

    private fun updateUIForState(state: GlbAnimationStateService.FileAnimationState) {
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

    override fun ID(): String = "GlbViewerAnimationWidget"

    override fun copy(): StatusBarWidget = GlbAnimationWidget(project)

    override fun getComponent(): JComponent = checkbox

    override fun dispose() {
        animationStateService.removeStateChangeListener(animationStateListener)
        viewerService.removeViewerChangeListener(viewerChangeListener)
    }
}
