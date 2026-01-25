package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/**
 * Application-level settings for the 3D Model Viewer plugin.
 * Persists user preferences across IDE restarts.
 */
@Service(Service.Level.APP)
@State(
    name = "Model3DViewerSettings",
    storages = [Storage("model3dviewer.xml")]
)
class Model3DSettings : PersistentStateComponent<Model3DSettings.State> {

    private var myState = State()
    private val listeners = mutableListOf<() -> Unit>()

    data class State(
        var objSupportEnabled: Boolean = false
    )

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    /**
     * Enable or disable OBJ file support.
     * @param enabled true to enable OBJ support, false to disable
     */
    fun setObjSupportEnabled(enabled: Boolean) {
        if (myState.objSupportEnabled != enabled) {
            myState.objSupportEnabled = enabled
            notifyListeners()
        }
    }

    /**
     * Add a listener to be notified when settings change.
     */
    fun addSettingsChangeListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    /**
     * Remove a settings change listener.
     */
    fun removeSettingsChangeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        listeners.forEach { it() }
    }

    companion object {
        fun getInstance(): Model3DSettings {
            return ApplicationManager.getApplication().getService(Model3DSettings::class.java)
        }
    }
}
