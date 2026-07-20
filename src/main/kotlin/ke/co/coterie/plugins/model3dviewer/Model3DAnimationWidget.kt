package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.application.ApplicationManager
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
            updateUIForState(state, file)
        }
    }

    private val viewerChangeListener: (VirtualFile?, Model3DViewer?) -> Unit = { file, viewer ->
        if (file != null) {
            val state = animationStateService.getState(file)
            // Visibility (handled in updateUIForState) follows whether the model
            // is animated, so the toggle is hidden for models without animations.
            updateUIForState(state, file)
            // Sync animation playing state with the newly selected viewer
            viewer?.toggleAnimation(state.isPlaying)
        } else {
            // No 3D model file selected, hide the widget
            updateUIForState(Model3DAnimationStateService.FileAnimationState(), null)
        }
    }

    private fun setWidgetVisible(visible: Boolean) {
        if (checkbox.isVisible != visible) {
            checkbox.isVisible = visible
            checkbox.parent?.revalidate()
            checkbox.parent?.repaint()
        }
    }

    private fun updateUIForState(state: Model3DAnimationStateService.FileAnimationState, file: VirtualFile?) {
        // Animation discovery arrives via the JCEF JS bridge, which invokes the
        // state-change listener off the EDT; marshal Swing mutations back onto it.
        val app = ApplicationManager.getApplication()
        if (!app.isDispatchThread) {
            app.invokeLater {
                if (!project.isDisposed) updateUIForState(state, file)
            }
            return
        }
        // Focus may have moved to another file between an off-EDT discovery and
        // this deferred apply; ignore a state that no longer matches the focus.
        if (file != null && viewerService.getCurrentFile()?.path != file.path) return
        val hasAnimations = state.availableAnimations.isNotEmpty()
        // Only show the toggle when the model actually has animations.
        setWidgetVisible(hasAnimations)
        checkbox.isEnabled = hasAnimations
        checkbox.isSelected = if (hasAnimations) state.isPlaying else false
    }

    init {
        animationStateService.addStateChangeListener(animationStateListener)
        viewerService.addViewerChangeListener(viewerChangeListener)

        // The widget stays hidden unless the focused model has animations.
        val currentFile = viewerService.getCurrentFile()
        if (currentFile != null) {
            updateUIForState(animationStateService.getState(currentFile), currentFile)
        } else {
            setWidgetVisible(false)
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
