package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.components.Service

/**
 * Tracks whether a project has finished opening, so the 3D-preview Got It tooltip
 * is not shown (and its once-per-user flag not consumed) during the bulk editor
 * restore that happens while the project is still opening. See
 * [Model3DJsonGotItListener].
 *
 * The ready flag is flipped by [Model3DJsonGotItStartupActivity] once the project
 * has finished opening.
 */
@Service(Service.Level.PROJECT)
class Model3DJsonGotItState {

    @Volatile
    var isReady: Boolean = false
        private set

    fun markReady() {
        isReady = true
    }
}
