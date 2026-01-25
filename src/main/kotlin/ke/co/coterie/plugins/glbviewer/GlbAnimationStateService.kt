package ke.co.coterie.plugins.glbviewer

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Service that stores animation state per GLB file.
 * This allows each file to maintain its own list of available animations,
 * selected animation, and playback state.
 */
@Service(Service.Level.PROJECT)
class GlbAnimationStateService : Disposable {

    data class FileAnimationState(
        val availableAnimations: List<String> = emptyList(),
        var selectedAnimation: String? = null,
        var isPlaying: Boolean = true
    )

    private val fileAnimationStates = mutableMapOf<String, FileAnimationState>()
    private val stateChangeListeners = mutableListOf<(VirtualFile, FileAnimationState) -> Unit>()

    /**
     * Get the animation state for a specific file.
     * Returns an empty state if none exists.
     */
    fun getState(file: VirtualFile): FileAnimationState {
        return fileAnimationStates.getOrPut(file.path) { FileAnimationState() }
    }

    /**
     * Update the available animations for a file.
     * Preserves the selected animation if it's still in the new list.
     */
    fun updateAvailableAnimations(file: VirtualFile, animations: List<String>) {
        val current = getState(file)
        val selectedAnim = if (current.selectedAnimation in animations) {
            current.selectedAnimation
        } else {
            animations.firstOrNull()
        }
        val newState = current.copy(
            availableAnimations = animations,
            selectedAnimation = selectedAnim
        )
        fileAnimationStates[file.path] = newState
        notifyListeners(file, newState)
    }

    /**
     * Set the selected animation for a file.
     */
    fun setSelectedAnimation(file: VirtualFile, animationName: String?) {
        val current = getState(file)
        val newState = current.copy(selectedAnimation = animationName)
        fileAnimationStates[file.path] = newState
        notifyListeners(file, newState)
    }

    /**
     * Set whether animation is playing for a file.
     */
    fun setAnimationPlaying(file: VirtualFile, isPlaying: Boolean) {
        val current = getState(file)
        val newState = current.copy(isPlaying = isPlaying)
        fileAnimationStates[file.path] = newState
        notifyListeners(file, newState)
    }

    /**
     * Add a listener for animation state changes.
     */
    fun addStateChangeListener(listener: (VirtualFile, FileAnimationState) -> Unit) {
        stateChangeListeners.add(listener)
    }

    /**
     * Remove a state change listener.
     */
    fun removeStateChangeListener(listener: (VirtualFile, FileAnimationState) -> Unit) {
        stateChangeListeners.remove(listener)
    }

    private fun notifyListeners(file: VirtualFile, state: FileAnimationState) {
        stateChangeListeners.forEach { it(file, state) }
    }

    override fun dispose() {
        fileAnimationStates.clear()
        stateChangeListeners.clear()
    }

    companion object {
        fun getInstance(project: Project): GlbAnimationStateService {
            return project.getService(GlbAnimationStateService::class.java)
        }
    }
}
